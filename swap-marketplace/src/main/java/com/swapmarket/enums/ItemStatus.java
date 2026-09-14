package com.swapmarket.enums;

public enum ItemStatus {
    AVAILABLE,
    LOCKED,     // a swap is pending on this item - prevents double-claiming
    SWAPPED
}
