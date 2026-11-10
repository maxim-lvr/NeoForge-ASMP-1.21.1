package net.maximlvr.asmpthings;

import com.mojang.blaze3d.shaders.FogShape;
import net.maximlvr.asmpthings.block.entity.ModBlockEntities;
import net.maximlvr.asmpthings.world.ModDimensions;
import net.minecraft.client.Minecraft;
import net.maximlvr.asmpthings.client.render.ArmiBossRenderer;
import net.maximlvr.asmpthings.client.model.ArmiBossModel;
import net.maximlvr.asmpthings.client.render.BankBlockEntityRenderer;
import net.maximlvr.asmpthings.client.render.CubeBossArrowRenderer;
import net.maximlvr.asmpthings.client.render.CubeBossBeamRenderer;
import net.maximlvr.asmpthings.client.render.CubeBossRenderer;
import net.maximlvr.asmpthings.client.render.CubeBossSwordRenderer;
import net.maximlvr.asmpthings.client.render.SkeleteShopTestRenderer;
import net.maximlvr.asmpthings.entity.ModEntities;
import net.minecraft.world.level.material.FogType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.maximlvr.asmpthings.client.model.CubeBossArrowModel;
import net.maximlvr.asmpthings.client.model.CubeBossModel;
import net.maximlvr.asmpthings.client.model.SkeleteShopWipTestModel;
import net.neoforged.neoforge.common.NeoForge;

@Mod(value = AsmpThingsMod.MOD_ID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = AsmpThingsMod.MOD_ID, value = Dist.CLIENT)
public class AsmpThingsModClient {
    public AsmpThingsModClient(ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        NeoForge.EVENT_BUS.addListener(AsmpThingsModClient::onRenderFog);
        NeoForge.EVENT_BUS.addListener(AsmpThingsModClient::onComputeFogColor);
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.UNDERTAKER.get(), net.maximlvr.asmpthings.client.render.UndertakerRenderer::new);
        event.registerEntityRenderer(ModEntities.GOLEM.get(), net.maximlvr.asmpthings.client.render.GolemRenderer::new);
        event.registerEntityRenderer(ModEntities.KRABER_SPHERE.get(), net.maximlvr.asmpthings.client.render.KraberSphereRenderer::new);
        event.registerEntityRenderer(ModEntities.DRONE.get(), net.maximlvr.asmpthings.client.render.DroneRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.ARMI_PLATFORM.get(), net.maximlvr.asmpthings.client.render.ArmiPlatformRenderer::new);
        event.registerEntityRenderer(ModEntities.ARMI_BOSS.get(), ArmiBossRenderer::new);
        event.registerEntityRenderer(ModEntities.CUBE_BOSS.get(), CubeBossRenderer::new);
        event.registerEntityRenderer(ModEntities.CUBE_BOSS_SWORD.get(), CubeBossSwordRenderer::new);
        event.registerEntityRenderer(ModEntities.CUBE_BOSS_BEAM.get(), CubeBossBeamRenderer::new);
        event.registerEntityRenderer(ModEntities.CUBE_BOSS_ARROW.get(), CubeBossArrowRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.BANK.get(), BankBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.SKELETE_SHOP_TEST.get(), SkeleteShopTestRenderer::new);
    }

    @SubscribeEvent
    public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(net.maximlvr.asmpthings.client.model.GolemModel.LAYER_LOCATION,
                net.maximlvr.asmpthings.client.model.GolemGeometry::createBodyLayer);
        event.registerLayerDefinition(net.maximlvr.asmpthings.client.model.DroneModel.LAYER_LOCATION,
                net.maximlvr.asmpthings.client.model.DroneModel::createBodyLayer);
        event.registerLayerDefinition(ArmiBossModel.LAYER_LOCATION, ArmiBossModel::createBodyLayer);
        event.registerLayerDefinition(CubeBossModel.LAYER_LOCATION, CubeBossModel::createBodyLayer);
        event.registerLayerDefinition(CubeBossArrowModel.LAYER_LOCATION, CubeBossArrowModel::createBodyLayer);
        event.registerLayerDefinition(SkeleteShopWipTestModel.LAYER_LOCATION, SkeleteShopWipTestModel::createBodyLayer);
    }

    @SubscribeEvent
    public static void addPlayerLayers(EntityRenderersEvent.AddLayers event) {
        for (var skin : event.getSkins()) {
            net.minecraft.client.renderer.entity.player.PlayerRenderer renderer = event.getSkin(skin);
            if (renderer != null) {
                renderer.addLayer(new net.maximlvr.asmpthings.client.ChargedGunEffects.ThirdPersonLayer(renderer));
            }
        }
    }

    private static void onRenderFog(ViewportEvent.RenderFog event) {
        if (!isInAsmpDimension() || event.getType() != FogType.NONE) {
            return;
        }

        event.setNearPlaneDistance(Math.min(event.getNearPlaneDistance(), 32.0F));
        event.setFarPlaneDistance(Math.min(event.getFarPlaneDistance(), 128.0F));
        event.setFogShape(FogShape.CYLINDER);
        event.setCanceled(true);
    }

    private static void onComputeFogColor(ViewportEvent.ComputeFogColor event) {
        if (!isInAsmpDimension()) {
            return;
        }

        event.setRed(0.045F);
        event.setGreen(0.05F);
        event.setBlue(0.065F);
    }

    private static boolean isInAsmpDimension() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.level != null && minecraft.level.dimension().equals(ModDimensions.ASMP_DIMENSION_KEY);
    }
}
