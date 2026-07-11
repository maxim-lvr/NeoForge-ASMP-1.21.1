package net.maximlvr.asmpthings.network.payload;

import net.maximlvr.asmpthings.AsmpThingsMod;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record OpenCardReaderConfigPayload(BlockPos pos, String targetAccountId, int amount) implements CustomPacketPayload {
    public static final Type<OpenCardReaderConfigPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(AsmpThingsMod.MOD_ID, "open_card_reader_config"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenCardReaderConfigPayload> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC,
                    OpenCardReaderConfigPayload::pos,
                    ByteBufCodecs.STRING_UTF8,
                    OpenCardReaderConfigPayload::targetAccountId,
                    ByteBufCodecs.VAR_INT,
                    OpenCardReaderConfigPayload::amount,
                    OpenCardReaderConfigPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
