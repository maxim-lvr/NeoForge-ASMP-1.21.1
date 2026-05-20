package net.maximlvr.asmpthings.component;

import com.mojang.serialization.Codec;
import net.maximlvr.asmpthings.AsmpThingsMod;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModDataComponents {
    public static final DeferredRegister.DataComponents DATA_COMPONENTS =
            DeferredRegister.createDataComponents(
                    Registries.DATA_COMPONENT_TYPE,
                    AsmpThingsMod.MOD_ID
            );

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> SCRATCH_DATA =
            DATA_COMPONENTS.registerComponentType("scratch_data",
                    builder -> builder
                            .persistent(Codec.STRING)
                            .networkSynchronized(ByteBufCodecs.STRING_UTF8)
            );

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> SCRATCH_PRIZE =
            DATA_COMPONENTS.registerComponentType("scratch_prize",
                    builder -> builder
                            .persistent(Codec.INT)
                            .networkSynchronized(ByteBufCodecs.VAR_INT)
            );

    public static void register(IEventBus eventBus) {
        DATA_COMPONENTS.register(eventBus);
    }
}