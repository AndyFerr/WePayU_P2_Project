package br.ufal.ic.p2.wepayu.report;

import br.ufal.ic.p2.wepayu.persistence.EmployeeRepository;
import br.ufal.ic.p2.wepayu.persistence.TransactionManager;
import br.ufal.ic.p2.wepayu.exceptions.InvalidValueException;
import br.ufal.ic.p2.wepayu.exceptions.ReportWriteException;
import br.ufal.ic.p2.wepayu.exceptions.WePayUException;
import br.ufal.ic.p2.wepayu.models.Employee;
import br.ufal.ic.p2.wepayu.schedule.PaymentSchedule;
import br.ufal.ic.p2.wepayu.utils.Formats;
import br.ufal.ic.p2.wepayu.utils.Parsers;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * US7: descobre quem é pago em uma data (pela agenda de cada empregado),
 * calcula bruto e descontos do período e gera o relatório.
 * Não altera o estado, portanto não participa do undo/redo.
 */
public class PayrollService {
    private final EmployeeRepository repository;
    private final TransactionManager transactions;
    private final PayrollReportWriter writer = new PayrollReportWriter();

    public PayrollService(EmployeeRepository repository, TransactionManager transactions) {
        this.repository = repository;
        this.transactions = transactions;
    }

    public List<PayrollEntry> entriesFor(LocalDate payDay) {
        List<PayrollEntry> entries = new ArrayList<>();
        for (Employee employee : repository.findAll()) {
            PaymentSchedule schedule = employee.getSchedule();
            if (!schedule.isPayDay(payDay)) {
                continue;
            }
            LocalDate from = schedule.periodStart(payDay);
            LocalDate toExclusive = payDay.plusDays(1);
            entries.add(new PayrollEntry(employee, from, payDay,
                    employee.grossPay(from, toExclusive), employee.payrollDeductions(payDay)));
        }
        entries.sort(Comparator.comparing((PayrollEntry e) -> e.employee().getName()));
        return entries;
    }

    /** Total de salários brutos pagos na data. */
    public String totalPayroll(String date) throws InvalidValueException {
        return Formats.money(PayrollEntry.totalGross(entriesFor(Parsers.date(date, "Data invalida."))));
    }

    /**
     * Roda a folha. Não muda o estado dos empregados, mas o enunciado de US 8 trata rodaFolha
     * como um comando desfazível (us8.txt faz "undo de rodaFolha"), então passa pelo histórico.
     */
    public void run(String date, String outputFile) throws WePayUException {
        transactions.executeVoid(() -> {
            LocalDate payDay = Parsers.date(date, "Data invalida.");
            List<PayrollEntry> entries = entriesFor(payDay);
            try {
                writer.write(payDay, entries, Path.of(outputFile));
            } catch (IOException e) {
                throw new ReportWriteException(outputFile, e);
            }
        });
    }
}