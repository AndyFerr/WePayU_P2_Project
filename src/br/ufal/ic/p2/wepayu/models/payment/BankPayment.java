package br.ufal.ic.p2.wepayu.models.payment;

import br.ufal.ic.p2.wepayu.utils.Require;

/** Depósito em conta bancária. */
public class BankPayment implements PaymentMethod {
    private final String bank;
    private final String agency;
    private final String account;

    public BankPayment(String bank, String agency, String account) {
        this.bank = Require.text(bank, "Banco");
        this.agency = Require.text(agency, "Agencia");
        this.account = Require.text(account, "Conta corrente");
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
    public String describe(String employeeAddress) {
        return bank + ", Ag. " + agency + " CC " + account;
    }
}