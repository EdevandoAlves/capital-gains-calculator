package com.capitalgains.tax;

import com.capitalgains.domain.LossPool;
import com.capitalgains.domain.TradeOperation;

import java.math.BigDecimal;

/**
 * Phase 1 placeholder: tax is always 0.00 and the loss pool is left untouched.
 */
public final class ZeroTaxEngine implements TaxEngine {

    private static final BigDecimal ZERO_TAX = BigDecimal.ZERO.setScale(2);

    @Override
    public BigDecimal tax(TradeOperation operation, BigDecimal realizedProfitLoss, LossPool lossPool) {
        return ZERO_TAX;
    }
}
