package net.maximlvr.asmpthings.network.payload;

import net.maximlvr.asmpthings.AsmpThingsMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record CrazyPhoneSetupResultPayload(boolean success, String message) implements CustomPacketPayload {
    public static final Type<CrazyPhoneSetupResultPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(AsmpThingsMod.MOD_ID, "crazy_phone_setup_result"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CrazyPhoneSetupResultPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.BOOL,
                    CrazyPhoneSetupResultPayload::success,
                    ByteBufCodecs.STRING_UTF8,
                    CrazyPhoneSetupResultPayload::message,
                    CrazyPhoneSetupResultPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
