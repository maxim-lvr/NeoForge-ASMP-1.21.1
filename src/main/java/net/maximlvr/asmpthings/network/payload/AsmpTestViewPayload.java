package net.maximlvr.asmpthings.network.payload;

import net.maximlvr.asmpthings.AsmpThingsMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import java.util.UUID;

public record AsmpTestViewPayload(int targetId, UUID targetUuid, boolean control) implements CustomPacketPayload {
    public static final Type<AsmpTestViewPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(AsmpThingsMod.MOD_ID, "test_view"));
    public static final StreamCodec<RegistryFriendlyByteBuf, AsmpTestViewPayload> STREAM_CODEC = StreamCodec.of(
            (b, p) -> { b.writeInt(p.targetId); b.writeUUID(p.targetUuid); b.writeBoolean(p.control); },
            b -> new AsmpTestViewPayload(b.readInt(), b.readUUID(), b.readBoolean()));
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
