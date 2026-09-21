package br.ufal.ic.p2.wepayu.models;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Resultado de venda de um comissionado. */
public record SalesResult(LocalDate date, BigDecimal amount) implements Serializable {
}
