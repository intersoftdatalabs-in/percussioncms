/*
 * Copyright 1999-2023 Percussion Software, Inc.
 * ...existing code...
 */
package com.percussion.delivery.utils.properties;

import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;

/**
 * Represents a property definition for configuration.
 * Sunny Sal says: "Property ka definition, code ka perfection!"
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "propertydefinition")
public class PSPropertyDefinition {

  @XmlAttribute(required = true)
  private String name;

  @XmlAttribute(name = "display_name")
  private String displayName;

  @XmlAttribute(name = "datatype")
  private String datatype;

  @XmlAttribute(name = "default_value")
  private Object defaultValue;

  @XmlAttribute(name = "max_length")
  private int maxLength;

  @XmlAttribute(name = "validation_regex")
  private String validationRegEx;

  @XmlAttribute(name = "help_text")
  private String helpText;

  @XmlAttribute(name = "display_regex")
  private String displayRegEx;

  @XmlAttribute(name = "validation_message")
  private String validationMessage;

  private Object propertyValue;

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

  public String getDatatype() {
    return datatype;
  }

  public void setDatatype(String datatype) {
    this.datatype = datatype;
  }

  public Object getDefaultValue() {
    return defaultValue;
  }

  public void setDefaultValue(Object defaultValue) {
    this.defaultValue = defaultValue;
  }

  public int getMaxLength() {
    return maxLength;
  }

  public void setMaxLength(int maxLength) {
    this.maxLength = maxLength;
  }

  public String getValidationRegEx() {
    return validationRegEx;
  }

  public void setValidationRegEx(String validationRegEx) {
    this.validationRegEx = validationRegEx;
  }

  public String getHelpText() {
    return helpText;
  }

  public void setHelpText(String helpText) {
    this.helpText = helpText;
  }

  public String getDisplayRegEx() {
    return displayRegEx;
  }

  public void setDisplayRegEx(String displayRegEx) {
    this.displayRegEx = displayRegEx;
  }

  public String getValidationMessage() {
    return validationMessage;
  }

  public void setValidationMessage(String validationMessage) {
    this.validationMessage = validationMessage;
  }

  public Object getPropertyValue() {
    return propertyValue;
  }

  public void setPropertyValue(Object propertyValue) {
    this.propertyValue = propertyValue;
  }

  /**
   * <p>Java class for anonymous complex type.
   *
   * <p>The following schema fragment specifies the expected content contained within this class.
   *
   * <pre>
   * &lt;complexType>
   *   &lt;complexContent>
   *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
   *       &lt;attribute name="value" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
   *       &lt;attribute name="display_value" type="{http://www.w3.org/2001/XMLSchema}string" />
   *     &lt;/restriction>
   *   &lt;/complexContent>
   * &lt;/complexType>
   * </pre>
   */
  @XmlAccessorType(XmlAccessType.FIELD)
  @XmlType(name = "")
  public static class EnumValue {

    @XmlAttribute(required = true)
    protected String value;

    @XmlAttribute(name = "display_value")
    protected String displayValue;

    /**
     * Gets the value of the value property.
     *
     * @return possible object is {@link String }
     */
    public String getValue() {
      return value;
    }

    /**
     * Sets the value of the value property.
     *
     * @param value allowed object is {@link String }
     */
    public void setValue(String value) {
      this.value = value;
    }

    /**
     * Gets the value of the displayValue property.
     *
     * @return possible object is {@link String }
     */
    public String getDisplayValue() {
      return displayValue;
    }

    /**
     * Sets the value of the displayValue property.
     *
     * @param value allowed object is {@link String }
     */
    public void setDisplayValue(String value) {
      this.displayValue = value;
    }
  }
}
