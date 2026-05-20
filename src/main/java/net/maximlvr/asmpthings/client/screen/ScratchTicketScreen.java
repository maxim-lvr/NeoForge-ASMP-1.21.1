package net.maximlvr.asmpthings.client.screen;

import net.maximlvr.asmpthings.AsmpThingsMod;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.maximlvr.asmpthings.component.ModDataComponents;
import net.maximlvr.asmpthings.network.payload.ScratchTicketScratchPayload;
import net.neoforged.neoforge.network.PacketDistributor;

public class ScratchTicketScreen extends Screen {
    private final ItemStack stack;

    private static final ResourceLocation CARD_TEXTURE_0 =
            ResourceLocation.fromNamespaceAndPath(
                    AsmpThingsMod.MOD_ID,
                    "textures/gui/scratch_ticket/card_goal_small_0.png"
            );

    private static final ResourceLocation CARD_TEXTURE_1 =
            ResourceLocation.fromNamespaceAndPath(
                    AsmpThingsMod.MOD_ID,
                    "textures/gui/scratch_ticket/card_goal_small_1.png"
            );

    private static final ResourceLocation CARD_TEXTURE_5 =
            ResourceLocation.fromNamespaceAndPath(
                    AsmpThingsMod.MOD_ID,
                    "textures/gui/scratch_ticket/card_goal_small_5.png"
            );

    private static final ResourceLocation CARD_TEXTURE_10 =
            ResourceLocation.fromNamespaceAndPath(
                    AsmpThingsMod.MOD_ID,
                    "textures/gui/scratch_ticket/card_goal_small_10.png"
            );

    private ResourceLocation getCardTexture() {
        int prize = stack.getOrDefault(ModDataComponents.SCRATCH_PRIZE, 0);

        return switch (prize) {
            case 1 -> CARD_TEXTURE_1;
            case 5 -> CARD_TEXTURE_5;
            case 10 -> CARD_TEXTURE_10;
            default -> CARD_TEXTURE_0;
        };
    }

    private static final ResourceLocation SCRATCH_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(
                    AsmpThingsMod.MOD_ID,
                    "textures/gui/scratch_ticket/card_goal_small_scratch.png"
            );

    private static final int CARD_TEXTURE_WIDTH = 530;
    private static final int CARD_TEXTURE_HEIGHT = 657;

    private static final int CARD_RENDER_WIDTH = 160;
    private static final int CARD_RENDER_HEIGHT = 198;

    // Zone grattable dans l'image originale 530x657
    // De x=150, y=260 à x=380, y=500
    private static final int SCRATCH_TEXTURE_X = 150;
    private static final int SCRATCH_TEXTURE_Y = 260;
    private static final int SCRATCH_TEXTURE_WIDTH = 230;
    private static final int SCRATCH_TEXTURE_HEIGHT = 240;

    private static final int GRID_COLS = 64;
    private static final int GRID_ROWS = 64;

    private static final int BRUSH_RADIUS = 6;

    private final boolean[][] scratchedPixels = new boolean[GRID_COLS][GRID_ROWS];

    public ScratchTicketScreen(ItemStack stack) {
        super(Component.literal("Scratch Ticket"));
        this.stack = stack;

        loadScratchData();
    }

    private void loadScratchData() {
        String data = stack.getOrDefault(ModDataComponents.SCRATCH_DATA, "");

        if (data.length() != GRID_COLS * GRID_ROWS) {
            return;
        }

        for (int row = 0; row < GRID_ROWS; row++) {
            for (int col = 0; col < GRID_COLS; col++) {
                int index = row * GRID_COLS + col;
                scratchedPixels[col][row] = data.charAt(index) == '1';
            }
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void renderBlurredBackground(float partialTick) {
        // Désactive le flou derrière le screen
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        int cardX = (this.width - CARD_RENDER_WIDTH) / 2;
        int cardY = (this.height - CARD_RENDER_HEIGHT) / 2;

        int scratchX = getScratchRenderX(cardX);
        int scratchY = getScratchRenderY(cardY);
        int scratchWidth = getScratchRenderWidth();
        int scratchHeight = getScratchRenderHeight();

        // Image principale du ticket dessous : source 530x657, affichée en 160x198
        guiGraphics.blit(
                getCardTexture(),
                cardX,
                cardY,
                CARD_RENDER_WIDTH,
                CARD_RENDER_HEIGHT,
                0,
                0,
                CARD_TEXTURE_WIDTH,
                CARD_TEXTURE_HEIGHT,
                CARD_TEXTURE_WIDTH,
                CARD_TEXTURE_HEIGHT
        );

        float cellWidth = (float) scratchWidth / GRID_COLS;
        float cellHeight = (float) scratchHeight / GRID_ROWS;

        // Image scratch par-dessus, uniquement dans la zone grattable
        for (int row = 0; row < GRID_ROWS; row++) {
            for (int col = 0; col < GRID_COLS; col++) {
                if (!scratchedPixels[col][row]) {
                    int x = scratchX + Math.round(col * cellWidth);
                    int y = scratchY + Math.round(row * cellHeight);

                    int nextX = scratchX + Math.round((col + 1) * cellWidth);
                    int nextY = scratchY + Math.round((row + 1) * cellHeight);

                    int drawWidth = Math.max(1, nextX - x);
                    int drawHeight = Math.max(1, nextY - y);

                    int u = SCRATCH_TEXTURE_X + Math.round(col * ((float) SCRATCH_TEXTURE_WIDTH / GRID_COLS));
                    int v = SCRATCH_TEXTURE_Y + Math.round(row * ((float) SCRATCH_TEXTURE_HEIGHT / GRID_ROWS));

                    int nextU = SCRATCH_TEXTURE_X + Math.round((col + 1) * ((float) SCRATCH_TEXTURE_WIDTH / GRID_COLS));
                    int nextV = SCRATCH_TEXTURE_Y + Math.round((row + 1) * ((float) SCRATCH_TEXTURE_HEIGHT / GRID_ROWS));

                    int sourceWidth = Math.max(1, nextU - u);
                    int sourceHeight = Math.max(1, nextV - v);

                    guiGraphics.blit(
                            SCRATCH_TEXTURE,
                            x,
                            y,
                            drawWidth,
                            drawHeight,
                            u,
                            v,
                            sourceWidth,
                            sourceHeight,
                            CARD_TEXTURE_WIDTH,
                            CARD_TEXTURE_HEIGHT
                    );
                }
            }
        }

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            scratchAt(mouseX, mouseY);
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button == 0) {
            scratchAt(mouseX, mouseY);
            return true;
        }

        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    private void scratchAt(double mouseX, double mouseY) {
        int cardX = (this.width - CARD_RENDER_WIDTH) / 2;
        int cardY = (this.height - CARD_RENDER_HEIGHT) / 2;

        int scratchX = getScratchRenderX(cardX);
        int scratchY = getScratchRenderY(cardY);
        int scratchWidth = getScratchRenderWidth();
        int scratchHeight = getScratchRenderHeight();

        int relativeX = (int) mouseX - scratchX;
        int relativeY = (int) mouseY - scratchY;

        if (relativeX < 0 || relativeY < 0) {
            return;
        }

        if (relativeX >= scratchWidth || relativeY >= scratchHeight) {
            return;
        }

        for (int row = 0; row < GRID_ROWS; row++) {
            for (int col = 0; col < GRID_COLS; col++) {
                float cellCenterX = (col + 0.5f) * scratchWidth / GRID_COLS;
                float cellCenterY = (row + 0.5f) * scratchHeight / GRID_ROWS;

                float dx = cellCenterX - relativeX;
                float dy = cellCenterY - relativeY;

                if (dx * dx + dy * dy <= BRUSH_RADIUS * BRUSH_RADIUS) {
                    if (!scratchedPixels[col][row]) {
                        scratchedPixels[col][row] = true;

                        int index = row * GRID_COLS + col;
                        PacketDistributor.sendToServer(new ScratchTicketScratchPayload(index));
                    }
                }
            }
        }
    }

    private int getScratchRenderX(int cardX) {
        return cardX + Math.round(SCRATCH_TEXTURE_X * ((float) CARD_RENDER_WIDTH / CARD_TEXTURE_WIDTH));
    }

    private int getScratchRenderY(int cardY) {
        return cardY + Math.round(SCRATCH_TEXTURE_Y * ((float) CARD_RENDER_HEIGHT / CARD_TEXTURE_HEIGHT));
    }

    private int getScratchRenderWidth() {
        return Math.round(SCRATCH_TEXTURE_WIDTH * ((float) CARD_RENDER_WIDTH / CARD_TEXTURE_WIDTH));
    }

    private int getScratchRenderHeight() {
        return Math.round(SCRATCH_TEXTURE_HEIGHT * ((float) CARD_RENDER_HEIGHT / CARD_TEXTURE_HEIGHT));
    }
}