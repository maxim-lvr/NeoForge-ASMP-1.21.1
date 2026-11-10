package net.maximlvr.asmpthings.entity.custom;

import net.maximlvr.asmpthings.AsmpThingsMod;
import net.maximlvr.asmpthings.network.payload.ArmiBossCinematicPayload;
import net.maximlvr.asmpthings.network.payload.ArmiBossMovePayload;
import net.maximlvr.asmpthings.network.payload.ArmiBossPathPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Map;

@EventBusSubscriber(modid = AsmpThingsMod.MOD_ID)
public class ArmiBossEntity extends Zombie {
    public static final int ARENA_HALF_SIZE = 40;
    public static final int WAIT_TICKS = 100;
    public static final int CINEMATIC_TICKS = 100;
    public static final double MODEL_HEIGHT = ArmiBossGeometry.HEIGHT;
    public static final float HAND_WIDTH = ArmiBossGeometry.HAND_WIDTH;
    public static final float HAND_HEIGHT = ArmiBossGeometry.HAND_HEIGHT;
    public static final float HAND_DEPTH = ArmiBossGeometry.HAND_DEPTH;
    public static final int CONTROL_TICKS = 100;
    private static final int RESCUE_TICKS = 60;
    private static final EntityDataAccessor<Integer> RESCUED_PLAYER = SynchedEntityData.defineId(
            ArmiBossEntity.class, EntityDataSerializers.INT);
    private enum Phase { IDLE, WAITING, CINEMATIC, CONTROL, HAND_VIEW }

    // Reserve the participant during the delay too, so two bosses cannot take control.
    private static final Map<ServerPlayer, ArmiBossEntity> PARTICIPANTS = new HashMap<>();
    private ServerPlayer participant;
    private Phase phase = Phase.IDLE;
    private int phaseTicks;
    private Vec3 lockedPosition;
    private float lockedYaw;
    private long lastPathRequest = Long.MIN_VALUE;
    private boolean restoreFlying;
    private float restingYaw;
    private Vec3 rescueStart;
    private Vec3 rescueTarget;
    private int rescueTicks;
    public final AnimationState idleAnimationState = new AnimationState();

    public ArmiBossEntity(EntityType<? extends Zombie> type, Level level) {
        super(type, level);
        setNoAi(true);
        setNoGravity(true);
        setPersistenceRequired();
        setCanPickUpLoot(false);
        xpReward = 0;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Zombie.createAttributes()
                .add(Attributes.MAX_HEALTH, 100.0)
                .add(Attributes.MOVEMENT_SPEED, 0.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.SPAWN_REINFORCEMENTS_CHANCE, 0.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(RESCUED_PLAYER, -1);
    }

    public int rescuedPlayerId() { return entityData.get(RESCUED_PLAYER); }

    public boolean isRescuing() { return rescuedPlayerId() >= 0; }

    @Override
    protected void registerGoals() {
    }

    @Override
    protected boolean isSunSensitive() { return false; }

    @Override
    protected boolean convertsInWater() { return false; }

    @Override
    protected boolean shouldDespawnInPeaceful() { return false; }

    @Override
    public boolean isPushable() { return false; }

    @Override
    protected AABB makeBoundingBox() {
        return ArmiBossGeometry.handBounds(position(), getYRot());
    }

    @Override
    public void setYRot(float yaw) {
        super.setYRot(yaw);
        setBoundingBox(makeBoundingBox());
    }

    @Override
    public void refreshDimensions() {
        super.refreshDimensions();
        setBoundingBox(makeBoundingBox());
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        AABB bounds = new AABB(getX() - 32, getY() - MODEL_HEIGHT, getZ() - 32,
                getX() + 32, getY() + 24, getZ() + 32);
        var rescued = level().getEntity(rescuedPlayerId());
        return rescued == null ? bounds : bounds.minmax(rescued.getBoundingBox().inflate(12));
    }

    @Override
    protected boolean shouldDropLoot() { return false; }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
                                        MobSpawnType reason, SpawnGroupData spawnData) {
        setBaby(false);
        return spawnData;
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setNoAi(true);
        setNoGravity(true);
        setBaby(false);
        setCanPickUpLoot(false);
        // A test never resumes with a player locked after loading a world.
        releaseParticipant();
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return source.is(DamageTypes.GENERIC_KILL) && super.hurt(source, amount);
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!player.getItemInHand(hand).is(Items.DIAMOND)) {
            return InteractionResult.PASS;
        }
        if (!level().isClientSide && player instanceof ServerPlayer serverPlayer && isAlive()) {
            if (phase != Phase.IDLE || PARTICIPANTS.containsKey(serverPlayer)) {
                player.displayClientMessage(Component.literal("Un test est deja en cours."), true);
                return InteractionResult.CONSUME;
            }
            if (player.isSpectator() || player.isSleeping() || findStandingPosition(serverPlayer) == null) {
                player.displayClientMessage(Component.literal("Aucun sol libre a proximite pour lancer le test."), true);
                return InteractionResult.CONSUME;
            }
            participant = serverPlayer;
            restingYaw = getYRot();
            PARTICIPANTS.put(serverPlayer, this);
            phase = Phase.WAITING;
            phaseTicks = 0;
            if (!player.getAbilities().instabuild) {
                player.getItemInHand(hand).shrink(1);
            }
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    @Override
    public void tick() {
        super.tick();
        setDeltaMovement(Vec3.ZERO);
        if (level().isClientSide) {
            idleAnimationState.startIfStopped(tickCount);
        }
        if (level().isClientSide || phase == Phase.IDLE) {
            return;
        }
        if (!isAlive() || participant == null || !participant.isAlive() || participant.hasDisconnected()
                || participant.isSpectator() || participant.level() != level() || !insideArena(participant.position())) {
            releaseParticipant();
            return;
        }
        if (tickRescue()) return;
        phaseTicks++;
        if (phase == Phase.WAITING && phaseTicks >= WAIT_TICKS) {
            beginCinematic();
        } else if (phase == Phase.CINEMATIC && phaseTicks >= CINEMATIC_TICKS) {
            phase = Phase.CONTROL;
            phaseTicks = 0;
            PacketDistributor.sendToPlayer(participant, new ArmiBossCinematicPayload(
                    getUUID(), getId(), lockedPosition, position(), CINEMATIC_TICKS, true));
        } else if (phase == Phase.CONTROL && phaseTicks >= CONTROL_TICKS) {
            phase = Phase.HAND_VIEW;
            phaseTicks = 0;
            if (restoreFlying && participant.getAbilities().mayfly) {
                participant.getAbilities().flying = true;
                participant.onUpdateAbilities();
            }
            restoreFlying = false;
            PacketDistributor.sendToPlayer(participant, new ArmiBossCinematicPayload(
                    getUUID(), getId(), lockedPosition, position(), CINEMATIC_TICKS, false, true));
        }
        if (phase == Phase.HAND_VIEW) {
            Vec3 direction = participant.position().subtract(position());
            float yaw = (float) Math.toDegrees(Math.atan2(-direction.x, direction.z));
            setYRot(yaw);
            setYHeadRot(yaw);
            setYBodyRot(yaw);
        }
    }

    private boolean insideArena(Vec3 position) {
        BlockPos center = blockPosition();
        return Math.abs(position.x - (center.getX() + 0.5)) <= ARENA_HALF_SIZE + 0.5
                && Math.abs(position.z - (center.getZ() + 0.5)) <= ARENA_HALF_SIZE + 0.5;
    }

    private boolean tickRescue() {
        if (phase != Phase.CONTROL && phase != Phase.HAND_VIEW) return false;
        if (!isRescuing()) {
            if (participant.getY() >= lockedPosition.y - 3 || participant.getAbilities().flying
                    || participant.getDeltaMovement().y > 0) return false;
            rescueTarget = findRescueLanding();
            if (rescueTarget == null) {
                participant.displayClientMessage(Component.literal("La plateforme de retour est inaccessible."), true);
                releaseParticipant();
                return true;
            }
            rescueStart = participant.position();
            rescueTicks = 0;
            participant.stopRiding();
            participant.stopFallFlying();
            entityData.set(RESCUED_PLAYER, participant.getId());
            PacketDistributor.sendToPlayer(participant, new ArmiBossPathPayload(getUUID(), java.util.List.of()));
        }
        double progress = Math.min(1.0, ++rescueTicks / (double) RESCUE_TICKS);
        double smooth = progress * progress * (3 - 2 * progress);
        Vec3 position = rescueStart.lerp(rescueTarget, smooth).add(0, Math.sin(Math.PI * progress) * 3, 0);
        participant.setDeltaMovement(Vec3.ZERO);
        participant.fallDistance = 0;
        participant.connection.teleport(position.x, position.y, position.z, participant.getYRot(), participant.getXRot());
        if (rescueTicks >= RESCUE_TICKS) {
            // Recheck the landing in case the platform or the held item changed during transport.
            Vec3 landing = findRescueLanding();
            if (landing != null) {
                participant.connection.teleport(landing.x, landing.y, landing.z, participant.getYRot(), participant.getXRot());
            }
            entityData.set(RESCUED_PLAYER, -1);
            rescueStart = null;
            rescueTarget = null;
            if (landing == null) releaseParticipant();
        }
        return true;
    }

    private Vec3 findRescueLanding() {
        // The starting platform is the return anchor; never modify the arena to make a landing.
        BlockPos anchor = BlockPos.containing(lockedPosition);
        for (int radius = 0; radius <= 4; radius++) {
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    if (Math.max(Math.abs(x), Math.abs(z)) != radius) continue;
                    BlockPos feet = anchor.offset(x, 0, z);
                    BlockPos floor = feet.below();
                    if (!level().hasChunkAt(feet) || !insideArena(Vec3.atBottomCenterOf(feet))) continue;
                    var state = level().getBlockState(floor);
                    Vec3 landing = Vec3.atBottomCenterOf(feet);
                    AABB box = participant.getDimensions(Pose.STANDING).makeBoundingBox(landing);
                    if (state.isFaceSturdy(level(), floor, Direction.UP)
                            && !ArmiBossPlatforms.hidden(state, floor, participant)
                            && level().getFluidState(feet).isEmpty() && level().getFluidState(feet.above()).isEmpty()
                            && level().getWorldBorder().isWithinBounds(box) && level().noCollision(participant, box)
                            && !box.intersects(getBoundingBox())) return landing;
                }
            }
        }
        return null;
    }

    private Vec3 findStandingPosition(ServerPlayer player) {
        Vec3 nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        BlockPos center = blockPosition();
        for (int x = center.getX() - ARENA_HALF_SIZE; x <= center.getX() + ARENA_HALF_SIZE; x++) {
            for (int z = center.getZ() - ARENA_HALF_SIZE; z <= center.getZ() + ARENA_HALF_SIZE; z++) {
                for (int y = player.blockPosition().getY() - 2; y <= player.blockPosition().getY() + 2; y++) {
                    Vec3 candidate = new Vec3(x + 0.5, y, z + 0.5);
                    double distance = candidate.distanceToSqr(player.position());
                    if (distance >= nearestDistance) {
                        continue;
                    }
                    BlockPos feet = BlockPos.containing(candidate);
                    AABB bounds = player.getDimensions(Pose.STANDING).makeBoundingBox(candidate);
                    if (!level().hasChunkAt(feet) || !level().getWorldBorder().isWithinBounds(bounds)
                            || bounds.intersects(getBoundingBox().inflate(0.25))
                            || !level().getBlockState(feet.below()).isFaceSturdy(level(), feet.below(), Direction.UP)
                            || !level().getFluidState(feet).isEmpty() || !level().getFluidState(feet.above()).isEmpty()
                            || !level().noCollision(player, bounds)) {
                        continue;
                    }
                    nearest = candidate;
                    nearestDistance = distance;
                }
            }
        }
        return nearest;
    }

    private void beginCinematic() {
        lockedPosition = findStandingPosition(participant);
        if (lockedPosition == null) {
            participant.displayClientMessage(Component.literal("Test annule : le sol n'est plus libre."), true);
            releaseParticipant();
            return;
        }
        lockedYaw = (float) Math.toDegrees(Math.atan2(-(getX() - lockedPosition.x), getZ() - lockedPosition.z));
        participant.stopRiding();
        participant.stopFallFlying();
        restoreFlying = participant.getAbilities().flying;
        if (restoreFlying) {
            participant.getAbilities().flying = false;
            participant.onUpdateAbilities();
        }
        participant.setSprinting(false);
        participant.setShiftKeyDown(false);
        participant.connection.teleport(lockedPosition.x, lockedPosition.y, lockedPosition.z, lockedYaw, 0.0F);
        phase = Phase.CINEMATIC;
        phaseTicks = 0;
        holdParticipant();
        PacketDistributor.sendToPlayer(participant, new ArmiBossCinematicPayload(
                getUUID(), getId(), lockedPosition, position(), CINEMATIC_TICKS, false));
    }

    public static void handleMove(ServerPlayer player, ArmiBossMovePayload payload) {
        ArmiBossEntity boss = PARTICIPANTS.get(player);
        if (boss == null || !boss.getUUID().equals(payload.bossUuid())) {
            return;
        }
        if (payload.cancel()) {
            boss.releaseParticipant();
            return;
        }
        if (boss.phase != Phase.CONTROL || boss.isRescuing() || !boss.isAlive() || !player.isAlive() || player.level() != boss.level()) {
            return;
        }
        long now = boss.level().getGameTime();
        if (boss.lastPathRequest != Long.MIN_VALUE && now - boss.lastPathRequest < 4) {
            return;
        }
        boss.lastPathRequest = now;
        var nodes = ArmiBossPathfinding.find(player, boss, payload.block());
        PacketDistributor.sendToPlayer(player, new ArmiBossPathPayload(boss.getUUID(), nodes));
        if (nodes.isEmpty()) {
            player.displayClientMessage(Component.literal("Ce bloc est inaccessible."), true);
        }
    }

    private void holdParticipant() {
        participant.setDeltaMovement(Vec3.ZERO);
        participant.fallDistance = 0;
        participant.setSprinting(false);
        if (participant.position().distanceToSqr(lockedPosition) > 1.0E-6) {
            participant.connection.teleport(lockedPosition.x, lockedPosition.y, lockedPosition.z, lockedYaw, 0.0F);
        }
        participant.setYRot(lockedYaw);
        participant.setXRot(0.0F);
        participant.setYHeadRot(lockedYaw);
        participant.setYBodyRot(lockedYaw);
    }

    public static Vec3 combatCenter(Player player) {
        ArmiBossEntity boss = PARTICIPANTS.get(player);
        return boss != null && boss.phase != Phase.IDLE && boss.phase != Phase.WAITING
                && boss.level() == player.level() ? boss.position() : null;
    }

    private void releaseParticipant() {
        entityData.set(RESCUED_PLAYER, -1);
        rescueStart = null;
        rescueTarget = null;
        rescueTicks = 0;
        if (phase != Phase.IDLE) {
            setYRot(restingYaw);
            setYHeadRot(restingYaw);
            setYBodyRot(restingYaw);
            setDeltaMovement(Vec3.ZERO);
        }
        if (participant != null) {
            participant.setDeltaMovement(Vec3.ZERO);
            participant.fallDistance = 0;
            PARTICIPANTS.remove(participant, this);
            if (restoreFlying && participant.getAbilities().mayfly) {
                participant.getAbilities().flying = true;
                participant.onUpdateAbilities();
            }
            if ((phase == Phase.CINEMATIC || phase == Phase.CONTROL || phase == Phase.HAND_VIEW) && !participant.hasDisconnected()) {
                PacketDistributor.sendToPlayer(participant, new ArmiBossCinematicPayload(
                        getUUID(), getId(), Vec3.ZERO, Vec3.ZERO, 0, false));
            }
        }
        participant = null;
        lockedPosition = null;
        phase = Phase.IDLE;
        phaseTicks = 0;
        lastPathRequest = Long.MIN_VALUE;
        restoreFlying = false;
    }

    @Override
    public void die(DamageSource source) {
        releaseParticipant();
        super.die(source);
    }

    @Override
    public void remove(RemovalReason reason) {
        releaseParticipant();
        super.remove(reason);
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ArmiBossEntity boss = PARTICIPANTS.get(player);
            if (boss != null && boss.phase == Phase.CINEMATIC) {
                if (player.level() != boss.level() || !player.isAlive() || boss.isRemoved()) {
                    boss.releaseParticipant();
                } else {
                    boss.holdParticipant();
                }
            }
        }
    }

    @SubscribeEvent
    public static void onParticipantDamage(LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ArmiBossEntity boss = PARTICIPANTS.get(player);
            if (boss != null && boss.phase != Phase.WAITING
                    && (event.getSource().is(DamageTypes.FALL) || boss.isRescuing())
                    && !event.getSource().is(DamageTypes.GENERIC_KILL)) event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        ArmiBossEntity boss = PARTICIPANTS.get(event.getEntity());
        if (boss != null) {
            boss.releaseParticipant();
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        PARTICIPANTS.clear();
    }
}
