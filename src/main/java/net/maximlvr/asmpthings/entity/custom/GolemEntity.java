package net.maximlvr.asmpthings.entity.custom;

import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.animal.AbstractGolem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public class GolemEntity extends AbstractGolem {
    public final AnimationState idleAnimation = new AnimationState();
    public final AnimationState walkState = new AnimationState();

    public GolemEntity(EntityType<? extends GolemEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createMobAttributes().add(Attributes.MAX_HEALTH, 80)
                .add(Attributes.MOVEMENT_SPEED, 0.12).add(Attributes.KNOCKBACK_RESISTANCE, 0.8)
                .add(Attributes.STEP_HEIGHT, 1);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new WaterAvoidingRandomStrollGoal(this, 1));
        goalSelector.addGoal(2, new LookAtPlayerGoal(this, Player.class, 8));
        goalSelector.addGoal(3, new RandomLookAroundGoal(this));
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            boolean moving = walkAnimation.speed() > 0.01F;
            idleAnimation.animateWhen(!moving, tickCount);
            walkState.animateWhen(moving, tickCount);
        }
    }
}
