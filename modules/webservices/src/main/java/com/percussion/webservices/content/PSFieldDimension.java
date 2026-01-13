package com.percussion.webservices.content;

public enum PSFieldDimension {
  optional,
  required,
  oneormore,
  zeroormore,
  count;

  public static PSFieldDimension fromString(String s) {
    if (s == null) return null;
    return PSFieldDimension.valueOf(s);
  }
}
