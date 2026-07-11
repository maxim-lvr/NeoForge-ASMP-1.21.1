package net.maximlvr.asmpthings.network.payload;

import net.maximlvr.asmpthings.AsmpThingsMod;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record OpenCardReaderPinPayload(BlockPos pos) implements CustomPacketPayload {
    public static final Type<OpenCardReaderPinPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(AsmpThingsMod.MOD_ID, "open_card_reader_pin"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenCardReaderPinPayload> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC,
                    OpenCardReaderPinPayload::pos,
                    OpenCardReaderPinPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
