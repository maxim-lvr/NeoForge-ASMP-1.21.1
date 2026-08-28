package net.maximlvr.asmpthings.entity.custom;

import net.maximlvr.asmpthings.block.ModBlocks;
import net.maximlvr.asmpthings.entity.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.HashSet;
import java.util.Set;

public class CubeBossEntity extends Monster {
    public static final int ANIMATION_SLEEPING = 0;
    public static final int ANIMATION_IDLE = 1;
    public static final int ANIMATION_RUN_CYCLE = 2;
    public static final int ANIMATION_ARROW_UP = 3;
    public static final int ANIMATION_ARROW_DOWN = 4;
    public static final int ANIMATION_ARROW_LEFT = 5;
    public static final int ANIMATION_ARROW_RIGHT = 6;

    private static final EntityDataAccessor<Boolean> DATA_AWAKE =
            SynchedEntityData.defineId(CubeBossEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_ANIMATION_MODE =
            SynchedEntityData.defineId(CubeBossEntity.class, EntityDataSerializers.INT);

    private static final int ARENA_HALF_SIZE = 10;
    private static final int ARENA_HEIGHT = 14;
    private static final int WAKE_DELAY_TICKS = 60;
    private static final int ARROW_PREVIEW_TICKS = 92;
    private static final int ARROW_RESOLVE_TICKS = 64;
    private static final int ARROW_RESOLVE_INTERVAL_TICKS = 8;
    private static final int ARROW_RAIN_TICKS = 120;
    private static final int ARROW_RAIN_FINISH_WAIT_TICKS = 45;
    private static final int ARROW_RAIN_INTERVAL_TICKS = 3;
    private static final int ARROW_RAIN_PATTERN_INTERVAL_TICKS = 9;
    private static final int ARROW_RAIN_TARGET_INTERVAL_TICKS = 12;
    private static final int ARROW_RAIN_RANDOM_BURST_SIZE = 3;
    private static final int ARROW_RAIN_BROKEN_PATTERN_SIZE = 7;
    private static final int BEAM_BARRAGE_TICKS = 100;
    private static final int SWORD_SPIRAL_TICKS = 80;
    private static final int SWORD_GRID_TICKS = 72;
    private static final int BEAM_BARRAGE_INTERVAL_TICKS = 10;
    private static final int SWORD_SPIRAL_INTERVAL_TICKS = 4;
    private static final int SWORD_GRID_INTERVAL_TICKS = 12;
    private static final int BEAM_BARRAGE_BURST_SIZE = 2;
    private static final int SPIRAL_LASER_TICKS = 112;
    private static final int SPIRAL_LASER_FINISH_WAIT_TICKS = 84;
    private static final int SPIRAL_LASER_INTERVAL_TICKS = 8;
    private static final int SPIRAL_LASER_WARNING_TICKS = 6;
    private static final int SPIRAL_LASER_ACTIVE_TICKS = 76;
    private static final int SPIRAL_LASER_GAP_HALF_WIDTH = 2;
    private static final double SPIRAL_LASER_SPEED = 0.31D;
    private static final int SAFE_TILE_TICKS = 96;
    private static final int SAFE_TILE_INTERVAL_TICKS = 24;
    private static final int MAX_BEAM_LENGTH = 30;

    private static final int PHASE_DORMANT = 0;
    private static final int PHASE_WAKE_DELAY = 1;
    private static final int PHASE_ARROW_ATTACK = 2;
    private static final int PHASE_ARROW_RESOLVE = 3;
    private static final int PHASE_ARROW_RAIN = 4;
    private static final int PHASE_BEAM_BARRAGE = 5;
    private static final int PHASE_SPIRAL_LASERS = 6;
    private static final int PHASE_SAFE_TILES = 7;
    private static final int PHASE_SWORD_SPIRAL = 8;
    private static final int PHASE_SWORD_GRID = 9;
    private static final int PHASE_FINISHED = 10;

    private final ServerBossEvent bossEvent = new ServerBossEvent(
            Component.literal("Cube Boss"),
            BossEvent.BossBarColor.YELLOW,
            BossEvent.BossBarOverlay.PROGRESS
    );

    public final AnimationState idleAnimationState = new AnimationState();
    public final AnimationState runCycleAnimationState = new AnimationState();
    public final AnimationState arrowUpAnimationState = new AnimationState();
    public final AnimationState arrowDownAnimationState = new AnimationState();
    public final AnimationState arrowLeftAnimationState = new AnimationState();
    public final AnimationState arrowRightAnimationState = new AnimationState();

    private int phase = PHASE_DORMANT;
    private int phaseTicks = 0;
    private Direction arrowAttackDirection = Direction.UP;
    private Direction resolvedArrowDirection = Direction.UP;
    private boolean arrowAttackSpawned = false;
    private BlockPos stelePos;
    private boolean steleLookupDone = false;

    public CubeBossEntity(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
        this.setPersistenceRequired();
        this.setNoAi(true);
        this.setNoGravity(true);
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
        this.setNoGravity(true);

        if (!this.level().isClientSide) {
            updateBossBarAwakeState(awake);

            if (awake) {
                startWakeDelayPhase();
            } else {
                this.phase = PHASE_DORMANT;
                this.phaseTicks = 0;
                this.arrowAttackSpawned = false;
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
        tag.putInt("CubeBossAnimationMode", this.getAnimationMode());
        tag.putInt("CubeBossArrowDirection", this.arrowAttackDirection.ordinal());
        tag.putInt("CubeBossResolvedArrowDirection", this.resolvedArrowDirection.ordinal());
        tag.putBoolean("CubeBossArrowSpawned", this.arrowAttackSpawned);

        if (this.stelePos != null) {
            tag.putLong("CubeBossStelePos", this.stelePos.asLong());
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);

        if (tag.contains("CubeBossStelePos")) {
            this.stelePos = BlockPos.of(tag.getLong("CubeBossStelePos"));
            this.steleLookupDone = true;
        }

        boolean awake = tag.getBoolean("Awake");
        this.entityData.set(DATA_AWAKE, awake);
        this.setNoAi(true);
        this.setNoGravity(true);

        if (!this.level().isClientSide) {
            updateBossBarAwakeState(awake);
        }

        if (this.isAwake()) {
            this.phase = tag.getInt("CubeBossPhase");
            this.phaseTicks = tag.getInt("CubeBossPhaseTicks");
            setAnimationMode(tag.getInt("CubeBossAnimationMode"));

            if (tag.contains("CubeBossArrowDirection")) {
                Direction[] directions = Direction.values();
                this.arrowAttackDirection = directions[Math.floorMod(tag.getInt("CubeBossArrowDirection"), directions.length)];
            }

            if (tag.contains("CubeBossResolvedArrowDirection")) {
                Direction[] directions = Direction.values();
                this.resolvedArrowDirection = directions[Math.floorMod(tag.getInt("CubeBossResolvedArrowDirection"), directions.length)];
            }

            this.arrowAttackSpawned = tag.getBoolean("CubeBossArrowSpawned");

            if (this.phase <= PHASE_DORMANT || this.phase > PHASE_FINISHED) {
                startWakeDelayPhase();
            }
        } else {
            this.phase = PHASE_DORMANT;
            this.phaseTicks = 0;
            this.arrowAttackSpawned = false;
            setAnimationMode(ANIMATION_SLEEPING);
        }
    }

    public void setStelePos(BlockPos stelePos) {
        this.stelePos = stelePos.immutable();
        this.steleLookupDone = true;
    }

    public boolean isLinkedToStele(BlockPos stelePos) {
        return this.stelePos != null && this.stelePos.equals(stelePos);
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

        if (animationMode == ANIMATION_ARROW_DOWN) {
            this.arrowDownAnimationState.startIfStopped(this.tickCount);
        } else {
            this.arrowDownAnimationState.stop();
        }

        if (animationMode == ANIMATION_ARROW_LEFT) {
            this.arrowLeftAnimationState.startIfStopped(this.tickCount);
        } else {
            this.arrowLeftAnimationState.stop();
        }

        if (animationMode == ANIMATION_ARROW_RIGHT) {
            this.arrowRightAnimationState.startIfStopped(this.tickCount);
        } else {
            this.arrowRightAnimationState.stop();
        }
    }

    private void tickEncounter() {
        switch (this.phase) {
            case PHASE_WAKE_DELAY -> {
                if (++this.phaseTicks >= WAKE_DELAY_TICKS) {
                    startArrowAttackPhase();
                }
            }
            case PHASE_ARROW_ATTACK -> {
                if (tickArrowAttack()) {
                    startArrowResolvePhase();
                }
            }
            case PHASE_ARROW_RESOLVE -> {
                if (tickArrowResolve()) {
                    startArrowRainPhase();
                }
            }
            case PHASE_ARROW_RAIN -> {
                if (tickArrowRain()) {
                    startBeamBarragePhase();
                }
            }
            case PHASE_BEAM_BARRAGE -> {
                if (tickBeamBarrage()) {
                    startSpiralLaserPhase();
                }
            }
            case PHASE_SPIRAL_LASERS -> {
                if (tickSpiralLasers()) {
                    startSafeTilePhase();
                }
            }
            case PHASE_SAFE_TILES -> {
                if (tickSafeTiles()) {
                    startSwordSpiralPhase();
                }
            }
            case PHASE_SWORD_SPIRAL -> {
                if (tickSwordSpiral()) {
                    startSwordGridPhase();
                }
            }
            case PHASE_SWORD_GRID -> {
                if (tickSwordGrid()) {
                    finishEncounter();
                }
            }
            default -> {
            }
        }
    }

    private void startWakeDelayPhase() {
        this.phase = PHASE_WAKE_DELAY;
        this.phaseTicks = 0;
        setAnimationMode(ANIMATION_ARROW_UP);
    }

    private void startArrowAttackPhase() {
        this.phase = PHASE_ARROW_ATTACK;
        this.phaseTicks = 0;
        this.arrowAttackSpawned = false;
        this.arrowAttackDirection = getRandomArrowAttackDirection();
        this.resolvedArrowDirection = getResolvedArrowDirection(this.arrowAttackDirection);
        setAnimationMode(ANIMATION_IDLE);
    }

    private void startArrowResolvePhase() {
        this.phase = PHASE_ARROW_RESOLVE;
        this.phaseTicks = 0;
        setArrowDirectionAnimation(this.resolvedArrowDirection);
    }

    private void startArrowRainPhase() {
        this.phase = PHASE_ARROW_RAIN;
        this.phaseTicks = 0;
        setAnimationMode(ANIMATION_RUN_CYCLE);
    }

    private void startBeamBarragePhase() {
        this.phase = PHASE_BEAM_BARRAGE;
        this.phaseTicks = 0;
        setAnimationMode(ANIMATION_IDLE);
    }

    private void startSpiralLaserPhase() {
        this.phase = PHASE_SPIRAL_LASERS;
        this.phaseTicks = 0;
        setAnimationMode(ANIMATION_ARROW_LEFT);
    }

    private void startSafeTilePhase() {
        this.phase = PHASE_SAFE_TILES;
        this.phaseTicks = 0;
        setAnimationMode(ANIMATION_ARROW_DOWN);
    }

    private void startSwordSpiralPhase() {
        this.phase = PHASE_SWORD_SPIRAL;
        this.phaseTicks = 0;
        setAnimationMode(ANIMATION_RUN_CYCLE);
    }

    private void startSwordGridPhase() {
        this.phase = PHASE_SWORD_GRID;
        this.phaseTicks = 0;
        setAnimationMode(ANIMATION_IDLE);
    }

    private void finishEncounter() {
        this.phase = PHASE_FINISHED;
        setAnimationMode(ANIMATION_SLEEPING);
        this.kill();
    }

    private boolean tickArrowAttack() {
        if (!this.arrowAttackSpawned) {
            spawnDirectionPreviewArrow();
            this.arrowAttackSpawned = true;
        }

        this.phaseTicks++;
        return this.phaseTicks >= ARROW_PREVIEW_TICKS;
    }

    private boolean tickArrowResolve() {
        if (this.phaseTicks < ARROW_RESOLVE_TICKS) {
            if (this.phaseTicks % ARROW_RESOLVE_INTERVAL_TICKS == 0) {
                spawnDirectionalBeamFan(this.resolvedArrowDirection, this.phaseTicks / ARROW_RESOLVE_INTERVAL_TICKS);
            }

            if (this.phaseTicks % 16 == 8) {
                spawnPlayerPressureSwords(false);
            }

            this.phaseTicks++;
            return false;
        }

        this.phaseTicks++;
        return this.phaseTicks >= ARROW_RESOLVE_TICKS + CubeBossBeamEntity.TOTAL_TICKS;
    }

    private boolean tickArrowRain() {
        if (this.phaseTicks < ARROW_RAIN_TICKS) {
            if (this.phaseTicks % ARROW_RAIN_INTERVAL_TICKS == 0) {
                spawnArrowRainBurst();
            }

            if (this.phaseTicks % ARROW_RAIN_PATTERN_INTERVAL_TICKS == 0) {
                spawnArrowRainBrokenPattern(this.phaseTicks / ARROW_RAIN_PATTERN_INTERVAL_TICKS);
            }

            if ((this.phaseTicks + 4) % ARROW_RAIN_TARGET_INTERVAL_TICKS == 0) {
                spawnArrowRainAtPlayers();
            }

            this.phaseTicks++;
            return false;
        }

        this.phaseTicks++;
        return this.phaseTicks >= ARROW_RAIN_TICKS + ARROW_RAIN_FINISH_WAIT_TICKS;
    }

    private boolean tickBeamBarrage() {
        if (this.phaseTicks < BEAM_BARRAGE_TICKS) {
            if (this.phaseTicks % BEAM_BARRAGE_INTERVAL_TICKS == 0) {
                for (int i = 0; i < BEAM_BARRAGE_BURST_SIZE; i++) {
                    spawnRandomBeam();
                }

                if ((this.phaseTicks / BEAM_BARRAGE_INTERVAL_TICKS) % 2 == 1) {
                    spawnOppositeBeamPair();
                }
            }

            if (this.phaseTicks % 20 == 5) {
                spawnPlayerColumnBeams();
            }

            this.phaseTicks++;
            return false;
        }

        this.phaseTicks++;
        return this.phaseTicks >= BEAM_BARRAGE_TICKS + CubeBossBeamEntity.TOTAL_TICKS;
    }

    private boolean tickSpiralLasers() {
        if (this.phaseTicks < SPIRAL_LASER_TICKS) {
            if (this.phaseTicks % SPIRAL_LASER_INTERVAL_TICKS == 0) {
                spawnSpiralLaserWall(this.phaseTicks / SPIRAL_LASER_INTERVAL_TICKS);
            }

            this.phaseTicks++;
            return false;
        }

        this.phaseTicks++;
        return this.phaseTicks >= SPIRAL_LASER_TICKS + SPIRAL_LASER_FINISH_WAIT_TICKS;
    }

    private boolean tickSafeTiles() {
        if (this.phaseTicks < SAFE_TILE_TICKS) {
            if (this.phaseTicks % SAFE_TILE_INTERVAL_TICKS == 0) {
                spawnSafeTileWave(this.phaseTicks / SAFE_TILE_INTERVAL_TICKS);
            }

            this.phaseTicks++;
            return false;
        }

        this.phaseTicks++;
        return this.phaseTicks >= SAFE_TILE_TICKS + CubeBossSwordEntity.TOTAL_TICKS;
    }

    private boolean tickSwordSpiral() {
        if (this.phaseTicks < SWORD_SPIRAL_TICKS) {
            if (this.phaseTicks % SWORD_SPIRAL_INTERVAL_TICKS == 0) {
                spawnSwordSpiralStep(this.phaseTicks / SWORD_SPIRAL_INTERVAL_TICKS);
            }

            if (this.phaseTicks % 16 == 8) {
                spawnPlayerPressureSwords(false);
            }

            this.phaseTicks++;
            return false;
        }

        this.phaseTicks++;
        return this.phaseTicks >= SWORD_SPIRAL_TICKS + CubeBossSwordEntity.TOTAL_TICKS;
    }

    private boolean tickSwordGrid() {
        if (this.phaseTicks < SWORD_GRID_TICKS) {
            if (this.phaseTicks % SWORD_GRID_INTERVAL_TICKS == 0) {
                spawnSwordGridStep(this.phaseTicks / SWORD_GRID_INTERVAL_TICKS);
            }

            if (this.phaseTicks % 12 == 6) {
                spawnPlayerPressureSwords(true);
            }

            this.phaseTicks++;
            return false;
        }

        this.phaseTicks++;
        return this.phaseTicks >= SWORD_GRID_TICKS + CubeBossSwordEntity.TOTAL_TICKS;
    }

    private void setAnimationMode(int animationMode) {
        this.entityData.set(DATA_ANIMATION_MODE, animationMode);
    }

    private void updateBossBarAwakeState(boolean awake) {
        this.bossEvent.setColor(awake ? BossEvent.BossBarColor.RED : BossEvent.BossBarColor.YELLOW);
        this.bossEvent.setName(Component.literal(awake ? "Cube Boss - Eveille" : "Cube Boss"));
    }

    private Direction getRandomArrowAttackDirection() {
        return switch (this.random.nextInt(4)) {
            case 0 -> Direction.UP;
            case 1 -> Direction.DOWN;
            case 2 -> Direction.WEST;
            default -> Direction.EAST;
        };
    }

    private Direction getResolvedArrowDirection(Direction shownDirection) {
        int roll = this.random.nextInt(5);

        if (roll <= 2) {
            return shownDirection;
        }

        if (roll == 3) {
            return shownDirection.getOpposite();
        }

        return switch (shownDirection) {
            case UP, DOWN -> this.random.nextBoolean() ? Direction.EAST : Direction.WEST;
            case EAST, WEST -> this.random.nextBoolean() ? Direction.UP : Direction.DOWN;
            default -> shownDirection;
        };
    }

    private void setArrowDirectionAnimation(Direction direction) {
        switch (direction) {
            case DOWN -> setAnimationMode(ANIMATION_ARROW_DOWN);
            case WEST -> setAnimationMode(ANIMATION_ARROW_LEFT);
            case EAST -> setAnimationMode(ANIMATION_ARROW_RIGHT);
            default -> setAnimationMode(ANIMATION_ARROW_UP);
        }
    }

    private void spawnDirectionPreviewArrow() {
        CubeBossArrowEntity arrow = ModEntities.CUBE_BOSS_ARROW.get().create(this.level());

        if (arrow == null) {
            return;
        }

        BlockPos origin = getArenaOrigin();
        double targetX = origin.getX() + 0.5D;
        double targetY = origin.getY() + ARENA_HEIGHT / 2.0D;
        double targetZ = origin.getZ() + 0.5D;

        arrow.setupDirectionPreview(this.arrowAttackDirection, targetX, targetY, targetZ);
        arrow.setPos(this.getX(), this.getY() + this.getBbHeight() * 0.5D, this.getZ());
        this.level().addFreshEntity(arrow);
    }

    private void spawnArrowRainBurst() {
        BlockPos origin = getArenaOrigin();

        for (int i = 0; i < ARROW_RAIN_RANDOM_BURST_SIZE; i++) {
            spawnRainArrowAt(origin, randomArenaX(origin), randomArenaZ(origin));
        }
    }

    private void spawnArrowRainBrokenPattern(int step) {
        BlockPos origin = getArenaOrigin();
        Set<BlockPos> spawned = new HashSet<>();
        int mode = Math.floorMod(step + this.random.nextInt(2), 4);
        int lane = this.random.nextInt(ARENA_HALF_SIZE * 2 + 1) - ARENA_HALF_SIZE;

        for (int i = 0; i < ARROW_RAIN_BROKEN_PATTERN_SIZE; i++) {
            int spread = -ARENA_HALF_SIZE + i * 3 + this.random.nextInt(3) - 1;
            int jitter = this.random.nextInt(5) - 2;
            int dx;
            int dz;

            switch (mode) {
                case 0 -> {
                    dx = lane + jitter;
                    dz = spread;
                }
                case 1 -> {
                    dx = spread;
                    dz = lane + jitter;
                }
                case 2 -> {
                    dx = spread;
                    dz = spread + lane / 2 + jitter;
                }
                default -> {
                    dx = spread;
                    dz = -spread + lane / 2 + jitter;
                }
            }

            spawnRainArrowAt(origin, origin.getX() + dx, origin.getZ() + dz, spawned);
        }
    }

    private void spawnArrowRainAtPlayers() {
        BlockPos origin = getArenaOrigin();
        Set<BlockPos> spawned = new HashSet<>();

        for (Player player : this.level().getEntitiesOfClass(Player.class, getArenaPlayerBox(origin))) {
            if (player.isSpectator()) {
                continue;
            }

            BlockPos playerPos = player.blockPosition();
            spawnHomingRainArrowAt(origin, playerPos.getX(), playerPos.getZ(), player.getX(), player.getZ(), spawned);

            int firstAxis = this.random.nextBoolean() ? 1 : -1;
            spawnRainArrowAt(origin, playerPos.getX() + firstAxis, playerPos.getZ(), spawned);
            spawnRainArrowAt(origin, playerPos.getX() - firstAxis, playerPos.getZ(), spawned);
            spawnRainArrowAt(origin, playerPos.getX(), playerPos.getZ() + 1, spawned);
            spawnRainArrowAt(origin, playerPos.getX(), playerPos.getZ() - 1, spawned);

            if (this.random.nextBoolean()) {
                spawnRainArrowAt(origin, playerPos.getX() + firstAxis, playerPos.getZ() + (this.random.nextBoolean() ? 1 : -1), spawned);
            }
        }
    }

    private void spawnRainArrowAt(BlockPos origin, int x, int z) {
        spawnRainArrowAt(origin, x, z, null);
    }

    private void spawnRainArrowAt(BlockPos origin, int x, int z, Set<BlockPos> spawned) {
        int clampedX = clampArenaX(origin, x);
        int clampedZ = clampArenaZ(origin, z);
        BlockPos arrowColumn = new BlockPos(clampedX, origin.getY() + ARENA_HEIGHT, clampedZ);

        if (spawned != null && !spawned.add(arrowColumn)) {
            return;
        }

        CubeBossArrowEntity arrow = ModEntities.CUBE_BOSS_ARROW.get().create(this.level());

        if (arrow == null) {
            return;
        }

        arrow.setupFallingRain(origin.getY());
        arrow.setPos(clampedX + 0.5D, origin.getY() + ARENA_HEIGHT + 0.65D, clampedZ + 0.5D);
        this.level().addFreshEntity(arrow);
    }

    private void spawnHomingRainArrowAt(BlockPos origin, int x, int z, double targetX, double targetZ, Set<BlockPos> spawned) {
        int clampedX = clampArenaX(origin, x);
        int clampedZ = clampArenaZ(origin, z);
        BlockPos arrowColumn = new BlockPos(clampedX, origin.getY() + ARENA_HEIGHT, clampedZ);

        if (spawned != null && !spawned.add(arrowColumn)) {
            return;
        }

        CubeBossArrowEntity arrow = ModEntities.CUBE_BOSS_ARROW.get().create(this.level());

        if (arrow == null) {
            return;
        }

        arrow.setPos(clampedX + 0.5D, origin.getY() + ARENA_HEIGHT + 0.65D, clampedZ + 0.5D);
        arrow.setupFallingRain(origin.getY(), targetX, targetZ, true);
        this.level().addFreshEntity(arrow);
    }

    private void spawnDirectionalBeamFan(Direction direction, int step) {
        BlockPos origin = getArenaOrigin();
        Set<BlockPos> spawned = new HashSet<>();

        if (direction == Direction.UP || direction == Direction.DOWN) {
            spawnVerticalBeamFan(origin, direction, step, spawned);
            return;
        }

        spawnHorizontalBeamFan(origin, direction, step, spawned);
    }

    private void spawnVerticalBeamFan(BlockPos origin, Direction direction, int step, Set<BlockPos> spawned) {
        int sourceY = direction == Direction.UP ? origin.getY() : origin.getY() + ARENA_HEIGHT;
        int drift = -ARENA_HALF_SIZE + Math.floorMod(step * 3, ARENA_HALF_SIZE * 2 + 1);

        for (int i = 0; i < 7; i++) {
            int dx = clampArenaOffset(drift + i * 3 - ARENA_HALF_SIZE / 2 + this.random.nextInt(3) - 1);
            int dz = clampArenaOffset((step % 2 == 0 ? dx : -dx) + this.random.nextInt(5) - 2);
            BlockPos source = new BlockPos(origin.getX() + dx, sourceY, origin.getZ() + dz);

            if (spawned.add(source)) {
                spawnBeam(direction, source);
            }
        }
    }

    private void spawnHorizontalBeamFan(BlockPos origin, Direction direction, int step, Set<BlockPos> spawned) {
        int sourceX = direction == Direction.EAST ? origin.getX() - ARENA_HALF_SIZE : origin.getX() + ARENA_HALF_SIZE;
        int y = origin.getY() + 1 + Math.floorMod(step, 4);
        int baseLane = -ARENA_HALF_SIZE + Math.floorMod(step * 4, ARENA_HALF_SIZE * 2 + 1);

        for (int i = 0; i < 5; i++) {
            int lane = clampArenaOffset(baseLane + i * 4 + this.random.nextInt(3) - 1);
            BlockPos source = new BlockPos(sourceX, y, origin.getZ() + lane);

            if (spawned.add(source)) {
                spawnBeam(direction, source);
            }
        }
    }

    private void spawnRandomBeam() {
        Direction direction = getRandomBeamDirection();
        BlockPos source = getBeamSource(direction);
        spawnBeam(direction, source);
    }

    private void spawnBeam(Direction direction, BlockPos source) {
        CubeBossBeamEntity beam = ModEntities.CUBE_BOSS_BEAM.get().create(this.level());

        if (beam == null) {
            return;
        }

        int length = getBeamLength(source, direction);
        beam.setup(direction, length);
        beam.setPos(source.getX() + 0.5D, source.getY() + 0.5D, source.getZ() + 0.5D);
        this.level().addFreshEntity(beam);
    }

    private void spawnOppositeBeamPair() {
        BlockPos origin = getArenaOrigin();
        int axis = this.random.nextInt(3);

        if (axis == 0) {
            int x = randomArenaX(origin);
            int z = randomArenaZ(origin);
            spawnBeam(Direction.UP, new BlockPos(x, origin.getY(), z));
            spawnBeam(Direction.DOWN, new BlockPos(x, origin.getY() + ARENA_HEIGHT, z));
            return;
        }

        int y = randomArenaY(origin);

        if (axis == 1) {
            int x = randomArenaX(origin);
            spawnBeam(Direction.NORTH, new BlockPos(x, y, origin.getZ() + ARENA_HALF_SIZE));
            spawnBeam(Direction.SOUTH, new BlockPos(x, y, origin.getZ() - ARENA_HALF_SIZE));
            return;
        }

        int z = randomArenaZ(origin);
        spawnBeam(Direction.WEST, new BlockPos(origin.getX() + ARENA_HALF_SIZE, y, z));
        spawnBeam(Direction.EAST, new BlockPos(origin.getX() - ARENA_HALF_SIZE, y, z));
    }

    private void spawnPlayerColumnBeams() {
        BlockPos origin = getArenaOrigin();

        for (Player player : this.level().getEntitiesOfClass(Player.class, getArenaPlayerBox(origin))) {
            if (player.isSpectator()) {
                continue;
            }

            BlockPos playerPos = player.blockPosition();
            int x = clampArenaX(origin, playerPos.getX());
            int z = clampArenaZ(origin, playerPos.getZ());

            if (this.random.nextBoolean()) {
                spawnBeam(Direction.UP, new BlockPos(x, origin.getY(), z));
            } else {
                spawnBeam(Direction.DOWN, new BlockPos(x, origin.getY() + ARENA_HEIGHT, z));
            }

            if (this.random.nextInt(3) == 0) {
                spawnBeam(Direction.UP, new BlockPos(clampArenaX(origin, x + (this.random.nextBoolean() ? 1 : -1)), origin.getY(), z));
            }
        }
    }

    private Direction getRandomBeamDirection() {
        Direction[] directions = Direction.values();
        return directions[this.random.nextInt(directions.length)];
    }

    private BlockPos getBeamSource(Direction direction) {
        BlockPos origin = getArenaOrigin();
        int x = randomArenaX(origin);
        int y = randomArenaY(origin);
        int z = randomArenaZ(origin);

        return switch (direction) {
            case UP -> new BlockPos(x, origin.getY(), z);
            case DOWN -> new BlockPos(x, origin.getY() + ARENA_HEIGHT, z);
            case NORTH -> new BlockPos(x, y, origin.getZ() + ARENA_HALF_SIZE);
            case SOUTH -> new BlockPos(x, y, origin.getZ() - ARENA_HALF_SIZE);
            case WEST -> new BlockPos(origin.getX() + ARENA_HALF_SIZE, y, z);
            case EAST -> new BlockPos(origin.getX() - ARENA_HALF_SIZE, y, z);
        };
    }

    private int getBeamLength(BlockPos source, Direction direction) {
        for (int distance = 1; distance <= MAX_BEAM_LENGTH; distance++) {
            if (isSolid(source.relative(direction, distance))) {
                return distance;
            }
        }

        return MAX_BEAM_LENGTH;
    }

    private void spawnSpiralLaserWall(int wave) {
        BlockPos origin = getArenaOrigin();
        Direction forward = getArenaForwardDirection(origin);
        int maxGapOffset = Math.max(1, ARENA_HALF_SIZE - SPIRAL_LASER_GAP_HALF_WIDTH - 2);
        double angle = wave * 0.72D;
        int gapCenter = (int) Math.round(Math.sin(angle) * maxGapOffset);
        double y = origin.getY() + 1.1D + Math.cos(angle) * 0.12D;

        if (forward.getAxis() == Direction.Axis.Z) {
            spawnMovingLaserWallOnX(origin, forward, y, gapCenter);
        } else {
            spawnMovingLaserWallOnZ(origin, forward, y, gapCenter);
        }
    }

    private Direction getArenaForwardDirection(BlockPos origin) {
        double dx = this.getX() - (origin.getX() + 0.5D);
        double dz = this.getZ() - (origin.getZ() + 0.5D);

        if (Math.abs(dx) >= Math.abs(dz)) {
            return dx >= 0.0D ? Direction.EAST : Direction.WEST;
        }

        return dz >= 0.0D ? Direction.SOUTH : Direction.NORTH;
    }

    private void spawnMovingLaserWallOnX(BlockPos origin, Direction forward, double y, int gapCenter) {
        double z = origin.getZ() - forward.getStepZ() * (ARENA_HALF_SIZE + 1) + 0.5D;
        double motionZ = forward.getStepZ() * SPIRAL_LASER_SPEED;

        spawnMovingLaserSegments(
                Direction.EAST,
                origin.getX(),
                y,
                z,
                gapCenter,
                0.0D,
                0.0D,
                motionZ,
                true
        );
    }

    private void spawnMovingLaserWallOnZ(BlockPos origin, Direction forward, double y, int gapCenter) {
        double x = origin.getX() - forward.getStepX() * (ARENA_HALF_SIZE + 1) + 0.5D;
        double motionX = forward.getStepX() * SPIRAL_LASER_SPEED;

        spawnMovingLaserSegments(
                Direction.SOUTH,
                x,
                y,
                origin.getZ(),
                gapCenter,
                motionX,
                0.0D,
                0.0D,
                false
        );
    }

    private void spawnMovingLaserSegments(
            Direction beamDirection,
            double fixedX,
            double y,
            double fixedZ,
            int gapCenter,
            double motionX,
            double motionY,
            double motionZ,
            boolean spansX
    ) {
        int min = -ARENA_HALF_SIZE;
        int max = ARENA_HALF_SIZE;
        int gapStart = Math.max(min + 1, gapCenter - SPIRAL_LASER_GAP_HALF_WIDTH);
        int gapEnd = Math.min(max - 1, gapCenter + SPIRAL_LASER_GAP_HALF_WIDTH);
        int leftEnd = gapStart - 1;
        int rightStart = gapEnd + 1;

        spawnMovingLaserSegment(beamDirection, fixedX, y, fixedZ, min, leftEnd, motionX, motionY, motionZ, spansX);
        spawnMovingLaserSegment(beamDirection, fixedX, y, fixedZ, rightStart, max, motionX, motionY, motionZ, spansX);
    }

    private void spawnMovingLaserSegment(
            Direction beamDirection,
            double fixedX,
            double y,
            double fixedZ,
            int startOffset,
            int endOffset,
            double motionX,
            double motionY,
            double motionZ,
            boolean spansX
    ) {
        int length = endOffset - startOffset;

        if (length <= 0) {
            return;
        }

        double x = spansX ? fixedX + startOffset + 0.5D : fixedX;
        double z = spansX ? fixedZ : fixedZ + startOffset + 0.5D;
        CubeBossBeamEntity beam = ModEntities.CUBE_BOSS_BEAM.get().create(this.level());

        if (beam == null) {
            return;
        }

        beam.setupMoving(
                beamDirection,
                length,
                SPIRAL_LASER_WARNING_TICKS,
                SPIRAL_LASER_ACTIVE_TICKS,
                motionX,
                motionY,
                motionZ
        );
        beam.setPos(x, y, z);
        this.level().addFreshEntity(beam);
    }

    private void spawnSafeTileWave(int wave) {
        Set<BlockPos> spawned = new HashSet<>();

        for (int dx = -ARENA_HALF_SIZE; dx <= ARENA_HALF_SIZE; dx++) {
            for (int dz = -ARENA_HALF_SIZE; dz <= ARENA_HALF_SIZE; dz++) {
                if ((Math.abs(dx) + Math.abs(dz) + wave) % 2 != 0 || isSafeTile(dx, dz, wave)) {
                    continue;
                }

                spawnSwordAtArenaOffset(dx, dz, spawned);
            }
        }
    }

    private boolean isSafeTile(int dx, int dz, int wave) {
        int firstSafeX = -6 + Math.floorMod(wave * 5, 13);
        int firstSafeZ = -6 + Math.floorMod(wave * 7 + 3, 13);
        int secondSafeX = -firstSafeZ;
        int secondSafeZ = firstSafeX;

        return isNearSafeCenter(dx, dz, firstSafeX, firstSafeZ)
                || isNearSafeCenter(dx, dz, secondSafeX, secondSafeZ);
    }

    private boolean isNearSafeCenter(int dx, int dz, int safeX, int safeZ) {
        return Math.abs(dx - safeX) <= 1 && Math.abs(dz - safeZ) <= 1;
    }

    private void spawnSwordSpiralStep(int step) {
        Set<BlockPos> spawned = new HashSet<>();
        int radius = 2 + step % (ARENA_HALF_SIZE - 1);
        double baseAngle = step * 0.55D;

        for (int arm = 0; arm < 4; arm++) {
            double angle = baseAngle + arm * Math.PI / 2.0D;
            int dx = (int) Math.round(Math.cos(angle) * radius);
            int dz = (int) Math.round(Math.sin(angle) * radius);
            spawnSwordAtArenaOffset(dx, dz, spawned);
        }

        if (step % 2 == 0) {
            int reverseRadius = ARENA_HALF_SIZE - step % ARENA_HALF_SIZE;
            double reverseAngle = -baseAngle * 0.75D;

            for (int arm = 0; arm < 4; arm++) {
                double angle = reverseAngle + arm * Math.PI / 2.0D;
                int dx = (int) Math.round(Math.cos(angle) * reverseRadius);
                int dz = (int) Math.round(Math.sin(angle) * reverseRadius);
                spawnSwordAtArenaOffset(dx, dz, spawned);
            }
        }

        if (step % 5 == 0) {
            spawnSwordAtArenaOffset(0, 0, spawned);
        }
    }

    private void spawnSwordGridStep(int step) {
        Set<BlockPos> spawned = new HashSet<>();
        int sweepX = -ARENA_HALF_SIZE + Math.floorMod(step * 4, ARENA_HALF_SIZE * 2 + 1);
        int sweepZ = -ARENA_HALF_SIZE + Math.floorMod(step * 5 + 7, ARENA_HALF_SIZE * 2 + 1);

        for (int dz = -ARENA_HALF_SIZE; dz <= ARENA_HALF_SIZE; dz += 2) {
            spawnSwordAtArenaOffset(sweepX, dz, spawned);
        }

        for (int dx = -ARENA_HALF_SIZE; dx <= ARENA_HALF_SIZE; dx += 3) {
            spawnSwordAtArenaOffset(dx, sweepZ, spawned);
        }

        int checkerOffset = step % 2 == 0 ? 0 : 2;

        for (int dx = -8; dx <= 8; dx += 4) {
            for (int dz = -8 + checkerOffset; dz <= 8; dz += 4) {
                spawnSwordAtArenaOffset(dx, dz, spawned);
            }
        }
    }

    private void spawnPlayerPressureSwords(boolean wide) {
        BlockPos origin = getArenaOrigin();
        Set<BlockPos> spawned = new HashSet<>();

        for (Player player : this.level().getEntitiesOfClass(Player.class, getArenaPlayerBox(origin))) {
            if (player.isSpectator()) {
                continue;
            }

            int dx = clampArenaOffset(player.blockPosition().getX() - origin.getX());
            int dz = clampArenaOffset(player.blockPosition().getZ() - origin.getZ());
            spawnSwordAtArenaOffset(dx, dz, spawned);

            int side = this.random.nextBoolean() ? 1 : -1;
            spawnSwordAtArenaOffset(dx + side, dz, spawned);
            spawnSwordAtArenaOffset(dx, dz - side, spawned);

            if (wide) {
                spawnSwordAtArenaOffset(dx - side, dz + side, spawned);
                spawnSwordAtArenaOffset(dx + this.random.nextInt(5) - 2, dz + this.random.nextInt(5) - 2, spawned);
            }
        }
    }

    private void spawnSwordAtArenaOffset(int dx, int dz, Set<BlockPos> spawned) {
        int clampedDx = clampArenaOffset(dx);
        int clampedDz = clampArenaOffset(dz);
        BlockPos surface = findAttackSurface(getArenaOrigin().offset(clampedDx, 0, clampedDz));

        if (surface != null && spawned.add(surface)) {
            spawnSwordWarning(surface);
        }
    }

    private int randomArenaX(BlockPos origin) {
        return origin.getX() + this.random.nextInt(ARENA_HALF_SIZE * 2 + 1) - ARENA_HALF_SIZE;
    }

    private int randomArenaY(BlockPos origin) {
        return origin.getY() + 1 + this.random.nextInt(Math.max(1, ARENA_HEIGHT - 1));
    }

    private int randomArenaZ(BlockPos origin) {
        return origin.getZ() + this.random.nextInt(ARENA_HALF_SIZE * 2 + 1) - ARENA_HALF_SIZE;
    }

    private AABB getArenaPlayerBox(BlockPos origin) {
        return new AABB(
                origin.getX() - ARENA_HALF_SIZE - 1.0D,
                origin.getY() - 1.0D,
                origin.getZ() - ARENA_HALF_SIZE - 1.0D,
                origin.getX() + ARENA_HALF_SIZE + 2.0D,
                origin.getY() + ARENA_HEIGHT + 2.0D,
                origin.getZ() + ARENA_HALF_SIZE + 2.0D
        );
    }

    private int clampArenaX(BlockPos origin, int x) {
        return origin.getX() + clampArenaOffset(x - origin.getX());
    }

    private int clampArenaZ(BlockPos origin, int z) {
        return origin.getZ() + clampArenaOffset(z - origin.getZ());
    }

    private int clampArenaOffset(int offset) {
        return Math.max(-ARENA_HALF_SIZE, Math.min(ARENA_HALF_SIZE, offset));
    }

    private BlockPos findAttackSurface(BlockPos around) {
        BlockPos origin = getArenaOrigin();
        int startY = origin.getY() + ARENA_HEIGHT;
        int minY = origin.getY() - 1;

        for (int y = startY; y >= minY; y--) {
            BlockPos feet = new BlockPos(around.getX(), y, around.getZ());

            if (isSolid(feet.below()) && !isSolid(feet)) {
                return feet;
            }
        }

        return null;
    }

    private BlockPos getArenaOrigin() {
        if (this.stelePos != null) {
            return this.stelePos;
        }

        if (!this.steleLookupDone) {
            this.steleLookupDone = true;
            this.stelePos = findNearestStele();

            if (this.stelePos != null) {
                return this.stelePos;
            }
        }

        return this.blockPosition();
    }

    private BlockPos findNearestStele() {
        BlockPos bossPos = this.blockPosition();
        BlockPos from = bossPos.offset(-ARENA_HALF_SIZE - 6, -4, -ARENA_HALF_SIZE - 6);
        BlockPos to = bossPos.offset(ARENA_HALF_SIZE + 6, 4, ARENA_HALF_SIZE + 6);

        for (BlockPos pos : BlockPos.betweenClosed(from, to)) {
            if (this.level().getBlockState(pos).is(ModBlocks.CUBE_BOSS_STELE.get())) {
                return pos.immutable();
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
