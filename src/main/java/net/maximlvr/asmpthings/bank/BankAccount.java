package net.maximlvr.asmpthings.bank;

import java.util.UUID;

public record BankAccount(String id, String name, UUID owner, int balance) {
    public BankAccount withName(String name) {
        return new BankAccount(id, name, owner, balance);
    }

    public BankAccount withBalance(int balance) {
        return new BankAccount(id, name, owner, balance);
    }
}
