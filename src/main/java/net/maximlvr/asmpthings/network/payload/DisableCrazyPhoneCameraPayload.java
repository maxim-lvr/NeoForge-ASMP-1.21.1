package net.maximlvr.asmpthings.network.payload;

import net.maximlvr.asmpthings.AsmpThingsMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record DisableCrazyPhoneCameraPayload() implements CustomPacketPayload {
    public static final Type<DisableCrazyPhoneCameraPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(AsmpThingsMod.MOD_ID, "disable_crazy_phone_camera"));

    public static final StreamCodec<RegistryFriendlyByteBuf, DisableCrazyPhoneCameraPayload> STREAM_CODEC =
            StreamCodec.unit(new DisableCrazyPhoneCameraPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
