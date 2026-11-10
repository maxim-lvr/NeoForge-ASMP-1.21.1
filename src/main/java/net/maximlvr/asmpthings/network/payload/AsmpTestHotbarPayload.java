package net.maximlvr.asmpthings.network.payload;

import net.maximlvr.asmpthings.AsmpThingsMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import java.util.List;
import java.util.ArrayList;

public record AsmpTestHotbarPayload(List<ItemStack> items) implements CustomPacketPayload {
    public static final Type<AsmpTestHotbarPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(AsmpThingsMod.MOD_ID, "test_hotbar"));
    public static final StreamCodec<RegistryFriendlyByteBuf, AsmpTestHotbarPayload> STREAM_CODEC = StreamCodec.of(
            (b, p) -> { for (int i = 0; i < 9; i++) ItemStack.OPTIONAL_STREAM_CODEC.encode(b, p.items.get(i)); },
            b -> { var items = new ArrayList<ItemStack>(9); for (int i = 0; i < 9; i++) items.add(ItemStack.OPTIONAL_STREAM_CODEC.decode(b)); return new AsmpTestHotbarPayload(items); });
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
