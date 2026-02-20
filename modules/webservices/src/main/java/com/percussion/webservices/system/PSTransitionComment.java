package com.percussion.webservices.system;

public enum PSTransitionComment {
  optional,
  required,
  doNotShow;

  public static PSTransitionComment fromString(String s) {
    if (s == null) return null;
    return PSTransitionComment.valueOf(s);
  }
}
