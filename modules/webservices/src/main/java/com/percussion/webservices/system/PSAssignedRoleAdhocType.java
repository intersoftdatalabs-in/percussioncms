package com.percussion.webservices.system;

public enum PSAssignedRoleAdhocType {
  adhoc,
  fixed;

  public static PSAssignedRoleAdhocType fromString(String s) {
    if (s == null) return null;
    return PSAssignedRoleAdhocType.valueOf(s);
  }
}
