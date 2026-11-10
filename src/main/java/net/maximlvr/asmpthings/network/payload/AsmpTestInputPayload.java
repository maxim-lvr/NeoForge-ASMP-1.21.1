package net.maximlvr.asmpthings.network.payload;

import net.maximlvr.asmpthings.AsmpThingsMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record AsmpTestInputPayload(float forward, float left, float yaw, float pitch, int flags, int slot) implements CustomPacketPayload {
    public static final int JUMP = 1, SPRINT = 2, SNEAK = 4, ATTACK = 8, USE = 16, INVENTORY = 32, STOP = 64;
    public static final Type<AsmpTestInputPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(AsmpThingsMod.MOD_ID, "test_input"));
    public static final StreamCodec<RegistryFriendlyByteBuf, AsmpTestInputPayload> STREAM_CODEC = StreamCodec.of(
            (b, p) -> { b.writeFloat(p.forward); b.writeFloat(p.left); b.writeFloat(p.yaw); b.writeFloat(p.pitch); b.writeInt(p.flags); b.writeInt(p.slot); },
            b -> new AsmpTestInputPayload(b.readFloat(), b.readFloat(), b.readFloat(), b.readFloat(), b.readInt(), b.readInt()));
    public boolean has(int flag) { return (flags & flag) != 0; }
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
