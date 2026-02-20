package com.percussion.webservices.system;

/**
 * Minimal enum shim for notification state/role recipient types used by webservices.
 */
public enum PSNotificationStateRoleRecipientType
{
    STATE,
    ROLE,
    RECIPIENT,
    UNKNOWN;

    public static PSNotificationStateRoleRecipientType fromString(String s)
    {
        if (s == null) return UNKNOWN;
        try {
            return PSNotificationStateRoleRecipientType.valueOf(s.toUpperCase());
        } catch (IllegalArgumentException e) {
            return UNKNOWN;
        }
    }
}
