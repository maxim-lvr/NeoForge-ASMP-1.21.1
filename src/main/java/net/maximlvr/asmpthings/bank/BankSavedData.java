package net.maximlvr.asmpthings.bank;

import net.maximlvr.asmpthings.AsmpThingsMod;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class BankSavedData extends SavedData {
    private static final String NAME = AsmpThingsMod.MOD_ID + "_bank";
    private final Map<String, BankAccount> accounts = new LinkedHashMap<>();
    private final Map<UUID, List<SavedIban>> savedIbans = new LinkedHashMap<>();

    public static BankSavedData get(MinecraftServer server) {
        return server.getLevel(Level.OVERWORLD).getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(BankSavedData::new, BankSavedData::load),
                NAME
        );
    }

    public static BankSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        BankSavedData data = new BankSavedData();
        ListTag list = tag.getList("accounts", 10);

        for (int i = 0; i < list.size(); i++) {
            CompoundTag accountTag = list.getCompound(i);
            UUID owner;

            try {
                owner = accountTag.getUUID("owner");
            } catch (IllegalArgumentException exception) {
                continue;
            }

            String id = accountTag.getString("id");

            if (id.length() != 4) {
                continue;
            }

            data.accounts.put(id, new BankAccount(
                    id,
                    accountTag.getString("name"),
                    owner,
                    Math.max(0, accountTag.getInt("balance"))
            ));
        }

        ListTag savedIbanOwners = tag.getList("savedIbans", 10);

        for (int i = 0; i < savedIbanOwners.size(); i++) {
            CompoundTag ownerTag = savedIbanOwners.getCompound(i);
            UUID owner;

            try {
                owner = ownerTag.getUUID("owner");
            } catch (IllegalArgumentException exception) {
                continue;
            }

            List<SavedIban> entries = new ArrayList<>();
            ListTag entryTags = ownerTag.getList("entries", 10);

            for (int entryIndex = 0; entryIndex < entryTags.size(); entryIndex++) {
                CompoundTag entryTag = entryTags.getCompound(entryIndex);
                String iban = entryTag.getString("iban");

                if (iban.length() == 4) {
                    entries.add(new SavedIban(entryTag.getString("name"), iban));
                }
            }

            data.savedIbans.put(owner, entries);
        }

        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();

        for (BankAccount account : accounts.values()) {
            CompoundTag accountTag = new CompoundTag();
            accountTag.putString("id", account.id());
            accountTag.putString("name", account.name());
            accountTag.putUUID("owner", account.owner());
            accountTag.putInt("balance", account.balance());
            list.add(accountTag);
        }

        tag.put("accounts", list);

        ListTag savedIbanOwners = new ListTag();

        for (Map.Entry<UUID, List<SavedIban>> ownerEntry : savedIbans.entrySet()) {
            CompoundTag ownerTag = new CompoundTag();
            ownerTag.putUUID("owner", ownerEntry.getKey());

            ListTag entryTags = new ListTag();

            for (SavedIban savedIban : ownerEntry.getValue()) {
                CompoundTag entryTag = new CompoundTag();
                entryTag.putString("name", savedIban.name());
                entryTag.putString("iban", savedIban.iban());
                entryTags.add(entryTag);
            }

            ownerTag.put("entries", entryTags);
            savedIbanOwners.add(ownerTag);
        }

        tag.put("savedIbans", savedIbanOwners);
        return tag;
    }

    public List<BankAccount> getAccounts(UUID owner) {
        List<BankAccount> ownedAccounts = new ArrayList<>();

        for (BankAccount account : accounts.values()) {
            if (account.owner().equals(owner)) {
                ownedAccounts.add(account);
            }
        }

        ownedAccounts.sort(Comparator.comparing(BankAccount::name).thenComparing(BankAccount::id));
        return ownedAccounts;
    }

    public BankAccount getAccount(String id) {
        return accounts.get(id);
    }

    public List<SavedIban> getSavedIbans(UUID owner) {
        return new ArrayList<>(savedIbans.getOrDefault(owner, List.of()));
    }

    public void saveIban(UUID owner, String name, String iban) {
        List<SavedIban> entries = savedIbans.computeIfAbsent(owner, ignored -> new ArrayList<>());
        entries.removeIf(entry -> entry.name().equalsIgnoreCase(name) || entry.iban().equals(iban));
        entries.add(new SavedIban(name, iban));
        entries.sort(Comparator.comparing(SavedIban::name).thenComparing(SavedIban::iban));
        setDirty();
    }

    public BankAccount createAccount(UUID owner, String name, RandomSource random) {
        String id = generateId(random);
        BankAccount account = new BankAccount(id, name, owner, 0);
        accounts.put(id, account);
        setDirty();
        return account;
    }

    public boolean deposit(String id, int amount) {
        BankAccount account = accounts.get(id);

        if (account == null || amount <= 0) {
            return false;
        }

        accounts.put(id, account.withBalance(account.balance() + amount));
        setDirty();
        return true;
    }

    public boolean withdraw(String id, int amount) {
        BankAccount account = accounts.get(id);

        if (account == null || amount <= 0 || account.balance() < amount) {
            return false;
        }

        accounts.put(id, account.withBalance(account.balance() - amount));
        setDirty();
        return true;
    }

    public boolean transfer(String fromId, String toId, int amount) {
        BankAccount from = accounts.get(fromId);
        BankAccount to = accounts.get(toId);

        if (from == null || to == null || amount <= 0 || from.balance() < amount || fromId.equals(toId)) {
            return false;
        }

        accounts.put(fromId, from.withBalance(from.balance() - amount));
        accounts.put(toId, to.withBalance(to.balance() + amount));
        setDirty();
        return true;
    }

    private String generateId(RandomSource random) {
        for (int attempts = 0; attempts < 10000; attempts++) {
            String id = String.format("%04d", random.nextInt(10000));

            if (!accounts.containsKey(id)) {
                return id;
            }
        }

        throw new IllegalStateException("No bank account ids available");
    }
}
