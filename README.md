# WePayU – Milestone 1 (base)

Pacote raiz: `br.ufal.ic.p2.wepayu` (fachada em `br.ufal.ic.p2.wepayu.Facade`).

    Facade                  -> traduz os comandos do EasyAccept (sem regra de negócio)
    services/
      EmployeeService       -> US 1-6 (criar, remover, lançar, consultar, alterar)
      PayrollService        -> US 7 (quem é pago hoje, bruto, descontos, total)
      PayrollReportWriter   -> layout do arquivo gerado por rodaFolha  <- ajustar aos arquivos esperados
      TransactionManager    -> US 8 (undo/redo + rollback em caso de erro)
      EmployeeRepository    -> único dono do estado; snapshot/restore
    models/                 -> Employee (Hourly/Salaried/Commissioned), PaymentMethod (Hand/Mail/Bank),
                               UnionMembership, TimeCard, SalesResult, ServiceCharge
    schedule/               -> PaymentSchedule (Weekly/Monthly) — ponto de extensão das US 9 e 10
    persistence/            -> FileStateStore (arquivo com o estado; `encerrarSistema` grava)
    utils/                  -> Parsers (datas/decimais), Formats (saída no formato dos testes), DateRange
    exceptions/             -> WePayUException (mensagem = texto que o expectError compara)

Requer Java 17+.

## Rodando os testes

Com o EasyAccept: use o seu `Main` (a fachada é `br.ufal.ic.p2.wepayu.Facade`).
`us*_1.txt` precisam rodar depois do `us*.txt` correspondente, pois leem o arquivo `wepayu-state.bin`
gravado por `encerrarSistema` no diretório de execução.

Sem o jar do EasyAccept, `ScriptRunner` interpreta os mesmos scripts (só a sintaxe usada em us1..us8):

    javac -d out $(find src -name '*.java')
    java -cp out br.ufal.ic.p2.wepayu.ScriptRunner tests/us1.txt tests/us1_1.txt tests/us2.txt ...

`equalFiles` compara byte a byte com `ok/folha-*.txt` (CRLF); é ignorado quando o arquivo esperado não existe.
