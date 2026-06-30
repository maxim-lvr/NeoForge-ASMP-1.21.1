package net.maximlvr.asmpthings.network.payload;

import net.maximlvr.asmpthings.AsmpThingsMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record SelectWerewolfKillTargetPayload(String playerUuid) implements CustomPacketPayload {
    public static final Type<SelectWerewolfKillTargetPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(AsmpThingsMod.MOD_ID, "select_werewolf_kill_target"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SelectWerewolfKillTargetPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8,
                    SelectWerewolfKillTargetPayload::playerUuid,
                    SelectWerewolfKillTargetPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
