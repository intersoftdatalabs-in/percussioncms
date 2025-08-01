// REFACTORED: CP-JAVA11
package com.percussion.delivery.comments.data;

/**
 * Enum representing the possible approval states for comments.
 */
public enum APPROVAL_STATE {
    PENDING,    // Comment awaiting moderation
    APPROVED,   // Comment has been approved
    REJECTED,   // Comment has been rejected
    SPAM;       // Comment has been marked as spam

    /**
     * Parse a string into an APPROVAL_STATE, case-insensitive.
     * @param state the string to parse
     * @return the matching APPROVAL_STATE
     * @throws IllegalArgumentException if the string doesn't match any state
     */
    public static APPROVAL_STATE fromString(String state) {
        return valueOf(state.toUpperCase());
    }
}
