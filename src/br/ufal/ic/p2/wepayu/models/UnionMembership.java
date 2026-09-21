package br.ufal.ic.p2.wepayu.models;

import br.ufal.ic.p2.wepayu.utils.DateRange;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Filiação de um empregado ao sindicato. O id do sindicato é diferente do id
 * do empregado na folha.
 */
public class UnionMembership implements Serializable {
    private String memberId;
    private BigDecimal fee;
    private final List<ServiceCharge> charges = new ArrayList<>();

    public UnionMembership(String memberId, BigDecimal fee) {
        this.memberId = memberId;
        this.fee = fee;
    }

    public String getMemberId() {
        return memberId;
    }

    public void setMemberId(String memberId) {
        this.memberId = memberId;
    }

    /** Taxa sindical cobrada por dia do período pago. */
    public BigDecimal getFee() {
        return fee;
    }

    public void setFee(BigDecimal fee) {
        this.fee = fee;
    }

    public void addCharge(ServiceCharge charge) {
        charges.add(charge);
    }

    public BigDecimal chargesBetween(LocalDate from, LocalDate toExclusive) {
        return charges.stream()
                .filter(c -> DateRange.contains(c.date(), from, toExclusive))
                .map(ServiceCharge::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
