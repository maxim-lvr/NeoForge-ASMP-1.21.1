package net.maximlvr.asmpthings.world;

import net.maximlvr.asmpthings.AsmpThingsMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;

public class ModDimensions {

    public static final ResourceKey<Level> ASMP_DIMENSION_KEY = ResourceKey.create(
            Registries.DIMENSION,
            ResourceLocation.fromNamespaceAndPath(AsmpThingsMod.MOD_ID, "asmp_dimension")
    );

    public static final ResourceKey<DimensionType> ASMP_DIMENSION_TYPE = ResourceKey.create(
            Registries.DIMENSION_TYPE,
            ResourceLocation.fromNamespaceAndPath(AsmpThingsMod.MOD_ID, "asmp_dimension_type")
    );
}