# STATUS.md — Operational State

Updated: 2026-11-16 (session 5)

## Current phase
DELIVERED — all five phases accepted. Project complete.

## Completed
- Phase 0: docs, contract, domain rules, roadmap, git init, initial commit.
- Phase 1 (ACCEPTED): Maven skeleton, operations engine, strict JSON I/O, CLI.
- Phase 2 (ACCEPTED): SimulatedTaxEngine (15%, exemption < 10000.00 exact threshold, loss carryforward, taxable-only consumption).
- Phase 3 (ACCEPTED): hardening — determinism, exit codes, boundaries, parametrized rounding, stress.
- Phase 4 (ACCEPTED): GitHub Actions CI (ubuntu, temurin 21, `mvn -B --no-transfer-progress verify`), portfolio README with byte-verified examples, YAML validated.
- Phase 5 (ACCEPTED): independent fresh audit + `docs/REVERSE_ENGINEERING_GUIDE.md` + repairs.

## Audit result (Phase 5) and disposition
- Financial correctness and contract conformance: no defects. Differential oracle (independent reference implementation built from DOMAIN_RULES only) matched the JAR byte-for-byte across 700 randomized simulations.
- Repairs applied by the orchestrator after triage (all MINOR/NIT):
  - M1: missing-field error message made deterministic (ordered required-field list) + determinism probe.
  - M2: oversell message now always names the asset (Portfolio pre-check), consistent for never-bought and flattened positions + e2e test.
  - M3: DOMAIN_RULES §4.4 and CONTRACT clarified — invariant holds on the stored scale-6 value; sub-cent averages display as 0.00; behavior pinned by e2e test.
  - M4: duplicate-key rejection now tested (both duplicate quantity and duplicate asset).
  - M5: realized-loss/realized-zero engine tests retargeted to a taxable gross (10000.00) so they exercise the intended guard.
  - M6/N1/N6/N7: README guide link + documentation index, ROADMAP wording, "non-blank input line", partial-stdout-only-on-write-failure wording.
  - N2/N3: LossPool guards non-positive gains on consume; DomainException instead of IllegalArgumentException; tests updated.
  - N5: 15% basic-rate test uses a different gross (20000.00) than the exact-threshold test.
  - N4/N8: accepted as documented notes (no speculative abstraction; pool-policy split is documented in guide questions).
- Earlier cleanup: unused `ZeroTaxEngine` removed (AGENTS.md/README updated).

## Key files
- Contract: docs/CONTRACT.md (source of truth for I/O) · Rules: docs/DOMAIN_RULES.md (source of truth for finance)
- Plan: docs/ROADMAP.md · Study guide: docs/REVERSE_ENGINEERING_GUIDE.md
- CI: .github/workflows/ci.yml · Build: pom.xml (jackson-databind 2.23.0, junit-jupiter 5.14.4, surefire 3.6.1, shade 3.6.2, release 21)
- Core: src/main/java/com/capitalgains/** · Tests: src/test/java/com/capitalgains/**

## Tests executed
- 144 green (`mvn verify`): ParserTest 66, RoundingTest 23, CliIntegrationTest 22, TaxEngineTest 12, AssetPositionTest 6, LossPoolTest 5, PortfolioTest 5, ExitCodeTest 3, DeterminismTest 2.
- Post-repair probes: flat-oversell message names the asset; sub-cent average displays 0.00 with realized 1.00 preserved; missing-field stderr identical across 3 JVM runs.

## Open problems
- CI workflow has never executed on GitHub (no remote configured) — verified structurally and by mirrored local goals only. Explicit documented limitation.

## Next concrete action
- None required. Optional next steps (user decision): create a GitHub remote and push (requires explicit authorization), add a LICENSE, or extend the tax simulation with new rules via a new roadmap cycle.

## Pending decisions
- None.

