package net.maximlvr.asmpthings;

import net.neoforged.neoforge.common.ModConfigSpec;

public class Config {

    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.BooleanValue DEEPSEEK_ENABLED;
    public static final ModConfigSpec.ConfigValue<String> DEEPSEEK_API_KEY;
    public static final ModConfigSpec.ConfigValue<String> DEEPSEEK_ENDPOINT;
    public static final ModConfigSpec.ConfigValue<String> DEEPSEEK_MODEL;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.push("deepseek");

        DEEPSEEK_ENABLED = builder
                .comment("Active ou désactive les réponses DeepSeek des PNJ.")
                .define("enabled", false);

        DEEPSEEK_API_KEY = builder
                .comment("Clé API DeepSeek. A garder côté serveur uniquement.")
                .define("api_key", "");

        DEEPSEEK_ENDPOINT = builder
                .comment("Endpoint DeepSeek compatible OpenAI.")
                .define("endpoint", "https://api.deepseek.com/chat/completions");

        DEEPSEEK_MODEL = builder
                .comment("Modèle DeepSeek utilisé pour les PNJ.")
                .define("model", "deepseek-v4-pro");

        builder.pop();

        SPEC = builder.build();
    }
}