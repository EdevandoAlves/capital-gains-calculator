package com.capitalgains;

import com.capitalgains.app.SimulationRunner;
import com.capitalgains.json.InvalidInputException;
import com.capitalgains.tax.SimulatedTaxEngine;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;

/**
 * CLI entry point: stdin to stdout, mapping failures to exit codes (CONTRACT.md).
 */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        int exitCode = execute(
                new InputStreamReader(System.in, StandardCharsets.UTF_8),
                new OutputStreamWriter(System.out, StandardCharsets.UTF_8),
                new OutputStreamWriter(System.err, StandardCharsets.UTF_8));
        System.exit(exitCode);
    }

    /**
     * Runs one invocation against the supplied streams and returns the process exit code.
     */
    public static int execute(Reader in, Writer out, Writer err) {
        try {
            String output = new SimulationRunner(new SimulatedTaxEngine()).run(in);
            out.write(output);
            out.flush();
            return 0;
        } catch (InvalidInputException e) {
            return fail(err, e.getMessage(), 1);
        } catch (IOException e) {
            return fail(err, "I/O failure: " + e.getMessage(), 2);
        } catch (RuntimeException e) {
            return fail(err, "internal error: " + e.getMessage(), 2);
        }
    }

    private static int fail(Writer err, String reason, int exitCode) {
        try {
            err.write("ERROR: " + reason.replaceAll("\\R", " ") + "\n");
            err.flush();
        } catch (IOException ignored) {
            // stderr is best-effort; the exit code still signals the failure
        }
        return exitCode;
    }
}
