package com.percussion.webservices.system;

public enum RelationshipConfigSummaryType {
  system,
  user;

  public static RelationshipConfigSummaryType fromString(String s) {
    if (s == null) return null;
    return RelationshipConfigSummaryType.valueOf(s);
  }
}
