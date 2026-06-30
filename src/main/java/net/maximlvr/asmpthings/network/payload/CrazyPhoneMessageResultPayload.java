package net.maximlvr.asmpthings.network.payload;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record CrazyPhoneMessageResultPayload(boolean success, String contactNumber, String messageText, String status) implements CustomPacketPayload {
    public static final Type<CrazyPhoneMessageResultPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("asmpthingsmod", "crazy_phone_message_result"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CrazyPhoneMessageResultPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.BOOL,
                    CrazyPhoneMessageResultPayload::success,
                    ByteBufCodecs.STRING_UTF8,
                    CrazyPhoneMessageResultPayload::contactNumber,
                    ByteBufCodecs.STRING_UTF8,
                    CrazyPhoneMessageResultPayload::messageText,
                    ByteBufCodecs.STRING_UTF8,
                    CrazyPhoneMessageResultPayload::status,
                    CrazyPhoneMessageResultPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
