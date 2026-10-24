package br.ufal.ic.p2.wepayu.models;

import br.ufal.ic.p2.wepayu.utils.DateRange;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Lançamentos de um empregado ao longo do contrato: cartões de ponto e resultados de venda.
 *
 * <p>Fica fora da hierarquia de tipos de propósito. Os lançamentos pertencem à pessoa, não ao
 * regime de pagamento em que ela está hoje: um horista que vira assalariado e depois volta a
 * horista não deixou de ter batido aqueles pontos. Mantendo o histórico aqui, a troca de tipo
 * (US 6) preserva os dados em vez de descartá-los.
 *
 * <p>As listas nunca saem da classe: quem consulta recebe um {@link Stream} já filtrado, que
 * não permite alterar o conteúdo original.
 */

public class EmployeeHistory implements Serializable {
    private final List<TimeCard> timeCards = new ArrayList<>();
    private final List<SalesResult> sales = new ArrayList<>();

    public void addTimeCard(TimeCard card) { timeCards.add(card); }
    public void addSale(SalesResult sale) { sales.add(sale); }

    public boolean hasTimeCards() { return !timeCards.isEmpty(); }

    /** Data do cartão mais antigo; vazio quando nenhum cartão foi lançado. */
    public Optional<LocalDate> firstTimeCardDate() {
        return timeCards.stream().map(TimeCard::date).min(LocalDate::compareTo);
    }

    public Stream<TimeCard> timeCardsBetween(LocalDate from, LocalDate toExclusive) {
        return timeCards.stream().filter(c -> DateRange.contains(c.date(), from, toExclusive));
    }

    public Stream<SalesResult> salesBetween(LocalDate from, LocalDate toExclusive) {
        return sales.stream().filter(s -> DateRange.contains(s.date(), from, toExclusive));
    }
}