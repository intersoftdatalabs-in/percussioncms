package com.percussion.webservices.ui.data;

public enum SearchFieldType {
  text,
  number,
  date;

  public static SearchFieldType fromString(String s) {
    if (s == null) return null;
    return SearchFieldType.valueOf(s);
  }
}
