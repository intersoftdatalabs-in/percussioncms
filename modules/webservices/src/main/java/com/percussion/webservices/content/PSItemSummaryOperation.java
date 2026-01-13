package com.percussion.webservices.content;

public enum PSItemSummaryOperation {
  read,
  write,
  transition,
  checkin,
  checkout,
  none;

  public static PSItemSummaryOperation fromString(String s) {
    if (s == null) return null;
    return PSItemSummaryOperation.valueOf(s);
  }
}
