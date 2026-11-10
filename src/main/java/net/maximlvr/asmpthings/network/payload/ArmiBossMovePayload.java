package net.maximlvr.asmpthings.network.payload;

import net.maximlvr.asmpthings.AsmpThingsMod;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

public record ArmiBossMovePayload(UUID bossUuid, BlockPos block, boolean cancel) implements CustomPacketPayload {
    public static final Type<ArmiBossMovePayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(AsmpThingsMod.MOD_ID, "armi_boss_move"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ArmiBossMovePayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> {
                buffer.writeUUID(payload.bossUuid());
                buffer.writeBlockPos(payload.block());
                buffer.writeBoolean(payload.cancel());
            }, buffer -> new ArmiBossMovePayload(buffer.readUUID(), buffer.readBlockPos(), buffer.readBoolean()));

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
