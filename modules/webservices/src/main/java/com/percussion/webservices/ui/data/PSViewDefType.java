package com.percussion.webservices.ui.data;

public enum PSViewDefType {
  list,
  grid,
  tree;

  public static PSViewDefType fromString(String s) {
    if (s == null) return null;
    return PSViewDefType.valueOf(s);
  }
}
