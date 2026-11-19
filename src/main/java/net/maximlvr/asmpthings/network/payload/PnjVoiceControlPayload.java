package net.maximlvr.asmpthings.network.payload;

import net.maximlvr.asmpthings.AsmpThingsMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import java.util.UUID;

public record PnjVoiceControlPayload(UUID sessionId, int action, String npcName,
                                     String itemId, int contentIndex, String message) implements CustomPacketPayload {
    public static final int START = 0, STOP = 1, INTERRUPT = 2;
    public static final Type<PnjVoiceControlPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(AsmpThingsMod.MOD_ID, "pnj_voice_control"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PnjVoiceControlPayload> STREAM_CODEC = StreamCodec.of(
            (b, p) -> { b.writeUUID(p.sessionId); b.writeVarInt(p.action); b.writeUtf(p.npcName, 128);
                b.writeUtf(p.itemId, 128); b.writeVarInt(p.contentIndex); b.writeUtf(p.message, 512); },
            b -> new PnjVoiceControlPayload(b.readUUID(), b.readVarInt(), b.readUtf(128),
                    b.readUtf(128), b.readVarInt(), b.readUtf(512)));
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
