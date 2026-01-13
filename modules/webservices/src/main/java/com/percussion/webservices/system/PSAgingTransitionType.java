package com.percussion.webservices.system;

public enum PSAgingTransitionType {
  publish,
  archive,
  expire;

  public static PSAgingTransitionType fromString(String s) {
    if (s == null) return null;
    return PSAgingTransitionType.valueOf(s);
  }
}
