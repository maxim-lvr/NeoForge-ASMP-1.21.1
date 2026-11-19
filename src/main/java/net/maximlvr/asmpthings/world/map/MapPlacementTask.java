package net.maximlvr.asmpthings.world.map;

import net.maximlvr.asmpthings.AsmpThingsMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

import java.io.IOException;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/** Server-thread placement. One chunk, one disk page and one section fragment at a time. */
public final class MapPlacementTask implements AutoCloseable {
    public static final long MAX_NANOS_PER_TICK = 8_000_000;
    private static final TicketType<ChunkPos> TICKET = TicketType.create("asmp_map_build", Comparator.comparingLong(ChunkPos::toLong));
    private static final int UPDATE_FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SUPPRESS_DROPS;
    private static final EnumSet<Heightmap.Types> HEIGHTMAPS = EnumSet.of(Heightmap.Types.WORLD_SURFACE,
            Heightmap.Types.OCEAN_FLOOR, Heightmap.Types.MOTION_BLOCKING, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES);
    private final ServerLevel level;
    private final MapPlanIndex index;
    private final BlockState[] states;
    private final StructureTemplate[] templates;
    private final int budget, radius, chunksPerSecond;
    private final BitSet completed = new BitSet();
    private ChunkWork current;
    private int nextChunk, finishedChunks, credits;
    private long filled, skipped, sections, operations;
    private boolean closed;

    public MapPlacementTask(ServerLevel level, MapPlanIndex index, BlockState[] states, StructureTemplate[] templates,
                            int budget, int radius, int chunksPerSecond) {
        this.level = level;
        this.index = index;
        this.states = states;
        this.templates = templates;
        this.budget = budget;
        this.radius = radius;
        this.chunksPerSecond = chunksPerSecond;
    }

    public void tick() throws IOException {
        if (closed || isFinished()) return;
        long deadline = System.nanoTime() + MAX_NANOS_PER_TICK;
        credits = Math.min(20, credits + chunksPerSecond);
        if (current == null) {
            if (credits < 20) return;
            int selected = radius >= 0 ? nearbyChunk() : nextChunk++;
            if (selected < 0 || selected >= index.chunkCount()) return;
            current = new ChunkWork(selected);
            credits -= 20;
        }
        current.advance(deadline);
        if (current.finished()) {
            completed.set(current.number);
            finishedChunks++;
            current.close();
            current = null;
        }
        // Never start a second chunk in this tick, even if the preceding chunk was cheap.
    }

    private int nearbyChunk() {
        for (int distance = 0; distance <= radius; distance++) {
            for (ServerPlayer player : level.players()) {
                int centerX = player.blockPosition().getX() >> 4, centerZ = player.blockPosition().getZ() >> 4;
                for (int z = -distance; z <= distance; z++) for (int x = -distance; x <= distance; x++) {
                    if (Math.max(Math.abs(x), Math.abs(z)) != distance) continue;
                    int number = index.findChunk(centerX + x, centerZ + z);
                    if (number >= 0 && !completed.get(number)) return number;
                }
            }
        }
        return -1;
    }

    public boolean isFinished() { return finishedChunks == index.chunkCount(); }
    public int finishedChunks() { return finishedChunks; }
    public String status() {
        String position = current == null ? "" : ", chunk " + current.pos.x + " " + current.pos.z;
        return "Génération " + (radius >= 0 ? "autour des joueurs" : "par chunks") + " : "
                + finishedChunks + "/" + index.chunkCount() + " chunks, " + operations + " opérations, "
                + filled + " blocs modifiés, " + skipped + " déjà conformes, " + sections + " sections"
                + position + ", cible " + Math.min(chunksPerSecond, 20) + " chunks/s, plafond de travail 8 ms/tick";
    }

    @Override public void close() {
        closed = true;
        if (current != null) {
            current.close();
            current = null;
        }
    }

    private final class ChunkWork implements AutoCloseable {
        final int number;
        final ChunkPos pos;
        final LevelChunk chunk;
        final MapPlanIndex.Cursor cursor;
        final BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        Iterator<MapOperation> pieces;
        MapOperation fragment;
        long blockOffset;
        boolean exhausted, changed, ticketReleased, packetSent;
        CompletableFuture<ChunkAccess> lighting;

        ChunkWork(int number) {
            this.number = number;
            long key = index.chunkKey(number);
            pos = new ChunkPos(MapPlanIndex.chunkX(key), MapPlanIndex.chunkZ(key));
            level.getChunkSource().addRegionTicket(TICKET, pos, 0, pos);
            try {
                chunk = level.getChunk(pos.x, pos.z);
                cursor = index.openChunk(number);
            } catch (RuntimeException error) {
                releaseTicket();
                throw error;
            }
        }

        void advance(long deadline) throws IOException {
            if (lighting != null) {
                if (lighting.isCompletedExceptionally()) throw new IOException("Échec de l'éclairage du chunk " + pos);
                return;
            }
            int used = 0;
            while (used < budget && System.nanoTime() < deadline) {
                if (fragment == null) {
                    if (pieces != null && pieces.hasNext()) fragment = pieces.next();
                    else {
                        MapOperation next = cursor.next();
                        if (next == null) {
                            exhausted = true;
                            finishChanges();
                            return;
                        }
                        operations++;
                        if (templates[next.material()] != null) {
                            StructurePlaceSettings settings = new StructurePlaceSettings().setMirror(Mirror.NONE)
                                    .setRotation(Rotation.values()[next.rotation()]).setIgnoreEntities(false).setFinalizeEntities(true);
                            boolean success = templates[next.material()].placeInWorld(level,
                                    new BlockPos(next.x(), next.y(), next.z()), new BlockPos(next.x(), next.y(), next.z()),
                                    settings, level.getRandom(), Block.UPDATE_ALL);
                            if (!success) throw new IOException("Échec du placement NBT dans " + pos);
                            changed = true;
                            return; // NBT placement itself is indivisible; at most one per tick.
                        }
                        pieces = next.sections();
                        fragment = pieces.next();
                    }
                    blockOffset = 0;
                }
                BlockState state = states[fragment.material()];
                if (state.getFluidState().isEmpty() && !state.hasBlockEntity() && state.getLightEmission() == 0) {
                    fillSection(fragment, state);
                    used += Math.max(1, (int) ((fragment.volume() + 63) / 64));
                    fragment = null;
                } else {
                    used += placeIndividualBlocks(state, budget - used, deadline);
                    if (blockOffset == fragment.volume()) fragment = null;
                    else return;
                }
            }
        }

        private int placeIndividualBlocks(BlockState state, int allowance, long deadline) {
            int used = 0;
            while (used < allowance && blockOffset < fragment.volume()) {
                if ((used & 63) == 0 && System.nanoTime() >= deadline) break;
                long offset = blockOffset++;
                int dz = (int) (offset % fragment.sizeZ());
                long plane = offset / fragment.sizeZ();
                int dy = (int) (plane % fragment.sizeY()), dx = (int) (plane / fragment.sizeY());
                mutable.set(fragment.x() + dx, fragment.y() + dy, fragment.z() + dz);
                if (level.getBlockState(mutable).equals(state)) skipped++;
                else {
                    level.setBlock(mutable, state, UPDATE_FLAGS);
                    filled++;
                    changed = true;
                }
                used++;
            }
            return used;
        }

        private void fillSection(MapOperation part, BlockState state) {
            int sectionIndex = chunk.getSectionIndex(part.y());
            LevelChunkSection section = chunk.getSection(sectionIndex);
            if (!section.maybeHas(existing -> !existing.equals(state))) { skipped += part.volume(); return; }
            clearBlockEntities(part);
            boolean wasEmpty = section.hasOnlyAir();
            if (part.sizeX() == 16 && part.sizeY() == 16 && part.sizeZ() == 16) {
                // A single-entry palette avoids setting 4096 identical blocks and preserves biome data.
                chunk.getSections()[sectionIndex] = new LevelChunkSection(new PalettedContainer<>(
                        Block.BLOCK_STATE_REGISTRY, state, PalettedContainer.Strategy.SECTION_STATES), section.getBiomes());
                filled += 4096;
            } else {
                for (int x = 0; x < part.sizeX(); x++) for (int y = 0; y < part.sizeY(); y++) for (int z = 0; z < part.sizeZ(); z++) {
                    int lx = (part.x() & 15) + x, ly = (part.y() & 15) + y, lz = (part.z() & 15) + z;
                    if (section.getBlockState(lx, ly, lz).equals(state)) skipped++;
                    else { section.setBlockState(lx, ly, lz, state, false); filled++; }
                }
            }
            sections++;
            changed = true;
            chunk.setUnsaved(true);
            boolean isEmpty = chunk.getSection(sectionIndex).hasOnlyAir();
            if (wasEmpty != isEmpty)
                level.getLightEngine().updateSectionStatus(SectionPos.of(pos, chunk.getSectionYFromSectionIndex(sectionIndex)), isEmpty);
        }

        private void clearBlockEntities(MapOperation part) {
            if (chunk.getBlockEntities().isEmpty()) return;
            for (BlockPos position : List.copyOf(chunk.getBlockEntities().keySet())) {
                if (position.getX() >= part.x() && position.getX() < part.endX()
                        && position.getY() >= part.y() && position.getY() < part.endY()
                        && position.getZ() >= part.z() && position.getZ() < part.endZ()) level.removeBlockEntity(position);
            }
        }

        private void finishChanges() {
            if (lighting != null || !changed) return;
            // Reset first: a completely excavated column has no opaque block to overwrite an old height.
            for (Heightmap.Types type : HEIGHTMAPS)
                Arrays.fill(chunk.getOrCreateHeightmapUnprimed(type).getRawData(), 0L);
            Heightmap.primeHeightmaps(chunk, HEIGHTMAPS);
            chunk.initializeLightSources();
            chunk.setUnsaved(true);
            lighting = level.getChunkSource().getLightEngine().lightChunk(chunk, false);
        }

        boolean finished() { return exhausted && (lighting == null || lighting.isDone()); }

        private void sendPacket() {
            if (packetSent || !changed) return;
            packetSent = true;
            List<ServerPlayer> players = level.getChunkSource().chunkMap.getPlayers(pos, false);
            if (!players.isEmpty()) {
                ClientboundLevelChunkWithLightPacket packet = new ClientboundLevelChunkWithLightPacket(chunk, level.getLightEngine(), null, null);
                for (ServerPlayer player : players) player.connection.send(packet);
            }
        }

        private void releaseTicket() {
            if (!ticketReleased) {
                ticketReleased = true;
                level.getChunkSource().removeRegionTicket(TICKET, pos, 0, pos);
            }
        }

        @Override public void close() {
            try {
                finishChanges();
                if (lighting != null && !lighting.isDone()) {
                    lighting.whenComplete((ignored, error) -> level.getServer().execute(() -> {
                        try { if (error == null) sendPacket(); }
                        finally { releaseTicket(); }
                    }));
                } else {
                    sendPacket();
                    releaseTicket();
                }
            } catch (Exception error) {
                releaseTicket();
                AsmpThingsMod.LOGGER.error("Impossible de finaliser le chunk de carte {}", pos, error);
            }
        }
    }
}
