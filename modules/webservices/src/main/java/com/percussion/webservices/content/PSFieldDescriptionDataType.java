package com.percussion.webservices.content;

public enum PSFieldDescriptionDataType {
  text,
  date,
  number,
  binary;

  public static PSFieldDescriptionDataType fromString(String s) {
    if (s == null) return null;
    return PSFieldDescriptionDataType.valueOf(s);
  }
}
