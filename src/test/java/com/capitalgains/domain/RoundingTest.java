package com.capitalgains.domain;

import com.capitalgains.tax.SimulatedTaxEngine;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RoundingTest {

    private final SimulatedTaxEngine taxEngine = new SimulatedTaxEngine();

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

    @ParameterizedTest
    @CsvSource({
            "10.00, 1, 10.00, 1, 10.000000",
            "1.00, 3, 2.00, 3, 1.500000",
            "20.00, 2, 60.00, 1, 33.333333",
            "0.000001, 1, 0.000002, 1, 0.000002",
            "0.000002, 1, 0.000003, 1, 0.000003",
            "0.000001, 3, 0.000004, 1, 0.000002"
    })
    void weightedAverageRoundsHalfUpAtScaleSix(
            String firstPrice, long firstQuantity,
            String secondPrice, long secondQuantity,
            String expectedAverage) {
        AssetPosition position = new AssetPosition();
        position.buy(dec(firstPrice), firstQuantity);

        position.buy(dec(secondPrice), secondQuantity);

        assertEquals(dec(expectedAverage), position.averageCost());
    }

    @ParameterizedTest
    @CsvSource({
            "10.00, 1, 10.005, 1, 0.01",
            "10.00, 3, 10.005, 3, 0.02",
            "10.00, 1, 10.004, 1, 0.00",
            "10.00, 1, 10.006, 1, 0.01",
            "10.00, 1, 9.995, 1, -0.01",
            "10.00, 1, 9.999, 1, 0.00",
            "20.00, 1, 20.01, 1, 0.01",
            "1.234567, 1, 1.234568, 1, 0.00",
            "0.000001, 1000000000, 0.000002, 1000000000, 1000.00"
    })
    void realizedRoundsHalfUpAtScaleTwo(
            String buyPrice, long buyQuantity,
            String sellPrice, long sellQuantity,
            String expectedRealized) {
        AssetPosition position = new AssetPosition();
        position.buy(dec(buyPrice), buyQuantity);

        BigDecimal realized = position.sell(dec(sellPrice), sellQuantity);

        assertEquals(dec(expectedRealized), realized);
    }

    @ParameterizedTest
    @CsvSource({
            "0.10, 0.02",
            "0.30, 0.05",
            "0.50, 0.08",
            "1.00, 0.15",
            "0.02, 0.00"
    })
    void taxRoundsHalfUpAtScaleTwo(String realizedGain, String expectedTax) {
        TradeOperation sale = new TradeOperation(
                "PETR", OperationType.SELL, dec("100.00"), 100);

        BigDecimal tax = taxEngine.tax(sale, dec(realizedGain), new LossPool());

        assertEquals(dec(expectedTax), tax);
    }
}
