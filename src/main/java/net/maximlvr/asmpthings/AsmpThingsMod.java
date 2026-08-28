package net.maximlvr.asmpthings;

import net.maximlvr.asmpthings.ai.AiNpcChatHandler;
import net.maximlvr.asmpthings.bank.BankEvents;
import net.maximlvr.asmpthings.block.ModBlocks;
import net.maximlvr.asmpthings.component.ModDataComponents;
import net.maximlvr.asmpthings.command.BackpackInspectCommand;
import net.maximlvr.asmpthings.entity.ModEntities;
import net.maximlvr.asmpthings.entity.custom.CubeBossEntity;
import net.maximlvr.asmpthings.integration.sophisticated.BackpackInspectionInventory;
import net.maximlvr.asmpthings.item.ModCreativeModeTabs;
import net.maximlvr.asmpthings.item.ModItems;
import net.maximlvr.asmpthings.network.ModNetworking;
import net.maximlvr.asmpthings.stats.ModStats;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.common.NeoForge;

import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;

import net.maximlvr.asmpthings.block.entity.ModBlockEntities;

@Mod(AsmpThingsMod.MOD_ID)
public class AsmpThingsMod {
    public static final String MOD_ID = "asmpthingsmod";
    public static final Logger LOGGER = LogUtils.getLogger();

    public AsmpThingsMod(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::registerAttributes);

        NeoForge.EVENT_BUS.register(new AiNpcChatHandler());
        NeoForge.EVENT_BUS.register(new BankEvents());
        NeoForge.EVENT_BUS.register(new BackpackInspectCommand());
        BackpackInspectionInventory.register();

        ModCreativeModeTabs.register(modEventBus);
        ModItems.register(modEventBus);
        ModDataComponents.register(modEventBus);
        ModStats.register(modEventBus);
        ModNetworking.register(modEventBus);
        ModBlocks.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        ModEntities.register(modEventBus);

        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(ModEntities.CUBE_BOSS.get(), CubeBossEntity.createAttributes().build());
    }
}
