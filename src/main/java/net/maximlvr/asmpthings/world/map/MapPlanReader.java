package net.maximlvr.asmpthings.world.map;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.function.BooleanSupplier;

/** Streaming JSON parser: never constructs a JsonElement tree for the map. */
public final class MapPlanReader {
    public record Material(String name, boolean template) {}
    @FunctionalInterface public interface Sink { void accept(MapOperation operation, Material material) throws IOException; }
    private final JsonReader reader;
    private final Sink sink;
    private final BooleanSupplier cancelled;
    private final int minY, maxY;
    private final Map<Material, Integer> ids = new HashMap<>();
    private final List<Material> materials = new ArrayList<>();

    public MapPlanReader(Reader reader, int minY, int maxY, BooleanSupplier cancelled, Sink sink) {
        this.reader = new JsonReader(reader);
        this.minY = minY;
        this.maxY = maxY;
        this.cancelled = cancelled;
        this.sink = sink;
    }

    public List<Material> read() throws IOException {
        try {
            readValue(0);
            if (reader.peek() != JsonToken.END_DOCUMENT) throw new IOException("Données après la fin du JSON");
            return List.copyOf(materials);
        } catch (CancellationException error) {
            throw error;
        } catch (IllegalArgumentException | ArithmeticException | IllegalStateException error) {
            throw new IOException("Plan invalide à " + reader.getPath() + ": " + error.getMessage(), error);
        }
    }

    private void readValue(int depth) throws IOException {
        checkCancelled();
        if (depth > 32) throw new IOException("JSON imbriqué trop profondément");
        if (reader.peek() == JsonToken.BEGIN_ARRAY) {
            reader.beginArray();
            while (reader.hasNext()) readValue(depth + 1);
            reader.endArray();
        } else if (reader.peek() == JsonToken.BEGIN_OBJECT) {
            readObject(depth);
        } else reader.skipValue();
    }

    private void readObject(int depth) throws IOException {
        reader.beginObject();
        String block = null, name = null, kind = "";
        int[] pos = null, size = null;
        int rotation = 0;
        while (reader.hasNext()) {
            checkCancelled();
            switch (reader.nextName()) {
                case "pos" -> pos = vector();
                case "size" -> size = vector();
                case "block" -> block = reader.nextString();
                case "name" -> name = reader.nextString();
                case "kind" -> kind = reader.nextString();
                case "rotation_y" -> rotation = Math.floorMod(reader.nextInt(), 360) / 90;
                case "runs", "placements" -> readValue(depth + 1);
                default -> {
                    // Also accept the older nested placement exports; metadata scalars are simply skipped.
                    if (reader.peek() == JsonToken.BEGIN_ARRAY || reader.peek() == JsonToken.BEGIN_OBJECT) readValue(depth + 1);
                    else reader.skipValue();
                }
            }
        }
        reader.endObject();
        if (pos == null || (block == null && name == null)) return;
        boolean template = block == null && !kind.equals("stone_run");
        String materialName = block != null ? block : name;
        if (materialName == null || materialName.isBlank() || materialName.length() > 512)
            throw new IOException("Matériau vide ou trop long");
        if (template && !materialName.contains(":")) materialName = "asmpthingsmod:" + materialName;
        if (size == null) size = new int[]{1, 1, 1};
        if (size[0] <= 0 || size[1] <= 0 || size[2] <= 0) throw new IOException("Dimensions non positives");
        int endX = Math.addExact(pos[0], size[0]), endY = Math.addExact(pos[1], size[1]), endZ = Math.addExact(pos[2], size[2]);
        if (pos[0] < -30000000 || pos[2] < -30000000 || endX > 30000000 || endZ > 30000000)
            throw new IOException("Placement hors des limites horizontales de Minecraft");
        if (!template) {
            int clippedY = Math.max(pos[1], minY), clippedEndY = Math.min(endY, maxY);
            if (clippedY >= clippedEndY) return;
            pos[1] = clippedY;
            size[1] = clippedEndY - clippedY;
        }
        Material material = new Material(materialName, template);
        Integer id = ids.get(material);
        if (id == null) {
            if (materials.size() >= 4096) throw new IOException("Plus de 4096 matériaux distincts dans le plan");
            id = materials.size();
            ids.put(material, id);
            materials.add(material);
        }
        sink.accept(new MapOperation(id, template ? rotation : 0, pos[0], pos[1], pos[2], size[0], size[1], size[2]), material);
    }

    private int[] vector() throws IOException {
        if (reader.peek() != JsonToken.BEGIN_ARRAY) { reader.skipValue(); return null; }
        int[] value = new int[3];
        reader.beginArray();
        for (int i = 0; i < 3; i++) {
            if (!reader.hasNext()) throw new IOException("Vecteur de moins de trois coordonnées");
            value[i] = reader.nextInt();
        }
        if (reader.hasNext()) throw new IOException("Vecteur de plus de trois coordonnées");
        reader.endArray();
        return value;
    }

    private void checkCancelled() {
        if (cancelled.getAsBoolean() || Thread.currentThread().isInterrupted()) throw new CancellationException("Chargement arrêté");
    }
}
