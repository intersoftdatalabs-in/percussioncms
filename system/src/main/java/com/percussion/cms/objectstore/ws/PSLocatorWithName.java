package com.percussion.cms.objectstore.ws;

public class PSLocatorWithName {
  public static final String ATTR_OVERRIDE_NAME = "overrideName";

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

  /**
   * Compatibility helper expected by older callers.
   *
   * @return the override name supplied when constructing this locator
   */
  public String getOverrideName() {
    return name;
  }
}
