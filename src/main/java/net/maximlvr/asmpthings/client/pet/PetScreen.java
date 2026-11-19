package net.maximlvr.asmpthings.client.pet;

import net.maximlvr.asmpthings.pet.PetConfig;
import net.maximlvr.asmpthings.pet.PetKind;
import net.maximlvr.asmpthings.pet.PetMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import java.util.Locale;

public final class PetScreen extends AbstractContainerScreen<PetMenu> {
    public PetScreen(PetMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }
    @Override protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xff373737);
        graphics.fill(leftPos + 2, topPos + 2, leftPos + imageWidth - 2, topPos + imageHeight - 2, 0xffc6c6c6);
        for (var slot : menu.slots) PetClient.slotFrame(graphics, leftPos + slot.x, topPos + slot.y);
        if (minecraft.player != null)
            InventoryScreen.renderEntityInInventoryFollowsMouse(graphics, leftPos + 46, topPos + 8,
                    leftPos + 78, topPos + 73, 25, 0.0625F, mouseX, mouseY, minecraft.player);
    }
    @Override protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, 85, 7, 0x404040, false);
        PetKind kind = PetKind.fromStack(menu.equipped());
        if (kind == null) {
            graphics.drawWordWrap(font, Component.translatable("menu.asmpthingsmod.pet_empty"), 85, 23, 83, 0x404040);
            graphics.drawString(font, "P", 31, 30, 0xffdddddd, false);
        } else {
            graphics.drawWordWrap(font, Component.translatable(kind.nameKey()), 85, 21, 83, 0x303030);
            var bonus = PetConfig.bonus(kind);
            String health = String.format(Locale.ROOT, "%.0f", bonus.health().get());
            String armor = String.format(Locale.ROOT, "%.0f", bonus.armor().get());
            graphics.drawWordWrap(font, Component.translatable("menu.asmpthingsmod.pet_bonuses", health, armor), 85, 39, 83, 0x405040);
        }
    }
    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }
    @Override public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (PetClient.OPEN.matches(keyCode, scanCode)) { onClose(); return true; }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
