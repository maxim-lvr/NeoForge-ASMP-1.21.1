package net.maximlvr.asmpthings.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.maximlvr.asmpthings.AsmpThingsMod;
import net.maximlvr.asmpthings.item.custom.ChargedGunItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemDisplayContext;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;

@EventBusSubscriber(modid = AsmpThingsMod.MOD_ID, value = Dist.CLIENT)
public final class ChargedGunEffects {
    private static final ResourceLocation CIRCLE = ResourceLocation.fromNamespaceAndPath(
            AsmpThingsMod.MOD_ID, "textures/item/circle_spell.png");

    private ChargedGunEffects() {}

    @SubscribeEvent
    public static void renderHand(RenderHandEvent event) {
        Minecraft client = Minecraft.getInstance();
        var player = client.player;
        if (player == null || !player.isUsingItem() || player.getUsedItemHand() != event.getHand()
                || !(event.getItemStack().getItem() instanceof ChargedGunItem)) return;

        event.setCanceled(true);
        boolean left = (event.getHand() == InteractionHand.MAIN_HAND ? player.getMainArm()
                : player.getMainArm().getOpposite()) == HumanoidArm.LEFT;
        ItemDisplayContext context = left ? ItemDisplayContext.FIRST_PERSON_LEFT_HAND : ItemDisplayContext.FIRST_PERSON_RIGHT_HAND;
        float ticks = player.getTicksUsingItem() + event.getPartialTick();
        float progress = Mth.clamp(ticks / ChargedGunItem.CHARGE_TICKS, 0, 1);
        PoseStack pose = event.getPoseStack();
        pose.pushPose();
        try {
            // Vanilla NONE-use hand pose, followed by a small charge-dependent vibration.
            pose.translate(left ? -0.56 : 0.56, -0.52 - event.getEquipProgress() * 0.6, -0.72);
            float amplitude = 0.002F + progress * 0.009F;
            pose.translate(Mth.sin(ticks * 2.7F) * amplitude, Mth.cos(ticks * 3.3F) * amplitude, 0);
            client.getItemRenderer().renderStatic(player, event.getItemStack(), context, left, pose,
                    event.getMultiBufferSource(), player.level(), event.getPackedLight(), OverlayTexture.NO_OVERLAY, player.getId());

            // Reuse the model's exported display transform so circles stay attached to the barrel.
            var model = client.getItemRenderer().getModel(event.getItemStack(), player.level(), player, player.getId());
            model.applyTransform(context, pose, left);
            pose.translate(0, 4.25 / 16.0, -15.5 / 16.0);
            var vertices = event.getMultiBufferSource().getBuffer(RenderType.entityTranslucentEmissive(CIRCLE));
            renderRings(pose, vertices, ticks, progress, -1);
        } finally {
            pose.popPose();
        }
    }

    public static final class ThirdPersonLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
        public ThirdPersonLayer(PlayerRenderer renderer) {
            super(renderer);
        }

        @Override
        public void render(PoseStack pose, MultiBufferSource buffers, int light, AbstractClientPlayer player,
                           float swing, float amount, float partial, float age, float yaw, float pitch) {
            if (!player.isUsingItem() || player.isInvisible()
                    || !(player.getUseItem().getItem() instanceof ChargedGunItem)) return;
            HumanoidArm arm = player.getUsedItemHand() == InteractionHand.MAIN_HAND
                    ? player.getMainArm() : player.getMainArm().getOpposite();
            boolean left = arm == HumanoidArm.LEFT;
            ItemDisplayContext context = left ? ItemDisplayContext.THIRD_PERSON_LEFT_HAND : ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;
            pose.pushPose();
            try {
                // Same hand and model transforms as Minecraft's ItemInHandLayer.
                getParentModel().translateToHand(arm, pose);
                pose.mulPose(Axis.XP.rotationDegrees(-90));
                pose.mulPose(Axis.YP.rotationDegrees(180));
                pose.translate((left ? -1 : 1) / 16F, 0.125, -0.625);
                var model = Minecraft.getInstance().getItemRenderer().getModel(player.getUseItem(), player.level(), player, player.getId());
                model.applyTransform(context, pose, left);
                // Barrel anchor in Blockbench coordinates (8, 13.75, -6.75), relative to the item center.
                pose.translate(0, 5.75 / 16.0, -14.75 / 16.0);
                // Smaller rings and tighter spacing in third person; first person stays unchanged.
                float circleScale = 0.6F / 0.275F;
                pose.scale(circleScale, circleScale, circleScale);
                float ticks = player.getTicksUsingItem() + partial;
                renderRings(pose, buffers.getBuffer(RenderType.entityTranslucentEmissive(CIRCLE)), ticks,
                        Mth.clamp(ticks / ChargedGunItem.CHARGE_TICKS, 0, 1), -1);
            } finally {
                pose.popPose();
            }
        }
    }

    private static void renderRings(PoseStack pose, VertexConsumer vertices, float ticks, float progress, int direction) {
        for (int ring = 0; ring < 3; ring++) {
            pose.pushPose();
            pose.translate(0, 0, direction * (0.1 + ring * 0.32));
            pose.mulPose(Axis.ZP.rotationDegrees(ticks * (ring % 2 == 0 ? 5 : -7) + ring * 30));
            float radius = (0.42F - ring * 0.1F) * (0.8F + progress * 0.2F);
            int alpha = (int) (100 + 155 * progress);
            for (int corner = 0; corner < 4; corner++) {
                float x = corner == 0 || corner == 3 ? -radius : radius;
                float y = corner < 2 ? -radius : radius;
                vertices.addVertex(pose.last(), x, y, 0).setColor(255, 255, 255, alpha)
                        .setUv(x < 0 ? 0 : 1, y < 0 ? 1 : 0).setOverlay(OverlayTexture.NO_OVERLAY)
                        .setLight(LightTexture.FULL_BRIGHT).setNormal(pose.last(), 0, 0, 1);
            }
            pose.popPose();
        }
    }
}
