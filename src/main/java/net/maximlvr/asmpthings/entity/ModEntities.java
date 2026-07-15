package net.maximlvr.asmpthings.entity;

import net.maximlvr.asmpthings.AsmpThingsMod;
import net.maximlvr.asmpthings.entity.custom.CubeBossEntity;
import net.maximlvr.asmpthings.entity.custom.CubeBossSwordEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModEntities {

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, AsmpThingsMod.MOD_ID);

    public static final DeferredHolder<EntityType<?>, EntityType<CubeBossEntity>> CUBE_BOSS =
            ENTITY_TYPES.register("cube_boss", () ->
                    EntityType.Builder.of(CubeBossEntity::new, MobCategory.MONSTER)
                            .sized(2.5F, 2.5F)
                            .clientTrackingRange(12)
                            .updateInterval(2)
                            .fireImmune()
                            .build("cube_boss")
            );

    public static final DeferredHolder<EntityType<?>, EntityType<CubeBossSwordEntity>> CUBE_BOSS_SWORD =
            ENTITY_TYPES.register("cube_boss_sword", () ->
                    EntityType.Builder.<CubeBossSwordEntity>of(CubeBossSwordEntity::new, MobCategory.MISC)
                            .sized(1.0F, 2.0F)
                            .clientTrackingRange(12)
                            .updateInterval(1)
                            .build("cube_boss_sword")
            );

    public static void register(IEventBus eventBus) {
        ENTITY_TYPES.register(eventBus);
    }
}
