package com.percussion.webservices.content;

public enum PSContentEditorDefinitionType {
  local,
  shared,
  system;

  public static PSContentEditorDefinitionType fromString(String s) {
    if (s == null) return null;
    return PSContentEditorDefinitionType.valueOf(s);
  }
}
