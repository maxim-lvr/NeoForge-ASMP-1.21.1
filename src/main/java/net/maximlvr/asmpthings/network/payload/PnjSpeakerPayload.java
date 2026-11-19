package net.maximlvr.asmpthings.network.payload;

import net.maximlvr.asmpthings.AsmpThingsMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import java.util.UUID;

public record PnjSpeakerPayload(UUID sessionId, String itemId, int contentIndex, byte[] audio) implements CustomPacketPayload {
    public static final int MAX_BYTES = 16384;
    public static final Type<PnjSpeakerPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(AsmpThingsMod.MOD_ID, "pnj_speaker"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PnjSpeakerPayload> STREAM_CODEC = StreamCodec.of(
            (b, p) -> { b.writeUUID(p.sessionId); b.writeUtf(p.itemId, 128);
                b.writeVarInt(p.contentIndex); b.writeByteArray(p.audio); },
            b -> new PnjSpeakerPayload(b.readUUID(), b.readUtf(128), b.readVarInt(), b.readByteArray(MAX_BYTES)));
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
