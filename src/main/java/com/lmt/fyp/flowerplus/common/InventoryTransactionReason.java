package com.lmt.fyp.flowerplus.common;

public enum InventoryTransactionReason {
    ORDER_FULFILLMENT,  // Used for an custom order
    STOCK_IN,           // New inventory received
    ADJUSTMENT,         // Manual correction
    WASTE,              // Expired/damaged flowers
    DAMAGE,             // Broken during handling
    RETURN,             // Customer returned items
    SAMPLE_USAGE        // For displays/testing or used on PreMade product
}
