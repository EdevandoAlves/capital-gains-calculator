package com.capitalgains.app;

import com.capitalgains.Main;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CliIntegrationTest {

    private static final String CONTRACT_INPUT =
            "[{\"asset\":\"PETR\",\"type\":\"BUY\",\"unitPrice\":10.00,\"quantity\":100},"
                    + "{\"asset\":\"PETR\",\"type\":\"SELL\",\"unitPrice\":12.50,\"quantity\":50}]\n"
                    + "[{\"asset\":\"VALE\",\"type\":\"BUY\",\"unitPrice\":30.00,\"quantity\":10}]\n";

    private static final String CONTRACT_OUTPUT =
            "[{\"operation\":1,\"asset\":\"PETR\",\"type\":\"BUY\",\"remainingQuantity\":100,"
                    + "\"averageCost\":10.00,\"realizedProfitLoss\":0.00,\"tax\":0.00,"
                    + "\"compensableLoss\":0.00},"
                    + "{\"operation\":2,\"asset\":\"PETR\",\"type\":\"SELL\",\"remainingQuantity\":50,"
                    + "\"averageCost\":10.00,\"realizedProfitLoss\":125.00,\"tax\":0.00,"
                    + "\"compensableLoss\":0.00}]\n"
                    + "[{\"operation\":1,\"asset\":\"VALE\",\"type\":\"BUY\",\"remainingQuantity\":10,"
                    + "\"averageCost\":30.00,\"realizedProfitLoss\":0.00,\"tax\":0.00,"
                    + "\"compensableLoss\":0.00}]\n";

    private record Invocation(int exitCode, String out, String err) {
    }

    private static Invocation invoke(String input) {
        StringWriter out = new StringWriter();
        StringWriter err = new StringWriter();
        int exitCode = Main.execute(new StringReader(input), out, err);
        return new Invocation(exitCode, out.toString(), err.toString());
    }

    @Test
    void contractExampleProducesExactOutput() {
        Invocation result = invoke(CONTRACT_INPUT);

        assertEquals(0, result.exitCode());
        assertEquals(CONTRACT_OUTPUT, result.out());
        assertEquals("", result.err());
    }

    @Test
    void exitCodeIsZeroOnSuccess() {
        assertEquals(0, invoke(CONTRACT_INPUT).exitCode());
    }

    @Test
    void emptyInputProducesNoOutput() {
        Invocation result = invoke("");

        assertEquals(0, result.exitCode());
        assertEquals("", result.out());
        assertEquals("", result.err());
    }

    @Test
    void blankLinesAreIgnored() {
        Invocation result = invoke("\n   \n\t\n" + CONTRACT_INPUT);

        assertEquals(0, result.exitCode());
        assertEquals(CONTRACT_OUTPUT, result.out());
    }

    @Test
    void emptySimulationOutputsEmptyArray() {
        Invocation result = invoke("[]\n");

        assertEquals(0, result.exitCode());
        assertEquals("[]\n", result.out());
    }

    @Test
    void lineOrderDoesNotChangeAnyLineOutput() {
        String first = "[{\"asset\":\"PETR\",\"type\":\"BUY\",\"unitPrice\":10.00,\"quantity\":100}]\n";
        String second = "[{\"asset\":\"VALE\",\"type\":\"BUY\",\"unitPrice\":30.00,\"quantity\":10},"
                + "{\"asset\":\"VALE\",\"type\":\"SELL\",\"unitPrice\":31.00,\"quantity\":5}]\n";

        String[] forward = invoke(first + second).out().split("\n");
        String[] reversed = invoke(second + first).out().split("\n");

        assertEquals(2, forward.length);
        assertEquals(2, reversed.length);
        assertEquals(forward[0], reversed[1]);
        assertEquals(forward[1], reversed[0]);
    }

    @Test
    void lossPoolIsPerSimulation() {
        String input = "[{\"asset\":\"PETR\",\"type\":\"BUY\",\"unitPrice\":10.00,\"quantity\":10},"
                + "{\"asset\":\"PETR\",\"type\":\"SELL\",\"unitPrice\":8.00,\"quantity\":5}]\n"
                + "[{\"asset\":\"VALE\",\"type\":\"BUY\",\"unitPrice\":30.00,\"quantity\":1}]\n";

        Invocation result = invoke(input);

        assertEquals(0, result.exitCode());
        String[] lines = result.out().split("\n");
        assertTrue(lines[0].endsWith("\"realizedProfitLoss\":-10.00,\"tax\":0.00,"
                + "\"compensableLoss\":10.00}]"));
        assertTrue(lines[1].endsWith("\"realizedProfitLoss\":0.00,\"tax\":0.00,"
                + "\"compensableLoss\":0.00}]"));
    }

    @Test
    void averageCostIsReportedWithTwoDecimals() {
        String input = "[{\"asset\":\"PETR\",\"type\":\"BUY\",\"unitPrice\":20.00,\"quantity\":2},"
                + "{\"asset\":\"PETR\",\"type\":\"BUY\",\"unitPrice\":60.00,\"quantity\":1},"
                + "{\"asset\":\"PETR\",\"type\":\"SELL\",\"unitPrice\":34.00,\"quantity\":1}]\n";

        String[] lines = invoke(input).out().split("\n");

        assertTrue(lines[0].contains("\"averageCost\":33.33,\"realizedProfitLoss\":0.00"));
        assertTrue(lines[0].endsWith("\"averageCost\":33.33,\"realizedProfitLoss\":0.67,"
                + "\"tax\":0.00,\"compensableLoss\":0.00}]"));
    }

    @Test
    void invalidInputExitsOneWithSingleErrorLineAndNoStdout() {
        Invocation result = invoke("[{\"asset\":\"petr\",\"type\":\"BUY\",\"unitPrice\":10.00,"
                + "\"quantity\":1}]\n");

        assertEquals(1, result.exitCode());
        assertEquals("", result.out());
        assertTrue(result.err().startsWith("ERROR: "));
        assertEquals(1, result.err().lines().count());
    }

    @Test
    void outputIsAtomicWhenSecondLineIsInvalid() {
        Invocation result = invoke("[{\"asset\":\"PETR\",\"type\":\"BUY\",\"unitPrice\":10.00,"
                + "\"quantity\":1}]\n"
                + "[{\"asset\":\"PETR\",\"type\":\"BUY\",\"unitPrice\":10.00,\"quantity\":1.5}]\n");

        assertEquals(1, result.exitCode());
        assertEquals("", result.out());
        assertTrue(result.err().startsWith("ERROR: line 2: "));
    }

    @Test
    void oversellExitsOneWithNoStdout() {
        Invocation result = invoke("[{\"asset\":\"PETR\",\"type\":\"BUY\",\"unitPrice\":10.00,"
                + "\"quantity\":1},{\"asset\":\"PETR\",\"type\":\"SELL\",\"unitPrice\":10.00,"
                + "\"quantity\":2}]\n");

        assertEquals(1, result.exitCode());
        assertEquals("", result.out());
        assertTrue(result.err().contains("exceeds position"));
    }
}
