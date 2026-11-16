package com.capitalgains.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LossPoolTest {

    @Test
    void newPoolIsEmptyAtTwoDecimals() {
        LossPool pool = new LossPool();

        assertEquals(new BigDecimal("0.00"), pool.balance());
    }

    @Test
    void lossesAccumulate() {
        LossPool pool = new LossPool();
        pool.add(new BigDecimal("10.00"));
        pool.add(new BigDecimal("2.50"));

        assertEquals(new BigDecimal("12.50"), pool.balance());
    }

    @Test
    void consumeOffsetsAtMostTheAvailableLoss() {
        LossPool pool = new LossPool();
        pool.add(new BigDecimal("10.00"));

        assertEquals(new BigDecimal("10.00"), pool.consume(new BigDecimal("25.00")));
        assertEquals(new BigDecimal("0.00"), pool.balance());
    }

    @Test
    void consumeTakesOnlyTheGainWhenPoolIsLarger() {
        LossPool pool = new LossPool();
        pool.add(new BigDecimal("10.00"));

        assertEquals(new BigDecimal("4.00"), pool.consume(new BigDecimal("4.00")));
        assertEquals(new BigDecimal("6.00"), pool.balance());
    }

    @Test
    void nonPositiveLossIsRejected() {
        LossPool pool = new LossPool();

        assertThrows(DomainException.class, () -> pool.add(BigDecimal.ZERO));
        assertThrows(DomainException.class, () -> pool.add(new BigDecimal("-1.00")));
        assertThrows(DomainException.class, () -> pool.consume(BigDecimal.ZERO));
        assertThrows(DomainException.class, () -> pool.consume(new BigDecimal("-1.00")));
        assertEquals(new BigDecimal("0.00"), pool.balance());
    }
}
