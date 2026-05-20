package net.maximlvr.asmpthings.network.payload;

import net.maximlvr.asmpthings.AsmpThingsMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record OpenScratchTicketPayload(boolean mainHand, int prize) implements CustomPacketPayload {
    public static final Type<OpenScratchTicketPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(AsmpThingsMod.MOD_ID, "open_scratch_ticket"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenScratchTicketPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.BOOL,
                    OpenScratchTicketPayload::mainHand,
                    ByteBufCodecs.VAR_INT,
                    OpenScratchTicketPayload::prize,
                    OpenScratchTicketPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}