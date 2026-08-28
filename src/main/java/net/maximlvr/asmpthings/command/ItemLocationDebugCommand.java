package net.maximlvr.asmpthings.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.maximlvr.asmpthings.AsmpThingsMod;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.storage.RegionFile;
import net.minecraft.world.level.chunk.storage.RegionStorageInfo;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.io.DataInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@EventBusSubscriber(modid = AsmpThingsMod.MOD_ID)
public final class ItemLocationDebugCommand {
    private static final Pattern SPLIT_TARGETS = Pattern.compile("[\\s,;]+");
    private static final Pattern REGION_FILE = Pattern.compile("r\\.(-?\\d+)\\.(-?\\d+)\\.mca");
    private static final Pattern UUID_TEXT = Pattern.compile("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}");
    private static final int MAX_REPORTED_ERRORS = 200;
    private static final int MAX_STACK_PREVIEW_LENGTH = 500;

    private ItemLocationDebugCommand() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("asmpfinditem")
                .requires(source -> source.hasPermission(2))
                .then(Commands.argument("items", StringArgumentType.greedyString())
                        .suggests(ItemLocationDebugCommand::suggestItems)
                        .executes(ItemLocationDebugCommand::startScan)));
    }

    private static int startScan(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        Set<String> targets = parseTargets(StringArgumentType.getString(context, "items"));

        if (targets.isEmpty()) {
            source.sendFailure(Component.literal("Aucun item valide. Exemple: /asmpfinditem minecraft:diamond sophisticatedbackpacks:backpack"));
            return 0;
        }

        MinecraftServer server = source.getServer();
        Path worldRoot = server.getWorldPath(LevelResource.ROOT).normalize();
        String stamp = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss").format(LocalDateTime.now());
        Path output = worldRoot.resolve("asmp_item_debug_" + stamp + ".json");

        source.sendSuccess(() -> Component.literal("Scan item lance en arriere-plan pour " + String.join(", ", targets) + "."), true);
        source.sendSuccess(() -> Component.literal("Dossier scanne: " + worldRoot), false);

        CompletableFuture
                .supplyAsync(() -> scanWorld(worldRoot, output, targets))
                .whenComplete((report, throwable) -> server.execute(() -> {
                    if (throwable != null) {
                        AsmpThingsMod.LOGGER.error("Erreur pendant le scan item debug", throwable);
                        source.sendFailure(Component.literal("Erreur pendant le scan item debug: " + throwable.getMessage()));
                        return;
                    }

                    source.sendSuccess(() -> Component.literal(
                            "Scan termine: " + report.totalItemCount + " item(s), "
                                    + report.totalStacksFound + " stack(s), fichier: " + report.output
                    ), true);

                    if (report.errorCount > 0) {
                        source.sendSuccess(() -> Component.literal(report.errorCount + " erreur(s) de lecture ignoree(s), details dans le JSON."), false);
                    }
                }));

        return 1;
    }

    private static CompletableFuture<Suggestions> suggestItems(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        String remaining = builder.getRemaining();
        int lastSeparator = Math.max(remaining.lastIndexOf(' '), Math.max(remaining.lastIndexOf(','), remaining.lastIndexOf(';')));
        String prefix = lastSeparator >= 0 ? remaining.substring(0, lastSeparator + 1) : "";
        String current = lastSeparator >= 0 ? remaining.substring(lastSeparator + 1) : remaining;

        List<String> suggestions = BuiltInRegistries.ITEM.keySet().stream()
                .map(ResourceLocation::toString)
                .filter(id -> id.startsWith(current))
                .sorted()
                .limit(80)
                .map(id -> prefix + id)
                .toList();

        return SharedSuggestionProvider.suggest(suggestions, builder);
    }

    private static Set<String> parseTargets(String rawTargets) {
        Set<String> targets = new HashSet<>();

        for (String token : SPLIT_TARGETS.split(rawTargets.trim())) {
            if (token.isBlank()) {
                continue;
            }

            ResourceLocation id = ResourceLocation.tryParse(token);

            if (id != null) {
                targets.add(id.toString());
            }
        }

        return targets;
    }

    private static ScanReport scanWorld(Path worldRoot, Path output, Set<String> targets) {
        ScanState state = new ScanState(worldRoot, output, targets);

        try (Stream<Path> paths = Files.walk(worldRoot, FileVisitOption.FOLLOW_LINKS)) {
            paths
                    .filter(Files::isRegularFile)
                    .sorted(Comparator.comparing(path -> worldRoot.relativize(path).toString()))
                    .forEach(path -> scanFile(path, state));
        } catch (IOException exception) {
            state.addError(worldRoot, "Impossible de parcourir le dossier world: " + exception.getMessage());
        }

        try {
            Files.writeString(output, state.toJson(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            state.addError(output, "Impossible d'ecrire le JSON: " + exception.getMessage());
        }

        return new ScanReport(output, state.totalStacksFound, state.totalItemCount, state.errorCount);
    }

    private static void scanFile(Path file, ScanState state) {
        String name = file.getFileName().toString();
        String lowerName = name.toLowerCase(Locale.ROOT);

        if (lowerName.startsWith("asmp_item_debug_") && lowerName.endsWith(".json")) {
            return;
        }

        try {
            if (lowerName.endsWith(".mca")) {
                scanRegionFile(file, state);
                return;
            }

            if (lowerName.endsWith(".mcc")) {
                return;
            }

            if (lowerName.endsWith(".dat") || lowerName.endsWith(".dat_old") || lowerName.endsWith(".nbt")) {
                scanNbtFile(file, state);
            }
        } catch (Exception exception) {
            state.addError(file, exception.getClass().getSimpleName() + ": " + exception.getMessage());
        }
    }

    private static void scanNbtFile(Path file, ScanState state) {
        Optional<CompoundTag> tag = readNbtFile(file, state);

        if (tag.isEmpty()) {
            return;
        }

        state.scannedFiles++;
        ScanContext context = ScanContext.forFile(state.worldRoot, file);
        scanTag(tag.get(), "root", context, state, 0);
    }

    private static Optional<CompoundTag> readNbtFile(Path file, ScanState state) {
        try {
            return Optional.ofNullable(NbtIo.readCompressed(file, NbtAccounter.unlimitedHeap()));
        } catch (IOException compressedException) {
            try {
                return Optional.ofNullable(NbtIo.read(file));
            } catch (IOException rawException) {
                state.addError(file, "NBT illisible: compressed=" + compressedException.getMessage() + ", raw=" + rawException.getMessage());
                return Optional.empty();
            }
        }
    }

    private static void scanRegionFile(Path file, ScanState state) throws IOException {
        Matcher matcher = REGION_FILE.matcher(file.getFileName().toString());

        if (!matcher.matches()) {
            return;
        }

        int regionX = Integer.parseInt(matcher.group(1));
        int regionZ = Integer.parseInt(matcher.group(2));
        RegionStorageInfo info = new RegionStorageInfo("asmp_item_debug", Level.OVERWORLD, inferRegionType(file));

        state.scannedFiles++;

        try (RegionFile region = new RegionFile(info, file, file.getParent(), false)) {
            for (int localX = 0; localX < 32; localX++) {
                for (int localZ = 0; localZ < 32; localZ++) {
                    ChunkPos chunkPos = new ChunkPos(regionX * 32 + localX, regionZ * 32 + localZ);

                    if (!region.hasChunk(chunkPos)) {
                        continue;
                    }

                    try (DataInputStream input = region.getChunkDataInputStream(chunkPos)) {
                        if (input == null) {
                            continue;
                        }

                        CompoundTag chunkTag = NbtIo.read(input, NbtAccounter.unlimitedHeap());

                        if (chunkTag == null) {
                            continue;
                        }

                        state.scannedChunks++;
                        ScanContext context = ScanContext.forFile(state.worldRoot, file).withChunk(chunkPos.x, chunkPos.z);
                        scanTag(chunkTag, "chunk[" + chunkPos.x + "," + chunkPos.z + "]", context, state, 0);
                    } catch (Exception exception) {
                        state.addError(file, "Chunk " + chunkPos.x + "," + chunkPos.z + ": " + exception.getMessage());
                    }
                }
            }
        }
    }

    private static String inferRegionType(Path file) {
        Path parent = file.getParent();

        if (parent == null) {
            return "region";
        }

        String folder = parent.getFileName().toString();

        if (folder.equals("entities") || folder.equals("poi") || folder.equals("region")) {
            return folder;
        }

        return "region";
    }

    private static void scanTag(Tag tag, String path, ScanContext context, ScanState state, int depth) {
        if (tag == null || depth > 512) {
            return;
        }

        if (tag instanceof CompoundTag compound) {
            scanCompound(compound, path, context, state, depth);
            return;
        }

        if (tag instanceof ListTag list) {
            for (int i = 0; i < list.size(); i++) {
                scanTag(list.get(i), path + "[" + i + "]", context, state, depth + 1);
            }
        }
    }

    private static void scanCompound(CompoundTag compound, String path, ScanContext context, ScanState state, int depth) {
        boolean itemStack = isLikelyItemStack(compound);
        ScanContext childContext = updateContext(compound, path, context, itemStack);

        if (itemStack) {
            String itemId = compound.getString("id");

            if (state.targets.contains(itemId)) {
                int count = readItemCount(compound);
                state.addMatch(new ItemMatch(
                        itemId,
                        count,
                        childContext.sourceType,
                        childContext.dimension,
                        childContext.relativeFile,
                        path,
                        childContext.chunkX,
                        childContext.chunkZ,
                        childContext.position,
                        childContext.containerId,
                        context.holderItem,
                        childContext.owner,
                        childContext.backpackUuid,
                        readSlot(compound),
                        trimPreview(compound.toString())
                ));
            }

            childContext = childContext.withHolderItem(itemId);
        }

        for (String key : compound.getAllKeys()) {
            Tag child = compound.get(key);
            ScanContext keyContext = childContext.withBackpackUuid(findUuidInKey(key, childContext.backpackUuid));
            scanTag(child, path + "." + escapePathKey(key), keyContext, state, depth + 1);
        }
    }

    private static ScanContext updateContext(CompoundTag compound, String path, ScanContext context, boolean itemStack) {
        ScanContext updated = context;
        Position position = readPosition(compound);

        if (position != null) {
            updated = updated.withPosition(position);
        }

        String uuid = readAnyUuid(compound);

        if (uuid != null) {
            updated = updated.withBackpackUuid(uuid);
        }

        if (!itemStack && compound.contains("id", Tag.TAG_STRING)) {
            String id = compound.getString("id");

            if (!id.isBlank()) {
                updated = updated.withContainerId(id);
            }
        }

        if (path.contains(".Inventory") || path.contains(".EnderItems")) {
            updated = updated.withContainerId("player_inventory");
        }

        if (updated.relativeFile.endsWith("sophisticatedbackpacks.dat") && updated.containerId == null) {
            updated = updated.withContainerId("sophisticatedbackpacks:data");
        }

        return updated;
    }

    private static boolean isLikelyItemStack(CompoundTag compound) {
        if (!compound.contains("id", Tag.TAG_STRING)) {
            return false;
        }

        String id = compound.getString("id");

        if (ResourceLocation.tryParse(id) == null) {
            return false;
        }

        return hasNumeric(compound, "count")
                || hasNumeric(compound, "Count")
                || hasNumeric(compound, "amount")
                || hasNumeric(compound, "Amount")
                || compound.contains("components", Tag.TAG_COMPOUND)
                || compound.contains("tag", Tag.TAG_COMPOUND)
                || compound.contains("Slot")
                || compound.contains("slot");
    }

    private static int readItemCount(CompoundTag compound) {
        int count = readNumeric(compound, "count", Integer.MIN_VALUE);

        if (count != Integer.MIN_VALUE) {
            return Math.max(1, count);
        }

        count = readNumeric(compound, "Count", Integer.MIN_VALUE);

        if (count != Integer.MIN_VALUE) {
            return Math.max(1, count);
        }

        count = readNumeric(compound, "amount", Integer.MIN_VALUE);

        if (count != Integer.MIN_VALUE) {
            return Math.max(1, count);
        }

        count = readNumeric(compound, "Amount", Integer.MIN_VALUE);

        if (count != Integer.MIN_VALUE) {
            return Math.max(1, count);
        }

        return 1;
    }

    private static Integer readSlot(CompoundTag compound) {
        int slot = readNumeric(compound, "Slot", Integer.MIN_VALUE);

        if (slot != Integer.MIN_VALUE) {
            return slot;
        }

        slot = readNumeric(compound, "slot", Integer.MIN_VALUE);

        if (slot != Integer.MIN_VALUE) {
            return slot;
        }

        return null;
    }

    private static boolean hasNumeric(CompoundTag compound, String key) {
        return compound.get(key) instanceof NumericTag;
    }

    private static int readNumeric(CompoundTag compound, String key, int fallback) {
        Tag tag = compound.get(key);

        if (tag instanceof NumericTag numericTag) {
            return numericTag.getAsInt();
        }

        return fallback;
    }

    private static Position readPosition(CompoundTag compound) {
        if (hasNumeric(compound, "x") && hasNumeric(compound, "y") && hasNumeric(compound, "z")) {
            return new Position(
                    ((NumericTag) compound.get("x")).getAsDouble(),
                    ((NumericTag) compound.get("y")).getAsDouble(),
                    ((NumericTag) compound.get("z")).getAsDouble()
            );
        }

        if (hasNumeric(compound, "X") && hasNumeric(compound, "Y") && hasNumeric(compound, "Z")) {
            return new Position(
                    ((NumericTag) compound.get("X")).getAsDouble(),
                    ((NumericTag) compound.get("Y")).getAsDouble(),
                    ((NumericTag) compound.get("Z")).getAsDouble()
            );
        }

        Tag posTag = compound.get("Pos");

        if (posTag instanceof ListTag pos) {
            if (pos.size() >= 3) {
                Tag x = pos.get(0);
                Tag y = pos.get(1);
                Tag z = pos.get(2);

                if (x instanceof NumericTag numericX && y instanceof NumericTag numericY && z instanceof NumericTag numericZ) {
                    return new Position(numericX.getAsDouble(), numericY.getAsDouble(), numericZ.getAsDouble());
                }
            }
        }

        return null;
    }

    private static String readAnyUuid(CompoundTag compound) {
        for (String key : List.of("uuid", "UUID", "Uuid", "backpackUuid", "backpackUUID", "BackpackUuid", "backpack_uuid")) {
            if (compound.contains(key, Tag.TAG_STRING)) {
                String value = compound.getString(key);

                if (UUID_TEXT.matcher(value).matches()) {
                    return value;
                }
            }

            if (compound.hasUUID(key)) {
                return compound.getUUID(key).toString();
            }
        }

        return null;
    }

    private static String findUuidInKey(String key, String fallback) {
        if (UUID_TEXT.matcher(key).matches()) {
            return key;
        }

        return fallback;
    }

    private static String escapePathKey(String key) {
        if (key.matches("[A-Za-z0-9_:-]+")) {
            return key;
        }

        return "\"" + key.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    private static String trimPreview(String text) {
        if (text.length() <= MAX_STACK_PREVIEW_LENGTH) {
            return text;
        }

        return text.substring(0, MAX_STACK_PREVIEW_LENGTH) + "...";
    }

    private static final class ScanState {
        private final Path worldRoot;
        private final Path output;
        private final Set<String> targets;
        private final List<ItemMatch> matches = new ArrayList<>();
        private final List<String> errors = new ArrayList<>();
        private final Map<String, Integer> stacksByItem = new HashMap<>();
        private final Map<String, Integer> countsByItem = new HashMap<>();
        private int scannedFiles = 0;
        private int scannedChunks = 0;
        private int totalStacksFound = 0;
        private int totalItemCount = 0;
        private int errorCount = 0;

        private ScanState(Path worldRoot, Path output, Set<String> targets) {
            this.worldRoot = worldRoot;
            this.output = output;
            this.targets = targets;
        }

        private void addMatch(ItemMatch match) {
            matches.add(match);
            totalStacksFound++;
            totalItemCount += match.count;
            stacksByItem.merge(match.itemId, 1, Integer::sum);
            countsByItem.merge(match.itemId, match.count, Integer::sum);
        }

        private void addError(Path file, String message) {
            errorCount++;

            if (errors.size() < MAX_REPORTED_ERRORS) {
                errors.add(relative(file) + " -> " + message);
            }
        }

        private String relative(Path file) {
            Path normalized = file.normalize();

            if (normalized.startsWith(worldRoot)) {
                return worldRoot.relativize(normalized).toString().replace('\\', '/');
            }

            return normalized.toString().replace('\\', '/');
        }

        private String toJson() {
            StringBuilder json = new StringBuilder(4096 + matches.size() * 512);
            List<String> sortedTargets = targets.stream().sorted().toList();
            List<String> sortedItems = countsByItem.keySet().stream().sorted().toList();

            json.append("{\n");
            appendField(json, 1, "generatedAt", LocalDateTime.now().toString()).append(",\n");
            appendField(json, 1, "world", worldRoot.toString()).append(",\n");
            appendStringArrayField(json, 1, "targets", sortedTargets).append(",\n");
            appendNumberField(json, 1, "totalStacksFound", totalStacksFound).append(",\n");
            appendNumberField(json, 1, "totalItemCount", totalItemCount).append(",\n");

            indent(json, 1).append("\"totalsByItem\": {\n");

            for (int i = 0; i < sortedItems.size(); i++) {
                String item = sortedItems.get(i);
                indent(json, 2).append(jsonString(item)).append(": {");
                json.append("\"stacks\": ").append(stacksByItem.getOrDefault(item, 0)).append(", ");
                json.append("\"count\": ").append(countsByItem.getOrDefault(item, 0)).append("}");

                if (i + 1 < sortedItems.size()) {
                    json.append(",");
                }

                json.append("\n");
            }

            indent(json, 1).append("},\n");
            indent(json, 1).append("\"scanned\": {\n");
            appendNumberField(json, 2, "files", scannedFiles).append(",\n");
            appendNumberField(json, 2, "chunks", scannedChunks).append(",\n");
            appendNumberField(json, 2, "errors", errorCount).append("\n");
            indent(json, 1).append("},\n");
            appendStringArrayField(json, 1, "readErrors", errors).append(",\n");
            indent(json, 1).append("\"results\": [\n");

            for (int i = 0; i < matches.size(); i++) {
                matches.get(i).appendJson(json, 2);

                if (i + 1 < matches.size()) {
                    json.append(",");
                }

                json.append("\n");
            }

            indent(json, 1).append("]\n");
            json.append("}\n");
            return json.toString();
        }
    }

    private record ScanReport(Path output, int totalStacksFound, int totalItemCount, int errorCount) {
    }

    private record ScanContext(
            String relativeFile,
            String sourceType,
            String dimension,
            Integer chunkX,
            Integer chunkZ,
            Position position,
            String containerId,
            String holderItem,
            String owner,
            String backpackUuid
    ) {
        private static ScanContext forFile(Path worldRoot, Path file) {
            String relative = worldRoot.relativize(file.normalize()).toString().replace('\\', '/');
            return new ScanContext(
                    relative,
                    inferSourceType(relative),
                    inferDimension(relative),
                    null,
                    null,
                    null,
                    null,
                    null,
                    inferOwner(relative),
                    null
            );
        }

        private ScanContext withChunk(int x, int z) {
            return new ScanContext(relativeFile, sourceType, dimension, x, z, position, containerId, holderItem, owner, backpackUuid);
        }

        private ScanContext withPosition(Position position) {
            return new ScanContext(relativeFile, sourceType, dimension, chunkX, chunkZ, position, containerId, holderItem, owner, backpackUuid);
        }

        private ScanContext withContainerId(String containerId) {
            return new ScanContext(relativeFile, sourceType, dimension, chunkX, chunkZ, position, containerId, holderItem, owner, backpackUuid);
        }

        private ScanContext withHolderItem(String holderItem) {
            return new ScanContext(relativeFile, sourceType, dimension, chunkX, chunkZ, position, containerId, holderItem, owner, backpackUuid);
        }

        private ScanContext withBackpackUuid(String backpackUuid) {
            return new ScanContext(relativeFile, sourceType, dimension, chunkX, chunkZ, position, containerId, holderItem, owner, backpackUuid);
        }
    }

    private record Position(double x, double y, double z) {
        private void appendJson(StringBuilder json, int depth) {
            indent(json, depth).append("\"pos\": {");
            json.append("\"x\": ").append(formatNumber(x)).append(", ");
            json.append("\"y\": ").append(formatNumber(y)).append(", ");
            json.append("\"z\": ").append(formatNumber(z)).append("}");
        }
    }

    private record ItemMatch(
            String itemId,
            int count,
            String sourceType,
            String dimension,
            String file,
            String nbtPath,
            Integer chunkX,
            Integer chunkZ,
            Position position,
            String containerId,
            String holderItem,
            String owner,
            String backpackUuid,
            Integer slot,
            String stackPreview
    ) {
        private void appendJson(StringBuilder json, int depth) {
            indent(json, depth).append("{\n");
            appendField(json, depth + 1, "item", itemId).append(",\n");
            appendNumberField(json, depth + 1, "count", count).append(",\n");
            appendField(json, depth + 1, "sourceType", sourceType).append(",\n");
            appendField(json, depth + 1, "dimension", dimension).append(",\n");
            appendField(json, depth + 1, "file", file).append(",\n");
            appendField(json, depth + 1, "nbtPath", nbtPath);

            if (chunkX != null && chunkZ != null) {
                json.append(",\n");
                indent(json, depth + 1).append("\"chunk\": {\"x\": ").append(chunkX).append(", \"z\": ").append(chunkZ).append("}");
            }

            if (position != null) {
                json.append(",\n");
                position.appendJson(json, depth + 1);
            }

            appendNullableField(json, depth + 1, "container", containerId);
            appendNullableField(json, depth + 1, "insideItem", holderItem);
            appendNullableField(json, depth + 1, "owner", owner);
            appendNullableField(json, depth + 1, "backpackUuid", backpackUuid);

            if (slot != null) {
                json.append(",\n");
                appendNumberField(json, depth + 1, "slot", slot);
            }

            json.append(",\n");
            appendField(json, depth + 1, "stackNbtPreview", stackPreview).append("\n");
            indent(json, depth).append("}");
        }
    }

    private static String inferSourceType(String relative) {
        if (relative.startsWith("playerdata/")) {
            return "playerdata";
        }

        if (relative.endsWith("data/sophisticatedbackpacks.dat") || relative.equals("data/sophisticatedbackpacks.dat")) {
            return "sophisticated_backpacks_data";
        }

        if (relative.startsWith("data/") || relative.contains("/data/")) {
            return "world_data";
        }

        if (relative.startsWith("entities/") || relative.contains("/entities/")) {
            return "entity_region";
        }

        if (relative.startsWith("region/") || relative.contains("/region/")) {
            return "block_region";
        }

        if (relative.startsWith("poi/") || relative.contains("/poi/")) {
            return "poi_region";
        }

        return "nbt_file";
    }

    private static String inferDimension(String relative) {
        if (relative.startsWith("DIM-1/")) {
            return "minecraft:the_nether";
        }

        if (relative.startsWith("DIM1/")) {
            return "minecraft:the_end";
        }

        if (relative.startsWith("dimensions/")) {
            String[] parts = relative.split("/");

            if (parts.length >= 3) {
                return parts[1] + ":" + parts[2];
            }
        }

        if (relative.startsWith("playerdata/") || relative.startsWith("data/")) {
            return "global";
        }

        return "minecraft:overworld";
    }

    private static String inferOwner(String relative) {
        if (!relative.startsWith("playerdata/")) {
            return null;
        }

        String name = Path.of(relative).getFileName().toString();

        if (name.endsWith(".dat")) {
            String uuid = name.substring(0, name.length() - 4);

            if (UUID_TEXT.matcher(uuid).matches()) {
                return uuid;
            }
        }

        return null;
    }

    private static StringBuilder appendField(StringBuilder json, int depth, String name, String value) {
        indent(json, depth).append(jsonString(name)).append(": ").append(jsonString(value));
        return json;
    }

    private static StringBuilder appendNullableField(StringBuilder json, int depth, String name, String value) {
        if (value == null || value.isBlank()) {
            return json;
        }

        json.append(",\n");
        appendField(json, depth, name, value);
        return json;
    }

    private static StringBuilder appendNumberField(StringBuilder json, int depth, String name, Number value) {
        indent(json, depth).append(jsonString(name)).append(": ").append(value);
        return json;
    }

    private static StringBuilder appendStringArrayField(StringBuilder json, int depth, String name, List<String> values) {
        indent(json, depth).append(jsonString(name)).append(": [");

        for (int i = 0; i < values.size(); i++) {
            json.append(jsonString(values.get(i)));

            if (i + 1 < values.size()) {
                json.append(", ");
            }
        }

        json.append("]");
        return json;
    }

    private static StringBuilder indent(StringBuilder json, int depth) {
        return json.append("  ".repeat(Math.max(0, depth)));
    }

    private static String jsonString(String value) {
        if (value == null) {
            return "null";
        }

        StringBuilder escaped = new StringBuilder(value.length() + 2);
        escaped.append('"');

        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);

            switch (c) {
                case '"' -> escaped.append("\\\"");
                case '\\' -> escaped.append("\\\\");
                case '\b' -> escaped.append("\\b");
                case '\f' -> escaped.append("\\f");
                case '\n' -> escaped.append("\\n");
                case '\r' -> escaped.append("\\r");
                case '\t' -> escaped.append("\\t");
                default -> {
                    if (c < 0x20) {
                        escaped.append(String.format("\\u%04x", (int) c));
                    } else {
                        escaped.append(c);
                    }
                }
            }
        }

        escaped.append('"');
        return escaped.toString();
    }

    private static String formatNumber(double value) {
        if (Math.rint(value) == value) {
            return Long.toString((long) value);
        }

        return Double.toString(value);
    }
}
