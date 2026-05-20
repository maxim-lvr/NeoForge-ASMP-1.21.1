package net.maximlvr.asmpthings.item;

import net.maximlvr.asmpthings.AsmpThingsMod;
import net.maximlvr.asmpthings.item.custom.WeatherStaff;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(AsmpThingsMod.MOD_ID);

    public static final DeferredItem<Item> CORONA = ITEMS.register("corona",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> CRAZY_COIN = ITEMS.register("crazy_coin",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> GOLDEN_NUT = ITEMS.register("golden_nut",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> WEATHER_STAFF = ITEMS.register("weather_staff",
            () -> new WeatherStaff(new Item.Properties().durability(32)));

    public static final DeferredItem<Item> WEATHER_TANK = ITEMS.register("weather_tank",
            () -> new Item(new Item.Properties()));


    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
