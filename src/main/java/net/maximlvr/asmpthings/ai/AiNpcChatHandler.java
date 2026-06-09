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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class AiNpcChatHandler {

    private static final Gson GSON = new Gson();

    private static final double TALK_DISTANCE = 8.0D;
    private static final int BUBBLE_DURATION_TICKS = 100;
    private static final int BUBBLE_MAX_LENGTH = 80;

    private static final Set<String> AI_NPC_NAMES = Set.of(
            "test",
            "test2GPT",
            "test3GPT"
    );

    private final Map<UUID, AiNpcMemory> memories = new HashMap<>();
    private final Map<UUID, NameBubbleState> activeNameBubbles = new HashMap<>();
    private final AiNpcPlayerMemoryStore playerMemoryStore = new AiNpcPlayerMemoryStore();

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
            player.sendSystemMessage(Component.literal("§cUtilisation : @test ton message"));
            return;
        }

        String targetName = parts[0].substring(1);
        String playerMessage = parts[1].trim();

        if (!AI_NPC_NAMES.contains(targetName)) {
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

        UUID playerUuid = player.getUUID();
        UUID npcUuid = targetNpc.getUUID();
        ResourceKey<Level> npcDimension = targetNpc.level().dimension();

        AiNpcMemory memory = memories.computeIfAbsent(npcUuid, uuid -> new AiNpcMemory());

        String profileBeforeMessage = playerMemoryStore.exportCompactProfileJsonForAi(
                targetName,
                playerUuid,
                player.getName().getString()
        );

        List<String> recentConversation = memory.getRecentMessages();

        memory.remember(player.getName().getString() + " a dit : " + playerMessage);

        int relationDelta = playerMemoryStore.updateRelationFromPlayerMessage(
                targetName,
                playerUuid,
                player.getName().getString(),
                playerMessage
        );

        player.sendSystemMessage(Component.literal("§7Tu dis à §e" + targetName + "§7 : " + playerMessage));
        player.sendSystemMessage(Component.literal("§8Relation avec " + targetName + " : " + formatRelationDelta(relationDelta)));
        player.sendSystemMessage(Component.literal("§8<" + targetName + "> réfléchit..."));

        AsmpThingsMod.LOGGER.info("[AI NPC] {} parle à {} : {}", player.getName().getString(), targetName, playerMessage);
        AsmpThingsMod.LOGGER.info("[AI NPC] Relation {} -> {} delta {}", targetName, player.getName().getString(), relationDelta);

        if (!Config.DEEPSEEK_ENABLED.get()) {
            String errorMessage = "DeepSeek est désactivé dans la config serveur.";
            player.sendSystemMessage(Component.literal("§c<" + targetName + "> " + errorMessage));
            showTextAboveNpc(server, targetNpc, targetName, errorMessage);
            return;
        }

        String systemPrompt = buildSystemPrompt(targetName);
        String userPrompt = buildTurnPrompt(
                player,
                targetName,
                playerMessage,
                profileBeforeMessage,
                recentConversation
        );

        DeepSeekClient.askNpcTurnAsync(targetName, systemPrompt, userPrompt).thenAccept(aiResponse -> {
            server.execute(() -> {
                ServerPlayer currentPlayer = server.getPlayerList().getPlayer(playerUuid);

                if (currentPlayer == null) {
                    return;
                }

                LivingEntity currentNpc = getLivingEntityByUuid(server, npcDimension, npcUuid);

                if (currentNpc == null || !currentNpc.isAlive()) {
                    currentPlayer.sendSystemMessage(Component.literal("§c<" + targetName + "> Le PNJ n'existe plus."));
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
                    AiNpcPlayerMemoryStore.ProfileUpdateResult profileUpdateResult =
                            playerMemoryStore.applyAiProfileUpdate(
                                    targetName,
                                    playerUuid,
                                    currentPlayer.getName().getString(),
                                    result.rawJson()
                            );

                    if (profileUpdateResult.updated()) {
                        String learned = profileUpdateResult.learnedInfo();

                        if (learned != null && !learned.isBlank()) {
                            currentPlayer.sendSystemMessage(Component.literal("§8" + targetName + " retient : " + learned));
                            AsmpThingsMod.LOGGER.info("[AI NPC] {} met à jour la fiche de {} : {}", targetName, currentPlayer.getName().getString(), learned);
                        } else {
                            currentPlayer.sendSystemMessage(Component.literal("§8" + targetName + " met à jour sa fiche joueur."));
                            AsmpThingsMod.LOGGER.info("[AI NPC] {} met à jour la fiche de {}", targetName, currentPlayer.getName().getString());
                        }
                    }
                }

                AsmpThingsMod.LOGGER.info("[AI NPC] {} répond via DeepSeek optimisé : {}", targetName, finalResponse);

                currentPlayer.sendSystemMessage(Component.literal("§e<" + targetName + "> §f" + finalResponse));
                showTextAboveNpc(server, currentNpc, targetName, bubbleResponse);
            });
        });
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

    private String buildSystemPrompt(String npcName) {
        String personality = switch (npcName) {
            case "test1GPT" -> """
                    Tu es test1GPT.
                    Tu es un PNJ Minecraft prudent, calme et observateur.
                    """;

            case "test2GPT" -> """
                    Tu es test2GPT.
                    Tu es un PNJ Minecraft curieux, énergique et social.
                    """;

            case "test3GPT" -> """
                    Tu es test3GPT.
                    Tu es un PNJ Minecraft sérieux, organisé et stratégique.
                    """;

            default -> """
                    Tu es un PNJ Minecraft.
                    """;
        };

        return personality + """
                
                Tu dois répondre au joueur et mettre à jour sa fiche mémoire dans un seul JSON.

                Règles de réponse :
                - Réponds uniquement en français.
                - Réponds comme un personnage présent dans Minecraft.
                - Ne dis jamais que tu es une IA.
                - Ne mentionne jamais DeepSeek, API, modèle, prompt ou système.
                - Utilise l'ancienne fiche joueur pour te souvenir de ses informations.
                - Ne dis pas "tu me l'as déjà dit" pour une information qui apparaît seulement dans le message actuel.
                - Si le joueur donne une nouvelle information, réagis comme si tu venais de l'apprendre.
                - Ne récite pas toute la fiche joueur.
                - Adapte ton ton selon la relation.
                
                Règles de mémoire :
                - Mets shouldUpdate à true uniquement si le message actuel contient une information utile, durable ou récurrente.
                - Tu peux enregistrer les goûts, amis, ennemis, objectifs, projets, habitudes, sujets récurrents, détails personnels.
                - Ne sauvegarde pas les salutations ou les demandes temporaires.
                - Garde les anciennes informations utiles.
                - Évite les doublons.
                
                Réponds uniquement avec un JSON valide.
                Pas de markdown.
                Pas de ```json.
                """;
    }

    private String buildTurnPrompt(
            ServerPlayer player,
            String npcName,
            String playerMessage,
            String profileBeforeMessage,
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
                
                Ancienne fiche joueur AVANT le message actuel :
                %s
                
                Conversation récente AVANT le message actuel :
                %s
                
                Message actuel du joueur :
                %s
                
                Retourne exactement ce format :
                
                {
                  "reply": "réponse naturelle du PNJ au joueur",
                  "shouldUpdate": true,
                  "playerSummary": "résumé compact du joueur, mis à jour si nécessaire",
                  "knownFacts": [
                    "fait connu utile"
                  ],
                  "recurringTopics": [
                    "sujet récurrent"
                  ],
                  "importantNotes": [
                    "note importante"
                  ],
                  "lastLearnedInfo": "nouvelle info apprise maintenant, ou vide si rien"
                }
                
                Si aucune mise à jour mémoire n'est nécessaire :
                - mets shouldUpdate à false
                - conserve playerSummary, knownFacts, recurringTopics et importantNotes depuis l'ancienne fiche
                - mets lastLearnedInfo à ""
                """.formatted(
                player.getName().getString(),
                npcName,
                player.level().dimension().location(),
                (int) player.getX(),
                (int) player.getY(),
                (int) player.getZ(),
                profileBeforeMessage,
                recentBuilder.toString(),
                playerMessage
        );
    }

    private NpcTurnResult parseNpcTurnResult(String aiResponse) {
        String json = extractJsonObject(aiResponse);

        if (json == null || json.isBlank()) {
            return new NpcTurnResult(aiResponse, "");
        }

        try {
            JsonObject object = GSON.fromJson(json, JsonObject.class);

            if (object == null) {
                return new NpcTurnResult(aiResponse, "");
            }

            String reply = readString(object, "reply", "");

            return new NpcTurnResult(reply, json);

        } catch (Exception e) {
            AsmpThingsMod.LOGGER.error("[AI NPC] Impossible de parser la réponse JSON du PNJ : {}", aiResponse, e);
            return new NpcTurnResult(aiResponse, "");
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

    private String formatRelationDelta(int delta) {
        if (delta > 0) {
            return "§a+" + delta;
        }

        if (delta < 0) {
            return "§c" + delta;
        }

        return "§70";
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