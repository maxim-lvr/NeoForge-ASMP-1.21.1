package net.maximlvr.asmpthings.network.payload;

import net.maximlvr.asmpthings.AsmpThingsMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

public record ArmiBossCinematicPayload(UUID bossUuid, int bossId, Vec3 playerPosition,
                                       Vec3 bossPosition, int durationTicks, boolean control, boolean handView) implements CustomPacketPayload {
    public ArmiBossCinematicPayload(UUID bossUuid, int bossId, Vec3 playerPosition,
                                   Vec3 bossPosition, int durationTicks, boolean control) {
        this(bossUuid, bossId, playerPosition, bossPosition, durationTicks, control, false);
    }
    public static final Type<ArmiBossCinematicPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(AsmpThingsMod.MOD_ID, "armi_boss_cinematic"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ArmiBossCinematicPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> {
                buffer.writeUUID(payload.bossUuid());
                buffer.writeVarInt(payload.bossId());
                buffer.writeVec3(payload.playerPosition());
                buffer.writeVec3(payload.bossPosition());
                buffer.writeVarInt(payload.durationTicks());
                buffer.writeBoolean(payload.control());
                buffer.writeBoolean(payload.handView());
            },
            buffer -> new ArmiBossCinematicPayload(buffer.readUUID(), buffer.readVarInt(),
                    buffer.readVec3(), buffer.readVec3(), buffer.readVarInt(), buffer.readBoolean(), buffer.readBoolean()));

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
