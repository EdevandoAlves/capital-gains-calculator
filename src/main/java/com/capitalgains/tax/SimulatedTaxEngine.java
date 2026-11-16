package com.capitalgains.tax;

import com.capitalgains.domain.LossPool;
import com.capitalgains.domain.OperationType;
import com.capitalgains.domain.TradeOperation;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Educational simulation of the tax rules in DOMAIN_RULES §2: 15% over the taxable gain
 * of a sale whose gross value is at least 10000.00, after offsetting the simulation's
 * accumulated losses. Not Brazilian legislation.
 *
 * <p>{@code SimulationRunner} adds realized losses to the pool before calling this engine,
 * so here the pool is only consumed by taxable profitable sales.
 */
public final class SimulatedTaxEngine implements TaxEngine {

    private static final BigDecimal RATE = new BigDecimal("0.15");
    private static final BigDecimal EXEMPTION_THRESHOLD = new BigDecimal("10000.00");
    private static final BigDecimal ZERO_TAX = BigDecimal.ZERO.setScale(2);

    @Override
    public BigDecimal tax(TradeOperation operation, BigDecimal realizedProfitLoss, LossPool lossPool) {
        if (operation.type() != OperationType.SELL) {
            return ZERO_TAX;
        }
        BigDecimal gross = operation.unitPrice().multiply(BigDecimal.valueOf(operation.quantity()));
        if (gross.compareTo(EXEMPTION_THRESHOLD) < 0) {
            return ZERO_TAX;
        }
        if (realizedProfitLoss.signum() <= 0) {
            return ZERO_TAX;
        }
        BigDecimal offset = lossPool.consume(realizedProfitLoss);
        BigDecimal taxable = realizedProfitLoss.subtract(offset);
        return taxable.multiply(RATE).setScale(2, RoundingMode.HALF_UP);
    }
}
