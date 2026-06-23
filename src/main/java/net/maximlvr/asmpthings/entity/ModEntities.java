package net.maximlvr.asmpthings.entity;

import net.maximlvr.asmpthings.AsmpThingsMod;
import net.maximlvr.asmpthings.entity.custom.CrawlerEntity;
import net.maximlvr.asmpthings.entity.custom.CrawlerHandEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModEntities {

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, AsmpThingsMod.MOD_ID);

    public static final DeferredHolder<EntityType<?>, EntityType<CrawlerEntity>> CRAWLER =
            ENTITY_TYPES.register("crawler", () ->
                    EntityType.Builder.of(CrawlerEntity::new, MobCategory.MONSTER)
                            .sized(1.4F, 1.0F)
                            .clientTrackingRange(10)
                            .updateInterval(2)
                            .build("crawler")
            );

    public static final DeferredHolder<EntityType<?>, EntityType<CrawlerHandEntity>> CRAWLER_HAND =
            ENTITY_TYPES.register("crawler_hand", () ->
                    EntityType.Builder.<CrawlerHandEntity>of(CrawlerHandEntity::new, MobCategory.MISC)
                            .sized(0.6F, 0.6F)
                            .clientTrackingRange(10)
                            .updateInterval(1)
                            .build("crawler_hand")
            );

    public static void register(IEventBus eventBus) {
        ENTITY_TYPES.register(eventBus);
    }
}