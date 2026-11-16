package com.capitalgains.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Weighted-average position of a single asset (DOMAIN_RULES §1).
 */
public final class AssetPosition {

    private static final int COST_SCALE = 6;
    private static final int MONEY_SCALE = 2;
    private static final BigDecimal ZERO_COST = BigDecimal.ZERO.setScale(COST_SCALE);

    private long quantity;
    private BigDecimal averageCost = ZERO_COST;

    public long quantity() {
        return quantity;
    }

    public BigDecimal averageCost() {
        return averageCost;
    }

    public void buy(BigDecimal unitPrice, long boughtQuantity) {
        BigDecimal totalCost = averageCost.multiply(BigDecimal.valueOf(quantity))
                .add(unitPrice.multiply(BigDecimal.valueOf(boughtQuantity)));
        quantity += boughtQuantity;
        averageCost = totalCost.divide(BigDecimal.valueOf(quantity), COST_SCALE, RoundingMode.HALF_UP);
    }

    public BigDecimal sell(BigDecimal unitPrice, long soldQuantity) {
        if (soldQuantity > quantity) {
            throw new DomainException("SELL of " + soldQuantity + " exceeds position of " + quantity);
        }
        BigDecimal realized = unitPrice.subtract(averageCost)
                .multiply(BigDecimal.valueOf(soldQuantity))
                .setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        quantity -= soldQuantity;
        if (quantity == 0) {
            averageCost = ZERO_COST;
        }
        return realized;
    }
}
