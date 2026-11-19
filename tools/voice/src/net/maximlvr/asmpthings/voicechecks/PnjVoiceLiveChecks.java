package net.maximlvr.asmpthings.voicechecks;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.maximlvr.asmpthings.ai.*;
import net.maximlvr.asmpthings.ai.voice.*;
import java.io.ByteArrayOutputStream;
import java.net.http.WebSocketHandshakeException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/** Explicit, paid API check. Uses synthetic speech; never opens the user's microphone. */
public final class PnjVoiceLiveChecks {
    public static void main(String[] args) throws Exception {
        String key = Files.readString(Path.of(System.getProperty("asmp.voice.keyFile"))).trim();
        String model = System.getProperty("asmp.voice.model", "gpt-realtime");
        byte[] spokenInput;
        try (Probe probe = new Probe(key, model, "Lis les phrases demandées à voix haute en français, sans rien ajouter.")) {
            probe.userText("Lis exactement cette phrase : Bonjour Marcel. Je m'appelle Camille et ma base est au bord du lac bleu. Souviens-toi de ma base pour la prochaine fois.");
            ByteArrayOutputStream audio = new ByteArrayOutputStream();
            while (true) {
                JsonObject event = probe.next();
                if (type(event).equals("response.output_audio.delta")) audio.writeBytes(Base64.getDecoder().decode(text(event, "delta")));
                if (type(event).equals("response.done")) {
                    requireCompleted(event.getAsJsonObject("response"));
                    break;
                }
            }
            spokenInput = audio.toByteArray();
            PnjVoiceChecks.require(spokenInput.length > 4800, "Pas d'audio de test reçu");
            System.out.println("OpenAI : session gpt-realtime et audio de test synthétique OK (" + spokenInput.length / 48 + " ms).");
        }

        Path directory = Files.createTempDirectory("asmp-realtime-live-");
        var personalities = new AiNpcPersonalityStore(directory);
        var players = new AiNpcPlayerMemoryStore(directory);
        var world = new AiNpcGeneralMemoryStore(directory);
        var memory = new PnjVoiceMemory(personalities, players, world);
        UUID player = UUID.randomUUID();
        personalities.ensureVoiceNpc("Marcel");
        String instructions = memory.instructions("Marcel", player, "Camille", "minecraft:overworld");
        try (Probe probe = new Probe(key, model, instructions)) {
            // Same PCM frames as the in-game client, at real-time speed, followed by silence for VAD.
            for (int offset = 0; offset < spokenInput.length; offset += 1920) {
                probe.connection.send(RealtimeProtocol.appendAudio(Arrays.copyOfRange(spokenInput, offset, Math.min(offset + 1920, spokenInput.length))));
                Thread.sleep(40);
            }
            for (int i = 0; i < 20; i++) {
                probe.connection.send(RealtimeProtocol.appendAudio(new byte[1920]));
                Thread.sleep(40);
            }
            boolean heardReply = false;
            boolean saved = false;
            boolean transcription = false;
            long speechStopped = 0;
            while (!saved) {
                JsonObject event = probe.next();
                switch (type(event)) {
                    case "input_audio_buffer.speech_stopped" -> speechStopped = event.get("_received_nanos").getAsLong();
                    case "conversation.item.input_audio_transcription.completed" -> {
                        String transcript = text(event, "transcript");
                        PnjVoiceChecks.require(!transcript.isBlank(), "Transcription vide");
                        players.recordPlayerMessage("Marcel", player, "Camille", transcript);
                        transcription = true;
                    }
                    case "response.output_audio.delta" -> {
                        if (!heardReply && speechStopped != 0)
                            System.out.println("Premier audio après fin de parole : " + (event.get("_received_nanos").getAsLong() - speechStopped) / 1_000_000 + " ms (hors lecture locale).");
                        heardReply = true;
                    }
                    case "response.done" -> {
                        JsonObject response = event.getAsJsonObject("response");
                        if (text(response, "status").equals("cancelled")) continue;
                        requireCompleted(response);
                        if (RealtimeProtocol.isMemoryResponse(response)) {
                            saved = memory.applyResponse("Marcel", player, "Camille", response);
                            PnjVoiceChecks.require(saved, "Pas de sauvegarde de mémoire");
                        } else {
                            PnjVoiceChecks.require(heardReply, "Réponse sans audio");
                            probe.connection.send(RealtimeProtocol.memoryUpdate(memory.instructions("Marcel", player, "Camille", "minecraft:overworld")));
                        }
                    }
                    default -> {}
                }
            }
            PnjVoiceChecks.require(heardReply && transcription, "Flux vocal ou transcription manquante");
        }
        var reloaded = new PnjVoiceMemory(new AiNpcPersonalityStore(directory),
                new AiNpcPlayerMemoryStore(directory), new AiNpcGeneralMemoryStore(directory));
        String restoredPrompt = reloaded.instructions("Marcel", player, "Camille", "minecraft:overworld");
        PnjVoiceChecks.require(restoredPrompt.toLowerCase(Locale.ROOT).contains("lac"), "La base n'a pas été mémorisée sur disque");
        try (Probe probe = new Probe(key, model, restoredPrompt)) {
            probe.userText("Tu te souviens de moi ? Où se trouve ma base ?");
            String reply = "";
            while (true) {
                JsonObject event = probe.next();
                if (type(event).equals("response.output_audio_transcript.done")) reply = text(event, "transcript");
                if (type(event).equals("response.done")) {
                    requireCompleted(event.getAsJsonObject("response"));
                    break;
                }
            }
            PnjVoiceChecks.require(reply.toLowerCase(Locale.ROOT).contains("lac"), "Le PNJ n'a pas rappelé le souvenir après reconnexion");
            probe.userText("Tu connais la blague du coiffeur ?");
            while (!type(probe.next()).equals("response.done")) {}
            probe.connection.send(RealtimeProtocol.memoryUpdate(restoredPrompt));
            while (true) {
                JsonObject event = probe.next();
                if (type(event).equals("response.done") && RealtimeProtocol.isMemoryResponse(event.getAsJsonObject("response"))) {
                    JsonObject response = event.getAsJsonObject("response");
                    boolean applied = reloaded.applyResponse("Marcel", player, "Camille", response);
                    if (!applied) System.out.println("Mémoire du scénario synthétique sans nouvelle information : " + response);
                    PnjVoiceChecks.require(applied, "Une discussion ordinaire fait échouer la mémoire");
                    break;
                }
            }
        }
        System.out.println("OpenAI : entrée PCM, VAD, réponse audio, transcription, mémoire sur disque et rappel après reconnexion OK.");
        try (var paths = Files.walk(directory)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) Files.delete(path);
        }
    }

    private static final class Probe implements AutoCloseable {
        final LinkedBlockingQueue<JsonObject> events = new LinkedBlockingQueue<>();
        final RealtimeConnection connection;
        final long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(100);

        Probe(String key, String model, String instructions) throws Exception {
            connection = new RealtimeConnection(RealtimeProtocol.configure(instructions, "marin", 400), event -> {
                event.addProperty("_received_nanos", System.nanoTime());
                events.add(event);
            }, error -> {
                while (error.getCause() != null) error = error.getCause();
                JsonObject failure = RealtimeProtocol.event("test.transport_error");
                failure.addProperty("code", error instanceof WebSocketHandshakeException handshake
                        ? "HTTP " + handshake.getResponse().statusCode() : error.getClass().getSimpleName());
                events.offer(failure);
            });
            connection.connect(key, model);
            while (!type(next()).equals("session.updated")) {}
        }

        JsonObject next() throws Exception {
            long remaining = deadline - System.nanoTime();
            if (remaining <= 0) throw new IllegalStateException("Délai du test Realtime dépassé");
            JsonObject event = events.poll(remaining, TimeUnit.NANOSECONDS);
            if (event == null) throw new IllegalStateException("Aucun événement Realtime reçu");
            if (type(event).equals("test.transport_error")) throw new IllegalStateException("Connexion Realtime : " + text(event, "code"));
            if (type(event).equals("error")) throw new IllegalStateException("Erreur Realtime : " + text(event.getAsJsonObject("error"), "code")
                    + " (paramètre " + text(event.getAsJsonObject("error"), "param") + ")");
            return event;
        }

        void userText(String text) {
            JsonObject content = new JsonObject();
            content.addProperty("type", "input_text");
            content.addProperty("text", text);
            JsonArray contents = new JsonArray();
            contents.add(content);
            JsonObject item = new JsonObject();
            item.addProperty("type", "message");
            item.addProperty("role", "user");
            item.add("content", contents);
            JsonObject event = RealtimeProtocol.event("conversation.item.create");
            event.add("item", item);
            connection.send(event);
            connection.send(RealtimeProtocol.event("response.create"));
        }

        @Override public void close() { connection.close(); }
    }

    private static String type(JsonObject event) { return text(event, "type"); }
    private static String text(JsonObject event, String key) {
        return event.has(key) && !event.get(key).isJsonNull() ? event.get(key).getAsString() : "";
    }
    private static void requireCompleted(JsonObject response) {
        PnjVoiceChecks.require(text(response, "status").equals("completed"), "Réponse incomplète : " + text(response, "status"));
    }
}
