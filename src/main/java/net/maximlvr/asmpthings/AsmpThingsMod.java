package net.maximlvr.asmpthings;

import net.maximlvr.asmpthings.block.ModBlocks;
import net.maximlvr.asmpthings.component.ModDataComponents;
import net.maximlvr.asmpthings.item.ModCreativeModeTabs;
import net.maximlvr.asmpthings.item.ModItems;
import net.maximlvr.asmpthings.network.ModNetworking;
import net.minecraft.world.item.CreativeModeTabs;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(AsmpThingsMod.MOD_ID)
public class AsmpThingsMod {
    public static final String MOD_ID = "asmpthingsmod";
    public static final Logger LOGGER = LogUtils.getLogger();

    public AsmpThingsMod(IEventBus modEventBus, ModContainer modContainer) {

        modEventBus.addListener(this::commonSetup);
        NeoForge.EVENT_BUS.register(this);

        ModCreativeModeTabs.register(modEventBus);

        ModItems.register(modEventBus);
        ModDataComponents.register(modEventBus);
        ModNetworking.register(modEventBus);
        ModBlocks.register(modEventBus);



        modEventBus.addListener(this::addCreative);
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(FMLCommonSetupEvent event) {

    }

    // Add the example block item to the building blocks tab
    private void addCreative(BuildCreativeModeTabContentsEvent event) {

    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {

    }
}
