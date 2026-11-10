package net.maximlvr.asmpthings.client;

import net.maximlvr.asmpthings.AsmpThingsMod;
import net.maximlvr.asmpthings.network.payload.GunLaserPayload;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;

import java.util.ArrayDeque;
import java.util.Deque;

@EventBusSubscriber(modid = AsmpThingsMod.MOD_ID, value = Dist.CLIENT)
public final class GunLaserRenderer {
    private record Trace(GunLaserPayload payload, long expiresAt) {}
    private static final Deque<Trace> TRACES = new ArrayDeque<>();
    private static ClientLevel traceLevel;

    private GunLaserRenderer() {}

    public static void add(GunLaserPayload payload) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level != traceLevel) {
            TRACES.clear();
            traceLevel = level;
        }
        if (level == null || !level.dimension().location().equals(payload.dimension())) return;
        if (TRACES.size() >= 128) TRACES.removeFirst();
        TRACES.addLast(new Trace(payload, Util.getMillis() + 180));
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        TRACES.clear();
        traceLevel = null;
    }

    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;
        Minecraft client = Minecraft.getInstance();
        if (client.level != traceLevel) {
            TRACES.clear();
            traceLevel = client.level;
        }
        long now = Util.getMillis();
        TRACES.removeIf(trace -> trace.expiresAt() <= now);
        if (TRACES.isEmpty()) return;

        var pose = event.getPoseStack();
        var camera = event.getCamera().getPosition();
        var buffers = client.renderBuffers().bufferSource();
        var vertices = buffers.getBuffer(RenderType.lightning());
        pose.pushPose();
        try {
            for (Trace trace : TRACES) {
                var start = trace.payload().start().subtract(camera);
                var end = trace.payload().end().subtract(camera);
                beam(pose.last(), vertices, start, end, 0.045, 90);
                beam(pose.last(), vertices, start, end, 0.018, 255);
            }
            buffers.endBatch(RenderType.lightning());
        } finally {
            pose.popPose();
        }
    }

    private static void beam(PoseStack.Pose pose, VertexConsumer vertices, Vec3 start, Vec3 end, double radius, int alpha) {
        Vec3 axis = end.subtract(start).normalize();
        Vec3 side = axis.cross(Math.abs(axis.y) > 0.9 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0)).normalize().scale(radius);
        Vec3 up = axis.cross(side);
        Vec3[] offsets = {side.add(up), side.subtract(up), side.add(up).scale(-1), up.subtract(side)};
        for (int face = 0; face < 4; face++) {
            Vec3 a = offsets[face], b = offsets[(face + 1) % 4];
            for (Vec3 point : new Vec3[]{start.add(a), end.add(a), end.add(b), start.add(b)}) {
                vertices.addVertex(pose, (float) point.x, (float) point.y, (float) point.z).setColor(255, 8, 4, alpha);
            }
        }
    }
}
