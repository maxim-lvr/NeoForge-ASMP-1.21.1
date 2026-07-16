package net.maximlvr.asmpthings.block.entity;

import net.maximlvr.asmpthings.AsmpThingsMod;
import net.maximlvr.asmpthings.block.ModBlocks;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, AsmpThingsMod.MOD_ID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CardReaderBlockEntity>> CARD_READER =
            BLOCK_ENTITY_TYPES.register("card_reader",
                    () -> BlockEntityType.Builder.of(CardReaderBlockEntity::new, ModBlocks.CARD_READER.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BankBlockEntity>> BANK =
            BLOCK_ENTITY_TYPES.register("bank",
                    () -> BlockEntityType.Builder.of(BankBlockEntity::new, ModBlocks.BANK.get(), ModBlocks.BANK_DOWN.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SkeleteShopTestBlockEntity>> SKELETE_SHOP_TEST =
            BLOCK_ENTITY_TYPES.register("skelete_shop_test",
                    () -> BlockEntityType.Builder.of(SkeleteShopTestBlockEntity::new, ModBlocks.SKELETE_SHOP_TEST.get()).build(null));

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITY_TYPES.register(eventBus);
    }
}
