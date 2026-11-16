package com.capitalgains.domain;

import java.math.BigDecimal;

/**
 * Accumulated realized losses of one simulation (DOMAIN_RULES §2). Only taxable
 * profitable sales may consume it; that behavior belongs to the tax engine.
 */
public final class LossPool {

    private BigDecimal balance = BigDecimal.ZERO.setScale(2);

    public BigDecimal balance() {
        return balance;
    }

    public void add(BigDecimal loss) {
        if (loss.signum() <= 0) {
            throw new DomainException("loss must be positive");
        }
        balance = balance.add(loss);
    }

    /**
     * Offsets the given positive gain against the pool and returns the consumed amount.
     */
    public BigDecimal consume(BigDecimal gain) {
        if (gain.signum() <= 0) {
            throw new DomainException("gain to offset must be positive");
        }
        BigDecimal offset = balance.min(gain);
        balance = balance.subtract(offset);
        return offset;
    }
}
