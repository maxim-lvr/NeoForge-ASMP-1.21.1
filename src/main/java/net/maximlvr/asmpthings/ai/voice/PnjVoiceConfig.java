package net.maximlvr.asmpthings.ai.voice;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class PnjVoiceConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue ENABLED;
    public static final ModConfigSpec.BooleanValue OPERATOR_ONLY;
    public static final ModConfigSpec.ConfigValue<String> API_KEY;
    public static final ModConfigSpec.ConfigValue<String> MODEL;
    public static final ModConfigSpec.ConfigValue<String> VOICE;
    public static final ModConfigSpec.IntValue MAX_SESSIONS;
    public static final ModConfigSpec.IntValue MAX_SESSION_SECONDS;
    public static final ModConfigSpec.IntValue SILENCE_MS;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.push("realtime");
        ENABLED = builder.comment("POC vocal /pnj on et /pnj off.").define("enabled", true);
        OPERATOR_ONLY = builder.comment("Réserver /pnj on aux opérateurs (appels API payants).")
                .define("operator_only", true);
        API_KEY = builder.comment("Clé côté serveur uniquement. OPENAI_API_KEY est prioritaire.")
                .define("api_key", "");
        MODEL = builder.define("model", "gpt-realtime");
        VOICE = builder.define("voice", "marin");
        MAX_SESSIONS = builder.defineInRange("max_sessions", 2, 1, 16);
        MAX_SESSION_SECONDS = builder.comment("Arrêt automatique de la connexion après cette durée.")
                .defineInRange("max_session_seconds", 600, 30, 3300);
        SILENCE_MS = builder.comment("Silence avant la réponse. Une valeur trop courte coupe les pauses naturelles.")
                .defineInRange("silence_ms", 400, 200, 2000);
        builder.pop();
        SPEC = builder.build();
    }

    public static String apiKey() {
        String environment = System.getenv("OPENAI_API_KEY");
        return environment != null && !environment.isBlank() ? environment.trim() : API_KEY.get().trim();
    }

    private PnjVoiceConfig() {}
}
