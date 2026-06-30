package net.maximlvr.asmpthings.network.payload;

import net.maximlvr.asmpthings.AsmpThingsMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record WerewolfGameStatePayload(boolean active, boolean night) implements CustomPacketPayload {
    public static final Type<WerewolfGameStatePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(AsmpThingsMod.MOD_ID, "werewolf_game_state"));

    public static final StreamCodec<RegistryFriendlyByteBuf, WerewolfGameStatePayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.BOOL,
                    WerewolfGameStatePayload::active,
                    ByteBufCodecs.BOOL,
                    WerewolfGameStatePayload::night,
                    WerewolfGameStatePayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
