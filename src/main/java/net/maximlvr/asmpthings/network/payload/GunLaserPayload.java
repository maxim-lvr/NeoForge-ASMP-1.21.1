package net.maximlvr.asmpthings.network.payload;

import net.maximlvr.asmpthings.AsmpThingsMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

public record GunLaserPayload(ResourceLocation dimension, Vec3 start, Vec3 end) implements CustomPacketPayload {
    public static final Type<GunLaserPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(AsmpThingsMod.MOD_ID, "gun_laser"));
    public static final StreamCodec<RegistryFriendlyByteBuf, GunLaserPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, trace) -> {
                buffer.writeResourceLocation(trace.dimension());
                buffer.writeVec3(trace.start());
                buffer.writeVec3(trace.end());
            }, buffer -> new GunLaserPayload(buffer.readResourceLocation(), buffer.readVec3(), buffer.readVec3()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
