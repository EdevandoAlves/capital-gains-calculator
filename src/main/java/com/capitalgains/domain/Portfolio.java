package com.capitalgains.domain;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

public final class Portfolio {

    private static final BigDecimal ZERO_COST = BigDecimal.ZERO.setScale(6);
    private static final BigDecimal ZERO_MONEY = BigDecimal.ZERO.setScale(2);

    private final Map<String, AssetPosition> positions = new HashMap<>();

    /**
     * Applies one operation and returns its realized profit/loss (0.00 for a BUY).
     */
    public BigDecimal apply(TradeOperation operation) {
        String asset = operation.asset();
        if (operation.type() == OperationType.BUY) {
            positions.computeIfAbsent(asset, ignored -> new AssetPosition())
                    .buy(operation.unitPrice(), operation.quantity());
            return ZERO_MONEY;
        }
        AssetPosition position = positions.get(asset);
        long held = position == null ? 0 : position.quantity();
        if (operation.quantity() > held) {
            throw new DomainException("SELL of " + operation.quantity() + " " + asset
                    + " exceeds position of " + held);
        }
        return position.sell(operation.unitPrice(), operation.quantity());
    }

    public long quantity(String asset) {
        AssetPosition position = positions.get(asset);
        return position == null ? 0 : position.quantity();
    }

    public BigDecimal averageCost(String asset) {
        AssetPosition position = positions.get(asset);
        return position == null ? ZERO_COST : position.averageCost();
    }
}
