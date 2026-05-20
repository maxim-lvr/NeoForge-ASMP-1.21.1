package net.maximlvr.asmpthings.client;

import net.maximlvr.asmpthings.client.screen.ScratchTicketScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;

public class ClientHooks {
    public static void openScratchTicketScreen(ItemStack stack) {
        Minecraft.getInstance().setScreen(new ScratchTicketScreen(stack));
    }
}