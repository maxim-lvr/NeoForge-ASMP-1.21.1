package net.maximlvr.asmpthings.network.payload;

import net.maximlvr.asmpthings.AsmpThingsMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record SendCrazyPhonePhotoPayload(
        boolean mainHand,
        String contactNumber,
        String title,
        String texture,
        String photoType
) implements CustomPacketPayload {

    public static final Type<SendCrazyPhonePhotoPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(AsmpThingsMod.MOD_ID, "send_crazy_phone_photo"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SendCrazyPhonePhotoPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.BOOL,
                    SendCrazyPhonePhotoPayload::mainHand,
                    ByteBufCodecs.STRING_UTF8,
                    SendCrazyPhonePhotoPayload::contactNumber,
                    ByteBufCodecs.STRING_UTF8,
                    SendCrazyPhonePhotoPayload::title,
                    ByteBufCodecs.STRING_UTF8,
                    SendCrazyPhonePhotoPayload::texture,
                    ByteBufCodecs.STRING_UTF8,
                    SendCrazyPhonePhotoPayload::photoType,
                    SendCrazyPhonePhotoPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}