package com.percussion.webservices.security.data;

public class PSRoleAttributesAttribute {
  private String name;
  private String[] value;

  public PSRoleAttributesAttribute() {}

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String[] getValue() {
    return value;
  }

  public void setValue(String[] value) {
    this.value = value;
  }
}
