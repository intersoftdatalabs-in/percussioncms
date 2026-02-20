package com.percussion.webservices.common;

public class RelationshipPropertiesProperty {
  private String value;
  private String name;
  private Boolean persisted;

  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    this.value = value;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Boolean getPersisted() {
    return persisted;
  }

  public void setPersisted(Boolean persisted) {
    this.persisted = persisted;
  }
}
