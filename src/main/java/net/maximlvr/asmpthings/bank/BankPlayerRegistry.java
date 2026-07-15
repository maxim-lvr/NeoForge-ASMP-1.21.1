package net.maximlvr.asmpthings.bank;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.maximlvr.asmpthings.item.ModItems;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class BankPlayerRegistry {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = "asmp_bank.json";
    private final Path path;
    private final Map<UUID, Entry> players = new LinkedHashMap<>();

    private BankPlayerRegistry(Path path) {
        this.path = path;
    }

    public static BankPlayerRegistry get(MinecraftServer server) {
        BankPlayerRegistry registry = new BankPlayerRegistry(Path.of("config", FILE_NAME));
        registry.load();
        return registry;
    }

    public void rememberPlayer(ServerPlayer player) {
        Entry existing = players.get(player.getUUID());
        boolean displayed = existing == null || existing.isDisplayed();
        int crazyCoins = existing == null ? 0 : existing.crazyCoins();
        List<AccountInfo> accounts = existing == null ? List.of() : existing.accounts();
        players.put(player.getUUID(), new Entry(player.getUUID(), player.getGameProfile().getName(), displayed, crazyCoins, accounts));
        save();
    }

    public void syncPlayerBankInfo(ServerPlayer player, BankSavedData bank) {
        syncKnownBankAccounts(bank);
        Entry existing = players.get(player.getUUID());
        boolean displayed = existing == null || existing.isDisplayed();
        players.put(player.getUUID(), new Entry(
                player.getUUID(),
                player.getGameProfile().getName(),
                displayed,
                countCrazyCoins(player),
                accountInfosForPlayer(player.getUUID(), bank.getAccounts(player.getUUID()))
        ));
        save();
    }

    private void syncKnownBankAccounts(BankSavedData bank) {
        for (Map.Entry<UUID, Entry> playerEntry : new ArrayList<>(players.entrySet())) {
            Entry entry = playerEntry.getValue();
            players.put(playerEntry.getKey(), new Entry(
                    entry.id(),
                    entry.name(),
                    entry.isDisplayed(),
                    entry.crazyCoins(),
                    accountInfosForPlayer(entry.id(), bank.getAccounts(entry.id()))
            ));
        }
    }

    public boolean setDisplayed(String playerName, boolean displayed, MinecraftServer server) {
        Entry entry = findByName(playerName);

        if (entry == null) {
            ServerPlayer onlinePlayer = server.getPlayerList().getPlayerByName(playerName);

            if (onlinePlayer == null) {
                return false;
            }

            entry = new Entry(onlinePlayer.getUUID(), onlinePlayer.getGameProfile().getName(), true, 0, List.of());
        }

        players.put(entry.id(), new Entry(entry.id(), entry.name(), displayed, entry.crazyCoins(), entry.accounts()));
        save();
        return true;
    }

    public boolean isDisplayed(UUID playerId) {
        Entry entry = players.get(playerId);
        return entry != null && entry.isDisplayed();
    }

    public List<Entry> displayedPlayers() {
        List<Entry> entries = new ArrayList<>();

        for (Entry entry : players.values()) {
            if (entry.isDisplayed()) {
                entries.add(entry);
            }
        }

        entries.sort(Comparator.comparing(Entry::name).thenComparing(entry -> entry.id().toString()));
        return entries;
    }

    public Entry get(UUID playerId) {
        return players.get(playerId);
    }

    private Entry findByName(String playerName) {
        for (Entry entry : players.values()) {
            if (entry.name().equalsIgnoreCase(playerName)) {
                return entry;
            }
        }

        return null;
    }

    private void load() {
        players.clear();

        if (!Files.exists(path)) {
            save();
            return;
        }

        try (Reader reader = Files.newBufferedReader(path)) {
            Data data = GSON.fromJson(reader, Data.class);

            if (data == null || data.players == null) {
                return;
            }

            for (PlayerData player : data.players) {
                try {
                    UUID id = UUID.fromString(player.uuid);
                    players.put(id, new Entry(
                            id,
                            player.name == null ? "" : player.name,
                            player.isDisplayed,
                            Math.max(0, player.crazyCoins),
                            accountInfosFromJson(player.accounts)
                    ));
                } catch (IllegalArgumentException ignored) {
                }
            }
        } catch (IOException ignored) {
        }
    }

    private void save() {
        try {
            Files.createDirectories(path.getParent());
            Data data = new Data();
            data.players = new ArrayList<>();

            for (Entry entry : players.values()) {
                PlayerData player = new PlayerData();
                player.uuid = entry.id().toString();
                player.name = entry.name();
                player.isDisplayed = entry.isDisplayed();
                player.crazyCoins = entry.crazyCoins();
                player.accounts = new ArrayList<>();

                for (AccountInfo account : entry.accounts()) {
                    AccountData accountData = new AccountData();
                    accountData.id = account.id();
                    accountData.name = account.name();
                    accountData.balance = account.balance();
                    accountData.owner = account.owner();
                    accountData.common = account.common();
                    accountData.adminAccount = account.adminAccount();
                    player.accounts.add(accountData);
                }

                data.players.add(player);
            }

            data.players.sort(Comparator.comparing(player -> player.name));

            try (Writer writer = Files.newBufferedWriter(path)) {
                GSON.toJson(data, writer);
            }
        } catch (IOException ignored) {
        }
    }

    private int countCrazyCoins(ServerPlayer player) {
        int count = 0;

        for (ItemStack stack : player.getInventory().items) {
            if (stack.is(ModItems.CRAZY_COIN.get())) {
                count += stack.getCount();
            }
        }

        return count;
    }

    private List<AccountInfo> accountInfosForPlayer(UUID playerId, List<BankAccount> accounts) {
        List<AccountInfo> infos = new ArrayList<>();

        for (BankAccount account : accounts) {
            infos.add(new AccountInfo(
                    account.id(),
                    account.name(),
                    account.balance(),
                    account.owner().equals(playerId),
                    !account.owner().equals(playerId),
                    account.adminAccount()
            ));
        }

        infos.sort(Comparator.comparing(AccountInfo::name).thenComparing(AccountInfo::id));
        return infos;
    }

    private List<AccountInfo> accountInfosFromJson(List<AccountData> accounts) {
        List<AccountInfo> infos = new ArrayList<>();

        if (accounts == null) {
            return infos;
        }

        for (AccountData account : accounts) {
            if (account.id == null || account.id.length() != 4) {
                continue;
            }

            infos.add(new AccountInfo(
                    account.id,
                    account.name == null ? "" : account.name,
                    Math.max(0, account.balance),
                    account.owner,
                    account.common,
                    account.adminAccount
            ));
        }

        infos.sort(Comparator.comparing(AccountInfo::name).thenComparing(AccountInfo::id));
        return infos;
    }

    public record Entry(UUID id, String name, boolean isDisplayed, int crazyCoins, List<AccountInfo> accounts) {
        public Entry {
            accounts = List.copyOf(accounts == null ? List.of() : accounts);
        }
    }

    public record AccountInfo(String id, String name, int balance, boolean owner, boolean common, boolean adminAccount) {
    }

    private static class Data {
        List<PlayerData> players = new ArrayList<>();
    }

    private static class PlayerData {
        String uuid;
        String name;
        boolean isDisplayed = true;
        int crazyCoins;
        List<AccountData> accounts = new ArrayList<>();
    }

    private static class AccountData {
        String id;
        String name;
        int balance;
        boolean owner;
        boolean common;
        boolean adminAccount;
    }
}
