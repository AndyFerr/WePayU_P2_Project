package br.ufal.ic.p2.wepayu.models;

import br.ufal.ic.p2.wepayu.schedule.PaymentSchedule;
import br.ufal.ic.p2.wepayu.schedule.Schedules;
import br.ufal.ic.p2.wepayu.utils.DateRange;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Comissionado: assalariado que também ganha um percentual das vendas.
 * Pago a cada 2 sextas: 2 semanas de salário fixo (salário x 12 / 26) + comissões do período.
 */
public class CommissionedEmployee extends Employee {
    private BigDecimal monthlySalary;
    private BigDecimal commissionRate;
    private final List<SalesResult> sales = new ArrayList<>();

    public CommissionedEmployee(String id, String name, String address,
                                BigDecimal monthlySalary, BigDecimal commissionRate) {
        super(id, name, address);
        this.monthlySalary = monthlySalary;
        this.commissionRate = commissionRate;
    }

    public CommissionedEmployee(Employee base, BigDecimal monthlySalary, BigDecimal commissionRate) {
        super(base);
        this.monthlySalary = monthlySalary;
        this.commissionRate = commissionRate;
    }

    @Override
    public String getType() {
        return "comissionado";
    }

    @Override
    public BigDecimal getSalary() {
        return monthlySalary;
    }

    @Override
    public void setSalary(BigDecimal salary) {
        this.monthlySalary = salary;
    }

    @Override
    public BigDecimal getCommissionRate() {
        return commissionRate;
    }

    public void setCommissionRate(BigDecimal commissionRate) {
        this.commissionRate = commissionRate;
    }

    @Override
    public PaymentSchedule getSchedule() {
        return Schedules.BIWEEKLY;
    }

    public void addSale(SalesResult sale) {
        sales.add(sale);
    }

    public BigDecimal salesBetween(LocalDate from, LocalDate toExclusive) {
        return sales.stream()
                .filter(s -> DateRange.contains(s.date(), from, toExclusive))
                .map(SalesResult::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal fixedPay() {
        return monthlySalary.multiply(BigDecimal.valueOf(12)).divide(BigDecimal.valueOf(26), 2, RoundingMode.DOWN);
    }

    public BigDecimal commissionBetween(LocalDate from, LocalDate toExclusive) {
        return salesBetween(from, toExclusive).multiply(commissionRate).setScale(2, RoundingMode.DOWN);
    }

    @Override
    public BigDecimal grossPay(LocalDate from, LocalDate toExclusive) {
        return fixedPay().add(commissionBetween(from, toExclusive));
    }
}
