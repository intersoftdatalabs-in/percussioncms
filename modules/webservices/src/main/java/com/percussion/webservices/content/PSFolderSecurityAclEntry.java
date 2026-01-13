package com.percussion.webservices.content;

public class PSFolderSecurityAclEntry {
  private String name;
  private PSFolderSecurityAclEntryType type;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public PSFolderSecurityAclEntryType getType() {
    return type;
  }

  public void setType(PSFolderSecurityAclEntryType type) {
    this.type = type;
  }
}
