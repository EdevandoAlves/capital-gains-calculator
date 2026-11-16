package com.capitalgains.json;

import com.capitalgains.domain.OperationType;
import com.capitalgains.domain.TradeOperation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ParserTest {

    private static final String VALID_OPERATION =
            "{\"asset\":\"PETR\",\"type\":\"BUY\",\"unitPrice\":10.00,\"quantity\":100}";

    private final SimulationParser parser = new SimulationParser();

    private static String op(String asset, String type, String unitPrice, String quantity) {
        return "{\"asset\":\"" + asset + "\",\"type\":\"" + type + "\",\"unitPrice\":" + unitPrice
                + ",\"quantity\":" + quantity + "}";
    }

    private void assertRejected(String line) {
        assertThrows(InvalidInputException.class, () -> parser.parse(line));
    }

    @Test
    void parsesValidLinePreservingNumberScale() {
        String line = "[" + VALID_OPERATION + ","
                + op("VALE", "SELL", "12.345678", "3") + "]";

        List<TradeOperation> operations = parser.parse(line);

        assertEquals(2, operations.size());
        TradeOperation first = operations.get(0);
        assertEquals("PETR", first.asset());
        assertEquals(OperationType.BUY, first.type());
        assertEquals(new BigDecimal("10.00"), first.unitPrice());
        assertEquals(2, first.unitPrice().scale());
        assertEquals(100, first.quantity());
        TradeOperation second = operations.get(1);
        assertEquals("VALE", second.asset());
        assertEquals(OperationType.SELL, second.type());
        assertEquals(new BigDecimal("12.345678"), second.unitPrice());
        assertEquals(3, second.quantity());
    }

    @Test
    void emptyArrayIsAccepted() {
        assertTrue(parser.parse("[]").isEmpty());
        assertTrue(parser.parse("  [ ]  ").isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "",
            "[",
            "[{\"asset\":\"PETR\",}]",
            "[" + "{\"asset\":\"PETR\",\"type\":\"BUY\",\"unitPrice\":10.00,\"quantity\":100}",
            "[] []",
            "null"
    })
    void malformedJsonIsRejected(String line) {
        assertRejected(line);
    }

    @Test
    void topLevelObjectIsRejected() {
        assertRejected(VALID_OPERATION);
    }

    @Test
    void unknownFieldIsRejected() {
        assertRejected("[{\"asset\":\"PETR\",\"type\":\"BUY\",\"unitPrice\":10.00,\"quantity\":100,"
                + "\"fee\":1.00}]");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"type\":\"BUY\",\"unitPrice\":10.00,\"quantity\":100}",
            "{\"asset\":\"PETR\",\"unitPrice\":10.00,\"quantity\":100}",
            "{\"asset\":\"PETR\",\"type\":\"BUY\",\"quantity\":100}",
            "{\"asset\":\"PETR\",\"type\":\"BUY\",\"unitPrice\":10.00}"
    })
    void missingFieldIsRejected(String operation) {
        assertRejected("[" + operation + "]");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"asset\":\"PETR\",\"type\":\"BUY\",\"unitPrice\":\"10.00\",\"quantity\":100}",
            "{\"asset\":\"PETR\",\"type\":\"BUY\",\"unitPrice\":10.00,\"quantity\":\"100\"}",
            "{\"asset\":1,\"type\":\"BUY\",\"unitPrice\":10.00,\"quantity\":100}",
            "{\"asset\":\"PETR\",\"type\":1,\"unitPrice\":10.00,\"quantity\":100}",
            "{\"asset\":\"PETR\",\"type\":\"BUY\",\"unitPrice\":true,\"quantity\":100}",
            "{\"asset\":\"PETR\",\"type\":\"BUY\",\"unitPrice\":10.00,\"quantity\":true}",
            "{\"asset\":null,\"type\":\"BUY\",\"unitPrice\":10.00,\"quantity\":100}"
    })
    void wrongFieldTypeIsRejected(String operation) {
        assertRejected("[" + operation + "]");
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "0.00", "-1.00", "-0.00"})
    void nonPositiveUnitPriceIsRejected(String unitPrice) {
        assertRejected("[" + op("PETR", "BUY", unitPrice, "100") + "]");
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "1.5", "0.1"})
    void invalidQuantityIsRejected(String quantity) {
        assertRejected("[" + op("PETR", "BUY", "10.00", quantity) + "]");
    }

    @ParameterizedTest
    @ValueSource(strings = {"1.2345678", "0.0000001"})
    void unitPriceScaleAboveSixIsRejected(String unitPrice) {
        assertRejected("[" + op("PETR", "BUY", unitPrice, "1") + "]");
    }

    @Test
    void unitPriceScaleOfSixIsAccepted() {
        List<TradeOperation> operations = parser.parse("[" + op("PETR", "BUY", "1.234567", "1") + "]");

        assertEquals(new BigDecimal("1.234567"), operations.get(0).unitPrice());
    }

    @ParameterizedTest
    @ValueSource(strings = {"petr", "Petr", "", "PETR-3", "PETR 3", "ABCDEFGHIJKLMNOPQRSTU"})
    void invalidAssetIsRejected(String asset) {
        assertRejected("[" + op(asset, "BUY", "10.00", "1") + "]");
    }

    @ParameterizedTest
    @ValueSource(strings = {"buy", "BUY ", "Buy", "", "HOLD"})
    void invalidTypeIsRejected(String type) {
        assertRejected("[" + op("PETR", type, "10.00", "1") + "]");
    }

    @Test
    void limitsAreInclusive() {
        List<TradeOperation> operations = parser.parse(
                "[" + op("PETR", "BUY", "1000000000", "1000000000") + "]");

        assertEquals(new BigDecimal("1000000000"), operations.get(0).unitPrice());
        assertEquals(1000000000L, operations.get(0).quantity());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"asset\":\"PETR\",\"type\":\"BUY\",\"unitPrice\":1000000000.000001,\"quantity\":1}",
            "{\"asset\":\"PETR\",\"type\":\"BUY\",\"unitPrice\":10.00,\"quantity\":1000000001}"
    })
    void valuesAboveTheLimitsAreRejected(String operation) {
        assertRejected("[" + operation + "]");
    }

    @Test
    void quantityWithoutFractionalPartIsAccepted() {
        List<TradeOperation> operations = parser.parse("[" + op("PETR", "BUY", "10.00", "100.00") + "]");

        assertEquals(100, operations.get(0).quantity());
    }

    @Test
    void assetOfTwentyCharactersIsAccepted() {
        List<TradeOperation> operations = parser.parse(
                "[" + op("ABCDEFGHIJKLMNOPQRST", "BUY", "10.00", "1") + "]");

        assertEquals("ABCDEFGHIJKLMNOPQRST", operations.get(0).asset());
    }
}
