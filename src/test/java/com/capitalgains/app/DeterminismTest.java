package com.capitalgains.app;

import com.capitalgains.Main;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * DOMAIN_RULES §4.6: identical input bytes must produce identical output bytes.
 */
class DeterminismTest {

    private static final String MULTI_LINE_INPUT =
            "[{\"asset\":\"PETR4\",\"type\":\"BUY\",\"unitPrice\":10.00,\"quantity\":100},"
                    + "{\"asset\":\"PETR4\",\"type\":\"SELL\",\"unitPrice\":12.50,\"quantity\":50}]\n"
                    + "[{\"asset\":\"VALE3\",\"type\":\"BUY\",\"unitPrice\":20.00,\"quantity\":100},"
                    + "{\"asset\":\"VALE3\",\"type\":\"SELL\",\"unitPrice\":10.00,\"quantity\":100},"
                    + "{\"asset\":\"ITUB4\",\"type\":\"BUY\",\"unitPrice\":40.00,\"quantity\":200},"
                    + "{\"asset\":\"ITUB4\",\"type\":\"SELL\",\"unitPrice\":100.00,\"quantity\":200}]\n"
                    + "[]\n"
                    + "[{\"asset\":\"ABCDEFGHIJKLMNOPQRST\",\"type\":\"BUY\",\"unitPrice\":0.000001,"
                    + "\"quantity\":1000000000}]\n";

    private static String run(String input) {
        StringWriter out = new StringWriter();
        StringWriter err = new StringWriter();
        int exitCode = Main.execute(new StringReader(input), out, err);
        assertEquals(0, exitCode);
        assertEquals("", err.toString());
        return out.toString();
    }

    @Test
    void sameInputProducesIdenticalBytesAcrossRuns() {
        String first = run(MULTI_LINE_INPUT);
        String second = run(MULTI_LINE_INPUT);

        assertEquals(first, second);
        assertEquals(4L, first.lines().count());
    }

    @Test
    void largeInputIsReproducible() {
        StringBuilder input = new StringBuilder();
        for (int i = 0; i < 300; i++) {
            input.append(line(i)).append('\n');
        }

        String first = run(input.toString());
        String second = run(input.toString());

        assertEquals(first, second);
        assertEquals(300L, first.lines().count());
    }

    private static String line(int index) {
        int buyPrice = 10 + (index % 5);
        int sellPrice = 12 + (index % 3);
        String tail = index % 10 == 0
                ? "{\"asset\":\"VALE3\",\"type\":\"BUY\",\"unitPrice\":100.00,\"quantity\":100},"
                        + "{\"asset\":\"VALE3\",\"type\":\"SELL\",\"unitPrice\":150.00,"
                        + "\"quantity\":100}"
                : "{\"asset\":\"VALE3\",\"type\":\"BUY\",\"unitPrice\":30.00,\"quantity\":10}";
        return "[{\"asset\":\"PETR4\",\"type\":\"BUY\",\"unitPrice\":" + buyPrice + ".00,"
                + "\"quantity\":100},"
                + "{\"asset\":\"PETR4\",\"type\":\"SELL\",\"unitPrice\":" + sellPrice + ".00,"
                + "\"quantity\":50}," + tail + "]";
    }
}
