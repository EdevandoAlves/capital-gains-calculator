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
    @ValueSource(strings = {"petr", "Petr", "", "PETR-3", "PETR 3", "PETR_3",
            "ABCDEFGHIJKLMNOPQRSTU"})
    void invalidAssetIsRejected(String asset) {
        assertRejected("[" + op(asset, "BUY", "10.00", "1") + "]");
    }

    @ParameterizedTest
    @ValueSource(strings = {"PETR4", "A1"})
    void alphanumericAssetIsAccepted(String asset) {
        List<TradeOperation> operations = parser.parse("[" + op(asset, "BUY", "10.00", "1") + "]");

        assertEquals(asset, operations.get(0).asset());
    }

    @ParameterizedTest
    @ValueSource(strings = {"buy", "sell", "BUY ", "Buy", "", "HOLD"})
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
    void quantityOfOneIsAccepted() {
        List<TradeOperation> operations = parser.parse("[" + op("PETR", "BUY", "10.00", "1") + "]");

        assertEquals(1, operations.get(0).quantity());
    }

    @Test
    void minimumUnitPriceWithSixDecimalsIsAccepted() {
        List<TradeOperation> operations = parser.parse("[" + op("PETR", "BUY", "0.000001", "1") + "]");

        assertEquals(new BigDecimal("0.000001"), operations.get(0).unitPrice());
        assertEquals(6, operations.get(0).unitPrice().scale());
    }

    @Test
    void unitPriceWithSevenDecimalsAtTheMaximumIsRejectedByScale() {
        InvalidInputException error = assertThrows(InvalidInputException.class,
                () -> parser.parse("[" + op("PETR", "BUY", "1000000000.0000001", "1") + "]"));

        assertTrue(error.getMessage().contains("at most 6 decimal places"));
    }

    @Test
    void unitPriceAboveTheMaximumIsRejectedByRange() {
        InvalidInputException error = assertThrows(InvalidInputException.class,
                () -> parser.parse("[" + op("PETR", "BUY", "1000000001", "1") + "]"));

        assertTrue(error.getMessage().contains("at most 1000000000"));
    }

    @Test
    void quantityAboveTheMaximumIsRejected() {
        assertRejected("[" + op("PETR", "BUY", "10.00", "1000000001") + "]");
    }

    @ParameterizedTest
    @ValueSource(strings = {"[null]", "[\"BUY\"]", "[5]", "[true]"})
    void nonObjectOperationElementIsRejected(String line) {
        assertRejected(line);
    }

    @Test
    void nonObjectOperationReportsItsPosition() {
        InvalidInputException error = assertThrows(InvalidInputException.class,
                () -> parser.parse("[" + op("PETR", "BUY", "10.00", "1") + ",null]"));

        assertTrue(error.getMessage().contains("operation 2: must be a JSON object"));
    }

    @Test
    void whitespaceSurroundingANonEmptyArrayIsAccepted() {
        List<TradeOperation> operations =
                parser.parse(" \t[" + op("PETR", "BUY", "10.00", "1") + "]\t ");

        assertEquals(1, operations.size());
        assertEquals("PETR", operations.get(0).asset());
    }

    @Test
    void quantityInExponentNotationIsAcceptedWhenIntegral() {
        List<TradeOperation> exponent = parser.parse("[" + op("PETR", "BUY", "10.00", "1e3") + "]");
        List<TradeOperation> plain = parser.parse("[" + op("PETR", "BUY", "10.00", "1000") + "]");

        assertEquals(1000, exponent.get(0).quantity());
        assertEquals(plain, exponent);
    }

    @Test
    void unitPriceInExponentNotationIsAccepted() {
        // Jackson normalizes the JSON number "1.0e1" to 10 (scale 0); the exponent form is a
        // number, so the parser accepts it and no scale violation applies.
        List<TradeOperation> exponent =
                parser.parse("[" + op("PETR", "BUY", "1.0e1", "1") + "]");
        List<TradeOperation> plain = parser.parse("[" + op("PETR", "BUY", "10.00", "1") + "]");

        assertEquals(new BigDecimal("10"), exponent.get(0).unitPrice());
        assertEquals(0, exponent.get(0).unitPrice().compareTo(plain.get(0).unitPrice()));
    }

    @Test
    void assetOfTwentyCharactersIsAccepted() {
        List<TradeOperation> operations = parser.parse(
                "[" + op("ABCDEFGHIJKLMNOPQRST", "BUY", "10.00", "1") + "]");

        assertEquals("ABCDEFGHIJKLMNOPQRST", operations.get(0).asset());
    }
}
