package com.capitalgains.domain;

import java.math.BigDecimal;

public record OperationResult(
        int operation,
        String asset,
        OperationType type,
        long remainingQuantity,
        BigDecimal averageCost,
        BigDecimal realizedProfitLoss,
        BigDecimal tax,
        BigDecimal compensableLoss) {
}
