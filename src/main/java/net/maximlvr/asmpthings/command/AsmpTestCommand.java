package net.maximlvr.asmpthings.command;

import net.maximlvr.asmpthings.AsmpThingsMod;
import net.maximlvr.asmpthings.entity.custom.ArmiBossEntity;
import net.maximlvr.asmpthings.network.payload.AsmpTestInputPayload;
import net.maximlvr.asmpthings.network.payload.AsmpTestViewPayload;
import net.minecraft.commands.Commands;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

@EventBusSubscriber(modid = AsmpThingsMod.MOD_ID)
public final class AsmpTestCommand {
    private static final Map<ServerPlayer, Session> SESSIONS = new HashMap<>();
    private static final class Session {
        final ServerPlayer target;
        final boolean control;
        AsmpTestInputPayload input;
        long inputTick;
        int actionCooldown;
        List<net.minecraft.world.item.ItemStack> hotbar = List.of();
        Session(ServerPlayer target, boolean control) { this.target = target; this.control = control; }
    }

    @SubscribeEvent
    public static void commands(RegisterCommandsEvent event) {
        var root = Commands.literal("asmptest").requires(s -> s.hasPermission(2));
        for (String mode : List.of("camera", "switch")) {
            root.then(Commands.literal(mode).then(Commands.argument("player1", EntityArgument.player())
                    .then(Commands.argument("player2", EntityArgument.player()).executes(c -> start(c.getSource(),
                            EntityArgument.getPlayer(c, "player1"), EntityArgument.getPlayer(c, "player2"), mode.equals("switch"))))));
        }
        root.then(Commands.literal("stop").executes(c -> { stop(c.getSource().getPlayerOrException()); return 1; })
                .then(Commands.argument("player", EntityArgument.player()).executes(c -> { stop(EntityArgument.getPlayer(c, "player")); return 1; })));
        event.getDispatcher().register(root);
    }

    private static boolean available(ServerPlayer p) {
        return p.isAlive() && !p.hasDisconnected() && !p.isSpectator() && !p.isPassenger()
                && !p.isSleeping() && ArmiBossEntity.combatCenter(p) == null;
    }

    private static int start(CommandSourceStack source, ServerPlayer a, ServerPlayer b, boolean control) {
        if (a == b || !available(a) || !available(b) || a.level() != b.level() || a.distanceToSqr(b) > 32 * 32
                || (control && (a.isCreative() || b.isCreative() || a.getAbilities().flying || b.getAbilities().flying))
                || SESSIONS.containsKey(a) || SESSIONS.containsKey(b)
                || SESSIONS.values().stream().anyMatch(s -> s.target == a || s.target == b)) {
            source.sendFailure(Component.literal("POC : deux joueurs libres, proches (32 blocs), meme dimension ; switch en survie, sans vol."));
            return 0;
        }
        a.closeContainer();
        if (control) b.closeContainer();
        SESSIONS.put(a, new Session(b, control));
        PacketDistributor.sendToPlayer(a, new AsmpTestViewPayload(b.getId(), b.getUUID(), control));
        if (control) {
            SESSIONS.put(b, new Session(a, true));
            PacketDistributor.sendToPlayer(b, new AsmpTestViewPayload(a.getId(), a.getUUID(), true));
        }
        source.sendSuccess(() -> Component.literal("POC actif. /asmptest stop <joueur> pour terminer."), false);
        return 1;
    }

    public static boolean controlsOther(ServerPlayer player) {
        Session s = SESSIONS.get(player);
        return s != null && s.control;
    }

    public static void input(ServerPlayer player, AsmpTestInputPayload input) {
        Session s = SESSIONS.get(player);
        if (s == null) return;
        if (input.has(AsmpTestInputPayload.STOP)) { stop(player); return; }
        if (!s.control || !Float.isFinite(input.forward()) || !Float.isFinite(input.left())
                || !Float.isFinite(input.yaw()) || !Float.isFinite(input.pitch()) || input.slot() < 0 || input.slot() > 8) return;
        if (input.has(AsmpTestInputPayload.INVENTORY) && player.containerMenu == player.inventoryMenu) {
            AsmpTestInventory.open(player, s.target);
        }
        s.input = input;
        s.inputTick = player.serverLevel().getGameTime();
    }

    @SubscribeEvent
    public static void tick(ServerTickEvent.Post event) {
        for (ServerPlayer controller : List.copyOf(SESSIONS.keySet())) {
            Session s = SESSIONS.get(controller);
            if (s == null) continue;
            ServerPlayer body = s.target;
            if (!available(controller) || !available(body) || controller.level() != body.level()
                    || controller.distanceToSqr(body) > 48 * 48) { stop(controller); continue; }
            if (!s.control) continue;
            boolean changed = s.hotbar.size() != 9;
            for (int i = 0; i < 9 && !changed; i++)
                changed = !net.minecraft.world.item.ItemStack.matches(s.hotbar.get(i), body.getInventory().getItem(i));
            if (changed) {
                s.hotbar = java.util.stream.IntStream.range(0, 9).mapToObj(i -> body.getInventory().getItem(i).copy()).toList();
                PacketDistributor.sendToPlayer(controller, new net.maximlvr.asmpthings.network.payload.AsmpTestHotbarPayload(s.hotbar));
            }
            var input = s.input;
            boolean fresh = input != null && controller.serverLevel().getGameTime() - s.inputTick < 10;
            boolean moving = fresh && controller.containerMenu == controller.inventoryMenu;
            // The owning client runs vanilla LocalPlayer physics and sends normal movement packets.
            var relayed = new AsmpTestInputPayload(moving ? Math.clamp(input.forward(), -1, 1) : 0,
                    moving ? Math.clamp(input.left(), -1, 1) : 0,
                    fresh ? input.yaw() : body.getYRot(), fresh ? Math.clamp(input.pitch(), -90, 90) : body.getXRot(),
                    moving ? input.flags() & (AsmpTestInputPayload.JUMP | AsmpTestInputPayload.SPRINT | AsmpTestInputPayload.SNEAK) : 0,
                    fresh ? input.slot() : body.getInventory().selected);
            if (fresh) body.getInventory().selected = input.slot();
            PacketDistributor.sendToPlayer(body, new net.maximlvr.asmpthings.network.payload.AsmpTestRelayPayload(controller.getUUID(), relayed));
            if (s.actionCooldown > 0) s.actionCooldown--;
            if (moving && s.actionCooldown == 0 && input.has(AsmpTestInputPayload.USE)) {
                var hit = body.level().clip(new ClipContext(body.getEyePosition(),
                        body.getEyePosition().add(body.getLookAngle().scale(body.blockInteractionRange())),
                        ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, body));
                // Modded menus would be sent to the body's connection, so the POC only places blocks/uses items.
                if (hit.getType() == HitResult.Type.BLOCK && body.getMainHandItem().getItem() instanceof net.minecraft.world.item.BlockItem) {
                    body.gameMode.useItemOn(body, body.level(), body.getMainHandItem(), InteractionHand.MAIN_HAND, hit);
                }
                s.actionCooldown = 6;
            }
            if (moving && s.actionCooldown == 0 && input.has(AsmpTestInputPayload.ATTACK)) {
                var start = body.getEyePosition();
                var end = start.add(body.getLookAngle().scale(body.entityInteractionRange()));
                var hit = net.minecraft.world.entity.projectile.ProjectileUtil.getEntityHitResult(body.level(), body, start, end,
                        body.getBoundingBox().expandTowards(end.subtract(start)).inflate(1), e -> e.isPickable() && e != body);
                if (hit != null && body.hasLineOfSight(hit.getEntity())) {
                    body.attack(hit.getEntity()); body.swing(InteractionHand.MAIN_HAND);
                }
                s.actionCooldown = 10;
            }
        }
    }

    public static boolean paired(ServerPlayer controller, ServerPlayer body) {
        Session s = SESSIONS.get(controller);
        return s != null && s.control && s.target == body;
    }

    public static void stop(ServerPlayer player) {
        Session session = SESSIONS.remove(player);
        if (session == null) return;
        reset(player);
        if (session.control) {
            SESSIONS.remove(session.target);
            reset(session.target);
        }
    }

    private static void reset(ServerPlayer player) {
        player.setSprinting(false); player.setShiftKeyDown(false);
        player.setDeltaMovement(Vec3.ZERO);
        player.closeContainer();
        if (!player.hasDisconnected()) {
            PacketDistributor.sendToPlayer(player, new AsmpTestViewPayload(-1, player.getUUID(), false));
            player.inventoryMenu.broadcastFullState();
        }
    }

    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            stop(player);
            for (var controller : List.copyOf(SESSIONS.keySet())) if (SESSIONS.get(controller).target == player) stop(controller);
        }
    }
    @SubscribeEvent public static void shutdown(ServerStoppedEvent event) { SESSIONS.clear(); }
}
