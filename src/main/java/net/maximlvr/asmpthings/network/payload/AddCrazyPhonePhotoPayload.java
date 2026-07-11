package net.maximlvr.asmpthings.network.payload;

import net.maximlvr.asmpthings.AsmpThingsMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record AddCrazyPhonePhotoPayload(boolean mainHand, String title, String texture, String photoType) implements CustomPacketPayload {
    public static final Type<AddCrazyPhonePhotoPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(AsmpThingsMod.MOD_ID, "add_crazy_phone_photo"));

    public static final StreamCodec<RegistryFriendlyByteBuf, AddCrazyPhonePhotoPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.BOOL,
                    AddCrazyPhonePhotoPayload::mainHand,
                    ByteBufCodecs.STRING_UTF8,
                    AddCrazyPhonePhotoPayload::title,
                    ByteBufCodecs.STRING_UTF8,
                    AddCrazyPhonePhotoPayload::texture,
                    ByteBufCodecs.STRING_UTF8,
                    AddCrazyPhonePhotoPayload::photoType,
                    AddCrazyPhonePhotoPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
