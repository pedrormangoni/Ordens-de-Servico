package br.upf.serviceorders.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class MonetaryAmounts {

    public static final BigDecimal MAX = new BigDecimal("99999999.99");

    private MonetaryAmounts() {
    }

    public static BigDecimal normalize(BigDecimal value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    public static boolean isWithinLimit(BigDecimal value) {
        return value != null
                && value.compareTo(BigDecimal.ZERO) >= 0
                && value.compareTo(MAX) <= 0;
    }

    public static boolean isPositiveWithinLimit(BigDecimal value) {
        return value != null
                && value.compareTo(BigDecimal.ZERO) > 0
                && value.compareTo(MAX) <= 0;
    }

    public static BigDecimal lineTotal(BigDecimal unitPrice, BigDecimal quantity) {
        return normalize(normalize(unitPrice).multiply(normalize(quantity)));
    }

    public static boolean isLineTotalWithinLimit(BigDecimal unitPrice, BigDecimal quantity) {
        return isWithinLimit(lineTotal(unitPrice, quantity));
    }

    public static boolean isNumericOverflow(Throwable cause) {
        while (cause != null) {
            String message = cause.getMessage();
            if (message != null && (message.contains("estouro de campo numeric")
                    || message.contains("numeric field overflow")
                    || message.contains("22003"))) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }
}
