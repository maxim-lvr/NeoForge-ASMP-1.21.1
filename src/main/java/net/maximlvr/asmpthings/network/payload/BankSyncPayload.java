package net.maximlvr.asmpthings.network.payload;

import net.maximlvr.asmpthings.AsmpThingsMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record BankSyncPayload(String accounts, String savedIbans, String selectedAccountId, int carriedCoins, String message) implements CustomPacketPayload {
    public static final Type<BankSyncPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(AsmpThingsMod.MOD_ID, "bank_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, BankSyncPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8,
                    BankSyncPayload::accounts,
                    ByteBufCodecs.STRING_UTF8,
                    BankSyncPayload::savedIbans,
                    ByteBufCodecs.STRING_UTF8,
                    BankSyncPayload::selectedAccountId,
                    ByteBufCodecs.VAR_INT,
                    BankSyncPayload::carriedCoins,
                    ByteBufCodecs.STRING_UTF8,
                    BankSyncPayload::message,
                    BankSyncPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
