package com.percussion.webservices.content;

import java.util.ArrayList;
import java.util.List;

public class PSItemChildren {
  private String name;
  private String displayName;
  private Boolean sequenced;
  private List<PSChildEntry> psChildEntry = new ArrayList<>();

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDisplayName() {
    return displayName;
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  public Boolean getSequenced() {
    return sequenced;
  }

  public void setSequenced(Boolean sequenced) {
    this.sequenced = sequenced;
  }

  public List<PSChildEntry> getPSChildEntry() {
    return psChildEntry;
  }

  public void setPSChildEntry(List<PSChildEntry> entries) {
    this.psChildEntry = entries;
  }
}
