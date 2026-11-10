package net.maximlvr.asmpthings.entity;

import net.maximlvr.asmpthings.AsmpThingsMod;
import net.maximlvr.asmpthings.entity.custom.ArmiBossEntity;
import net.maximlvr.asmpthings.entity.custom.DroneEntity;
import net.maximlvr.asmpthings.entity.custom.CubeBossArrowEntity;
import net.maximlvr.asmpthings.entity.custom.CubeBossBeamEntity;
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

    public static final DeferredHolder<EntityType<?>, EntityType<net.maximlvr.asmpthings.entity.custom.UndertakerEntity>> UNDERTAKER =
            ENTITY_TYPES.register("undertaker", () -> EntityType.Builder
                    .of(net.maximlvr.asmpthings.entity.custom.UndertakerEntity::new, MobCategory.CREATURE)
                    .sized(0.8F, 2.0F).clientTrackingRange(10).updateInterval(2).build("undertaker"));

    public static final DeferredHolder<EntityType<?>, EntityType<net.maximlvr.asmpthings.entity.custom.GolemEntity>> GOLEM =
            ENTITY_TYPES.register("golem", () -> EntityType.Builder
                    .of(net.maximlvr.asmpthings.entity.custom.GolemEntity::new, MobCategory.CREATURE)
                    .sized(1.6F, 3.65F).clientTrackingRange(10).updateInterval(2).build("golem"));

    public static final DeferredHolder<EntityType<?>, EntityType<net.maximlvr.asmpthings.entity.custom.KraberSphereEntity>> KRABER_SPHERE =
            ENTITY_TYPES.register("kraber_sphere", () -> EntityType.Builder
                    .of(net.maximlvr.asmpthings.entity.custom.KraberSphereEntity::new, MobCategory.MISC)
                    .sized(0.1F, 0.1F).clientTrackingRange(8).updateInterval(1).build("kraber_sphere"));

    public static final DeferredHolder<EntityType<?>, EntityType<DroneEntity>> DRONE =
            ENTITY_TYPES.register("drone", () -> EntityType.Builder.of(DroneEntity::new, MobCategory.MONSTER)
                    .sized(0.9F, 1.0F).clientTrackingRange(10).updateInterval(2).build("drone"));

    public static final DeferredHolder<EntityType<?>, EntityType<ArmiBossEntity>> ARMI_BOSS =
            ENTITY_TYPES.register("armi_boss", () ->
                    EntityType.Builder.of(ArmiBossEntity::new, MobCategory.MONSTER)
                            .sized(ArmiBossEntity.HAND_WIDTH, ArmiBossEntity.HAND_HEIGHT)
                            .clientTrackingRange(12)
                            .updateInterval(1)
                            .fireImmune()
                            .build("armi_boss"));

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

    public static final DeferredHolder<EntityType<?>, EntityType<CubeBossBeamEntity>> CUBE_BOSS_BEAM =
            ENTITY_TYPES.register("cube_boss_beam", () ->
                    EntityType.Builder.<CubeBossBeamEntity>of(CubeBossBeamEntity::new, MobCategory.MISC)
                            .sized(0.4F, 0.4F)
                            .clientTrackingRange(32)
                            .updateInterval(1)
                            .build("cube_boss_beam")
            );

    public static final DeferredHolder<EntityType<?>, EntityType<CubeBossArrowEntity>> CUBE_BOSS_ARROW =
            ENTITY_TYPES.register("cube_boss_arrow", () ->
                    EntityType.Builder.<CubeBossArrowEntity>of(CubeBossArrowEntity::new, MobCategory.MISC)
                            .sized(0.35F, 0.35F)
                            .clientTrackingRange(32)
                            .updateInterval(1)
                            .build("cube_boss_arrow")
            );

    public static void register(IEventBus eventBus) {
        ENTITY_TYPES.register(eventBus);
    }
}
