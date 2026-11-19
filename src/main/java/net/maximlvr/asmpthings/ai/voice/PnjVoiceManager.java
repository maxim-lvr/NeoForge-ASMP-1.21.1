package net.maximlvr.asmpthings.ai.voice;

import com.google.gson.JsonObject;
import net.maximlvr.asmpthings.AsmpThingsMod;
import net.maximlvr.asmpthings.ai.AiNpcKnowledge;
import net.maximlvr.asmpthings.network.payload.*;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.net.http.WebSocketHandshakeException;
import java.util.*;
import java.util.concurrent.CompletionException;

/** Owns sessions and persistent knowledge exclusively on the Minecraft server thread. */
@EventBusSubscriber(modid = AsmpThingsMod.MOD_ID)
public final class PnjVoiceManager {
    private static final Map<UUID, Session> SESSIONS = new HashMap<>();
    private static final Map<UUID, Long> LAST_START = new HashMap<>();
    private static final double SELECT_DISTANCE = 12;
    private static final double STOP_DISTANCE = 16;

    @SubscribeEvent
    public static void commands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("pnj")
                .executes(context -> {
                    context.getSource().sendSuccess(() -> Component.literal("/pnj on : parler au PNJ renommé devant toi. /pnj off : couper le micro."), false);
                    return 1;
                })
                .then(Commands.literal("on").requires(source -> !PnjVoiceConfig.OPERATOR_ONLY.get() || source.hasPermission(2))
                        .executes(context -> start(context.getSource())))
                .then(Commands.literal("off").executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    stop(player, "Micro coupé.");
                    return 1;
                })));
    }

    private static int start(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        if (SESSIONS.containsKey(player.getUUID())) return reject(source, "Une conversation est déjà active. /pnj off pour l'arrêter.");
        if (!PnjVoiceConfig.ENABLED.get()) return reject(source, "Le POC vocal est désactivé dans asmpthingsmod-voice.toml.");
        if (!player.isAlive() || player.isSpectator()) return reject(source, "Rejoins le jeu pour parler à un PNJ.");
        String key = PnjVoiceConfig.apiKey();
        if (key.isBlank()) return reject(source, "Clé OpenAI absente : configure OPENAI_API_KEY ou realtime.api_key dans config/asmpthingsmod-voice.toml côté serveur.");
        if (SESSIONS.size() >= PnjVoiceConfig.MAX_SESSIONS.get()) return reject(source, "Toutes les conversations vocales sont occupées.");
        long now = System.nanoTime();
        if (now - LAST_START.getOrDefault(player.getUUID(), 0L) < 2_000_000_000L)
            return reject(source, "Attends deux secondes avant de reconnecter le PNJ.");
        LivingEntity npc = selectNpc(player);
        if (npc == null) return reject(source, "Regarde un PNJ renommé à moins de 12 blocs, ou approche-toi à moins de 6 blocs.");
        if (SESSIONS.values().stream().anyMatch(session -> session.npc.getUUID().equals(npc.getUUID())))
            return reject(source, "Ce PNJ discute déjà avec un autre joueur.");
        String name = npc.getCustomName().getString().trim();
        if (name.isBlank() || name.length() > 128) return reject(source, "Le nom du PNJ doit contenir entre 1 et 128 caractères.");
        AiNpcKnowledge knowledge = AiNpcKnowledge.get();
        if (!knowledge.personalities.ensureVoiceNpc(name)) return reject(source, "Ce PNJ est désactivé dans le fichier de personnalités.");
        Session session = new Session(player, npc, name, knowledge.voice.instructions(name, player.getUUID(),
                player.getName().getString(), player.level().dimension().location().toString()));
        SESSIONS.put(player.getUUID(), session);
        LAST_START.put(player.getUUID(), now);
        source.sendSuccess(() -> Component.literal("[PNJ] Connexion à " + name + "… Le micro s'ouvrira quand la session sera prête."), false);
        try {
            session.connection.connect(key, PnjVoiceConfig.MODEL.get());
        } catch (Exception error) {
            session.fail(error);
        }
        return 1;
    }

    public static LivingEntity selectNpc(ServerPlayer player) {
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getLookAngle().scale(SELECT_DISTANCE));
        List<LivingEntity> candidates = player.level().getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(SELECT_DISTANCE),
                entity -> !(entity instanceof Player) && entity.isAlive() && entity.hasCustomName()
                        && player.distanceToSqr(entity) <= SELECT_DISTANCE * SELECT_DISTANCE && player.hasLineOfSight(entity));
        LivingEntity selected = null;
        double closestHit = Double.MAX_VALUE;
        for (LivingEntity candidate : candidates) {
            Optional<Vec3> hit = candidate.getBoundingBox().inflate(0.2).clip(eye, end);
            if (hit.isPresent() && eye.distanceToSqr(hit.get()) < closestHit) {
                closestHit = eye.distanceToSqr(hit.get());
                selected = candidate;
            }
        }
        if (selected != null) return selected;
        return candidates.stream().filter(npc -> player.distanceToSqr(npc) <= 36)
                .min(Comparator.comparingDouble(player::distanceToSqr)).orElse(null);
    }

    public static void microphone(ServerPlayer player, PnjMicrophonePayload payload) {
        Session session = current(player, payload.sessionId());
        if (session == null || !session.ready) return;
        byte[] audio = payload.audio();
        if (audio.length == 0 || audio.length > PnjMicrophonePayload.MAX_BYTES || (audio.length & 1) != 0) return;
        long now = System.nanoTime();
        session.micCredits = Math.min(96000, session.micCredits + (now - session.lastMic) / 1_000_000_000.0 * 48000);
        session.lastMic = now;
        if (audio.length > session.micCredits) { stop(player, "Débit micro excessif. Conversation arrêtée."); return; }
        session.micCredits -= audio.length;
        session.connection.send(RealtimeProtocol.appendAudio(audio));
    }

    public static void clientEvent(ServerPlayer player, PnjVoiceClientEventPayload payload) {
        Session session = current(player, payload.sessionId());
        if (session == null) return;
        if (payload.action() == PnjVoiceClientEventPayload.STOP) {
            stop(player, "Micro coupé côté client.");
        } else if (payload.action() == PnjVoiceClientEventPayload.PLAYED && session.pendingTruncation != null) {
            Truncation pending = session.pendingTruncation;
            if (!pending.itemId.equals(payload.itemId()) || pending.contentIndex != payload.contentIndex()) return;
            session.pendingTruncation = null;
            session.connection.send(RealtimeProtocol.truncate(pending.itemId, pending.contentIndex,
                    Math.clamp(payload.playedMs(), 0, pending.maxMs)));
        }
    }

    private static Session current(ServerPlayer player, UUID id) {
        Session session = SESSIONS.get(player.getUUID());
        return session != null && session.id.equals(id) ? session : null;
    }

    public static void stop(ServerPlayer player, String message) {
        Session session = SESSIONS.remove(player.getUUID());
        if (session != null) {
            session.connection.close();
            if (!player.hasDisconnected()) session.control(PnjVoiceControlPayload.STOP, "", 0, message);
        } else if (!player.hasDisconnected()) {
            player.sendSystemMessage(Component.literal("[PNJ] Aucune conversation vocale active. Micro coupé."));
        }
    }

    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            Session session = SESSIONS.remove(player.getUUID());
            if (session != null) session.connection.close();
            LAST_START.remove(player.getUUID());
        }
    }

    @SubscribeEvent public static void stopped(ServerStoppedEvent event) {
        SESSIONS.values().forEach(session -> session.connection.close());
        SESSIONS.clear();
        LAST_START.clear();
        AiNpcKnowledge.unload();
    }

    @SubscribeEvent public static void tick(ServerTickEvent.Post event) {
        long now = System.nanoTime();
        for (Session session : List.copyOf(SESSIONS.values())) {
            ServerPlayer player = session.player;
            LivingEntity npc = session.npc;
            if (!player.isAlive() || player.hasDisconnected() || player.isSpectator() || !npc.isAlive()
                    || npc.isRemoved() || player.level() != npc.level() || player.distanceToSqr(npc) > STOP_DISTANCE * STOP_DISTANCE
                    || npc.getCustomName() == null || !session.name.equals(npc.getCustomName().getString().trim())) {
                stop(player, "Conversation terminée : PNJ hors de portée ou indisponible.");
            } else if (now - session.started > PnjVoiceConfig.MAX_SESSION_SECONDS.get() * 1_000_000_000L) {
                stop(player, "Durée maximale atteinte. /pnj on pour reprendre.");
            } else if (!session.ready && now - session.started > 20_000_000_000L) {
                stop(player, "OpenAI ne répond pas. Vérifie la connexion, la clé et le modèle configuré.");
            } else if (session.ready && now - session.lastMic > 10_000_000_000L) {
                stop(player, "Aucun flux du micro depuis 10 secondes. Vérifie le périphérique puis relance /pnj on.");
            } else if (npc instanceof Mob mob && event.getServer().getTickCount() % 5 == 0) {
                mob.getLookControl().setLookAt(player, 30, 30);
            }
        }
    }

    private static int reject(CommandSourceStack source, String message) {
        source.sendFailure(Component.literal(message));
        return 0;
    }

    private record Truncation(String itemId, int contentIndex, int maxMs) {}

    private static final class Session {
        final UUID id = UUID.randomUUID();
        final ServerPlayer player;
        final LivingEntity npc;
        final String name;
        final MinecraftServer server;
        final RealtimeConnection connection;
        final long started = System.nanoTime();
        final Set<String> processed = new LinkedHashSet<>();
        boolean ready;
        boolean userSpeaking;
        boolean memoryInFlight;
        boolean memoryDirty;
        long lastMic = started;
        double micCredits = 96000;
        String currentResponse = "";
        String interruptedResponse = "";
        String lastAudioItem = "";
        int lastContentIndex;
        int generatedBytes;
        Truncation pendingTruncation;

        Session(ServerPlayer player, LivingEntity npc, String name, String prompt) {
            this.player = player;
            this.npc = npc;
            this.name = name;
            this.server = player.server;
            this.connection = new RealtimeConnection(RealtimeProtocol.configure(prompt,
                    PnjVoiceConfig.VOICE.get(), PnjVoiceConfig.SILENCE_MS.get()),
                    event -> server.execute(() -> {
                        if (current(player, id) == this) {
                            try { handle(event); } catch (Exception error) { fail(error); }
                        }
                    }), error -> server.execute(() -> { if (current(player, id) == this) fail(error); }));
        }

        void control(int action, String itemId, int contentIndex, String message) {
            PacketDistributor.sendToPlayer(player, new PnjVoiceControlPayload(id, action, name, itemId, contentIndex, message));
        }

        void handle(JsonObject event) {
            switch (text(event, "type")) {
                case "session.updated" -> {
                    if (!ready) {
                        ready = true;
                        lastMic = System.nanoTime();
                        control(PnjVoiceControlPayload.START, "", 0, "Parle à " + name + ". /pnj off pour couper le micro.");
                    }
                }
                case "input_audio_buffer.speech_started" -> {
                    userSpeaking = true;
                    interruptedResponse = currentResponse;
                    if (!lastAudioItem.isEmpty()) {
                        pendingTruncation = new Truncation(lastAudioItem, lastContentIndex, generatedBytes / RealtimeProtocol.BYTES_PER_MS);
                        control(PnjVoiceControlPayload.INTERRUPT, lastAudioItem, lastContentIndex, "");
                        lastAudioItem = "";
                    }
                }
                case "input_audio_buffer.speech_stopped" -> userSpeaking = false;
                case "response.created" -> {
                    JsonObject response = event.getAsJsonObject("response");
                    if (!RealtimeProtocol.isMemoryResponse(response)) currentResponse = text(response, "id");
                }
                case "response.output_audio.delta" -> outputAudio(event);
                case "conversation.item.input_audio_transcription.completed" -> {
                    if (rememberEvent("input:" + text(event, "item_id"))) {
                        String transcript = clip(text(event, "transcript"), 4000).trim();
                        if (!transcript.isEmpty()) {
                            AiNpcKnowledge.get().players.recordPlayerMessage(name, player.getUUID(), player.getName().getString(), transcript);
                            player.sendSystemMessage(Component.literal("§7[Micro → " + name + "] " + transcript));
                        }
                    }
                }
                case "response.output_audio_transcript.done" -> {
                    if (!text(event, "response_id").equals(interruptedResponse))
                        player.sendSystemMessage(Component.literal("§e<" + name + "> §f" + clip(text(event, "transcript"), 4000)));
                }
                case "response.done" -> responseDone(event.getAsJsonObject("response"));
                case "error" -> {
                    JsonObject error = event.getAsJsonObject("error");
                    String code = clip(text(error, "code"), 100);
                    AsmpThingsMod.LOGGER.warn("[PNJ vocal] Erreur API Realtime : {}", code);
                    stop(player, "OpenAI Realtime : " + (code.isEmpty() ? "configuration ou accès refusé" : code)
                            + ". Vérifie la clé, les crédits API et le modèle.");
                }
                default -> { /* Realtime also emits lifecycle and partial transcription events. */ }
            }
        }

        void outputAudio(JsonObject event) {
            if (text(event, "response_id").equals(interruptedResponse)) return;
            byte[] audio = Base64.getDecoder().decode(text(event, "delta"));
            if (audio.length == 0 || (audio.length & 1) != 0 || audio.length > 512000)
                throw new IllegalArgumentException("Audio Realtime invalide");
            String item = text(event, "item_id");
            int contentIndex = event.get("content_index").getAsInt();
            if (!item.equals(lastAudioItem)) {
                lastAudioItem = item;
                generatedBytes = 0;
            }
            lastContentIndex = contentIndex;
            generatedBytes += audio.length;
            for (int offset = 0; offset < audio.length; offset += PnjSpeakerPayload.MAX_BYTES) {
                byte[] chunk = Arrays.copyOfRange(audio, offset, Math.min(audio.length, offset + PnjSpeakerPayload.MAX_BYTES));
                PacketDistributor.sendToPlayer(player, new PnjSpeakerPayload(id, item, contentIndex, chunk));
            }
        }

        void responseDone(JsonObject response) {
            String responseId = text(response, "id");
            if (!rememberEvent("response:" + responseId)) return;
            if (RealtimeProtocol.isMemoryResponse(response)) {
                memoryInFlight = false;
                boolean saved = AiNpcKnowledge.get().voice.applyResponse(name, player.getUUID(), player.getName().getString(), response);
                if (!saved) player.sendSystemMessage(Component.literal("§7[PNJ] La mise à jour des souvenirs a échoué pour ce tour."));
                else {
                    JsonObject updated = new JsonObject();
                    updated.addProperty("type", "realtime");
                    updated.addProperty("instructions", AiNpcKnowledge.get().voice.instructions(name, player.getUUID(),
                            player.getName().getString(), player.level().dimension().location().toString()));
                    JsonObject event = RealtimeProtocol.event("session.update");
                    event.add("session", updated);
                    connection.send(event);
                }
                if (memoryDirty) requestMemory();
                return;
            }
            String status = text(response, "status");
            if (status.equals("failed")) { stop(player, "La réponse OpenAI a échoué. Vérifie les crédits et l'accès au modèle."); return; }
            if (status.equals("completed") || status.equals("cancelled")) requestMemory();
        }

        void requestMemory() {
            if (memoryInFlight) { memoryDirty = true; return; }
            memoryInFlight = true;
            memoryDirty = false;
            connection.send(RealtimeProtocol.memoryUpdate(AiNpcKnowledge.get().voice.instructions(name,
                    player.getUUID(), player.getName().getString(), player.level().dimension().location().toString())));
        }

        boolean rememberEvent(String key) {
            if (!processed.add(key)) return false;
            if (processed.size() > 512) processed.remove(processed.iterator().next());
            return true;
        }

        void fail(Throwable error) {
            while (error instanceof CompletionException && error.getCause() != null) error = error.getCause();
            String reason = "Connexion Realtime interrompue. Vérifie le réseau et la configuration OpenAI.";
            if (error instanceof WebSocketHandshakeException handshake) {
                int status = handshake.getResponse().statusCode();
                reason = "OpenAI a refusé la connexion (HTTP " + status + "). Vérifie la clé, les crédits et l'accès au modèle.";
            }
            // Do not log exception messages: authentication errors can contain credential fragments.
            AsmpThingsMod.LOGGER.warn("[PNJ vocal] Échec de connexion : {}", error.getClass().getSimpleName());
            stop(player, reason);
        }
    }

    private static String text(JsonObject object, String key) {
        return object != null && object.has(key) && !object.get(key).isJsonNull() ? object.get(key).getAsString() : "";
    }

    private static String clip(String text, int length) {
        return text.length() <= length ? text : text.substring(0, length);
    }

    private PnjVoiceManager() {}
}
