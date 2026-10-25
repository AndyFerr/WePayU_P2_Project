package br.ufal.ic.p2.wepayu.services;

import br.ufal.ic.p2.wepayu.exceptions.WePayUException;
import br.ufal.ic.p2.wepayu.models.CommissionedEmployee;
import br.ufal.ic.p2.wepayu.models.HourlyEmployee;
import br.ufal.ic.p2.wepayu.models.SalesResult;
import br.ufal.ic.p2.wepayu.models.TimeCard;
import br.ufal.ic.p2.wepayu.persistence.TransactionManager;
import br.ufal.ic.p2.wepayu.utils.MonetaryField;
import br.ufal.ic.p2.wepayu.utils.Parsers;
import br.ufal.ic.p2.wepayu.utils.ValueParser;

import java.math.BigDecimal;

/**
 * Lançamentos do que foi trabalhado ou vendido (US 3 e 4): cartões de ponto do horista e
 * resultados de venda do comissionado.
 */
public class TimeSheetService {
    private final TransactionManager transactions;
    private final EmployeeFinder finder;

    public TimeSheetService(TransactionManager transactions, EmployeeFinder finder) {
        this.transactions = transactions;
        this.finder = finder;
    }

    public void postTimeCard(String id, String date, String hours) throws WePayUException {
        transactions.executeVoid(() -> {
            HourlyEmployee employee = finder.hourlyById(id);
            BigDecimal hoursValue = ValueParser.positive(hours, MonetaryField.HOURS);
            employee.addTimeCard(new TimeCard(Parsers.date(date, "Data invalida."), hoursValue));
        });
    }

    public void postSale(String id, String date, String amount) throws WePayUException {
        transactions.executeVoid(() -> {
            CommissionedEmployee employee = finder.commissionedById(id);
            BigDecimal value = ValueParser.positive(amount, MonetaryField.AMOUNT);
            employee.addSale(new SalesResult(Parsers.date(date, "Data invalida."), value));
        });
    }
}