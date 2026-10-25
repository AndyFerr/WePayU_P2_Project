package br.ufal.ic.p2.wepayu.models.payment;

/** Cheque em mãos. */
public class HandPayment implements PaymentMethod {
    @Override
    public String getName() {
        return "emMaos";
    }

    @Override
    public String describe(String employeeAddress) {
        return "Em maos";
    }
}