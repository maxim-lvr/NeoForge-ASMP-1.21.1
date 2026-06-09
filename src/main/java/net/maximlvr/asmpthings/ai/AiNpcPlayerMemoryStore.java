package net.maximlvr.asmpthings.ai;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import net.maximlvr.asmpthings.AsmpThingsMod;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class AiNpcPlayerMemoryStore {

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .create();

    private static final Type SAVE_TYPE = new TypeToken<Map<String, Map<String, PlayerMemory>>>() {
    }.getType();

    private static final int MAX_KNOWN_FACTS = 80;
    private static final int MAX_RECURRING_TOPICS = 30;
    private static final int MAX_IMPORTANT_NOTES = 50;

    private static final int PROMPT_MAX_KNOWN_FACTS = 14;
    private static final int PROMPT_MAX_RECURRING_TOPICS = 8;
    private static final int PROMPT_MAX_IMPORTANT_NOTES = 10;

    private final Path savePath;
    private final Map<String, Map<String, PlayerMemory>> memories = new HashMap<>();

    public AiNpcPlayerMemoryStore() {
        this.savePath = FMLPaths.CONFIGDIR.get().resolve("asmpthings_ai_player_memory.json");
        load();
    }

    public PlayerMemory getOrCreate(String npcName, UUID playerUuid, String playerName) {
        String playerKey = playerUuid.toString();

        Map<String, PlayerMemory> npcMemories = memories.computeIfAbsent(npcName, ignored -> new HashMap<>());

        PlayerMemory memory = npcMemories.computeIfAbsent(playerKey, ignored -> {
            PlayerMemory created = new PlayerMemory();
            created.playerUuid = playerKey;
            created.playerName = playerName;
            created.relationshipScore = 0;
            created.interactions = 0;
            created.firstSeen = Instant.now().toString();
            created.lastSeen = Instant.now().toString();
            created.lastMessage = "";
            created.lastMoodChange = 0;
            created.playerSummary = "";
            created.knownFacts = new ArrayList<>();
            created.recurringTopics = new ArrayList<>();
            created.importantNotes = new ArrayList<>();
            created.lastLearnedInfo = "";
            created.profileUpdateCount = 0;
            created.lastProfileUpdate = "";
            return created;
        });

        memory.playerName = playerName;
        memory.lastSeen = Instant.now().toString();

        ensureLists(memory);

        return memory;
    }

    public int updateRelationFromPlayerMessage(String npcName, UUID playerUuid, String playerName, String message) {
        PlayerMemory memory = getOrCreate(npcName, playerUuid, playerName);

        int delta = calculateMoodDelta(message);

        memory.relationshipScore = clamp(memory.relationshipScore + delta, -100, 100);
        memory.interactions++;
        memory.lastMessage = message;
        memory.lastMoodChange = delta;
        memory.lastSeen = Instant.now().toString();

        save();

        return delta;
    }

    public String exportCompactProfileJsonForAi(String npcName, UUID playerUuid, String playerName) {
        PlayerMemory memory = getOrCreate(npcName, playerUuid, playerName);

        JsonObject object = new JsonObject();

        object.addProperty("playerName", memory.playerName);
        object.addProperty("relationshipScore", memory.relationshipScore);
        object.addProperty("relationLabel", getRelationLabel(memory.relationshipScore));
        object.addProperty("interactions", memory.interactions);
        object.addProperty("playerSummary", safe(memory.playerSummary));
        object.add("knownFacts", toJsonArray(limitList(memory.knownFacts, PROMPT_MAX_KNOWN_FACTS)));
        object.add("recurringTopics", toJsonArray(limitList(memory.recurringTopics, PROMPT_MAX_RECURRING_TOPICS)));
        object.add("importantNotes", toJsonArray(limitList(memory.importantNotes, PROMPT_MAX_IMPORTANT_NOTES)));

        return GSON.toJson(object);
    }

    public ProfileUpdateResult applyAiProfileUpdate(
            String npcName,
            UUID playerUuid,
            String playerName,
            String aiJsonResponse
    ) {
        PlayerMemory memory = getOrCreate(npcName, playerUuid, playerName);

        String json = extractJsonObject(aiJsonResponse);

        if (json == null || json.isBlank()) {
            AsmpThingsMod.LOGGER.warn("[AI NPC] Mise à jour fiche joueur impossible, JSON absent : {}", aiJsonResponse);
            return new ProfileUpdateResult(false, "");
        }

        try {
            JsonObject object = GSON.fromJson(json, JsonObject.class);

            if (object == null) {
                return new ProfileUpdateResult(false, "");
            }

            boolean shouldUpdate = readBoolean(object, "shouldUpdate", false);

            if (!shouldUpdate) {
                memory.lastLearnedInfo = "";
                save();
                return new ProfileUpdateResult(false, "");
            }

            String newSummary = readString(object, "playerSummary", memory.playerSummary);
            List<String> newKnownFacts = readStringList(object, "knownFacts", memory.knownFacts, MAX_KNOWN_FACTS);
            List<String> newRecurringTopics = readStringList(object, "recurringTopics", memory.recurringTopics, MAX_RECURRING_TOPICS);
            List<String> newImportantNotes = readStringList(object, "importantNotes", memory.importantNotes, MAX_IMPORTANT_NOTES);
            String lastLearnedInfo = readString(object, "lastLearnedInfo", "");

            memory.playerSummary = clip(newSummary, 1200);
            memory.knownFacts = newKnownFacts;
            memory.recurringTopics = newRecurringTopics;
            memory.importantNotes = newImportantNotes;
            memory.lastLearnedInfo = clip(lastLearnedInfo, 300);
            memory.profileUpdateCount++;
            memory.lastProfileUpdate = Instant.now().toString();

            save();

            return new ProfileUpdateResult(true, memory.lastLearnedInfo);

        } catch (Exception e) {
            AsmpThingsMod.LOGGER.error("[AI NPC] Erreur parsing fiche joueur IA : {}", aiJsonResponse, e);
            return new ProfileUpdateResult(false, "");
        }
    }

    private List<String> limitList(List<String> source, int limit) {
        List<String> result = new ArrayList<>();

        if (source == null || source.isEmpty()) {
            return result;
        }

        int start = Math.max(0, source.size() - limit);

        for (int i = start; i < source.size(); i++) {
            String value = source.get(i);

            if (value != null && !value.isBlank()) {
                result.add(value);
            }
        }

        return result;
    }

    private void ensureLists(PlayerMemory memory) {
        if (memory.knownFacts == null) {
            memory.knownFacts = new ArrayList<>();
        }

        if (memory.recurringTopics == null) {
            memory.recurringTopics = new ArrayList<>();
        }

        if (memory.importantNotes == null) {
            memory.importantNotes = new ArrayList<>();
        }

        if (memory.playerSummary == null) {
            memory.playerSummary = "";
        }

        if (memory.lastLearnedInfo == null) {
            memory.lastLearnedInfo = "";
        }

        if (memory.lastProfileUpdate == null) {
            memory.lastProfileUpdate = "";
        }
    }

    private JsonArray toJsonArray(List<String> values) {
        JsonArray array = new JsonArray();

        if (values == null) {
            return array;
        }

        for (String value : values) {
            if (value != null && !value.isBlank()) {
                array.add(value);
            }
        }

        return array;
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

    private boolean readBoolean(JsonObject object, String key, boolean fallback) {
        JsonElement element = object.get(key);

        if (element == null || element.isJsonNull()) {
            return fallback;
        }

        try {
            return element.getAsBoolean();
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private String readString(JsonObject object, String key, String fallback) {
        JsonElement element = object.get(key);

        if (element == null || element.isJsonNull()) {
            return fallback == null ? "" : fallback;
        }

        try {
            return sanitize(element.getAsString());
        } catch (Exception ignored) {
            return fallback == null ? "" : fallback;
        }
    }

    private List<String> readStringList(JsonObject object, String key, List<String> fallback, int maxSize) {
        JsonElement element = object.get(key);

        if (element == null || !element.isJsonArray()) {
            return sanitizeAndLimitList(fallback, maxSize);
        }

        List<String> result = new ArrayList<>();

        for (JsonElement item : element.getAsJsonArray()) {
            if (item == null || item.isJsonNull()) {
                continue;
            }

            try {
                String value = sanitize(item.getAsString());

                if (!value.isBlank() && !containsNormalized(result, value)) {
                    result.add(clip(value, 250));
                }

            } catch (Exception ignored) {
            }

            if (result.size() >= maxSize) {
                break;
            }
        }

        return result;
    }

    private List<String> sanitizeAndLimitList(List<String> source, int maxSize) {
        List<String> result = new ArrayList<>();

        if (source == null) {
            return result;
        }

        for (String value : source) {
            String cleaned = sanitize(value);

            if (!cleaned.isBlank() && !containsNormalized(result, cleaned)) {
                result.add(clip(cleaned, 250));
            }

            if (result.size() >= maxSize) {
                break;
            }
        }

        return result;
    }

    private boolean containsNormalized(List<String> list, String value) {
        String normalized = normalize(value);

        for (String existing : list) {
            if (normalize(existing).equals(normalized)) {
                return true;
            }
        }

        return false;
    }

    private String sanitize(String value) {
        if (value == null) {
            return "";
        }

        return value
                .replace("\n", " ")
                .replace("\r", " ")
                .replace("\t", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String normalize(String value) {
        return sanitize(value).toLowerCase(Locale.ROOT);
    }

    private String clip(String value, int maxLength) {
        String cleaned = sanitize(value);

        if (cleaned.length() <= maxLength) {
            return cleaned;
        }

        return cleaned.substring(0, maxLength);
    }

    private String getRelationLabel(int score) {
        if (score >= 60) {
            return "très amicale";
        }

        if (score >= 25) {
            return "amicale";
        }

        if (score <= -60) {
            return "hostile";
        }

        if (score <= -25) {
            return "méfiante";
        }

        return "neutre";
    }

    private int calculateMoodDelta(String message) {
        String lower = message.toLowerCase(Locale.ROOT);

        int delta = 0;

        if (containsAny(lower,
                "merci",
                "stp",
                "s'il te plait",
                "s'il te plaît",
                "bravo",
                "bien joué",
                "t'es fort",
                "tu es fort",
                "j'aime bien",
                "ami",
                "pote",
                "je t'aide",
                "je vais t'aider",
                "gentil",
                "sympa",
                "super",
                "cool"
        )) {
            delta += 5;
        }

        if (containsAny(lower,
                "idiot",
                "nul",
                "tais-toi",
                "ta gueule",
                "débile",
                "debile",
                "je te tue",
                "je vais te tuer",
                "meurs",
                "dégage",
                "degage",
                "je te déteste",
                "je te deteste",
                "sale",
                "menace"
        )) {
            delta -= 8;
        }

        if (containsAny(lower,
                "désolé",
                "desole",
                "pardon",
                "excuse"
        )) {
            delta += 3;
        }

        if (containsAny(lower,
                "attaque",
                "combat",
                "tuer",
                "voler",
                "piège",
                "piege"
        )) {
            delta -= 3;
        }

        return clamp(delta, -10, 10);
    }

    private boolean containsAny(String text, String... words) {
        for (String word : words) {
            if (text.contains(word)) {
                return true;
            }
        }

        return false;
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private void load() {
        if (!Files.exists(savePath)) {
            save();
            return;
        }

        try (Reader reader = Files.newBufferedReader(savePath)) {
            Map<String, Map<String, PlayerMemory>> loaded = GSON.fromJson(reader, SAVE_TYPE);

            if (loaded != null) {
                memories.clear();
                memories.putAll(loaded);
            }

            for (Map<String, PlayerMemory> npcMemories : memories.values()) {
                for (PlayerMemory memory : npcMemories.values()) {
                    ensureLists(memory);
                }
            }

            AsmpThingsMod.LOGGER.info("[AI NPC] Mémoire joueurs chargée : {}", savePath);

        } catch (Exception e) {
            AsmpThingsMod.LOGGER.error("[AI NPC] Impossible de charger la mémoire joueurs : {}", savePath, e);
        }
    }

    public void save() {
        try {
            Files.createDirectories(savePath.getParent());

            try (Writer writer = Files.newBufferedWriter(savePath)) {
                GSON.toJson(memories, SAVE_TYPE, writer);
            }

        } catch (IOException e) {
            AsmpThingsMod.LOGGER.error("[AI NPC] Impossible de sauvegarder la mémoire joueurs : {}", savePath, e);
        }
    }

    public record ProfileUpdateResult(
            boolean updated,
            String learnedInfo
    ) {
    }

    public static class PlayerMemory {
        public String playerUuid;
        public String playerName;
        public int relationshipScore;
        public int interactions;
        public String firstSeen;
        public String lastSeen;
        public String lastMessage;
        public int lastMoodChange;

        public String playerSummary;
        public List<String> knownFacts;
        public List<String> recurringTopics;
        public List<String> importantNotes;
        public String lastLearnedInfo;
        public int profileUpdateCount;
        public String lastProfileUpdate;
    }
}