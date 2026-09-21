package br.ufal.ic.p2.wepayu.models;

/** Depósito em conta bancária. */
public class BankPayment implements PaymentMethod {
    private final String bank;
    private final String agency;
    private final String account;

    public BankPayment(String bank, String agency, String account) {
        this.bank = bank;
        this.agency = agency;
        this.account = account;
    }

    public String getBank() {
        return bank;
    }

    public String getAgency() {
        return agency;
    }

    public String getAccount() {
        return account;
    }

    @Override
    public String getName() {
        return "banco";
    }

    @Override
    public String describe(Employee employee) {
        return bank + ", Ag. " + agency + " CC " + account;
    }
}
