# Reverse-Engineering Guide

A study roadmap for understanding this project **without an AI assistant**. It is a map, not a
walkthrough: it tells you where to look, what to ask, and how to check yourself. Answers are
deliberately absent — the point is that you can derive them from the sources and the tests.

**Suggested budget:** 3 sessions of ~60–90 minutes. Session 1: execution flow + class tour.
Session 2: Java concepts + financial questions. Session 3: edge cases + manual challenges.

**Ground rule:** read the docs (`docs/CONTRACT.md`, `docs/DOMAIN_RULES.md`) before the code, then
read each class and predict what the next one must do before you look at it.

---

## 1. Reading order

| Order | File | Why first |
|-------|------|-----------|
| 1 | `docs/CONTRACT.md` | The I/O promise. Everything else serves this file. |
| 2 | `docs/DOMAIN_RULES.md` | The financial promise, including the precision policy and the invariants (§4) that the tests target. |
| 3 | `src/main/java/com/capitalgains/Main.java` | 57 lines. The whole process boundary lives here. |
| 4 | `src/main/java/com/capitalgains/app/SimulationRunner.java` | The only orchestrator: loop, per-simulation state, buffering. |
| 5 | `src/main/java/com/capitalgains/domain/AssetPosition.java` | The mathematical core: weighted average and realization. |
| 6 | `src/main/java/com/capitalgains/domain/Portfolio.java` | Map of assets; where oversell is rejected. |
| 7 | `src/main/java/com/capitalgains/domain/LossPool.java` | 32 lines; the shared state of one simulation. |
| 8 | `src/main/java/com/capitalgains/tax/SimulatedTaxEngine.java` | The tax decision tree. |
| 9 | `src/main/java/com/capitalgains/json/SimulationParser.java` | The largest class; the validation matrix. |
| 10 | `src/main/java/com/capitalgains/json/ResultWriter.java` | Hand-built JSON, field order, money formatting. |
| 11 | `src/test/java/com/capitalgains/**` | Read tests as executable specifications, not as proof. |

---

## 2. Execution flow, traced through the real classes

```
stdin bytes
  └─ Main.main                                  (Main.java:22)
       wraps System.in/out/err in UTF-8 readers/writers
     └─ Main.execute(Reader, Writer, Writer)     (Main.java:33)
          └─ new SimulationRunner(new SimulatedTaxEngine()).run(in)   (SimulationRunner.java:34)
               ├─ BufferedReader.readLine() loop                      (:39)
               │    ├─ lineNumber++                 physical line counter, blanks included
               │    ├─ line.isBlank() -> continue   blanks produce no output line   (:41)
               │    ├─ parser.parse(line)           -> List<TradeOperation>         (:46)
               │    │     throws InvalidInputException -> rethrown with "line N: "  (:47-49)
               │    └─ process(operations)          -> one JSON array string        (:52)
               │          throws DomainException    -> converted to InvalidInputException
               │          ├─ new Portfolio() + new LossPool()   fresh per simulation (:62-63)
               │          ├─ for each operation, in array order (:66)
               │          │    ├─ portfolio.apply(op) -> realized P/L              (:67)
               │          │    ├─ if realized < 0: lossPool.add(|realized|)        (:68-70)
               │          │    ├─ taxEngine.tax(op, realized, lossPool)            (:71)
               │          │    └─ snapshot into OperationResult (index, qty, avg, pool) (:72-80)
               │          └─ writer.write(results) -> fixed-order JSON array       (:82)
               └─ output.append(simulationOutput).append('\n')     all lines buffered (:56)
          └─ Main writes the whole buffer, then flushes                     (Main.java:36-37)
               returns 0
     exceptions: InvalidInputException -> 1, IOException -> 2, other RuntimeException -> 2
     error text: "ERROR: " + message with every line break collapsed      (Main.java:48-56)
```

Three layers each add a prefix to the same failure, and knowing the order tells you who detected it:

| Layer | Example fragment | Detected by |
|-------|------------------|-------------|
| parser | `operation 1: field 'unitPrice' ...` | `SimulationParser` |
| runner | `line 2: ` + parser or domain message | `SimulationRunner` |
| CLI | `ERROR: ` + runner message | `Main` |

Two consequences worth tracing yourself:

- Nothing reaches stdout before the loop ends, because `run` returns a `String`.
- `SELL` of an unheld asset is a *domain* error (`Portfolio.apply`), not a parse error, yet it
  surfaces with the same `line N:` prefix. Find the `catch` that makes that true.

---

## 3. Class tour: what to look at, and why

| Class | Look at | Why it matters |
|-------|---------|----------------|
| `Main` | `main` vs `execute` split (:22, :33) | The process boundary is one line; everything testable takes streams as parameters. `ExitCodeTest` and `CliIntegrationTest` never spawn a JVM. |
| `SimulationRunner` | the loop (:39-57) and `process` (:61-83) | The only place that knows a "simulation" is one line with its own `Portfolio` and `LossPool`. Note what is created per line and what is created per operation. |
| `SimulationParser` | constructor (:34-42) then `parseOperation` (:61) | The constructor is where strictness lives (Jackson features). The method order — unknown fields, missing fields, then each value — decides which error a multi-fault line reports. |
| `ResultWriter` | `write` (:15) and `money` (:36) | Output is assembled by string concatenation, not by Jackson. Read the class comment and decide whether you agree that escaping is unnecessary. |
| `TradeOperation` | the record header (`TradeOperation.java:5`) | An already-validated value object: no validation methods, because only the parser constructs it. |
| `OperationType` | two constants (`OperationType.java:3`) | Why an enum rather than a boolean or a `String`? What does the parser do with anything else? |
| `AssetPosition` | `buy` (:26), `sell` (:33), the reset branch (:41) | Every rounding decision in the project is visible here: where division happens, at which scale, and where `setScale(2)` happens. |
| `Portfolio` | `apply` (:17), plus the `get` paths in `apply`/`quantity`/`averageCost` | One `Map` is the whole multi-asset model. Note the difference between "no record" and "record at quantity 0". |
| `LossPool` | `add` (:17) and `consume` (:27) | `min(balance, gain)` is the entire offset policy; the guard in `add` tells you what the class refuses to store. |
| `OperationResult` | the record header | A snapshot, not a live view — it is built *after* the tax engine may have mutated the pool. |
| `TaxEngine` | the javadoc (:10-15) | The seam: what a different engine is allowed to change, and what it is not. |
| `SimulatedTaxEngine` | `tax` (:24-39) | Four guards in a specific order (BUY, exempt, non-gain, then offset). The order determines behavior at the boundaries. |
| `DomainException` / `InvalidInputException` | both are unchecked, both carry only a message | Why two types instead of one? Who catches which, and where is the difference visible to a user? |

---

## 4. Java concepts used, and where they appear

| Concept | Where to study it |
|---------|-------------------|
| `record` | `TradeOperation.java:5`, `OperationResult.java:5`, and the test helper `Invocation` in `CliIntegrationTest.java:30`. Note what the implicit canonical constructor does *not* do — no validation — and what `equals`/`hashCode`/`toString` you get for free (and why `ParserTest` relies on that). |
| `enum` | `OperationType.java`; used in `switch` at `SimulationParser.java:89` and in the `!=` guard at `SimulatedTaxEngine.java:26`. |
| `switch` expression | `SimulationParser.parseType` (:89-94) — expression form with `->` and a `default` that throws. |
| `BigDecimal` precision vs scale | `AssetPosition.java:30` (division with explicit scale), `:39` (`setScale(2)`), `ResultWriter.java:37` (`toPlainString`). Ask why `toPlainString` and not `toString`, and what `1E+3` would look like otherwise. |
| `RoundingMode.HALF_UP` | Four call sites only: `AssetPosition.java:30`, `AssetPosition.java:39`, `SimulatedTaxEngine.java:38`, `ResultWriter.java:37`. Map each to a sentence in `DOMAIN_RULES.md` §3. |
| `BigDecimal.compareTo` vs `equals` | `SimulatedTaxEngine.java:30` compares to the threshold; tests compare `BigDecimal` values. Explain why `equals` is unsafe for numeric comparison and where scale leaks into it. |
| `Map` API | `Portfolio.java:12-39`: `computeIfAbsent`, `get`, `null`-as-absent. Note that the map is `HashMap` and that output order comes from operation order, not map order. |
| Jackson tree model + strict config | `SimulationParser.java:35-41`: `JsonFactory.builder().enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)`, `DeserializationFeature.FAIL_ON_TRAILING_TOKENS`, `USE_BIG_DECIMAL_FOR_FLOATS`, `JsonNodeFactory.withExactBigDecimals(true)`. Turn each one off in a scratch copy and find the test that fails — and the test that *doesn't*, which is itself informative. |
| Maven Shade | `pom.xml:43-63`, `ManifestResourceTransformer` with `com.capitalgains.Main`. Explains why `java -jar` works with no classpath. |
| JUnit 5 parametrized tests | `ParserTest.java` (`@ValueSource`), `RoundingTest.java` (`@CsvSource`). Note that a parametrized method counts as N tests, which is why the suite is 141 and not ~40. |
| `Assertions.assertThrows` with capture | `ParserTest.java:196`, `:223` — asserts on the message, not just the type. |
| try-with-resources | **Not used anywhere in `src/main`.** `SimulationRunner` wraps the incoming `Reader` in a `BufferedReader` and never closes it. Find the three candidate sites (`Main`'s wrappers, the `BufferedReader`, `ObjectMapper.readValue`) and decide for each whether closing would be correct — closing `System.in`/`System.out` has consequences. |

---

## 5. Questions about architectural decisions (no answers here)

1. `SimulationRunner.run` returns one `String` instead of writing as it goes. What does that buy,
   what does it cost, and which part of `docs/CONTRACT.md` would be violated without it?
2. Why is `TaxEngine` an interface when the project has exactly one production implementation and
   no plugin loading? What evidence in the code suggests the seam was chosen for *testing* rather
   than for extension?
3. Losses are added to the pool in `SimulationRunner`, but the pool is consumed inside the tax
   engine. Why is the policy split across two classes, and what would break if all of it lived in
   the engine? Which class would then need to know the realized P/L?
4. Why does `Main` expose `execute(Reader, Writer, Writer)` instead of only `main`? What kind of
   test becomes impossible if you delete it?
5. Why does the parser reject unknown fields instead of ignoring them? Which failure mode is
   worse for a caller: a silent typo (`"quanity": 100`) or a hard error?
6. Why is precision 6 for the stored average but 2 for every reported money value? What would you
   lose by reporting 6 decimals? What would you lose by storing 2?
7. Why fail fast (first bad line aborts everything) instead of returning a per-line error object
   and continuing? Which property of the contract becomes trivial under fail-fast, and which
   becomes impossible?
8. Why is the parser configured with `withExactBigDecimals(true)`, i.e. keeping the *written*
   scale instead of normalizing `10.0000000` to `10`? What user-visible rule depends on the
   difference?
9. `ResultWriter` builds JSON with `StringBuilder` while the parser uses Jackson. Why is that not
   an inconsistency? Which guarantee in the parser makes the writer's lack of escaping safe?
10. Errors are three prefix layers deep but there is only one exception type per layer. What does
    that tell you about who is responsible for adding context, and why does `Main` collapse line
    breaks in the message?
11. Both `DomainException` and `InvalidInputException` are unchecked. What would change if they
    were checked — in the parser loop, in `Portfolio`, and at the `Main` catch sites?
12. What is the memory profile of a 10-million-line input, and which README limitation admits it?

---

## 6. Questions about financial rules (no answers here)

1. `DOMAIN_RULES.md` §2 feeds the pool with losses from **exempt** sales, but an exempt sale with
   a gain does not consume it. Is that asymmetric? What would a "symmetric" rule look like, and
   which invariant in §4 would it endanger?
2. What happens for a sale with gross exactly `10000.00`? Read `SimulatedTaxEngine.java:30` and
   then `TaxEngineTest.saleWithGrossExactlyTenThousandIsTaxable`. Why is the comparison written as
   `compareTo(threshold) < 0` rather than `<= threshold`?
3. The pool is fed with `|realized|` *after* that value was rounded to 2 decimals. What is lost by
   not carrying an unrounded loss into the pool? Construct a case where the difference is at most
   one cent, and decide whether `DOMAIN_RULES.md` §3 ("no premature rounding") is violated or
   simply scoped to each reported value.
4. If the average cost were stored at scale 2 instead of 6, which rule would quietly break first —
   the reported average, the realized P/L, or the tax? Which existing test would catch it, and
   which would not?
5. Why does a `SELL` preserve the stored average of the remaining units instead of recomputing a
   cost basis from total proceeds? What accounting convention does that correspond to?
6. What is the realized P/L of a sale of the last unit at a price slightly below the average, if
   the difference times quantity rounds to zero? Does the pool grow? Does §4.8 still hold?
7. The threshold is on **gross sale value**, not on gain. Why does that make a large-quantity,
   tiny-price sale behave differently from a small-quantity, high-price sale with the same gain?
8. Once the position is flat, the stored average resets to zero. Why is that necessary for the
   *next* `BUY`, and what would the weighted average compute if the zero reset were omitted?
9. `DOMAIN_RULES.md` §4.4 says `averageCost = 0` **iff** the position is flat. Is that statement
   about the stored value or the printed value? Try to construct a counterexample for the printed
   value and decide which reading the document intends.

---

## 7. Edge cases to examine, with the test that covers them

| Edge case | Where it is pinned |
|-----------|--------------------|
| Repeating-decimal average `100/3`, then a one-cent-scale realization | `RoundingTest.repeatingDecimalAverageRealizesZeroPointSixSeven` (§5 worked example) |
| Rounding once per sale, not per unit (`0.005 × 3`) | `RoundingTest.realizedIsRoundedOncePerSaleNotPerUnit` |
| Negative half rounds away from zero (`9.995`) | `RoundingTest.negativeHalfIsRoundedAwayFromZero` |
| Weighted average at scale 6, HALF_UP, table-driven | `RoundingTest.weightedAverageRoundsHalfUpAtScaleSix` |
| Tax rounded once at 2 decimals | `RoundingTest.taxRoundsHalfUpAtScaleTwo` |
| Exemption boundary: `9999.99` vs `10000.00`, including six-decimal gross | `TaxEngineTest.saleBelowTenThousandGrossIsExemptAndLeavesPoolUntouched`, `TaxEngineTest.saleWithGrossExactlyTenThousandIsTaxable`, `CliIntegrationTest.thresholdIsEvaluatedOnExactSixDecimalGross` |
| Exempt profitable sale must not consume the pool | `TaxEngineTest.exemptProfitableSaleDoesNotConsumePool` |
| Partial and full offset, plus leftover when pool > gain | `TaxEngineTest.lossPoolPartiallyOffsetsGain`, `lossPoolFullyOffsetsGain`, `taxIsZeroWhenPoolExceedsGainAndRemainderStays` |
| BUY is never taxed and never touches the pool | `TaxEngineTest.buyIsNotTaxedAndLeavesPoolUntouched` |
| Loss carryforward across *different assets* in one simulation | `CliIntegrationTest.lossCarryforwardAcrossAssetsInOneSimulation`, `lossPoolDoesNotLeakBetweenSimulationsForTax` |
| Zero reset, then re-buy at a new average | `AssetPositionTest.positionResetsToZeroWhenFullySold`, `CliIntegrationTest.sellAllResetsAverageAndRebuyUsesTheNewAverage` |
| Oversell rejected without mutating state | `AssetPositionTest.sellAbovePositionIsRejectedWithoutMovingState`, `PortfolioTest.sellAbovePositionIsRejected`, `sellOfUnheldAssetIsRejected` |
| Scale 7 rejected, scale 6 accepted | `ParserTest.unitPriceScaleAboveSixIsRejected`, `unitPriceScaleOfSixIsAccepted`, `unitPriceWithSevenDecimalsAtTheMaximumIsRejectedByScale` |
| Exponent notation accepted for price and quantity | `ParserTest.unitPriceInExponentNotationIsAccepted`, `quantityInExponentNotationIsAcceptedWhenIntegral` |
| Malformed JSON, trailing tokens, non-array root | `ParserTest.malformedJsonIsRejected`, `ParserTest.topLevelObjectIsRejected`; for duplicate keys see challenge 4 in §8 |
| Atomicity: bad second line produces no stdout | `CliIntegrationTest.outputIsAtomicWhenSecondLineIsInvalid` |
| CRLF input | `CliIntegrationTest.crlfLineEndingsAreProcessedLikeLf` |
| Exit code 2 on I/O and unexpected failures | `ExitCodeTest` (all three) |
| Determinism and large input | `DeterminismTest`, `CliIntegrationTest.tenThousandAlternatingOperationsAreProcessed` |

---

## 8. Manual challenges (implement first, look later)

Do these **without reading the implementation of the class involved**. Verify each with the JAR
or Maven; a challenge is done when your prediction matched the observed bytes.

1. **Hand-compute a 4-operation simulation.** Predict all eight fields of every result, on paper,
   for:
   `BUY 1000 @ 10.00`, `SELL 300 @ 9.00`, `BUY 200 @ 20.00`, `SELL 900 @ 13.00`.
   Then run it through the JAR. Where does the pool matter, where does the threshold, and which
   operation's `averageCost` is *not* what the division first suggests?
2. **Two sales, same gain, different tax.** Build two single-simulation lines where the only
   difference is quantity/price, so `realizedProfitLoss` is identical but `tax` differs. Confirm
   with the JAR and name the exact rule that produces the difference.
3. **Break the precision on purpose.** Copy `AssetPosition.java` to a scratch package, store the
   average at scale 2, and build an input whose output changes (prices with long decimal
   expansions and three or more buys are your friend). Run the copy against the same input and
   diff the two outputs. Which reported field diverges first?
4. **Probe the parser's strictness.** Write a JSON line with a duplicated key (`"quantity"`
   twice) and one with a trailing second array on the same line. Both must fail — then search
   `src/test` for a test that pins the duplicate-key case. If you find one, read it; if you do
   not, write it and watch it pass.
5. **Probe the scale rule.** Produce (a) a `unitPrice` literal with seven decimals that is
   rejected, and (b) a numerically equal value in exponent form that is accepted. Explain, with
   reference to `SimulationParser.java:34-42` and `:101`, which rule fires in each case.
6. **Starve the pool.** Build a simulation where the pool exceeds the gain, then insert an exempt
   profitable sale *before* the taxable one. Predict `compensableLoss` after each operation and
   verify. Then reorder the two sales and explain the difference in final tax.
7. **Make the invariant wobble.** Try to build a non-flat position whose reported `averageCost` is
   `0.00`. Is it possible under the input contract? Which sentence in `DOMAIN_RULES.md` §4 does
   your result seem to contradict, and is the contradiction in the code or in the document?
8. **Change one rounding mode.** In a scratch copy, switch `RoundingMode.HALF_UP` to `DOWN` in
   `AssetPosition` only, then run `mvn -Dtest=RoundingTest test`. Predict which methods fail
   before you run it. Repeat with only the tax rounding changed and run `TaxEngineTest`.

---

## 9. How to explore

**Run the suite and read the report, not just the summary**

```bash
mvn -q verify
grep -h "Tests run" target/surefire-reports/*.txt
```

`target/surefire-reports/` also contains the XML with per-case timings — useful when a
parametrized test fails and you need to know *which* row.

**Run one test class, or one method**

```bash
mvn -Dtest=ParserTest test
mvn -Dtest=RoundingTest#taxRoundsHalfUpAtScaleTwo test
mvn -Dtest='*IntegrationTest' test
```

Surefire runs in the `test` phase, so this is faster than `verify` while you iterate.

**Drive the real CLI**

```bash
mvn -q package
printf '[{"asset":"PETR","type":"BUY","unitPrice":10.00,"quantity":100}]\n' \
  | java -jar target/java-capital-gains-calculator.jar; echo "exit=$?"
```

To see exact bytes (trailing newline, decimals), pipe through `od -c` or `xxd`. To see the error
channel separately: `... 2>err.txt 1>out.txt`.

**Call the classes directly, without a build cycle**

The shaded JAR is a normal classpath entry, so `jshell` can reach `Main.execute` and the domain
classes:

```bash
jshell --class-path target/java-capital-gains-calculator.jar
```

```java
var out = new java.io.StringWriter();
com.capitalgains.Main.execute(new java.io.StringReader("[]\n"), out, new java.io.StringWriter());
out.toString();
```

This is the fastest way to poke at `AssetPosition`, `LossPool` or `SimulatedTaxEngine` in
isolation — no `main`, no stdin, no process.

**Trace a single simulation by hand**

Pick the 4-operation line from challenge 1, run it, and write the intermediate values on paper in
the order `SimulationRunner.process` computes them: apply → pool → tax → snapshot. Any mismatch
tells you exactly which class to reread.

**Where to go next**

Once you can predict the output of any input line without running it, you understand the project.
The natural extensions — fees, corporate actions, per-year apuração, a second tax regime behind
`TaxEngine` — are then a design conversation, not a reverse-engineering one.
