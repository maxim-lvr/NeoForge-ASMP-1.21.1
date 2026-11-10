package net.maximlvr.asmpthings.network.payload;

import net.maximlvr.asmpthings.AsmpThingsMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import java.util.UUID;

public record AsmpTestRelayPayload(UUID controller, AsmpTestInputPayload input) implements CustomPacketPayload {
    public static final Type<AsmpTestRelayPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(AsmpThingsMod.MOD_ID, "test_relay"));
    public static final StreamCodec<RegistryFriendlyByteBuf, AsmpTestRelayPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> {
                buffer.writeUUID(payload.controller());
                AsmpTestInputPayload.STREAM_CODEC.encode(buffer, payload.input());
            }, buffer -> new AsmpTestRelayPayload(buffer.readUUID(), AsmpTestInputPayload.STREAM_CODEC.decode(buffer)));
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
