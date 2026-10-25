package br.ufal.ic.p2.wepayu.models;

import br.ufal.ic.p2.wepayu.schedule.PaymentSchedule;
import br.ufal.ic.p2.wepayu.schedule.Schedules;

import br.ufal.ic.p2.wepayu.utils.Require;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

/** Horista: recebe por hora; acima de 8h/dia, a hora extra vale 1,5x. */
public class HourlyEmployee extends Employee {
    private static final BigDecimal DAILY_LIMIT = new BigDecimal("8");
    private static final BigDecimal OVERTIME_FACTOR = new BigDecimal("1.5");

    private BigDecimal hourlyRate;

    public HourlyEmployee(String id, String name, String address, BigDecimal hourlyRate) {
        super(id, name, address, Schedules.WEEKLY);
        this.hourlyRate = Require.nonNegative(hourlyRate, "Salario por hora");
    }

    public HourlyEmployee(Employee base, BigDecimal hourlyRate) {
        super(base);
        this.hourlyRate = Require.nonNegative(hourlyRate, "Salario por hora");
    }

    @Override
    public String getType() {
        return "horista";
    }

    @Override
    public BigDecimal getSalary() {
        return hourlyRate;
    }

    @Override
    public void setSalary(BigDecimal salary) {
        this.hourlyRate = Require.nonNegative(salary, "Salario por hora");
    }

    public void addTimeCard(TimeCard card) {
        history().addTimeCard(card);
    }

    public BigDecimal normalHours(LocalDate from, LocalDate toExclusive) {
        return history().timeCardsBetween(from, toExclusive).map(c -> c.hours().min(DAILY_LIMIT))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal overtimeHours(LocalDate from, LocalDate toExclusive) {
        return history().timeCardsBetween(from, toExclusive)
                .map(c -> c.hours().subtract(DAILY_LIMIT).max(BigDecimal.ZERO))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    public BigDecimal grossPay(LocalDate from, LocalDate toExclusive) {
        BigDecimal normal = normalHours(from, toExclusive).multiply(hourlyRate);
        BigDecimal overtime = overtimeHours(from, toExclusive).multiply(hourlyRate).multiply(OVERTIME_FACTOR);
        return normal.add(overtime).setScale(2, RoundingMode.DOWN);
    }

    /**
     * Horista não pode ter contracheque negativo: se as deduções (taxa sindical + taxas de serviço) do
     * período mais o débito acumulado superam o bruto, só se desconta até o bruto e o resto fica como débito
     * para os próximos pagamentos. O débito é recalculado reprocessando as semanas desde o primeiro
     * cartão de ponto (o horista é "contratado" no primeiro cartão), então rodar a folha de novo,
     * ou desfazer, sempre dá o mesmo resultado.
     */
    @Override
    public BigDecimal payrollDeductions(LocalDate payDay) {
        if (!isUnionMember() || !history().hasTimeCards()) {
            return BigDecimal.ZERO;
        }
        PaymentSchedule schedule = getSchedule();
        LocalDate day = history().firstTimeCardDate().orElseThrow();
        while (!schedule.isPayDay(day)) {
            day = day.plusDays(1);
        }
        BigDecimal debt = BigDecimal.ZERO;
        for (; !day.isAfter(payDay); day = day.plusDays(7)) {
            LocalDate from = schedule.periodStart(day);
            LocalDate toExclusive = day.plusDays(1);
            BigDecimal owed = deductions(from, toExclusive).add(debt);
            BigDecimal deducted = owed.min(grossPay(from, toExclusive));
            debt = owed.subtract(deducted);
            if (day.equals(payDay)) {
                return deducted;
            }
        }
        return BigDecimal.ZERO;
    }
}