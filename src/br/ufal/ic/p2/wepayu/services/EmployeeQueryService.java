package br.ufal.ic.p2.wepayu.services;

import br.ufal.ic.p2.wepayu.exceptions.WePayUException;
import br.ufal.ic.p2.wepayu.models.CommissionedEmployee;
import br.ufal.ic.p2.wepayu.models.Employee;
import br.ufal.ic.p2.wepayu.models.HourlyEmployee;
import br.ufal.ic.p2.wepayu.utils.Formats;
import br.ufal.ic.p2.wepayu.utils.ValueParser;
import br.ufal.ic.p2.wepayu.utils.ValueParser.Period;

/**
 * Consultas sobre empregados: nenhuma delas altera o sistema, portanto nenhuma passa
 * pelo {@link TransactionManager} nem entra no undo/redo.
 */
public class EmployeeQueryService {
    private final EmployeeRepository repository;
    private final EmployeeFinder finder;

    public EmployeeQueryService(EmployeeRepository repository, EmployeeFinder finder) {
        this.repository = repository;
        this.finder = finder;
    }

    public int count() {
        return repository.findAll().size();
    }

    public String attributeOf(String id, String attribute) throws WePayUException {
        return finder.byId(id).getAttribute(attribute);
    }

    public String idByName(String name, int index) throws WePayUException {
        return finder.byName(name, index).getId();
    }

    public String normalHours(String id, String start, String end) throws WePayUException {
        HourlyEmployee employee = finder.hourlyById(id);
        Period period = ValueParser.period(start, end);
        return Formats.hours(employee.normalHours(period.from(), period.toExclusive()));
    }

    public String overtimeHours(String id, String start, String end) throws WePayUException {
        HourlyEmployee employee = finder.hourlyById(id);
        Period period = ValueParser.period(start, end);
        return Formats.hours(employee.overtimeHours(period.from(), period.toExclusive()));
    }

    public String sales(String id, String start, String end) throws WePayUException {
        CommissionedEmployee employee = finder.commissionedById(id);
        Period period = ValueParser.period(start, end);
        return Formats.money(employee.salesBetween(period.from(), period.toExclusive()));
    }

    public String serviceCharges(String id, String start, String end) throws WePayUException {
        Employee employee = finder.byId(id);
        Period period = ValueParser.period(start, end);
        return Formats.money(employee.serviceChargesBetween(period.from(), period.toExclusive()));
    }
}