package com.lmt.fyp.flowerplus.common;

public enum NotificationType {
    ORDER_UPDATE,      // Order status changed for both Custom and premade order
    PROMOTION,         // Voucher/sale notification
    SYSTEM,            // Account, password, etc.
    REVIEW_REQUEST,    // Ask for review
    DELIVERY_ALERT   // Driver is nearby
}
