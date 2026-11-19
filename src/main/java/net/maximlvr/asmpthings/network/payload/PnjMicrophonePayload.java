package net.maximlvr.asmpthings.network.payload;

import net.maximlvr.asmpthings.AsmpThingsMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import java.util.UUID;

public record PnjMicrophonePayload(UUID sessionId, byte[] audio) implements CustomPacketPayload {
    public static final int MAX_BYTES = 3840;
    public static final Type<PnjMicrophonePayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(AsmpThingsMod.MOD_ID, "pnj_microphone"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PnjMicrophonePayload> STREAM_CODEC = StreamCodec.of(
            (b, p) -> { b.writeUUID(p.sessionId); b.writeByteArray(p.audio); },
            b -> new PnjMicrophonePayload(b.readUUID(), b.readByteArray(MAX_BYTES)));
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
