package com.percussion.webservices.content;

public enum PSFieldDescriptionDataType {
  text,
  date,
  number,
  binary;

  public static PSFieldDescriptionDataType fromString(String s) {
    if (s == null) return null;
    return PSFieldDescriptionDataType.valueOf(s);
  }

  /** Returns the enum value as a String. */
  public String getValue() {
    return name();
  }
}
