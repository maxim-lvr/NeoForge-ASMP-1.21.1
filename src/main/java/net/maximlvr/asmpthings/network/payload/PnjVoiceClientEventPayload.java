package net.maximlvr.asmpthings.network.payload;

import net.maximlvr.asmpthings.AsmpThingsMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import java.util.UUID;

public record PnjVoiceClientEventPayload(UUID sessionId, int action, String itemId,
                                        int contentIndex, int playedMs) implements CustomPacketPayload {
    public static final int STOP = 0, PLAYED = 1;
    public static final Type<PnjVoiceClientEventPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(AsmpThingsMod.MOD_ID, "pnj_voice_client_event"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PnjVoiceClientEventPayload> STREAM_CODEC = StreamCodec.of(
            (b, p) -> { b.writeUUID(p.sessionId); b.writeVarInt(p.action); b.writeUtf(p.itemId, 128);
                b.writeVarInt(p.contentIndex); b.writeVarInt(p.playedMs); },
            b -> new PnjVoiceClientEventPayload(b.readUUID(), b.readVarInt(), b.readUtf(128), b.readVarInt(), b.readVarInt()));
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
