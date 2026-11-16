# AGENTS.md — Agent Instructions

## Project

`java-capital-gains-calculator` — a pure-Java 21 CLI that processes stock trade simulations from stdin (JSON) and writes results to stdout (JSON): weighted average cost, realized profit/loss, and a *simulated* educational capital-gains tax.

## Non-negotiable conventions

- Java 21 (`maven.compiler.release=21`), Maven build. No Spring, no REST, no runtime dependency beyond Jackson.
- All monetary math uses `BigDecimal`. `float`/`double` are forbidden in any calculation.
- CLI is fail-fast: the first invalid input aborts the whole run with a nonzero exit code and no partial stdout. Output is atomic.
- Follow [docs/CONTRACT.md](docs/CONTRACT.md) exactly (I/O shape, validation, exit codes).
- Financial rules and precision policy live in [docs/DOMAIN_RULES.md](docs/DOMAIN_RULES.md). Never change rules without updating that document and its tests in the same change.
- Tax rules are an explicit educational simulation — never present them as real Brazilian legislation.
- Tests are mandatory for every behavioral change. Run `mvn -q verify` before declaring any task done.
- Keep diffs minimal; no speculative abstractions; comments only where non-obvious.

## Architecture

Single Maven module, package root `com.capitalgains`:

- `Main` — CLI entry point: reads stdin, delegates, maps failures to exit codes (0 / 1 / 2).
- `json` — strict line-delimited JSON parsing (Jackson) and result serialization (fixed field order).
- `domain` — `TradeOperation`, `OperationType`, `AssetPosition` (weighted average state), `Portfolio` (per-asset positions), `OperationResult`, `LossPool`.
- `tax` — `TaxEngine` interface and `SimulatedTaxEngine` (production: 15% rate, R$ 10,000 gross-value exemption, loss carryforward).
- `app` — `SimulationRunner`: parse → process each simulation independently → tax → serialize; buffers all output (atomicity).

Processing order is the order received; simulations are independent; assets within a simulation share only the loss pool.

## Commands

- Build + tests: `mvn -q verify`
- Package runnable JAR: `mvn -q package` → `target/java-capital-gains-calculator.jar`
- Run: `java -jar target/java-capital-gains-calculator.jar < simulations.jsonl`

## Project state & continuity

On session start or context compaction, read in order: `AGENTS.md` → `docs/ROADMAP.md` → `docs/STATUS.md` → repo state. Resume the next pending task; never restart planning if the roadmap is still valid. Update `docs/STATUS.md` at every phase completion or meaningful checkpoint.
