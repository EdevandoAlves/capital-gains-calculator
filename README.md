# java-capital-gains-calculator

A pure-Java 21 CLI for processing stock-trade simulations from stdin and writing deterministic
JSON results to stdout: weighted average cost, realized profit/loss, and a **simulated**
educational capital-gains tax.

No frameworks, no services, no network — just a runnable JAR with Jackson for JSON. All monetary
arithmetic uses `BigDecimal`, and the same input bytes always produce the same output bytes.

> **Educational simulation — not Brazilian tax legislation.**
> The tax rules implemented here (15% rate, gross-value exemption, loss carryforward) are a
> fictional, simplified exercise used to model the mechanics. They are **not** a statement of
> Brazilian law, are not tax advice, and must not be used to compute real tax liability.
> See [docs/DOMAIN_RULES.md](docs/DOMAIN_RULES.md) §2 for the exact rules implemented.

## Requirements

- JDK 21 or newer
- Maven 3.9 or newer

## Build and test

```bash
mvn verify
```

This compiles the sources, runs the full test suite, and produces the runnable JAR as part of the
`package` phase.

## Package

```bash
mvn package
```

The shaded, self-contained artifact is written to `target/java-capital-gains-calculator.jar`.
It includes its Jackson runtime dependencies and the `Main-Class` manifest entry, so it runs with
nothing else on the classpath.

## Usage

The process reads **one JSON simulation per line** from stdin and writes one JSON result line per
simulation, in the same order:

```bash
java -jar target/java-capital-gains-calculator.jar < simulations.jsonl
```

or directly from a heredoc:

```bash
java -jar target/java-capital-gains-calculator.jar <<'EOF'
[{"asset":"PETR4","type":"BUY","unitPrice":20.00,"quantity":100},{"asset":"PETR4","type":"SELL","unitPrice":150.00,"quantity":100}]
[{"asset":"VALE3","type":"BUY","unitPrice":30.00,"quantity":200},{"asset":"VALE3","type":"SELL","unitPrice":25.00,"quantity":200},{"asset":"VALE3","type":"BUY","unitPrice":100.00,"quantity":100},{"asset":"VALE3","type":"SELL","unitPrice":200.00,"quantity":100}]
[{"asset":"ITSA4","type":"BUY","unitPrice":50.00,"quantity":10},{"asset":"ITSA4","type":"SELL","unitPrice":60.00,"quantity":10}]
EOF
```

Output (exit code `0`):

```json
[{"operation":1,"asset":"PETR4","type":"BUY","remainingQuantity":100,"averageCost":20.00,"realizedProfitLoss":0.00,"tax":0.00,"compensableLoss":0.00},{"operation":2,"asset":"PETR4","type":"SELL","remainingQuantity":0,"averageCost":0.00,"realizedProfitLoss":13000.00,"tax":1950.00,"compensableLoss":0.00}]
[{"operation":1,"asset":"VALE3","type":"BUY","remainingQuantity":200,"averageCost":30.00,"realizedProfitLoss":0.00,"tax":0.00,"compensableLoss":0.00},{"operation":2,"asset":"VALE3","type":"SELL","remainingQuantity":0,"averageCost":0.00,"realizedProfitLoss":-1000.00,"tax":0.00,"compensableLoss":1000.00},{"operation":3,"asset":"VALE3","type":"BUY","remainingQuantity":100,"averageCost":100.00,"realizedProfitLoss":0.00,"tax":0.00,"compensableLoss":1000.00},{"operation":4,"asset":"VALE3","type":"SELL","remainingQuantity":0,"averageCost":0.00,"realizedProfitLoss":10000.00,"tax":1350.00,"compensableLoss":0.00}]
[{"operation":1,"asset":"ITSA4","type":"BUY","remainingQuantity":10,"averageCost":50.00,"realizedProfitLoss":0.00,"tax":0.00,"compensableLoss":0.00},{"operation":2,"asset":"ITSA4","type":"SELL","remainingQuantity":0,"averageCost":0.00,"realizedProfitLoss":100.00,"tax":0.00,"compensableLoss":0.00}]
```

What the three simulations show:

1. **Taxable gain** — PETR4 sold for a gross value of 15,000.00, above the exemption threshold:
   13,000.00 of gain is taxed at 15%, producing 1,950.00 of simulated tax.
2. **Loss carryforward** — VALE3 realizes a 1,000.00 loss, which builds the simulation's loss pool.
   A later taxable gain of 10,000.00 is reduced by that pool before tax: 9,000.00 taxable at 15%
   gives 1,350.00, and the pool drains back to zero.
3. **Exempt sale** — ITSA4's gross value of 600.00 is below the threshold, so the 100.00 gain
   produces no tax.

### Error behavior

The CLI is **fail-fast and atomic**: results are buffered and flushed only after every line has
succeeded. The first invalid input produces no stdout at all and a single `ERROR: <reason>` line on
stderr. For example, selling an asset that is not held:

```bash
$ printf '[{"asset":"PETR4","type":"SELL","unitPrice":10.00,"quantity":1}]\n' \
    | java -jar target/java-capital-gains-calculator.jar
ERROR: line 1: SELL of 1 PETR4 exceeds position of 0
$ echo $?
1
```

## Contract (summary)

Full specification: [docs/CONTRACT.md](docs/CONTRACT.md).

Each stdin line is a JSON array of operations, processed in array order:

| field       | type    | rules                                                                     |
|-------------|---------|---------------------------------------------------------------------------|
| `asset`     | string  | `^[A-Z0-9]{1,20}$`                                                        |
| `type`      | string  | `"BUY"` or `"SELL"` (case-sensitive)                                      |
| `unitPrice` | number  | `> 0` and `<= 1000000000`, at most 6 decimal places                       |
| `quantity`  | integer | integral JSON number, `>= 1` and `<= 1000000000` (no fractional shares)   |

Every result line is a JSON array with one object per operation, in a fixed field order:
`operation`, `asset`, `type`, `remainingQuantity`, `averageCost`, `realizedProfitLoss`, `tax`,
`compensableLoss`. Monetary fields always carry exactly two decimals. Empty and whitespace-only
input lines are ignored; an empty simulation (`[]`) produces `[]`. Unknown fields, duplicate keys,
wrong types, out-of-range values, and overselling are rejected. Each simulation is independent:
assets within a simulation share a loss pool, but nothing is carried across input lines.

Exit codes:

| code | meaning                                                      |
|------|--------------------------------------------------------------|
| 0    | all simulations processed and output written                 |
| 1    | input/validation failure (nothing written to stdout)         |
| 2    | unexpected internal error (I/O failure or bug)               |

## Architecture

Single Maven module, package root `com.capitalgains`:

- `Main` — CLI entry point: stdin/stdout wiring with the production `SimulatedTaxEngine`, mapping
  failures to exit codes 0 / 1 / 2.
- `json` — strict line-delimited JSON parsing with Jackson and result serialization in fixed field
  order.
- `domain` — `TradeOperation`, `OperationType`, `AssetPosition` (weighted-average state),
  `Portfolio` (per-asset positions), `OperationResult`, `LossPool`.
- `tax` — `TaxEngine` interface and `SimulatedTaxEngine` (production: 15% simulated rate,
  R$ 10,000 gross-value exemption, loss carryforward).
- `app` — `SimulationRunner`: parse → process each simulation independently → tax → serialize;
  buffers all output to guarantee atomicity.

Simulations run strictly in input order; assets within one simulation are independent and share
only that simulation's loss pool.

A deeper walkthrough of the design and the reasoning behind it — `docs/REVERSE_ENGINEERING_GUIDE.md`
— is planned for the final phase.

## Limitations

- **One JVM run per invocation.** There is no server, daemon, or interactive mode; each run reads
  all of stdin and exits.
- **No persistence.** Positions, realized results, and loss pools live only in memory for the
  duration of a single simulation line. Nothing is carried across lines or across runs.
- **Simulated tax only.** The rate, exemption threshold, and carryforward rules are an educational
  fiction, not Brazilian legislation (see the disclaimer and [docs/DOMAIN_RULES.md](docs/DOMAIN_RULES.md) §2).
- **No network.** The tool never fetches prices, corporate actions, or tax tables.
- **No corporate actions.** Splits, bonuses, dividends, fees, and brokerage costs are out of scope;
  only BUY and SELL operations exist.
- **Documented bounds.** `quantity` and `unitPrice` are limited to 1,000,000,000, `unitPrice` to 6
  decimal places, and fractional shares are rejected. Short positions are not supported: a SELL
  above the current position aborts the run.
- **Buffered output.** The whole result is held in memory before being written, which keeps output
  atomic at the cost of memory proportional to the total output size.

## Documentation

- [docs/CONTRACT.md](docs/CONTRACT.md) — public CLI contract: input/output shape, validation, exit codes.
- [docs/DOMAIN_RULES.md](docs/DOMAIN_RULES.md) — financial rules, precision policy, invariants.
- [docs/ROADMAP.md](docs/ROADMAP.md) — phase plan and acceptance criteria.
- [docs/STATUS.md](docs/STATUS.md) — current project state.
- [AGENTS.md](AGENTS.md) — engineering conventions for contributors and agents.

## License

None declared for now. This is a portfolio/educational project, and no `LICENSE` file is included.
