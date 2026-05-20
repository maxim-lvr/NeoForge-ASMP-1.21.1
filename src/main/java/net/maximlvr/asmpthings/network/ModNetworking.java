package net.maximlvr.asmpthings.network;

import net.maximlvr.asmpthings.component.ModDataComponents;
import net.maximlvr.asmpthings.item.ModItems;
import net.maximlvr.asmpthings.network.payload.OpenScratchTicketPayload;
import net.maximlvr.asmpthings.network.payload.ScratchTicketScratchPayload;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public class ModNetworking {
    private static final int GRID_COLS = 64;
    private static final int GRID_ROWS = 64;
    private static final int TOTAL_CELLS = GRID_COLS * GRID_ROWS;

    public static void register(IEventBus eventBus) {
        eventBus.addListener(ModNetworking::registerPayloads);
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("1");

        registrar.playToServer(
                ScratchTicketScratchPayload.TYPE,
                ScratchTicketScratchPayload.STREAM_CODEC,
                (payload, context) -> {
                    context.enqueueWork(() -> {
                        var player = context.player();

                        ItemStack stack = player.getMainHandItem();

                        if (!stack.is(ModItems.GOAL_SMALL_TICKET.get())) {
                            stack = player.getOffhandItem();
                        }

                        if (!stack.is(ModItems.GOAL_SMALL_TICKET.get())) {
                            return;
                        }

                        int index = payload.index();

                        if (index < 0 || index >= TOTAL_CELLS) {
                            return;
                        }

                        String data = stack.getOrDefault(ModDataComponents.SCRATCH_DATA, "");

                        if (data.length() != TOTAL_CELLS) {
                            data = "0".repeat(TOTAL_CELLS);
                        }

                        char[] chars = data.toCharArray();
                        chars[index] = '1';

                        stack.set(ModDataComponents.SCRATCH_DATA, new String(chars));
                    });
                }
        );

        registrar.playToClient(
                OpenScratchTicketPayload.TYPE,
                OpenScratchTicketPayload.STREAM_CODEC,
                (payload, context) -> {
                    context.enqueueWork(() -> {
                        var player = net.minecraft.client.Minecraft.getInstance().player;

                        if (player == null) {
                            return;
                        }

                        ItemStack stack = payload.mainHand()
                                ? player.getMainHandItem()
                                : player.getOffhandItem();

                        stack.set(ModDataComponents.SCRATCH_PRIZE, payload.prize());

                        net.maximlvr.asmpthings.client.ClientHooks.openScratchTicketScreen(stack);
                    });
                }
        );
    }
}