// REFACTORED: CP-JAVA11
package com.percussion.delivery.comments.data;

/**
 * Enum representing the available sort options for comments.
 */
public enum SORTBY {
  CREATED_DATE,
  USERNAME,
  TITLE,
  STATE;

  /**
   * Parse a string into a SORTBY enum, case-insensitive.
   * @param value the string to parse
   * @return the matching SORTBY enum
   * @throws IllegalArgumentException if the string doesn't match any sort option
   */
  public static SORTBY fromString(String value) {
    return valueOf(value.toUpperCase());
  }
}
