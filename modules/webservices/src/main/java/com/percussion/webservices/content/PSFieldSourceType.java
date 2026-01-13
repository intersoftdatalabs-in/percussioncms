package com.percussion.webservices.content;

public enum PSFieldSourceType {
  local,
  shared,
  system,
  unknown;

  public static PSFieldSourceType fromString(String s) {
    if (s == null) return null;
    return PSFieldSourceType.valueOf(s);
  }
}
