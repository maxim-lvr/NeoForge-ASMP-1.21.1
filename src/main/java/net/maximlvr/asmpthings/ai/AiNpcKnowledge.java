package net.maximlvr.asmpthings.ai;

import net.maximlvr.asmpthings.ai.voice.PnjVoiceMemory;

/** Shared by text and voice: two independent caches must not overwrite the same files. */
public final class AiNpcKnowledge {
    private static AiNpcKnowledge instance;

    public final AiNpcPlayerMemoryStore players = new AiNpcPlayerMemoryStore();
    public final AiNpcGeneralMemoryStore world = new AiNpcGeneralMemoryStore();
    public final AiNpcPersonalityStore personalities = new AiNpcPersonalityStore();
    public final PnjVoiceMemory voice = new PnjVoiceMemory(personalities, players, world);

    private AiNpcKnowledge() {}

    public static synchronized AiNpcKnowledge get() {
        if (instance == null) instance = new AiNpcKnowledge();
        return instance;
    }

    public static synchronized void unload() {
        instance = null;
    }
}
