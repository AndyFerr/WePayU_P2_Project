package br.ufal.ic.p2.wepayu;

import br.ufal.ic.p2.wepayu.exceptions.StatePersistenceException;
import br.ufal.ic.p2.wepayu.exceptions.SystemClosedException;
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

    /**
     * Falha ocorrida ao carregar o arquivo de dados, se houve. O construtor não pode lançar
     * exceção (quem o instancia é o EasyAccept, por reflexão), então o erro fica guardado e
     * é relatado no primeiro comando — em vez de o sistema simplesmente aparecer vazio.
     */
    private StatePersistenceException loadFailure;

    public Facade() {
        try {
            Optional<byte[]> saved = store.load();
            if (saved.isPresent()) {
                repository.restore(saved.get());
            }
        } catch (StatePersistenceException e) {
            loadFailure = e;
            repository.clear();
        } catch (RuntimeException e) {
            // arquivo existe mas o conteúdo não é um estado válido (corrompido, truncado,
            // ou gravado por outra versão do sistema)
            loadFailure = StatePersistenceException.unreadable(store.describe(), e);
            repository.clear();
        }
    }

    // ---- ciclo de vida ------------------------------------------------------------------------

    /** Também é desfazível: us8.txt faz "undo de zerarSistema" e espera os empregados de volta. */
    public void zerarSistema() throws WePayUException {
        closed = false;
        loadFailure = null; // o usuário pediu explicitamente para começar do zero
        transactions.executeVoid(repository::clear);
    }

    public void encerrarSistema() throws WePayUException {
        ensureStateLoaded();
        store.save(repository.snapshot());
        closed = true;
    }

    // ---- US1 / US2 ----------------------------------------------------------------------------

    public String criarEmpregado(String nome, String endereco, String tipo, String salario) throws WePayUException {
        ensureOpen();
        return employees.createEmployee(nome, endereco, tipo, salario, null);
    }

    public String criarEmpregado(String nome, String endereco, String tipo, String salario, String comissao)
            throws WePayUException {
        ensureOpen();
        return employees.createEmployee(nome, endereco, tipo, salario, comissao);
    }

    public void removerEmpregado(String emp) throws WePayUException {
        ensureOpen();
        employees.removeEmployee(emp);
    }

    public String getAtributoEmpregado(String emp, String atributo) throws WePayUException {
        ensureOpen();
        return employees.getAttribute(emp, atributo);
    }

    public int getNumeroDeEmpregados() throws WePayUException {
        ensureOpen();
        return employees.countEmployees();
    }

    public String getEmpregadoPorNome(String nome, int indice) throws WePayUException {
        ensureOpen();
        return employees.findByName(nome, indice);
    }

    // ---- US3 / US4 / US5 ----------------------------------------------------------------------

    public void lancaCartao(String emp, String data, String horas) throws WePayUException {
        ensureOpen();
        employees.postTimeCard(emp, data, horas);
    }

    public void lancaVenda(String emp, String data, String valor) throws WePayUException {
        ensureOpen();
        employees.postSale(emp, data, valor);
    }

    public void lancaTaxaServico(String membro, String data, String valor) throws WePayUException {
        ensureOpen();
        employees.postServiceCharge(membro, data, valor);
    }

    public String getHorasNormaisTrabalhadas(String emp, String dataInicial, String dataFinal) throws WePayUException {
        ensureOpen();
        return employees.getNormalHours(emp, dataInicial, dataFinal);
    }

    /** O enunciado chama este comando de getHorasTrabalhadas; mantido como alias. */
    public String getHorasTrabalhadas(String emp, String dataInicial, String dataFinal) throws WePayUException {
        return getHorasNormaisTrabalhadas(emp, dataInicial, dataFinal);
    }

    public String getHorasExtrasTrabalhadas(String emp, String dataInicial, String dataFinal) throws WePayUException {
        ensureOpen();
        return employees.getOvertimeHours(emp, dataInicial, dataFinal);
    }

    public String getVendasRealizadas(String emp, String dataInicial, String dataFinal) throws WePayUException {
        ensureOpen();
        return employees.getSales(emp, dataInicial, dataFinal);
    }

    public String getTaxasServico(String emp, String dataInicial, String dataFinal) throws WePayUException {
        ensureOpen();
        return employees.getServiceCharges(emp, dataInicial, dataFinal);
    }

    // ---- US6: o EasyAccept resolve o método pelo número de argumentos ---------------------------

    public void alteraEmpregado(String emp, String atributo, String valor) throws WePayUException {
        ensureOpen();
        employees.updateEmployee(emp, atributo, valor);
    }

    /** tipo=horista salario=..., tipo=comissionado comissao=..., etc. */
    public void alteraEmpregado(String emp, String atributo, String valor, String valor2) throws WePayUException {
        ensureOpen();
        employees.updateEmployee(emp, atributo, valor, valor2);
    }

    /** sindicalizado=true idSindicato=... taxaSindical=... (ou tipo=comissionado salario=... comissao=...). */
    public void alteraEmpregado(String emp, String atributo, String valor, String valor2, String valor3)
            throws WePayUException {
        ensureOpen();
        employees.updateEmployee(emp, atributo, valor, valor2, valor3);
    }

    /** metodoPagamento=banco banco=... agencia=... contaCorrente=... */
    public void alteraEmpregado(String emp, String atributo, String valor, String banco, String agencia,
                                String contaCorrente) throws WePayUException {
        ensureOpen();
        employees.updateEmployee(emp, atributo, valor, banco, agencia, contaCorrente);
    }

    // ---- US9 / US10 -----------------------------------------------------------------------------

    /** valor de agendaPagamento é tratado dentro do alteraEmpregado de 3 argumentos, acima. */
    public void criarAgendaDePagamentos(String descricao) throws WePayUException {
        ensureOpen();
        employees.createSchedule(descricao);
    }

    // ---- US7 ----------------------------------------------------------------------------------

    public void rodaFolha(String data, String saida) throws WePayUException {
        ensureOpen();
        payroll.run(data, saida);
    }

    public String totalFolha(String data) throws WePayUException {
        ensureOpen();
        return payroll.totalPayroll(data);
    }

    // ---- US8 ----------------------------------------------------------------------------------

    public void undo() throws WePayUException {
        ensureOpen();
        transactions.undo();
    }

    public void redo() throws WePayUException {
        ensureOpen();
        transactions.redo();
    }

    private void ensureOpen() throws WePayUException {
        if (closed) {
            throw new SystemClosedException();
        }
        ensureStateLoaded();
    }

    /** Relata, uma vez só, a falha de carregamento guardada pelo construtor. */
    private void ensureStateLoaded() throws StatePersistenceException {
        if (loadFailure != null) {
            StatePersistenceException failure = loadFailure;
            loadFailure = null;
            throw failure;
        }
    }
}