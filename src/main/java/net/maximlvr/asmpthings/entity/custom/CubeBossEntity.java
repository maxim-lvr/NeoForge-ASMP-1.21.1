package net.maximlvr.asmpthings.entity.custom;

import net.maximlvr.asmpthings.entity.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class CubeBossEntity extends Monster {
    public static final int ANIMATION_SLEEPING = 0;
    public static final int ANIMATION_IDLE = 1;
    public static final int ANIMATION_RUN_CYCLE = 2;
    public static final int ANIMATION_ARROW_UP = 3;

    private static final EntityDataAccessor<Boolean> DATA_AWAKE =
            SynchedEntityData.defineId(CubeBossEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_ANIMATION_MODE =
            SynchedEntityData.defineId(CubeBossEntity.class, EntityDataSerializers.INT);

    private static final int MAX_SWORD_WAVE_RADIUS = 10;
    private static final int WAVE_STEP_TICKS = 6;
    private static final int MAX_ENCOUNTER_ROUNDS = 2;
    private static final int IDLE_AFTER_ZOMBIE_TICKS = 60;
    private static final int ARROW_UP_TICKS = 40;
    private static final double ZOMBIE_HEALTH = 30.0D;

    private static final int PHASE_DORMANT = 0;
    private static final int PHASE_ZOMBIE = 1;
    private static final int PHASE_IDLE_AFTER_ZOMBIE = 2;
    private static final int PHASE_ARROW_UP = 3;
    private static final int PHASE_SWORD_WAVE = 4;
    private static final int PHASE_FINISHED = 5;

    private final ServerBossEvent bossEvent = new ServerBossEvent(
            Component.literal("Cube Boss"),
            BossEvent.BossBarColor.YELLOW,
            BossEvent.BossBarOverlay.PROGRESS
    );

    public final AnimationState idleAnimationState = new AnimationState();
    public final AnimationState runCycleAnimationState = new AnimationState();
    public final AnimationState arrowUpAnimationState = new AnimationState();

    private int phase = PHASE_DORMANT;
    private int phaseTicks = 0;
    private int completedRounds = 0;
    private UUID challengeZombieId;
    private boolean swordWaveRunning = false;
    private int nextSwordWaveRadius = 1;
    private int swordWaveStepCooldown = 0;
    private int swordWaveFinishCooldown = 0;

    public CubeBossEntity(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
        this.setPersistenceRequired();
        this.setNoAi(true);
        this.xpReward = 100;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 300.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.0D)
                .add(Attributes.FOLLOW_RANGE, 48.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.ARMOR, 12.0D);
    }

    public boolean isAwake() {
        return this.entityData.get(DATA_AWAKE);
    }

    public int getAnimationMode() {
        return this.entityData.get(DATA_ANIMATION_MODE);
    }

    public void setAwake(boolean awake) {
        this.entityData.set(DATA_AWAKE, awake);
        this.setNoAi(true);

        if (!this.level().isClientSide) {
            this.bossEvent.setColor(awake ? BossEvent.BossBarColor.RED : BossEvent.BossBarColor.YELLOW);
            this.bossEvent.setName(Component.literal(awake ? "Cube Boss - Eveille" : "Cube Boss"));

            if (awake) {
                startZombiePhase();
            } else {
                this.phase = PHASE_DORMANT;
                this.phaseTicks = 0;
                this.completedRounds = 0;
                this.challengeZombieId = null;
                resetSwordWave();
                setAnimationMode(ANIMATION_SLEEPING);
            }
        }
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_AWAKE, false);
        builder.define(DATA_ANIMATION_MODE, ANIMATION_SLEEPING);
    }

    @Override
    protected void registerGoals() {
    }

    @Override
    public void tick() {
        super.tick();
        this.setDeltaMovement(0.0D, this.getDeltaMovement().y, 0.0D);

        if (this.level().isClientSide) {
            updateClientAnimations();
        } else {
            this.setNoAi(true);
            this.bossEvent.setProgress(this.getHealth() / this.getMaxHealth());

            if (this.isAwake()) {
                tickEncounter();
            }
        }
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        this.setNoAi(true);
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!this.isAwake() && (stack.is(Items.GOLDEN_APPLE) || stack.is(Items.ENCHANTED_GOLDEN_APPLE))) {
            if (!this.level().isClientSide) {
                this.setAwake(true);

                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }
            }

            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }

        return super.mobInteract(player, hand);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.is(DamageTypes.GENERIC_KILL) || source.is(DamageTypes.FELL_OUT_OF_WORLD)) {
            return super.hurt(source, amount);
        }

        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void doPush(net.minecraft.world.entity.Entity entity) {
    }

    @Override
    public void push(double x, double y, double z) {
    }

    @Override
    public void remove(RemovalReason reason) {
        this.bossEvent.removeAllPlayers();
        removeChallengeZombie();
        super.remove(reason);
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        this.bossEvent.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        this.bossEvent.removePlayer(player);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Awake", this.isAwake());
        tag.putInt("CubeBossPhase", this.phase);
        tag.putInt("CubeBossPhaseTicks", this.phaseTicks);
        tag.putInt("CubeBossCompletedRounds", this.completedRounds);
        tag.putInt("CubeBossAnimationMode", this.getAnimationMode());

        if (this.challengeZombieId != null) {
            tag.putUUID("CubeBossZombie", this.challengeZombieId);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.setAwake(tag.getBoolean("Awake"));

        if (this.isAwake()) {
            this.phase = tag.getInt("CubeBossPhase");
            this.phaseTicks = tag.getInt("CubeBossPhaseTicks");
            this.completedRounds = tag.getInt("CubeBossCompletedRounds");
            setAnimationMode(tag.getInt("CubeBossAnimationMode"));

            if (tag.hasUUID("CubeBossZombie")) {
                this.challengeZombieId = tag.getUUID("CubeBossZombie");
            }
        }
    }

    private void updateClientAnimations() {
        int animationMode = getAnimationMode();

        if (animationMode == ANIMATION_IDLE || animationMode == ANIMATION_RUN_CYCLE) {
            this.idleAnimationState.startIfStopped(this.tickCount);
        } else {
            this.idleAnimationState.stop();
        }

        if (animationMode == ANIMATION_RUN_CYCLE) {
            this.runCycleAnimationState.startIfStopped(this.tickCount);
        } else {
            this.runCycleAnimationState.stop();
        }

        if (animationMode == ANIMATION_ARROW_UP) {
            this.arrowUpAnimationState.startIfStopped(this.tickCount);
        } else {
            this.arrowUpAnimationState.stop();
        }
    }

    private void tickEncounter() {
        switch (this.phase) {
            case PHASE_ZOMBIE -> {
                if (isChallengeZombieDefeated()) {
                    startIdleAfterZombiePhase();
                }
            }
            case PHASE_IDLE_AFTER_ZOMBIE -> {
                if (++this.phaseTicks >= IDLE_AFTER_ZOMBIE_TICKS) {
                    startArrowUpPhase();
                }
            }
            case PHASE_ARROW_UP -> {
                if (++this.phaseTicks >= ARROW_UP_TICKS) {
                    startSwordWavePhase();
                }
            }
            case PHASE_SWORD_WAVE -> {
                if (tickSwordWaveAttack()) {
                    this.completedRounds++;

                    if (this.completedRounds >= MAX_ENCOUNTER_ROUNDS) {
                        finishEncounter();
                    } else {
                        startZombiePhase();
                    }
                }
            }
            default -> {
            }
        }
    }

    private void startZombiePhase() {
        this.phase = PHASE_ZOMBIE;
        this.phaseTicks = 0;
        resetSwordWave();
        setAnimationMode(ANIMATION_RUN_CYCLE);
        spawnChallengeZombie();
    }

    private void startIdleAfterZombiePhase() {
        this.phase = PHASE_IDLE_AFTER_ZOMBIE;
        this.phaseTicks = 0;
        this.challengeZombieId = null;
        setAnimationMode(ANIMATION_IDLE);
    }

    private void startArrowUpPhase() {
        this.phase = PHASE_ARROW_UP;
        this.phaseTicks = 0;
        setAnimationMode(ANIMATION_ARROW_UP);
    }

    private void startSwordWavePhase() {
        this.phase = PHASE_SWORD_WAVE;
        this.phaseTicks = 0;
        resetSwordWave();
        setAnimationMode(ANIMATION_IDLE);
    }

    private void finishEncounter() {
        this.phase = PHASE_FINISHED;
        setAnimationMode(ANIMATION_SLEEPING);
        this.kill();
    }

    private void setAnimationMode(int animationMode) {
        this.entityData.set(DATA_ANIMATION_MODE, animationMode);
    }

    private void spawnChallengeZombie() {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        Zombie zombie = EntityType.ZOMBIE.create(serverLevel);

        if (zombie == null) {
            return;
        }

        BlockPos spawnPos = findZombieSpawnSurface();
        zombie.moveTo(spawnPos.getX() + 0.5D, spawnPos.getY(), spawnPos.getZ() + 0.5D, this.getYRot(), 0.0F);
        zombie.setCustomName(Component.literal("Gardien du Cube Boss"));
        zombie.setPersistenceRequired();

        if (zombie.getAttribute(Attributes.MAX_HEALTH) != null) {
            zombie.getAttribute(Attributes.MAX_HEALTH).setBaseValue(ZOMBIE_HEALTH);
        }

        zombie.setHealth((float) ZOMBIE_HEALTH);
        serverLevel.addFreshEntity(zombie);
        this.challengeZombieId = zombie.getUUID();
    }

    private BlockPos findZombieSpawnSurface() {
        BlockPos origin = this.blockPosition();

        for (int attempt = 0; attempt < 16; attempt++) {
            double angle = this.random.nextDouble() * Math.PI * 2.0D;
            int radius = 4 + this.random.nextInt(4);
            int dx = (int) Math.round(Math.cos(angle) * radius);
            int dz = (int) Math.round(Math.sin(angle) * radius);
            BlockPos surface = findAttackSurface(origin.offset(dx, 0, dz));

            if (surface != null) {
                return surface;
            }
        }

        BlockPos fallback = findAttackSurface(origin.offset(4, 0, 0));
        return fallback != null ? fallback : origin;
    }

    private boolean isChallengeZombieDefeated() {
        if (this.challengeZombieId == null || !(this.level() instanceof ServerLevel serverLevel)) {
            return true;
        }

        net.minecraft.world.entity.Entity entity = serverLevel.getEntity(this.challengeZombieId);

        if (!(entity instanceof LivingEntity livingEntity)) {
            return true;
        }

        return !livingEntity.isAlive();
    }

    private void removeChallengeZombie() {
        if (this.challengeZombieId == null || !(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        net.minecraft.world.entity.Entity entity = serverLevel.getEntity(this.challengeZombieId);

        if (entity instanceof Zombie) {
            entity.discard();
        }

        this.challengeZombieId = null;
    }

    private boolean tickSwordWaveAttack() {
        if (this.swordWaveFinishCooldown > 0) {
            this.swordWaveFinishCooldown--;
            return this.swordWaveFinishCooldown <= 0;
        }

        if (!this.swordWaveRunning) {
            this.swordWaveRunning = true;
            this.nextSwordWaveRadius = 1;
            this.swordWaveStepCooldown = 0;
        }

        if (this.swordWaveStepCooldown > 0) {
            this.swordWaveStepCooldown--;
            return false;
        }

        spawnSwordRing(this.nextSwordWaveRadius);
        this.nextSwordWaveRadius++;
        this.swordWaveStepCooldown = WAVE_STEP_TICKS;

        if (this.nextSwordWaveRadius > MAX_SWORD_WAVE_RADIUS) {
            this.swordWaveRunning = false;
            this.swordWaveFinishCooldown = CubeBossSwordEntity.TOTAL_TICKS;
        }

        return false;
    }

    private void resetSwordWave() {
        this.swordWaveRunning = false;
        this.nextSwordWaveRadius = 1;
        this.swordWaveStepCooldown = 0;
        this.swordWaveFinishCooldown = 0;
    }

    private void spawnSwordRing(int radius) {
        BlockPos origin = this.blockPosition();
        Set<BlockPos> spawned = new HashSet<>();

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) {
                    continue;
                }

                BlockPos surface = findAttackSurface(origin.offset(dx, 0, dz));

                if (surface != null && spawned.add(surface)) {
                    spawnSwordWarning(surface);
                }
            }
        }
    }

    private BlockPos findAttackSurface(BlockPos around) {
        int startY = around.getY() + 4;
        int minY = around.getY() - 8;

        for (int y = startY; y >= minY; y--) {
            BlockPos feet = new BlockPos(around.getX(), y, around.getZ());

            if (isSolid(feet.below()) && !isSolid(feet)) {
                return feet;
            }
        }

        return null;
    }

    private boolean isSolid(BlockPos pos) {
        BlockState state = this.level().getBlockState(pos);
        return !state.getCollisionShape(this.level(), pos).isEmpty();
    }

    private void spawnSwordWarning(BlockPos surface) {
        CubeBossSwordEntity sword = ModEntities.CUBE_BOSS_SWORD.get().create(this.level());

        if (sword == null) {
            return;
        }

        sword.setPos(surface.getX() + 0.5D, surface.getY(), surface.getZ() + 0.5D);
        this.level().addFreshEntity(sword);
    }
}
