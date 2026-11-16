package com.capitalgains.tax;

import com.capitalgains.domain.LossPool;
import com.capitalgains.domain.OperationType;
import com.capitalgains.domain.TradeOperation;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * One focused test per rule of DOMAIN_RULES §2.
 */
class TaxEngineTest {

    private final SimulatedTaxEngine engine = new SimulatedTaxEngine();

    private static TradeOperation sell(String unitPrice, long quantity) {
        return new TradeOperation("PETR", OperationType.SELL, new BigDecimal(unitPrice), quantity);
    }

    private static TradeOperation buy(String unitPrice, long quantity) {
        return new TradeOperation("PETR", OperationType.BUY, new BigDecimal(unitPrice), quantity);
    }

    private static LossPool poolWith(String loss) {
        LossPool pool = new LossPool();
        pool.add(new BigDecimal(loss));
        return pool;
    }

    @Test
    void chargesFifteenPercentOnTaxableGain() {
        LossPool pool = new LossPool();

        BigDecimal tax = engine.tax(sell("200.00", 100), new BigDecimal("5000.00"), pool);

        assertEquals(new BigDecimal("750.00"), tax);
        assertEquals(new BigDecimal("0.00"), pool.balance());
    }

    @Test
    void saleBelowTenThousandGrossIsExemptAndLeavesPoolUntouched() {
        LossPool pool = poolWith("500.00");

        BigDecimal tax = engine.tax(sell("9999.99", 1), new BigDecimal("3000.00"), pool);

        assertEquals(new BigDecimal("0.00"), tax);
        assertEquals(new BigDecimal("500.00"), pool.balance());
    }

    @Test
    void saleWithGrossExactlyTenThousandIsTaxable() {
        LossPool pool = new LossPool();

        BigDecimal tax = engine.tax(sell("100.00", 100), new BigDecimal("5000.00"), pool);

        assertEquals(new BigDecimal("750.00"), tax);
    }

    @Test
    void lossPoolPartiallyOffsetsGain() {
        LossPool pool = poolWith("400.00");

        BigDecimal tax = engine.tax(sell("200.00", 100), new BigDecimal("1000.00"), pool);

        assertEquals(new BigDecimal("90.00"), tax);
        assertEquals(new BigDecimal("0.00"), pool.balance());
    }

    @Test
    void lossPoolFullyOffsetsGain() {
        LossPool pool = poolWith("1000.00");

        BigDecimal tax = engine.tax(sell("200.00", 100), new BigDecimal("1000.00"), pool);

        assertEquals(new BigDecimal("0.00"), tax);
        assertEquals(new BigDecimal("0.00"), pool.balance());
    }

    @Test
    void taxIsZeroWhenPoolExceedsGainAndRemainderStays() {
        LossPool pool = poolWith("1500.00");

        BigDecimal tax = engine.tax(sell("200.00", 100), new BigDecimal("1000.00"), pool);

        assertEquals(new BigDecimal("0.00"), tax);
        assertEquals(new BigDecimal("500.00"), pool.balance());
    }

    @Test
    void exemptProfitableSaleDoesNotConsumePool() {
        LossPool pool = poolWith("1000.00");

        BigDecimal tax = engine.tax(sell("200.00", 30), new BigDecimal("3000.00"), pool);

        assertEquals(new BigDecimal("0.00"), tax);
        assertEquals(new BigDecimal("1000.00"), pool.balance());
    }

    @Test
    void realizedLossIsNotTaxedAndLeavesPoolUntouched() {
        LossPool pool = new LossPool();

        BigDecimal tax = engine.tax(sell("100.00", 100), new BigDecimal("-1000.00"), pool);

        assertEquals(new BigDecimal("0.00"), tax);
        assertEquals(new BigDecimal("0.00"), pool.balance());
    }

    @Test
    void realizedZeroIsNotTaxedAndLeavesPoolUntouched() {
        LossPool pool = poolWith("10.00");

        BigDecimal tax = engine.tax(sell("100.00", 100), new BigDecimal("0.00"), pool);

        assertEquals(new BigDecimal("0.00"), tax);
        assertEquals(new BigDecimal("10.00"), pool.balance());
    }

    @Test
    void buyIsNotTaxedAndLeavesPoolUntouched() {
        LossPool pool = poolWith("10.00");

        BigDecimal tax = engine.tax(buy("100.00", 100), new BigDecimal("0.00"), pool);

        assertEquals(new BigDecimal("0.00"), tax);
        assertEquals(new BigDecimal("10.00"), pool.balance());
    }

    @Test
    void taxRoundsOnceHalfUpToTwoDecimals() {
        LossPool pool = new LossPool();

        BigDecimal tax = engine.tax(sell("100.00", 100), new BigDecimal("0.05"), pool);

        assertEquals(new BigDecimal("0.01"), tax);
        assertEquals(2, tax.scale());
    }

    @Test
    void taxIsNeverNegative() {
        LossPool pool = poolWith("10000.00");

        BigDecimal tax = engine.tax(sell("200.00", 100), new BigDecimal("5000.00"), pool);

        assertEquals(new BigDecimal("0.00"), tax);
        assertEquals(0, tax.signum());
    }
}
