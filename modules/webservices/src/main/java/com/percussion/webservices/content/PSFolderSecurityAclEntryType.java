package com.percussion.webservices.content;

public enum PSFolderSecurityAclEntryType {
  role,
  user,
  group;

  public static PSFolderSecurityAclEntryType fromString(String s) {
    if (s == null) return null;
    return PSFolderSecurityAclEntryType.valueOf(s);
  }
}
