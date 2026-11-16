# ROADMAP.md

Status legend: `☐` pending · `◐` in progress · `☑` done (accepted)

## Phase 0 — Foundation, contracts, docs ☑
Deliverables: AGENTS.md, docs/{ROADMAP,STATUS,CONTRACT,DOMAIN_RULES}.md, README stub, .gitignore, git init + commit.
Acceptance: contract unambiguous and testable; rules carry worked examples; repo clean.

## Phase 1 — Maven skeleton, operations engine, JSON I/O, CLI ☑
Deliverables: pom.xml (Java 21, Jackson, JUnit 5, shade JAR), package layout per AGENTS.md, domain engine (BUY/SELL, weighted average, multi-asset, zero reset, oversell rejection), strict JSON parser/writer, `Main` with exit codes, `ZeroTaxEngine` placeholder (tax always 0.00; loss pool already tracked and reported).
Acceptance:
- `mvn -q verify` green.
- Runnable JAR reproduces the CONTRACT example output byte-for-byte.
- Weighted average (incl. repeating decimals), zero reset, oversell rejection, multi-asset and per-line independence all covered by tests.
- No deviation from CONTRACT/DOMAIN_RULES.

## Phase 2 — Simulated tax engine ◐
Deliverables: `SimulatedTaxEngine` (15%, exemption below 10000.00 gross, loss carryforward, offsetting) replacing `ZeroTaxEngine`; tests for every rule in DOMAIN_RULES §2, incl. the exact-threshold edge and loss-pool invariants.
Acceptance: each §2 rule has a focused test; `mvn -q verify` green; docs consistent.

## Phase 3 — Robustness & edge hardening ☐
Deliverables: validation-matrix tests (malformed JSON, unknown fields, limits, scale), atomic-output and exit-code tests, parametrized rounding tests, determinism test, larger stress case.
Acceptance: every documented rejection has a test; determinism/independence verified; `mvn -q verify` green.

## Phase 4 — CI, packaging, docs ☐
Deliverables: GitHub Actions workflow (ubuntu, JDK 21, `mvn verify`), shade JAR verified runnable, README complete (build/run/examples/limitations), docs sync.
Acceptance: workflow consistent with local build; README enables independent execution.

## Phase 5 — Final audit & handoff ☐
Deliverables: fresh delegate review, fixes for real findings, `docs/REVERSE_ENGINEERING_GUIDE.md`, final commit, STATUS updated.
Acceptance: no known critical issues; limitations explicit; suite green.

## Rules
- One delegate at a time; sequential phases; repairs delegated with precise findings and a required regression test.
- Scope additions require demonstrated benefit.
