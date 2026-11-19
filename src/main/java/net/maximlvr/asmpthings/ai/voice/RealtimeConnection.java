package net.maximlvr.asmpthings.ai.voice;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/** No game objects here: all callbacks are marshalled to the server by the owner. */
public final class RealtimeConnection implements WebSocket.Listener, AutoCloseable {
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private static final int MAX_EVENT_CHARS = 1024 * 1024;
    private static final int MAX_PENDING_SENDS = 100;
    private final Consumer<JsonObject> events;
    private final Consumer<Throwable> errors;
    private final JsonObject configuration;
    private final AtomicBoolean closed = new AtomicBoolean();
    private final StringBuilder incoming = new StringBuilder();
    private WebSocket socket;
    private CompletableFuture<Void> sends = CompletableFuture.completedFuture(null);
    private int pendingSends;

    public RealtimeConnection(JsonObject configuration, Consumer<JsonObject> events, Consumer<Throwable> errors) {
        this.configuration = configuration;
        this.events = events;
        this.errors = errors;
    }

    public void connect(String apiKey, String model) {
        String query = URLEncoder.encode(model, StandardCharsets.UTF_8);
        HTTP.newWebSocketBuilder().connectTimeout(Duration.ofSeconds(10))
                .header("Authorization", "Bearer " + apiKey)
                .buildAsync(URI.create("wss://api.openai.com/v1/realtime?model=" + query), this)
                .whenComplete((connection, error) -> { if (error != null) fail(error); });
    }

    @Override public synchronized void onOpen(WebSocket webSocket) {
        if (closed.get()) { webSocket.abort(); return; }
        socket = webSocket;
        webSocket.request(1);
        send(configuration);
    }

    /** Serialize writes: the JDK WebSocket rejects overlapping sendText calls. */
    public synchronized void send(JsonObject event) {
        if (closed.get() || socket == null) return;
        if (pendingSends >= MAX_PENDING_SENDS) {
            fail(new IllegalStateException("La connexion audio est trop lente."));
            return;
        }
        pendingSends++;
        String json = event.toString();
        sends = sends.thenCompose(ignored -> closed.get() ? CompletableFuture.completedFuture(null)
                : socket.sendText(json, true).thenApply(sent -> (Void) null));
        sends.whenComplete((ignored, error) -> {
            synchronized (this) { pendingSends--; }
            if (error != null) fail(error);
        });
    }

    @Override public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
        if (closed.get()) return null;
        try {
            if (incoming.length() + data.length() > MAX_EVENT_CHARS)
                throw new IllegalArgumentException("Événement Realtime trop volumineux.");
            incoming.append(data);
            if (last) {
                JsonObject event = JsonParser.parseString(incoming.toString()).getAsJsonObject();
                incoming.setLength(0);
                events.accept(event);
            }
            webSocket.request(1);
        } catch (Exception error) {
            fail(error);
        }
        return null;
    }

    @Override public CompletionStage<?> onClose(WebSocket webSocket, int status, String reason) {
        fail(new IllegalStateException("La session Realtime a été fermée (" + status + ")."));
        return null;
    }

    @Override public void onError(WebSocket webSocket, Throwable error) { fail(error); }

    private void fail(Throwable error) {
        if (closed.compareAndSet(false, true)) {
            if (socket != null) socket.abort();
            errors.accept(error);
        }
    }

    @Override public void close() {
        closed.set(true);
        if (socket != null) socket.abort();
    }
}
