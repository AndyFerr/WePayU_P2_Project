package br.ufal.ic.p2.wepayu.models;

import br.ufal.ic.p2.wepayu.schedule.Schedules;

import br.ufal.ic.p2.wepayu.utils.Require;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Assalariado: salário mensal fixo; a fração paga em cada período depende da agenda escolhida. */
public class SalariedEmployee extends Employee {
    private BigDecimal monthlySalary;

    public SalariedEmployee(String id, String name, String address, BigDecimal monthlySalary) {
        super(id, name, address, Schedules.MONTHLY);
        this.monthlySalary = Require.nonNegative(monthlySalary, "Salario mensal");
    }

    public SalariedEmployee(Employee base, BigDecimal monthlySalary) {
        super(base);
        this.monthlySalary = Require.nonNegative(monthlySalary, "Salario mensal");
    }

    @Override
    public String getType() {
        return "assalariado";
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
    public BigDecimal grossPay(LocalDate from, LocalDate toExclusive) {
        return getSchedule().periodicSalary(monthlySalary);
    }
}