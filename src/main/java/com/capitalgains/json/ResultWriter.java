package com.capitalgains.json;

import com.capitalgains.domain.OperationResult;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Serializes one simulation's results as a JSON array with fixed field order (CONTRACT.md).
 * Assets are validated to {@code ^[A-Z0-9]{1,20}$}, so no string escaping is required.
 */
public final class ResultWriter {

    public String write(List<OperationResult> results) {
        StringBuilder json = new StringBuilder();
        json.append('[');
        for (int i = 0; i < results.size(); i++) {
            if (i > 0) {
                json.append(',');
            }
            OperationResult result = results.get(i);
            json.append("{\"operation\":").append(result.operation())
                    .append(",\"asset\":\"").append(result.asset())
                    .append("\",\"type\":\"").append(result.type().name())
                    .append("\",\"remainingQuantity\":").append(result.remainingQuantity())
                    .append(",\"averageCost\":").append(money(result.averageCost()))
                    .append(",\"realizedProfitLoss\":").append(money(result.realizedProfitLoss()))
                    .append(",\"tax\":").append(money(result.tax()))
                    .append(",\"compensableLoss\":").append(money(result.compensableLoss()))
                    .append('}');
        }
        return json.append(']').toString();
    }

    private static String money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }
}
