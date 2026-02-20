package com.percussion.webservices.system;

public enum PSAssignedRoleAssignmentType {
  manual,
  automatic;

  public static PSAssignedRoleAssignmentType fromString(String s) {
    if (s == null) return null;
    return PSAssignedRoleAssignmentType.valueOf(s);
  }
}
