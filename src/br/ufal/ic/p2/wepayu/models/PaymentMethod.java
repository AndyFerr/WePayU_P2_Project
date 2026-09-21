package br.ufal.ic.p2.wepayu.models;

import java.io.Serializable;

/** Método de pagamento escolhido pelo empregado (Strategy). */
public interface PaymentMethod extends Serializable {

    /** Nome usado em getAtributoEmpregado(..., "metodoPagamento"). */
    String getName();

    /** Texto usado na coluna "Metodo" do relatório da folha. */
    String describe(Employee employee);
}
