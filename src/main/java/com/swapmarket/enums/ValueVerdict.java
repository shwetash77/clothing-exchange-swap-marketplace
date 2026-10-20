package com.swapmarket.enums;

/** Outcome of comparing the value of the requested item against the offered item. */
public enum ValueVerdict {
    FAIR,           // offered value is within the fair band of the requested value
    OFFER_LOWER,    // offered item is worth noticeably less than the requested item
    OFFER_HIGHER,   // offered item is worth noticeably more than the requested item
    DEPOSIT_ONLY,   // no item was offered, so there is nothing to compare
    UNKNOWN         // at least one of the items has no original price recorded
}
