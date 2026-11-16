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
    void lossCarryforwardAcrossAssetsInOneSimulation() {
        String input = "[{\"asset\":\"A\",\"type\":\"BUY\",\"unitPrice\":20.00,\"quantity\":100},"
                + "{\"asset\":\"A\",\"type\":\"SELL\",\"unitPrice\":10.00,\"quantity\":100},"
                + "{\"asset\":\"B\",\"type\":\"BUY\",\"unitPrice\":40.00,\"quantity\":200},"
                + "{\"asset\":\"B\",\"type\":\"SELL\",\"unitPrice\":100.00,\"quantity\":200}]\n";

        String expected = "[{\"operation\":1,\"asset\":\"A\",\"type\":\"BUY\",\"remainingQuantity\":100,"
                + "\"averageCost\":20.00,\"realizedProfitLoss\":0.00,\"tax\":0.00,"
                + "\"compensableLoss\":0.00},"
                + "{\"operation\":2,\"asset\":\"A\",\"type\":\"SELL\",\"remainingQuantity\":0,"
                + "\"averageCost\":0.00,\"realizedProfitLoss\":-1000.00,\"tax\":0.00,"
                + "\"compensableLoss\":1000.00},"
                + "{\"operation\":3,\"asset\":\"B\",\"type\":\"BUY\",\"remainingQuantity\":200,"
                + "\"averageCost\":40.00,\"realizedProfitLoss\":0.00,\"tax\":0.00,"
                + "\"compensableLoss\":1000.00},"
                + "{\"operation\":4,\"asset\":\"B\",\"type\":\"SELL\",\"remainingQuantity\":0,"
                + "\"averageCost\":0.00,\"realizedProfitLoss\":12000.00,\"tax\":1650.00,"
                + "\"compensableLoss\":0.00}]\n";

        Invocation result = invoke(input);

        assertEquals(0, result.exitCode());
        assertEquals(expected, result.out());
    }

    @Test
    void exemptProfitableSaleIsNotTaxed() {
        String input = "[{\"asset\":\"C\",\"type\":\"BUY\",\"unitPrice\":100.00,\"quantity\":100},"
                + "{\"asset\":\"C\",\"type\":\"SELL\",\"unitPrice\":200.00,\"quantity\":30}]\n";

        String expected = "[{\"operation\":1,\"asset\":\"C\",\"type\":\"BUY\",\"remainingQuantity\":100,"
                + "\"averageCost\":100.00,\"realizedProfitLoss\":0.00,\"tax\":0.00,"
                + "\"compensableLoss\":0.00},"
                + "{\"operation\":2,\"asset\":\"C\",\"type\":\"SELL\",\"remainingQuantity\":70,"
                + "\"averageCost\":100.00,\"realizedProfitLoss\":3000.00,\"tax\":0.00,"
                + "\"compensableLoss\":0.00}]\n";

        Invocation result = invoke(input);

        assertEquals(0, result.exitCode());
        assertEquals(expected, result.out());
    }

    @Test
    void saleWithGrossExactlyTenThousandIsTaxed() {
        String input = "[{\"asset\":\"C\",\"type\":\"BUY\",\"unitPrice\":50.00,\"quantity\":100},"
                + "{\"asset\":\"C\",\"type\":\"SELL\",\"unitPrice\":100.00,\"quantity\":100}]\n";

        String expected = "[{\"operation\":1,\"asset\":\"C\",\"type\":\"BUY\",\"remainingQuantity\":100,"
                + "\"averageCost\":50.00,\"realizedProfitLoss\":0.00,\"tax\":0.00,"
                + "\"compensableLoss\":0.00},"
                + "{\"operation\":2,\"asset\":\"C\",\"type\":\"SELL\",\"remainingQuantity\":0,"
                + "\"averageCost\":0.00,\"realizedProfitLoss\":5000.00,\"tax\":750.00,"
                + "\"compensableLoss\":0.00}]\n";

        Invocation result = invoke(input);

        assertEquals(0, result.exitCode());
        assertEquals(expected, result.out());
    }

    @Test
    void lossPoolDoesNotLeakBetweenSimulationsForTax() {
        String lossLine = "[{\"asset\":\"A\",\"type\":\"BUY\",\"unitPrice\":20.00,\"quantity\":100},"
                + "{\"asset\":\"A\",\"type\":\"SELL\",\"unitPrice\":10.00,\"quantity\":100}]\n";
        String gainLine = "[{\"asset\":\"B\",\"type\":\"BUY\",\"unitPrice\":50.00,\"quantity\":100},"
                + "{\"asset\":\"B\",\"type\":\"SELL\",\"unitPrice\":100.00,\"quantity\":100}]\n";

        String[] lines = invoke(lossLine + gainLine).out().split("\n");

        assertEquals(2, lines.length);
        assertTrue(lines[0].endsWith("\"realizedProfitLoss\":-1000.00,\"tax\":0.00,"
                + "\"compensableLoss\":1000.00}]"));
        // No offset from line 1: 5000.00 * 15% = 750.00, not 600.00.
        assertTrue(lines[1].endsWith("\"realizedProfitLoss\":5000.00,\"tax\":750.00,"
                + "\"compensableLoss\":0.00}]"));
    }

    @Test
    void thresholdIsEvaluatedOnExactSixDecimalGross() {
        String input = "[{\"asset\":\"D\",\"type\":\"BUY\",\"unitPrice\":99.000001,\"quantity\":100},"
                + "{\"asset\":\"D\",\"type\":\"SELL\",\"unitPrice\":99.999999,\"quantity\":100}]\n"
                + "[{\"asset\":\"E\",\"type\":\"BUY\",\"unitPrice\":99.000001,\"quantity\":100},"
                + "{\"asset\":\"E\",\"type\":\"SELL\",\"unitPrice\":100.000001,\"quantity\":100}]\n";

        String[] lines = invoke(input).out().split("\n");

        assertEquals(2, lines.length);
        // gross 9999.9999 < 10000.00 -> exempt
        assertTrue(lines[0].endsWith("\"tax\":0.00,\"compensableLoss\":0.00}]"));
        // gross 10000.0001 >= 10000.00 -> taxable: 100.00 * 15% = 15.00
        assertTrue(lines[1].endsWith("\"tax\":15.00,\"compensableLoss\":0.00}]"));
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

    @Test
    void crlfLineEndingsAreProcessedLikeLf() {
        Invocation lf = invoke(CONTRACT_INPUT);
        Invocation crlf = invoke(CONTRACT_INPUT.replace("\n", "\r\n"));

        assertEquals(0, crlf.exitCode());
        assertEquals(CONTRACT_OUTPUT, crlf.out());
        assertEquals("", crlf.err());
        assertEquals(lf.out(), crlf.out());
    }

    @Test
    void sellAllResetsAverageAndRebuyUsesTheNewAverage() {
        String input = "[{\"asset\":\"PETR\",\"type\":\"BUY\",\"unitPrice\":10.00,\"quantity\":100},"
                + "{\"asset\":\"PETR\",\"type\":\"SELL\",\"unitPrice\":15.00,\"quantity\":100},"
                + "{\"asset\":\"PETR\",\"type\":\"BUY\",\"unitPrice\":20.00,\"quantity\":50},"
                + "{\"asset\":\"PETR\",\"type\":\"SELL\",\"unitPrice\":22.00,\"quantity\":50}]\n";

        String expected = "[{\"operation\":1,\"asset\":\"PETR\",\"type\":\"BUY\","
                + "\"remainingQuantity\":100,\"averageCost\":10.00,\"realizedProfitLoss\":0.00,"
                + "\"tax\":0.00,\"compensableLoss\":0.00},"
                + "{\"operation\":2,\"asset\":\"PETR\",\"type\":\"SELL\","
                + "\"remainingQuantity\":0,\"averageCost\":0.00,\"realizedProfitLoss\":500.00,"
                + "\"tax\":0.00,\"compensableLoss\":0.00},"
                + "{\"operation\":3,\"asset\":\"PETR\",\"type\":\"BUY\","
                + "\"remainingQuantity\":50,\"averageCost\":20.00,\"realizedProfitLoss\":0.00,"
                + "\"tax\":0.00,\"compensableLoss\":0.00},"
                + "{\"operation\":4,\"asset\":\"PETR\",\"type\":\"SELL\","
                + "\"remainingQuantity\":0,\"averageCost\":0.00,\"realizedProfitLoss\":100.00,"
                + "\"tax\":0.00,\"compensableLoss\":0.00}]\n";

        Invocation result = invoke(input);

        assertEquals(0, result.exitCode());
        assertEquals(expected, result.out());
    }

    @Test
    void interleavedAssetsKeepIndependentLifecycles() {
        String input = "[{\"asset\":\"PETR\",\"type\":\"BUY\",\"unitPrice\":10.00,\"quantity\":10},"
                + "{\"asset\":\"VALE\",\"type\":\"BUY\",\"unitPrice\":30.00,\"quantity\":20},"
                + "{\"asset\":\"PETR\",\"type\":\"SELL\",\"unitPrice\":12.00,\"quantity\":10},"
                + "{\"asset\":\"VALE\",\"type\":\"SELL\",\"unitPrice\":25.00,\"quantity\":5},"
                + "{\"asset\":\"PETR\",\"type\":\"BUY\",\"unitPrice\":7.00,\"quantity\":5},"
                + "{\"asset\":\"VALE\",\"type\":\"SELL\",\"unitPrice\":40.00,\"quantity\":15}]\n";

        String expected = "[{\"operation\":1,\"asset\":\"PETR\",\"type\":\"BUY\","
                + "\"remainingQuantity\":10,\"averageCost\":10.00,\"realizedProfitLoss\":0.00,"
                + "\"tax\":0.00,\"compensableLoss\":0.00},"
                + "{\"operation\":2,\"asset\":\"VALE\",\"type\":\"BUY\","
                + "\"remainingQuantity\":20,\"averageCost\":30.00,\"realizedProfitLoss\":0.00,"
                + "\"tax\":0.00,\"compensableLoss\":0.00},"
                + "{\"operation\":3,\"asset\":\"PETR\",\"type\":\"SELL\","
                + "\"remainingQuantity\":0,\"averageCost\":0.00,\"realizedProfitLoss\":20.00,"
                + "\"tax\":0.00,\"compensableLoss\":0.00},"
                + "{\"operation\":4,\"asset\":\"VALE\",\"type\":\"SELL\","
                + "\"remainingQuantity\":15,\"averageCost\":30.00,\"realizedProfitLoss\":-25.00,"
                + "\"tax\":0.00,\"compensableLoss\":25.00},"
                + "{\"operation\":5,\"asset\":\"PETR\",\"type\":\"BUY\","
                + "\"remainingQuantity\":5,\"averageCost\":7.00,\"realizedProfitLoss\":0.00,"
                + "\"tax\":0.00,\"compensableLoss\":25.00},"
                + "{\"operation\":6,\"asset\":\"VALE\",\"type\":\"SELL\","
                + "\"remainingQuantity\":0,\"averageCost\":0.00,\"realizedProfitLoss\":150.00,"
                + "\"tax\":0.00,\"compensableLoss\":25.00}]\n";

        Invocation result = invoke(input);

        assertEquals(0, result.exitCode());
        assertEquals(expected, result.out());
    }

    @Test
    void tenThousandAlternatingOperationsAreProcessed() {
        StringBuilder input = new StringBuilder("[");
        for (int i = 0; i < 5000; i++) {
            if (i > 0) {
                input.append(',');
            }
            input.append("{\"asset\":\"PETR\",\"type\":\"BUY\",\"unitPrice\":10.00,\"quantity\":2},")
                    .append("{\"asset\":\"PETR\",\"type\":\"SELL\",\"unitPrice\":12.00,\"quantity\":1}");
        }
        input.append("]\n");

        Invocation result = invoke(input.toString());

        assertEquals(0, result.exitCode());
        assertEquals("", result.err());
        String out = result.out();
        assertEquals(1L, out.lines().count());
        assertEquals(10000, out.split("\"operation\":", -1).length - 1);
        assertTrue(out.startsWith("[{\"operation\":1,\"asset\":\"PETR\",\"type\":\"BUY\","
                + "\"remainingQuantity\":2,\"averageCost\":10.00,"));
        assertTrue(out.endsWith("{\"operation\":10000,\"asset\":\"PETR\",\"type\":\"SELL\","
                + "\"remainingQuantity\":5000,\"averageCost\":10.00,\"realizedProfitLoss\":2.00,"
                + "\"tax\":0.00,\"compensableLoss\":0.00}]\n"));
    }
}
