package br.ufal.ic.p2.wepayu.services;

import br.ufal.ic.p2.wepayu.models.CommissionedEmployee;
import br.ufal.ic.p2.wepayu.models.Employee;
import br.ufal.ic.p2.wepayu.models.HourlyEmployee;
import br.ufal.ic.p2.wepayu.models.SalariedEmployee;
import br.ufal.ic.p2.wepayu.utils.Formats;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Gera o arquivo texto da folha (parâmetro "saida" de rodaFolha).
 * ATENÇÃO: us7.txt compara este arquivo com ok/folha-AAAA-MM-DD.txt (equalFiles), byte a byte.
 * Todo o layout (larguras das colunas, títulos, barras, linhas de total) está concentrado aqui e
 * nas constantes abaixo, para ser ajustado aos arquivos esperados.
 */
public class PayrollReportWriter {
    private static final int WIDTH = 127;
    /** Os arquivos esperados (ok/) usam CRLF, inclusive na última linha. */
    private static final String NL = "\r\n";

    // larguras das colunas de cada tabela (a última é o método de pagamento)
    private static final int[] HOURLY = {36, 5, 5, 13, 9, 15, 38};
    private static final int[] SALARIED = {48, 13, 9, 15, 38};
    private static final int[] COMMISSIONED = {21, 8, 8, 8, 13, 9, 15, 38};

    public void write(LocalDate payDay, List<PayrollEntry> entries, Path output) throws IOException {
        StringBuilder out = new StringBuilder();
        String title = "FOLHA DE PAGAMENTO DO DIA " + payDay;
        out.append(title).append(NL).append("=".repeat(title.length())).append(NL + NL);

        hourlySection(out, entries);
        salariedSection(out, entries);
        commissionedSection(out, entries);

        out.append("TOTAL FOLHA: ").append(Formats.money(PayrollService.totalGross(entries))).append(NL);

        if (output.getParent() != null) {
            Files.createDirectories(output.getParent());
        }
        Files.writeString(output, out.toString(), StandardCharsets.UTF_8);
    }

    private void hourlySection(StringBuilder out, List<PayrollEntry> all) {
        List<PayrollEntry> rows = only(all, HourlyEmployee.class);
        section(out, "HORISTAS", HOURLY, new String[]{"Nome", "Horas", "Extra", "Salario Bruto", "Descontos",
                "Salario Liquido", "Metodo"});
        for (PayrollEntry e : rows) {
            HourlyEmployee h = (HourlyEmployee) e.employee();
            LocalDate end = e.payDay().plusDays(1);
            row(out, HOURLY, h.getName(),
                    Formats.hours(h.normalHours(e.periodStart(), end)),
                    Formats.hours(h.overtimeHours(e.periodStart(), end)),
                    Formats.money(e.gross()), Formats.money(e.deductions()), Formats.money(e.net()),
                    method(h));
        }
        out.append(NL);
        row(out, HOURLY, "TOTAL HORISTAS",
                Formats.hours(sum(rows, e -> ((HourlyEmployee) e.employee()).normalHours(e.periodStart(), e.payDay().plusDays(1)))),
                Formats.hours(sum(rows, e -> ((HourlyEmployee) e.employee()).overtimeHours(e.periodStart(), e.payDay().plusDays(1)))),
                Formats.money(sum(rows, PayrollEntry::gross)), Formats.money(sum(rows, PayrollEntry::deductions)),
                Formats.money(sum(rows, PayrollEntry::net)), "");
        out.append(NL);
    }

    private void salariedSection(StringBuilder out, List<PayrollEntry> all) {
        List<PayrollEntry> rows = only(all, SalariedEmployee.class);
        section(out, "ASSALARIADOS", SALARIED,
                new String[]{"Nome", "Salario Bruto", "Descontos", "Salario Liquido", "Metodo"});
        for (PayrollEntry e : rows) {
            row(out, SALARIED, e.employee().getName(), Formats.money(e.gross()), Formats.money(e.deductions()),
                    Formats.money(e.net()), method(e.employee()));
        }
        out.append(NL);
        row(out, SALARIED, "TOTAL ASSALARIADOS", Formats.money(sum(rows, PayrollEntry::gross)),
                Formats.money(sum(rows, PayrollEntry::deductions)), Formats.money(sum(rows, PayrollEntry::net)), "");
        out.append(NL);
    }

    private void commissionedSection(StringBuilder out, List<PayrollEntry> all) {
        List<PayrollEntry> rows = only(all, CommissionedEmployee.class);
        section(out, "COMISSIONADOS", COMMISSIONED, new String[]{"Nome", "Fixo", "Vendas", "Comissao",
                "Salario Bruto", "Descontos", "Salario Liquido", "Metodo"});
        for (PayrollEntry e : rows) {
            CommissionedEmployee c = (CommissionedEmployee) e.employee();
            LocalDate end = e.payDay().plusDays(1);
            row(out, COMMISSIONED, c.getName(), Formats.money(c.fixedPay()),
                    Formats.money(c.salesBetween(e.periodStart(), end)),
                    Formats.money(c.commissionBetween(e.periodStart(), end)),
                    Formats.money(e.gross()), Formats.money(e.deductions()), Formats.money(e.net()), method(c));
        }
        out.append(NL);
        row(out, COMMISSIONED, "TOTAL COMISSIONADOS",
                Formats.money(sum(rows, e -> ((CommissionedEmployee) e.employee()).fixedPay())),
                Formats.money(sum(rows, e -> ((CommissionedEmployee) e.employee()).salesBetween(e.periodStart(), e.payDay().plusDays(1)))),
                Formats.money(sum(rows, e -> ((CommissionedEmployee) e.employee()).commissionBetween(e.periodStart(), e.payDay().plusDays(1)))),
                Formats.money(sum(rows, PayrollEntry::gross)), Formats.money(sum(rows, PayrollEntry::deductions)),
                Formats.money(sum(rows, PayrollEntry::net)), "");
        out.append(NL);
    }

    // ---- helpers de layout ------------------------------------------------------------------

    private static void section(StringBuilder out, String name, int[] widths, String[] headers) {
        String bar = "=".repeat(WIDTH);
        String title = "===================== " + name + " ";
        out.append(bar).append(NL)
                .append(title).append("=".repeat(Math.max(0, WIDTH - title.length()))).append(NL)
                .append(bar).append(NL);
        leftAlignedRow(out, widths, headers);
        String[] separators = new String[widths.length];
        for (int i = 0; i < widths.length; i++) {
            separators[i] = "=".repeat(widths[i]);
        }
        leftAlignedRow(out, widths, separators);
    }

    /** Cabeçalho: todas as colunas alinhadas à esquerda. */
    private static void leftAlignedRow(StringBuilder out, int[] widths, String[] cells) {
        StringBuilder line = new StringBuilder();
        for (int i = 0; i < widths.length; i++) {
            if (i > 0) {
                line.append(' ');
            }
            line.append(i == widths.length - 1 ? cells[i] : String.format("%-" + widths[i] + "s", cells[i]));
        }
        out.append(line.toString().stripTrailing()).append(NL);
    }

    /** Primeira coluna alinhada à esquerda, últimas (método) à esquerda sem preenchimento, demais à direita. */
    private static void row(StringBuilder out, int[] widths, Object... cells) {
        StringBuilder line = new StringBuilder();
        for (int i = 0; i < widths.length; i++) {
            String cell = String.valueOf(cells[i]);
            boolean last = i == widths.length - 1;
            if (i > 0) {
                line.append(' ');
            }
            if (last) {
                line.append(cell);
            } else if (i == 0) {
                line.append(String.format("%-" + widths[i] + "s", cell));
            } else {
                line.append(String.format("%" + widths[i] + "s", cell));
            }
        }
        out.append(line.toString().stripTrailing()).append(NL);
    }

    private static String method(Employee e) {
        return e.getPaymentMethod().describe(e);
    }

    private static List<PayrollEntry> only(List<PayrollEntry> entries, Class<? extends Employee> type) {
        Predicate<PayrollEntry> isType = e -> type.isInstance(e.employee());
        return entries.stream().filter(isType).toList();
    }

    private static BigDecimal sum(List<PayrollEntry> rows, Function<PayrollEntry, BigDecimal> value) {
        return rows.stream().map(value).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
