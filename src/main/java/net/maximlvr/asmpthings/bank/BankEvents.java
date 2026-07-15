package net.maximlvr.asmpthings.bank;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.arguments.BoolArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.time.LocalDate;
import java.time.ZoneId;

public class BankEvents {
    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("asmpbank")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("create")
                        .then(Commands.argument("name", StringArgumentType.word())
                                .executes(context -> createAdminAccount(
                                        context.getSource(),
                                        StringArgumentType.getString(context, "name")
                                ))))
                .then(Commands.literal("isDisplayed")
                        .then(Commands.argument("player", StringArgumentType.word())
                                .then(Commands.argument("displayed", BoolArgumentType.bool())
                                        .executes(context -> setDisplayed(
                                                context.getSource(),
                                                StringArgumentType.getString(context, "player"),
                                                BoolArgumentType.getBool(context, "displayed")
                                        )))))
                .then(Commands.argument("account", StringArgumentType.word())
                        .then(Commands.literal("add")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(context -> addAdminMember(
                                                context.getSource(),
                                                StringArgumentType.getString(context, "account"),
                                                EntityArgument.getPlayer(context, "player")
                                        ))))));
    }

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        String day = LocalDate.now(ZoneId.systemDefault()).toString();
        BankPlayerRegistry playerRegistry = BankPlayerRegistry.get(player.server);
        playerRegistry.rememberPlayer(player);
        BankSavedData bank = BankSavedData.get(player.server);

        for (BankSavedData.SalaryPayment payment : bank.payDailySalary(player.getUUID(), day, playerRegistry)) {
            if (payment.paid()) {
                player.displayClientMessage(Component.literal("Salaire recu: " + payment.amount() + " crazycoin(s) de " + payment.accountName()), false);
            } else {
                player.displayClientMessage(Component.literal("Compte en banque de la ville vide."), false);
            }
        }

        playerRegistry.syncPlayerBankInfo(player, bank);
    }

    private static int createAdminAccount(net.minecraft.commands.CommandSourceStack source, String name) {
        BankSavedData bank = BankSavedData.get(source.getServer());

        if (bank.getAdminAccountByName(name) != null) {
            source.sendFailure(Component.literal("Un compte admin existe deja avec ce nom."));
            return 0;
        }

        BankAccount account = bank.createAdminAccount(name, source.getServer().overworld().getRandom());
        source.sendSuccess(() -> Component.literal("Compte admin " + account.name() + " cree. IBAN: " + account.id()), false);
        return 1;
    }

    private static int addAdminMember(net.minecraft.commands.CommandSourceStack source, String accountName, ServerPlayer player) {
        BankSavedData bank = BankSavedData.get(source.getServer());
        BankAccount account = bank.getAdminAccountByName(accountName);

        if (account == null) {
            source.sendFailure(Component.literal("Compte admin introuvable."));
            return 0;
        }

        bank.addAdminMember(account.id(), player.getUUID(), player.getGameProfile().getName());
        source.sendSuccess(() -> Component.literal(player.getGameProfile().getName() + " ajoute au compte " + account.name() + "."), false);
        return 1;
    }

    private static int setDisplayed(net.minecraft.commands.CommandSourceStack source, String playerName, boolean displayed) {
        BankPlayerRegistry registry = BankPlayerRegistry.get(source.getServer());

        if (!registry.setDisplayed(playerName, displayed, source.getServer())) {
            source.sendFailure(Component.literal("Joueur introuvable dans la banque."));
            return 0;
        }

        source.sendSuccess(() -> Component.literal(playerName + " isDisplayed = " + displayed), false);
        return 1;
    }
}
