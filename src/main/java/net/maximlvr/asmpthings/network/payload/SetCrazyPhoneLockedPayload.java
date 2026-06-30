package net.maximlvr.asmpthings.network.payload;

import net.maximlvr.asmpthings.AsmpThingsMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record SetCrazyPhoneLockedPayload(boolean mainHand, boolean locked) implements CustomPacketPayload {
    public static final Type<SetCrazyPhoneLockedPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(AsmpThingsMod.MOD_ID, "set_crazy_phone_locked"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SetCrazyPhoneLockedPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.BOOL,
                    SetCrazyPhoneLockedPayload::mainHand,
                    ByteBufCodecs.BOOL,
                    SetCrazyPhoneLockedPayload::locked,
                    SetCrazyPhoneLockedPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
