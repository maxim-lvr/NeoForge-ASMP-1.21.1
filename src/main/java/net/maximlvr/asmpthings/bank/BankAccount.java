package net.maximlvr.asmpthings.bank;

import java.util.List;
import java.util.UUID;

public record BankAccount(String id, String name, UUID owner, int balance, List<BankMember> members, boolean adminAccount, List<BankCitizen> citizens) {
    public BankAccount(String id, String name, UUID owner, int balance) {
        this(id, name, owner, balance, List.of(), false, List.of());
    }

    public BankAccount {
        members = List.copyOf(members == null ? List.of() : members);
        citizens = List.copyOf(citizens == null ? List.of() : citizens);
    }

    public BankAccount withName(String name) {
        return new BankAccount(id, name, owner, balance, members, adminAccount, citizens);
    }

    public BankAccount withBalance(int balance) {
        return new BankAccount(id, name, owner, balance, members, adminAccount, citizens);
    }

    public BankAccount withMembers(List<BankMember> members) {
        return new BankAccount(id, name, owner, balance, members, adminAccount, citizens);
    }

    public BankAccount withCitizens(List<BankCitizen> citizens) {
        return new BankAccount(id, name, owner, balance, members, adminAccount, citizens);
    }

    public boolean hasAccess(UUID playerId) {
        if (owner.equals(playerId)) {
            return true;
        }

        for (BankMember member : members) {
            if (member.id().equals(playerId)) {
                return true;
            }
        }

        return false;
    }
}
