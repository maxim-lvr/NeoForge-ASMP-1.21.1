package net.maximlvr.asmpthings.block;

import net.maximlvr.asmpthings.AsmpThingsMod;
import net.maximlvr.asmpthings.block.custom.*;
import net.maximlvr.asmpthings.item.ModItems;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModBlocks {

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(AsmpThingsMod.MOD_ID);

    public static final DeferredBlock<Block> ANAESTHETIC_MACHINE_BLOCK = registerBlock("anaesthetic_machine",
            () -> new AnaestheticMachineBlock(BlockBehaviour.Properties.of()
                    .sound(SoundType.METAL)
                    .strength(1f, 10f)
                    .noOcclusion()
                    .hasPostProcess((bs, br, bp) -> true)
                    .emissiveRendering((bs, br, bp) -> true)
                    .isRedstoneConductor((bs, br, bp) -> false)
            ));

    public static final DeferredBlock<Block> ANALYTICAL_BALANCE_BLOCK = registerBlock("analytical_balance",
            () -> new AnalyticalBalanceBlock(BlockBehaviour.Properties.of()
                    .sound(SoundType.METAL)
                    .strength(1f, 10f)
                    .noOcclusion()
                    .hasPostProcess((bs, br, bp) -> true)
                    .emissiveRendering((bs, br, bp) -> true)
                    .isRedstoneConductor((bs, br, bp) -> false)
            ));

    public static final DeferredBlock<Block> BABYBLUEXTILES_BLOCK = registerBlock("babybluextiles",
            () -> new Block(BlockBehaviour.Properties.of().strength(2f)));

    public static final DeferredBlock<Block> BEDSIDE_HEAD_UNIT = registerBlock("bedside_head_unit",
            () -> new BedsideHeadUnitBlock(BlockBehaviour.Properties.of()
                    .sound(SoundType.METAL)
                    .strength(1f, 10f)
                    .noOcclusion()
                    .hasPostProcess((bs, br, bp) -> true)
                    .emissiveRendering((bs, br, bp) -> true)
                    .isRedstoneConductor((bs, br, bp) -> false)
            ));

    public static final DeferredBlock<Block> BLOCK_OF_STAINLESS_STEEL_BLOCK = registerBlock("block_of_stainless_steel",
            () -> new BlockOfStainlessSteelBlock(BlockBehaviour.Properties.of()
                    .sound(SoundType.METAL)
                    .strength(1f, 10f)
                    .noOcclusion()
                    .hasPostProcess((bs, br, bp) -> true)
                    .emissiveRendering((bs, br, bp) -> true)
                    .isRedstoneConductor((bs, br, bp) -> false)
            ));

    public static final DeferredBlock<Block> SURGICAL_INSTRUMENTS_BLOCK = registerBlock("surgical_instruments",
            () -> new SurgicaInstrumentsBlock(BlockBehaviour.Properties.of()
                    .sound(SoundType.METAL)
                    .strength(1f, 10f)
                    .noOcclusion()
                    .hasPostProcess((bs, br, bp) -> true)
                    .emissiveRendering((bs, br, bp) -> true)
                    .isRedstoneConductor((bs, br, bp) -> false)
            ));

    public static final DeferredBlock<Block> SURGICAL_INSTRUMENT_TROLLEY_BLOCK = registerBlock("surgical_instrument_trolley",
            () -> new SurgicalInstrumentTrolleyBlock(BlockBehaviour.Properties.of()
                    .sound(SoundType.METAL)
                    .strength(1f, 10f)
                    .noOcclusion()
                    .hasPostProcess((bs, br, bp) -> true)
                    .emissiveRendering((bs, br, bp) -> true)
                    .isRedstoneConductor((bs, br, bp) -> false)
            ));

    public static final DeferredBlock<Block> SURGICAL_LAMP_BLOCK = registerBlock("surgical_lamp",
            () -> new SurgicalLampBlock(BlockBehaviour.Properties.of()
                    .sound(SoundType.METAL)
                    .strength(1f, 10f)
                    .noOcclusion()
                    .hasPostProcess((bs, br, bp) -> true)
                    .emissiveRendering((bs, br, bp) -> true)
                    .isRedstoneConductor((bs, br, bp) -> false)
            ));

    public static final DeferredBlock<Block> SURGICAL_TROLLEY_BLOCK = registerBlock("surgical_trolley",
            () -> new SurgicalTrolleyBlock(BlockBehaviour.Properties.of()
                    .sound(SoundType.METAL)
                    .strength(1f, 10f)
                    .noOcclusion()
                    .hasPostProcess((bs, br, bp) -> true)
                    .emissiveRendering((bs, br, bp) -> true)
                    .isRedstoneConductor((bs, br, bp) -> false)
            ));

    public static final DeferredBlock<Block> SURGICAL_WORKSTATION_BLOCK = registerBlock("surgical_workstation",
            () -> new SurgicalWorkstationBlock(BlockBehaviour.Properties.of()
                    .sound(SoundType.METAL)
                    .strength(1f, 10f)
                    .noOcclusion()
                    .hasPostProcess((bs, br, bp) -> true)
                    .emissiveRendering((bs, br, bp) -> true)
                    .isRedstoneConductor((bs, br, bp) -> false)
            ));

    public static final DeferredBlock<Block> OPERATING_TABLE_BLOCK = registerBlock("operating_table",
            () -> new OperatingTableBlock(BlockBehaviour.Properties.of()
                    .sound(SoundType.METAL)
                    .strength(1f, 10f)
                    .noOcclusion()
                    .hasPostProcess((bs, br, bp) -> true)
                    .emissiveRendering((bs, br, bp) -> true)
                    .isRedstoneConductor((bs, br, bp) -> false)
            ));

    public static final DeferredBlock<Block> EPOS_CASHIER_SYSTEM_BLOCK = registerBlock("epos_cashier_system",
            () -> new EposCashierSystemBlock(BlockBehaviour.Properties.of()
                    .sound(SoundType.METAL)
                    .strength(1f, 10f)
                    .noOcclusion()
                    .hasPostProcess((bs, br, bp) -> true)
                    .emissiveRendering((bs, br, bp) -> true)
                    .isRedstoneConductor((bs, br, bp) -> false)
            ));

    public static final DeferredBlock<Block> ILLUMINATED_PHARMACY_SIGN_BLOCK = registerBlock("illuminated_pharmacy_sign",
            () -> new IlluminatedPharmacySignBlock(BlockBehaviour.Properties.of()
                    .sound(SoundType.METAL)
                    .strength(1f, 10f)
                    .noOcclusion()
                    .hasPostProcess((bs, br, bp) -> true)
                    .emissiveRendering((bs, br, bp) -> true)
                    .isRedstoneConductor((bs, br, bp) -> false)
            ));

    public static final DeferredBlock<Block> PHARMACY_COUNTER_STOCKED_BLOCK = registerBlock("pharmacy_counter_stocked",
            () -> new PharmacyCounterStockedBlock(BlockBehaviour.Properties.of()
                    .sound(SoundType.METAL)
                    .strength(1f, 10f)
                    .noOcclusion()
                    .hasPostProcess((bs, br, bp) -> true)
                    .emissiveRendering((bs, br, bp) -> true)
                    .isRedstoneConductor((bs, br, bp) -> false)
            ));

    public static final DeferredBlock<Block> REFRIDGERATED_CENTRIFUGE_BLOCK = registerBlock("refridgerated_centrifuge",
            () -> new RefridgeratedCentrifugeBlock(BlockBehaviour.Properties.of()
                    .sound(SoundType.METAL)
                    .strength(1f, 10f)
                    .noOcclusion()
                    .hasPostProcess((bs, br, bp) -> true)
                    .emissiveRendering((bs, br, bp) -> true)
                    .isRedstoneConductor((bs, br, bp) -> false)
            ));

    public static final DeferredBlock<Block> ULTRASOUND_BLOCK = registerBlock("ultrasound",
            () -> new UltraSoundBlock(BlockBehaviour.Properties.of()
                    .sound(SoundType.METAL)
                    .strength(1f, 10f)
                    .noOcclusion()
                    .hasPostProcess((bs, br, bp) -> true)
                    .emissiveRendering((bs, br, bp) -> true)
                    .isRedstoneConductor((bs, br, bp) -> false)
            ));

    public static final DeferredBlock<Block> COMPUTER_CT_SCAN_VIEWER_BLOCK = registerBlock("computer_ct_scan_viewer",
            () -> new ComputerCtScanViewerBlock(BlockBehaviour.Properties.of()
                    .sound(SoundType.METAL)
                    .strength(1f, 10f)
                    .noOcclusion()
                    .hasPostProcess((bs, br, bp) -> true)
                    .emissiveRendering((bs, br, bp) -> true)
                    .isRedstoneConductor((bs, br, bp) -> false)
            ));

    public static final DeferredBlock<Block> COMPUTER_X_RAY_VIEWER_BLOCK = registerBlock("computer_x_ray_viewer",
            () -> new ComputerXRayViewerBlock(BlockBehaviour.Properties.of()
                    .sound(SoundType.METAL)
                    .strength(1f, 10f)
                    .noOcclusion()
                    .hasPostProcess((bs, br, bp) -> true)
                    .emissiveRendering((bs, br, bp) -> true)
                    .isRedstoneConductor((bs, br, bp) -> false)
            ));


    public static final DeferredBlock<Block> MAGIC_BLOCK = registerBlock("magic_block",
            () -> new MagicBlock(BlockBehaviour.Properties.of().strength(2f).requiresCorrectToolForDrops()));

    private static <T extends Block> DeferredBlock<T> registerBlock(String name, Supplier<T> block) {
        DeferredBlock<T> toReturn = BLOCKS.register(name, block);
        registerBlockItem(name, toReturn);
        return toReturn;
    }

    private static <T extends Block> void registerBlockItem(String name, DeferredBlock<T> block) {
        ModItems.ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
    }

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}
