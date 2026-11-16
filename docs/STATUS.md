# STATUS.md — Operational State

Updated: 2026-11-16 (session 1)

## Current phase
Phase 1 — implementation delegated to DeepSeek V4.1 Flash (OpenCode).

## Completed
- Phase 0: docs, contract, domain rules, roadmap, git init, initial commit.

## Key files
- Contract: docs/CONTRACT.md (source of truth for I/O)
- Rules: docs/DOMAIN_RULES.md (source of truth for finance)
- Plan: docs/ROADMAP.md

## Tests executed
- None yet (no code). Phase 1 must deliver `mvn -q verify` green.

## Open problems
- None.

## Next concrete action
- Review Phase 1 delegate output: inspect diff, check contract conformance and financial rules, run `mvn -q verify` and the CONTRACT-example JAR smoke test.

## Pending decisions
- None. Jackson/JUnit versions chosen by the Phase 1 delegate are recorded here afterwards.
