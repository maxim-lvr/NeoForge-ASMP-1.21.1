package net.maximlvr.asmpthings.network.payload;

import net.maximlvr.asmpthings.AsmpThingsMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record OpenBattlePreviewPayload() implements CustomPacketPayload {
    public static final Type<OpenBattlePreviewPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(AsmpThingsMod.MOD_ID, "open_battle_preview"));
    public static final StreamCodec<RegistryFriendlyByteBuf, OpenBattlePreviewPayload> STREAM_CODEC =
            StreamCodec.unit(new OpenBattlePreviewPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
