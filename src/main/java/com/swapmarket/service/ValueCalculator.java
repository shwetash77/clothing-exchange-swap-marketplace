package com.swapmarket.service;

import com.swapmarket.dto.ValueComparison;
import com.swapmarket.entity.Item;
import com.swapmarket.enums.ValueVerdict;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;
import java.util.Map;

/**
 * The swap value calculator.
 *
 * Estimated swap value = original retail price x condition factor, rounded to
 * whole rupees. The factors are deliberately simple and live in one place so
 * they are easy to explain and tune:
 *
 *   New with tags 80% | Like new 60% | Good 40% | Fair 20%
 *
 * Two items are considered a fair swap when the offered value is within
 * +/-15% of the requested value. Outside that band the comparison reports
 * which side is lower and, when the offer is the lower one, the top-up that
 * would even things out.
 *
 * Items listed before original prices existed have no estimate; any comparison
 * involving them is reported as UNKNOWN rather than guessed.
 */
@Component
public class ValueCalculator {

    private static final Map<String, BigDecimal> CONDITION_FACTORS = Map.of(
            "new with tags", new BigDecimal("0.80"),
            "like new", new BigDecimal("0.60"),
            "good", new BigDecimal("0.40"),
            "fair", new BigDecimal("0.20"));

    /** Used when the condition is missing or not one of the known labels. */
    private static final BigDecimal DEFAULT_FACTOR = new BigDecimal("0.40");

    static final BigDecimal FAIR_LOWER_BOUND = new BigDecimal("0.85");
    static final BigDecimal FAIR_UPPER_BOUND = new BigDecimal("1.15");

    /** Returns the estimated value in INR, or null when there is no usable original price. */
    public BigDecimal estimate(BigDecimal originalPrice, String condition) {
        if (originalPrice == null || originalPrice.signum() <= 0) {
            return null;
        }
        BigDecimal factor = condition == null
                ? DEFAULT_FACTOR
                : CONDITION_FACTORS.getOrDefault(condition.trim().toLowerCase(Locale.ROOT), DEFAULT_FACTOR);
        BigDecimal value = originalPrice.multiply(factor).setScale(0, RoundingMode.HALF_UP);
        return value.signum() > 0 ? value : null;
    }

    public BigDecimal estimate(Item item) {
        return item == null ? null : estimate(item.getOriginalPrice(), item.getCondition());
    }

    /** Compares the requested item against the offered one (offered may be null for deposit-only). */
    public ValueComparison compare(Item requested, Item offered) {
        BigDecimal requestedValue = estimate(requested);

        if (offered == null) {
            return new ValueComparison(requestedValue, null, null, null, ValueVerdict.DEPOSIT_ONLY);
        }

        BigDecimal offeredValue = estimate(offered);
        if (requestedValue == null || offeredValue == null) {
            return new ValueComparison(requestedValue, offeredValue, null, null, ValueVerdict.UNKNOWN);
        }

        BigDecimal difference = offeredValue.subtract(requestedValue);
        BigDecimal ratio = offeredValue.divide(requestedValue, 4, RoundingMode.HALF_UP);

        if (ratio.compareTo(FAIR_LOWER_BOUND) < 0) {
            return new ValueComparison(requestedValue, offeredValue, difference,
                    requestedValue.subtract(offeredValue), ValueVerdict.OFFER_LOWER);
        }
        if (ratio.compareTo(FAIR_UPPER_BOUND) > 0) {
            return new ValueComparison(requestedValue, offeredValue, difference, null, ValueVerdict.OFFER_HIGHER);
        }
        return new ValueComparison(requestedValue, offeredValue, difference, null, ValueVerdict.FAIR);
    }
}
