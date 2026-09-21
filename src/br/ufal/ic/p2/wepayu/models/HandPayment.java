package br.ufal.ic.p2.wepayu.models;

/** Cheque em mãos. */
public class HandPayment implements PaymentMethod {
    @Override
    public String getName() {
        return "emMaos";
    }

    @Override
    public String describe(Employee employee) {
        return "Em maos";
    }
}
