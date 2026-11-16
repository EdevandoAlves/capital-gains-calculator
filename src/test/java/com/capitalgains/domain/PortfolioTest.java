package com.capitalgains.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PortfolioTest {

    private static TradeOperation buy(String asset, String unitPrice, long quantity) {
        return new TradeOperation(asset, OperationType.BUY, new BigDecimal(unitPrice), quantity);
    }

    private static TradeOperation sell(String asset, String unitPrice, long quantity) {
        return new TradeOperation(asset, OperationType.SELL, new BigDecimal(unitPrice), quantity);
    }

    @Test
    void assetsAreIndependent() {
        Portfolio portfolio = new Portfolio();
        portfolio.apply(buy("PETR", "10.00", 100));
        portfolio.apply(buy("VALE", "30.00", 10));

        BigDecimal realized = portfolio.apply(sell("PETR", "12.50", 50));

        assertEquals(new BigDecimal("125.00"), realized);
        assertEquals(50, portfolio.quantity("PETR"));
        assertEquals(new BigDecimal("10.000000"), portfolio.averageCost("PETR"));
        assertEquals(10, portfolio.quantity("VALE"));
        assertEquals(new BigDecimal("30.000000"), portfolio.averageCost("VALE"));
    }

    @Test
    void sellPreservesAverageAndLaterBuyRecomputesIt() {
        Portfolio portfolio = new Portfolio();
        portfolio.apply(buy("PETR", "10.00", 10));
        portfolio.apply(sell("PETR", "10.00", 4));

        portfolio.apply(buy("PETR", "20.00", 6));

        assertEquals(12, portfolio.quantity("PETR"));
        assertEquals(new BigDecimal("15.000000"), portfolio.averageCost("PETR"));
    }

    @Test
    void sellAbovePositionIsRejected() {
        Portfolio portfolio = new Portfolio();
        portfolio.apply(buy("PETR", "10.00", 10));

        assertThrows(DomainException.class, () -> portfolio.apply(sell("PETR", "10.00", 11)));

        assertEquals(10, portfolio.quantity("PETR"));
    }

    @Test
    void sellOfUnheldAssetIsRejected() {
        Portfolio portfolio = new Portfolio();

        assertThrows(DomainException.class, () -> portfolio.apply(sell("VALE", "10.00", 1)));

        assertEquals(0, portfolio.quantity("VALE"));
    }

    @Test
    void unknownAssetHasFlatPosition() {
        Portfolio portfolio = new Portfolio();

        assertEquals(0, portfolio.quantity("PETR"));
        assertEquals(0, portfolio.averageCost("PETR").compareTo(BigDecimal.ZERO));
    }
}
