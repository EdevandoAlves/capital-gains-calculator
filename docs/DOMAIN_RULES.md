# DOMAIN_RULES.md — Financial Rules, Precision Policy, Invariants

These rules are part of the tested contract. Any change requires updating this document and tests in the same change.

## 1. Positions

- Each `asset` has an independent position: `{quantity, weightedAverageCost}`.
- **BUY**: adds `quantity` units; no P/L, no tax.
  `newAvg = (oldQty * oldAvg + qty * unitPrice) / (oldQty + qty)` — division computed at scale 6, HALF_UP; the average is stored at scale 6.
- **SELL**: reduces the position; rejected if `quantity > position` (fail-fast, per CONTRACT).
  `realized = (unitPrice - avg) * qty`, rounded **once** to scale 2, HALF_UP, at realization.
  Remaining units keep the stored average. When the position reaches zero, the average resets to `0`.
- Assets are independent; the only shared state within a simulation is the loss pool (§2).

## 2. Simulated tax engine (educational — NOT Brazilian legislation)

Apuração is per simulation. State: a single accumulated-loss pool shared by all assets of the simulation.

- Rate: **15%** over the taxable gain of a sale.
- Gross sale value `gross = unitPrice * quantity` (exact).
- The sale is **exempt iff `gross < 10000.00`** (strictly less; a sale with gross exactly 10000.00 is taxable).
- Realized **loss** (realized < 0): `pool += |realized|` — for exempt and taxable sales alike.
- Taxable sale with realized **gain** `g`:
  1. `offset = min(pool, g)`; `pool -= offset`.
  2. `taxable = g - offset`.
  3. `tax = taxable * 0.15`, rounded once to scale 2, HALF_UP. Never negative.
- Exempt sale with gain: no tax; pool untouched (exempt sales never consume losses).
- BUY: tax 0.00; pool untouched.

## 3. Precision policy

- `BigDecimal` everywhere; `float`/`double` forbidden in any calculation.
- Input `unitPrice`: at most 6 decimal places (validated).
- Weighted average: division at scale 6, HALF_UP, recomputed on every BUY.
- `realizedProfitLoss`: rounded once, scale 2, HALF_UP, at realization.
- `tax`: rounded once, scale 2, HALF_UP, after offsetting.
- No premature rounding: intermediate products use full precision; each reported value is rounded exactly once, where documented above.
- `averageCost` is serialized from the stored scale-6 value with one HALF_UP rounding to 2 decimals.
- Output monetary values: exactly 2 decimals.

## 4. Invariants (test targets)

1. `tax >= 0` always.
2. `pool >= 0` always.
3. `remainingQuantity >= 0` always (no short positions).
4. `averageCost >= 0`; `= 0` iff the position is flat.
5. Simulations are independent: reordering input lines cannot change any line's output.
6. Determinism: identical input bytes ⇒ identical output bytes.
7. A SELL preserves the average of the remaining units.
8. The loss pool only decreases via taxable profitable sales.

## 5. Worked examples (must be covered by tests)

- Repeating-decimal average: BUY 2 @ 20.00, BUY 1 @ 60.00 → avg = 100/3 = 33.333333 (scale 6). SELL 1 @ 34.00 → realized (34 − 33.333333) = 0.666667 → **0.67** reported.
- Threshold edge: a SELL with gross exactly 10000.00 and a gain is taxable; gross 9999.99 is exempt.
- Loss carryforward: a SELL at a loss builds the pool; a later taxable gain offsets it; an exempt profitable sale does not consume the pool.
