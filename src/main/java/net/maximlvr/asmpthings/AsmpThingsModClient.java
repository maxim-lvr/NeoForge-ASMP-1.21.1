package net.maximlvr.asmpthings;

import net.maximlvr.asmpthings.block.entity.ModBlockEntities;
import net.maximlvr.asmpthings.client.render.BankBlockEntityRenderer;
import net.maximlvr.asmpthings.client.render.CubeBossArrowRenderer;
import net.maximlvr.asmpthings.client.render.CubeBossBeamRenderer;
import net.maximlvr.asmpthings.client.render.CubeBossRenderer;
import net.maximlvr.asmpthings.client.render.CubeBossSwordRenderer;
import net.maximlvr.asmpthings.client.render.SkeleteShopTestRenderer;
import net.maximlvr.asmpthings.entity.ModEntities;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.maximlvr.asmpthings.client.model.CubeBossArrowModel;
import net.maximlvr.asmpthings.client.model.CubeBossModel;
import net.maximlvr.asmpthings.client.model.SkeleteShopWipTestModel;

@Mod(value = AsmpThingsMod.MOD_ID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = AsmpThingsMod.MOD_ID, value = Dist.CLIENT)
public class AsmpThingsModClient {
    public AsmpThingsModClient(ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.CUBE_BOSS.get(), CubeBossRenderer::new);
        event.registerEntityRenderer(ModEntities.CUBE_BOSS_SWORD.get(), CubeBossSwordRenderer::new);
        event.registerEntityRenderer(ModEntities.CUBE_BOSS_BEAM.get(), CubeBossBeamRenderer::new);
        event.registerEntityRenderer(ModEntities.CUBE_BOSS_ARROW.get(), CubeBossArrowRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.BANK.get(), BankBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.SKELETE_SHOP_TEST.get(), SkeleteShopTestRenderer::new);
    }

    @SubscribeEvent
    public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(CubeBossModel.LAYER_LOCATION, CubeBossModel::createBodyLayer);
        event.registerLayerDefinition(CubeBossArrowModel.LAYER_LOCATION, CubeBossArrowModel::createBodyLayer);
        event.registerLayerDefinition(SkeleteShopWipTestModel.LAYER_LOCATION, SkeleteShopWipTestModel::createBodyLayer);
    }
}
