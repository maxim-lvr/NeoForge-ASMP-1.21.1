package net.maximlvr.asmpthings.client;

import net.maximlvr.asmpthings.AsmpThingsMod;
import net.maximlvr.asmpthings.network.payload.AsmpTestInputPayload;
import net.maximlvr.asmpthings.network.payload.AsmpTestViewPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = AsmpThingsMod.MOD_ID, value = Dist.CLIENT)
public final class AsmpTestClient {
    private static AsmpTestViewPayload view;
    private static float yaw, pitch;
    private static int missingTicks;
    private static int controlSlot;
    private static long inputTick;
    private static final AsmpTestRemoteInput remoteInput = new AsmpTestRemoteInput();
    private static java.util.List<net.minecraft.world.item.ItemStack> hotbar = java.util.List.of();

    public static void hotbar(net.maximlvr.asmpthings.network.payload.AsmpTestHotbarPayload payload) {
        if (controlling()) hotbar = payload.items();
    }

    public static void handle(AsmpTestViewPayload payload) {
        clearInput();
        view = payload.targetId() < 0 ? null : payload;
        missingTicks = 0;
        hotbar = java.util.List.of();
        var target = target();
        if (target != null) { yaw = target.getYRot(); pitch = target.getXRot(); }
        controlSlot = 0;
    }

    public static void relay(net.maximlvr.asmpthings.network.payload.AsmpTestRelayPayload payload) {
        if (controlling() && view.targetUuid().equals(payload.controller())) {
            remoteInput.accept(payload.input(), inputTick);
        }
    }

    public static boolean sprintDown() {
        var input = remoteInput.current(inputTick);
        return input != null && input.has(AsmpTestInputPayload.SPRINT);
    }

    private static void clearInput() {
        remoteInput.clear();
        var player = Minecraft.getInstance().player;
        if (controlling() && player != null) {
            remoteInput.apply(player.input, inputTick, 1);
            player.setSprinting(false);
            player.setShiftKeyDown(false);
        }
    }

    public static Entity target() {
        var level = Minecraft.getInstance().level;
        if (view == null || level == null) return null;
        var entity = level.getEntity(view.targetId());
        return entity != null && entity.getUUID().equals(view.targetUuid()) ? entity : null;
    }

    public static boolean controlling() { return view != null && view.control(); }

    public static void turn(double x, double y) {
        yaw += (float) x * 0.15F;
        pitch = Math.clamp(pitch + (float) y * 0.15F, -90, 90);
    }

    public static float viewYaw() { return controlling() ? yaw : target().getYRot(); }
    public static float viewPitch() { return controlling() ? pitch : target().getXRot(); }

    private static void send(float forward, float left, int flags) {
        var minecraft = Minecraft.getInstance();
        if (view != null && minecraft.player != null && minecraft.getConnection() != null)
            PacketDistributor.sendToServer(new AsmpTestInputPayload(forward, left, yaw, pitch, flags, controlSlot));
    }

    private static void stop() { send(0, 0, AsmpTestInputPayload.STOP); clearInput(); view = null; }

    @SubscribeEvent public static void movement(MovementInputUpdateEvent event) {
        if (!controlling()) return;
        var minecraft = Minecraft.getInstance();
        var input = event.getInput();
        // Read physical bindings, never the remote inputs applied to this body's Input instance.
        int flags = (minecraft.options.keyJump.isDown() ? AsmpTestInputPayload.JUMP : 0)
                | (minecraft.options.keyShift.isDown() ? AsmpTestInputPayload.SNEAK : 0)
                | (minecraft.options.keySprint.isDown() ? AsmpTestInputPayload.SPRINT : 0)
                | (minecraft.options.keyAttack.isDown() ? AsmpTestInputPayload.ATTACK : 0)
                | (minecraft.options.keyUse.isDown() ? AsmpTestInputPayload.USE : 0);
        float forward = (minecraft.options.keyUp.isDown() ? 1 : 0) - (minecraft.options.keyDown.isDown() ? 1 : 0);
        float left = (minecraft.options.keyLeft.isDown() ? 1 : 0) - (minecraft.options.keyRight.isDown() ? 1 : 0);
        send(minecraft.screen == null ? forward : 0, minecraft.screen == null ? left : 0,
                minecraft.screen == null ? flags : 0);
        var player = minecraft.player;
        var remote = remoteInput.current(inputTick);
        if (remote != null) {
            player.setYRot(remote.yaw());
            player.setXRot(remote.pitch());
            player.setYHeadRot(remote.yaw());
            player.getInventory().selected = remote.slot();
        }
        float slowdown = player.isMovingSlowly()
                ? (float) player.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.SNEAKING_SPEED) : 1;
        remoteInput.apply(input, inputTick, slowdown);
    }

    @SubscribeEvent public static void interaction(InputEvent.InteractionKeyMappingTriggered event) {
        if (controlling()) { event.setCanceled(true); event.setSwingHand(false); }
    }

    @SubscribeEvent public static void inventory(ScreenEvent.Opening event) {
        if (controlling() && event.getNewScreen() instanceof InventoryScreen) {
            event.setCanceled(true);
            send(0, 0, AsmpTestInputPayload.INVENTORY);
        }
    }

    @SubscribeEvent public static void key(InputEvent.Key event) {
        if (controlling() && Minecraft.getInstance().screen == null && event.getAction() == GLFW.GLFW_PRESS) {
            for (int i = 0; i < 9; i++) {
                if (Minecraft.getInstance().options.keyHotbarSlots[i].matches(event.getKey(), event.getScanCode())) controlSlot = i;
            }
        }
        if (view != null && event.getKey() == GLFW.GLFW_KEY_ESCAPE && event.getAction() == GLFW.GLFW_PRESS
                && Minecraft.getInstance().screen == null) stop();
    }

    @SubscribeEvent public static void scroll(InputEvent.MouseScrollingEvent event) {
        if (controlling() && Minecraft.getInstance().screen == null) {
            controlSlot = Math.floorMod(controlSlot - (int) Math.signum(event.getScrollDeltaY()), 9);
            event.setCanceled(true);
        }
    }

    @SubscribeEvent public static void beforeTick(ClientTickEvent.Pre event) { inputTick++; }

    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        if (view == null) return;
        if (target() == null) { if (++missingTicks > 40) stop(); }
        else missingTicks = 0;
        if (ArmiBossCinematicClient.isActive()) stop();
    }

    @SubscribeEvent public static void hand(RenderHandEvent event) { if (view != null) event.setCanceled(true); }
    @SubscribeEvent public static void hud(RenderGuiLayerEvent.Pre event) {
        if (!controlling()) return;
        if (event.getName().equals(net.neoforged.neoforge.client.gui.VanillaGuiLayers.SELECTED_ITEM_NAME)) {
            event.setCanceled(true); return;
        }
        if (!event.getName().equals(net.neoforged.neoforge.client.gui.VanillaGuiLayers.HOTBAR)) return;
        event.setCanceled(true);
        var minecraft = Minecraft.getInstance();
        var graphics = event.getGuiGraphics();
        int x = graphics.guiWidth() / 2 - 91, y = graphics.guiHeight() - 22;
        graphics.blitSprite(net.minecraft.resources.ResourceLocation.withDefaultNamespace("hud/hotbar"), x, y, 182, 22);
        graphics.blitSprite(net.minecraft.resources.ResourceLocation.withDefaultNamespace("hud/hotbar_selection"),
                x - 1 + controlSlot * 20, y - 1, 24, 23);
        for (int i = 0; i < hotbar.size(); i++) {
            graphics.renderItem(hotbar.get(i), x + 3 + i * 20, y + 3);
            graphics.renderItemDecorations(minecraft.font, hotbar.get(i), x + 3 + i * 20, y + 3);
        }
    }
    @SubscribeEvent public static void body(RenderPlayerEvent.Pre event) { if (event.getEntity() == target()) event.setCanceled(true); }
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event) { clearInput(); view = null; hotbar = java.util.List.of(); }
}
