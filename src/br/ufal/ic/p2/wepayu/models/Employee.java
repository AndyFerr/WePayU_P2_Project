package br.ufal.ic.p2.wepayu.models;

import br.ufal.ic.p2.wepayu.exceptions.WePayUException;
import br.ufal.ic.p2.wepayu.schedule.PaymentSchedule;
import br.ufal.ic.p2.wepayu.utils.Formats;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Raiz da hierarquia de empregados. Cada tipo (horista, assalariado,
 * comissionado) sabe calcular o próprio salário bruto e qual agenda o paga;
 * quem roda a folha não precisa de {@code if (tipo == ...)}.
 */
public abstract class Employee implements Serializable {
    private final String id;
    private String name;
    private String address;
    private PaymentMethod paymentMethod = new HandPayment();
    private UnionMembership union;

    protected Employee(String id, String name, String address) {
        this.id = id;
        this.name = name;
        this.address = address;
    }

    /** Usado na troca de tipo: preserva identidade, método de pagamento e sindicato. */
    protected Employee(Employee base) {
        this(base.id, base.name, base.address);
        this.paymentMethod = base.paymentMethod;
        this.union = base.union;
    }

    // ---- comportamento específico de cada tipo ------------------------------------------------

    /** "horista", "assalariado" ou "comissionado". */
    public abstract String getType();

    /** Salário por hora (horista) ou salário mensal (demais). */
    public abstract BigDecimal getSalary();

    public abstract void setSalary(BigDecimal salary);

    public abstract PaymentSchedule getSchedule();

    /** Salário bruto do período [from, toExclusive). */
    public abstract BigDecimal grossPay(LocalDate from, LocalDate toExclusive);

    /** Taxa de comissão; {@code null} quando o empregado não é comissionado. */
    public BigDecimal getCommissionRate() {
        return null;
    }

    // ---- comportamento comum ------------------------------------------------------------------

    /** Taxa sindical diária x dias do período + taxas de serviço lançadas no período. */
    public BigDecimal deductions(LocalDate from, LocalDate toExclusive) {
        if (union == null) {
            return BigDecimal.ZERO;
        }
        long days = ChronoUnit.DAYS.between(from, toExclusive);
        return union.getFee().multiply(BigDecimal.valueOf(days)).add(union.chargesBetween(from, toExclusive));
    }

    /** Descontos efetivamente abatidos do pagamento de {@code payDay}. */
    public BigDecimal payrollDeductions(LocalDate payDay) {
        return deductions(getSchedule().periodStart(payDay), payDay.plusDays(1));
    }

    /** Implementa getAtributoEmpregado. */
    public String getAttribute(String attribute) throws WePayUException {
        if (attribute == null) {
            throw new WePayUException("Atributo nao existe.");
        }
        return switch (attribute) {
            case "nome" -> name;
            case "endereco" -> address;
            case "tipo" -> getType();
            case "salario" -> Formats.money(getSalary());
            case "comissao" -> {
                BigDecimal rate = getCommissionRate();
                if (rate == null) {
                    throw new WePayUException("Empregado nao eh comissionado.");
                }
                yield Formats.rate(rate);
            }
            case "sindicalizado" -> String.valueOf(union != null);
            case "idSindicato" -> requireUnion().getMemberId();
            case "taxaSindical" -> Formats.rate(requireUnion().getFee());
            case "metodoPagamento" -> paymentMethod.getName();
            case "banco" -> requireBank().getBank();
            case "agencia" -> requireBank().getAgency();
            case "contaCorrente" -> requireBank().getAccount();
            default -> throw new WePayUException("Atributo nao existe.");
        };
    }

    private UnionMembership requireUnion() throws WePayUException {
        if (union == null) {
            throw new WePayUException("Empregado nao eh sindicalizado.");
        }
        return union;
    }

    private BankPayment requireBank() throws WePayUException {
        if (paymentMethod instanceof BankPayment bank) {
            return bank;
        }
        throw new WePayUException("Empregado nao recebe em banco.");
    }

    // ---- getters / setters --------------------------------------------------------------------

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public UnionMembership getUnion() {
        return union;
    }

    public void setUnion(UnionMembership union) {
        this.union = union;
    }
}
