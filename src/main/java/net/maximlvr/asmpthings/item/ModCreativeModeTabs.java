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


    public static final Supplier<CreativeModeTab> CORONA_ITEMS_TAB = CREATIVE_MODE_TAB.register("asmp_item_tab",
            () -> CreativeModeTab.builder().icon(() -> new ItemStack(ModItems.CRAZY_COIN.get()))
                    .title(Component.translatable("creativetab.asmpthingsmod.corona_items"))
                    .displayItems((itemDisplayParameters, output) -> {
                        output.accept(ModItems.CORONA);
                        output.accept(ModItems.CRAZY_COIN);
                        output.accept(ModItems.GOLDEN_NUT);
                        output.accept(ModItems.WEATHER_STAFF);
                        output.accept(ModItems.WEATHER_TANK);
                        output.accept(ModItems.GOAL_SMALL_TICKET);

                        output.accept(ModBlocks.MAGIC_BLOCK);

                    }).build());

    public static void register(IEventBus eventBus){
        CREATIVE_MODE_TAB.register(eventBus);
    }
}
