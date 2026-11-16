package com.capitalgains.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RoundingTest {

    private static BigDecimal dec(String value) {
        return new BigDecimal(value);
    }

    @Test
    void repeatingDecimalAverageRealizesZeroPointSixSeven() {
        // DOMAIN_RULES §5: average 100/3 = 33.333333; 34.00 - 33.333333 = 0.666667 -> 0.67
        AssetPosition position = new AssetPosition();
        position.buy(dec("20.00"), 2);
        position.buy(dec("60.00"), 1);

        BigDecimal realized = position.sell(dec("34.00"), 1);

        assertEquals(dec("0.67"), realized);
    }

    @Test
    void realizedIsRoundedOncePerSaleNotPerUnit() {
        // exact 0.005 * 3 = 0.015 -> 0.02; rounding per unit would yield 0.03
        AssetPosition position = new AssetPosition();
        position.buy(dec("10.00"), 3);

        BigDecimal realized = position.sell(dec("10.005"), 3);

        assertEquals(dec("0.02"), realized);
    }

    @Test
    void negativeHalfIsRoundedAwayFromZero() {
        AssetPosition position = new AssetPosition();
        position.buy(dec("10.00"), 1);

        BigDecimal realized = position.sell(dec("9.995"), 1);

        assertEquals(dec("-0.01"), realized);
    }
}
