package br.ufal.ic.p2.wepayu;

import br.ufal.ic.p2.wepayu.exceptions.WePayUException;
import br.ufal.ic.p2.wepayu.persistence.FileStateStore;
import br.ufal.ic.p2.wepayu.services.EmployeeFinder;
import br.ufal.ic.p2.wepayu.services.EmployeeQueryService;
import br.ufal.ic.p2.wepayu.services.EmployeeRegistrationService;
import br.ufal.ic.p2.wepayu.services.EmployeeRepository;
import br.ufal.ic.p2.wepayu.services.EmployeeUpdateService;
import br.ufal.ic.p2.wepayu.services.PayrollService;
import br.ufal.ic.p2.wepayu.services.ScheduleService;
import br.ufal.ic.p2.wepayu.services.SystemService;
import br.ufal.ic.p2.wepayu.services.TimeSheetService;
import br.ufal.ic.p2.wepayu.services.TransactionManager;
import br.ufal.ic.p2.wepayu.services.UnionService;

import java.nio.file.Path;

/**
 * Fachada usada pelo EasyAccept: um método por comando do script de testes, todos com
 * parâmetros String (exceto {@code indice}).
 *
 * <p>Não contém regra de negócio nenhuma. Cada método traduz o "vocabulário" dos testes
 * para uma chamada de serviço, dentro de {@link SystemService#run}, que é quem sabe se o
 * sistema ainda aceita comandos.
 */
public class Facade {
    private static final String STATE_FILE = "wepayu-state.bin";

    private final EmployeeRepository repository = new EmployeeRepository();
    private final TransactionManager transactions = new TransactionManager(repository);
    private final EmployeeFinder finder = new EmployeeFinder(repository);

    private final SystemService system =
            new SystemService(repository, transactions, new FileStateStore(Path.of(STATE_FILE)));
    private final EmployeeRegistrationService registration =
            new EmployeeRegistrationService(repository, transactions, finder);
    private final EmployeeQueryService queries = new EmployeeQueryService(repository, finder);
    private final TimeSheetService timeSheets = new TimeSheetService(transactions, finder);
    private final UnionService union = new UnionService(repository, transactions);
    private final ScheduleService schedules = new ScheduleService(repository, transactions);
    private final EmployeeUpdateService updates =
            new EmployeeUpdateService(transactions, finder, registration, union, schedules);
    private final PayrollService payroll = new PayrollService(repository, transactions);

    // ---- ciclo de vida ------------------------------------------------------------------------

    public void zerarSistema() throws WePayUException {
        system.reset();
    }

    public void encerrarSistema() throws WePayUException {
        system.close();
    }

    // ---- US1 / US2 ----------------------------------------------------------------------------

    public String criarEmpregado(String nome, String endereco, String tipo, String salario) throws WePayUException {
        return system.run(() -> registration.create(nome, endereco, tipo, salario, null));
    }

    public String criarEmpregado(String nome, String endereco, String tipo, String salario, String comissao)
            throws WePayUException {
        return system.run(() -> registration.create(nome, endereco, tipo, salario, comissao));
    }

    public void removerEmpregado(String emp) throws WePayUException {
        system.run(() -> registration.remove(emp));
    }

    public String getAtributoEmpregado(String emp, String atributo) throws WePayUException {
        return system.run(() -> queries.attributeOf(emp, atributo));
    }

    public int getNumeroDeEmpregados() throws WePayUException {
        return system.run(queries::count);
    }

    public String getEmpregadoPorNome(String nome, int indice) throws WePayUException {
        return system.run(() -> queries.idByName(nome, indice));
    }

    // ---- US3 / US4 / US5 ----------------------------------------------------------------------

    public void lancaCartao(String emp, String data, String horas) throws WePayUException {
        system.run(() -> timeSheets.postTimeCard(emp, data, horas));
    }

    public void lancaVenda(String emp, String data, String valor) throws WePayUException {
        system.run(() -> timeSheets.postSale(emp, data, valor));
    }

    public void lancaTaxaServico(String membro, String data, String valor) throws WePayUException {
        system.run(() -> union.postServiceCharge(membro, data, valor));
    }

    public String getHorasNormaisTrabalhadas(String emp, String dataInicial, String dataFinal)
            throws WePayUException {
        return system.run(() -> queries.normalHours(emp, dataInicial, dataFinal));
    }

    /** O enunciado chama este comando de getHorasTrabalhadas; mantido como alias. */
    public String getHorasTrabalhadas(String emp, String dataInicial, String dataFinal) throws WePayUException {
        return getHorasNormaisTrabalhadas(emp, dataInicial, dataFinal);
    }

    public String getHorasExtrasTrabalhadas(String emp, String dataInicial, String dataFinal)
            throws WePayUException {
        return system.run(() -> queries.overtimeHours(emp, dataInicial, dataFinal));
    }

    public String getVendasRealizadas(String emp, String dataInicial, String dataFinal) throws WePayUException {
        return system.run(() -> queries.sales(emp, dataInicial, dataFinal));
    }

    public String getTaxasServico(String emp, String dataInicial, String dataFinal) throws WePayUException {
        return system.run(() -> queries.serviceCharges(emp, dataInicial, dataFinal));
    }

    // ---- US6: o EasyAccept resolve o método pelo número de argumentos ---------------------------

    public void alteraEmpregado(String emp, String atributo, String valor) throws WePayUException {
        system.run(() -> updates.update(emp, atributo, valor));
    }

    /** tipo=horista salario=..., tipo=comissionado comissao=..., etc. */
    public void alteraEmpregado(String emp, String atributo, String valor, String valor2) throws WePayUException {
        system.run(() -> updates.update(emp, atributo, valor, valor2));
    }

    /** sindicalizado=true idSindicato=... taxaSindical=... (ou tipo=comissionado salario=... comissao=...). */
    public void alteraEmpregado(String emp, String atributo, String valor, String valor2, String valor3)
            throws WePayUException {
        system.run(() -> updates.update(emp, atributo, valor, valor2, valor3));
    }

    /** metodoPagamento=banco banco=... agencia=... contaCorrente=... */
    public void alteraEmpregado(String emp, String atributo, String valor, String banco, String agencia,
                                String contaCorrente) throws WePayUException {
        system.run(() -> updates.update(emp, atributo, valor, banco, agencia, contaCorrente));
    }

    // ---- US9 / US10 -----------------------------------------------------------------------------

    /** valor de agendaPagamento é tratado dentro do alteraEmpregado de 3 argumentos, acima. */
    public void criarAgendaDePagamentos(String descricao) throws WePayUException {
        system.run(() -> schedules.create(descricao));
    }

    // ---- US7 ----------------------------------------------------------------------------------

    public void rodaFolha(String data, String saida) throws WePayUException {
        system.run(() -> payroll.run(data, saida));
    }

    public String totalFolha(String data) throws WePayUException {
        return system.run(() -> payroll.totalPayroll(data));
    }

    // ---- US8 ----------------------------------------------------------------------------------

    public void undo() throws WePayUException {
        system.undo();
    }

    public void redo() throws WePayUException {
        system.redo();
    }
}