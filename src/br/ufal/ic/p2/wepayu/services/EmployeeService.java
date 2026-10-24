package br.ufal.ic.p2.wepayu.services;

import br.ufal.ic.p2.wepayu.exceptions.WePayUException;
import br.ufal.ic.p2.wepayu.exceptions.EmployeeNotFoundException;
import br.ufal.ic.p2.wepayu.models.BankPayment;
import br.ufal.ic.p2.wepayu.models.CommissionedEmployee;
import br.ufal.ic.p2.wepayu.models.Employee;
import br.ufal.ic.p2.wepayu.models.HandPayment;
import br.ufal.ic.p2.wepayu.models.HourlyEmployee;
import br.ufal.ic.p2.wepayu.models.MailPayment;
import br.ufal.ic.p2.wepayu.models.SalariedEmployee;
import br.ufal.ic.p2.wepayu.models.SalesResult;
import br.ufal.ic.p2.wepayu.models.ServiceCharge;
import br.ufal.ic.p2.wepayu.models.TimeCard;
import br.ufal.ic.p2.wepayu.models.UnionMembership;
import br.ufal.ic.p2.wepayu.schedule.PaymentSchedule;
import br.ufal.ic.p2.wepayu.schedule.ScheduleParser;
import br.ufal.ic.p2.wepayu.utils.Formats;
import br.ufal.ic.p2.wepayu.utils.Parsers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Casos de uso das User Stories 1 a 6 (cadastro, lançamentos, consultas e alterações).
 * Toda operação que muda estado passa pelo {@link TransactionManager}, o que
 * dá rollback em caso de erro e habilita o undo/redo (US 8).
 */
public class EmployeeService {
    private static final Set<String> TYPES = Set.of("horista", "assalariado", "comissionado");

    private final EmployeeRepository repository;
    private final TransactionManager transactions;

    public EmployeeService(EmployeeRepository repository, TransactionManager transactions) {
        this.repository = repository;
        this.transactions = transactions;
    }

    // ================================ US1 / US2: criar e remover ================================

    /**
     * @param commission {@code null} quando o cliente usou a forma sem comissão do comando.
     */
    public String createEmployee(String name, String address, String type, String salary, String commission)
            throws WePayUException {
        return transactions.execute(() -> {
            if (Parsers.isBlank(name)) {
                throw new WePayUException("Nome nao pode ser nulo.");
            }
            if (Parsers.isBlank(address)) {
                throw new WePayUException("Endereco nao pode ser nulo.");
            }
            if (type == null || !TYPES.contains(type)) {
                throw new WePayUException("Tipo invalido.");
            }
            BigDecimal salaryValue = parseSalary(salary);
            boolean commissioned = type.equals("comissionado");
            if (commissioned == (commission == null)) {
                // comissionado sem comissão, ou outro tipo com comissão
                throw new WePayUException("Tipo nao aplicavel.");
            }

            String id = repository.nextId();
            Employee employee = switch (type) {
                case "horista" -> new HourlyEmployee(id, name, address, salaryValue);
                case "assalariado" -> new SalariedEmployee(id, name, address, salaryValue);
                default -> new CommissionedEmployee(id, name, address, salaryValue, parseCommission(commission));
            };
            repository.add(employee);
            return id;
        });
    }

    public void removeEmployee(String id) throws WePayUException {
        transactions.executeVoid(() -> repository.remove(find(id).getId()));
    }

    // ================================ consultas ================================================

    public int countEmployees() {
        return repository.findAll().size();
    }

    public String getAttribute(String id, String attribute) throws WePayUException {
        return find(id).getAttribute(attribute);
    }

    /** @param index posição (a partir de 1) entre os empregados com esse nome, na ordem de criação. */
    public String findByName(String name, int index) throws WePayUException {
        List<Employee> matches = repository.findByName(name);
        if (index < 1 || index > matches.size()) {
            throw new WePayUException("Nao ha empregado com esse nome.");
        }
        return matches.get(index - 1).getId();
    }

    public String getNormalHours(String id, String start, String end) throws WePayUException {
        HourlyEmployee employee = findHourly(id);
        LocalDate[] range = parseRange(start, end);
        return Formats.hours(employee.normalHours(range[0], range[1]));
    }

    public String getOvertimeHours(String id, String start, String end) throws WePayUException {
        HourlyEmployee employee = findHourly(id);
        LocalDate[] range = parseRange(start, end);
        return Formats.hours(employee.overtimeHours(range[0], range[1]));
    }

    public String getSales(String id, String start, String end) throws WePayUException {
        CommissionedEmployee employee = findCommissioned(id);
        LocalDate[] range = parseRange(start, end);
        return Formats.money(employee.salesBetween(range[0], range[1]));
    }

    public String getServiceCharges(String id, String start, String end) throws WePayUException {
        Employee employee = find(id);
        if (employee.getUnion() == null) {
            throw new WePayUException("Empregado nao eh sindicalizado.");
        }
        LocalDate[] range = parseRange(start, end);
        return Formats.money(employee.getUnion().chargesBetween(range[0], range[1]));
    }

    // ================================ US3 / US4 / US5: lançamentos ==============================

    public void postTimeCard(String id, String date, String hours) throws WePayUException {
        transactions.executeVoid(() -> {
            HourlyEmployee employee = findHourly(id);
            BigDecimal hoursValue = Parsers.decimal(hours, "Horas devem ser positivas.", "Horas devem ser numericas.");
            if (hoursValue.signum() <= 0) {
                throw new WePayUException("Horas devem ser positivas.");
            }
            employee.addTimeCard(new TimeCard(Parsers.date(date, "Data invalida."), hoursValue));
        });
    }

    public void postSale(String id, String date, String amount) throws WePayUException {
        transactions.executeVoid(() -> {
            CommissionedEmployee employee = findCommissioned(id);
            BigDecimal value = parsePositive(amount);
            employee.addSale(new SalesResult(Parsers.date(date, "Data invalida."), value));
        });
    }

    /** @param memberId identificação do membro no sindicato (não é o id do empregado). */
    public void postServiceCharge(String memberId, String date, String amount) throws WePayUException {
        transactions.executeVoid(() -> {
            if (Parsers.isBlank(memberId)) {
                throw new WePayUException("Identificacao do membro nao pode ser nula.");
            }
            Employee employee = repository.findByUnionId(memberId)
                    .orElseThrow(() -> new WePayUException("Membro nao existe."));
            BigDecimal value = parsePositive(amount);
            employee.getUnion().addCharge(new ServiceCharge(Parsers.date(date, "Data invalida."), value));
        });
    }

    // ================================ US6: alterar empregado ====================================

    /**
     * @param extra parâmetros adicionais do comando, que dependem do atributo:
     *              tipo (salário e/ou comissão), metodoPagamento=banco (banco, agência, conta),
     *              sindicalizado=true (idSindicato, taxaSindical).
     */
    public void updateEmployee(String id, String attribute, String value, String... extra)
            throws WePayUException {
        transactions.executeVoid(() -> {
            Employee employee = find(id);
            if (attribute == null) {
                throw new WePayUException("Atributo nao existe.");
            }
            switch (attribute) {
                case "nome" -> {
                    if (Parsers.isBlank(value)) {
                        throw new WePayUException("Nome nao pode ser nulo.");
                    }
                    employee.setName(value);
                }
                case "endereco" -> {
                    if (Parsers.isBlank(value)) {
                        throw new WePayUException("Endereco nao pode ser nulo.");
                    }
                    employee.setAddress(value);
                }
                case "tipo" -> changeType(employee, value, extra);
                case "salario" -> employee.setSalary(parseSalary(value));
                case "comissao" -> findCommissioned(id).setCommissionRate(parseCommission(value));
                case "metodoPagamento" -> changePaymentMethod(employee, value, extra);
                case "agendaPagamento" -> changeSchedule(employee, value);
                case "sindicalizado" -> changeUnionMembership(employee, value, extra);
                case "idSindicato" -> {
                    requireUnion(employee);
                    ensureUnionIdAvailable(employee, value);
                    employee.getUnion().setMemberId(value);
                }
                case "taxaSindical" -> {
                    requireUnion(employee);
                    employee.getUnion().setFee(parseUnionFee(value));
                }
                default -> throw new WePayUException("Atributo nao existe.");
            }
        });
    }

    private void changeType(Employee current, String newType, String[] extra) throws WePayUException {
        if (newType == null || !TYPES.contains(newType)) {
            throw new WePayUException("Tipo invalido.");
        }
        if (newType.equals(current.getType())) {
            return;
        }
        Employee replacement = switch (newType) {
            case "horista" -> new HourlyEmployee(current, salaryOrCurrent(current, arg(extra, 0)));
            case "assalariado" -> new SalariedEmployee(current, salaryOrCurrent(current, arg(extra, 0)));
            default -> {
                // "comissao" sozinha, ou "salario" seguido de "comissao"
                BigDecimal salary = extra.length > 1 ? parseSalary(extra[0]) : current.getSalary();
                BigDecimal commission = parseCommission(extra.length > 0 ? extra[extra.length - 1] : null);
                yield new CommissionedEmployee(current, salary, commission);
            }
        };
        repository.replace(replacement);
    }

    // ================================ US9 / US10: agendas de pagamento ==========================

    private void changeSchedule(Employee employee, String description) throws WePayUException {
        PaymentSchedule schedule = repository.findSchedule(description)
                .orElseThrow(() -> new WePayUException("Agenda de pagamento nao esta disponivel"));
        employee.setSchedule(schedule);
    }

    /** US10: registra uma nova agenda a partir de uma descrição como "semanal 2 3" ou "mensal 1". */
    public void createSchedule(String description) throws WePayUException {
        transactions.executeVoid(() -> {
            PaymentSchedule schedule = ScheduleParser.parse(description);
            if (repository.findSchedule(schedule.getDescription()).isPresent()) {
                throw new WePayUException("Agenda de pagamentos ja existe");
            }
            repository.addSchedule(schedule);
        });
    }

    private void changePaymentMethod(Employee employee, String method, String[] extra) throws WePayUException {
        switch (method == null ? "" : method) {
            case "emMaos" -> employee.setPaymentMethod(new HandPayment());
            case "correios" -> employee.setPaymentMethod(new MailPayment());
            case "banco" -> {
                if (Parsers.isBlank(arg(extra, 0))) {
                    throw new WePayUException("Banco nao pode ser nulo.");
                }
                if (Parsers.isBlank(arg(extra, 1))) {
                    throw new WePayUException("Agencia nao pode ser nulo.");
                }
                if (Parsers.isBlank(arg(extra, 2))) {
                    throw new WePayUException("Conta corrente nao pode ser nulo.");
                }
                employee.setPaymentMethod(new BankPayment(extra[0], extra[1], extra[2]));
            }
            default -> throw new WePayUException("Metodo de pagamento invalido.");
        }
    }

    private void changeUnionMembership(Employee employee, String flag, String[] extra) throws WePayUException {
        if ("true".equals(flag)) {
            String unionId = arg(extra, 0);
            if (Parsers.isBlank(unionId)) {
                throw new WePayUException("Identificacao do sindicato nao pode ser nula.");
            }
            BigDecimal fee = parseUnionFee(arg(extra, 1));
            ensureUnionIdAvailable(employee, unionId);
            if (employee.getUnion() == null) {
                employee.setUnion(new UnionMembership(unionId, fee));
            } else {
                employee.getUnion().setMemberId(unionId);
                employee.getUnion().setFee(fee);
            }
        } else if ("false".equals(flag)) {
            employee.setUnion(null);
        } else {
            throw new WePayUException("Valor deve ser true ou false.");
        }
    }

    // ================================ helpers ===================================================

    private Employee find(String id) throws EmployeeNotFoundException {
        if (Parsers.isBlank(id)) {
            throw EmployeeNotFoundException.missingId();
        }
        return repository.findById(id).orElseThrow(EmployeeNotFoundException::unknownId);
    }

    private HourlyEmployee findHourly(String id) throws WePayUException {
        if (find(id) instanceof HourlyEmployee hourly) {
            return hourly;
        }
        throw new WePayUException("Empregado nao eh horista.");
    }

    private CommissionedEmployee findCommissioned(String id) throws WePayUException {
        if (find(id) instanceof CommissionedEmployee commissioned) {
            return commissioned;
        }
        throw new WePayUException("Empregado nao eh comissionado.");
    }

    private void requireUnion(Employee employee) throws WePayUException {
        if (employee.getUnion() == null) {
            throw new WePayUException("Empregado nao eh sindicalizado.");
        }
    }

    private void ensureUnionIdAvailable(Employee employee, String unionId) throws WePayUException {
        if (Parsers.isBlank(unionId)) {
            throw new WePayUException("Identificacao do sindicato nao pode ser nula.");
        }
        Optional<Employee> owner = repository.findByUnionId(unionId);
        if (owner.isPresent() && !owner.get().getId().equals(employee.getId())) {
            throw new WePayUException("Ha outro empregado com esta identificacao de sindicato");
        }
    }

    private LocalDate[] parseRange(String start, String end) throws WePayUException {
        LocalDate from = Parsers.date(start, "Data inicial invalida.");
        LocalDate to = Parsers.date(end, "Data final invalida.");
        if (from.isAfter(to)) {
            throw new WePayUException("Data inicial nao pode ser posterior aa data final.");
        }
        return new LocalDate[]{from, to};
    }

    private BigDecimal parseSalary(String raw) throws WePayUException {
        BigDecimal value = Parsers.decimal(raw, "Salario nao pode ser nulo.", "Salario deve ser numerico.");
        if (value.signum() < 0) {
            throw new WePayUException("Salario deve ser nao-negativo.");
        }
        return value;
    }

    private BigDecimal parseCommission(String raw) throws WePayUException {
        BigDecimal value = Parsers.decimal(raw, "Comissao nao pode ser nula.", "Comissao deve ser numerica.");
        if (value.signum() < 0) {
            throw new WePayUException("Comissao deve ser nao-negativa.");
        }
        return value;
    }

    private BigDecimal parseUnionFee(String raw) throws WePayUException {
        BigDecimal value = Parsers.decimal(raw, "Taxa sindical nao pode ser nula.", "Taxa sindical deve ser numerica.");
        if (value.signum() < 0) {
            throw new WePayUException("Taxa sindical deve ser nao-negativa.");
        }
        return value;
    }

    private BigDecimal parsePositive(String raw) throws WePayUException {
        BigDecimal value = Parsers.decimal(raw, "Valor deve ser positivo.", "Valor deve ser numerico.");
        if (value.signum() <= 0) {
            throw new WePayUException("Valor deve ser positivo.");
        }
        return value;
    }

    private BigDecimal salaryOrCurrent(Employee current, String rawSalary) throws WePayUException {
        return rawSalary == null ? current.getSalary() : parseSalary(rawSalary);
    }

    private static String arg(String[] extra, int index) {
        return extra != null && index < extra.length ? extra[index] : null;
    }
}
