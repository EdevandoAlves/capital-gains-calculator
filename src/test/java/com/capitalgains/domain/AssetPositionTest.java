package com.capitalgains.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AssetPositionTest {

    private static BigDecimal dec(String value) {
        return new BigDecimal(value);
    }

    @Test
    void buyOfFlatPositionSetsAverageAtScaleSix() {
        AssetPosition position = new AssetPosition();

        position.buy(dec("10.00"), 100);

        assertEquals(100, position.quantity());
        assertEquals(dec("10.000000"), position.averageCost());
        assertEquals(6, position.averageCost().scale());
    }

    @Test
    void buyUsesWeightedAverageWithRepeatingDecimal() {
        AssetPosition position = new AssetPosition();
        position.buy(dec("20.00"), 2);

        position.buy(dec("60.00"), 1);

        assertEquals(3, position.quantity());
        assertEquals(dec("33.333333"), position.averageCost());
    }

    @Test
    void sellPreservesAverageOfRemainingUnits() {
        AssetPosition position = new AssetPosition();
        position.buy(dec("10.00"), 100);

        BigDecimal realized = position.sell(dec("14.00"), 40);

        assertEquals(dec("160.00"), realized);
        assertEquals(60, position.quantity());
        assertEquals(dec("10.000000"), position.averageCost());
    }

    @Test
    void positionResetsToZeroWhenFullySold() {
        AssetPosition position = new AssetPosition();
        position.buy(dec("5.00"), 10);

        BigDecimal realized = position.sell(dec("6.50"), 10);

        assertEquals(dec("15.00"), realized);
        assertEquals(0, position.quantity());
        assertEquals(0, position.averageCost().compareTo(BigDecimal.ZERO));
        assertEquals(6, position.averageCost().scale());

        position.buy(dec("7.00"), 5);
        assertEquals(dec("7.000000"), position.averageCost());
    }

    @Test
    void sellAbovePositionIsRejectedWithoutMovingState() {
        AssetPosition position = new AssetPosition();
        position.buy(dec("10.00"), 10);

        assertThrows(DomainException.class, () -> position.sell(dec("10.00"), 11));

        assertEquals(10, position.quantity());
        assertEquals(dec("10.000000"), position.averageCost());
    }

    @Test
    void realizedLossIsNegative() {
        AssetPosition position = new AssetPosition();
        position.buy(dec("10.00"), 10);

        BigDecimal realized = position.sell(dec("8.00"), 5);

        assertEquals(dec("-10.00"), realized);
        assertTrue(realized.signum() < 0);
    }
}
