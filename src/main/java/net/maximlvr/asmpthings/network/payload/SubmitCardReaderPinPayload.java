package net.maximlvr.asmpthings.network.payload;

import net.maximlvr.asmpthings.AsmpThingsMod;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record SubmitCardReaderPinPayload(BlockPos pos, boolean mainHand, String pin) implements CustomPacketPayload {
    public static final Type<SubmitCardReaderPinPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(AsmpThingsMod.MOD_ID, "submit_card_reader_pin"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SubmitCardReaderPinPayload> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC,
                    SubmitCardReaderPinPayload::pos,
                    ByteBufCodecs.BOOL,
                    SubmitCardReaderPinPayload::mainHand,
                    ByteBufCodecs.STRING_UTF8,
                    SubmitCardReaderPinPayload::pin,
                    SubmitCardReaderPinPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
