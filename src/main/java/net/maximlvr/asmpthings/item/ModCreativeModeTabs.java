package net.maximlvr.asmpthings.item;

import net.maximlvr.asmpthings.AsmpThingsMod;
import net.maximlvr.asmpthings.block.ModBlocks;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModCreativeModeTabs {

    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TAB =
        DeferredRegister.create(Registries.CREATIVE_MODE_TAB, AsmpThingsMod.MOD_ID);


    public static final Supplier<CreativeModeTab> ASMP_ITEMS_TAB = CREATIVE_MODE_TAB.register("asmp_item_tab",
            () -> CreativeModeTab.builder().icon(() -> new ItemStack(ModItems.CRAZY_COIN.get()))
                    .title(Component.translatable("creativetab.asmpthingsmod.asmp_item"))
                    .displayItems((itemDisplayParameters, output) -> {
                        output.accept(ModItems.CORONA);
                        output.accept(ModItems.CRAZY_COIN);
                        output.accept(ModItems.GOLDEN_NUT);
                        output.accept(ModItems.COSMECOIN);
                        output.accept(ModItems.WEATHER_STAFF);
                        output.accept(ModItems.WEATHER_TANK);
                        output.accept(ModItems.GOAL_SMALL_TICKET);
                        output.accept(ModItems.ROLE_CARD);

                    }).build());

    public static final Supplier<CreativeModeTab> HOSPITAL_ITEM_TAB = CREATIVE_MODE_TAB.register("hospital_item_tab",
            () -> CreativeModeTab.builder().icon(() -> new ItemStack(ModBlocks.ANAESTHETIC_MACHINE_BLOCK.get()))
                    .title(Component.translatable("creativetab.asmpthingsmod.hospital_item"))
                    .displayItems((itemDisplayParameters, output) -> {

                        output.accept(ModBlocks.ANAESTHETIC_MACHINE_BLOCK);
                        output.accept(ModBlocks.ANALYTICAL_BALANCE_BLOCK);
                        output.accept(ModBlocks.BLOCK_OF_STAINLESS_STEEL_BLOCK);
                        output.accept(ModBlocks.BABYBLUEXTILES_BLOCK);
                        output.accept(ModBlocks.BEDSIDE_HEAD_UNIT);
                        output.accept(ModBlocks.EPOS_CASHIER_SYSTEM_BLOCK);
                        output.accept(ModBlocks.ILLUMINATED_PHARMACY_SIGN_BLOCK);
                        output.accept(ModBlocks.COMPUTER_CT_SCAN_VIEWER_BLOCK);
                        output.accept(ModBlocks.COMPUTER_X_RAY_VIEWER_BLOCK);
                        output.accept(ModBlocks.OPERATING_TABLE_BLOCK);
                        output.accept(ModBlocks.PHARMACY_COUNTER_STOCKED_BLOCK);
                        output.accept(ModBlocks.REFRIDGERATED_CENTRIFUGE_BLOCK);
                        output.accept(ModBlocks.ULTRASOUND_BLOCK);
                        output.accept(ModBlocks.SURGICAL_WORKSTATION_BLOCK);
                        output.accept(ModBlocks.SURGICAL_INSTRUMENTS_BLOCK);
                        output.accept(ModBlocks.SURGICAL_LAMP_BLOCK);
                        output.accept(ModBlocks.SURGICAL_INSTRUMENT_TROLLEY_BLOCK);
                        output.accept(ModBlocks.SURGICAL_TROLLEY_BLOCK);

                        output.accept(ModBlocks.X_RAY_MACHINE_BLOCK);
                        output.accept(ModBlocks.X_RAY_LIGHT_BLOCK);
                        output.accept(ModBlocks.X_RAY_BUCKY_STAND_BLOCK);
                        output.accept(ModBlocks.X_RAY_LIGHT_BOX_ARMS_BLOCK);
                        output.accept(ModBlocks.X_RAY_LIGHT_BOX_BLANK_BLOCK);
                        output.accept(ModBlocks.X_RAY_LIGHT_BOX_CHEST_BLOCK);
                        output.accept(ModBlocks.X_RAY_LIGHT_BOX_FEET_BLOCK);
                        output.accept(ModBlocks.X_RAY_LIGHT_BOX_NECK_BLOCK);
                        output.accept(ModBlocks.WARD_RESUS_TROLLEY_BLOCK);
                        output.accept(ModBlocks.WARD_STORAGE_DRAWERS_BLOCK);
                        output.accept(ModBlocks.HOSPITAL_BED_BLOCK);
                        output.accept(ModBlocks.IV_STAND_BLOCK);
                        output.accept(ModBlocks.HEART_RATE_MONITOR_BLOCK);
                        output.accept(ModBlocks.HAND_SANITISER_DISPENSER_BLOCK);


                    }).build());

    public static void register(IEventBus eventBus){
        CREATIVE_MODE_TAB.register(eventBus);
    }
}
