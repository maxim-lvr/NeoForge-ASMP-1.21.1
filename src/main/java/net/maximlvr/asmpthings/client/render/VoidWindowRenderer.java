package net.maximlvr.asmpthings.client.render;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.maximlvr.asmpthings.AsmpThingsMod;
import net.maximlvr.asmpthings.block.entity.VoidWindowBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.TheEndPortalRenderer;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import org.joml.Matrix4f;
import net.minecraft.world.phys.Vec3;
import java.io.IOException;

/** Project the current vanilla sky onto cube faces, without drawing terrain into the snapshot. */
public final class VoidWindowRenderer extends TheEndPortalRenderer<VoidWindowBlockEntity> {
    private static TextureTarget sky;
    private static ShaderInstance shader;
    private static ShaderInstance mountainShader;
    private static Vec3 mountainOrigin;
    private static boolean requested = true, captured;
    private final boolean mountains;

    public VoidWindowRenderer(BlockEntityRendererProvider.Context context) {
        this(context, false);
    }

    public VoidWindowRenderer(BlockEntityRendererProvider.Context context, boolean mountains) {
        super(context);
        this.mountains = mountains;
    }

    public static void registerShaders(RegisterShadersEvent event) {
        try {
            event.registerShader(new ShaderInstance(event.getResourceProvider(),
                    ResourceLocation.fromNamespaceAndPath(AsmpThingsMod.MOD_ID, "sky_window"), DefaultVertexFormat.POSITION),
                    loaded -> shader = loaded);
            event.registerShader(new ShaderInstance(event.getResourceProvider(),
                    ResourceLocation.fromNamespaceAndPath(AsmpThingsMod.MOD_ID, "mountain_window"), DefaultVertexFormat.POSITION),
                    loaded -> mountainShader = loaded);
        } catch (IOException error) {
            throw new IllegalStateException("Impossible de charger le rendu de la fenêtre sur le ciel", error);
        }
    }

    public static void captureSky(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_SKY) return;
        captured = false;
        // Copy only while windows have been visible; first appearance requests the next frame.
        if (!requested) return;
        requested = false;
        RenderTarget main = Minecraft.getInstance().getMainRenderTarget();
        int oldRead = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
        int oldDraw = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
        try {
            if (sky == null) sky = new TextureTarget(main.width, main.height, false, Minecraft.ON_OSX);
            else if (sky.width != main.width || sky.height != main.height) sky.resize(main.width, main.height, Minecraft.ON_OSX);
            GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, main.frameBufferId);
            GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, sky.frameBufferId);
            GlStateManager._glBlitFrameBuffer(0, 0, main.width, main.height, 0, 0, sky.width, sky.height,
                    GL11.GL_COLOR_BUFFER_BIT, GL11.GL_NEAREST);
            captured = true;
            if (mountainShader != null && mountainOrigin != null) {
                Vec3 camera = event.getCamera().getPosition();
                Vec3 offset = camera.subtract(mountainOrigin);
                float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
                var viewer = event.getCamera().getEntity();
                // Keep vertical scenery relative to the viewer's feet, at every world altitude.
                // Horizontal displacement still drives the three layers' parallax.
                double eyeAboveFeet = viewer == null ? 1.62 : camera.y - viewer.getPosition(partialTick).y;
                mountainShader.safeGetUniform("CameraOffset").set((float) offset.x, (float) eyeAboveFeet, (float) offset.z);
                mountainShader.safeGetUniform("GroundLevel").set(0.0F);
                mountainShader.safeGetUniform("InverseViewProjection").set(new Matrix4f(event.getProjectionMatrix())
                        .mul(event.getModelViewMatrix()).invert());
                var level = Minecraft.getInstance().level;
                if (level != null) {
                    Vec3 color = level.getSkyColor(camera, partialTick);
                    mountainShader.safeGetUniform("SceneLight").set((float) Math.clamp((color.x + color.y + color.z) * 0.75, 0.12, 1.0));
                }
            }
        } finally {
            GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, oldRead);
            GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, oldDraw);
        }
    }

    public static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
        if (sky != null) { sky.destroyBuffers(); sky = null; }
        requested = true;
        captured = false;
        mountainOrigin = null;
    }

    @Override public void render(VoidWindowBlockEntity block, float partialTick, PoseStack poses,
                                 MultiBufferSource buffers, int light, int overlay) {
        requested = true;
        if (mountains && mountainOrigin == null) {
            var camera = Minecraft.getInstance().gameRenderer.getMainCamera();
            mountainOrigin = camera.getPosition();
            return;
        }
        if (captured && (mountains ? mountainShader : shader) != null) super.render(block, partialTick, poses, buffers, light, overlay);
    }

    @Override protected RenderType renderType() { return mountains ? SkyRenderType.MOUNTAINS : SkyRenderType.TYPE; }
    @Override protected float getOffsetUp() { return 1.0F; }
    @Override protected float getOffsetDown() { return 0.0F; }
    @Override public int getViewDistance() { return 192; }

    private static final class SkyRenderType extends RenderType {
        static final RenderType TYPE = windowType("asmp_sky_window", () -> shader);
        static final RenderType MOUNTAINS = windowType("asmp_mountain_window", () -> mountainShader);

        private static RenderType windowType(String name, java.util.function.Supplier<ShaderInstance> shaderSupplier) {
            return create(name, DefaultVertexFormat.POSITION, VertexFormat.Mode.QUADS, 1536,
                false, false, CompositeState.builder()
                        .setShaderState(new ShaderStateShard(shaderSupplier))
                        .setTextureState(new EmptyTextureStateShard(
                                () -> RenderSystem.setShaderTexture(0, sky == null ? 0 : sky.getColorTextureId()), () -> {}))
                        .createCompositeState(false));
        }

        private SkyRenderType() {
            super("asmp_sky_window", DefaultVertexFormat.POSITION, VertexFormat.Mode.QUADS, 1536,
                    false, false, () -> {}, () -> {});
        }
    }
}
