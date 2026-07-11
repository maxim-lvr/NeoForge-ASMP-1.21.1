package net.maximlvr.asmpthings;

import net.maximlvr.asmpthings.client.render.CrawlerRenderer;
import net.maximlvr.asmpthings.block.entity.ModBlockEntities;
import net.maximlvr.asmpthings.client.render.BankBlockEntityRenderer;
import net.maximlvr.asmpthings.entity.ModEntities;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;
import net.maximlvr.asmpthings.client.model.CrawlerModel;
import net.maximlvr.asmpthings.client.render.CrawlerHandRenderer;
import net.maximlvr.asmpthings.client.model.CrawlerHandModel;
import net.maximlvr.asmpthings.client.model.CrawlerArmModel;

// This class will not load on dedicated servers. Accessing client side code from here is safe.
@Mod(value = AsmpThingsMod.MOD_ID, dist = Dist.CLIENT)
// You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
@EventBusSubscriber(modid = AsmpThingsMod.MOD_ID, value = Dist.CLIENT)
public class AsmpThingsModClient {
    public AsmpThingsModClient(ModContainer container) {

        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {

    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.CRAWLER.get(), CrawlerRenderer::new);
        event.registerEntityRenderer(ModEntities.CRAWLER_HAND.get(), CrawlerHandRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.BANK.get(), BankBlockEntityRenderer::new);
    }

    @SubscribeEvent
    public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(CrawlerModel.LAYER_LOCATION, CrawlerModel::createBodyLayer);
        event.registerLayerDefinition(CrawlerHandModel.LAYER_LOCATION, CrawlerHandModel::createBodyLayer);
        event.registerLayerDefinition(CrawlerArmModel.LAYER_LOCATION, CrawlerArmModel::createBodyLayer);
    }
}
