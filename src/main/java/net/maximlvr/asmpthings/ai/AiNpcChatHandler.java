package net.maximlvr.asmpthings.ai;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.maximlvr.asmpthings.AsmpThingsMod;
import net.maximlvr.asmpthings.Config;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.ServerChatEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class AiNpcChatHandler {

    private static final Gson GSON = new Gson();

    private static final double TALK_DISTANCE = 50.0D;
    private static final double LOCAL_CHAT_DISTANCE = 50.0D;
    private static final double LOCAL_CHAT_DISTANCE_SQR = LOCAL_CHAT_DISTANCE * LOCAL_CHAT_DISTANCE;

    private static final int BUBBLE_DURATION_TICKS = 100;
    private static final int BUBBLE_MAX_LENGTH = 80;

    private final Map<UUID, AiNpcMemory> memories = new HashMap<>();
    private final Map<UUID, NameBubbleState> activeNameBubbles = new HashMap<>();
    private final Set<UUID> playersWaitingForNpcResponse = new HashSet<>();

    private final AiNpcPlayerMemoryStore playerMemoryStore = new AiNpcPlayerMemoryStore();
    private final AiNpcPersonalityStore personalityStore = new AiNpcPersonalityStore();
    private final AiNpcGeneralMemoryStore generalMemoryStore = new AiNpcGeneralMemoryStore();

    @SubscribeEvent
    public void onServerChat(ServerChatEvent event) {
        ServerPlayer player = event.getPlayer();
        MinecraftServer server = player.server;

        String rawMessage = event.getRawText();

        if (!rawMessage.startsWith("@")) {
            return;
        }

        String[] parts = rawMessage.split(" ", 2);

        if (parts.length < 2) {
            event.setCanceled(true);
            player.sendSystemMessage(Component.literal("§cUtilisation : @test ton message"));
            return;
        }

        String targetName = parts[0].substring(1);
        String playerMessage = parts[1].trim();

        if (!personalityStore.isAiNpcName(targetName)) {
            return;
        }

        event.setCanceled(true);

        UUID playerUuid = player.getUUID();

        if (playersWaitingForNpcResponse.contains(playerUuid)) {
            player.sendSystemMessage(Component.literal("§cMessage déjà en cours. Veuillez renvoyer après la réponse du PNJ."));
            return;
        }

        Optional<LivingEntity> npc = findNearbyNamedNpc(player, targetName);

        if (npc.isEmpty()) {
            player.sendSystemMessage(Component.literal("§cAucun PNJ nommé " + targetName + " à moins de " + TALK_DISTANCE + " blocs."));
            return;
        }

        LivingEntity targetNpc = npc.get();

        if (targetNpc instanceof Mob mob) {
            mob.getLookControl().setLookAt(player, 30.0F, 30.0F);
        }

        playersWaitingForNpcResponse.add(playerUuid);

        UUID npcUuid = targetNpc.getUUID();
        ResourceKey<Level> npcDimension = targetNpc.level().dimension();

        AiNpcMemory memory = memories.computeIfAbsent(npcUuid, uuid -> new AiNpcMemory());

        String profileBeforeMessage = playerMemoryStore.exportCompactProfileJsonForAi(
                targetName,
                playerUuid,
                player.getName().getString()
        );

        String generalMemoryBeforeMessage = generalMemoryStore.exportCompactGeneralMemoryJsonForAi(targetName);

        String staticKnowledge = personalityStore.exportStaticKnowledgeJsonForAi(targetName);

        List<String> recentConversation = memory.getRecentMessages();

        memory.remember(player.getName().getString() + " a dit : " + playerMessage);

        playerMemoryStore.recordPlayerMessage(
                targetName,
                playerUuid,
                player.getName().getString(),
                playerMessage
        );

        sendLocalMessageAroundPlayer(
                server,
                player,
                Component.literal("§7" + player.getName().getString() + " dit à §e" + targetName + "§7 : " + playerMessage)
        );

        player.sendSystemMessage(Component.literal("§8<" + targetName + "> réfléchit..."));

        AsmpThingsMod.LOGGER.info("[AI NPC] {} parle à {} : {}", player.getName().getString(), targetName, playerMessage);

        if (!Config.DEEPSEEK_ENABLED.get()) {
            playersWaitingForNpcResponse.remove(playerUuid);

            String errorMessage = "DeepSeek est désactivé dans la config serveur.";
            sendLocalMessageAroundEntity(
                    server,
                    targetNpc,
                    Component.literal("§c<" + targetName + "> " + errorMessage)
            );
            showTextAboveNpc(server, targetNpc, targetName, errorMessage);
            return;
        }

        String systemPrompt = personalityStore.buildSystemPrompt(targetName);

        String userPrompt = buildTurnPrompt(
                player,
                targetName,
                playerMessage,
                staticKnowledge,
                profileBeforeMessage,
                generalMemoryBeforeMessage,
                recentConversation
        );

        DeepSeekClient.askNpcTurnAsync(targetName, systemPrompt, userPrompt).whenComplete((aiResponse, throwable) -> {
            server.execute(() -> {
                try {
                    ServerPlayer currentPlayer = server.getPlayerList().getPlayer(playerUuid);

                    if (currentPlayer == null) {
                        return;
                    }

                    LivingEntity currentNpc = getLivingEntityByUuid(server, npcDimension, npcUuid);

                    if (currentNpc == null || !currentNpc.isAlive()) {
                        currentPlayer.sendSystemMessage(Component.literal("§c<" + targetName + "> Le PNJ n'existe plus."));
                        return;
                    }

                    if (throwable != null) {
                        AsmpThingsMod.LOGGER.error("[AI NPC] Erreur async pendant la réponse de {}", targetName, throwable);

                        String errorMessage = "Je me suis perdu dans mes pensées. Réessaie.";
                        sendLocalMessageAroundEntity(
                                server,
                                currentNpc,
                                Component.literal("§c<" + targetName + "> " + errorMessage)
                        );
                        showTextAboveNpc(server, currentNpc, targetName, errorMessage);
                        return;
                    }

                    NpcTurnResult result = parseNpcTurnResult(aiResponse);

                    String finalResponse = result.reply();

                    if (finalResponse == null || finalResponse.isBlank()) {
                        finalResponse = "Je n'arrive pas à formuler ma réponse.";
                    }

                    finalResponse = cleanResponseForChat(finalResponse);
                    String bubbleResponse = cleanResponseForBubble(finalResponse);

                    AiNpcMemory currentMemory = memories.computeIfAbsent(npcUuid, uuid -> new AiNpcMemory());
                    currentMemory.remember(targetName + " a répondu : " + finalResponse);

                    if (result.rawJson() != null && !result.rawJson().isBlank()) {
                        applyMemoryAndRelationUpdates(
                                currentPlayer,
                                targetName,
                                playerUuid,
                                result.rawJson()
                        );
                    }

                    AsmpThingsMod.LOGGER.info("[AI NPC] {} répond via DeepSeek optimisé : {}", targetName, finalResponse);

                    sendLocalMessageAroundEntity(
                            server,
                            currentNpc,
                            Component.literal("§e<" + targetName + "> §f" + finalResponse)
                    );

                    showTextAboveNpc(server, currentNpc, targetName, bubbleResponse);

                } finally {
                    playersWaitingForNpcResponse.remove(playerUuid);
                }
            });
        });
    }

    private void applyMemoryAndRelationUpdates(
            ServerPlayer player,
            String targetName,
            UUID playerUuid,
            String rawJson
    ) {
        AiNpcPlayerMemoryStore.ProfileUpdateResult profileUpdateResult =
                playerMemoryStore.applyAiProfileUpdate(
                        targetName,
                        playerUuid,
                        player.getName().getString(),
                        rawJson
                );

        if (profileUpdateResult.updated()) {
            String learned = profileUpdateResult.learnedInfo();

            if (learned != null && !learned.isBlank()) {
                AsmpThingsMod.LOGGER.info("[AI NPC] {} met à jour la fiche de {} : {}", targetName, player.getName().getString(), learned);
            }
        }

        AiNpcGeneralMemoryStore.GeneralUpdateResult generalUpdateResult =
                generalMemoryStore.applyAiGeneralMemoryUpdate(targetName, rawJson);

        if (generalUpdateResult.updated()) {
            String learned = generalUpdateResult.learnedInfo();

            if (learned != null && !learned.isBlank()) {
                AsmpThingsMod.LOGGER.info("[AI NPC] {} met à jour la mémoire générale : {}", targetName, learned);
            }
        }

        String relationIntent = readNestedString(rawJson, "playerMemory", "relationIntent", "same");

        AiNpcPlayerMemoryStore.RelationUpdateResult relationUpdateResult =
                playerMemoryStore.applyRelationIntent(
                        targetName,
                        playerUuid,
                        player.getName().getString(),
                        relationIntent
                );

        if (relationUpdateResult.changed()) {
            AsmpThingsMod.LOGGER.info(
                    "[AI NPC] Relation {} -> {} : {} -> {} via {}",
                    targetName,
                    player.getName().getString(),
                    relationUpdateResult.beforeState(),
                    relationUpdateResult.afterState(),
                    relationUpdateResult.intent()
            );
        }
    }

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        int currentTick = server.getTickCount();

        activeNameBubbles.entrySet().removeIf(entry -> {
            NameBubbleState state = entry.getValue();

            if (currentTick < state.restoreAtTick()) {
                return false;
            }

            Entity entity = server.getLevel(state.dimension()) != null
                    ? server.getLevel(state.dimension()).getEntity(entry.getKey())
                    : null;

            if (entity instanceof LivingEntity livingEntity && livingEntity.isAlive()) {
                livingEntity.setCustomName(state.originalName());
                livingEntity.setCustomNameVisible(state.originalNameVisible());
            }

            return true;
        });
    }

    private void sendLocalMessageAroundPlayer(MinecraftServer server, ServerPlayer sourcePlayer, Component message) {
        ResourceKey<Level> dimension = sourcePlayer.level().dimension();

        for (ServerPlayer targetPlayer : server.getPlayerList().getPlayers()) {
            if (targetPlayer.level().dimension() != dimension) {
                continue;
            }

            if (targetPlayer.distanceToSqr(sourcePlayer) <= LOCAL_CHAT_DISTANCE_SQR) {
                targetPlayer.sendSystemMessage(message);
            }
        }
    }

    private void sendLocalMessageAroundEntity(MinecraftServer server, Entity sourceEntity, Component message) {
        ResourceKey<Level> dimension = sourceEntity.level().dimension();

        for (ServerPlayer targetPlayer : server.getPlayerList().getPlayers()) {
            if (targetPlayer.level().dimension() != dimension) {
                continue;
            }

            if (targetPlayer.distanceToSqr(sourceEntity) <= LOCAL_CHAT_DISTANCE_SQR) {
                targetPlayer.sendSystemMessage(message);
            }
        }
    }

    private String buildTurnPrompt(
            ServerPlayer player,
            String npcName,
            String playerMessage,
            String staticKnowledge,
            String profileBeforeMessage,
            String generalMemoryBeforeMessage,
            List<String> recentConversation
    ) {
        StringBuilder recentBuilder = new StringBuilder();

        if (recentConversation == null || recentConversation.isEmpty()) {
            recentBuilder.append("Aucune conversation récente.");
        } else {
            for (String line : recentConversation) {
                recentBuilder.append("- ").append(line).append("\n");
            }
        }

        return """
                Contexte :
                - Joueur : %s
                - PNJ : %s
                - Dimension : %s
                - Position joueur : %d %d %d

                Personnalité persistante et connaissances fixes du PNJ :
                %s

                Ancienne fiche joueur AVANT le message actuel :
                %s

                Ancienne mémoire générale AVANT le message actuel :
                %s

                Conversation récente AVANT le message actuel :
                %s

                Message actuel du joueur :
                %s

                Retourne un JSON compact exactement comme ceci :

                {
                  "reply": "réponse courte du PNJ, maximum 2 phrases",
                  "playerMemory": {
                    "shouldUpdate": false,
                    "relationIntent": "same",
                    "playerSummary": "",
                    "knownFacts": [],
                    "recurringTopics": [],
                    "importantNotes": [],
                    "lastLearnedInfo": ""
                  },
                  "generalMemory": {
                    "shouldUpdate": false,
                    "worldSummary": "",
                    "knownFacts": [],
                    "aliases": [],
                    "recurringTopics": [],
                    "importantNotes": [],
                    "lastLearnedInfo": ""
                  }
                }

                Contraintes :
                - reply doit faire maximum 2 phrases.
                - chaque liste doit contenir maximum 5 éléments.
                - chaque élément de liste doit faire maximum 120 caractères.
                - relationIntent vaut seulement "increase", "decrease" ou "same".
                - ne crée pas de longues explications dans les champs mémoire.
                - si playerMemory.shouldUpdate vaut false, laisse playerSummary, knownFacts, recurringTopics, importantNotes et lastLearnedInfo vides.
                - si generalMemory.shouldUpdate vaut false, laisse worldSummary, knownFacts, aliases, recurringTopics, importantNotes et lastLearnedInfo vides.
                - ne recopie pas l'ancienne mémoire si elle n'a pas changé.
                """.formatted(
                player.getName().getString(),
                npcName,
                player.level().dimension().location(),
                (int) player.getX(),
                (int) player.getY(),
                (int) player.getZ(),
                staticKnowledge,
                profileBeforeMessage,
                generalMemoryBeforeMessage,
                recentBuilder.toString(),
                playerMessage
        );
    }

    private NpcTurnResult parseNpcTurnResult(String aiResponse) {
        String json = extractJsonObject(aiResponse);

        if (json == null || json.isBlank()) {
            String fallbackReply = extractReplyFallback(aiResponse);

            if (fallbackReply == null || fallbackReply.isBlank()) {
                fallbackReply = "Je me suis emmêlé dans mes pensées. Répète-moi ça plus simplement.";
            }

            return new NpcTurnResult(fallbackReply, "");
        }

        try {
            JsonObject object = GSON.fromJson(json, JsonObject.class);

            if (object == null) {
                String fallbackReply = extractReplyFallback(aiResponse);

                if (fallbackReply == null || fallbackReply.isBlank()) {
                    fallbackReply = "Je me suis emmêlé dans mes pensées. Répète-moi ça plus simplement.";
                }

                return new NpcTurnResult(fallbackReply, "");
            }

            String reply = readString(object, "reply", "");

            return new NpcTurnResult(reply, json);

        } catch (Exception e) {
            AsmpThingsMod.LOGGER.error("[AI NPC] Impossible de parser la réponse JSON du PNJ : {}", aiResponse, e);

            String fallbackReply = extractReplyFallback(aiResponse);

            if (fallbackReply == null || fallbackReply.isBlank()) {
                fallbackReply = "Je me suis emmêlé dans mes pensées. Répète-moi ça plus simplement.";
            }

            return new NpcTurnResult(fallbackReply, "");
        }
    }

    private String extractReplyFallback(String aiResponse) {
        if (aiResponse == null || aiResponse.isBlank()) {
            return "";
        }

        String marker = "\"reply\"";
        int markerIndex = aiResponse.indexOf(marker);

        if (markerIndex < 0) {
            return "";
        }

        int colonIndex = aiResponse.indexOf(':', markerIndex);

        if (colonIndex < 0) {
            return "";
        }

        int firstQuoteIndex = aiResponse.indexOf('"', colonIndex + 1);

        if (firstQuoteIndex < 0) {
            return "";
        }

        StringBuilder result = new StringBuilder();
        boolean escaping = false;

        for (int i = firstQuoteIndex + 1; i < aiResponse.length(); i++) {
            char c = aiResponse.charAt(i);

            if (escaping) {
                switch (c) {
                    case 'n' -> result.append(' ');
                    case 'r' -> result.append(' ');
                    case 't' -> result.append(' ');
                    case '"' -> result.append('"');
                    case '\\' -> result.append('\\');
                    default -> result.append(c);
                }

                escaping = false;
                continue;
            }

            if (c == '\\') {
                escaping = true;
                continue;
            }

            if (c == '"') {
                break;
            }

            result.append(c);
        }

        return result.toString().trim();
    }

    private String readNestedString(String rawJson, String objectKey, String stringKey, String fallback) {
        String json = extractJsonObject(rawJson);

        if (json == null || json.isBlank()) {
            return fallback;
        }

        try {
            JsonObject root = GSON.fromJson(json, JsonObject.class);

            if (root == null || !root.has(objectKey) || !root.get(objectKey).isJsonObject()) {
                return fallback;
            }

            JsonObject child = root.getAsJsonObject(objectKey);

            return readString(child, stringKey, fallback);

        } catch (Exception e) {
            return fallback;
        }
    }

    private String extractJsonObject(String value) {
        if (value == null) {
            return null;
        }

        String cleaned = value.trim();

        int start = cleaned.indexOf('{');
        int end = cleaned.lastIndexOf('}');

        if (start < 0 || end < 0 || end <= start) {
            return null;
        }

        return cleaned.substring(start, end + 1);
    }

    private String readString(JsonObject object, String key, String fallback) {
        JsonElement element = object.get(key);

        if (element == null || element.isJsonNull()) {
            return fallback;
        }

        try {
            return element.getAsString();
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private void showTextAboveNpc(MinecraftServer server, LivingEntity npc, String npcName, String response) {
        UUID uuid = npc.getUUID();

        activeNameBubbles.computeIfAbsent(uuid, ignored -> new NameBubbleState(
                npc.getCustomName(),
                npc.isCustomNameVisible(),
                server.getTickCount() + BUBBLE_DURATION_TICKS,
                npc.level().dimension()
        ));

        NameBubbleState oldState = activeNameBubbles.get(uuid);

        activeNameBubbles.put(uuid, new NameBubbleState(
                oldState.originalName(),
                oldState.originalNameVisible(),
                server.getTickCount() + BUBBLE_DURATION_TICKS,
                npc.level().dimension()
        ));

        npc.setCustomName(Component.literal(npcName + " : " + response));
        npc.setCustomNameVisible(true);
    }

    private String cleanResponseForChat(String response) {
        return response
                .replace("\n", " ")
                .replace("\r", " ")
                .trim();
    }

    private String cleanResponseForBubble(String response) {
        String cleaned = response
                .replace("\n", " ")
                .replace("\r", " ")
                .trim();

        if (cleaned.length() > BUBBLE_MAX_LENGTH) {
            cleaned = cleaned.substring(0, BUBBLE_MAX_LENGTH - 3) + "...";
        }

        return cleaned;
    }

    private Optional<LivingEntity> findNearbyNamedNpc(ServerPlayer player, String targetName) {
        List<Entity> entities = player.level().getEntities(
                player,
                player.getBoundingBox().inflate(TALK_DISTANCE),
                entity -> entity instanceof LivingEntity
                        && entity.hasCustomName()
                        && entity.getCustomName() != null
                        && isMatchingNpcName(entity.getCustomName().getString(), targetName)
        );

        return entities.stream()
                .filter(entity -> entity instanceof LivingEntity)
                .map(entity -> (LivingEntity) entity)
                .findFirst();
    }

    private LivingEntity getLivingEntityByUuid(MinecraftServer server, ResourceKey<Level> dimension, UUID uuid) {
        ServerLevel level = server.getLevel(dimension);

        if (level == null) {
            return null;
        }

        Entity entity = level.getEntity(uuid);

        if (entity instanceof LivingEntity livingEntity) {
            return livingEntity;
        }

        return null;
    }

    private boolean isMatchingNpcName(String currentName, String targetName) {
        return currentName.equals(targetName) || currentName.startsWith(targetName + " : ");
    }

    private record NpcTurnResult(
            String reply,
            String rawJson
    ) {
    }

    private record NameBubbleState(
            Component originalName,
            boolean originalNameVisible,
            int restoreAtTick,
            ResourceKey<Level> dimension
    ) {
    }
}