package br.ufal.ic.p2.wepayu.services;

import br.ufal.ic.p2.wepayu.persistence.EmployeeRepository;
import br.ufal.ic.p2.wepayu.persistence.TransactionManager;
import br.ufal.ic.p2.wepayu.exceptions.InvalidValueException;
import br.ufal.ic.p2.wepayu.exceptions.WePayUException;
import br.ufal.ic.p2.wepayu.models.CommissionedEmployee;
import br.ufal.ic.p2.wepayu.models.Employee;
import br.ufal.ic.p2.wepayu.models.HourlyEmployee;
import br.ufal.ic.p2.wepayu.models.SalariedEmployee;
import br.ufal.ic.p2.wepayu.utils.MonetaryField;
import br.ufal.ic.p2.wepayu.utils.Parsers;
import br.ufal.ic.p2.wepayu.utils.ValueParser;

import java.math.BigDecimal;
import java.util.Set;

/**
 * Cadastro de empregados (US 1 e 2) e troca do regime de pagamento (parte da US 6):
 * quem entra na folha, quem sai, e em que regime cada um é pago.
 */
public class EmployeeRegistrationService {
    private static final Set<String> TYPES = Set.of("horista", "assalariado", "comissionado");

    private final EmployeeRepository repository;
    private final TransactionManager transactions;
    private final EmployeeFinder finder;

    public EmployeeRegistrationService(EmployeeRepository repository, TransactionManager transactions,
                                       EmployeeFinder finder) {
        this.repository = repository;
        this.transactions = transactions;
        this.finder = finder;
    }

    /** @param commission {@code null} quando o cliente usou a forma sem comissão do comando. */
    public String create(String name, String address, String type, String salary, String commission)
            throws WePayUException {
        return transactions.execute(() -> {
            if (Parsers.isBlank(name)) {
                throw new InvalidValueException("Nome nao pode ser nulo.");
            }
            if (Parsers.isBlank(address)) {
                throw new InvalidValueException("Endereco nao pode ser nulo.");
            }
            requireKnownType(type);
            BigDecimal salaryValue = ValueParser.nonNegative(salary, MonetaryField.SALARY);
            boolean commissioned = type.equals("comissionado");
            if (commissioned == (commission == null)) {
                // comissionado sem comissão, ou outro tipo com comissão
                throw new InvalidValueException("Tipo nao aplicavel.");
            }

            String id = repository.nextId();
            Employee employee = switch (type) {
                case "horista" -> new HourlyEmployee(id, name, address, salaryValue);
                case "assalariado" -> new SalariedEmployee(id, name, address, salaryValue);
                default -> new CommissionedEmployee(id, name, address, salaryValue,
                        ValueParser.nonNegative(commission, MonetaryField.COMMISSION));
            };
            repository.add(employee);
            return id;
        });
    }

    public void remove(String id) throws WePayUException {
        transactions.executeVoid(() -> repository.remove(finder.byId(id).getId()));
    }

    /**
     * Troca o regime de pagamento. O substituto nasce a partir do atual, então identidade,
     * sindicato, método de pagamento, agenda e histórico de lançamentos são preservados;
     * só muda o que o comando pediu.
     *
     * <p>Pedir o tipo que o empregado já tem não é um comando vazio: continua valendo para
     * aplicar um salário ou uma comissão novos que venham junto.
     */
    void changeType(Employee current, String newType, String[] extra) throws WePayUException {
        requireKnownType(newType);
        Employee replacement = switch (newType) {
            case "horista" -> new HourlyEmployee(current, salaryOrCurrent(current, arg(extra, 0)));
            case "assalariado" -> new SalariedEmployee(current, salaryOrCurrent(current, arg(extra, 0)));
            default -> new CommissionedEmployee(current, commissionedSalary(current, extra),
                    commissionedRate(current, extra));
        };
        repository.replace(replacement);
    }

    private void requireKnownType(String type) throws InvalidValueException {
        if (type == null || !TYPES.contains(type)) {
            throw new InvalidValueException("Tipo invalido.");
        }
    }

    private BigDecimal salaryOrCurrent(Employee current, String rawSalary) throws InvalidValueException {
        return rawSalary == null ? current.getSalary() : ValueParser.nonNegative(rawSalary, MonetaryField.SALARY);
    }

    /** No comando, "comissao" vem sozinha ou precedida de "salario". */
    private BigDecimal commissionedSalary(Employee current, String[] extra) throws InvalidValueException {
        return extra.length > 1
                ? ValueParser.nonNegative(extra[0], MonetaryField.SALARY)
                : current.getSalary();
    }

    private BigDecimal commissionedRate(Employee current, String[] extra) throws InvalidValueException {
        if (extra.length == 0) {
            // já comissionado e sem comissão nova: mantém a atual
            if (current.getCommissionRate() != null) {
                return current.getCommissionRate();
            }
            throw new InvalidValueException("Comissao nao pode ser nula.");
        }
        return ValueParser.nonNegative(extra[extra.length - 1], MonetaryField.COMMISSION);
    }

    private static String arg(String[] extra, int index) {
        return extra != null && index < extra.length ? extra[index] : null;
    }
}