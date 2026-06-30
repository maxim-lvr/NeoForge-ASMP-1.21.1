package net.maximlvr.asmpthings.network.payload;

import net.maximlvr.asmpthings.AsmpThingsMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record SetupCrazyPhonePayload(boolean mainHand, String name, String number, String password) implements CustomPacketPayload {
    public static final Type<SetupCrazyPhonePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(AsmpThingsMod.MOD_ID, "setup_crazy_phone"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SetupCrazyPhonePayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.BOOL,
                    SetupCrazyPhonePayload::mainHand,
                    ByteBufCodecs.STRING_UTF8,
                    SetupCrazyPhonePayload::name,
                    ByteBufCodecs.STRING_UTF8,
                    SetupCrazyPhonePayload::number,
                    ByteBufCodecs.STRING_UTF8,
                    SetupCrazyPhonePayload::password,
                    SetupCrazyPhonePayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
