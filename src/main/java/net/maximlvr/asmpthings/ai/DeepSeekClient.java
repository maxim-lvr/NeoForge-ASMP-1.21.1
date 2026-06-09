package net.maximlvr.asmpthings.ai;

import net.maximlvr.asmpthings.AsmpThingsMod;
import net.maximlvr.asmpthings.Config;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DeepSeekClient {

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private static final ExecutorService AI_EXECUTOR = Executors.newFixedThreadPool(2, runnable -> {
        Thread thread = new Thread(runnable);
        thread.setName("ASMPThings-DeepSeek-Thread");
        thread.setDaemon(true);
        return thread;
    });

    public static CompletableFuture<String> askNpcTurnAsync(String npcName, String systemPrompt, String userPrompt) {
        return CompletableFuture.supplyAsync(() -> askBlocking(npcName, systemPrompt, userPrompt, 900), AI_EXECUTOR);
    }

    private static String askBlocking(String npcName, String systemPrompt, String userPrompt, int maxTokens) {
        boolean enabled = Config.DEEPSEEK_ENABLED.get();
        String endpoint = Config.DEEPSEEK_ENDPOINT.get();
        String model = Config.DEEPSEEK_MODEL.get();
        String apiKey = Config.DEEPSEEK_API_KEY.get();

        if (!enabled) {
            return "";
        }

        if (apiKey == null || apiKey.isBlank()) {
            AsmpThingsMod.LOGGER.warn("[AI NPC] DeepSeek est activé mais api_key est vide.");
            return "";
        }

        try {
            String jsonBody = buildJsonBody(systemPrompt, userPrompt, maxTokens);

            AsmpThingsMod.LOGGER.info("[AI NPC] Envoi requête DeepSeek optimisée pour {}", npcName);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .timeout(Duration.ofSeconds(45))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

            AsmpThingsMod.LOGGER.info("[AI NPC] DeepSeek HTTP status = {}", response.statusCode());

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                AsmpThingsMod.LOGGER.warn("[AI NPC] DeepSeek HTTP {} : {}", response.statusCode(), response.body());
                return "";
            }

            String content = extractAssistantContent(response.body());

            if (content == null || content.isBlank()) {
                AsmpThingsMod.LOGGER.warn("[AI NPC] Réponse DeepSeek vide ou illisible : {}", response.body());
                return "";
            }

            AsmpThingsMod.LOGGER.info("[AI NPC] Réponse DeepSeek extraite = {}", content);

            return content.trim();

        } catch (IOException e) {
            AsmpThingsMod.LOGGER.error("[AI NPC] Erreur réseau DeepSeek", e);
            return "";
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            AsmpThingsMod.LOGGER.error("[AI NPC] Requête DeepSeek interrompue", e);
            return "";
        } catch (Exception e) {
            AsmpThingsMod.LOGGER.error("[AI NPC] Erreur DeepSeek inconnue", e);
            return "";
        }
    }

    private static String buildJsonBody(String systemPrompt, String userPrompt, int maxTokens) {
        String model = Config.DEEPSEEK_MODEL.get();

        return """
                {
                  "model": "%s",
                  "messages": [
                    {
                      "role": "system",
                      "content": "%s"
                    },
                    {
                      "role": "user",
                      "content": "%s"
                    }
                  ],
                  "temperature": 0.7,
                  "max_tokens": %d,
                  "stream": false
                }
                """.formatted(
                escapeJson(model),
                escapeJson(systemPrompt),
                escapeJson(userPrompt),
                maxTokens
        );
    }

    private static String extractAssistantContent(String json) {
        String marker = "\"content\":\"";
        int start = json.indexOf(marker);

        if (start < 0) {
            return null;
        }

        start += marker.length();

        StringBuilder result = new StringBuilder();
        boolean escaping = false;

        for (int i = start; i < json.length(); i++) {
            char c = json.charAt(i);

            if (escaping) {
                switch (c) {
                    case 'n' -> result.append('\n');
                    case 'r' -> result.append('\r');
                    case 't' -> result.append('\t');
                    case '"' -> result.append('"');
                    case '\\' -> result.append('\\');
                    case '/' -> result.append('/');
                    default -> result.append(c);
                }

                escaping = false;
                continue;
            }

            if (c == '\\') {
                escaping = true;
                continue;
            }

            if (c == '"') {
                break;
            }

            result.append(c);
        }

        return result.toString();
    }

    private static String escapeJson(String value) {
        if (value == null) {
            return "";
        }

        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}