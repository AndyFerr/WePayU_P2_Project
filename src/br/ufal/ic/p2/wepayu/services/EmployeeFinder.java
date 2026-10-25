package br.ufal.ic.p2.wepayu.services;

import br.ufal.ic.p2.wepayu.exceptions.EmployeeNotFoundException;
import br.ufal.ic.p2.wepayu.exceptions.EmployeeTypeMismatchException;
import br.ufal.ic.p2.wepayu.models.CommissionedEmployee;
import br.ufal.ic.p2.wepayu.models.Employee;
import br.ufal.ic.p2.wepayu.models.HourlyEmployee;
import br.ufal.ic.p2.wepayu.utils.Parsers;

/**
 * Busca de empregados com as checagens que todos os casos de uso repetem: id informado,
 * empregado existente e, quando a operação exige, o tipo certo.
 *
 * <p>Existe para que os serviços de cadastro, lançamentos, sindicato e consultas não
 * precisem cada um ter a sua cópia dessas três verificações.
 */
public class EmployeeFinder {
    private final EmployeeRepository repository;

    public EmployeeFinder(EmployeeRepository repository) {
        this.repository = repository;
    }

    public Employee byId(String id) throws EmployeeNotFoundException {
        if (Parsers.isBlank(id)) {
            throw EmployeeNotFoundException.missingId();
        }
        return repository.findById(id).orElseThrow(EmployeeNotFoundException::unknownId);
    }

    public HourlyEmployee hourlyById(String id) throws EmployeeNotFoundException, EmployeeTypeMismatchException {
        if (byId(id) instanceof HourlyEmployee hourly) {
            return hourly;
        }
        throw EmployeeTypeMismatchException.notHourly();
    }

    public CommissionedEmployee commissionedById(String id)
            throws EmployeeNotFoundException, EmployeeTypeMismatchException {
        if (byId(id) instanceof CommissionedEmployee commissioned) {
            return commissioned;
        }
        throw EmployeeTypeMismatchException.notCommissioned();
    }

    /** @param index posição (a partir de 1) entre os empregados com esse nome, na ordem de criação. */
    public Employee byName(String name, int index) throws EmployeeNotFoundException {
        var matches = repository.findByName(name);
        if (index < 1 || index > matches.size()) {
            throw EmployeeNotFoundException.unknownName();
        }
        return matches.get(index - 1);
    }
}