package br.ufal.ic.p2.wepayu.report;

import br.ufal.ic.p2.wepayu.models.Employee;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Resultado do cálculo de um empregado em um dia de folha (o "contracheque"). */
public record PayrollEntry(Employee employee, LocalDate periodStart, LocalDate payDay,
                           BigDecimal gross, BigDecimal deductions) {

    public BigDecimal net() {
        return gross.subtract(deductions);
    }

    /** Soma dos salários brutos de um conjunto de contracheques. */
    public static BigDecimal totalGross(List<PayrollEntry> entries) {
        return entries.stream().map(PayrollEntry::gross).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}