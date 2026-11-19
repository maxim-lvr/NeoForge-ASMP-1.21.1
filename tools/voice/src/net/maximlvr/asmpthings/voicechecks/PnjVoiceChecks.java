package net.maximlvr.asmpthings.voicechecks;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.maximlvr.asmpthings.ai.*;
import net.maximlvr.asmpthings.ai.voice.*;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

public final class PnjVoiceChecks {
    public static void main(String[] args) throws Exception {
        protocol();
        transport();
        memories();
        System.out.println("PNJ : protocole, fragments WebSocket, backpressure, fermeture et mémoire après rechargement OK.");
    }

    static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void protocol() {
        JsonObject session = RealtimeProtocol.configure("PNJ nommé Marcel", "marin", 400).getAsJsonObject("session");
        require(session.get("type").getAsString().equals("realtime"), "Session GA requise");
        require(session.getAsJsonObject("audio").getAsJsonObject("input").getAsJsonObject("format").get("rate").getAsInt() == 24000, "Taux PCM");
        require(session.get("tool_choice").getAsString().equals("none"), "La mémoire ne doit pas retarder la parole");
        byte[] pcm = new byte[] {0, 0, -1, 127, 0, -128};
        require(Arrays.equals(pcm, Base64.getDecoder().decode(RealtimeProtocol.appendAudio(pcm).get("audio").getAsString())), "PCM modifié");
        try { RealtimeProtocol.appendAudio(new byte[3]); throw new AssertionError("PCM tronqué accepté"); }
        catch (IllegalArgumentException expected) {}
        require(RealtimeProtocol.truncate("item_1", 0, 240).get("audio_end_ms").getAsInt() == 240, "Durée d'interruption");
        JsonObject memory = RealtimeProtocol.memoryUpdate("Souvenirs existants").getAsJsonObject("response");
        require(memory.get("conversation").getAsString().equals("none"), "La mémoire pollue la conversation");
        require(RealtimeProtocol.isMemoryResponse(memory), "Réponse mémoire non reconnue");
        require(memory.getAsJsonArray("output_modalities").get(0).getAsString().equals("text"), "La mémoire ne doit pas parler");
    }

    private static void transport() {
        List<JsonObject> events = new ArrayList<>();
        AtomicInteger failures = new AtomicInteger();
        var connection = new RealtimeConnection(RealtimeProtocol.configure("test", "marin", 400), events::add, error -> failures.incrementAndGet());
        var socket = new FakeSocket();
        connection.onOpen(socket);
        connection.onText(socket, "{\"type\":\"session.", false);
        require(events.isEmpty(), "Fragment JSON traité avant sa fin");
        connection.onText(socket, "updated\"}", true);
        require(events.size() == 1, "Fragmentation WebSocket");
        connection.send(RealtimeProtocol.event("first"));
        connection.send(RealtimeProtocol.event("second"));
        require(socket.sent.size() == 1, "Écritures WebSocket simultanées");
        socket.complete();
        require(socket.sent.size() == 2 && socket.sent.get(1).contains("first"), "Ordre des écritures");
        socket.complete();
        require(socket.sent.size() == 3 && socket.sent.get(2).contains("second"), "Ordre des écritures");
        connection.close();
        connection.onClose(socket, 1000, "normal");
        require(failures.get() == 0 && socket.aborted, "Fermeture volontaire en erreur");
        connection.send(RealtimeProtocol.event("after_close"));
        require(socket.sent.size() == 3, "Envoi après fermeture");

        var blockedSocket = new FakeSocket();
        var blocked = new RealtimeConnection(RealtimeProtocol.event("start"), event -> {}, error -> failures.incrementAndGet());
        blocked.onOpen(blockedSocket);
        for (int i = 0; i < 150; i++) blocked.send(RealtimeProtocol.event("audio"));
        require(failures.get() == 1 && blockedSocket.aborted, "File audio sans limite");
    }

    private static void memories() throws Exception {
        Path directory = Files.createTempDirectory("asmp-voice-memory-check-");
        var personalities = new AiNpcPersonalityStore(directory);
        var players = new AiNpcPlayerMemoryStore(directory);
        var world = new AiNpcGeneralMemoryStore(directory);
        require(personalities.ensureVoiceNpc("Marcel"), "PNJ renommé non enregistré");
        UUID player = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        players.recordPlayerMessage("Marcel", player, "Camille", "Ma base est au lac.");
        String update = """
                {"playerMemory":{"shouldUpdate":true,"relationIntent":"same","knownFacts":["Camille habite au lac"],"playerSummary":"Camille habite au lac"},
                 "generalMemory":{"shouldUpdate":true,"knownFacts":["Le village possède un phare"]}}
                """;
        var memory = new PnjVoiceMemory(personalities, players, world);
        require(memory.applyTool("Marcel", player, "Camille", update).get("saved").getAsBoolean(), "Mise à jour refusée");
        require(!memory.applyTool("Marcel", player, "Camille", "{}").get("saved").getAsBoolean(), "Arguments invalides acceptés");
        JsonObject background = JsonParser.parseString("""
                {"status":"completed","metadata":{"purpose":"npc_memory"},"output":[
                  {"type":"message","content":[{"type":"output_text","text":""}]}
                ]}
                """).getAsJsonObject();
        background.getAsJsonArray("output").get(0).getAsJsonObject().getAsJsonArray("content").get(0)
                .getAsJsonObject().addProperty("text", update);
        require(memory.applyResponse("Marcel", player, "Camille", background), "JSON de mémoire ignoré");
        JsonObject content = background.getAsJsonArray("output").get(0).getAsJsonObject()
                .getAsJsonArray("content").get(0).getAsJsonObject();
        content.addProperty("text", """
                <?xml version="1.0" encoding="UTF-8"?>
                <updates>
                  <playerMemory><shouldUpdate>false</shouldUpdate><relationIntent>same</relationIntent></playerMemory>
                  <generalMemory><shouldUpdate>false</shouldUpdate></generalMemory>
                </updates>
                """);
        require(memory.applyResponse("Marcel", player, "Camille", background), "Tour XML sans nouveau souvenir rejeté");
        content.addProperty("text", """
                <updates>
                  <playerMemory><shouldUpdate>true</shouldUpdate><knownFacts><fact>Camille aime les pommes</fact></knownFacts></playerMemory>
                  <generalMemory><shouldUpdate>false</shouldUpdate></generalMemory>
                </updates>
                """);
        require(memory.applyResponse("Marcel", player, "Camille", background), "Fait XML non sauvegardé");
        require(players.exportCompactProfileJsonForAi("Marcel", player, "Camille").contains("pommes"), "Fait XML perdu");
        content.addProperty("text", "<!DOCTYPE updates [<!ENTITY probe SYSTEM 'file:///etc/passwd'>]><updates>&probe;</updates>");
        require(!memory.applyResponse("Marcel", player, "Camille", background), "Entités XML externes autorisées");
        content.addProperty("text", update);
        background.remove("metadata");
        require(!memory.applyResponse("Marcel", player, "Camille", background), "Une réponse vocale ordinaire modifie les souvenirs");
        var reloadedPlayers = new AiNpcPlayerMemoryStore(directory);
        var reloadedWorld = new AiNpcGeneralMemoryStore(directory);
        var reloadedPersonalities = new AiNpcPersonalityStore(directory);
        require(reloadedPersonalities.isAiNpcName("Marcel"), "Personnalité perdue au redémarrage");
        require(reloadedPlayers.exportCompactProfileJsonForAi("Marcel", player, "Camille").contains("lac"), "Souvenir joueur perdu");
        require(reloadedWorld.exportCompactGeneralMemoryJsonForAi("Marcel").contains("phare"), "Souvenir général perdu");
        require(!reloadedPlayers.exportCompactProfileJsonForAi("Marcel", other, "Autre").contains("lac"), "Confusion entre joueurs");
        require(!reloadedWorld.exportCompactGeneralMemoryJsonForAi("Autre PNJ").contains("phare"), "Confusion entre PNJ");
        String prompt = new PnjVoiceMemory(reloadedPersonalities, reloadedPlayers, reloadedWorld)
                .instructions("Marcel", player, "Camille", "minecraft:overworld");
        require(prompt.contains("lac") && prompt.contains("phare") && !prompt.contains("Réponds uniquement avec un JSON valide"), "Contexte vocal incorrect");
        // Fixture files only; never touch the user's real memories.
        try (var paths = Files.walk(directory)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) Files.delete(path);
        }
    }

    private static final class FakeSocket implements WebSocket {
        final List<String> sent = new ArrayList<>();
        final Deque<CompletableFuture<WebSocket>> pending = new ArrayDeque<>();
        boolean aborted;
        void complete() { pending.removeFirst().complete(this); }
        @Override public CompletableFuture<WebSocket> sendText(CharSequence data, boolean last) {
            sent.add(data.toString());
            CompletableFuture<WebSocket> future = new CompletableFuture<>();
            pending.addLast(future);
            return future;
        }
        @Override public CompletableFuture<WebSocket> sendBinary(ByteBuffer data, boolean last) { throw new UnsupportedOperationException(); }
        @Override public CompletableFuture<WebSocket> sendPing(ByteBuffer data) { return CompletableFuture.completedFuture(this); }
        @Override public CompletableFuture<WebSocket> sendPong(ByteBuffer data) { return CompletableFuture.completedFuture(this); }
        @Override public CompletableFuture<WebSocket> sendClose(int status, String reason) { return CompletableFuture.completedFuture(this); }
        @Override public void request(long n) {}
        @Override public String getSubprotocol() { return ""; }
        @Override public boolean isOutputClosed() { return aborted; }
        @Override public boolean isInputClosed() { return aborted; }
        @Override public void abort() { aborted = true; }
    }
}
