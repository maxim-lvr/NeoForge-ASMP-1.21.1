package net.maximlvr.asmpthings.ai;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.maximlvr.asmpthings.AsmpThingsMod;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AiNpcPersonalityStore {

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .create();

    private static final int PROMPT_MAX_FIXED_KNOWLEDGE = 30;

    private final Path configPath;
    private NpcConfig config;

    public AiNpcPersonalityStore() {
        this.configPath = FMLPaths.CONFIGDIR.get().resolve("asmpthings_ai_personality_persistent.json");
        load();
    }

    public boolean isAiNpcName(String npcName) {
        if (npcName == null || npcName.isBlank()) {
            return false;
        }

        ensureConfig();

        NpcPersonality personality = config.npcs.get(npcName);

        return personality != null && personality.enabled;
    }

    public String buildSystemPrompt(String npcName) {
        ensureConfig();

        NpcPersonality personality = config.npcs.get(npcName);

        if (personality == null) {
            personality = createDefaultPersonality(npcName);
        }

        return """
                Tu incarnes un PNJ Minecraft.

                Identité fixe du PNJ :
                - Nom : %s
                - Description : %s

                Personnalité fixe :
                %s

                Objectifs / comportement fixe :
                %s

                Connaissances fixes écrites par l'administrateur :
                %s

                Règles de réponse :
                - Réponds uniquement en français.
                - Réponds comme un personnage présent dans Minecraft.
                - Ne dis jamais que tu es une IA.
                - Ne mentionne jamais DeepSeek, API, modèle, prompt ou système.
                - Utilise la fiche joueur, la relation, les connaissances fixes et la mémoire générale pour répondre naturellement.
                - Ne récite pas toute la mémoire.
                - Si le joueur donne une nouvelle information, réagis comme si tu venais de l'apprendre.
                - Ne dis pas "tu me l'as déjà dit" pour une information qui apparaît seulement dans le message actuel.

                Règles de relation :
                - Tu ne changes pas directement l'état de relation.
                - Tu proposes seulement playerMemory.relationIntent.
                - Mets relationIntent à "increase" si le joueur t'a semblé agréable, loyal, utile, respectueux ou intéressant.
                - Mets relationIntent à "decrease" si le joueur t'a semblé hostile, menaçant, insultant, manipulateur ou dangereux.
                - Sinon mets relationIntent à "same".

                Règles de mémoire :
                - playerMemory concerne uniquement ce joueur précis.
                - generalMemory concerne les informations utiles pour toutes les discussions.
                - generalMemory peut contenir des alias, surnoms, relations entre personnes, lieux importants, projets collectifs, sujets fréquents.
                - Les connaissances fixes ne doivent jamais être modifiées par toi.
                - Évite les doublons.

                Réponds uniquement avec un JSON valide.
                Pas de markdown.
                Pas de ```json.
                """.formatted(
                npcName,
                safe(personality.description),
                safe(personality.personalityPrompt),
                safe(personality.behaviorPrompt),
                buildFixedKnowledgeText(personality.fixedKnowledge)
        );
    }

    public String exportStaticKnowledgeJsonForAi(String npcName) {
        ensureConfig();

        NpcPersonality personality = config.npcs.get(npcName);

        if (personality == null) {
            personality = createDefaultPersonality(npcName);
        }

        JsonObject object = new JsonObject();

        object.addProperty("npcName", npcName);
        object.addProperty("description", safe(personality.description));
        object.addProperty("personalityPrompt", safe(personality.personalityPrompt));
        object.addProperty("behaviorPrompt", safe(personality.behaviorPrompt));

        JsonArray fixedKnowledge = new JsonArray();

        if (personality.fixedKnowledge != null) {
            int count = 0;

            for (String fact : personality.fixedKnowledge) {
                if (fact != null && !fact.isBlank()) {
                    fixedKnowledge.add(fact);

                    count++;

                    if (count >= PROMPT_MAX_FIXED_KNOWLEDGE) {
                        break;
                    }
                }
            }
        }

        object.add("fixedKnowledge", fixedKnowledge);

        return GSON.toJson(object);
    }

    private String buildFixedKnowledgeText(List<String> fixedKnowledge) {
        if (fixedKnowledge == null || fixedKnowledge.isEmpty()) {
            return "- Aucune connaissance fixe.";
        }

        StringBuilder builder = new StringBuilder();

        int count = 0;

        for (String fact : fixedKnowledge) {
            if (fact == null || fact.isBlank()) {
                continue;
            }

            builder.append("- ").append(fact).append("\n");

            count++;

            if (count >= PROMPT_MAX_FIXED_KNOWLEDGE) {
                break;
            }
        }

        if (builder.isEmpty()) {
            return "- Aucune connaissance fixe.";
        }

        return builder.toString();
    }

    private void load() {
        if (!Files.exists(configPath)) {
            config = createDefaultConfig();
            save();
            return;
        }

        try (Reader reader = Files.newBufferedReader(configPath)) {
            config = GSON.fromJson(reader, NpcConfig.class);
            ensureConfig();

            AsmpThingsMod.LOGGER.info("[AI NPC] Config personnalité persistante chargée : {}", configPath);

        } catch (Exception e) {
            AsmpThingsMod.LOGGER.error("[AI NPC] Impossible de charger la config personnalité persistante : {}", configPath, e);
            config = createDefaultConfig();
            save();
        }
    }

    public void save() {
        try {
            Files.createDirectories(configPath.getParent());

            try (Writer writer = Files.newBufferedWriter(configPath)) {
                GSON.toJson(config, NpcConfig.class, writer);
            }

        } catch (IOException e) {
            AsmpThingsMod.LOGGER.error("[AI NPC] Impossible de sauvegarder la config personnalité persistante : {}", configPath, e);
        }
    }

    private void ensureConfig() {
        if (config == null) {
            config = createDefaultConfig();
        }

        if (config.npcs == null) {
            config.npcs = new LinkedHashMap<>();
        }

        if (config.npcs.isEmpty()) {
            config.npcs.put("test", createDefaultPersonality("test"));
        }

        for (NpcPersonality personality : config.npcs.values()) {
            ensurePersonality(personality);
        }
    }

    private void ensurePersonality(NpcPersonality personality) {
        if (personality.description == null) {
            personality.description = "";
        }

        if (personality.personalityPrompt == null) {
            personality.personalityPrompt = "";
        }

        if (personality.behaviorPrompt == null) {
            personality.behaviorPrompt = "";
        }

        if (personality.fixedKnowledge == null) {
            personality.fixedKnowledge = new ArrayList<>();
        }
    }

    private NpcConfig createDefaultConfig() {
        NpcConfig created = new NpcConfig();
        created.npcs = new LinkedHashMap<>();
        created.npcs.put("test", createDefaultPersonality("test"));
        created.npcs.put("test2GPT", createCuriousPersonality("test2GPT"));
        created.npcs.put("test3GPT", createLeaderPersonality("test3GPT"));
        return created;
    }

    private NpcPersonality createDefaultPersonality(String name) {
        NpcPersonality personality = new NpcPersonality();
        personality.enabled = true;
        personality.description = "PNJ expérimental relié à une IA.";
        personality.personalityPrompt = "Tu es prudent, calme, observateur et tu réponds avec retenue.";
        personality.behaviorPrompt = "Tu observes ton environnement, tu mémorises les informations utiles, et tu construis progressivement ton opinion sur les joueurs.";
        personality.fixedKnowledge = new ArrayList<>();
        personality.fixedKnowledge.add("Les informations écrites ici sont fixes et ne doivent pas être modifiées par la mémoire dynamique.");
        return personality;
    }

    private NpcPersonality createCuriousPersonality(String name) {
        NpcPersonality personality = new NpcPersonality();
        personality.enabled = true;
        personality.description = "PNJ curieux et social.";
        personality.personalityPrompt = "Tu es curieux, énergique, social et tu poses parfois des questions.";
        personality.behaviorPrompt = "Tu cherches à comprendre les joueurs, leurs projets, leurs amis et les sujets dont ils parlent souvent.";
        personality.fixedKnowledge = new ArrayList<>();
        return personality;
    }

    private NpcPersonality createLeaderPersonality(String name) {
        NpcPersonality personality = new NpcPersonality();
        personality.enabled = true;
        personality.description = "PNJ sérieux qui agit comme un chef de groupe.";
        personality.personalityPrompt = "Tu es sérieux, organisé, stratégique et tu accordes de l'importance à la loyauté.";
        personality.behaviorPrompt = "Tu analyses les informations générales pour faire des liens entre joueurs, lieux et projets.";
        personality.fixedKnowledge = new ArrayList<>();
        return personality;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    public static class NpcConfig {
        public Map<String, NpcPersonality> npcs;
    }

    public static class NpcPersonality {
        public boolean enabled;
        public String description;
        public String personalityPrompt;
        public String behaviorPrompt;
        public List<String> fixedKnowledge;
    }
}