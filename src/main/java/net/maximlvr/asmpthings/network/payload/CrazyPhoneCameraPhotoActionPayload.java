package net.maximlvr.asmpthings.network.payload;

import net.maximlvr.asmpthings.AsmpThingsMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record CrazyPhoneCameraPhotoActionPayload(
        boolean mainHand,
        int albumIndex,
        String imageIndexes,
        String action,
        String contactNumber
) implements CustomPacketPayload {
    public static final Type<CrazyPhoneCameraPhotoActionPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(AsmpThingsMod.MOD_ID, "crazy_phone_camera_photo_action"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CrazyPhoneCameraPhotoActionPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.BOOL,
                    CrazyPhoneCameraPhotoActionPayload::mainHand,
                    ByteBufCodecs.INT,
                    CrazyPhoneCameraPhotoActionPayload::albumIndex,
                    ByteBufCodecs.STRING_UTF8,
                    CrazyPhoneCameraPhotoActionPayload::imageIndexes,
                    ByteBufCodecs.STRING_UTF8,
                    CrazyPhoneCameraPhotoActionPayload::action,
                    ByteBufCodecs.STRING_UTF8,
                    CrazyPhoneCameraPhotoActionPayload::contactNumber,
                    CrazyPhoneCameraPhotoActionPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
