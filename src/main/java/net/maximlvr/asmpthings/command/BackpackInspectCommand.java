package net.maximlvr.asmpthings.command;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.maximlvr.asmpthings.AsmpThingsMod;
import net.maximlvr.asmpthings.integration.sophisticated.BackpackInspectionInventory;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.AccessLogRecord;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackAccessLogger;
import net.p3pp3rf1y.sophisticatedbackpacks.common.gui.BackpackContainer;
import net.p3pp3rf1y.sophisticatedbackpacks.common.gui.BackpackContext;
import net.p3pp3rf1y.sophisticatedcore.common.gui.SophisticatedMenuProvider;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class BackpackInspectCommand {
    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("asmpbackpack")
                .requires(source -> source.hasPermission(2))
                .executes(context -> listBackpacks(context.getSource(), Optional.empty()))
                .then(Commands.literal("list")
                        .executes(context -> listBackpacks(context.getSource(), Optional.empty()))
                        .then(Commands.argument("joueur", StringArgumentType.word())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(BackpackAccessLogger.getPlayerNames(), builder))
                                .executes(context -> listBackpacks(
                                        context.getSource(),
                                        Optional.of(StringArgumentType.getString(context, "joueur"))
                                ))))
                .then(Commands.argument("uuid", StringArgumentType.word())
                        .suggests(BackpackInspectCommand::suggestBackpackUuids)
                        .executes(context -> openBackpack(
                                context.getSource(),
                                StringArgumentType.getString(context, "uuid")
                        ))));
    }

    private static int openBackpack(CommandSourceStack source, String uuidText) throws CommandSyntaxException {
        UUID backpackUuid;

        try {
            backpackUuid = UUID.fromString(uuidText);
        } catch (IllegalArgumentException exception) {
            source.sendFailure(Component.literal("UUID de backpack invalide: " + uuidText));
            return 0;
        }

        Optional<AccessLogRecord> backpackLog = BackpackAccessLogger.getBackpackLog(backpackUuid);

        if (backpackLog.isEmpty()) {
            source.sendFailure(Component.literal("Aucun backpack connu avec l'UUID " + backpackUuid + "."));
            return 0;
        }

        ServerPlayer player = source.getPlayerOrException();
        AccessLogRecord record = backpackLog.get();
        BackpackContext context = new BackpackContext.Item(
                BackpackInspectionInventory.HANDLER_NAME,
                BackpackInspectionInventory.encode(record),
                0
        );

        player.openMenu(new SophisticatedMenuProvider(
                (containerId, inventory, openingPlayer) -> new BackpackContainer(containerId, openingPlayer, context),
                getBackpackTitle(record),
                false
        ), context::toBuffer);

        source.sendSuccess(() -> Component.literal("Backpack ouvert: " + backpackUuid), true);
        return 1;
    }

    private static CompletableFuture<Suggestions> suggestBackpackUuids(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        BackpackAccessLogger.getAllBackpackLogs().stream()
                .sorted(Comparator.comparing(AccessLogRecord::getPlayerName).thenComparing(record -> record.getBackpackUuid().toString()))
                .forEach(record -> builder.suggest(record.getBackpackUuid().toString(), Component.literal(getSuggestionLabel(record))));

        return builder.buildFuture();
    }

    private static int listBackpacks(CommandSourceStack source, Optional<String> playerName) {
        List<AccessLogRecord> backpackLogs = playerName
                .map(BackpackInspectCommand::resolvePlayerName)
                .map(BackpackAccessLogger::getBackpackLogsForPlayer)
                .map(ArrayList::new)
                .orElseGet(() -> new ArrayList<>(BackpackAccessLogger.getAllBackpackLogs()));

        if (backpackLogs.isEmpty()) {
            String suffix = playerName.map(name -> " pour " + name).orElse("");
            source.sendFailure(Component.literal("Aucun backpack connu" + suffix + "."));
            return 0;
        }

        backpackLogs.sort(Comparator.comparingLong(AccessLogRecord::getAccessTime).reversed());
        source.sendSuccess(() -> Component.literal(backpackLogs.size() + " backpack(s) connu(s) :"), false);
        backpackLogs.forEach(record -> source.sendSuccess(() -> Component.literal(formatListLine(record)), false));

        return backpackLogs.size();
    }

    private static String resolvePlayerName(String playerName) {
        for (String knownPlayerName : BackpackAccessLogger.getPlayerNames()) {
            if (knownPlayerName.equalsIgnoreCase(playerName)) {
                return knownPlayerName;
            }
        }

        return playerName;
    }

    private static Component getBackpackTitle(AccessLogRecord record) {
        if (record.getBackpackName().isBlank()) {
            return Component.literal("Backpack " + record.getBackpackUuid());
        }

        return Component.literal(record.getBackpackName());
    }

    private static String getSuggestionLabel(AccessLogRecord record) {
        String backpackName = record.getBackpackName().isBlank() ? "sans nom" : record.getBackpackName();
        return record.getPlayerName() + " - " + backpackName;
    }

    private static String formatListLine(AccessLogRecord record) {
        return record.getBackpackUuid()
                + " - " + record.getPlayerName()
                + " - " + (record.getBackpackName().isBlank() ? "sans nom" : record.getBackpackName())
                + " - " + record.getBackpackItemRegistryName();
    }
}
