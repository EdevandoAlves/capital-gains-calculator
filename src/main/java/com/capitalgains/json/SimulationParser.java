package com.capitalgains.json;

import com.capitalgains.domain.OperationType;
import com.capitalgains.domain.TradeOperation;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.StreamReadFeature;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Strict parser for one simulation line: a JSON array of operations (CONTRACT.md).
 */
public final class SimulationParser {

    private static final Pattern ASSET_PATTERN = Pattern.compile("^[A-Z0-9]{1,20}$");
    private static final Set<String> FIELDS = Set.of("asset", "type", "unitPrice", "quantity");
    private static final List<String> REQUIRED_FIELD_ORDER = List.of("asset", "type", "unitPrice", "quantity");
    private static final BigDecimal MAX_PRICE = new BigDecimal("1000000000");
    private static final BigDecimal MAX_QUANTITY = new BigDecimal("1000000000");
    private static final BigDecimal MIN_QUANTITY = BigDecimal.ONE;
    private static final int MAX_PRICE_SCALE = 6;

    private final ObjectMapper mapper;

    public SimulationParser() {
        this.mapper = new ObjectMapper(JsonFactory.builder()
                .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
                .build())
                .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
                .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
                // keep the scale as written so "at most 6 decimal places" is validated on the input text
                .setNodeFactory(JsonNodeFactory.withExactBigDecimals(true));
    }

    public List<TradeOperation> parse(String line) {
        JsonNode root;
        try {
            root = mapper.readValue(line, JsonNode.class);
        } catch (JsonProcessingException e) {
            throw new InvalidInputException("malformed JSON: " + e.getOriginalMessage());
        }
        if (root == null || !root.isArray()) {
            throw new InvalidInputException("simulation must be a JSON array of operations");
        }
        List<TradeOperation> operations = new ArrayList<>(root.size());
        for (int i = 0; i < root.size(); i++) {
            operations.add(parseOperation(root.get(i), i + 1));
        }
        return List.copyOf(operations);
    }

    private TradeOperation parseOperation(JsonNode node, int index) {
        String where = "operation " + index + ": ";
        if (node == null || !node.isObject()) {
            throw new InvalidInputException(where + "must be a JSON object");
        }
        Iterator<String> names = node.fieldNames();
        while (names.hasNext()) {
            String name = names.next();
            if (!FIELDS.contains(name)) {
                throw new InvalidInputException(where + "unknown field '" + name + "'");
            }
        }
        for (String field : REQUIRED_FIELD_ORDER) {
            if (!node.has(field)) {
                throw new InvalidInputException(where + "missing required field '" + field + "'");
            }
        }
        String asset = text(node.get("asset"), where + "field 'asset'");
        if (!ASSET_PATTERN.matcher(asset).matches()) {
            throw new InvalidInputException(where + "field 'asset' must match ^[A-Z0-9]{1,20}$");
        }
        OperationType type = parseType(text(node.get("type"), where + "field 'type'"), where);
        BigDecimal unitPrice = parseUnitPrice(node.get("unitPrice"), where);
        long quantity = parseQuantity(node.get("quantity"), where);
        return new TradeOperation(asset, type, unitPrice, quantity);
    }

    private static OperationType parseType(String value, String where) {
        return switch (value) {
            case "BUY" -> OperationType.BUY;
            case "SELL" -> OperationType.SELL;
            default -> throw new InvalidInputException(where + "field 'type' must be BUY or SELL");
        };
    }

    private static BigDecimal parseUnitPrice(JsonNode node, String where) {
        if (!node.isNumber()) {
            throw new InvalidInputException(where + "field 'unitPrice' must be a number");
        }
        BigDecimal unitPrice = node.decimalValue();
        if (unitPrice.scale() > MAX_PRICE_SCALE) {
            throw new InvalidInputException(where + "field 'unitPrice' must have at most 6 decimal places");
        }
        if (unitPrice.signum() <= 0) {
            throw new InvalidInputException(where + "field 'unitPrice' must be greater than 0");
        }
        if (unitPrice.compareTo(MAX_PRICE) > 0) {
            throw new InvalidInputException(where + "field 'unitPrice' must be at most 1000000000");
        }
        return unitPrice;
    }

    private static long parseQuantity(JsonNode node, String where) {
        if (!node.isNumber()) {
            throw new InvalidInputException(where + "field 'quantity' must be an integer");
        }
        BigDecimal quantity = node.decimalValue();
        if (quantity.stripTrailingZeros().scale() > 0) {
            throw new InvalidInputException(where + "field 'quantity' must not have a fractional part");
        }
        if (quantity.compareTo(MIN_QUANTITY) < 0) {
            throw new InvalidInputException(where + "field 'quantity' must be at least 1");
        }
        if (quantity.compareTo(MAX_QUANTITY) > 0) {
            throw new InvalidInputException(where + "field 'quantity' must be at most 1000000000");
        }
        return quantity.longValueExact();
    }

    private static String text(JsonNode node, String field) {
        if (node == null || !node.isTextual()) {
            throw new InvalidInputException(field + " must be a string");
        }
        return node.textValue();
    }
}
