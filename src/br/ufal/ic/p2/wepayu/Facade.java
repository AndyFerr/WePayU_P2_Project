package br.ufal.ic.p2.wepayu;

import br.ufal.ic.p2.wepayu.exceptions.WePayUException;
import br.ufal.ic.p2.wepayu.persistence.FileStateStore;
import br.ufal.ic.p2.wepayu.services.EmployeeRepository;
import br.ufal.ic.p2.wepayu.services.EmployeeService;
import br.ufal.ic.p2.wepayu.services.PayrollService;
import br.ufal.ic.p2.wepayu.services.TransactionManager;

import java.nio.file.Path;
import java.util.Optional;

/**
 * Fachada usada pelo EasyAccept: um método por comando do script de testes, todos com
 * parâmetros String (exceto {@code indice}). Não contém regra de negócio; só traduz o
 * "vocabulário" dos testes para chamadas aos serviços.
 */
public class Facade {
    private static final String STATE_FILE = "wepayu-state.bin";

    private final EmployeeRepository repository = new EmployeeRepository();
    private final TransactionManager transactions = new TransactionManager(repository);
    private final EmployeeService employees = new EmployeeService(repository, transactions);
    private final PayrollService payroll = new PayrollService(repository, transactions);
    private final FileStateStore store = new FileStateStore(Path.of(STATE_FILE));
    private boolean closed;

    public Facade() {
        try {
            Optional<byte[]> saved = store.load();
            if (saved.isPresent()) {
                repository.restore(saved.get());
            }
        } catch (Exception e) {
            repository.clear(); // arquivo ausente/corrompido: começa vazio
        }
    }

    // ---- ciclo de vida ------------------------------------------------------------------------

    /** Também é desfazível: us8.txt faz "undo de zerarSistema" e espera os empregados de volta. */
    public void zerarSistema() throws Exception {
        closed = false;
        transactions.executeVoid(repository::clear);
    }

    public void encerrarSistema() throws Exception {
        store.save(repository.snapshot());
        closed = true;
    }

    // ---- US1 / US2 ----------------------------------------------------------------------------

    public String criarEmpregado(String nome, String endereco, String tipo, String salario) throws Exception {
        ensureOpen();
        return employees.createEmployee(nome, endereco, tipo, salario, null);
    }

    public String criarEmpregado(String nome, String endereco, String tipo, String salario, String comissao)
            throws Exception {
        ensureOpen();
        return employees.createEmployee(nome, endereco, tipo, salario, comissao);
    }

    public void removerEmpregado(String emp) throws Exception {
        ensureOpen();
        employees.removeEmployee(emp);
    }

    public String getAtributoEmpregado(String emp, String atributo) throws Exception {
        ensureOpen();
        return employees.getAttribute(emp, atributo);
    }

    public int getNumeroDeEmpregados() throws Exception {
        ensureOpen();
        return employees.countEmployees();
    }

    public String getEmpregadoPorNome(String nome, int indice) throws Exception {
        ensureOpen();
        return employees.findByName(nome, indice);
    }

    // ---- US3 / US4 / US5 ----------------------------------------------------------------------

    public void lancaCartao(String emp, String data, String horas) throws Exception {
        ensureOpen();
        employees.postTimeCard(emp, data, horas);
    }

    public void lancaVenda(String emp, String data, String valor) throws Exception {
        ensureOpen();
        employees.postSale(emp, data, valor);
    }

    public void lancaTaxaServico(String membro, String data, String valor) throws Exception {
        ensureOpen();
        employees.postServiceCharge(membro, data, valor);
    }

    public String getHorasNormaisTrabalhadas(String emp, String dataInicial, String dataFinal) throws Exception {
        ensureOpen();
        return employees.getNormalHours(emp, dataInicial, dataFinal);
    }

    /** O enunciado chama este comando de getHorasTrabalhadas; mantido como alias. */
    public String getHorasTrabalhadas(String emp, String dataInicial, String dataFinal) throws Exception {
        return getHorasNormaisTrabalhadas(emp, dataInicial, dataFinal);
    }

    public String getHorasExtrasTrabalhadas(String emp, String dataInicial, String dataFinal) throws Exception {
        ensureOpen();
        return employees.getOvertimeHours(emp, dataInicial, dataFinal);
    }

    public String getVendasRealizadas(String emp, String dataInicial, String dataFinal) throws Exception {
        ensureOpen();
        return employees.getSales(emp, dataInicial, dataFinal);
    }

    public String getTaxasServico(String emp, String dataInicial, String dataFinal) throws Exception {
        ensureOpen();
        return employees.getServiceCharges(emp, dataInicial, dataFinal);
    }

    // ---- US6: o EasyAccept resolve o método pelo número de argumentos ---------------------------

    public void alteraEmpregado(String emp, String atributo, String valor) throws Exception {
        ensureOpen();
        employees.updateEmployee(emp, atributo, valor);
    }

    /** tipo=horista salario=..., tipo=comissionado comissao=..., etc. */
    public void alteraEmpregado(String emp, String atributo, String valor, String valor2) throws Exception {
        ensureOpen();
        employees.updateEmployee(emp, atributo, valor, valor2);
    }

    /** sindicalizado=true idSindicato=... taxaSindical=... (ou tipo=comissionado salario=... comissao=...). */
    public void alteraEmpregado(String emp, String atributo, String valor, String valor2, String valor3)
            throws Exception {
        ensureOpen();
        employees.updateEmployee(emp, atributo, valor, valor2, valor3);
    }

    /** metodoPagamento=banco banco=... agencia=... contaCorrente=... */
    public void alteraEmpregado(String emp, String atributo, String valor, String banco, String agencia,
                                String contaCorrente) throws Exception {
        ensureOpen();
        employees.updateEmployee(emp, atributo, valor, banco, agencia, contaCorrente);
    }

    // ---- US7 ----------------------------------------------------------------------------------

    public void rodaFolha(String data, String saida) throws Exception {
        ensureOpen();
        payroll.run(data, saida);
    }

    public String totalFolha(String data) throws Exception {
        ensureOpen();
        return payroll.totalPayroll(data);
    }

    // ---- US8 ----------------------------------------------------------------------------------

    public void undo() throws Exception {
        ensureOpen();
        transactions.undo();
    }

    public void redo() throws Exception {
        ensureOpen();
        transactions.redo();
    }

    private void ensureOpen() throws WePayUException {
        if (closed) {
            throw new WePayUException("Nao pode dar comandos depois de encerrarSistema.");
        }
    }
}
