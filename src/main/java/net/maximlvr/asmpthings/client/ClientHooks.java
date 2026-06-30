package net.maximlvr.asmpthings.client;

import net.maximlvr.asmpthings.client.screen.ScratchTicketScreen;
import net.maximlvr.asmpthings.client.screen.CrazyPhoneScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;

public class ClientHooks {
    public static void openScratchTicketScreen(ItemStack stack) {
        Minecraft.getInstance().setScreen(new ScratchTicketScreen(stack));
    }

    public static void openCrazyPhoneScreen(ItemStack stack, boolean mainHand) {
        Minecraft.getInstance().setScreen(new CrazyPhoneScreen(stack, mainHand));
    }
}
