package br.ufal.ic.p2.wepayu.models;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Taxa de serviço cobrada pelo sindicato. */
public record ServiceCharge(LocalDate date, BigDecimal amount) implements Serializable {
}
