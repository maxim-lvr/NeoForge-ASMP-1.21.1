package net.maximlvr.asmpthings.network.payload;

import net.maximlvr.asmpthings.AsmpThingsMod;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record SaveCardReaderConfigPayload(BlockPos pos, String targetAccountId, int amount) implements CustomPacketPayload {
    public static final Type<SaveCardReaderConfigPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(AsmpThingsMod.MOD_ID, "save_card_reader_config"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SaveCardReaderConfigPayload> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC,
                    SaveCardReaderConfigPayload::pos,
                    ByteBufCodecs.STRING_UTF8,
                    SaveCardReaderConfigPayload::targetAccountId,
                    ByteBufCodecs.VAR_INT,
                    SaveCardReaderConfigPayload::amount,
                    SaveCardReaderConfigPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
