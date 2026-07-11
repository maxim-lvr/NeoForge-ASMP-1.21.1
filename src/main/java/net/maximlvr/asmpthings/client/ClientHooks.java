package net.maximlvr.asmpthings.client;

import net.maximlvr.asmpthings.client.screen.ScratchTicketScreen;
import net.maximlvr.asmpthings.client.screen.CrazyPhoneScreen;
import net.maximlvr.asmpthings.client.screen.BankScreen;
import net.maximlvr.asmpthings.client.screen.CardReaderConfigScreen;
import net.maximlvr.asmpthings.client.screen.CardReaderPinScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;

public class ClientHooks {
    public static void openScratchTicketScreen(ItemStack stack) {
        Minecraft.getInstance().setScreen(new ScratchTicketScreen(stack));
    }

    public static void openCrazyPhoneScreen(ItemStack stack, boolean mainHand) {
        Minecraft.getInstance().setScreen(new CrazyPhoneScreen(stack, mainHand));
    }

    public static void openBankScreen() {
        Minecraft.getInstance().setScreen(new BankScreen());
    }

    public static void openCardReaderConfigScreen(BlockPos pos, String targetAccountId, int amount) {
        Minecraft.getInstance().setScreen(new CardReaderConfigScreen(pos, targetAccountId, amount));
    }

    public static void openCardReaderPinScreen(BlockPos pos) {
        Minecraft.getInstance().setScreen(new CardReaderPinScreen(pos));
    }
}
