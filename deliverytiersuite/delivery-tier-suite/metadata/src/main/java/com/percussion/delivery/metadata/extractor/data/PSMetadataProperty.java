/*
 * Copyright 1999-2025 Percussion Software, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.percussion.delivery.metadata.extractor.data;

import com.percussion.delivery.metadata.IPSMetadataProperty;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;
import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.Objects;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.commons.lang3.math.NumberUtils;

/**
 * Represents a metadata property name value pair.
 *
 * @author miltonpividori
 */
@XmlRootElement(name = "property")
@XmlAccessorType(XmlAccessType.PUBLIC_MEMBER)
public class PSMetadataProperty implements Serializable, IPSMetadataProperty {

  private static final long serialVersionUID = 1L;

  /** ISO-8601 formatter used when parsing date-typed properties from their string form. */
  private static final DateTimeFormatter DATE_TIME =
      DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

  /** Property name. For example: {@code dcterms:creator}. */
  private String name;

  /** Declared value type of this property. */
  @XmlTransient private VALUETYPE valuetype = VALUETYPE.STRING;

  /**
   * Value of the metadata property. It may be a {@link String}, {@link LocalDateTime} or {@link
   * Double}. The runtime value type can be inspected via the {@link #getValuetype() valuetype}
   * field.
   */
  private Serializable value;

  /** Default JAXB / no-arg constructor. */
  /** Default JAXB / no-arg constructor. */
  public PSMetadataProperty() {
    // Default constructor
  }

  /**
   * Ctor to create a property of the specified valuetype.
   *
   * @param name the property name, cannot be <code>null</code> or empty.
   * @param valuetype the {@link #valuetype} for the property. Cannot be <code>null</code>.
   * @param value the value to be stored in the property. May be <code>null</code> or empty.
   */
  public PSMetadataProperty(String name, VALUETYPE valuetype, Object value) {
    this.name = name;
    this.valuetype = valuetype;
    this.value = toSerializable(value);
  }

  /**
   * Convenience ctor to create a string value type property.
   *
   * @param name cannot be <code>null</code> or empty.
   * @param value the value, may be <code>null</code>.
   */
  public PSMetadataProperty(String name, String value) {
    this(name, VALUETYPE.STRING, value);
  }

  /**
   * Convenience ctor to create a number value type property from an int value.
   *
   * @param name the property name; cannot be <code>null</code> or empty.
   * @param value the int value to wrap.
   */
  public PSMetadataProperty(String name, int value) {
    this(name, VALUETYPE.NUMBER, value);
  }

  /**
   * Convenience ctor to create a number value type property from a double value.
   *
   * @param name the property name; cannot be <code>null</code> or empty.
   * @param value the double value to wrap.
   */
  public PSMetadataProperty(String name, double value) {
    this(name, VALUETYPE.NUMBER, value);
  }

  /**
   * Convenience ctor to create a number value type property from a float value.
   *
   * @param name the property name; cannot be <code>null</code> or empty.
   * @param value the float value to wrap.
   */
  public PSMetadataProperty(String name, float value) {
    this(name, VALUETYPE.NUMBER, value);
  }

  /**
   * Convenience ctor to create a number value type property from a long value.
   *
   * @param name the property name; cannot be <code>null</code> or empty.
   * @param value the long value to wrap.
   */
  public PSMetadataProperty(String name, long value) {
    this(name, VALUETYPE.NUMBER, value);
  }

  /**
   * Convenience ctor to create a number value type property from a short value.
   *
   * @param name the property name; cannot be <code>null</code> or empty.
   * @param value the short value to wrap.
   */
  public PSMetadataProperty(String name, short value) {
    this(name, VALUETYPE.NUMBER, value);
  }

  /**
   * Convenience ctor to create a Date value type property from a {@link LocalDateTime}.
   *
   * @param name name cannot be <code>null</code> or empty.
   * @param value may be <code>null</code>.
   */
  public PSMetadataProperty(String name, LocalDateTime value) {
    this(name, VALUETYPE.DATE, value);
  }

  /**
   * Convenience ctor to create a Date value type property from a legacy {@link Date}.
   *
   * @param name name cannot be <code>null</code> or empty.
   * @param value may be <code>null</code>.
   */
  public PSMetadataProperty(String name, Date value) {
    this(name, VALUETYPE.DATE, toLocalDateTime(value));
  }

  /* (non-Javadoc)
   * @see com.percussion.delivery.metadata.extractor.data.IPSMetadataProperty#getName()
   */
  @XmlElement
  public String getName() {
    return name;
  }

  /* (non-Javadoc)
   * @see com.percussion.delivery.metadata.extractor.data.IPSMetadataProperty#setName(java.lang.String)
   */
  public void setName(String name) {
    this.name = name;
  }

  /* (non-Javadoc)
   * @see com.percussion.delivery.metadata.extractor.data.IPSMetadataProperty#getValuetype()
   */
  @XmlTransient
  public VALUETYPE getValuetype() {
    return valuetype;
  }

  /**
   * Sets the {@link VALUETYPE} of this property. Coerces the underlying value to the supplied type
   * when possible.
   *
   * @param type the new value type; may not be {@code null}.
   */
  @XmlTransient
  public void setValuetype(VALUETYPE type) {
    valuetype = type;
    if (value != null) {
      value = toSerializable(convertVal(value, type));
    }
  }

  /* (non-Javadoc)
   * @see com.percussion.delivery.metadata.extractor.data.IPSMetadataProperty#getValue()
   */

  @XmlTransient
  public Object getValue() {
    return value;
  }

  /**
   * Returns the string representation of the underlying property value.
   *
   * @return the {@code toString} of the value, never {@code null} after the property has been
   *     initialised.
   */
  @XmlElement(name = "value")
  public String getStringValue() {
    return value.toString();
  }

  /**
   * Replaces the underlying value with the supplied string and marks the property as {@link
   * VALUETYPE#STRING}.
   *
   * @param value the string value to set; may be {@code null}.
   */
  @XmlElement(name = "value")
  public void setStringValue(String value) {
    this.value = value;
    valuetype = VALUETYPE.STRING;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#hashCode()
   */
  /* (non-Javadoc)
   * @see com.percussion.delivery.metadata.extractor.data.IPSMetadataProperty#hashCode()
   */
  @Override
  public int hashCode() {
    return Objects.hash(name, valuetype, value);
  }

  /* (non-Javadoc)
   * @see java.lang.Object#equals(java.lang.Object)
   */
  /* (non-Javadoc)
   * @see com.percussion.delivery.metadata.extractor.data.IPSMetadataProperty#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof PSMetadataProperty)) {
      return false;
    }
    PSMetadataProperty other = (PSMetadataProperty) obj;
    return Objects.equals(name, other.name)
        && Objects.equals(valuetype, other.valuetype)
        && Objects.equals(value, other.value);
  }

  /* (non-Javadoc)
   * @see com.percussion.delivery.metadata.extractor.data.IPSMetadataProperty#toString()
   */
  @Override
  public String toString() {
    return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
        .append("name", name)
        .append("value", value)
        .toString();
  }

  /**
   * Returns the property as a {@link LocalDateTime} when the declared value type is {@code DATE}.
   *
   * @return the date value.
   * @throws RuntimeException if the value type is not {@code DATE}.
   */
  public LocalDateTime getDatevalue() {
    if (valuetype != VALUETYPE.DATE)
      throw new RuntimeException("Cannot return a date for property type " + valuetype.toString());
    return (LocalDateTime) value;
  }

  /**
   * Returns the property as a {@link Double} when the declared value type is {@code NUMBER}.
   *
   * @return the numeric value.
   * @throws RuntimeException if the value type is not {@code NUMBER}.
   */
  public Double getNumbervalue() {
    if (valuetype != VALUETYPE.NUMBER)
      throw new RuntimeException(
          "Cannot return a number for property type " + valuetype.toString());
    return (Double) value;
  }

  /**
   * Returns the property as a {@link String}.
   *
   * @return the string value, never {@code null}.
   */
  public String getStringvalue() {
    return StringUtils.defaultString(value.toString());
  }

  /**
   * Sets the date value of this property.
   *
   * @param val the date value to set; may be {@code null}.
   */
  @XmlTransient
  public void setDatevalue(LocalDateTime val) {
    valuetype = VALUETYPE.DATE;
    value = val;
  }

  /**
   * Sets the numeric value of this property.
   *
   * @param val the numeric value to set; may be {@code null}.
   */
  @XmlTransient
  public void setNumbervalue(Double val) {
    valuetype = VALUETYPE.NUMBER;
    value = val;
  }

  /**
   * Sets the string value of this property.
   *
   * @param val the string value to set; may be {@code null}.
   */
  @XmlTransient
  public void setStringvalue(String val) {
    valuetype = VALUETYPE.STRING;
    value = val;
  }

  /**
   * Sets the free-text (long string) value of this property.
   *
   * @param val the text value to set; may be {@code null}.
   */
  @XmlTransient
  public void setTextvalue(String val) {
    valuetype = VALUETYPE.TEXT;

    value = val;
  }

  /**
   * Sets the untyped value of this property, coercing to the declared value type when one is set.
   *
   * @param val the value to set; may be {@code null}.
   */
  @XmlTransient
  public void setValue(Object val) {
    if (valuetype == null) {
      value = toSerializable(val);
    } else {
      value = toSerializable(convertVal(val, valuetype));
    }
  }

  private Serializable toSerializable(Object val) {
    if (val == null) {
      return null;
    }
    if (val instanceof Serializable) {
      return (Serializable) val;
    }
    throw new IllegalArgumentException("value must implement Serializable");
  }

  private Object convertVal(Object val, VALUETYPE type) {
    if (val instanceof String) {
      if (type == VALUETYPE.STRING || type == VALUETYPE.TEXT) {
        valuetype = type;
        return val;
      } else if (type == VALUETYPE.NUMBER) {
        if (NumberUtils.isCreatable((String) val)) {
          Double doub = Double.parseDouble((String) val);
          valuetype = VALUETYPE.NUMBER;
          return doub;
        }
        throw new IllegalArgumentException("value does not match number type");
      } else if (type == VALUETYPE.DATE) {
        try {
          LocalDateTime date = LocalDateTime.parse((String) val, DATE_TIME);
          valuetype = VALUETYPE.DATE;
          return date;
        } catch (DateTimeParseException e) {
          throw new IllegalArgumentException("value does not match date type");
        }
      }
      valuetype = VALUETYPE.STRING;
      return val;
    } else if (val instanceof Double && type != VALUETYPE.NUMBER) {
      throw new IllegalArgumentException("value type does not match Double");
    } else if (val instanceof LocalDateTime && type != VALUETYPE.DATE) {
      throw new IllegalArgumentException("value type does not match LocalDateTime");
    } else if (val instanceof Date) {
      if (type != VALUETYPE.DATE) {
        throw new IllegalArgumentException("value type does not match Date");
      }
      valuetype = VALUETYPE.DATE;
      return toLocalDateTime(val);
    } else if (type == VALUETYPE.DATE) {
      valuetype = VALUETYPE.DATE;
      return toLocalDateTime(val);
    }
    valuetype = type;
    return val;
  }

  private static LocalDateTime toLocalDateTime(Object value) {
    if (value == null) {
      return null;
    }
    if (value instanceof LocalDateTime) {
      return (LocalDateTime) value;
    }
    if (value instanceof Instant) {
      return LocalDateTime.ofInstant((Instant) value, ZoneId.systemDefault());
    }
    // java.sql.Date#toInstant() throws UnsupportedOperationException — use millis instead.
    if (value instanceof Date) {
      return LocalDateTime.ofInstant(
          Instant.ofEpochMilli(((Date) value).getTime()), ZoneId.systemDefault());
    }
    throw new IllegalArgumentException("value does not match date type");
  }
}
