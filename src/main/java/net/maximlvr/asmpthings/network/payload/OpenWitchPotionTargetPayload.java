package net.maximlvr.asmpthings.network.payload;

import net.maximlvr.asmpthings.AsmpThingsMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record OpenWitchPotionTargetPayload(String playersData, String potionKind) implements CustomPacketPayload {
    public static final Type<OpenWitchPotionTargetPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(AsmpThingsMod.MOD_ID, "open_witch_potion_target"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenWitchPotionTargetPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8,
                    OpenWitchPotionTargetPayload::playersData,
                    ByteBufCodecs.STRING_UTF8,
                    OpenWitchPotionTargetPayload::potionKind,
                    OpenWitchPotionTargetPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
