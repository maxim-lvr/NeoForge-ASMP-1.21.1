package net.maximlvr.asmpthings.network;

import net.maximlvr.asmpthings.ai.voice.PnjVoiceManager;
import net.maximlvr.asmpthings.network.payload.*;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public final class PnjVoiceNetworking {
    public static void register(IEventBus bus) {
        bus.addListener(PnjVoiceNetworking::registerPayloads);
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("1");
        registrar.playToServer(PnjMicrophonePayload.TYPE, PnjMicrophonePayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer player) PnjVoiceManager.microphone(player, payload);
                }));
        registrar.playToServer(PnjVoiceClientEventPayload.TYPE, PnjVoiceClientEventPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer player) PnjVoiceManager.clientEvent(player, payload);
                }));
        registrar.playToClient(PnjVoiceControlPayload.TYPE, PnjVoiceControlPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() ->
                        net.maximlvr.asmpthings.client.voice.PnjVoiceClient.control(payload)));
        registrar.playToClient(PnjSpeakerPayload.TYPE, PnjSpeakerPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() ->
                        net.maximlvr.asmpthings.client.voice.PnjVoiceClient.audio(payload)));
    }

    private PnjVoiceNetworking() {}
}
