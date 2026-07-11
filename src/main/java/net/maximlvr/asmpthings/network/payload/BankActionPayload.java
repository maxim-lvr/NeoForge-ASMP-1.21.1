package net.maximlvr.asmpthings.network.payload;

import net.maximlvr.asmpthings.AsmpThingsMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record BankActionPayload(String action, String accountId, String targetAccountId, String text, int amount, String pin) implements CustomPacketPayload {
    public static final Type<BankActionPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(AsmpThingsMod.MOD_ID, "bank_action"));

    public static final StreamCodec<RegistryFriendlyByteBuf, BankActionPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8,
                    BankActionPayload::action,
                    ByteBufCodecs.STRING_UTF8,
                    BankActionPayload::accountId,
                    ByteBufCodecs.STRING_UTF8,
                    BankActionPayload::targetAccountId,
                    ByteBufCodecs.STRING_UTF8,
                    BankActionPayload::text,
                    ByteBufCodecs.VAR_INT,
                    BankActionPayload::amount,
                    ByteBufCodecs.STRING_UTF8,
                    BankActionPayload::pin,
                    BankActionPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
