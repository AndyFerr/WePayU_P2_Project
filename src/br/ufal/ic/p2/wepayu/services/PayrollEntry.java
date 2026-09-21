package br.ufal.ic.p2.wepayu.services;

import br.ufal.ic.p2.wepayu.models.Employee;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Resultado do cálculo de um empregado em um dia de folha (o "contracheque"). */
public record PayrollEntry(Employee employee, LocalDate periodStart, LocalDate payDay,
                           BigDecimal gross, BigDecimal deductions) {

    public BigDecimal net() {
        return gross.subtract(deductions);
    }
}
