package net.maximlvr.asmpthings.network.payload;

import net.maximlvr.asmpthings.AsmpThingsMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record CrazyPhoneCameraAlbumActionPayload(
        boolean mainHand,
        String action,
        int albumIndex,
        String name,
        int uploadCount
) implements CustomPacketPayload {
    public static final Type<CrazyPhoneCameraAlbumActionPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(AsmpThingsMod.MOD_ID, "crazy_phone_camera_album_action"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CrazyPhoneCameraAlbumActionPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.BOOL,
                    CrazyPhoneCameraAlbumActionPayload::mainHand,
                    ByteBufCodecs.STRING_UTF8,
                    CrazyPhoneCameraAlbumActionPayload::action,
                    ByteBufCodecs.INT,
                    CrazyPhoneCameraAlbumActionPayload::albumIndex,
                    ByteBufCodecs.STRING_UTF8,
                    CrazyPhoneCameraAlbumActionPayload::name,
                    ByteBufCodecs.INT,
                    CrazyPhoneCameraAlbumActionPayload::uploadCount,
                    CrazyPhoneCameraAlbumActionPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
