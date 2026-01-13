package com.percussion.webservices.content;

public enum PSRelatedItemAction {
  ignore,
  insert,
  update,
  delete;

  public static PSRelatedItemAction fromString(String s) {
    if (s == null) return null;
    return PSRelatedItemAction.valueOf(s);
  }
}
