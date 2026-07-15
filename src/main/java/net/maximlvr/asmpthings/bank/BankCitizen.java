package net.maximlvr.asmpthings.bank;

import java.util.UUID;

public record BankCitizen(UUID playerId, String name, String iban, int salary, String lastPaidDay) {
    public BankCitizen withLastPaidDay(String lastPaidDay) {
        return new BankCitizen(playerId, name, iban, salary, lastPaidDay);
    }

    public BankCitizen withName(String name) {
        return new BankCitizen(playerId, name, iban, salary, lastPaidDay);
    }
}
