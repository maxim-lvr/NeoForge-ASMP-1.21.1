package net.maximlvr.asmpthings.stats;

import net.maximlvr.asmpthings.AsmpThingsMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModStats {
    public static final DeferredRegister<ResourceLocation> CUSTOM_STATS =
            DeferredRegister.create(Registries.CUSTOM_STAT, AsmpThingsMod.MOD_ID);

    public static final DeferredHolder<ResourceLocation, ResourceLocation> SCRATCH_TICKETS_SCRATCHED =
            CUSTOM_STATS.register("scratch_tickets_scratched", () ->
                    ResourceLocation.fromNamespaceAndPath(AsmpThingsMod.MOD_ID, "scratch_tickets_scratched"));

    public static final DeferredHolder<ResourceLocation, ResourceLocation> CRAZY_PHONE_MESSAGES_SENT =
            CUSTOM_STATS.register("crazy_phone_messages_sent", () ->
                    ResourceLocation.fromNamespaceAndPath(AsmpThingsMod.MOD_ID, "crazy_phone_messages_sent"));

    public static void register(IEventBus eventBus) {
        CUSTOM_STATS.register(eventBus);
    }
}
