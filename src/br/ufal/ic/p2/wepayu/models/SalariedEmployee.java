package br.ufal.ic.p2.wepayu.models;

import br.ufal.ic.p2.wepayu.schedule.PaymentSchedule;
import br.ufal.ic.p2.wepayu.schedule.Schedules;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Assalariado: salário fixo mensal, pago no último dia útil do mês. */
public class SalariedEmployee extends Employee {
    private BigDecimal monthlySalary;

    public SalariedEmployee(String id, String name, String address, BigDecimal monthlySalary) {
        super(id, name, address);
        this.monthlySalary = monthlySalary;
    }

    public SalariedEmployee(Employee base, BigDecimal monthlySalary) {
        super(base);
        this.monthlySalary = monthlySalary;
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
        this.monthlySalary = salary;
    }

    @Override
    public PaymentSchedule getSchedule() {
        return Schedules.MONTHLY;
    }

    @Override
    public BigDecimal grossPay(LocalDate from, LocalDate toExclusive) {
        return monthlySalary;
    }
}
