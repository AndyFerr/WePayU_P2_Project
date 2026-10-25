package br.ufal.ic.p2.wepayu.models.payment;

/** Cheque pelos correios (enviado para o endereço do empregado). */
public class MailPayment implements PaymentMethod {
    @Override
    public String getName() {
        return "correios";
    }

    @Override
    public String describe(String employeeAddress) {
        return "Correios, " + employeeAddress;
    }
}