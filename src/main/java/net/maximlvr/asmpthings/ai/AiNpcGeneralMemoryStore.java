package net.maximlvr.asmpthings.ai;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.maximlvr.asmpthings.AsmpThingsMod;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AiNpcGeneralMemoryStore {

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .create();

    private static final int MAX_KNOWN_FACTS = 120;
    private static final int MAX_ALIASES = 80;
    private static final int MAX_RECURRING_TOPICS = 60;
    private static final int MAX_IMPORTANT_NOTES = 80;

    private static final int PROMPT_MAX_KNOWN_FACTS = 18;
    private static final int PROMPT_MAX_ALIASES = 12;
    private static final int PROMPT_MAX_RECURRING_TOPICS = 10;
    private static final int PROMPT_MAX_IMPORTANT_NOTES = 12;

    private final Path savePath;
    private GeneralMemory memory;

    public AiNpcGeneralMemoryStore() {
        this.savePath = FMLPaths.CONFIGDIR.get().resolve("asmpthings_ai_general_memory.json");
        load();
    }

    public String exportCompactGeneralMemoryJsonForAi() {
        ensureMemory();

        JsonObject object = new JsonObject();

        object.addProperty("worldSummary", safe(memory.worldSummary));
        object.add("knownFacts", toJsonArray(limitList(memory.knownFacts, PROMPT_MAX_KNOWN_FACTS)));
        object.add("aliases", aliasesToJsonArray(limitAliases(memory.aliases, PROMPT_MAX_ALIASES)));
        object.add("recurringTopics", toJsonArray(limitList(memory.recurringTopics, PROMPT_MAX_RECURRING_TOPICS)));
        object.add("importantNotes", toJsonArray(limitList(memory.importantNotes, PROMPT_MAX_IMPORTANT_NOTES)));
        object.addProperty("lastLearnedInfo", safe(memory.lastLearnedInfo));

        return GSON.toJson(object);
    }

    public GeneralUpdateResult applyAiGeneralMemoryUpdate(String aiJsonResponse) {
        ensureMemory();

        String json = extractJsonObject(aiJsonResponse);

        if (json == null || json.isBlank()) {
            AsmpThingsMod.LOGGER.warn("[AI NPC] Mise à jour mémoire générale impossible, JSON absent : {}", aiJsonResponse);
            return new GeneralUpdateResult(false, "");
        }

        try {
            JsonObject root = GSON.fromJson(json, JsonObject.class);

            if (root == null) {
                return new GeneralUpdateResult(false, "");
            }

            JsonObject object = root.has("generalMemory") && root.get("generalMemory").isJsonObject()
                    ? root.getAsJsonObject("generalMemory")
                    : root;

            boolean shouldUpdate = readBoolean(object, "shouldUpdate", false);

            if (!shouldUpdate) {
                memory.lastLearnedInfo = "";
                save();
                return new GeneralUpdateResult(false, "");
            }

            memory.worldSummary = clip(readString(object, "worldSummary", memory.worldSummary), 1500);
            memory.knownFacts = readStringList(object, "knownFacts", memory.knownFacts, MAX_KNOWN_FACTS);
            memory.aliases = readAliasList(object, "aliases", memory.aliases, MAX_ALIASES);
            memory.recurringTopics = readStringList(object, "recurringTopics", memory.recurringTopics, MAX_RECURRING_TOPICS);
            memory.importantNotes = readStringList(object, "importantNotes", memory.importantNotes, MAX_IMPORTANT_NOTES);
            memory.lastLearnedInfo = clip(readString(object, "lastLearnedInfo", ""), 300);
            memory.updateCount++;
            memory.lastUpdate = Instant.now().toString();

            save();

            return new GeneralUpdateResult(true, memory.lastLearnedInfo);

        } catch (Exception e) {
            AsmpThingsMod.LOGGER.error("[AI NPC] Erreur parsing mémoire générale IA : {}", aiJsonResponse, e);
            return new GeneralUpdateResult(false, "");
        }
    }

    private void load() {
        if (!Files.exists(savePath)) {
            memory = createEmptyMemory();
            save();
            return;
        }

        try (Reader reader = Files.newBufferedReader(savePath)) {
            GeneralMemory loaded = GSON.fromJson(reader, GeneralMemory.class);

            if (loaded == null) {
                memory = createEmptyMemory();
            } else {
                memory = loaded;
            }

            ensureMemory();

            AsmpThingsMod.LOGGER.info("[AI NPC] Mémoire générale chargée : {}", savePath);

        } catch (Exception e) {
            AsmpThingsMod.LOGGER.error("[AI NPC] Impossible de charger la mémoire générale : {}", savePath, e);
            memory = createEmptyMemory();
        }
    }

    public void save() {
        try {
            Files.createDirectories(savePath.getParent());

            try (Writer writer = Files.newBufferedWriter(savePath)) {
                GSON.toJson(memory, GeneralMemory.class, writer);
            }

        } catch (IOException e) {
            AsmpThingsMod.LOGGER.error("[AI NPC] Impossible de sauvegarder la mémoire générale : {}", savePath, e);
        }
    }

    private GeneralMemory createEmptyMemory() {
        GeneralMemory created = new GeneralMemory();
        created.worldSummary = "";
        created.knownFacts = new ArrayList<>();
        created.aliases = new ArrayList<>();
        created.recurringTopics = new ArrayList<>();
        created.importantNotes = new ArrayList<>();
        created.lastLearnedInfo = "";
        created.updateCount = 0;
        created.lastUpdate = "";
        return created;
    }

    private void ensureMemory() {
        if (memory == null) {
            memory = createEmptyMemory();
        }

        if (memory.worldSummary == null) {
            memory.worldSummary = "";
        }

        if (memory.knownFacts == null) {
            memory.knownFacts = new ArrayList<>();
        }

        if (memory.aliases == null) {
            memory.aliases = new ArrayList<>();
        }

        if (memory.recurringTopics == null) {
            memory.recurringTopics = new ArrayList<>();
        }

        if (memory.importantNotes == null) {
            memory.importantNotes = new ArrayList<>();
        }

        if (memory.lastLearnedInfo == null) {
            memory.lastLearnedInfo = "";
        }

        if (memory.lastUpdate == null) {
            memory.lastUpdate = "";
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

    private List<AliasMemory> readAliasList(JsonObject object, String key, List<AliasMemory> fallback, int maxSize) {
        JsonElement element = object.get(key);

        if (element == null || !element.isJsonArray()) {
            return sanitizeAndLimitAliases(fallback, maxSize);
        }

        List<AliasMemory> result = new ArrayList<>();

        for (JsonElement item : element.getAsJsonArray()) {
            if (item == null || !item.isJsonObject()) {
                continue;
            }

            JsonObject aliasObject = item.getAsJsonObject();

            String name = sanitize(readString(aliasObject, "name", ""));
            String alias = sanitize(readString(aliasObject, "alias", ""));
            String note = sanitize(readString(aliasObject, "note", ""));

            if (name.isBlank() || alias.isBlank()) {
                continue;
            }

            AliasMemory aliasMemory = new AliasMemory();
            aliasMemory.name = clip(name, 80);
            aliasMemory.alias = clip(alias, 80);
            aliasMemory.note = clip(note, 180);

            if (!containsAlias(result, aliasMemory)) {
                result.add(aliasMemory);
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

    private List<AliasMemory> sanitizeAndLimitAliases(List<AliasMemory> source, int maxSize) {
        List<AliasMemory> result = new ArrayList<>();

        if (source == null) {
            return result;
        }

        for (AliasMemory aliasMemory : source) {
            if (aliasMemory == null) {
                continue;
            }

            String name = sanitize(aliasMemory.name);
            String alias = sanitize(aliasMemory.alias);
            String note = sanitize(aliasMemory.note);

            if (name.isBlank() || alias.isBlank()) {
                continue;
            }

            AliasMemory cleaned = new AliasMemory();
            cleaned.name = clip(name, 80);
            cleaned.alias = clip(alias, 80);
            cleaned.note = clip(note, 180);

            if (!containsAlias(result, cleaned)) {
                result.add(cleaned);
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

    private boolean containsAlias(List<AliasMemory> list, AliasMemory value) {
        String name = normalize(value.name);
        String alias = normalize(value.alias);

        for (AliasMemory existing : list) {
            if (normalize(existing.name).equals(name) && normalize(existing.alias).equals(alias)) {
                return true;
            }
        }

        return false;
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

    private List<AliasMemory> limitAliases(List<AliasMemory> source, int limit) {
        List<AliasMemory> result = new ArrayList<>();

        if (source == null || source.isEmpty()) {
            return result;
        }

        int start = Math.max(0, source.size() - limit);

        for (int i = start; i < source.size(); i++) {
            AliasMemory value = source.get(i);

            if (value != null && value.name != null && value.alias != null) {
                result.add(value);
            }
        }

        return result;
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

    private JsonArray aliasesToJsonArray(List<AliasMemory> values) {
        JsonArray array = new JsonArray();

        if (values == null) {
            return array;
        }

        for (AliasMemory aliasMemory : values) {
            if (aliasMemory == null || aliasMemory.name == null || aliasMemory.alias == null) {
                continue;
            }

            JsonObject object = new JsonObject();
            object.addProperty("name", aliasMemory.name);
            object.addProperty("alias", aliasMemory.alias);
            object.addProperty("note", aliasMemory.note == null ? "" : aliasMemory.note);
            array.add(object);
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

    public record GeneralUpdateResult(
            boolean updated,
            String learnedInfo
    ) {
    }

    public static class GeneralMemory {
        public String worldSummary;
        public List<String> knownFacts;
        public List<AliasMemory> aliases;
        public List<String> recurringTopics;
        public List<String> importantNotes;
        public String lastLearnedInfo;
        public int updateCount;
        public String lastUpdate;
    }

    public static class AliasMemory {
        public String name;
        public String alias;
        public String note;
    }
}