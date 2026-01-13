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
}
