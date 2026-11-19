package net.maximlvr.asmpthings.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.maximlvr.asmpthings.AsmpThingsMod;
import net.maximlvr.asmpthings.world.ModDimensions;
import net.maximlvr.asmpthings.world.map.MapPlacementTask;
import net.maximlvr.asmpthings.world.map.MapPlanIndex;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@EventBusSubscriber(modid = AsmpThingsMod.MOD_ID)
public final class MapBuildPlacementCommand {
    private static final ResourceLocation ZONE_RESOURCE = ResourceLocation.fromNamespaceAndPath(
            AsmpThingsMod.MOD_ID, "msmp/map_generation_final_seed42_10000x10000x1000.json");
    private static final int DEFAULT_BUDGET = 16384, MAX_BUDGET = 65536;
    private static final int DEFAULT_RADIUS = 5, MAX_RADIUS = 16;
    private static final int DEFAULT_CHUNKS_PER_SECOND = 10, MAX_CHUNKS_PER_SECOND = 100;
    private static final ExecutorService IO = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "ASMP-map-index");
        thread.setDaemon(true);
        return thread;
    });
    private static Job activeJob;

    @SubscribeEvent public static void commands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("asmpbuildzone").requires(source -> source.hasPermission(2))
                .then(Commands.literal("start")
                        .executes(context -> start(context.getSource(), -1, DEFAULT_BUDGET, DEFAULT_CHUNKS_PER_SECOND))
                        .then(Commands.argument("budgetParTick", IntegerArgumentType.integer(1, MAX_BUDGET))
                                .executes(context -> start(context.getSource(), -1,
                                        IntegerArgumentType.getInteger(context, "budgetParTick"), DEFAULT_CHUNKS_PER_SECOND))))
                .then(Commands.literal("chunks")
                        .then(Commands.literal("start")
                                .then(Commands.argument("chunksParSeconde", IntegerArgumentType.integer(1, MAX_CHUNKS_PER_SECOND))
                                        .executes(context -> start(context.getSource(), -1, DEFAULT_BUDGET,
                                                IntegerArgumentType.getInteger(context, "chunksParSeconde")))))
                        .then(Commands.literal("status").executes(context -> status(context.getSource())))
                        .then(Commands.literal("stop").executes(context -> stop(context.getSource()))))
                .then(Commands.literal("lazy")
                        .then(Commands.literal("start")
                                .executes(context -> start(context.getSource(), DEFAULT_RADIUS, DEFAULT_BUDGET, DEFAULT_CHUNKS_PER_SECOND))
                                .then(Commands.argument("rayonChunks", IntegerArgumentType.integer(0, MAX_RADIUS))
                                        .executes(context -> start(context.getSource(),
                                                IntegerArgumentType.getInteger(context, "rayonChunks"), DEFAULT_BUDGET, DEFAULT_CHUNKS_PER_SECOND))
                                        .then(Commands.argument("budgetParTick", IntegerArgumentType.integer(1, MAX_BUDGET))
                                                .executes(context -> start(context.getSource(),
                                                        IntegerArgumentType.getInteger(context, "rayonChunks"),
                                                        IntegerArgumentType.getInteger(context, "budgetParTick"), DEFAULT_CHUNKS_PER_SECOND)))))
                        .then(Commands.literal("status").executes(context -> status(context.getSource())))
                        .then(Commands.literal("stop").executes(context -> stop(context.getSource()))))
                .then(Commands.literal("status").executes(context -> status(context.getSource())))
                .then(Commands.literal("stop").executes(context -> stop(context.getSource()))));
    }

    private static int start(CommandSourceStack source, int radius, int budget, int chunksPerSecond) {
        if (activeJob != null) return failure(source, "Une génération est déjà en cours. /asmpbuildzone status ou stop.");
        ServerLevel level = source.getServer().getLevel(ModDimensions.ASMP_DIMENSION_KEY);
        if (level == null) return failure(source, "Dimension introuvable : " + ModDimensions.ASMP_DIMENSION_KEY.location());
        Resource resource = source.getServer().getResourceManager().getResource(ZONE_RESOURCE).orElse(null);
        if (resource == null) return failure(source, "Plan introuvable : " + ZONE_RESOURCE);
        activeJob = new Job(source, level, resource, radius, budget, chunksPerSecond);
        source.sendSuccess(() -> Component.literal("Préparation de la carte en arrière-plan. /asmpbuildzone status pour suivre, stop pour annuler."), false);
        return 1;
    }

    private static int status(CommandSourceStack source) {
        String text = activeJob == null ? "Aucune génération en cours." : activeJob.status();
        source.sendSuccess(() -> Component.literal(text), false);
        return 1;
    }

    private static int stop(CommandSourceStack source) {
        Job job = activeJob;
        activeJob = null;
        if (job != null) job.close();
        source.sendSuccess(() -> Component.literal(job == null ? "Aucune génération en cours."
                : "Génération arrêtée. Les blocs déjà posés sont conservés."), false);
        return 1;
    }

    @SubscribeEvent public static void stopped(ServerStoppedEvent event) {
        if (activeJob != null) activeJob.close();
        activeJob = null;
    }

    @SubscribeEvent public static void tick(ServerTickEvent.Post event) {
        Job job = activeJob;
        if (job == null) return;
        try {
            if (job.tick()) {
                activeJob = null;
                job.close();
            }
        } catch (Exception error) {
            activeJob = null;
            job.close();
            AsmpThingsMod.LOGGER.error("Échec de la génération de carte", error);
            failure(job.source, "Génération arrêtée : " + error.getMessage());
        }
    }

    private static int failure(CommandSourceStack source, String text) {
        source.sendFailure(Component.literal(text));
        return 0;
    }

    private static void dispose(MapPlanIndex index) {
        IO.execute(() -> {
            try { index.close(); }
            catch (IOException error) { AsmpThingsMod.LOGGER.warn("Impossible de supprimer l'index temporaire de carte", error); }
        });
    }

    private static final class Job implements AutoCloseable {
        final CommandSourceStack source;
        final ServerLevel level;
        final int radius, budget, rate;
        final MapPlanIndex.Progress progress = new MapPlanIndex.Progress();
        final Future<?> preparation;
        volatile boolean cancelled, prepared;
        volatile Exception preparationError;
        MapPlanIndex result, index;
        MapPlacementTask placement;
        int ticks;

        Job(CommandSourceStack source, ServerLevel level, Resource resource, int radius, int budget, int rate) {
            this.source = source;
            this.level = level;
            this.radius = radius;
            this.budget = budget;
            this.rate = rate;
            Path cacheRoot = source.getServer().getWorldPath(LevelResource.ROOT).resolve("asmp_map_cache");
            int minY = level.getMinBuildHeight(), maxY = level.getMaxBuildHeight();
            preparation = IO.submit(() -> {
                try (Reader reader = resource.openAsReader()) {
                    MapPlanIndex built = MapPlanIndex.build(reader, cacheRoot, minY, maxY, () -> cancelled, progress);
                    synchronized (this) {
                        if (cancelled) dispose(built);
                        else result = built;
                    }
                } catch (Exception error) {
                    if (!cancelled) preparationError = error;
                } finally {
                    prepared = true;
                }
            });
        }

        boolean tick() throws IOException, CommandSyntaxException {
            ticks++;
            if (placement == null) {
                if (preparationError != null) throw new IOException(preparationError.getMessage(), preparationError);
                if (prepared) {
                    synchronized (this) { index = result; result = null; }
                    if (index == null || index.chunkCount() == 0) {
                        failure(source, "Aucun placement dans le plan.");
                        return true;
                    }
                    BlockState[] states = new BlockState[index.materials().size()];
                    StructureTemplate[] templates = new StructureTemplate[states.length];
                    var lookup = level.registryAccess().lookupOrThrow(Registries.BLOCK);
                    for (int i = 0; i < states.length; i++) {
                        var material = index.materials().get(i);
                        if (material.template()) {
                            templates[i] = level.getServer().getStructureManager().get(ResourceLocation.parse(material.name()))
                                    .orElseThrow(() -> new IOException("Structure NBT manquante : " + material.name()));
                        } else states[i] = BlockStateParser.parseForBlock(lookup, material.name(), false).blockState();
                    }
                    placement = new MapPlacementTask(level, index, states, templates, budget, radius, rate);
                    source.sendSuccess(() -> Component.literal("Index prêt : " + index.operationCount() + " opérations, "
                            + index.chunkCount() + " chunks. Placement progressif dans " + level.dimension().location() + "."), false);
                }
            } else {
                placement.tick();
                if (placement.isFinished()) {
                    String message = "Génération terminée. " + placement.status();
                    source.sendSuccess(() -> Component.literal(message), false);
                    return true;
                }
            }
            if (ticks % 40 == 0) {
                String message = status();
                source.sendSuccess(() -> Component.literal(message), false);
            }
            return false;
        }

        String status() { return placement == null ? progress.describe() : placement.status(); }

        @Override public synchronized void close() {
            cancelled = true;
            preparation.cancel(true);
            if (placement != null) placement.close();
            if (index != null) { dispose(index); index = null; }
            if (result != null) { dispose(result); result = null; }
        }
    }

    private MapBuildPlacementCommand() {}
}
