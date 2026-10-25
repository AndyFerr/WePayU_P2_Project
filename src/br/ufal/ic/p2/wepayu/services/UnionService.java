package br.ufal.ic.p2.wepayu.services;

import br.ufal.ic.p2.wepayu.exceptions.InvalidValueException;
import br.ufal.ic.p2.wepayu.exceptions.UnionException;
import br.ufal.ic.p2.wepayu.exceptions.WePayUException;
import br.ufal.ic.p2.wepayu.models.Employee;
import br.ufal.ic.p2.wepayu.models.ServiceCharge;
import br.ufal.ic.p2.wepayu.persistence.EmployeeRepository;
import br.ufal.ic.p2.wepayu.persistence.TransactionManager;
import br.ufal.ic.p2.wepayu.utils.MonetaryField;
import br.ufal.ic.p2.wepayu.utils.Parsers;
import br.ufal.ic.p2.wepayu.utils.ValueParser;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Filiação sindical e taxas de serviço (US 5 e parte da US 6).
 *
 * <p>O sindicato identifica seus membros por um id próprio, independente do id da folha,
 * e esse id precisa ser único entre os empregados — regras que só fazem sentido aqui.
 */
public class UnionService {
    private final EmployeeRepository repository;
    private final TransactionManager transactions;

    public UnionService(EmployeeRepository repository, TransactionManager transactions) {
        this.repository = repository;
        this.transactions = transactions;
    }

    /** @param memberId identificação do membro no sindicato (não é o id do empregado). */
    public void postServiceCharge(String memberId, String date, String amount) throws WePayUException {
        transactions.executeVoid(() -> {
            if (Parsers.isBlank(memberId)) {
                throw new InvalidValueException("Identificacao do membro nao pode ser nula.");
            }
            Employee employee = repository.findByUnionId(memberId)
                    .orElseThrow(UnionException::unknownMember);
            BigDecimal value = ValueParser.positive(amount, MonetaryField.AMOUNT);
            employee.addServiceCharge(new ServiceCharge(Parsers.date(date, "Data invalida."), value));
        });
    }

    /** Filia ou desfilia o empregado, conforme {@code flag} seja "true" ou "false". */
    void changeMembership(Employee employee, String flag, String[] extra) throws WePayUException {
        if ("true".equals(flag)) {
            String unionId = extra != null && extra.length > 0 ? extra[0] : null;
            if (Parsers.isBlank(unionId)) {
                throw new InvalidValueException("Identificacao do sindicato nao pode ser nula.");
            }
            BigDecimal fee = ValueParser.nonNegative(
                    extra.length > 1 ? extra[1] : null, MonetaryField.UNION_FEE);
            requireAvailableId(employee, unionId);
            employee.joinUnion(unionId, fee);
        } else if ("false".equals(flag)) {
            employee.leaveUnion();
        } else {
            throw new InvalidValueException("Valor deve ser true ou false.");
        }
    }

    void changeMemberId(Employee employee, String unionId) throws WePayUException {
        requireAvailableId(employee, unionId);
        employee.changeUnionMemberId(unionId);
    }

    void changeFee(Employee employee, String rawFee) throws WePayUException {
        employee.changeUnionFee(ValueParser.nonNegative(rawFee, MonetaryField.UNION_FEE));
    }

    private void requireAvailableId(Employee employee, String unionId) throws WePayUException {
        if (Parsers.isBlank(unionId)) {
            throw new InvalidValueException("Identificacao do sindicato nao pode ser nula.");
        }
        Optional<Employee> owner = repository.findByUnionId(unionId);
        if (owner.isPresent() && !owner.get().getId().equals(employee.getId())) {
            throw UnionException.duplicatedMemberId();
        }
    }
}