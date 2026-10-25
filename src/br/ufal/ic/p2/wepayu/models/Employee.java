package br.ufal.ic.p2.wepayu.models;

import br.ufal.ic.p2.wepayu.exceptions.EmployeeTypeMismatchException;
import br.ufal.ic.p2.wepayu.exceptions.UnknownAttributeException;
import br.ufal.ic.p2.wepayu.exceptions.WePayUException;
import br.ufal.ic.p2.wepayu.schedule.PaymentSchedule;
import br.ufal.ic.p2.wepayu.utils.Formats;
import br.ufal.ic.p2.wepayu.utils.Require;

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
    private PaymentSchedule schedule;
    private final EmployeeHistory history;

    /** @param schedule agenda inicial (cada tipo tem seu default; ver US 9/10). */
    protected Employee(String id, String name, String address, PaymentSchedule schedule) {
        this.id = Require.text(id, "Id do empregado");
        this.name = Require.text(name, "Nome");
        this.address = Require.text(address, "Endereco");
        this.schedule = Require.present(schedule, "Agenda de pagamento");
        this.history = new EmployeeHistory();
    }

    /**
     * Usado na troca de tipo: preserva identidade, método de pagamento, sindicato, agenda e,
     * principalmente, o histórico de lançamentos. Trocar o regime de pagamento não apaga os
     * cartões de ponto nem as vendas já registradas.
     */
    protected Employee(Employee base) {
        this.id = base.id;
        this.name = base.name;
        this.address = base.address;
        this.schedule = base.schedule;
        this.paymentMethod = base.paymentMethod;
        this.union = base.union;
        this.history = base.history;
    }

    /** Lançamentos do empregado. Visível só para a hierarquia: não vaza para fora do modelo. */
    protected EmployeeHistory history() {
        return history;
    }

    // ---- comportamento específico de cada tipo ------------------------------------------------

    /** "horista", "assalariado" ou "comissionado". */
    public abstract String getType();

    /** Salário por hora (horista) ou salário mensal (demais). */
    public abstract BigDecimal getSalary();

    public abstract void setSalary(BigDecimal salary);

    /** Salário bruto do período [from, toExclusive). */
    public abstract BigDecimal grossPay(LocalDate from, LocalDate toExclusive);

    /** Taxa de comissão; {@code null} quando o empregado não é comissionado. */
    public BigDecimal getCommissionRate() {
        return null;
    }

    public PaymentSchedule getSchedule() {
        return schedule;
    }

    public void setSchedule(PaymentSchedule schedule) {
        this.schedule = Require.present(schedule, "Agenda de pagamento");
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
            throw new UnknownAttributeException();
        }
        return switch (attribute) {
            case "nome" -> name;
            case "endereco" -> address;
            case "tipo" -> getType();
            case "salario" -> Formats.money(getSalary());
            case "comissao" -> {
                BigDecimal rate = getCommissionRate();
                if (rate == null) {
                    throw EmployeeTypeMismatchException.notCommissioned();
                }
                yield Formats.rate(rate);
            }
            case "sindicalizado" -> String.valueOf(union != null);
            case "idSindicato" -> requireUnion().getMemberId();
            case "taxaSindical" -> Formats.rate(requireUnion().getFee());
            case "metodoPagamento" -> paymentMethod.getName();
            case "agendaPagamento" -> schedule.getDescription();
            case "banco" -> requireBank().getBank();
            case "agencia" -> requireBank().getAgency();
            case "contaCorrente" -> requireBank().getAccount();
            default -> throw new UnknownAttributeException();
        };
    }

    private UnionMembership requireUnion() throws EmployeeTypeMismatchException {
        if (union == null) {
            throw EmployeeTypeMismatchException.notUnionMember();
        }
        return union;
    }

    private BankPayment requireBank() throws EmployeeTypeMismatchException {
        if (paymentMethod instanceof BankPayment bank) {
            return bank;
        }
        throw EmployeeTypeMismatchException.notPaidByBank();
    }

    // ---- getters / setters --------------------------------------------------------------------

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = Require.text(name, "Nome");
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = Require.text(address, "Endereco");
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = Require.present(paymentMethod, "Metodo de pagamento");
    }

    // ---- sindicato ----------------------------------------------------------------------------
    //
    // A filiação não é exposta: quem precisa mexer nela pede ao empregado, que é quem conhece
    // as regras. Devolver o objeto interno deixaria qualquer um alterar taxa e cobranças pelas
    // costas do empregado.

    public boolean isUnionMember() {
        return union != null;
    }

    /** Usado na busca por id de sindicato; não lança quando o empregado não é sindicalizado. */
    public boolean hasUnionMemberId(String memberId) {
        return union != null && union.getMemberId().equals(memberId);
    }

    public String getUnionMemberId() throws EmployeeTypeMismatchException {
        return requireUnion().getMemberId();
    }

    public BigDecimal getUnionFee() throws EmployeeTypeMismatchException {
        return requireUnion().getFee();
    }

    public void joinUnion(String memberId, BigDecimal fee) {
        if (union == null) {
            union = new UnionMembership(memberId, fee);
        } else {
            union.setMemberId(memberId);
            union.setFee(fee);
        }
    }

    public void leaveUnion() {
        union = null;
    }

    public void changeUnionMemberId(String memberId) throws EmployeeTypeMismatchException {
        requireUnion().setMemberId(memberId);
    }

    public void changeUnionFee(BigDecimal fee) throws EmployeeTypeMismatchException {
        requireUnion().setFee(fee);
    }

    public void addServiceCharge(ServiceCharge charge) throws EmployeeTypeMismatchException {
        requireUnion().addCharge(charge);
    }

    public BigDecimal serviceChargesBetween(LocalDate from, LocalDate toExclusive)
            throws EmployeeTypeMismatchException {
        return requireUnion().chargesBetween(from, toExclusive);
    }
}