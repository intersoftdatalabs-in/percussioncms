package com.percussion.webservices.ui.data;

/** Compatibility DTO for PSHierarchyNode.Properties.Property expected by converters. */
public class PSHierarchyNodePropertiesProperty {
  private Long parentId;
  private String name;
  private String value;

  public PSHierarchyNodePropertiesProperty() {}

  public PSHierarchyNodePropertiesProperty(Long parentId, String name, String value) {
    this.parentId = parentId;
    this.name = name;
    this.value = value;
  }

  public Long getParentId() {
    return parentId;
  }

  public void setParentId(Long parentId) {
    this.parentId = parentId;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    this.value = value;
  }
}
