package net.maximlvr.asmpthings.client;

import com.mojang.blaze3d.shaders.FogShape;
import net.maximlvr.asmpthings.AsmpThingsMod;
import net.maximlvr.asmpthings.entity.custom.ArmiBossEntity;
import net.maximlvr.asmpthings.entity.custom.ArmiBossGeometry;
import net.maximlvr.asmpthings.entity.custom.ArmiBossPlatforms;
import net.minecraft.world.phys.AABB;
import net.maximlvr.asmpthings.network.payload.ArmiBossCinematicPayload;
import net.maximlvr.asmpthings.network.payload.ArmiBossMovePayload;
import net.maximlvr.asmpthings.network.payload.ArmiBossPathPayload;
import net.maximlvr.asmpthings.client.screen.ArmiBossControlScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.material.FogType;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = AsmpThingsMod.MOD_ID, value = Dist.CLIENT)
public final class ArmiBossCinematicClient {
    private static final double FOV = 60.0;
    private static ArmiBossCinematicPayload sequence;
    private static ClientLevel sequenceLevel;
    private static long startTick;
    private static CameraFrame fixedFrame;
    private static int platformColors = -1;
    private static final ArmiBossAutoWalk autoWalk = new ArmiBossAutoWalk();

    private ArmiBossCinematicClient() {
    }

    public static void handle(ArmiBossCinematicPayload payload) {
        if (payload.durationTicks() <= 0) {
            if (sequence != null && sequence.bossUuid().equals(payload.bossUuid())) {
                clear();
            }
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) {
            return;
        }
        boolean sameSequence = sequence != null && sequence.bossUuid().equals(payload.bossUuid());
        sequence = payload;
        sequenceLevel = minecraft.level;
        ArmiBossPlatforms.setClientCenter(payload.bossPosition());
        if (!sameSequence) {
            startTick = sequenceLevel.getGameTime();
            fixedFrame = null;
            autoWalk.clear();
        }
        if (payload.handView()) {
            fixedFrame = null;
            autoWalk.clear();
            if (minecraft.screen instanceof ArmiBossControlScreen) {
                minecraft.setScreen(null);
            }
        } else if (payload.control()) {
            fixedFrame = orbitFrame(1.0);
            minecraft.setScreen(new ArmiBossControlScreen());
        } else {
            holdPlayer();
        }
    }

    public static boolean isActive() {
        Minecraft minecraft = Minecraft.getInstance();
        if (sequence == null || minecraft.level != sequenceLevel || minecraft.player == null || !minecraft.player.isAlive()) {
            return false;
        }
        Entity boss = sequenceLevel.getEntity(sequence.bossId());
        return boss instanceof ArmiBossEntity && boss.isAlive() && boss.getUUID().equals(sequence.bossUuid())
                && (sequence.control() || sequence.handView() || sequenceLevel.getGameTime() - startTick <= sequence.durationTicks() + 40);
    }

    public static boolean locksControls() {
        return isActive() && (!sequence.handView() || isRescuing());
    }

    private static boolean isRescuing() {
        return sequence != null && sequenceLevel != null
                && sequenceLevel.getEntity(sequence.bossId()) instanceof ArmiBossEntity boss && boss.isRescuing();
    }

    public static boolean isViewingFrom(ArmiBossEntity boss) {
        return isActive() && sequence.handView() && sequence.bossUuid().equals(boss.getUUID());
    }

    public record CameraFrame(Vec3 position, float yaw, float pitch) {
    }

    public static CameraFrame cameraFrame(float partialTick) {
        if (sequence.handView()) {
            Entity boss = sequenceLevel.getEntity(sequence.bossId());
            Vec3 palm = ArmiBossGeometry.handBounds(boss.getPosition(partialTick), boss.getYRot()).getCenter();
            Vec3 target = Minecraft.getInstance().player.getPosition(partialTick).add(0, 1.0, 0);
            Vec3 direction = target.subtract(palm).normalize();
            // Place the lens just in front of the palm, outside its texture.
            Vec3 position = palm.add(direction.scale(ArmiBossGeometry.HAND_DEPTH * 0.5 + 0.4));
            return new CameraFrame(position,
                    (float) Math.toDegrees(Math.atan2(-direction.x, direction.z)),
                    (float) -Math.toDegrees(Math.atan2(direction.y, direction.horizontalDistance())));
        }
        if (fixedFrame != null) {
            return fixedFrame;
        }
        double progress = Math.clamp((sequenceLevel.getGameTime() - startTick + partialTick) / sequence.durationTicks(), 0.0, 1.0);
        return orbitFrame(progress);
    }

    private static CameraFrame orbitFrame(double progress) {
        Minecraft minecraft = Minecraft.getInstance();
        Vec3 player = sequence.playerPosition();
        Vec3 boss = sequence.bossPosition();
        Entity entity = sequenceLevel.getEntity(sequence.bossId());
        AABB hand = ArmiBossGeometry.handBounds(boss, entity == null ? 0 : entity.getYRot());
        // Frame the hand and the player's body, not the 60-block arm extending into the sky.
        AABB subjects = hand.inflate(20.0, 2.0, 20.0).minmax(new AABB(player.x - 0.5, player.y, player.z - 0.5,
                player.x + 0.5, player.y + 2, player.z + 0.5));
        Vec3 center = subjects.getCenter();
        double aspect = (double) minecraft.getWindow().getWidth() / Math.max(1, minecraft.getWindow().getHeight());
        double halfFov = Math.atan(Math.tan(Math.toRadians(FOV / 2)) * Math.min(1.0, aspect));
        // Fit a sphere enclosing both bodies, including on narrow windows.
        double subjectRadius = subjects.getMinPosition().distanceTo(subjects.getMaxPosition()) * 0.5;
        double radius = Math.max(6.0, subjectRadius / Math.sin(halfFov) * 1.1);
        // A quarter turn in five seconds: half the original angular speed.
        double rearRightAngle = Math.atan2(player.z - boss.z, player.x - boss.x) - Math.PI / 4;
        double angle = rearRightAngle - (1.0 - progress) * Math.PI / 2;
        double horizontalRadius = radius / Math.sqrt(2.0);
        Vec3 position = new Vec3(center.x + Math.cos(angle) * horizontalRadius,
                center.y + horizontalRadius, center.z + Math.sin(angle) * horizontalRadius);
        Vec3 direction = center.subtract(position);
        float yaw = (float) Math.toDegrees(Math.atan2(-direction.x, direction.z));
        float pitch = (float) -Math.toDegrees(Math.atan2(direction.y, direction.horizontalDistance()));
        return new CameraFrame(position, yaw, pitch);
    }

    private static void holdPlayer() {
        var player = Minecraft.getInstance().player;
        if (player == null || sequence == null) {
            return;
        }
        Vec3 delta = sequence.bossPosition().subtract(sequence.playerPosition());
        float yaw = (float) Math.toDegrees(Math.atan2(-delta.x, delta.z));
        player.setPos(sequence.playerPosition());
        player.setDeltaMovement(Vec3.ZERO);
        player.setYRot(yaw);
        player.setXRot(0.0F);
        player.setYHeadRot(yaw);
        player.setYBodyRot(yaw);
        player.setSprinting(false);
        player.fallDistance = 0;
    }

    private static void clear() {
        autoWalk.clear();
        sequence = null;
        sequenceLevel = null;
        fixedFrame = null;
        Minecraft minecraft = Minecraft.getInstance();
        ArmiBossPlatforms.setClientCenter(null);
        platformColors = -1;
        if (minecraft.screen instanceof ArmiBossControlScreen) {
            minecraft.setScreen(null);
        }
    }

    public static void exitControl() {
        if (sequence != null && Minecraft.getInstance().getConnection() != null) {
            PacketDistributor.sendToServer(new ArmiBossMovePayload(sequence.bossUuid(), BlockPos.ZERO, true));
        }
        clear();
    }

    public static void handlePath(ArmiBossPathPayload payload) {
        if (isActive() && sequence.control() && sequence.bossUuid().equals(payload.bossUuid())) {
            autoWalk.clear();
            autoWalk.setPath(payload.nodes());
        }
    }

    public static void clickBlock(double mouseX, double mouseY) {
        if (!isActive() || !sequence.control() || isRescuing()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        var camera = minecraft.gameRenderer.getMainCamera();
        double aspect = (double) minecraft.getWindow().getWidth() / Math.max(1, minecraft.getWindow().getHeight());
        double tangent = Math.tan(Math.toRadians(FOV / 2));
        Vec3 direction = new Vec3(camera.getLookVector())
                .add(new Vec3(camera.getUpVector()).scale((1 - 2 * mouseY) * tangent))
                .add(new Vec3(camera.getLeftVector()).scale((1 - 2 * mouseX) * tangent * aspect)).normalize();
        var hit = sequenceLevel.clip(new ClipContext(camera.getPosition(), camera.getPosition().add(direction.scale(160)),
                ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, minecraft.player));
        if (hit.getType() == HitResult.Type.BLOCK) {
            autoWalk.clear();
            PacketDistributor.sendToServer(new ArmiBossMovePayload(sequence.bossUuid(), hit.getBlockPos(), false));
        }
    }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        if (isActive()) {
            int colors = ArmiBossPlatforms.heldColors(Minecraft.getInstance().player);
            if (platformColors != colors) {
                platformColors = colors;
                autoWalk.clear();
            }
            if (isRescuing()) {
                autoWalk.clear();
                return;
            } else if (sequence.handView()) {
                return;
            } else if (!sequence.control()) {
                holdPlayer();
            } else if (!(Minecraft.getInstance().screen instanceof ArmiBossControlScreen)) {
                exitControl();
            }
        } else if (sequence != null && Minecraft.getInstance().level == sequenceLevel) {
            exitControl();
        } else {
            clear();
        }
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        clear();
    }


    @SubscribeEvent
    public static void onMovement(MovementInputUpdateEvent event) {
        if (locksControls()) {
            var input = event.getInput();
            input.forwardImpulse = 0;
            input.leftImpulse = 0;
            input.up = input.down = input.left = input.right = false;
            input.jumping = input.shiftKeyDown = false;
            if (sequence.control() && !isRescuing()) {
                var player = Minecraft.getInstance().player;
                if (player.getAbilities().flying) {
                    player.getAbilities().flying = false;
                    player.onUpdateAbilities();
                }
                autoWalk.applyInput(input);
            }
        }
    }

    @SubscribeEvent
    public static void onInteraction(InputEvent.InteractionKeyMappingTriggered event) {
        if (locksControls()) {
            event.setCanceled(true);
            event.setSwingHand(false);
        }
    }

    @SubscribeEvent
    public static void onHand(RenderHandEvent event) {
        if (isActive()) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onKey(InputEvent.Key event) {
        if (event.getKey() == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE
                && event.getAction() == org.lwjgl.glfw.GLFW.GLFW_PRESS
                && isActive() && sequence.handView()) {
            exitControl();
        }
    }

    @SubscribeEvent
    public static void onFov(ViewportEvent.ComputeFov event) {
        if (isActive()) {
            event.setFOV(FOV);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void onArenaFog(ViewportEvent.RenderFog event) {
        if (event.getType() == FogType.NONE && isActive()) {
            Vec3 camera = event.getCamera().getPosition();
            // Fog distances originate at the detached camera, not at the player.
            double arenaDistance = Math.max(camera.distanceTo(Minecraft.getInstance().player.position()),
                    camera.distanceTo(sequence.bossPosition()));
            event.setNearPlaneDistance((float) arenaDistance + 30.0F);
            event.setFarPlaneDistance((float) arenaDistance + 50.0F);
            event.setFogShape(FogShape.SPHERE);
            event.setCanceled(true);
        }
    }

}
