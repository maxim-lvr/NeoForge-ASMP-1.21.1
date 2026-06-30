package net.maximlvr.asmpthings.network.payload;

import net.maximlvr.asmpthings.AsmpThingsMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record CrazyPhoneContactResultPayload(boolean success, String uuid, String name, String number, String message) implements CustomPacketPayload {
    public static final Type<CrazyPhoneContactResultPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(AsmpThingsMod.MOD_ID, "crazy_phone_contact_result"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CrazyPhoneContactResultPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.BOOL,
                    CrazyPhoneContactResultPayload::success,
                    ByteBufCodecs.STRING_UTF8,
                    CrazyPhoneContactResultPayload::uuid,
                    ByteBufCodecs.STRING_UTF8,
                    CrazyPhoneContactResultPayload::name,
                    ByteBufCodecs.STRING_UTF8,
                    CrazyPhoneContactResultPayload::number,
                    ByteBufCodecs.STRING_UTF8,
                    CrazyPhoneContactResultPayload::message,
                    CrazyPhoneContactResultPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
