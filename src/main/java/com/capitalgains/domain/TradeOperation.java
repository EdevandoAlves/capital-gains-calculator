package com.capitalgains.domain;

import java.math.BigDecimal;

public record TradeOperation(String asset, OperationType type, BigDecimal unitPrice, long quantity) {
}
