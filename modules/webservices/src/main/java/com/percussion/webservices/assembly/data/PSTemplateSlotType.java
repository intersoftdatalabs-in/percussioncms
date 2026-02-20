package com.percussion.webservices.assembly.data;

public enum PSTemplateSlotType {
  regular,
  inline;

  public static PSTemplateSlotType fromString(String s) {
    if (s == null) return null;
    return PSTemplateSlotType.valueOf(s);
  }
}
