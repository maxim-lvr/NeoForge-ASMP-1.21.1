package net.maximlvr.asmpthings.network.payload;

import net.maximlvr.asmpthings.AsmpThingsMod;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public record ArmiBossPathPayload(UUID bossUuid, List<BlockPos> nodes) implements CustomPacketPayload {
    public static final int MAX_NODES = 256;
    public static final Type<ArmiBossPathPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(AsmpThingsMod.MOD_ID, "armi_boss_path"));

    public ArmiBossPathPayload {
        if (nodes.size() > MAX_NODES) {
            throw new IllegalArgumentException("Armi boss path is too long");
        }
        nodes = List.copyOf(nodes);
    }

    public static final StreamCodec<RegistryFriendlyByteBuf, ArmiBossPathPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> {
                buffer.writeUUID(payload.bossUuid());
                buffer.writeVarInt(payload.nodes().size());
                payload.nodes().forEach(buffer::writeBlockPos);
            }, buffer -> {
                UUID boss = buffer.readUUID();
                int size = buffer.readVarInt();
                if (size < 0 || size > MAX_NODES) {
                    throw new IllegalArgumentException("Invalid Armi boss path length");
                }
                List<BlockPos> nodes = new ArrayList<>(size);
                for (int i = 0; i < size; i++) {
                    nodes.add(buffer.readBlockPos());
                }
                return new ArmiBossPathPayload(boss, nodes);
            });

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
