package com.percussion.webservices.content;

/**
 * Compatibility DTO for PSAaRelationship.Folder (generated nested type). Minimal implementation
 * expected by converters (id, name, path).
 */
public class PSAaRelationshipFolder {
  private long id;
  private String name;
  private String path;

  public PSAaRelationshipFolder() {}

  public PSAaRelationshipFolder(long id, String name, String path) {
    this.id = id;
    this.name = name;
    this.path = path;
  }

  public long getId() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getPath() {
    return path;
  }

  public void setPath(String path) {
    this.path = path;
  }
}
