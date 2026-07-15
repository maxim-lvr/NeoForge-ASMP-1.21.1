package net.maximlvr.asmpthings.client.screen;

import net.maximlvr.asmpthings.AsmpThingsMod;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.maximlvr.asmpthings.component.ModDataComponents;
import net.maximlvr.asmpthings.item.custom.ScratchTicketPrize;
import net.maximlvr.asmpthings.network.payload.ScratchTicketScratchPayload;
import net.neoforged.neoforge.network.PacketDistributor;

public class ScratchTicketScreen extends Screen {
    private final ItemStack stack;

    private static final ResourceLocation CARD_TEXTURE_LOST = cardTexture("card_goal_perdu.png");
    private static final ResourceLocation CARD_TEXTURE_CHIENGUE = cardTexture("card_goal_chiengue.png");
    private static final ResourceLocation CARD_TEXTURE_KOMBUCIAO = cardTexture("card_goal_kombuciao.png");
    private static final ResourceLocation CARD_TEXTURE_2_COINS = cardTexture("card_goal_2coins.png");
    private static final ResourceLocation CARD_TEXTURE_BOUTEILLE = cardTexture("card_goal_bouteille.png");
    private static final ResourceLocation CARD_TEXTURE_5_COINS = cardTexture("card_goal_5coins.png");
    private static final ResourceLocation CARD_TEXTURE_DIAMOND = cardTexture("card_goal_diamant.png");
    private static final ResourceLocation CARD_TEXTURE_IGNITIUM = cardTexture("card_goal_ignitium.png");
    private static final ResourceLocation CARD_TEXTURE_DISC = cardTexture("card_goal_disque.png");
    private static final ResourceLocation CARD_TEXTURE_BOUEE = cardTexture("card_goal_bouee.png");
    private static final ResourceLocation CARD_TEXTURE_CARDS = cardTexture("card_goal_base_set.png");
    private static final ResourceLocation CARD_TEXTURE_PELUCHE = cardTexture("card_goal_peluche.png");
    private static final ResourceLocation CARD_TEXTURE_10_COINS = cardTexture("card_goal_10coins.png");
    private static final ResourceLocation CARD_TEXTURE_64_COINS = cardTexture("card_goal_64coins.png");

    private ResourceLocation getCardTexture() {
        int prize = stack.getOrDefault(ModDataComponents.SCRATCH_PRIZE, 0);

        return switch (prize) {
            case ScratchTicketPrize.CHIENGUE -> CARD_TEXTURE_CHIENGUE;
            case ScratchTicketPrize.KOMBUCIAO -> CARD_TEXTURE_KOMBUCIAO;
            case ScratchTicketPrize.TWO_COINS -> CARD_TEXTURE_2_COINS;
            case ScratchTicketPrize.BOUTEILLE -> CARD_TEXTURE_BOUTEILLE;
            case ScratchTicketPrize.FIVE_COINS -> CARD_TEXTURE_5_COINS;
            case ScratchTicketPrize.DIAMOND_BLOCK -> CARD_TEXTURE_DIAMOND;
            case ScratchTicketPrize.IGNITIUM_BLOCK -> CARD_TEXTURE_IGNITIUM;
            case ScratchTicketPrize.DISC -> CARD_TEXTURE_DISC;
            case ScratchTicketPrize.BOUEE -> CARD_TEXTURE_BOUEE;
            case ScratchTicketPrize.CARDS -> CARD_TEXTURE_CARDS;
            case ScratchTicketPrize.PELUCHE -> CARD_TEXTURE_PELUCHE;
            case ScratchTicketPrize.TEN_COINS -> CARD_TEXTURE_10_COINS;
            case ScratchTicketPrize.STACK_COINS -> CARD_TEXTURE_64_COINS;
            default -> CARD_TEXTURE_LOST;
        };
    }

    private static final ResourceLocation SCRATCH_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(
                    AsmpThingsMod.MOD_ID,
                    "textures/gui/scratch_ticket/card_goal_small_scratch.png"
            );

    private static final int CARD_TEXTURE_WIDTH = 760;
    private static final int CARD_TEXTURE_HEIGHT = 1000;

    private static final int CARD_RENDER_WIDTH = 160;
    private static final int CARD_RENDER_HEIGHT = Math.round(CARD_RENDER_WIDTH * ((float) CARD_TEXTURE_HEIGHT / CARD_TEXTURE_WIDTH));

    private static final ScratchArea[] SCRATCH_AREAS = {
            new ScratchArea(150, 348, 450, 430),
            new ScratchArea(30, 889, 700, 88)
    };

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

        // Image principale du ticket dessous : source 760x1000, affichée en 160x211
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

        float cellWidth = (float) CARD_RENDER_WIDTH / GRID_COLS;
        float cellHeight = (float) CARD_RENDER_HEIGHT / GRID_ROWS;

        // Image scratch par-dessus, uniquement dans la zone grattable
        for (int row = 0; row < GRID_ROWS; row++) {
            for (int col = 0; col < GRID_COLS; col++) {
                if (!scratchedPixels[col][row] && isScratchableCell(col, row)) {
                    int x = cardX + Math.round(col * cellWidth);
                    int y = cardY + Math.round(row * cellHeight);

                    int nextX = cardX + Math.round((col + 1) * cellWidth);
                    int nextY = cardY + Math.round((row + 1) * cellHeight);

                    int drawWidth = Math.max(1, nextX - x);
                    int drawHeight = Math.max(1, nextY - y);

                    int u = Math.round(col * ((float) CARD_TEXTURE_WIDTH / GRID_COLS));
                    int v = Math.round(row * ((float) CARD_TEXTURE_HEIGHT / GRID_ROWS));

                    int nextU = Math.round((col + 1) * ((float) CARD_TEXTURE_WIDTH / GRID_COLS));
                    int nextV = Math.round((row + 1) * ((float) CARD_TEXTURE_HEIGHT / GRID_ROWS));

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

        int relativeX = (int) mouseX - cardX;
        int relativeY = (int) mouseY - cardY;

        if (relativeX < 0 || relativeY < 0) {
            return;
        }

        if (relativeX >= CARD_RENDER_WIDTH || relativeY >= CARD_RENDER_HEIGHT) {
            return;
        }

        if (!isScratchableRenderPoint(relativeX, relativeY)) {
            return;
        }

        for (int row = 0; row < GRID_ROWS; row++) {
            for (int col = 0; col < GRID_COLS; col++) {
                if (!isScratchableCell(col, row)) {
                    continue;
                }

                float cellCenterX = (col + 0.5f) * CARD_RENDER_WIDTH / GRID_COLS;
                float cellCenterY = (row + 0.5f) * CARD_RENDER_HEIGHT / GRID_ROWS;

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

    private static ResourceLocation cardTexture(String fileName) {
        return ResourceLocation.fromNamespaceAndPath(
                AsmpThingsMod.MOD_ID,
                "textures/gui/scratch_ticket/" + fileName
        );
    }

    private static boolean isScratchableCell(int col, int row) {
        float textureX = (col + 0.5f) * CARD_TEXTURE_WIDTH / GRID_COLS;
        float textureY = (row + 0.5f) * CARD_TEXTURE_HEIGHT / GRID_ROWS;

        return isScratchableTexturePoint(textureX, textureY);
    }

    private static boolean isScratchableRenderPoint(float relativeX, float relativeY) {
        float textureX = relativeX * CARD_TEXTURE_WIDTH / CARD_RENDER_WIDTH;
        float textureY = relativeY * CARD_TEXTURE_HEIGHT / CARD_RENDER_HEIGHT;

        return isScratchableTexturePoint(textureX, textureY);
    }

    private static boolean isScratchableTexturePoint(float textureX, float textureY) {
        for (ScratchArea area : SCRATCH_AREAS) {
            if (area.contains(textureX, textureY)) {
                return true;
            }
        }

        return false;
    }

    private record ScratchArea(int x, int y, int width, int height) {
        boolean contains(float textureX, float textureY) {
            return textureX >= x
                    && textureY >= y
                    && textureX < x + width
                    && textureY < y + height;
        }
    }
}
