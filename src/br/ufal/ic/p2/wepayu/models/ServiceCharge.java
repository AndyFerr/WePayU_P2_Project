package br.ufal.ic.p2.wepayu.models;

import br.ufal.ic.p2.wepayu.utils.Require;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Taxa de serviço cobrada pelo sindicato. */
public record ServiceCharge(LocalDate date, BigDecimal amount) implements Serializable {
    public ServiceCharge {
        Require.present(date, "Data da taxa de servico");
        Require.positive(amount, "Valor da taxa de servico");
    }
}