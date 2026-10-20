package com.swapmarket.service;

import static org.junit.jupiter.api.Assertions.*;

import com.swapmarket.dto.ValueComparison;
import com.swapmarket.entity.Item;
import com.swapmarket.enums.ValueVerdict;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class ValueCalculatorTest {

    private final ValueCalculator calculator = new ValueCalculator();

    private static Item item(String originalPrice, String condition) {
        Item item = new Item();
        item.setOriginalPrice(originalPrice == null ? null : new BigDecimal(originalPrice));
        item.setCondition(condition);
        return item;
    }

    /** Compares numerically so scale differences (1000 vs 1000.00) never matter. */
    private static void assertValue(String expected, BigDecimal actual) {
        assertNotNull(actual, "expected " + expected + " but value was null");
        assertEquals(0, new BigDecimal(expected).compareTo(actual), "expected " + expected + " but was " + actual);
    }

    // ---------- estimate ----------

    @Test
    void estimate_appliesTheConditionFactor() {
        assertValue("1600", calculator.estimate(new BigDecimal("2000"), "New with tags"));
        assertValue("1200", calculator.estimate(new BigDecimal("2000"), "Like new"));
        assertValue("800", calculator.estimate(new BigDecimal("2000"), "Good"));
        assertValue("400", calculator.estimate(new BigDecimal("2000"), "Fair"));
    }

    @Test
    void estimate_ignoresCaseAndSurroundingSpaces() {
        assertValue("1200", calculator.estimate(new BigDecimal("2000"), "  LIKE NEW "));
    }

    @Test
    void estimate_unknownOrMissingCondition_fallsBackToGood() {
        assertValue("800", calculator.estimate(new BigDecimal("2000"), "Vintage"));
        assertValue("800", calculator.estimate(new BigDecimal("2000"), null));
    }

    @Test
    void estimate_withoutUsablePrice_isNull() {
        assertNull(calculator.estimate(null, "Good"));
        assertNull(calculator.estimate(BigDecimal.ZERO, "Good"));
        assertNull(calculator.estimate(new BigDecimal("-50"), "Good"));
    }

    @Test
    void estimate_roundsToWholeRupees() {
        // 1003 x 0.20 = 200.60 -> 201
        assertValue("201", calculator.estimate(new BigDecimal("1003"), "Fair"));
    }

    @Test
    void estimate_ofItem_usesItsPriceAndCondition() {
        assertValue("1000", calculator.estimate(item("2500", "Good")));
        assertNull(calculator.estimate((Item) null));
    }

    // ---------- compare ----------

    @Test
    void compare_withNoOfferedItem_isDepositOnly() {
        ValueComparison result = calculator.compare(item("2500", "Good"), null);

        assertEquals(ValueVerdict.DEPOSIT_ONLY, result.verdict());
        assertValue("1000", result.requestedValue());
        assertNull(result.offeredValue());
        assertNull(result.difference());
        assertNull(result.suggestedTopUp());
    }

    @Test
    void compare_whenEitherItemHasNoPrice_isUnknown() {
        ValueComparison noOfferedPrice = calculator.compare(item("2500", "Good"), item(null, "Good"));
        assertEquals(ValueVerdict.UNKNOWN, noOfferedPrice.verdict());
        assertValue("1000", noOfferedPrice.requestedValue());
        assertNull(noOfferedPrice.offeredValue());

        ValueComparison noRequestedPrice = calculator.compare(item(null, "Good"), item("2500", "Good"));
        assertEquals(ValueVerdict.UNKNOWN, noRequestedPrice.verdict());
        assertNull(noRequestedPrice.suggestedTopUp());
    }

    @Test
    void compare_similarValues_isFair() {
        ValueComparison result = calculator.compare(item("2500", "Good"), item("2250", "Good")); // 1000 vs 900

        assertEquals(ValueVerdict.FAIR, result.verdict());
        assertValue("-100", result.difference());
        assertNull(result.suggestedTopUp());
    }

    @Test
    void compare_fairBandBoundariesAreInclusive() {
        // exactly 85% of the requested value
        assertEquals(ValueVerdict.FAIR,
                calculator.compare(item("2500", "Good"), item("2125", "Good")).verdict()); // 1000 vs 850
        // exactly 115% of the requested value
        assertEquals(ValueVerdict.FAIR,
                calculator.compare(item("2500", "Good"), item("2875", "Good")).verdict()); // 1000 vs 1150
    }

    @Test
    void compare_clearlyLowerOffer_suggestsTopUp() {
        ValueComparison result = calculator.compare(item("2500", "Good"), item("1250", "Good")); // 1000 vs 500

        assertEquals(ValueVerdict.OFFER_LOWER, result.verdict());
        assertValue("-500", result.difference());
        assertValue("500", result.suggestedTopUp());
    }

    @Test
    void compare_justBelowTheBand_isLower() {
        // 849 / 1000 = 0.849 -> below 85%
        ValueComparison result = calculator.compare(item("2500", "Good"), item("2122.5", "Good"));

        assertEquals(ValueVerdict.OFFER_LOWER, result.verdict());
    }

    @Test
    void compare_clearlyHigherOffer_hasNoTopUp() {
        ValueComparison result = calculator.compare(item("2500", "Good"), item("3750", "Good")); // 1000 vs 1500

        assertEquals(ValueVerdict.OFFER_HIGHER, result.verdict());
        assertValue("500", result.difference());
        assertNull(result.suggestedTopUp());
    }

    @Test
    void compare_takesConditionIntoAccount() {
        // Same 3000 retail price: a "Like new" piece (1800) versus a "Fair" one (600).
        ValueComparison result = calculator.compare(item("3000", "Like new"), item("3000", "Fair"));

        assertEquals(ValueVerdict.OFFER_LOWER, result.verdict());
        assertValue("1200", result.suggestedTopUp());
    }
}
