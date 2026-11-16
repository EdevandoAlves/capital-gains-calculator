package com.capitalgains.app;

import com.capitalgains.Main;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * CONTRACT.md exit code 2: unexpected internal errors (I/O failure or bug).
 */
class ExitCodeTest {

    private static final String VALID_INPUT =
            "[{\"asset\":\"PETR\",\"type\":\"BUY\",\"unitPrice\":10.00,\"quantity\":1}]\n";

    @Test
    void stdoutWriteFailureExitsTwoAndAcceptsNothing() {
        FailingWriter out = new FailingWriter();
        StringWriter err = new StringWriter();

        int exitCode = Main.execute(new StringReader(VALID_INPUT), out, err);

        assertEquals(2, exitCode);
        assertEquals(1, out.writeAttempts());
        assertEquals("", out.written());
        assertTrue(err.toString().startsWith("ERROR: I/O failure: "));
        assertEquals(1L, err.toString().lines().count());
    }

    @Test
    void stdinReadFailureExitsTwo() {
        Reader failingInput = new Reader() {
            @Override
            public int read(char[] buffer, int offset, int length) throws IOException {
                throw new IOException("read failed");
            }

            @Override
            public void close() {
            }
        };
        StringWriter out = new StringWriter();
        StringWriter err = new StringWriter();

        int exitCode = Main.execute(failingInput, out, err);

        assertEquals(2, exitCode);
        assertEquals("", out.toString());
        assertTrue(err.toString().startsWith("ERROR: I/O failure: read failed"));
        assertEquals(1L, err.toString().lines().count());
    }

    @Test
    void unexpectedRuntimeFailureExitsTwo() {
        Reader failingInput = new Reader() {
            @Override
            public int read(char[] buffer, int offset, int length) {
                throw new IllegalStateException("boom");
            }

            @Override
            public void close() {
            }
        };
        StringWriter out = new StringWriter();
        StringWriter err = new StringWriter();

        int exitCode = Main.execute(failingInput, out, err);

        assertEquals(2, exitCode);
        assertEquals("", out.toString());
        assertTrue(err.toString().startsWith("ERROR: internal error: boom"));
        assertEquals(1L, err.toString().lines().count());
    }

    private static final class FailingWriter extends Writer {

        private final StringBuilder buffer = new StringBuilder();
        private int writeAttempts;

        @Override
        public void write(char[] characters, int offset, int length) throws IOException {
            writeAttempts++;
            throw new IOException("disk full");
        }

        @Override
        public void flush() {
        }

        @Override
        public void close() {
        }

        int writeAttempts() {
            return writeAttempts;
        }

        String written() {
            return buffer.toString();
        }
    }
}
