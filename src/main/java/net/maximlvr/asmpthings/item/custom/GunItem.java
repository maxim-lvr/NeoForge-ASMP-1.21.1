package net.maximlvr.asmpthings.item.custom;

import net.maximlvr.asmpthings.component.ModDataComponents;
import net.maximlvr.asmpthings.network.payload.GunLaserPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.HumanoidArm;
import net.neoforged.neoforge.network.PacketDistributor;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.List;

public class GunItem extends Item {
    private final GunSettings settings;

    public GunItem(Properties properties, GunSettings settings) {
        super(properties.stacksTo(1).component(ModDataComponents.GUN_AMMO.get(), settings.capacity()));
        this.settings = settings;
    }

    public int getAmmo(ItemStack stack) {
        return Mth.clamp(stack.getOrDefault(ModDataComponents.GUN_AMMO.get(), settings.capacity()), 0, settings.capacity());
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (level instanceof ServerLevel serverLevel) {
            refill(stack, serverLevel.getServer().overworld().getGameTime());
        }
    }

    protected final void refill(ItemStack stack, long now) {
        int ammo = getAmmo(stack);
        if (ammo >= settings.capacity()) {
            stack.remove(ModDataComponents.GUN_NEXT_REFILL.get());
            return;
        }
        Long next = stack.get(ModDataComponents.GUN_NEXT_REFILL.get());
        if (next == null) {
            stack.set(ModDataComponents.GUN_NEXT_REFILL.get(), now + settings.refillTicks());
        } else if (now >= next) {
            // Catch up after storage/logout without syncing a countdown every tick.
            int restored = (int) Math.min(settings.capacity() - ammo, 1 + (now - next) / settings.refillTicks());
            stack.set(ModDataComponents.GUN_AMMO.get(), ammo + restored);
            if (ammo + restored == settings.capacity()) {
                stack.remove(ModDataComponents.GUN_NEXT_REFILL.get());
            } else {
                stack.set(ModDataComponents.GUN_NEXT_REFILL.get(), next + (long) restored * settings.refillTicks());
            }
        }
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isSpectator() || player.getCooldowns().isOnCooldown(this)) {
            return InteractionResultHolder.fail(stack);
        }
        if (level instanceof ServerLevel serverLevel) {
            long now = serverLevel.getServer().overworld().getGameTime();
            refill(stack, now);
            if (getAmmo(stack) == 0) {
                serverLevel.playSound(null, player.blockPosition(), SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 0.5F, 1.4F);
                player.getCooldowns().addCooldown(this, settings.shotCooldownTicks());
                return InteractionResultHolder.fail(stack);
            }
            stack.set(ModDataComponents.GUN_AMMO.get(), getAmmo(stack) - 1);
            // Every successful shot restarts this weapon's refill delay.
            stack.set(ModDataComponents.GUN_NEXT_REFILL.get(), now + settings.refillTicks());
            fire(serverLevel, player, hand);
            player.getCooldowns().addCooldown(this, settings.shotCooldownTicks());
            player.awardStat(Stats.ITEM_USED.get(this));
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    private void fire(ServerLevel level, Player player, InteractionHand hand) {
        Vec3 start = player.getEyePosition();
        Vec3 direction = player.getLookAngle();
        Vec3 end = start.add(direction.scale(settings.range()));
        var blockHit = level.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        end = blockHit.getLocation();
        var entityHit = ProjectileUtil.getEntityHitResult(player, start, end,
                player.getBoundingBox().expandTowards(end.subtract(start)).inflate(1),
                target -> target.isAlive() && !target.isSpectator() && target.isPickable(), start.distanceToSqr(end));
        if (entityHit != null) {
            end = entityHit.getLocation();
            Entity target = entityHit.getEntity();
            if (!(target instanceof Player other) || player.canHarmPlayer(other)) {
                target.hurt(level.damageSources().mobProjectile(player, player), settings.damage());
            }
        } else if (blockHit.getType() == HitResult.Type.BLOCK) {
            onBlockShot(level, player, blockHit);
        }
        level.playSound(null, player.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.6F, 1.8F);
        Vec3 muzzle = start.add(direction.scale(0.7));
        if (player instanceof ServerPlayer shooter) {
            HumanoidArm arm = hand == InteractionHand.MAIN_HAND ? player.getMainArm() : player.getMainArm().getOpposite();
            Vec3 right = new Vec3(-Math.cos(Math.toRadians(player.getYRot())), 0,
                    -Math.sin(Math.toRadians(player.getYRot())));
            // Cosmetic hand offset only: keep the actual hitscan aligned with the crosshair.
            Vec3 laserStart = muzzle.add(right.scale(arm == HumanoidArm.RIGHT ? 0.25 : -0.25)).add(0, -0.25, 0);
            PacketDistributor.sendToPlayersTrackingEntityAndSelf(shooter,
                    new GunLaserPayload(level.dimension().location(), laserStart, end));
        }
    }

    protected void onBlockShot(ServerLevel level, Player player, BlockHitResult hit) {
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13F * getAmmo(stack) / settings.capacity());
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return getAmmo(stack) == 0 ? 0xCC4444 : 0xE6C35C;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.asmpthingsmod.gun_ammo", getAmmo(stack), settings.capacity()));
    }
}
