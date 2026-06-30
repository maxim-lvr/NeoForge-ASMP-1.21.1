package net.maximlvr.asmpthings.network.payload;

import net.maximlvr.asmpthings.AsmpThingsMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record AddCrazyPhoneContactByNumberPayload(boolean mainHand, String name, String number) implements CustomPacketPayload {
    public static final Type<AddCrazyPhoneContactByNumberPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(AsmpThingsMod.MOD_ID, "add_crazy_phone_contact_by_number"));

    public static final StreamCodec<RegistryFriendlyByteBuf, AddCrazyPhoneContactByNumberPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.BOOL,
                    AddCrazyPhoneContactByNumberPayload::mainHand,
                    ByteBufCodecs.STRING_UTF8,
                    AddCrazyPhoneContactByNumberPayload::name,
                    ByteBufCodecs.STRING_UTF8,
                    AddCrazyPhoneContactByNumberPayload::number,
                    AddCrazyPhoneContactByNumberPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
