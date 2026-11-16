# CONTRACT.md — Public CLI Contract (v1)

Deterministic, line-delimited JSON processor. Same input ⇒ same output, byte-for-byte.

## Input (stdin)

- UTF-8 text. Each line holds one **simulation**: a JSON array of operations.
- Lines that are empty or whitespace-only are ignored.
- A simulation may be an empty array (`[]`) → outputs `[]`.
- Operations are processed strictly in array order; simulations strictly in line order.
- Operation object — exactly these fields, all required, no unknown fields:

| field       | type    | rules                                              |
|-------------|---------|----------------------------------------------------|
| `asset`     | string  | matches `^[A-Z0-9]{1,20}$`                          |
| `type`      | string  | `"BUY"` or `"SELL"` (exact, case-sensitive)         |
| `unitPrice` | number  | `> 0`, `<= 1000000000`, at most 6 decimal places as written (`10.0000000` is rejected even though numerically exact) |
| `quantity`  | integer | integral value, `>= 1`, `<= 1000000000`; may be written with trailing decimal zeros (`100.00` is accepted, `1.5` is not); numeric strings are rejected |

Example input (2 simulations):

```json
[{"asset":"PETR","type":"BUY","unitPrice":10.00,"quantity":100},{"asset":"PETR","type":"SELL","unitPrice":12.50,"quantity":50}]
[{"asset":"VALE","type":"BUY","unitPrice":30.00,"quantity":10}]
```

## Output (stdout)

One line per accepted input line, same order. Each line is a JSON array with one result object per operation:

```json
[{"operation":1,"asset":"PETR","type":"BUY","remainingQuantity":100,"averageCost":10.00,"realizedProfitLoss":0.00,"tax":0.00,"compensableLoss":0.00},{"operation":2,"asset":"PETR","type":"SELL","remainingQuantity":50,"averageCost":10.00,"realizedProfitLoss":125.00,"tax":0.00,"compensableLoss":0.00}]
[{"operation":1,"asset":"VALE","type":"BUY","remainingQuantity":10,"averageCost":30.00,"realizedProfitLoss":0.00,"tax":0.00,"compensableLoss":0.00}]
```

| field                 | meaning                                                        |
|-----------------------|----------------------------------------------------------------|
| `operation`           | 1-based index of the operation inside its simulation           |
| `asset` / `type`      | echo of the operation                                          |
| `remainingQuantity`   | position of that asset after the operation                     |
| `averageCost`         | weighted average cost after the operation (`0.00` if flat)     |
| `realizedProfitLoss`  | realized P/L of a SELL (`0.00` for BUY), positive = gain       |
| `tax`                 | simulated tax charged by the operation (see DOMAIN_RULES)      |
| `compensableLoss`     | accumulated loss pool of the simulation after the operation    |

Monetary fields always carry exactly 2 decimals. Numbers are JSON numbers (never strings), plain notation.

## Errors — fail-fast, atomic

On the first problem, the process stops with **no stdout output** (results are buffered and only flushed on full success) and a single `ERROR: <reason>` line on stderr.

Rejected inputs include: malformed JSON (including duplicate object keys); missing or unknown fields; wrong types; `unitPrice`/`quantity` out of range or wrong scale; SELL exceeding the current position of that asset.

## Exit codes

| code | meaning                                             |
|------|-----------------------------------------------------|
| 0    | all simulations processed and output written        |
| 1    | input/validation failure (see above)                |
| 2    | unexpected internal error (I/O failure or bug)      |

## Stability

- Field order in output objects is fixed (as above).
- Unknown input fields are a contract violation (no silent ignore).
- The contract is versioned; breaking changes require a major version bump of the JAR/docs.
