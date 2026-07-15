package net.maximlvr.asmpthings.network.payload;

import net.maximlvr.asmpthings.AsmpThingsMod;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record CrazyPhoneSyncPayload(boolean mainHand, CompoundTag tag) implements CustomPacketPayload {
    public static final Type<CrazyPhoneSyncPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(AsmpThingsMod.MOD_ID, "crazy_phone_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CrazyPhoneSyncPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> {
                buffer.writeBoolean(payload.mainHand());
                buffer.writeNbt(payload.tag());
            },
            buffer -> new CrazyPhoneSyncPayload(buffer.readBoolean(), buffer.readNbt())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
