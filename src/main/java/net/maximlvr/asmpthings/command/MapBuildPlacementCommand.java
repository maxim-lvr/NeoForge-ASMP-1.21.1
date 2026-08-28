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
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@EventBusSubscriber(modid = AsmpThingsMod.MOD_ID)
public final class MapBuildPlacementCommand {
    private static final ResourceLocation ZONE_RESOURCE = ResourceLocation.fromNamespaceAndPath(
            AsmpThingsMod.MOD_ID,
            "msmp/dense_1_minecraft.json"
    );
    private static final int DEFAULT_WORK_BUDGET_PER_TICK = 16384;
    private static final int MAX_WORK_BUDGET_PER_TICK = 65536;
    private static final int TEMPLATE_WORK_COST = 64;
    private static final int BLOCK_FILL_UPDATE_FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SUPPRESS_DROPS;
    private static PlacementTask activeTask;

    private MapBuildPlacementCommand() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("asmpbuildzone")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("start")
                        .executes(context -> start(context, DEFAULT_WORK_BUDGET_PER_TICK))
                        .then(Commands.argument("budgetParTick", IntegerArgumentType.integer(1, MAX_WORK_BUDGET_PER_TICK))
                                .executes(context -> start(context, IntegerArgumentType.getInteger(context, "budgetParTick")))))
                .then(Commands.literal("status")
                        .executes(MapBuildPlacementCommand::status))
                .then(Commands.literal("stop")
                        .executes(MapBuildPlacementCommand::stop)));
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (activeTask == null) {
            return;
        }

        activeTask.tick();

        if (activeTask.isFinished()) {
            activeTask.sendFinishedMessage();
            activeTask = null;
        }
    }

    private static int start(CommandContext<CommandSourceStack> context, int workBudgetPerTick) {
        CommandSourceStack source = context.getSource();

        if (activeTask != null) {
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

    private static int status(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();

        if (activeTask == null) {
            source.sendSuccess(() -> Component.literal("Aucune pose de zone en cours."), false);
            return 1;
        }

        source.sendSuccess(() -> Component.literal(activeTask.getStatusLine()), false);
        return 1;
    }

    private static int stop(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();

        if (activeTask == null) {
            source.sendSuccess(() -> Component.literal("Aucune pose de zone en cours."), false);
            return 1;
        }

        String status = activeTask.getStatusLine();
        activeTask = null;
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
                placements.add(placement);
            }
        }
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
            placements.add(placement);
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
                Size.ONE,
                rotationFromY(rotationY)
        );
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
    }

    private static final class PlacementTask {
        private final CommandSourceStack source;
        private final ServerLevel level;
        private final List<Placement> placements;
        private final int workBudgetPerTick;
        private final Map<ResourceLocation, Optional<StructureTemplate>> templateCache = new HashMap<>();
        private final HolderLookup<Block> blockLookup;
        private final Map<String, Optional<BlockState>> blockStateCache = new HashMap<>();
        private final Set<Long> loadedChunks = new HashSet<>();
        private int index = 0;
        private int placed = 0;
        private int failed = 0;
        private long blocksFilled = 0;
        private long currentBlockOffset = 0;
        private int tickCounter = 0;

        private PlacementTask(CommandSourceStack source, ServerLevel level, List<Placement> placements, int workBudgetPerTick) {
            this.source = source;
            this.level = level;
            this.placements = placements;
            this.workBudgetPerTick = workBudgetPerTick;
            this.blockLookup = source.getServer().registryAccess().lookupOrThrow(Registries.BLOCK);
        }

        private void tick() {
            int usedBudget = 0;

            while (index < placements.size() && usedBudget < workBudgetPerTick) {
                Placement placement = placements.get(index);

                if (placement.mode == PlacementMode.BLOCK_FILL) {
                    usedBudget += placeBlockFill(placement, workBudgetPerTick - usedBudget);
                    continue;
                }

                if (usedBudget > 0 && usedBudget + TEMPLATE_WORK_COST > workBudgetPerTick) {
                    break;
                }

                place(placement);
                index++;
                currentBlockOffset = 0;
                usedBudget += TEMPLATE_WORK_COST;
            }

            tickCounter++;

            if (!isFinished() && tickCounter % 20 == 0) {
                source.sendSuccess(() -> Component.literal(getStatusLine()), false);
            }
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
            long key = (((long) chunkX) << 32) ^ (chunkZ & 0xffffffffL);

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

            return "Progression zone: " + index + "/" + placements.size()
                    + ", poses OK: " + placed
                    + ", echecs: " + failed
                    + ", blocs remplis: " + blocksFilled
                    + currentPlacement
                    + ", dimension: " + level.dimension().location();
        }

        private void sendFinishedMessage() {
            source.sendSuccess(() -> Component.literal("Pose terminee. " + getStatusLine()), true);
        }
    }
}
