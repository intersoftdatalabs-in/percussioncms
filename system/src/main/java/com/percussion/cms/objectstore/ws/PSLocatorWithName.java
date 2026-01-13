package com.percussion.cms.objectstore.ws;

public class PSLocatorWithName {
  private final int id;
  private int revision;
  private final String name;

  public PSLocatorWithName(int id, int revision, String name) {
    this.id = id;
    this.revision = revision;
    this.name = name;
  }

  public int getId() {
    return id;
  }

  public int getRevision() {
    return revision;
  }

  public String getName() {
    return name;
  }
}
