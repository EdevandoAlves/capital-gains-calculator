# STATUS.md — Operational State

Updated: 2026-11-16 (session 4)

## Current phase
Phase 5 — final audit, REVERSE_ENGINEERING_GUIDE.md, handoff (about to be delegated).

## Completed
- Phase 0: docs, contract, domain rules, roadmap, git init, initial commit.
- Phase 1 (ACCEPTED): Maven skeleton, operations engine, strict JSON I/O, CLI (placeholder tax engine).
- Phase 2 (ACCEPTED): SimulatedTaxEngine (15%, exemption < 10000.00 exact threshold, loss carryforward, taxable-only consumption), wired into Main. 95 tests.
- Phase 3 (ACCEPTED): hardening — determinism, exit codes, boundaries, parametrized rounding, stress (141 tests, tests-only).
  - Contract amendment (orchestrator decision): exponent notation pinned in docs/CONTRACT.md — unitPrice scale validated on the parsed value; quantity accepts any integral JSON number form.
- Phase 4 (ACCEPTED): GitHub Actions CI (push/pull_request, ubuntu-latest, temurin 21, Maven cache, `mvn -B --no-transfer-progress verify`, surefire artifact with `if: always()`); portfolio README (disclaimer, requirements, build/run, 3-simulation example, contract summary, exit codes, architecture, limitations); YAML parsed OK; packaging verified from clean state.
- Cleanup (orchestrator, small finding): removed unused `ZeroTaxEngine`; AGENTS.md/README tax bullets updated; `mvn -q verify` green after removal.

## Key files
- Contract: docs/CONTRACT.md (source of truth for I/O)
- Rules: docs/DOMAIN_RULES.md (source of truth for finance)
- Plan: docs/ROADMAP.md
- CI: .github/workflows/ci.yml (mirrors local `mvn -B verify`)
- Core: src/main/java/com/capitalgains/** · Tests: src/test/java/com/capitalgains/**
- Build: pom.xml (jackson-databind 2.23.0, junit-jupiter 5.14.4, surefire 3.6.1, shade 3.6.2, release 21)

## Tests executed
- 141 green (`mvn verify`) after the ZeroTaxEngine removal: ParserTest 65, RoundingTest 23, CliIntegrationTest 20, TaxEngineTest 12, AssetPositionTest 6, LossPoolTest 5, PortfolioTest 5, ExitCodeTest 3, DeterminismTest 2.
- README example re-run by orchestrator: byte-identical to the README output block (README_BYTES_OK).

## Open problems
- CI workflow has never executed on GitHub (no remote configured) — verified structurally and by mirrored local goals only. Explicit limitation, documented in README/ROADMAP.

## Next concrete action
- Delegate Phase 5: fresh independent audit (findings only, no fixes) + write docs/REVERSE_ENGINEERING_GUIDE.md; orchestrator triage of findings, repairs if needed, final commit.

## Pending decisions
- None (ZeroTaxEngine removal decided and done).
