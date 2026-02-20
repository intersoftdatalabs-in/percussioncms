package com.percussion.webservices.content;

public enum PSFieldDataType {
  text,
  date,
  number,
  binary;

  public static PSFieldDataType fromString(String s) {
    if (s == null) return null;
    return PSFieldDataType.valueOf(s);
  }

  /** Returns the enum value as a String. */
  public String getValue() {
    return name();
  }
}
