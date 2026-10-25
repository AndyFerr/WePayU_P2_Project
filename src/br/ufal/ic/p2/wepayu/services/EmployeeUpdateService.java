package br.ufal.ic.p2.wepayu.services;

import br.ufal.ic.p2.wepayu.exceptions.InvalidValueException;
import br.ufal.ic.p2.wepayu.exceptions.UnknownAttributeException;
import br.ufal.ic.p2.wepayu.exceptions.WePayUException;
import br.ufal.ic.p2.wepayu.models.BankPayment;
import br.ufal.ic.p2.wepayu.models.Employee;
import br.ufal.ic.p2.wepayu.models.HandPayment;
import br.ufal.ic.p2.wepayu.models.MailPayment;
import br.ufal.ic.p2.wepayu.utils.MonetaryField;
import br.ufal.ic.p2.wepayu.utils.Parsers;
import br.ufal.ic.p2.wepayu.utils.ValueParser;

/**
 * Atendimento do comando alteraEmpregado (US 6).
 *
 * <p>O comando é um só, mas cada atributo pertence a um assunto diferente. Esta classe
 * decide de quem é o atributo e encaminha: tipo vai para o cadastro, sindicato para o
 * serviço do sindicato, agenda para o de agendas. Só os atributos que não têm dono em
 * outro lugar — nome, endereço, salário, comissão e método de pagamento — são tratados aqui.
 */
public class EmployeeUpdateService {
    private final TransactionManager transactions;
    private final EmployeeFinder finder;
    private final EmployeeRegistrationService registration;
    private final UnionService union;
    private final ScheduleService schedules;

    public EmployeeUpdateService(TransactionManager transactions, EmployeeFinder finder,
                                 EmployeeRegistrationService registration, UnionService union,
                                 ScheduleService schedules) {
        this.transactions = transactions;
        this.finder = finder;
        this.registration = registration;
        this.union = union;
        this.schedules = schedules;
    }

    /**
     * @param extra parâmetros adicionais do comando, que dependem do atributo:
     *              tipo (salário e/ou comissão), metodoPagamento=banco (banco, agência, conta),
     *              sindicalizado=true (idSindicato, taxaSindical).
     */
    public void update(String id, String attribute, String value, String... extra) throws WePayUException {
        transactions.executeVoid(() -> {
            Employee employee = finder.byId(id);
            if (attribute == null) {
                throw new UnknownAttributeException();
            }
            switch (attribute) {
                case "nome" -> employee.setName(required(value, "Nome nao pode ser nulo."));
                case "endereco" -> employee.setAddress(required(value, "Endereco nao pode ser nulo."));
                case "salario" -> employee.setSalary(ValueParser.nonNegative(value, MonetaryField.SALARY));
                case "comissao" -> finder.commissionedById(id)
                        .setCommissionRate(ValueParser.nonNegative(value, MonetaryField.COMMISSION));
                case "metodoPagamento" -> changePaymentMethod(employee, value, extra);
                case "tipo" -> registration.changeType(employee, value, extra);
                case "agendaPagamento" -> schedules.assign(employee, value);
                case "sindicalizado" -> union.changeMembership(employee, value, extra);
                case "idSindicato" -> union.changeMemberId(employee, value);
                case "taxaSindical" -> union.changeFee(employee, value);
                default -> throw new UnknownAttributeException();
            }
        });
    }

    private String required(String value, String message) throws InvalidValueException {
        if (Parsers.isBlank(value)) {
            throw new InvalidValueException(message);
        }
        return value;
    }

    private void changePaymentMethod(Employee employee, String method, String[] extra) throws WePayUException {
        switch (method == null ? "" : method) {
            case "emMaos" -> employee.setPaymentMethod(new HandPayment());
            case "correios" -> employee.setPaymentMethod(new MailPayment());
            case "banco" -> employee.setPaymentMethod(new BankPayment(
                    required(arg(extra, 0), "Banco nao pode ser nulo."),
                    required(arg(extra, 1), "Agencia nao pode ser nulo."),
                    required(arg(extra, 2), "Conta corrente nao pode ser nulo.")));
            default -> throw new InvalidValueException("Metodo de pagamento invalido.");
        }
    }

    private static String arg(String[] extra, int index) {
        return extra != null && index < extra.length ? extra[index] : null;
    }
}