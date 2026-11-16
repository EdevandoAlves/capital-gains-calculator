# STATUS.md — Operational State

Updated: 2026-11-16 (session 3)

## Current phase
Phase 4 — CI, packaging, docs (about to be delegated).

## Completed
- Phase 0: docs, contract, domain rules, roadmap, git init, initial commit.
- Phase 1 (ACCEPTED): Maven skeleton, operations engine, strict JSON I/O, CLI, ZeroTaxEngine placeholder.
- Phase 2 (ACCEPTED): SimulatedTaxEngine (15%, exemption < 10000.00 exact threshold, loss carryforward, taxable-only consumption), wired into Main. 95 tests.
- Phase 3 (ACCEPTED): hardening — 141 tests total, tests-only change (no production edits).
  - New: DeterminismTest (byte-equality across runs, 300-line reproducibility), ExitCodeTest (exit 2 via failing reader/writer, single ERROR line, nothing written), boundary matrix in ParserTest (qty/price/asset limits, non-object elements, trailing tokens, exponent notation), CRLF handling, sell-all→rebuy avg reset e2e, interleaved multi-asset e2e, 10k-op stress (0.12 s), 23 parametrized rounding cases incl. scale-6 and scale-2 HALF_UP ties.
  - Contract amendment (orchestrator decision): exponent notation pinned in docs/CONTRACT.md — unitPrice scale validated on the parsed value (decimal literal keeps written scale; `1.0e1` normalized to scale 0); quantity accepts any integral JSON number form.

## Key files
- Contract: docs/CONTRACT.md (source of truth for I/O)
- Rules: docs/DOMAIN_RULES.md (source of truth for finance)
- Plan: docs/ROADMAP.md
- Core: src/main/java/com/capitalgains/** · Tests: src/test/java/com/capitalgains/**
- Build: pom.xml (jackson-databind 2.23.0, junit-jupiter 5.14.4, surefire 3.6.1, shade 3.6.2, release 21)

## Tests executed
- 141 green (`mvn verify`): ParserTest 65, RoundingTest 23, CliIntegrationTest 20, TaxEngineTest 12, AssetPositionTest 6, LossPoolTest 5, PortfolioTest 5, ExitCodeTest 3, DeterminismTest 2.
- Orchestrator CLI probes re-run: PETR4 micro-price × 1e9 qty → realized 1000.00, exempt; exponent-form input (1e3 / 2.0e0 / 1.0e2) → realized −99800.00, pool 99800.00.

## Open problems
- None known.

## Next concrete action
- Delegate Phase 4: GitHub Actions CI (ubuntu, JDK 21 temurin, `mvn -B verify`), README completion (build/run/examples/errors/limitations/simulation disclaimer), docs consistency pass (AGENTS.md architecture note still describes ZeroTaxEngine as production wiring — update), JAR end-to-end from a clean package.

## Pending decisions
- None.
