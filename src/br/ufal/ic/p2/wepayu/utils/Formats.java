package br.ufal.ic.p2.wepayu.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Formatação de números para o formato brasileiro usado nos scripts de teste. */
public final class Formats {
    private Formats() {
    }

    /** Valor monetário com 2 casas: 2500 -> "2500,00". */
    public static String money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP).toPlainString().replace('.', ',');
    }

    /** Taxas/percentuais: no mínimo 2 casas, mas sem perder precisão (0,055 -> "0,055"). */
    public static String rate(BigDecimal value) {
        int scale = Math.max(2, value.stripTrailingZeros().scale());
        return value.setScale(scale, RoundingMode.HALF_UP).toPlainString().replace('.', ',');
    }

    /** Horas sem zeros à direita: 8 -> "8", 7.50 -> "7,5". */
    public static String hours(BigDecimal value) {
        BigDecimal stripped = value.stripTrailingZeros();
        if (stripped.scale() < 0) {
            stripped = stripped.setScale(0);
        }
        return stripped.toPlainString().replace('.', ',');
    }
}
