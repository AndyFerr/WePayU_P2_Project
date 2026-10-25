package br.ufal.ic.p2.wepayu.models;

import br.ufal.ic.p2.wepayu.schedule.Schedules;

import br.ufal.ic.p2.wepayu.utils.Require;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

/**
 * Comissionado: assalariado que também ganha um percentual das vendas. A parte fixa
 * (salário) é a fração do salário mensal correspondente a um período da agenda escolhida;
 * a comissão é sobre as vendas lançadas nesse mesmo período.
 */
public class CommissionedEmployee extends Employee {
    private BigDecimal monthlySalary;
    private BigDecimal commissionRate;

    public CommissionedEmployee(String id, String name, String address,
                                BigDecimal monthlySalary, BigDecimal commissionRate) {
        super(id, name, address, Schedules.BIWEEKLY);
        this.monthlySalary = Require.nonNegative(monthlySalary, "Salario mensal");
        this.commissionRate = Require.nonNegative(commissionRate, "Taxa de comissao");
    }

    public CommissionedEmployee(Employee base, BigDecimal monthlySalary, BigDecimal commissionRate) {
        super(base);
        this.monthlySalary = Require.nonNegative(monthlySalary, "Salario mensal");
        this.commissionRate = Require.nonNegative(commissionRate, "Taxa de comissao");
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
        this.monthlySalary = Require.nonNegative(salary, "Salario mensal");
    }

    @Override
    public BigDecimal getCommissionRate() {
        return commissionRate;
    }

    public void setCommissionRate(BigDecimal commissionRate) {
        this.commissionRate = Require.nonNegative(commissionRate, "Taxa de comissao");
    }

    public void addSale(SalesResult sale) {
        history().addSale(sale);
    }

    public BigDecimal salesBetween(LocalDate from, LocalDate toExclusive) {
        return history().salesBetween(from, toExclusive)
                .map(SalesResult::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal fixedPay() {
        return getSchedule().periodicSalary(monthlySalary);
    }

    public BigDecimal commissionBetween(LocalDate from, LocalDate toExclusive) {
        return salesBetween(from, toExclusive).multiply(commissionRate).setScale(2, RoundingMode.DOWN);
    }

    @Override
    public BigDecimal grossPay(LocalDate from, LocalDate toExclusive) {
        return fixedPay().add(commissionBetween(from, toExclusive));
    }
}