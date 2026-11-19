package net.maximlvr.asmpthings.pet;

import net.maximlvr.asmpthings.AsmpThingsMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public final class PetNetworking {
    public record OpenMenu() implements CustomPacketPayload {
        public static final Type<OpenMenu> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(AsmpThingsMod.MOD_ID, "open_pet"));
        public static final StreamCodec<RegistryFriendlyByteBuf, OpenMenu> CODEC = StreamCodec.unit(new OpenMenu());
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }
    static void register(IEventBus bus) { bus.addListener(PetNetworking::payloads); }
    private static void payloads(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToServer(OpenMenu.TYPE, OpenMenu.CODEC, (payload, context) -> context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player && player.isAlive() && !player.isSpectator()
                    && !(player.containerMenu instanceof PetMenu))
                player.openMenu(new SimpleMenuProvider((id, inventory, owner) -> new PetMenu(id, inventory),
                        Component.translatable("menu.asmpthingsmod.pet")));
        }));
    }
    private PetNetworking() {}
}
