package net.maximlvr.asmpthings.client.screen;

import net.maximlvr.asmpthings.client.ArmiBossCinematicClient;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class ArmiBossControlScreen extends Screen {
    public ArmiBossControlScreen() {
        super(Component.literal("Armi Boss"));
    }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            ArmiBossCinematicClient.clickBlock(mouseX / width, mouseY / height);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void onClose() {
        ArmiBossCinematicClient.exitControl();
    }
}
