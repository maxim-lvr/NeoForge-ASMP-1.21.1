package net.maximlvr.asmpthings.item.custom;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class WeatherStaff extends Item {
    public WeatherStaff(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        ItemStack stack = player.getItemInHand(usedHand);

        if (player.getCooldowns().isOnCooldown(this)) {
            return InteractionResultHolder.fail(stack);
        }

        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        }

        if (stack.getDamageValue() >= stack.getMaxDamage()) {
            return InteractionResultHolder.fail(stack);
        }

        if (serverLevel.isRaining()) {
            serverLevel.setWeatherParameters(6000, 0, false, false);
        } else {
            serverLevel.setWeatherParameters(0, 6000, true, false);
        }

        if (!player.isCreative()) {
            stack.setDamageValue(stack.getDamageValue() + 1);
        }

        player.getCooldowns().addCooldown(this, 60);

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}