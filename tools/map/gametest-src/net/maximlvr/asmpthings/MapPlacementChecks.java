package net.maximlvr.asmpthings;

import net.maximlvr.asmpthings.world.ModDimensions;
import net.maximlvr.asmpthings.world.map.*;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import java.io.StringReader;
import java.nio.file.Files;

@GameTestHolder("asmpthingsmod")
@PrefixGameTestTemplate(false)
public final class MapPlacementChecks {
    @GameTest(template = "map_empty", batch = "map_placement", timeoutTicks = 400)
    public static void orderedFillsAndChunkFinalization(GameTestHelper helper) throws Exception {
        var level = helper.getLevel();
        BlockPos origin = helper.absolutePos(new BlockPos(1, 1, 1));
        int x = ((origin.getX() >> 4) + 1) << 4;
        int z = ((origin.getZ() >> 4) + 1) << 4;
        int y = ((origin.getY() >> 4) + 1) << 4;
        BlockPos chest = new BlockPos(x + 9, y + 2, z + 3);
        level.setBlockAndUpdate(chest, Blocks.CHEST.defaultBlockState());
        helper.assertTrue(level.getBlockEntity(chest) != null, "Fixture coffre absente");
        var biomeBefore = level.getBiome(new BlockPos(x + 1, y + 1, z + 1));
        String json = """
                {"runs":[
                  {"block":"minecraft:deepslate","pos":[%d,%d,%d],"size":[32,32,16]},
                  {"block":"minecraft:air","pos":[%d,-64,%d],"size":[8,384,16]},
                  {"block":"minecraft:air","pos":[%d,%d,%d],"size":[1,3,1]},
                  {"block":"minecraft:water","pos":[%d,%d,%d],"size":[2,1,1]},
                  {"block":"minecraft:ladder[facing=north]","pos":[%d,%d,%d],"size":[1,3,1]}
                ]}
                """.formatted(x, y, z, x + 8, z, x + 16, y + 1, z + 3,
                x + 15, y + 1, z + 5, x + 16, y + 1, z + 3);
        var directory = Files.createTempDirectory("asmp-map-engine-check-");
        MapPlanIndex index = MapPlanIndex.build(new StringReader(json), directory, level.getMinBuildHeight(),
                level.getMaxBuildHeight(), () -> false, new MapPlanIndex.Progress());
        BlockState[] states = new BlockState[index.materials().size()];
        var lookup = level.registryAccess().lookupOrThrow(Registries.BLOCK);
        for (int i = 0; i < states.length; i++)
            states[i] = BlockStateParser.parseForBlock(lookup, index.materials().get(i).name(), false).blockState();
        MapPlacementTask task = new MapPlacementTask(level, index, states, new StructureTemplate[states.length], 128, -1, 100);
        helper.succeedWhen(() -> {
            try { task.tick(); } catch (Exception error) { throw new RuntimeException(error); }
            helper.assertTrue(task.isFinished(), "Pose en cours : " + task.status());
            helper.assertTrue(level.getBlockState(new BlockPos(x + 2, y + 2, z + 2)).is(Blocks.DEEPSLATE), "Remplissage solide incorrect");
            helper.assertTrue(level.getBlockState(chest).isAir() && level.getBlockEntity(chest) == null, "Entité de bloc fantôme après remplacement de section");
            helper.assertTrue(level.getHeight(Heightmap.Types.WORLD_SURFACE, x + 9, z + 3) == level.getMinBuildHeight(), "Heightmap de la colonne excavée incorrecte");
            BlockState ladder = level.getBlockState(new BlockPos(x + 16, y + 2, z + 3));
            helper.assertTrue(ladder.is(Blocks.LADDER) && ladder.getValue(LadderBlock.FACING) == Direction.NORTH, "Échelle ou orientation perdue");
            helper.assertTrue(level.getBlockState(new BlockPos(x + 15, y + 1, z + 5)).is(Blocks.WATER)
                    && level.getBlockState(new BlockPos(x + 16, y + 1, z + 5)).is(Blocks.WATER), "Eau tronquée à la limite du chunk");
            helper.assertTrue(level.getBiome(new BlockPos(x + 1, y + 1, z + 1)).equals(biomeBefore), "Biome changé par le remplacement de section");
            task.close();
            try { index.close(); Files.delete(directory); } catch (Exception error) { throw new RuntimeException(error); }
        });
    }

    @GameTest(template = "map_empty", batch = "map_command", timeoutTicks = 200)
    public static void commandsDoNotParseTheMapOnTheServerThread(GameTestHelper helper) throws Exception {
        var server = helper.getLevel().getServer();
        helper.assertTrue(server.getLevel(ModDimensions.ASMP_DIMENSION_KEY) != null, "Dimension ASMP absente");
        var dispatcher = server.getCommands().getDispatcher();
        var source = server.createCommandSourceStack().withPermission(4).withSuppressedOutput();
        long start = System.nanoTime();
        int result = dispatcher.execute("asmpbuildzone lazy start 0", source);
        long elapsed = System.nanoTime() - start;
        helper.assertTrue(result == 1, "La commande de préparation a échoué");
        helper.assertTrue(elapsed < 250_000_000L, "La commande bloque encore le thread serveur");
        helper.assertTrue(dispatcher.execute("asmpbuildzone status", source) == 1, "Status indisponible pendant le chargement");
        helper.assertTrue(dispatcher.execute("asmpbuildzone stop", source) == 1, "Stop indisponible pendant le chargement");
        helper.succeed();
    }
}
