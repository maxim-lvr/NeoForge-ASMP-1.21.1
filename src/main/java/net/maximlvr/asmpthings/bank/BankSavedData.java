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

            List<BankMember> members = new ArrayList<>();
            ListTag memberTags = accountTag.getList("members", 10);

            for (int memberIndex = 0; memberIndex < memberTags.size(); memberIndex++) {
                CompoundTag memberTag = memberTags.getCompound(memberIndex);

                try {
                    members.add(new BankMember(memberTag.getUUID("id"), memberTag.getString("name")));
                } catch (IllegalArgumentException ignored) {
                }
            }

            List<BankCitizen> citizens = new ArrayList<>();
            ListTag citizenTags = accountTag.getList("citizens", 10);

            for (int citizenIndex = 0; citizenIndex < citizenTags.size(); citizenIndex++) {
                CompoundTag citizenTag = citizenTags.getCompound(citizenIndex);
                String iban = citizenTag.getString("iban");
                UUID playerId;

                try {
                    playerId = citizenTag.getUUID("playerId");
                } catch (RuntimeException exception) {
                    continue;
                }

                if (iban.isEmpty() || iban.length() == 4) {
                    citizens.add(new BankCitizen(
                            playerId,
                            citizenTag.getString("name"),
                            iban,
                            Math.max(0, citizenTag.getInt("salary")),
                            citizenTag.getString("lastPaidDay")
                    ));
                }
            }

            data.accounts.put(id, new BankAccount(
                    id,
                    accountTag.getString("name"),
                    owner,
                    Math.max(0, accountTag.getInt("balance")),
                    members,
                    accountTag.getBoolean("adminAccount"),
                    citizens
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
            accountTag.putBoolean("adminAccount", account.adminAccount());

            ListTag memberTags = new ListTag();

            for (BankMember member : account.members()) {
                CompoundTag memberTag = new CompoundTag();
                memberTag.putUUID("id", member.id());
                memberTag.putString("name", member.name());
                memberTags.add(memberTag);
            }

            accountTag.put("members", memberTags);

            ListTag citizenTags = new ListTag();

            for (BankCitizen citizen : account.citizens()) {
                CompoundTag citizenTag = new CompoundTag();
                citizenTag.putUUID("playerId", citizen.playerId());
                citizenTag.putString("name", citizen.name());
                citizenTag.putString("iban", citizen.iban());
                citizenTag.putInt("salary", citizen.salary());
                citizenTag.putString("lastPaidDay", citizen.lastPaidDay());
                citizenTags.add(citizenTag);
            }

            accountTag.put("citizens", citizenTags);
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

    public List<BankAccount> getAccounts(UUID playerId) {
        List<BankAccount> accessibleAccounts = new ArrayList<>();

        for (BankAccount account : accounts.values()) {
            if (account.hasAccess(playerId)) {
                accessibleAccounts.add(account);
            }
        }

        accessibleAccounts.sort(Comparator.comparing(BankAccount::name).thenComparing(BankAccount::id));
        return accessibleAccounts;
    }

    public List<BankAccount> getOwnedAccounts(UUID owner) {
        List<BankAccount> ownedAccounts = new ArrayList<>();

        for (BankAccount account : accounts.values()) {
            if (account.owner().equals(owner)) {
                ownedAccounts.add(account);
            }
        }

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

    public BankAccount createAdminAccount(String name, RandomSource random) {
        String id = generateId(random);
        BankAccount account = new BankAccount(id, name, new UUID(0L, 0L), 0, List.of(), true, List.of());
        accounts.put(id, account);
        setDirty();
        return account;
    }

    public BankAccount getAdminAccountByName(String name) {
        for (BankAccount account : accounts.values()) {
            if (account.adminAccount() && account.name().equalsIgnoreCase(name)) {
                return account;
            }
        }

        return null;
    }

    public boolean addMember(String id, UUID owner, UUID memberId, String memberName) {
        BankAccount account = accounts.get(id);

        if (account == null || !account.owner().equals(owner) || account.owner().equals(memberId)) {
            return false;
        }

        List<BankMember> members = new ArrayList<>(account.members());
        members.removeIf(member -> member.id().equals(memberId));
        members.add(new BankMember(memberId, memberName));
        members.sort(Comparator.comparing(BankMember::name).thenComparing(member -> member.id().toString()));
        accounts.put(id, account.withMembers(members));
        setDirty();
        return true;
    }

    public boolean addAdminMember(String id, UUID memberId, String memberName) {
        BankAccount account = accounts.get(id);

        if (account == null || !account.adminAccount()) {
            return false;
        }

        List<BankMember> members = new ArrayList<>(account.members());
        members.removeIf(member -> member.id().equals(memberId));
        members.add(new BankMember(memberId, memberName));
        members.sort(Comparator.comparing(BankMember::name).thenComparing(member -> member.id().toString()));
        accounts.put(id, account.withMembers(members));
        setDirty();
        return true;
    }

    public boolean saveCitizen(String id, UUID playerId, String name, String iban, int salary) {
        BankAccount account = accounts.get(id);

        if (account == null || !account.adminAccount() || playerId.equals(new UUID(0L, 0L)) || salary < 0) {
            return false;
        }

        if (!iban.isEmpty() && (iban.length() != 4 || getAccount(iban) == null)) {
            return false;
        }

        List<BankCitizen> citizens = new ArrayList<>(account.citizens());
        citizens.removeIf(citizen -> citizen.playerId().equals(playerId));
        citizens.add(new BankCitizen(playerId, name, iban, salary, ""));
        citizens.sort(Comparator.comparing(BankCitizen::name).thenComparing(BankCitizen::iban));
        accounts.put(id, account.withCitizens(citizens));
        setDirty();
        return true;
    }

    public List<SalaryPayment> payDailySalary(UUID playerId, String day, BankPlayerRegistry playerRegistry) {
        List<SalaryPayment> payments = new ArrayList<>();

        if (!playerRegistry.isDisplayed(playerId)) {
            return payments;
        }

        for (BankAccount account : new ArrayList<>(accounts.values())) {
            if (!account.adminAccount()) {
                continue;
            }

            List<BankCitizen> citizens = new ArrayList<>(account.citizens());
            boolean changed = false;

            for (int i = 0; i < citizens.size(); i++) {
                BankCitizen citizen = citizens.get(i);

                if (!citizen.playerId().equals(playerId) || citizen.lastPaidDay().equals(day)) {
                    continue;
                }

                BankPlayerRegistry.Entry playerEntry = playerRegistry.get(playerId);
                if (playerEntry != null && !citizen.name().equals(playerEntry.name())) {
                    citizen = citizen.withName(playerEntry.name());
                    citizens.set(i, citizen);
                    changed = true;
                }

                if (citizen.salary() == 0) {
                    citizens.set(i, citizen.withLastPaidDay(day));
                    changed = true;
                    payments.add(new SalaryPayment(account.name(), 0, true));
                    continue;
                }

                BankAccount target = accounts.get(citizen.iban());
                BankAccount source = accounts.get(account.id());

                if (target == null || source == null || source.balance() < citizen.salary()) {
                    citizens.set(i, citizen.withLastPaidDay(day));
                    changed = true;
                    payments.add(new SalaryPayment(account.name(), citizen.salary(), false));
                    continue;
                }

                accounts.put(account.id(), source.withBalance(source.balance() - citizen.salary()));
                accounts.put(target.id(), target.withBalance(target.balance() + citizen.salary()));
                citizens.set(i, citizen.withLastPaidDay(day));
                changed = true;
                payments.add(new SalaryPayment(account.name(), citizen.salary(), true));
            }

            if (changed) {
                BankAccount updated = accounts.get(account.id());
                accounts.put(account.id(), updated.withCitizens(citizens));
                setDirty();
            }
        }

        return payments;
    }

    public boolean removeMember(String id, UUID owner, UUID memberId) {
        BankAccount account = accounts.get(id);

        if (account == null || !account.owner().equals(owner)) {
            return false;
        }

        List<BankMember> members = new ArrayList<>(account.members());

        if (!members.removeIf(member -> member.id().equals(memberId))) {
            return false;
        }

        accounts.put(id, account.withMembers(members));
        setDirty();
        return true;
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

    public record SalaryPayment(String accountName, int amount, boolean paid) {
    }
}
