package com.percussion.webservices.content;

public class PSSearchParamsFolderFilter {
  private String value;
  private Boolean includeSubFolders = Boolean.TRUE;

  public String get_value() {
    return value;
  }

  public void set_value(String value) {
    this.value = value;
  }

  public Boolean isIncludeSubFolders() {
    return includeSubFolders;
  }

  public void setIncludeSubFolders(Boolean includeSubFolders) {
    this.includeSubFolders = includeSubFolders;
  }
}
