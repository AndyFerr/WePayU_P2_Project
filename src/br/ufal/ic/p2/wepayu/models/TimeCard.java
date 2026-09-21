package br.ufal.ic.p2.wepayu.models;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Cartão de ponto: horas trabalhadas em um dia por um horista. */
public record TimeCard(LocalDate date, BigDecimal hours) implements Serializable {
}
