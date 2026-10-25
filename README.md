# WePayU — Sistema de Folha de Pagamento

Implementação das User Stories 1 a 10 do projeto de P2. Sem interface gráfica: o sistema é
exercitado pelos scripts de aceitação do EasyAccept, que conversam com a fachada
`br.ufal.ic.p2.wepayu.Facade`.

## Como rodar os testes

Pela classe `Main`, que executa todos os scripts na ordem das User Stories:

    javac -d out -cp lib/easyaccept.jar $(find src -name '*.java')
    java -cp out:lib/easyaccept.jar br.ufal.ic.p2.wepayu.Main

Ou um script isolado:

    java -cp out:lib/easyaccept.jar easyaccept.EasyAccept br.ufal.ic.p2.wepayu.Facade tests/us7.txt

Cada script `usN_1` lê o estado gravado pelo `usN` correspondente, então os dois precisam
rodar em execuções separadas da JVM para que a persistência seja de fato exercitada.
Apagar `wepayu-state.bin` reinicia o sistema do zero.

## Organização

    Facade                        traduz os comandos do EasyAccept em chamadas de serviço;
                                  sem regra de negócio, uma linha por comando

    services/                     casos de uso, um por assunto
      SystemService               ciclo de vida: carregar, gravar, zerar, encerrar, undo/redo
      EmployeeRegistrationService US 1, 2 e troca de regime: quem entra, quem sai, como é pago
      EmployeeQueryService        consultas (não alteram estado, não entram no undo/redo)
      TimeSheetService            US 3 e 4: cartões de ponto e resultados de venda
      UnionService                US 5: filiação sindical e taxas de serviço
      ScheduleService             US 9 e 10: agendas disponíveis e qual paga cada empregado
      EmployeeUpdateService       US 6: recebe alteraEmpregado e encaminha ao dono do atributo
      PayrollService              US 7: quem é pago na data, bruto e descontos do período
      EmployeeFinder              busca de empregado com as checagens que os casos de uso repetiam

    models/                       o domínio
      Employee                    raiz; Hourly / Salaried / Commissioned calculam o próprio bruto
      EmployeeHistory             cartões e vendas do empregado, preservados na troca de regime
      UnionMembership             filiação; o id do sindicato é independente do id da folha
      TimeCard, SalesResult, ServiceCharge
      payment/                    PaymentMethod (Strategy) + Hand / Mail / Bank

    schedule/                     PaymentSchedule (Strategy) + Weekly / Monthly + ScheduleParser

    persistence/                  o estado do sistema
      EmployeeRepository          único dono do estado; snapshot/restore
      TransactionManager          US 8: undo/redo e rollback quando um comando falha
      FileStateStore              leitura e gravação do arquivo de dados

    report/                       saída da folha
      PayrollEntry                o contracheque de um empregado em uma data
      PayrollReportWriter         layout do arquivo gerado por rodaFolha

    exceptions/                   uma classe por assunto, todas sob WePayUException (abstrata)

    utils/                        Parsers, ValueParser, MonetaryField, Formats, DateRange, Require

## Decisões que valem explicar

**Cada tipo de empregado e cada agenda sabem se calcular.** Rodar a folha não tem `if` por
tipo: o serviço pergunta ao empregado qual o bruto do período, e à agenda se o dia é de
pagamento. Tipos e agendas novos entram sem tocar no cálculo.

**Os lançamentos pertencem à pessoa, não ao regime.** Cartões de ponto e vendas ficam em
`EmployeeHistory`, fora da hierarquia de tipos, para que trocar de horista para assalariado
e voltar não apague o que já foi lançado.

**Dois níveis de validação.** Os serviços validam a entrada do usuário e produzem
`InvalidValueException` com as mensagens que o enunciado define. Os objetos de domínio
validam os próprios invariantes com `Require` e lançam `IllegalArgumentException` — ali, um
valor inválido só chega por bug, já que a entrada passou pela validação anterior.

**Undo/redo por snapshot.** O `TransactionManager` copia o estado antes de cada comando: se
ele falha, o estado volta; se dá certo, a cópia vai para a pilha de undo. O custo cresce com
o número de empregados, o que não é problema nesta escala, e em troca nenhum comando precisa
saber como se desfazer.

**Um arquivo de dados ilegível não passa em silêncio.** Arquivo ausente é sistema novo;
arquivo presente que não pôde ser lido vira `StatePersistenceException`, relatada no primeiro
comando. O arquivo é preservado, e `zerarSistema` libera o caminho.