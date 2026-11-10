package net.maximlvr.asmpthings.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.maximlvr.asmpthings.AsmpThingsMod;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class BattlePreviewScreen extends Screen {
    private static final ResourceLocation BACKGROUND = ResourceLocation.fromNamespaceAndPath(
            AsmpThingsMod.MOD_ID, "textures/gui/battle_preview/background.png");
    private static final ResourceLocation BOSS = ResourceLocation.fromNamespaceAndPath(
            AsmpThingsMod.MOD_ID, "textures/gui/battle_preview/boss.png");
    private final long closesAt = Util.getMillis() + 5_000L;

    private BattlePreviewScreen() {
        super(Component.literal("Combat 2D"));
    }

    public static void open() {
        Minecraft client = Minecraft.getInstance();
        if (client.player != null && !(client.screen instanceof BattlePreviewScreen)) {
            client.setScreen(new BattlePreviewScreen());
        }
    }

    @Override
    public void tick() {
        if (Util.getMillis() >= closesAt) {
            onClose();
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // The supplied scenery replaces the world's usual blurred screen background.
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.blit(BACKGROUND, 0, 0, width, height, 0, 0, 600, 360, 600, 360);
        int floorY = Math.round(height * 0.75F);
        int bossHeight = Math.round(Math.min(height * 0.60F, width * 0.40F * 467 / 395));
        int bossWidth = Math.round(bossHeight * 395F / 467);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        graphics.blit(BOSS, Math.round(width * 0.76F) - bossWidth / 2, floorY - bossHeight,
                bossWidth, bossHeight, 0, 0, 395, 467, 395, 467);
        RenderSystem.disableBlend();
        renderPlayer(graphics, floorY);
    }

    private void renderPlayer(GuiGraphics graphics, int floorY) {
        var player = minecraft.player;
        if (player == null) {
            return;
        }
        float body = player.yBodyRot;
        float oldBody = player.yBodyRotO;
        float yaw = player.getYRot();
        float pitch = player.getXRot();
        float head = player.yHeadRot;
        float oldHead = player.yHeadRotO;
        try {
            // Fixed profile towards the boss; restore world-facing rotations after drawing.
            player.yBodyRot = player.yBodyRotO = 90;
            player.setYRot(90);
            player.setXRot(0);
            player.yHeadRot = player.yHeadRotO = 90;
            float scale = Math.min(height * 0.32F, width * 0.22F) / player.getBbHeight();
            InventoryScreen.renderEntityInInventory(graphics, width * 0.23F, floorY, scale,
                    new Vector3f(), new Quaternionf().rotateZ((float) Math.PI), new Quaternionf(), player);
        } finally {
            player.yBodyRot = body;
            player.yBodyRotO = oldBody;
            player.setYRot(yaw);
            player.setXRot(pitch);
            player.yHeadRot = head;
            player.yHeadRotO = oldHead;
        }
    }
}
