package net.maximlvr.asmpthings.network.payload;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record SendCrazyPhoneMessagePayload(boolean mainHand, String contactNumber, String message) implements CustomPacketPayload {
    public static final Type<SendCrazyPhoneMessagePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("asmpthingsmod", "send_crazy_phone_message"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SendCrazyPhoneMessagePayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.BOOL,
                    SendCrazyPhoneMessagePayload::mainHand,
                    ByteBufCodecs.STRING_UTF8,
                    SendCrazyPhoneMessagePayload::contactNumber,
                    ByteBufCodecs.STRING_UTF8,
                    SendCrazyPhoneMessagePayload::message,
                    SendCrazyPhoneMessagePayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
