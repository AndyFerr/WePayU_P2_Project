package br.ufal.ic.p2.wepayu.models.payment;

import java.io.Serializable;

/** Método de pagamento escolhido pelo empregado (Strategy). */
public interface PaymentMethod extends Serializable {

    /** Nome usado em getAtributoEmpregado(..., "metodoPagamento"). */
    String getName();

    /**
     * Texto usado na coluna "Metodo" do relatório da folha.
     *
     * @param employeeAddress endereço do empregado, de que só a entrega pelos correios precisa.
     *                        Recebê-lo pronto mantém os métodos de pagamento independentes de
     *                        {@code Employee}, que já depende deles.
     */
    String describe(String employeeAddress);
}