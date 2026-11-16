package com.capitalgains.app;

import com.capitalgains.domain.DomainException;
import com.capitalgains.domain.LossPool;
import com.capitalgains.domain.OperationResult;
import com.capitalgains.domain.Portfolio;
import com.capitalgains.domain.TradeOperation;
import com.capitalgains.json.InvalidInputException;
import com.capitalgains.json.ResultWriter;
import com.capitalgains.json.SimulationParser;
import com.capitalgains.tax.TaxEngine;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Parses every input line, processes each simulation independently, and buffers the whole
 * output so that nothing is emitted unless every line succeeds (CONTRACT.md).
 */
public final class SimulationRunner {

    private final SimulationParser parser = new SimulationParser();
    private final ResultWriter writer = new ResultWriter();
    private final TaxEngine taxEngine;

    public SimulationRunner(TaxEngine taxEngine) {
        this.taxEngine = taxEngine;
    }

    public String run(Reader reader) throws IOException {
        BufferedReader lines = new BufferedReader(reader);
        StringBuilder output = new StringBuilder();
        long lineNumber = 0;
        String line;
        while ((line = lines.readLine()) != null) {
            lineNumber++;
            if (line.isBlank()) {
                continue;
            }
            List<TradeOperation> operations;
            try {
                operations = parser.parse(line);
            } catch (InvalidInputException e) {
                throw new InvalidInputException("line " + lineNumber + ": " + e.getMessage());
            }
            String simulationOutput;
            try {
                simulationOutput = process(operations);
            } catch (DomainException e) {
                throw new InvalidInputException("line " + lineNumber + ": " + e.getMessage());
            }
            output.append(simulationOutput).append('\n');
        }
        return output.toString();
    }

    private String process(List<TradeOperation> operations) {
        Portfolio portfolio = new Portfolio();
        LossPool lossPool = new LossPool();
        List<OperationResult> results = new ArrayList<>(operations.size());
        int index = 1;
        for (TradeOperation operation : operations) {
            BigDecimal realized = portfolio.apply(operation);
            if (realized.signum() < 0) {
                lossPool.add(realized.negate());
            }
            BigDecimal tax = taxEngine.tax(operation, realized, lossPool);
            results.add(new OperationResult(
                    index++,
                    operation.asset(),
                    operation.type(),
                    portfolio.quantity(operation.asset()),
                    portfolio.averageCost(operation.asset()),
                    realized,
                    tax,
                    lossPool.balance()));
        }
        return writer.write(results);
    }
}
