package net.maximlvr.asmpthings.command;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.maximlvr.asmpthings.AsmpThingsMod;
import net.maximlvr.asmpthings.world.ModDimensions;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@EventBusSubscriber(modid = AsmpThingsMod.MOD_ID)
public final class MapBuildPlacementCommand {
    private static final ResourceLocation ZONE_RESOURCE = ResourceLocation.fromNamespaceAndPath(
            AsmpThingsMod.MOD_ID,
            "msmp/map_generation_final_seed42_10000x10000.json");
    private static final int DEFAULT_WORK_BUDGET_PER_TICK = 16384;
    private static final int MAX_WORK_BUDGET_PER_TICK = 65536;
    private static final int TEMPLATE_WORK_COST = 64;
    private static final int SECTION_FILL_COST = 1;
    private static final int DIRECT_BLOCK_FILL_COST_DIVISOR = 64;
    private static final int BULK_CHUNK_REFRESHES_PER_TICK = 48;
    private static final int DEFAULT_LAZY_RADIUS_CHUNKS = 5;
    private static final int MAX_LAZY_RADIUS_CHUNKS = 16;
    private static final int MAX_CHUNKS_PER_SECOND = 100;
    private static final int BLOCK_FILL_UPDATE_FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SUPPRESS_DROPS;
    private static PlacementTask activeTask;
    private static LazyPlacementTask activeLazyTask;

    private MapBuildPlacementCommand() {
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        activeTask = null;
        activeLazyTask = null;
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("asmpbuildzone")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("start")
                        .executes(context -> start(context, DEFAULT_WORK_BUDGET_PER_TICK))
                        .then(Commands.argument("budgetParTick", IntegerArgumentType.integer(1, MAX_WORK_BUDGET_PER_TICK))
                                .executes(context -> start(context, IntegerArgumentType.getInteger(context, "budgetParTick")))))
                .then(Commands.literal("chunks")
                        .then(Commands.literal("start")
                                .then(Commands.argument("chunksParSeconde", IntegerArgumentType.integer(1, MAX_CHUNKS_PER_SECOND))
                                        .executes(context -> startLazy(context, 0,
                                                DEFAULT_WORK_BUDGET_PER_TICK, IntegerArgumentType.getInteger(context, "chunksParSeconde")))))
                        .then(Commands.literal("status")
                                .executes(MapBuildPlacementCommand::status))
                        .then(Commands.literal("stop")
                                .executes(MapBuildPlacementCommand::stop)))
                .then(Commands.literal("lazy")
                        .then(Commands.literal("start")
                                .executes(context -> startLazy(context, DEFAULT_LAZY_RADIUS_CHUNKS, DEFAULT_WORK_BUDGET_PER_TICK))
                                .then(Commands.argument("rayonChunks", IntegerArgumentType.integer(0, MAX_LAZY_RADIUS_CHUNKS))
                                        .executes(context -> startLazy(
                                                context,
                                                IntegerArgumentType.getInteger(context, "rayonChunks"),
                                                DEFAULT_WORK_BUDGET_PER_TICK
                                        ))
                                        .then(Commands.argument("budgetParTick", IntegerArgumentType.integer(1, MAX_WORK_BUDGET_PER_TICK))
                                                .executes(context -> startLazy(
                                                        context,
                                                        IntegerArgumentType.getInteger(context, "rayonChunks"),
                                                        IntegerArgumentType.getInteger(context, "budgetParTick")
                                                )))))
                        .then(Commands.literal("status")
                                .executes(MapBuildPlacementCommand::status))
                        .then(Commands.literal("stop")
                                .executes(MapBuildPlacementCommand::stop)))
                .then(Commands.literal("status")
                        .executes(MapBuildPlacementCommand::status))
                .then(Commands.literal("stop")
                        .executes(MapBuildPlacementCommand::stop)));
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (activeTask == null) {
            if (activeLazyTask == null) {
                return;
            }
        }

        if (activeTask != null) {
            activeTask.tick();

            if (activeTask.isFinished()) {
                activeTask.sendFinishedMessage();
                activeTask = null;
            }
        }

        if (activeLazyTask == null) {
            return;
        }

        activeLazyTask.tick();

        if (activeLazyTask.isFinished()) {
            activeLazyTask.sendFinishedMessage();
            activeLazyTask = null;
        }
    }

    private static int start(CommandContext<CommandSourceStack> context, int workBudgetPerTick) {
        CommandSourceStack source = context.getSource();

        if (activeTask != null || activeLazyTask != null) {
            source.sendFailure(Component.literal("Une pose de zone est deja en cours. Utilise /asmpbuildzone status ou /asmpbuildzone stop."));
            return 0;
        }

        MinecraftServer server = source.getServer();
        ServerLevel level = server.getLevel(ModDimensions.ASMP_DIMENSION_KEY);

        if (level == null) {
            source.sendFailure(Component.literal("Dimension introuvable: " + ModDimensions.ASMP_DIMENSION_KEY.location()));
            return 0;
        }

        List<Placement> placements;

        try {
            placements = loadPlacements(server);
        } catch (Exception exception) {
            source.sendFailure(Component.literal("Impossible de lire " + ZONE_RESOURCE + ": " + exception.getMessage()));
            return 0;
        }

        if (placements.isEmpty()) {
            source.sendFailure(Component.literal("Aucun placement trouve dans " + ZONE_RESOURCE + "."));
            return 0;
        }

        List<String> missingTemplates = findMissingTemplates(server.getStructureManager(), placements);

        if (!missingTemplates.isEmpty()) {
            source.sendFailure(Component.literal("Template(s) manquant(s): " + String.join(", ", missingTemplates)));
            return 0;
        }

        List<String> missingBlocks = findMissingBlocks(server, placements);

        if (!missingBlocks.isEmpty()) {
            source.sendFailure(Component.literal("Bloc(s) invalide(s): " + String.join(", ", missingBlocks)));
            return 0;
        }

        activeTask = new PlacementTask(source, level, placements, workBudgetPerTick);
        source.sendSuccess(() -> Component.literal(
                "Pose demarree dans " + ModDimensions.ASMP_DIMENSION_KEY.location()
                        + ": " + placements.size() + " placements, budget " + workBudgetPerTick + " blocs/tick."
        ), true);
        return 1;
    }

    private static int startLazy(CommandContext<CommandSourceStack> context, int radiusChunks, int workBudgetPerTick) {
        return startLazy(context, radiusChunks, workBudgetPerTick, 0);
    }

    private static int startLazy(CommandContext<CommandSourceStack> context, int radiusChunks, int workBudgetPerTick, int chunksPerSecond) {
        CommandSourceStack source = context.getSource();

        if (activeTask != null || activeLazyTask != null) {
            source.sendFailure(Component.literal("Une pose de zone est deja en cours. Utilise /asmpbuildzone status ou /asmpbuildzone stop."));
            return 0;
        }

        MinecraftServer server = source.getServer();
        ServerLevel level = server.getLevel(ModDimensions.ASMP_DIMENSION_KEY);

        if (level == null) {
            source.sendFailure(Component.literal("Dimension introuvable: " + ModDimensions.ASMP_DIMENSION_KEY.location()));
            return 0;
        }

        List<Placement> placements;

        try {
            placements = loadPlacements(server);
        } catch (Exception exception) {
            source.sendFailure(Component.literal("Impossible de lire " + ZONE_RESOURCE + ": " + exception.getMessage()));
            return 0;
        }

        if (placements.isEmpty()) {
            source.sendFailure(Component.literal("Aucun placement trouve dans " + ZONE_RESOURCE + "."));
            return 0;
        }

        List<String> missingTemplates = findMissingTemplates(server.getStructureManager(), placements);

        if (!missingTemplates.isEmpty()) {
            source.sendFailure(Component.literal("Template(s) manquant(s): " + String.join(", ", missingTemplates)));
            return 0;
        }

        List<String> missingBlocks = findMissingBlocks(server, placements);

        if (!missingBlocks.isEmpty()) {
            source.sendFailure(Component.literal("Bloc(s) invalide(s): " + String.join(", ", missingBlocks)));
            return 0;
        }

        activeLazyTask = new LazyPlacementTask(source, level, placements, workBudgetPerTick, radiusChunks, chunksPerSecond);
        source.sendSuccess(() -> Component.literal(
                "Generation " + (chunksPerSecond > 0 ? "chunk par chunk" : "lazy") + " demarree dans " + ModDimensions.ASMP_DIMENSION_KEY.location()
                        + ": " + activeLazyTask.totalChunks() + " chunks avec placements"
                        + (chunksPerSecond > 0 ? ", toute la map" : ", rayon " + radiusChunks)
                        + ", budget " + workBudgetPerTick + " blocs/tick."
                        + (chunksPerSecond > 0 ? " Cadence cible: " + chunksPerSecond + " chunks/seconde (a 20 TPS)." : "")
        ), true);
        return 1;
    }

    private static int status(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();

        if (activeTask == null && activeLazyTask == null) {
            source.sendSuccess(() -> Component.literal("Aucune pose de zone en cours."), false);
            return 1;
        }

        String status = activeTask != null ? activeTask.getStatusLine() : activeLazyTask.getStatusLine();
        source.sendSuccess(() -> Component.literal(status), false);
        return 1;
    }

    private static int stop(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();

        if (activeTask == null && activeLazyTask == null) {
            source.sendSuccess(() -> Component.literal("Aucune pose de zone en cours."), false);
            return 1;
        }

        String status = activeTask != null ? activeTask.getStatusLine() : activeLazyTask.getStatusLine();
        if (activeTask != null) {
            activeTask.flushBulkChunkRefreshes(Integer.MAX_VALUE);
        }
        if (activeLazyTask != null && activeLazyTask.currentChunkTask != null) {
            activeLazyTask.currentChunkTask.flushBulkChunkRefreshes(Integer.MAX_VALUE);
        }
        activeTask = null;
        activeLazyTask = null;
        source.sendSuccess(() -> Component.literal("Pose arretee. " + status), true);
        return 1;
    }

    private static List<Placement> loadPlacements(MinecraftServer server) throws IOException {
        Resource resource = server.getResourceManager()
                .getResource(ZONE_RESOURCE)
                .orElseThrow(() -> new IOException("ressource introuvable"));
        List<Placement> placements = new ArrayList<>();

        try (BufferedReader reader = resource.openAsReader()) {
            JsonElement root = JsonParser.parseReader(reader);

            if (root.isJsonObject()) {
                JsonObject rootObject = root.getAsJsonObject();
                JsonArray runs = rootObject.getAsJsonArray("runs");

                if (runs != null) {
                    collectPlacementArray(runs, placements);
                    return placements;
                }

                JsonArray placementArray = rootObject.getAsJsonArray("placements");

                if (placementArray != null) {
                    collectPlacementArray(placementArray, placements);
                    return placements;
                }
            }

            collectPlacements(root, placements);
        }

        return placements;
    }

    private static void collectPlacementArray(JsonArray placementArray, List<Placement> placements) {
        for (JsonElement element : placementArray) {
            if (!element.isJsonObject()) {
                continue;
            }

            Placement placement = parsePlacement(element.getAsJsonObject());

            if (placement != null) {
                addPlacement(placement, placements);
            }
        }
    }

    private static void addPlacement(Placement placement, List<Placement> placements) {
        if (placement.mode != PlacementMode.BLOCK_FILL || placement.fitsSingleSection()) {
            placements.add(placement);
            return;
        }

        splitBlockFillBySections(placement, placements);
    }

    private static void splitBlockFillBySections(Placement placement, List<Placement> placements) {
        int endX = placement.pos.getX() + placement.size.x;
        int endY = placement.pos.getY() + placement.size.y;
        int endZ = placement.pos.getZ() + placement.size.z;

        for (int x = placement.pos.getX(); x < endX; ) {
            int nextX = Math.min(endX, nextSectionBoundary(x));
            int sizeX = nextX - x;

            for (int y = placement.pos.getY(); y < endY; ) {
                int nextY = Math.min(endY, nextSectionBoundary(y));
                int sizeY = nextY - y;

                for (int z = placement.pos.getZ(); z < endZ; ) {
                    int nextZ = Math.min(endZ, nextSectionBoundary(z));
                    int sizeZ = nextZ - z;

                    placements.add(Placement.blockFill(
                            placement.kind,
                            placement.blockStateName,
                            new BlockPos(x, y, z),
                            new Size(sizeX, sizeY, sizeZ)
                    ));
                    z = nextZ;
                }

                y = nextY;
            }

            x = nextX;
        }
    }

    private static int nextSectionBoundary(int coordinate) {
        return SectionPos.sectionToBlockCoord(SectionPos.blockToSectionCoord(coordinate) + 1);
    }

    private static void collectPlacements(JsonElement element, List<Placement> placements) {
        if (element == null || element.isJsonNull()) {
            return;
        }

        if (element.isJsonArray()) {
            for (JsonElement child : element.getAsJsonArray()) {
                collectPlacements(child, placements);
            }

            return;
        }

        if (!element.isJsonObject()) {
            return;
        }

        JsonObject object = element.getAsJsonObject();
        Placement placement = parsePlacement(object);

        if (placement != null) {
            addPlacement(placement, placements);
            return;
        }

        for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
            collectPlacements(entry.getValue(), placements);
        }
    }

    private static Placement parsePlacement(JsonObject object) {
        if (!object.has("pos")) {
            return null;
        }

        JsonArray pos = object.getAsJsonArray("pos");

        if (pos == null || pos.size() < 3) {
            return null;
        }

        String kind = object.has("kind") ? object.get("kind").getAsString() : "structure";
        BlockPos blockPos = new BlockPos(pos.get(0).getAsInt(), pos.get(1).getAsInt(), pos.get(2).getAsInt());

        if (object.has("block") || kind.equals("stone_run")) {
            String blockName = object.has("block")
                    ? object.get("block").getAsString()
                    : object.has("name") ? object.get("name").getAsString() : "";

            if (blockName.isBlank()) {
                return null;
            }

            if (kind.equals("base") && blockName.equals("minecraft:stone")) {
                return null;
            }

            return Placement.blockFill(kind, blockName, blockPos, readSize(object));
        }

        if (!object.has("name")) {
            return null;
        }

        String name = object.get("name").getAsString();
        int rotationY = object.has("rotation_y") ? object.get("rotation_y").getAsInt() : 0;
        return new Placement(
                PlacementMode.TEMPLATE,
                structureId(name),
                null,
                kind,
                blockPos,
                readSize(object),
                rotationFromY(rotationY)
        );
    }

    private static long chunkKey(int chunkX, int chunkZ) {
        return ChunkPos.asLong(chunkX, chunkZ);
    }

    private static int chunkX(long chunkKey) {
        return ChunkPos.getX(chunkKey);
    }

    private static int chunkZ(long chunkKey) {
        return ChunkPos.getZ(chunkKey);
    }

    private static Size readSize(JsonObject object) {
        JsonArray size = object.getAsJsonArray("size");

        if (size == null || size.size() < 3) {
            return Size.ONE;
        }

        return new Size(
                Math.max(1, size.get(0).getAsInt()),
                Math.max(1, size.get(1).getAsInt()),
                Math.max(1, size.get(2).getAsInt())
        );
    }

    private static ResourceLocation structureId(String name) {
        ResourceLocation parsed = ResourceLocation.tryParse(name);

        if (parsed != null && name.contains(":")) {
            return parsed;
        }

        return ResourceLocation.fromNamespaceAndPath(AsmpThingsMod.MOD_ID, name);
    }

    private static Rotation rotationFromY(int rotationY) {
        int normalized = Math.floorMod(rotationY, 360);

        return switch (normalized) {
            case 90 -> Rotation.CLOCKWISE_90;
            case 180 -> Rotation.CLOCKWISE_180;
            case 270 -> Rotation.COUNTERCLOCKWISE_90;
            default -> Rotation.NONE;
        };
    }

    private static List<String> findMissingTemplates(StructureTemplateManager manager, List<Placement> placements) {
        Set<ResourceLocation> checked = new HashSet<>();
        List<String> missing = new ArrayList<>();

        for (Placement placement : placements) {
            if (placement.mode != PlacementMode.TEMPLATE) {
                continue;
            }

            if (!checked.add(placement.structureId)) {
                continue;
            }

            if (manager.get(placement.structureId).isEmpty()) {
                missing.add(placement.structureId.toString());
            }
        }

        return missing;
    }

    private static List<String> findMissingBlocks(MinecraftServer server, List<Placement> placements) {
        HolderLookup<Block> blockLookup = server.registryAccess().lookupOrThrow(Registries.BLOCK);
        Set<String> checked = new HashSet<>();
        List<String> missing = new ArrayList<>();

        for (Placement placement : placements) {
            if (placement.mode != PlacementMode.BLOCK_FILL) {
                continue;
            }

            if (!checked.add(placement.blockStateName)) {
                continue;
            }

            try {
                BlockStateParser.parseForBlock(blockLookup, placement.blockStateName, false);
            } catch (CommandSyntaxException exception) {
                missing.add(placement.blockStateName);
            }
        }

        return missing;
    }

    private enum PlacementMode {
        TEMPLATE,
        BLOCK_FILL
    }

    private record Size(int x, int y, int z) {
        private static final Size ONE = new Size(1, 1, 1);
    }

    private record Placement(
            PlacementMode mode,
            ResourceLocation structureId,
            String blockStateName,
            String kind,
            BlockPos pos,
            Size size,
            Rotation rotation
    ) {
        private static Placement blockFill(String kind, String blockStateName, BlockPos pos, Size size) {
            return new Placement(PlacementMode.BLOCK_FILL, null, blockStateName, kind, pos, size, Rotation.NONE);
        }

        private long volume() {
            return (long) size.x * size.y * size.z;
        }

        private boolean fitsSingleSection() {
            return SectionPos.blockToSectionCoord(pos.getX()) == SectionPos.blockToSectionCoord(pos.getX() + size.x - 1)
                    && SectionPos.blockToSectionCoord(pos.getY()) == SectionPos.blockToSectionCoord(pos.getY() + size.y - 1)
                    && SectionPos.blockToSectionCoord(pos.getZ()) == SectionPos.blockToSectionCoord(pos.getZ() + size.z - 1);
        }

        private boolean isFullSection() {
            return fitsSingleSection()
                    && (pos.getX() & 15) == 0
                    && (pos.getY() & 15) == 0
                    && (pos.getZ() & 15) == 0
                    && size.x == 16
                    && size.y == 16
                    && size.z == 16;
        }
    }

    private static final class PlacementTask {
        private final CommandSourceStack source;
        private final ServerLevel level;
        private final List<Placement> placements;
        private final int workBudgetPerTick;
        private final Map<ResourceLocation, Optional<StructureTemplate>> templateCache = new HashMap<>();
        private final HolderLookup<Block> blockLookup;
        private final Registry<Biome> biomeRegistry;
        private final Map<String, Optional<BlockState>> blockStateCache = new HashMap<>();
        private final Set<Long> loadedChunks = new HashSet<>();
        private final Set<Long> dirtyBulkChunks = new HashSet<>();
        private int index = 0;
        private int placed = 0;
        private int failed = 0;
        private long blocksFilled = 0;
        private long blocksSkipped = 0;
        private long sectionFills = 0;
        private long refreshedBulkChunks = 0;
        private long currentBlockOffset = 0;
        private int tickCounter = 0;

        private PlacementTask(CommandSourceStack source, ServerLevel level, List<Placement> placements, int workBudgetPerTick) {
            this.source = source;
            this.level = level;
            this.placements = placements;
            this.workBudgetPerTick = workBudgetPerTick;
            this.blockLookup = source.getServer().registryAccess().lookupOrThrow(Registries.BLOCK);
            this.biomeRegistry = level.registryAccess().registryOrThrow(Registries.BIOME);
        }

        private void tick() {
            runBudget(workBudgetPerTick);
            tickCounter++;
            flushBulkChunkRefreshes(BULK_CHUNK_REFRESHES_PER_TICK);

            if (!isFinished() && tickCounter % 20 == 0) {
                source.sendSuccess(() -> Component.literal(getStatusLine()), false);
            }
        }

        private int runBudget(int budget) {
            int usedBudget = 0;

            while (index < placements.size() && usedBudget < budget) {
                Placement placement = placements.get(index);

                if (placement.mode == PlacementMode.BLOCK_FILL) {
                    usedBudget += placeBlockFill(placement, budget - usedBudget);
                    continue;
                }

                if (usedBudget > 0 && usedBudget + TEMPLATE_WORK_COST > budget) {
                    break;
                }

                place(placement);
                index++;
                currentBlockOffset = 0;
                usedBudget += TEMPLATE_WORK_COST;
            }

            return usedBudget;
        }

        private void place(Placement placement) {
            Optional<StructureTemplate> template = templateCache.computeIfAbsent(
                    placement.structureId,
                    id -> level.getServer().getStructureManager().get(id)
            );

            if (template.isEmpty()) {
                failed++;
                return;
            }

            StructurePlaceSettings settings = new StructurePlaceSettings()
                    .setMirror(Mirror.NONE)
                    .setRotation(placement.rotation)
                    .setIgnoreEntities(false)
                    .setFinalizeEntities(true);

            ensureChunksLoaded(placement);
            boolean success = template.get().placeInWorld(
                    level,
                    placement.pos,
                    placement.pos,
                    settings,
                    level.getRandom(),
                    Block.UPDATE_ALL
            );

            if (success) {
                placed++;
            } else {
                failed++;
            }
        }

        private int placeBlockFill(Placement placement, int budget) {
            Optional<BlockState> optionalState = blockStateCache.computeIfAbsent(
                    placement.blockStateName,
                    this::parseBlockState
            );

            if (optionalState.isEmpty()) {
                failed++;
                index++;
                currentBlockOffset = 0;
                return 1;
            }

            BlockState state = optionalState.get();

            if (canUseDirectSectionFill(placement, state)) {
                return placeDirectSectionFill(placement, state);
            }

            BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
            long volume = placement.volume();
            int used = 0;

            while (used < budget && currentBlockOffset < volume) {
                long offset = currentBlockOffset;
                int dz = (int) (offset % placement.size.z);
                long yOffset = offset / placement.size.z;
                int dy = (int) (yOffset % placement.size.y);
                int dx = (int) (yOffset / placement.size.y);

                mutable.set(placement.pos.getX() + dx, placement.pos.getY() + dy, placement.pos.getZ() + dz);
                ensureChunkLoaded(mutable);

                if (level.getBlockState(mutable).equals(state)) {
                    blocksSkipped++;
                    currentBlockOffset++;
                    used++;
                    continue;
                }

                level.setBlock(mutable, state, BLOCK_FILL_UPDATE_FLAGS);

                currentBlockOffset++;
                blocksFilled++;
                used++;
            }

            if (currentBlockOffset >= volume) {
                placed++;
                index++;
                currentBlockOffset = 0;
            }

            return used;
        }

        private boolean canUseDirectSectionFill(Placement placement, BlockState state) {
            return placement.fitsSingleSection()
                    && state.getLightEmission() == 0
                    && state.getFluidState().isEmpty()
                    && !state.hasBlockEntity();
        }

        private int placeDirectSectionFill(Placement placement, BlockState state) {
            LevelChunk chunk = level.getChunk(placement.pos.getX() >> 4, placement.pos.getZ() >> 4);
            int sectionIndex = chunk.getSectionIndex(placement.pos.getY());

            if (sectionIndex < 0 || sectionIndex >= chunk.getSections().length) {
                failed++;
                index++;
                currentBlockOffset = 0;
                return SECTION_FILL_COST;
            }

            if (placement.isFullSection()) {
                placeFullSection(chunk, sectionIndex, state);
            } else {
                placePartialSection(chunk, sectionIndex, placement, state);
            }

            placed++;
            index++;
            currentBlockOffset = 0;
            return placement.isFullSection() ? SECTION_FILL_COST : Math.max(SECTION_FILL_COST, (int) (placement.volume() / DIRECT_BLOCK_FILL_COST_DIVISOR));
        }

        private void placeFullSection(LevelChunk chunk, int sectionIndex, BlockState state) {
            LevelChunkSection currentSection = chunk.getSection(sectionIndex);

            if (state.isAir() && currentSection.hasOnlyAir()) {
                blocksSkipped += LevelChunkSection.SECTION_SIZE;
                return;
            }

            chunk.getSections()[sectionIndex] = createFilledSection(state);
            chunk.setUnsaved(true);
            sectionFills++;
            blocksFilled += LevelChunkSection.SECTION_SIZE;
            markBulkChunkChanged(chunk);
            updateSectionStatus(chunk, sectionIndex, state.isAir());
        }

        private LevelChunkSection createFilledSection(BlockState state) {
            LevelChunkSection section = new LevelChunkSection(biomeRegistry);

            if (state.isAir()) {
                return section;
            }

            for (int x = 0; x < 16; x++) {
                for (int y = 0; y < 16; y++) {
                    for (int z = 0; z < 16; z++) {
                        section.setBlockState(x, y, z, state, false);
                    }
                }
            }

            return section;
        }

        private void placePartialSection(LevelChunk chunk, int sectionIndex, Placement placement, BlockState state) {
            LevelChunkSection section = chunk.getSection(sectionIndex);
            int localX = placement.pos.getX() & 15;
            int localY = placement.pos.getY() & 15;
            int localZ = placement.pos.getZ() & 15;
            boolean wasEmpty = section.hasOnlyAir();
            int changed = 0;
            int skipped = 0;

            for (int dx = 0; dx < placement.size.x; dx++) {
                for (int dy = 0; dy < placement.size.y; dy++) {
                    for (int dz = 0; dz < placement.size.z; dz++) {
                        int x = localX + dx;
                        int y = localY + dy;
                        int z = localZ + dz;

                        if (section.getBlockState(x, y, z).equals(state)) {
                            skipped++;
                            continue;
                        }

                        section.setBlockState(x, y, z, state, false);
                        changed++;
                    }
                }
            }

            if (changed > 0) {
                chunk.setUnsaved(true);
                blocksFilled += changed;
                markBulkChunkChanged(chunk);

                boolean isEmpty = section.hasOnlyAir();

                if (wasEmpty != isEmpty) {
                    updateSectionStatus(chunk, sectionIndex, isEmpty);
                }
            }

            blocksSkipped += skipped;
        }

        private void markBulkChunkChanged(LevelChunk chunk) {
            dirtyBulkChunks.add(chunk.getPos().toLong());
        }

        private void updateSectionStatus(LevelChunk chunk, int sectionIndex, boolean isEmpty) {
            int sectionY = chunk.getSectionYFromSectionIndex(sectionIndex);
            level.getChunkSource().getLightEngine().updateSectionStatus(SectionPos.of(chunk.getPos(), sectionY), isEmpty);
        }

        private void flushBulkChunkRefreshes(int maxChunks) {
            if (dirtyBulkChunks.isEmpty()) {
                return;
            }

            Iterator<Long> iterator = dirtyBulkChunks.iterator();
            int sent = 0;

            while (iterator.hasNext() && sent < maxChunks) {
                long chunkKey = iterator.next();
                iterator.remove();
                int chunkX = chunkX(chunkKey);
                int chunkZ = chunkZ(chunkKey);
                LevelChunk chunk = level.getChunk(chunkX, chunkZ);
                List<ServerPlayer> players = level.getChunkSource().chunkMap.getPlayers(new ChunkPos(chunkX, chunkZ), false);

                if (!players.isEmpty()) {
                    ClientboundLevelChunkWithLightPacket packet = new ClientboundLevelChunkWithLightPacket(chunk, level.getLightEngine(), null, null);

                    for (ServerPlayer player : players) {
                        player.connection.send(packet);
                    }

                    refreshedBulkChunks++;
                }

                sent++;
            }
        }

        private Optional<BlockState> parseBlockState(String blockStateName) {
            try {
                return Optional.of(BlockStateParser.parseForBlock(blockLookup, blockStateName, false).blockState());
            } catch (CommandSyntaxException exception) {
                return Optional.empty();
            }
        }

        private void ensureChunksLoaded(Placement placement) {
            int minChunkX = placement.pos.getX() >> 4;
            int minChunkZ = placement.pos.getZ() >> 4;
            int maxChunkX = (placement.pos.getX() + placement.size.x - 1) >> 4;
            int maxChunkZ = (placement.pos.getZ() + placement.size.z - 1) >> 4;

            for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
                for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                    ensureChunkLoaded(chunkX, chunkZ);
                }
            }
        }

        private void ensureChunkLoaded(BlockPos pos) {
            ensureChunkLoaded(pos.getX() >> 4, pos.getZ() >> 4);
        }

        private void ensureChunkLoaded(int chunkX, int chunkZ) {
            long key = chunkKey(chunkX, chunkZ);

            if (loadedChunks.add(key)) {
                level.getChunk(chunkX, chunkZ);
            }
        }

        private boolean isFinished() {
            return index >= placements.size();
        }

        private String getStatusLine() {
            String currentPlacement = "";

            if (index < placements.size()) {
                Placement placement = placements.get(index);

                if (placement.mode == PlacementMode.BLOCK_FILL && currentBlockOffset > 0) {
                    currentPlacement = ", run en cours: " + currentBlockOffset + "/" + placement.volume();
                }
            }

            return "Progression placements: " + index + "/" + placements.size()
                    + ", poses OK: " + placed
                    + ", echecs: " + failed
                    + ", blocs modifies: " + blocksFilled
                    + ", blocs deja OK: " + blocksSkipped
                    + ", sections bulk: " + sectionFills
                    + ", chunks refresh: " + refreshedBulkChunks
                    + currentPlacement
                    + ", dimension: " + level.dimension().location();
        }

        private void sendFinishedMessage() {
            flushBulkChunkRefreshes(Integer.MAX_VALUE);
            source.sendSuccess(() -> Component.literal("Pose terminee. " + getStatusLine()), true);
        }
    }

    private static final class LazyPlacementTask {
        private final CommandSourceStack source;
        private final ServerLevel level;
        private final Map<Long, List<Placement>> placementsByChunk = new LinkedHashMap<>();
        private final Deque<Long> chunkQueue = new ArrayDeque<>();
        private final Set<Long> queuedChunks = new HashSet<>();
        private final Set<Long> completedChunks = new HashSet<>();
        private final int workBudgetPerTick;
        private final int radiusChunks;
        private final int chunksPerSecond;
        private int chunkStartCredits = 0;
        private PlacementTask currentChunkTask;
        private long currentChunkKey;
        private ChunkPos lastCompletedChunk;
        private int tickCounter = 0;
        private long chunksStarted = 0;
        private long chunksFinished = 0;

        private LazyPlacementTask(CommandSourceStack source, ServerLevel level, List<Placement> placements, int workBudgetPerTick, int radiusChunks, int chunksPerSecond) {
            this.source = source;
            this.level = level;
            this.workBudgetPerTick = workBudgetPerTick;
            this.radiusChunks = radiusChunks;
            this.chunksPerSecond = chunksPerSecond;
            buildChunkIndex(placements);
            if (chunksPerSecond > 0) {
                List<Long> sortedChunks = new ArrayList<>(placementsByChunk.keySet());
                // North to south (Z), west to east (X) within each row.
                sortedChunks.sort(Comparator.comparingInt((Long key) -> chunkZ(key))
                        .thenComparingInt(key -> chunkX(key)));
                chunkQueue.addAll(sortedChunks);
                queuedChunks.addAll(placementsByChunk.keySet());
            }
        }

        private void buildChunkIndex(List<Placement> placements) {
            for (Placement placement : placements) {
                int chunkX = placement.pos.getX() >> 4;
                int chunkZ = placement.pos.getZ() >> 4;

                placementsByChunk.computeIfAbsent(chunkKey(chunkX, chunkZ), key -> new ArrayList<>()).add(placement);
            }
        }

        private int totalChunks() {
            return placementsByChunk.size();
        }

        private void tick() {
            if (chunksPerSecond == 0) {
                queueChunksAroundPlayers();
            }
            // Twenty credits start one chunk; cap savings to avoid a burst after waiting.
            chunkStartCredits = Math.min(chunkStartCredits + chunksPerSecond, 19 + chunksPerSecond);
            int usedBudget = 0;

            while (usedBudget < workBudgetPerTick) {
                if (currentChunkTask == null) {
                    if (chunksPerSecond > 0 && chunkStartCredits < 20) {
                        break;
                    }
                    if (!startNextChunk()) {
                        break;
                    }
                    if (chunksPerSecond > 0) {
                        chunkStartCredits -= 20;
                    }
                }

                int used = currentChunkTask.runBudget(workBudgetPerTick - usedBudget);
                currentChunkTask.flushBulkChunkRefreshes(BULK_CHUNK_REFRESHES_PER_TICK);
                usedBudget += Math.max(1, used);

                if (!currentChunkTask.isFinished()) {
                    break;
                }

                currentChunkTask.flushBulkChunkRefreshes(Integer.MAX_VALUE);
                completedChunks.add(currentChunkKey);
                lastCompletedChunk = new ChunkPos(currentChunkKey);
                chunksFinished++;
                currentChunkTask = null;
            }

            tickCounter++;

            if (!isFinished() && tickCounter % 20 == 0) {
                source.sendSuccess(() -> Component.literal(getStatusLine()), false);
            }
        }

        private void queueChunksAroundPlayers() {
            for (ServerPlayer player : level.players()) {
                BlockPos playerPos = player.blockPosition();
                int centerChunkX = playerPos.getX() >> 4;
                int centerChunkZ = playerPos.getZ() >> 4;

                for (int distance = 0; distance <= radiusChunks; distance++) {
                    queueChunkRing(centerChunkX, centerChunkZ, distance);
                }
            }
        }

        private void queueChunkRing(int centerChunkX, int centerChunkZ, int distance) {
            if (distance == 0) {
                queueChunk(centerChunkX, centerChunkZ);
                return;
            }

            for (int dx = -distance; dx <= distance; dx++) {
                queueChunk(centerChunkX + dx, centerChunkZ - distance);
                queueChunk(centerChunkX + dx, centerChunkZ + distance);
            }

            for (int dz = -distance + 1; dz <= distance - 1; dz++) {
                queueChunk(centerChunkX - distance, centerChunkZ + dz);
                queueChunk(centerChunkX + distance, centerChunkZ + dz);
            }
        }

        private void queueChunk(int chunkX, int chunkZ) {
            long key = chunkKey(chunkX, chunkZ);

            if (completedChunks.contains(key) || queuedChunks.contains(key)) {
                return;
            }

            if (currentChunkTask != null && currentChunkKey == key) {
                return;
            }

            if (!placementsByChunk.containsKey(key)) {
                return;
            }

            chunkQueue.addLast(key);
            queuedChunks.add(key);
        }

        private boolean startNextChunk() {
            while (!chunkQueue.isEmpty()) {
                long key = chunkQueue.removeFirst();
                queuedChunks.remove(key);
                List<Placement> placements = placementsByChunk.get(key);

                if (placements == null || placements.isEmpty()) {
                    completedChunks.add(key);
                    chunksFinished++;
                    continue;
                }

                currentChunkKey = key;
                currentChunkTask = new PlacementTask(source, level, placements, workBudgetPerTick);
                chunksStarted++;
                return true;
            }

            return false;
        }

        private boolean isFinished() {
            return !placementsByChunk.isEmpty() && completedChunks.size() >= placementsByChunk.size();
        }

        private String getStatusLine() {
            String current = "";

            if (currentChunkTask != null) {
                current = ", chunk en cours: " + chunkX(currentChunkKey) + " " + chunkZ(currentChunkKey)
                        + " (" + currentChunkTask.index + "/" + currentChunkTask.placements.size() + " placements)";
            }

            return "Generation " + (chunksPerSecond > 0 ? "chunk par chunk" : "lazy") + ": chunks faits " + completedChunks.size() + "/" + placementsByChunk.size()
                    + ", chunks demarres: " + chunksStarted
                    + ", chunks termines: " + chunksFinished
                    + ", file: " + chunkQueue.size()
                    + (chunksPerSecond > 0 ? ", toute la map" : ", rayon: " + radiusChunks)
                    + ", budget: " + workBudgetPerTick
                    + (chunksPerSecond > 0 ? ", cadence cible: " + chunksPerSecond + " chunks/s (20 TPS)" : "")
                    + (lastCompletedChunk == null ? ", dernier chunk termine: aucun"
                            : ", dernier chunk termine (coin sud-est, blocs): X=" + lastCompletedChunk.getMaxBlockX()
                                    + " Z=" + lastCompletedChunk.getMaxBlockZ())
                    + current
                    + ", dimension: " + level.dimension().location();
        }

        private void sendFinishedMessage() {
            source.sendSuccess(() -> Component.literal("Generation terminee. " + getStatusLine()), true);
        }
    }
}
