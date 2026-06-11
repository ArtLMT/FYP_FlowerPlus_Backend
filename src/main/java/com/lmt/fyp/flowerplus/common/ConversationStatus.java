package com.lmt.fyp.flowerplus.common;

public enum ConversationStatus {
    ACTIVE,                // Ongoing
    WAITING_FOR_STAFF,     // Queued for staff
    WAITING_FOR_CUSTOMER,  // Waiting for customer reply
    RESOLVED,              // Solved by staff/AI
    CLOSED,                // Manually closed
    ARCHIVED               // Auto-archived after inactivity
}
