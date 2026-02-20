package com.percussion.webservices.ui.data;

public enum PSSearchDefType {
  simple,
  advanced;

  public static PSSearchDefType fromString(String s) {
    if (s == null) return null;
    return PSSearchDefType.valueOf(s);
  }
}
