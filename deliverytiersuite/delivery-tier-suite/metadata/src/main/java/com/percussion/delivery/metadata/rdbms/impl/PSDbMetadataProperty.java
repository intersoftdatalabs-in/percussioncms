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
package com.percussion.delivery.metadata.rdbms.impl;

import com.percussion.delivery.metadata.IPSMetadataProperty;
import com.percussion.delivery.metadata.utils.PSHashCalculator;
import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Nationalized;

/**
 * Represents a metadata property name / value pair attached to a {@link PSDbMetadataEntry}.
 *
 * <p>{@code datevalue} is a {@link LocalDateTime} so Hibernate 7 maps the TIMESTAMP column without
 * deprecated {@code @Temporal}.
 *
 * @author erikserating
 */
@Entity
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region = "PSMetadataProperty")
@Table(
    name = "PERC_PAGE_METADATA_PROPERTIES",
    indexes = {
      @Index(columnList = "ENTRY_ID", name = "entryId_hidx"),
      @Index(columnList = "NAME,DATEVALUE", name = "name_date_hidx"),
      @Index(columnList = "NAME,VALUE_HASH", name = "name_valuehash_hidx")
    })
public final class PSDbMetadataProperty implements IPSMetadataProperty {

  /** Surrogate primary key for this property row. */
  @Id
  @Column(unique = true, name = "ID", nullable = false)
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private int id;

  /** Declared value type of this property. */
  @Basic @Nationalized private VALUETYPE valuetype;

  /** Short string value of this property (used for {@link VALUETYPE#STRING} entries). */
  @Column(length = 4000)
  @Nationalized
  private String stringvalue;

  /** Property name, e.g. {@code dcterms:creator}. */
  @Column(nullable = false, length = PSDbMetadataProperty.MAX_PROPERTY_NAME_LENGTH)
  @Nationalized
  private String name;

  /** Long-form text value (used for {@link VALUETYPE#TEXT} entries). */
  @Column(length = Integer.MAX_VALUE)
  @Nationalized
  @Lob
  @Basic(fetch = FetchType.LAZY)
  private String textvalue;

  /** Date value of this property (used for {@link VALUETYPE#DATE} entries). */
  @Basic private LocalDateTime datevalue;

  /** Numeric value of this property (used for {@link VALUETYPE#NUMBER} entries). */
  @Basic private Double numbervalue;

  /**
   * Hash of the property's value. It's updated when the {@link #calculateHash(Object)} function is
   * called.
   */
  @Column(name = "VALUE_HASH", nullable = false, length = 40)
  @Nationalized
  private String valueHash;

  /** This field represents the max length that the name of an instance of this class can have. */
  public static final int MAX_PROPERTY_NAME_LENGTH = 100;

  /** No-arg constructor required by Hibernate. */
  public PSDbMetadataProperty() {}

  /** HashCalculator instance used to get the hash of the metadata property's value. */
  private static PSHashCalculator hashCalculator = new PSHashCalculator();

  /**
   * Ctor to create a property of the specified valuetype.
   *
   * @param name the property name, cannot be <code>null</code> or empty.
   * @param type the {@link #valuetype} for the property. Cannot be <code>null</code>.
   * @param value the value to be stored in the property. May be <code>null</code> or empty.
   */
  public PSDbMetadataProperty(String name, VALUETYPE type, Object value) {
    this();

    if (name == null || name.length() == 0)
      throw new IllegalArgumentException("name cannot be null or empty.");
    if (type == null) throw new IllegalArgumentException("type cannot be null.");
    this.setName(name);
    boolean nan = true;
    if (type == VALUETYPE.DATE) {
      setDatevalue(toLocalDateTime(value));
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

        }
      }
      if (nan)
        throw new IllegalArgumentException(
            "The valuetype specified is 'NUMBER', but the passed in value is not a number.");
      setNumbervalue(d);
    } else if (type == VALUETYPE.TEXT) {
      String val = value.toString();
      setTextvalue(val);
    } else if (type == VALUETYPE.STRING) {
      String val = value.toString();
      if (val.length() > 4000)
        throw new IllegalArgumentException(
            "The maximum length for a string value is 4000 chars, use a text value for greater"
                + " lengths.");
      setStringvalue(val);
    }
  }

  /**
   * Convenience ctor to create a string value type property.
   *
   * @param name cannot be <code>null</code> or empty.
   * @param value the value, may be <code>null</code>.
   */
  public PSDbMetadataProperty(String name, String value) {
    this(name, VALUETYPE.STRING, value);
  }

  /**
   * Convenience ctor to create a number value type property from an int value.
   *
   * @param name the property name; cannot be <code>null</code> or empty.
   * @param value the int value to wrap.
   */
  public PSDbMetadataProperty(String name, int value) {
    this(name, VALUETYPE.NUMBER, value);
  }

  /**
   * Convenience ctor to create a number value type property from a double value.
   *
   * @param name the property name; cannot be <code>null</code> or empty.
   * @param value the double value to wrap.
   */
  public PSDbMetadataProperty(String name, double value) {
    this(name, VALUETYPE.NUMBER, value);
  }

  /**
   * Convenience ctor to create a number value type property from a float value.
   *
   * @param name the property name; cannot be <code>null</code> or empty.
   * @param value the float value to wrap.
   */
  public PSDbMetadataProperty(String name, float value) {
    this(name, VALUETYPE.NUMBER, value);
  }

  /**
   * Convenience ctor to create a number value type property from a long value.
   *
   * @param name the property name; cannot be <code>null</code> or empty.
   * @param value the long value to wrap.
   */
  public PSDbMetadataProperty(String name, long value) {
    this(name, VALUETYPE.NUMBER, value);
  }

  /**
   * Convenience ctor to create a number value type property from a short value.
   *
   * @param name the property name; cannot be <code>null</code> or empty.
   * @param value the short value to wrap.
   */
  public PSDbMetadataProperty(String name, short value) {
    this(name, VALUETYPE.NUMBER, value);
  }

  /**
   * Convenience ctor to create a Date value type property from a {@link LocalDateTime}.
   *
   * @param name name cannot be <code>null</code> or empty.
   * @param value may be <code>null</code>.
   */
  public PSDbMetadataProperty(String name, LocalDateTime value) {
    this(name, VALUETYPE.DATE, value);
  }

  /**
   * Convenience ctor to create a Date value type property from a legacy {@link Date}.
   *
   * @param name name cannot be <code>null</code> or empty.
   * @param value may be <code>null</code>.
   */
  public PSDbMetadataProperty(String name, Date value) {
    this(name, VALUETYPE.DATE, value);
  }

  /**
   * Returns the owning metadata entry, populated by the JPA {@code @ManyToOne} association.
   *
   * @return the metadataEntry, may be <code>null</code> for transient instances.
   */
  public PSDbMetadataEntry getMetadataEntry() {
    return entry;
  }

  /**
   * Sets the owning metadata entry.
   *
   * @param metadataEntry the metadataEntry to set.
   */
  public void setMetadataEntry(PSDbMetadataEntry metadataEntry) {
    entry = metadataEntry;
  }

  /**
   * @return the name
   */
  public String getName() {
    return name;
  }

  /**
   * @param name the name to set
   */
  public void setName(String name) {
    this.name = name;
  }

  /** Owning metadata entry, populated through the JPA {@code @ManyToOne} association. */
  @ManyToOne(optional = false)
  @JoinColumns(@JoinColumn(name = "ENTRY_ID", referencedColumnName = "pagepathhash"))
  private PSDbMetadataEntry entry;

  /**
   * Returns the cached hash of this property's value.
   *
   * @return the value hash as a {@link String}, never <code>null</code> after the property has been
   *     initialised.
   */
  public String getHash() {
    return valueHash;
  }

  /**
   * Calculates the hash of the given value, using {@link PSHashCalculator}. If the parameter is
   * {@code null} then the hash is calculated over an empty string. If not, the hash is calculated
   * over the result of {@code toString()} on the parameter.
   *
   * @param value the value to hash; may be <code>null</code>.
   */
  public void calculateHash(Object value) {
    if (value == null) valueHash = hashCalculator.calculateHash(StringUtils.EMPTY);
    else valueHash = hashCalculator.calculateHash(value.toString());
  }

  /**
   * @return the valuetype
   */
  public VALUETYPE getValuetype() {
    return valuetype;
  }

  /**
   * Sets the declared value type of this property.
   *
   * @param valuetype the valuetype to set.
   */
  public void setValuetype(VALUETYPE valuetype) {
    this.valuetype = valuetype;
  }

  /**
   * Returns the untyped value.
   *
   * @return May be <code>null</code>.
   */
  public Object getValue() {
    Object result = null;
    switch (getValuetype()) {
      case STRING:
        result = getStringvalue();
        break;

      case TEXT:
        result = getTextvalue();
        break;

      case DATE:
        result = getDatevalue();
        break;

      case NUMBER:
        result = getNumbervalue();
        break;
    }

    return result;
  }

  /**
   * @return the stringvalue
   */
  public String getStringvalue() {
    if (valuetype == VALUETYPE.STRING) return stringvalue;
    if (valuetype == VALUETYPE.TEXT) return textvalue;
    if (valuetype == VALUETYPE.DATE) {
      return datevalue == null ? "" : datevalue.toString();
    }
    if (valuetype == VALUETYPE.NUMBER) {
      return numbervalue.toString();
    }
    return "";
  }

  /**
   * @param stringvalue the stringvalue to set
   */
  public void setStringvalue(String stringvalue) {
    this.valuetype = VALUETYPE.STRING;
    this.stringvalue = stringvalue;

    calculateHash(this.stringvalue);
  }

  /**
   * Returns the long-form free-text value of this property.
   *
   * @return the textvalue, may be <code>null</code> when this property has a non-text value type or
   *     when no value has been set.
   */
  public String getTextvalue() {
    return textvalue;
  }

  /**
   * @param textvalue the textvalue to set
   */
  public void setTextvalue(String textvalue) {
    this.valuetype = VALUETYPE.TEXT;
    this.textvalue = textvalue;

    calculateHash(this.textvalue);
  }

  /**
   * @return the datevalue
   */
  public LocalDateTime getDatevalue() {
    return datevalue;
  }

  /**
   * @param datevalue the datevalue to set
   */
  public void setDatevalue(LocalDateTime datevalue) {
    this.valuetype = VALUETYPE.DATE;
    this.datevalue = datevalue;

    calculateHash(this.datevalue);
  }

  /**
   * @return the numbervalue
   */
  public Double getNumbervalue() {
    return numbervalue;
  }

  /**
   * @param numbervalue the numbervalue to set
   */
  public void setNumbervalue(Double numbervalue) {
    this.valuetype = VALUETYPE.NUMBER;
    this.numbervalue = numbervalue;

    calculateHash(this.numbervalue);
  }

  /**
   * Returns the surrogate primary key for this property.
   *
   * @return the database id, never negative after the property has been persisted.
   */
  public int getId() {
    return this.id;
  }

  /**
   * Coerces supported date-like inputs to {@link LocalDateTime} using the JVM default zone for
   * legacy {@link Date} / {@link Instant} values.
   *
   * @param value the raw date value; may be {@code null}.
   * @return the coerced local date-time, or {@code null} when {@code value} is {@code null}.
   */
  static LocalDateTime toLocalDateTime(Object value) {
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
    throw new IllegalArgumentException(
        "Value type 'Date' was specified but the passed in value is not a date object.");
  }
}
