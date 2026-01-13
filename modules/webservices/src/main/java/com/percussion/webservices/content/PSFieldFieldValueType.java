package com.percussion.webservices.content;

public enum PSFieldFieldValueType {
  meta,
  content,
  unknown;

  public static PSFieldFieldValueType fromString(String s) {
    if (s == null) return null;
    return PSFieldFieldValueType.valueOf(s);
  }
}
