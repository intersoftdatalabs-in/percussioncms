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
package com.percussion.pagemanagement.assembler;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.ManyToOne;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;

/** Represents a metadata property with a name, type, and value. */
public class PSMetadataProperty {

  private PropertyId id;
  private VALUETYPE valuetype;
  private String stringvalue;
  private String textvalue;
  private Date datevalue;
  private Double numbervalue;

  private PSMetadataProperty() {
    // For JPA/Hibernate
  }

  public PSMetadataProperty(String name, VALUETYPE type, Object value) {
    this();
    if (name == null || name.isEmpty()) {
      throw new IllegalArgumentException("name cannot be null or empty.");
    }
    if (type == null) {
      throw new IllegalArgumentException("type cannot be null.");
    }
    setName(name);
    boolean nan = true;
    if (type == VALUETYPE.DATE) {
      if (!(value instanceof Date)) {
        throw new IllegalArgumentException(
            "Value type 'Date' was specified but the passed in value is not a date object.");
      }
      setDatevalue((Date) value);
    } else if (type == VALUETYPE.NUMBER) {
      Double d = null;
      if (value instanceof Integer
          || value instanceof Float
          || value instanceof Long
          || value instanceof Short) {
        d = Double.valueOf(value.toString());
        nan = false;
      } else if (value instanceof Double) {
        d = (Double) value;
        nan = false;
      } else if (value instanceof String) {
        try {
          d = Double.parseDouble(value.toString());
          nan = false;
        } catch (NumberFormatException ignore) {
          // Ignore parse error
        }
      }
      if (nan) {
        throw new IllegalArgumentException(
            "The valuetype specified is 'NUMBER', but the passed in value is not a number.");
      }
      setNumbervalue(d);
    } else if (type == VALUETYPE.TEXT) {
      String val = value.toString();
      setTextvalue(val);
    } else if (type == VALUETYPE.STRING) {
      String val = value.toString();
      if (val.length() > 4000) {
        throw new IllegalArgumentException(
            "The maximum length for a string value is 4000 chars, use a text value for greater"
                + " lengths.");
      }
      setStringvalue(val);
    }
  }

  public PSMetadataProperty(String name, String value) {
    this(name, VALUETYPE.STRING, value);
  }

  public PSMetadataProperty(String name, int value) {
    this(name, VALUETYPE.NUMBER, value);
  }

  public PSMetadataProperty(String name, double value) {
    this(name, VALUETYPE.NUMBER, value);
  }

  public PSMetadataProperty(String name, float value) {
    this(name, VALUETYPE.NUMBER, value);
  }

  public PSMetadataProperty(String name, long value) {
    this(name, VALUETYPE.NUMBER, value);
  }

  public PSMetadataProperty(String name, short value) {
    this(name, VALUETYPE.NUMBER, value);
  }

  public PSMetadataProperty(String name, Date value) {
    this(name, VALUETYPE.DATE, value);
  }

  public PropertyId getId() {
    return id;
  }

  public void setId(PropertyId id) {
    this.id = id;
  }

  public PSMetadataEntry getMetadataEntry() {
    return Optional.ofNullable(id).map(PropertyId::getMetadataEntry).orElse(null);
  }

  public void setMetadataEntry(PSMetadataEntry metadataEntry) {
    createIdIfNull();
    id.setMetadataEntry(metadataEntry);
  }

  public String getName() {
    return Optional.ofNullable(id).map(PropertyId::getName).orElse(null);
  }

  public final void setName(String name) {
    createIdIfNull();
    id.setName(name);
  }

  public String getHash() {
    return Optional.ofNullable(id).map(PropertyId::getValueHash).orElse(null);
  }

  public VALUETYPE getValuetype() {
    return valuetype;
  }

  public void setValuetype(VALUETYPE valuetype) {
    this.valuetype = valuetype;
  }

  /**
   * Returns the untyped value.
   *
   * @return May be null.
   */
  public Object getValue() {
    return switch (getValuetype()) {
      case STRING -> getStringvalue();
      case TEXT -> getTextvalue();
      case DATE -> getDatevalue();
      case NUMBER -> getNumbervalue();
    };
  }

  public String getStringvalue() {
    if (valuetype == VALUETYPE.STRING) {
      return stringvalue;
    }
    if (valuetype == VALUETYPE.TEXT) {
      return textvalue;
    }
    if (valuetype == VALUETYPE.DATE) {
      return datevalue != null ? datevalue.toString() : "";
    }
    if (valuetype == VALUETYPE.NUMBER) {
      return numbervalue != null ? numbervalue.toString() : "";
    }
    return "";
  }

  public final void setStringvalue(String stringvalue) {
    this.valuetype = VALUETYPE.STRING;
    this.stringvalue = stringvalue;
    id.calculateHash(this.stringvalue);
  }

  public String getTextvalue() {
    return textvalue;
  }

  public final void setTextvalue(String textvalue) {
    this.valuetype = VALUETYPE.TEXT;
    this.textvalue = textvalue;
    id.calculateHash(this.textvalue);
  }

  public Date getDatevalue() {
    return datevalue;
  }

  public final void setDatevalue(Date datevalue) {
    this.valuetype = VALUETYPE.DATE;
    this.datevalue = datevalue;
    id.calculateHash(this.datevalue);
  }

  public Double getNumbervalue() {
    return numbervalue;
  }

  public final void setNumbervalue(Double numbervalue) {
    this.valuetype = VALUETYPE.NUMBER;
    this.numbervalue = numbervalue;
    id.calculateHash(this.numbervalue);
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof PSMetadataProperty) || obj == null) {
      return false;
    }
    var other = (PSMetadataProperty) obj;
    if (this.id == null || other.id == null) {
      return false;
    }
    return this.id.equals(other.id);
  }

  @Override
  public int hashCode() {
    return Optional.ofNullable(this.id).map(Object::hashCode).orElse(0);
  }

  /** Creates a new PropertyId if the current is null. */
  private void createIdIfNull() {
    if (id == null) {
      id = new PropertyId();
    }
  }

  public enum VALUETYPE {
    DATE,
    NUMBER,
    STRING,
    TEXT
  }
}

/**
 * Class that represents a composite key for PSMetadataProperty. The composite key for a metadata
 * property consists of a metadata entry (which the property belongs to) and the property name.
 */
@Embeddable
class PropertyId implements Serializable {
  private static final long serialVersionUID = 1L;

  private static final HashCalculator hashCalculator = new HashCalculator();

  @ManyToOne
  @JoinColumns(@JoinColumn(name = "ENTRY_ID", referencedColumnName = "pagePath"))
  private PSMetadataEntry metadataEntry;

  @Column(nullable = false, length = 100)
  private String name;

  @Column(name = "VALUE_HASH", nullable = false, length = 40)
  private String valueHash;

  public PSMetadataEntry getMetadataEntry() {
    return metadataEntry;
  }

  public void setMetadataEntry(PSMetadataEntry metadataEntry) {
    this.metadataEntry = metadataEntry;
  }

  public String getName() {
    return name;
  }

  public final void setName(String name) {
    this.name = StringUtils.isEmpty(name) ? null : name;
  }

  public String getValueHash() {
    return valueHash;
  }

  /**
   * Calculates the hash of the given value, using HashCalculator. If the parameter is null, then
   * the hash is calculated over an empty string. Otherwise, the hash is calculated over the result
   * of 'toString' method on the parameter.
   */
  public void calculateHash(Object value) {
    valueHash = hashCalculator.calculateHash(value == null ? StringUtils.EMPTY : value.toString());
  }

  @Override
  public int hashCode() {
    int pagePathHashCode =
        (metadataEntry != null && metadataEntry.getPagepath() != null)
            ? metadataEntry.getPagepath().hashCode()
            : 0;
    int propertyNameHashCode = name != null ? name.hashCode() : 0;
    int hashCode = valueHash != null ? valueHash.hashCode() : 0;
    return pagePathHashCode + propertyNameHashCode + hashCode;
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof PropertyId) || obj == null) {
      return false;
    }
    var other = (PropertyId) obj;
    if (this.metadataEntry == null || other.metadataEntry == null) {
      return false;
    }
    return StringUtils.equals(this.metadataEntry.getPagepath(), other.metadataEntry.getPagepath())
        && StringUtils.equals(this.name, other.name)
        && StringUtils.equals(this.valueHash, other.valueHash);
  }
}

/**
 * Responsible for calculating a hash over a value. It uses SHA-256 by default and UTF-8 to convert
 * the string value.
 */
class HashCalculator {
  private static final String HEXES = "0123456789ABCDEF";
  private static final String HASH_ALGORITHM = "SHA-256";
  private static final String CONTENT_ENCODING = "UTF-8";
  private MessageDigest digest;

  public HashCalculator() {
    try {
      digest = MessageDigest.getInstance(HASH_ALGORITHM);
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    }
  }

  public String calculateHash(String value) {
    digest.reset();
    byte[] hashResult;
    try {
      hashResult = digest.digest(value.getBytes(CONTENT_ENCODING));
      return getHex(hashResult);
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e);
    }
  }

  private String getHex(byte[] raw) {
    if (raw == null) {
      return null;
    }
    final StringBuilder hex = new StringBuilder(2 * raw.length);
    for (final byte b : raw) {
      hex.append(HEXES.charAt((b & 0xF0) >> 4)).append(HEXES.charAt((b & 0x0F)));
    }
    return hex.toString();
  }
}
