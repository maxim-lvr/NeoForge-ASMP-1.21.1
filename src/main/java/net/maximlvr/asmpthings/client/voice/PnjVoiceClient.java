package net.maximlvr.asmpthings.client.voice;

import net.maximlvr.asmpthings.AsmpThingsMod;
import net.maximlvr.asmpthings.network.payload.*;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = AsmpThingsMod.MOD_ID, value = Dist.CLIENT)
public final class PnjVoiceClient {
    private static volatile PnjAudioSession active;

    public static void control(PnjVoiceControlPayload payload) {
        if (payload.action() == PnjVoiceControlPayload.START) {
            if (active != null) active.close();
            PnjAudioSession session = new PnjAudioSession(payload.sessionId(), payload.npcName(),
                    PnjAudioConfig.MICROPHONE.get(), PnjAudioConfig.SPEAKER.get(), PnjAudioConfig.VOLUME.get());
            active = session;
            session.start();
            message(payload.message());
        } else if (active != null && active.id.equals(payload.sessionId())) {
            if (payload.action() == PnjVoiceControlPayload.STOP) {
                active.close();
                active = null;
                message(payload.message());
            } else if (payload.action() == PnjVoiceControlPayload.INTERRUPT) {
                active.interruptPlayback(payload.itemId(), payload.contentIndex());
            }
        } else if (payload.action() == PnjVoiceControlPayload.STOP) {
            message(payload.message());
        }
    }

    public static void audio(PnjSpeakerPayload payload) {
        PnjAudioSession session = active;
        if (session != null && session.id.equals(payload.sessionId())) session.enqueue(payload);
    }

    static void microphone(PnjAudioSession session, byte[] audio) {
        Minecraft.getInstance().execute(() -> {
            session.pendingMicrophone.decrementAndGet();
            if (active == session && Minecraft.getInstance().getConnection() != null)
                PacketDistributor.sendToServer(new PnjMicrophonePayload(session.id, audio));
        });
    }

    static void played(PnjAudioSession session, String item, int index, int milliseconds) {
        Minecraft.getInstance().execute(() -> {
            if (active == session && Minecraft.getInstance().getConnection() != null)
                PacketDistributor.sendToServer(new PnjVoiceClientEventPayload(session.id,
                        PnjVoiceClientEventPayload.PLAYED, item, index, milliseconds));
        });
    }

    static void failed(PnjAudioSession session, String reason) {
        Minecraft.getInstance().execute(() -> {
            if (active != session) return;
            active = null;
            session.close();
            message(reason);
            if (Minecraft.getInstance().getConnection() != null)
                PacketDistributor.sendToServer(new PnjVoiceClientEventPayload(session.id,
                        PnjVoiceClientEventPayload.STOP, "", 0, 0));
        });
    }

    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
        if (active != null) active.close();
        active = null;
    }

    @SubscribeEvent public static void hud(RenderGuiEvent.Post event) {
        PnjAudioSession session = active;
        Minecraft minecraft = Minecraft.getInstance();
        if (session == null || minecraft.options.hideGui) return;
        String name = session.npcName.length() > 32 ? session.npcName.substring(0, 32) + "…" : session.npcName;
        String label = "PNJ : " + name + (session.microphoneOpen ? " | Micro actif" : " | Ouverture du micro…");
        event.getGuiGraphics().drawString(minecraft.font, label, 8, 8, 0xFFE0E0, true);
        event.getGuiGraphics().fill(8, 20, 88, 23, 0x99000000);
        int width = (int) Math.clamp(session.microphoneLevel * 600, 0, 80);
        event.getGuiGraphics().fill(8, 20, 8 + width, 23, 0xFF55DD88);
    }

    private static void message(String text) {
        if (!text.isBlank() && Minecraft.getInstance().player != null)
            Minecraft.getInstance().player.sendSystemMessage(Component.literal("[PNJ] " + text));
    }

    private PnjVoiceClient() {}
}
