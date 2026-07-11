package net.maximlvr.asmpthings.client.screen;

import net.maximlvr.asmpthings.network.payload.SaveCardReaderConfigPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

public class CardReaderConfigScreen extends Screen {
    private final BlockPos pos;
    private final String targetAccountId;
    private final int amount;
    private EditBox accountField;
    private EditBox amountField;

    public CardReaderConfigScreen(BlockPos pos, String targetAccountId, int amount) {
        super(Component.literal("Lecteur carte"));
        this.pos = pos;
        this.targetAccountId = targetAccountId;
        this.amount = amount;
    }

    @Override
    protected void init() {
        int x = width / 2 - 80;
        int y = height / 2 - 42;

        accountField = new EditBox(font, x, y + 16, 160, 18, Component.literal("Compte receveur"));
        accountField.setMaxLength(4);
        accountField.setFilter(value -> value.isEmpty() || value.matches("\\d{0,4}"));
        accountField.setValue(targetAccountId);
        addRenderableWidget(accountField);

        amountField = new EditBox(font, x, y + 52, 160, 18, Component.literal("Montant"));
        amountField.setFilter(value -> value.isEmpty() || value.matches("\\d+"));
        amountField.setValue(String.valueOf(amount));
        addRenderableWidget(amountField);

        addRenderableWidget(Button.builder(Component.literal("Sauvegarder"), button -> save())
                .bounds(x, y + 82, 160, 20)
                .build());
    }

    private void save() {
        PacketDistributor.sendToServer(new SaveCardReaderConfigPayload(pos, accountField.getValue(), parseAmount()));
        onClose();
    }

    private int parseAmount() {
        try {
            return Integer.parseInt(amountField.getValue());
        } catch (NumberFormatException exception) {
            return 1;
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        int x = width / 2 - 80;
        int y = height / 2 - 42;
        guiGraphics.drawString(font, "Compte receveur", x, y + 4, 0xFFFFFF);
        guiGraphics.drawString(font, "Prix", x, y + 40, 0xFFFFFF);
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
