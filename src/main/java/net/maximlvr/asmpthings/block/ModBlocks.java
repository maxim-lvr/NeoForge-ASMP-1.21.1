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
import net.minecraft.world.level.block.Blocks;

import java.util.function.Supplier;

public class ModBlocks {

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(AsmpThingsMod.MOD_ID);

    public static final DeferredBlock<Block> DEEPSLATE_PORTAL = registerBlock(
            "deepslate_portal",
            () -> new DeepslatePortalBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.DEEPSLATE)
                    .noCollission()
                    .strength(-1.0F, 3600000.0F)
                    .lightLevel(state -> 10)
                    .sound(SoundType.DEEPSLATE))
    );

    public static final DeferredBlock<Block> BANK = registerBlock(
            "bank",
            () -> new BankBlock(BlockBehaviour.Properties.of()
                    .strength(1.5F, 6.0F)
                    .sound(SoundType.METAL))
    );

    public static final DeferredBlock<Block> BANK_DOWN = registerBlock(
            "bank_down",
            () -> new BankBlock(BlockBehaviour.Properties.of()
                    .strength(1.5F, 6.0F)
                    .sound(SoundType.METAL))
    );

    public static final DeferredBlock<Block> CARD_READER = registerBlock(
            "card_reader",
            () -> new CardReaderBlock(BlockBehaviour.Properties.of()
                    .noOcclusion()
                    .strength(1.0F, 4.0F)
                    .sound(SoundType.METAL))
    );

    public static final DeferredBlock<Block> KELETE_KALIFA = registerBlock(
            "kelete_kalifa",
            () -> new HorizontalFacingBlock(BlockBehaviour.Properties.of()
                    .noOcclusion()
                    .strength(1.0F, 4.0F)
                    .sound(SoundType.METAL))
    );

    public static final DeferredBlock<Block> SKELETE_SHOP_TEST = registerBlock(
            "skelete_shop_test",
            () -> new SkeleteShopTestBlock(BlockBehaviour.Properties.of()
                    .noOcclusion()
                    .strength(1.0F, 4.0F)
                    .sound(SoundType.METAL))
    );

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
