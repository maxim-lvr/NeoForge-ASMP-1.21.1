package net.maximlvr.asmpthings.client.screen;

import net.maximlvr.asmpthings.network.payload.SubmitCardReaderPinPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

public class CardReaderPinScreen extends Screen {
    private final BlockPos pos;
    private EditBox pinField;

    public CardReaderPinScreen(BlockPos pos) {
        super(Component.literal("Code carte"));
        this.pos = pos;
    }

    @Override
    protected void init() {
        int x = width / 2 - 60;
        int y = height / 2 - 24;

        pinField = new EditBox(font, x, y, 120, 18, Component.literal("Code"));
        pinField.setMaxLength(6);
        pinField.setFilter(value -> value.isEmpty() || value.matches("\\d{0,6}"));
        addRenderableWidget(pinField);

        addRenderableWidget(Button.builder(Component.literal("Valider"), button -> submit())
                .bounds(x, y + 28, 120, 20)
                .build());
    }

    private void submit() {
        PacketDistributor.sendToServer(new SubmitCardReaderPinPayload(pos, isMainHandCard(), pinField.getValue()));
        onClose();
    }

    private boolean isMainHandCard() {
        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.player == null) {
            return true;
        }

        ItemStack mainHand = minecraft.player.getItemInHand(InteractionHand.MAIN_HAND);
        return net.maximlvr.asmpthings.item.ModItems.isBankCard(mainHand);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.drawString(font, "Code de la carte", width / 2 - 60, height / 2 - 38, 0xFFFFFF);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    protected void renderBlurredBackground(float partialTick) {
        // Disable the vanilla blur behind this interface.
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
