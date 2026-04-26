package net.maximlvr.asmpthings.item;

import net.maximlvr.asmpthings.AsmpThingsMod;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(AsmpThingsMod.MOD_ID);

    public static final DeferredItem<Item> CORONA = ITEMS.register("corona",
            () -> new Item(new Item.Properties()));


    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
