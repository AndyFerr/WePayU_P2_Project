package br.ufal.ic.p2.wepayu.services;

import br.ufal.ic.p2.wepayu.exceptions.ScheduleException;
import br.ufal.ic.p2.wepayu.exceptions.WePayUException;
import br.ufal.ic.p2.wepayu.models.Employee;
import br.ufal.ic.p2.wepayu.schedule.PaymentSchedule;
import br.ufal.ic.p2.wepayu.schedule.ScheduleParser;

/** Agendas de pagamento (US 9 e 10): quais existem e qual delas paga cada empregado. */
public class ScheduleService {
    private final EmployeeRepository repository;
    private final TransactionManager transactions;

    public ScheduleService(EmployeeRepository repository, TransactionManager transactions) {
        this.repository = repository;
        this.transactions = transactions;
    }

    /** US10: registra uma nova agenda a partir de uma descrição como "semanal 2 3" ou "mensal 1". */
    public void create(String description) throws WePayUException {
        transactions.executeVoid(() -> {
            PaymentSchedule schedule = ScheduleParser.parse(description);
            if (repository.findSchedule(schedule.getDescription()).isPresent()) {
                throw ScheduleException.alreadyExists();
            }
            repository.addSchedule(schedule);
        });
    }

    void assign(Employee employee, String description) throws WePayUException {
        PaymentSchedule schedule = repository.findSchedule(description)
                .orElseThrow(ScheduleException::notAvailable);
        employee.setSchedule(schedule);
    }
}