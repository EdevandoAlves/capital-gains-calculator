# java-capital-gains-calculator

A Java 21 CLI application for processing stock trades, calculating weighted average cost, realized profit/loss, and simulated capital gains taxes.

> Status: under active development (Phase 1). Plan: [docs/ROADMAP.md](docs/ROADMAP.md).

The tax rules are an **educational simulation** — they do not represent Brazilian legislation. See [docs/DOMAIN_RULES.md](docs/DOMAIN_RULES.md).

## Build & test (JDK 21+, Maven 3.9+)

```bash
mvn -q verify
```

## Usage

```bash
java -jar target/java-capital-gains-calculator.jar < simulations.jsonl
```

Input/output contract: [docs/CONTRACT.md](docs/CONTRACT.md).
