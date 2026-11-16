package com.capitalgains.tax;

import com.capitalgains.domain.LossPool;
import com.capitalgains.domain.TradeOperation;

import java.math.BigDecimal;

public interface TaxEngine {

    /**
     * Tax charged for one operation, rounded to 2 decimals. Implementations may consume
     * from {@code lossPool} for taxable profitable sales; BUYs, losses and exempt sales
     * leave the pool untouched.
     */
    BigDecimal tax(TradeOperation operation, BigDecimal realizedProfitLoss, LossPool lossPool);
}
