# STATUS.md — Operational State

Updated: 2026-11-16 (session 2)

## Current phase
Phase 3 — robustness & edge hardening (about to be delegated).

## Completed
- Phase 0: docs, contract, domain rules, roadmap, git init, initial commit.
- Phase 1 (ACCEPTED): Maven skeleton, operations engine, strict JSON I/O, CLI, ZeroTaxEngine placeholder. 78 tests.
  - Build: jackson-databind 2.23.0, junit-jupiter 5.14.4, surefire 3.6.1, shade 3.6.2; release 21.
  - Contract amendments recorded in docs/CONTRACT.md (scale-as-written unitPrice, integral quantity accepts 100.00, duplicate keys rejected) and docs/DOMAIN_RULES.md (averageCost serialization HALF_UP).
- Phase 2 (ACCEPTED after review): SimulatedTaxEngine (15%, exemption < 10000.00 exact threshold, loss carryforward, taxable-only consumption), wired into Main; ZeroTaxEngine kept as unused placeholder. 95 tests.
  - Orchestrator e2e verification: carryforward pool 1000.00 → tax 1650.00 / pool 0.00; exempt profitable sale does not consume pool; partial offset → 150.00; gross exactly 10000.00 → 750.00; no cross-simulation leak (full-rate 300.00); 9999.9999 exempt vs 10000.0001 taxable → 1499.85; CONTRACT example byte-for-byte unchanged.

## Key files
- Contract: docs/CONTRACT.md (source of truth for I/O)
- Rules: docs/DOMAIN_RULES.md (source of truth for finance)
- Plan: docs/ROADMAP.md
- Tax engine: src/main/java/com/capitalgains/tax/SimulatedTaxEngine.java
- Core: src/main/java/com/capitalgains/{Main.java,app/SimulationRunner.java,domain/*,json/*,tax/*}

## Tests executed
- 95 green (`mvn verify`): CliIntegrationTest 16, ParserTest 48, TaxEngineTest 12, AssetPositionTest 6, LossPoolTest 5, PortfolioTest 5, RoundingTest 3.

## Open problems
- None known.

## Next concrete action
- Delegate Phase 3: determinism test, atomicity/exit-code coverage incl. exit 2 via injected failing writer, boundary values (qty/price/asset limits), sell-all-then-rebuy avg-reset e2e, parametrized rounding, CRLF input, stress line (~10k ops). Delegate must read existing test suites first and add only missing cases.

## Pending decisions
- None.
