package net.maximlvr.asmpthings.network.payload;

import net.maximlvr.asmpthings.AsmpThingsMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record CrazyPhonePhotoResultPayload(boolean success, String message) implements CustomPacketPayload {
    public static final Type<CrazyPhonePhotoResultPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(AsmpThingsMod.MOD_ID, "crazy_phone_photo_result"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CrazyPhonePhotoResultPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.BOOL,
                    CrazyPhonePhotoResultPayload::success,
                    ByteBufCodecs.STRING_UTF8,
                    CrazyPhonePhotoResultPayload::message,
                    CrazyPhonePhotoResultPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
