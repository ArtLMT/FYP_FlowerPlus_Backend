package com.lmt.fyp.flowerplus.common;

public enum OrderStatus {
    PENDING,
    PAID,
    APPROVED,
    REJECTED, // Custom order isn't available or undoable
    CANCELLED,  // By user
    PROCESSING,
    READY_FOR_SHIPPING,
    COMPLETED

}
