package net.maximlvr.asmpthings.item;

import net.minecraft.world.food.FoodProperties;

public class ModFoods {
    public static final FoodProperties BEER = new FoodProperties.Builder()
            .nutrition(1)
            .saturationModifier(0.5F)
            .alwaysEdible()
            .build();
}
