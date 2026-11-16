# STATUS.md — Operational State

Updated: 2026-11-16 (session 1)

## Current phase
Phase 2 — simulated tax engine (about to be delegated).

## Completed
- Phase 0: docs, contract, domain rules, roadmap, git init, initial commit.
- Phase 1 (ACCEPTED after review): Maven skeleton, operations engine, strict JSON I/O, CLI, ZeroTaxEngine placeholder.
  - Build: jackson-databind 2.23.0, junit-jupiter 5.14.4, surefire 3.6.1, shade 3.6.2; release 21.
  - Verified by orchestrator: `mvn -q verify` green, 78 tests; CONTRACT example byte-for-byte; repeating-decimal avg 33.33/0.67; loss pool −500.00 → compensableLoss 500.00 with avg reset; oversell/unknown-field/qty 1.5/price 7dp all exit 1 with empty stdout; blanks ignored; `[]` → `[]`; line independence; no float/double.
  - Contract amendments recorded in docs/CONTRACT.md (scale-as-written for unitPrice, integral quantity accepts 100.00, duplicate keys rejected) and docs/DOMAIN_RULES.md (averageCost serialization HALF_UP).

## Key files
- Contract: docs/CONTRACT.md (source of truth for I/O)
- Rules: docs/DOMAIN_RULES.md (source of truth for finance)
- Plan: docs/ROADMAP.md
- Core: src/main/java/com/capitalgains/{Main.java,app/SimulationRunner.java,domain/*,json/*,tax/*}

## Tests executed
- 78 tests green (`mvn verify`), suites: CliIntegrationTest 11, AssetPositionTest 6, LossPoolTest 5, PortfolioTest 5, RoundingTest 3, ParserTest 48.

## Open problems
- None known.

## Next concrete action
- Delegate Phase 2: `SimulatedTaxEngine` per DOMAIN_RULES §2, swap ZeroTaxEngine in Main/SimulationRunner wiring, focused tests per §2 rule (15%, < 10000.00 exemption incl. exact-threshold edge, loss carryforward, taxable-only consumption, non-negative tax, per-simulation apuração).

## Pending decisions
- None.
