package net.maximlvr.asmpthings.client.voice;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class PnjAudioConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.ConfigValue<String> MICROPHONE;
    public static final ModConfigSpec.ConfigValue<String> SPEAKER;
    public static final ModConfigSpec.DoubleValue VOLUME;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        MICROPHONE = builder.comment("Nom OpenAL du micro. Vide = périphérique système par défaut.")
                .define("microphone", "");
        SPEAKER = builder.comment("Nom OpenAL de la sortie audio. Vide = sortie système par défaut.")
                .define("speaker", "");
        VOLUME = builder.defineInRange("volume", 1.0, 0.0, 2.0);
        SPEC = builder.build();
    }

    private PnjAudioConfig() {}
}
