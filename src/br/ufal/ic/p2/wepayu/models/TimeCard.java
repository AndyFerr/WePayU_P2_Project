package br.ufal.ic.p2.wepayu.models;

import br.ufal.ic.p2.wepayu.utils.Require;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Cartão de ponto: horas trabalhadas em um dia por um horista. */
public record TimeCard(LocalDate date, BigDecimal hours) implements Serializable {
    public TimeCard {
        Require.present(date, "Data do cartao de ponto");
        Require.positive(hours, "Horas do cartao de ponto");
    }
}