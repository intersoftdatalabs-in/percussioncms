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

package com.percussion.delivery.metadata;

import java.util.Date;

/**
 * A typed value attached to an {@link IPSMetadataEntry}. Each property carries the name of the
 * underlying Dublin Core or Percussion field it represents, the declared value type and one of the
 * strongly typed accessors ({@code datevalue}, {@code numbervalue}, {@code stringvalue}, {@code
 * textvalue}) used by the indexer to materialize the property.
 */
public interface IPSMetadataProperty {

  /**
   * Returns the property name.
   *
   * @return the property name, may be <code>null</code>.
   */
  public String getName();

  /**
   * Sets the property name.
   *
   * @param name the property name to set; may be <code>null</code>.
   */
  public void setName(String name);

  /**
   * Returns the declared value type of this property.
   *
   * @return the value type, never <code>null</code>.
   */
  public VALUETYPE getValuetype();

  /**
   * Returns the untyped value.
   *
   * @return May be <code>null</code>.
   */
  public Object getValue();

  /**
   * Returns the property as a {@link Date} when the declared value type is {@code DATE}.
   *
   * @return the date value, may be <code>null</code>.
   */
  public Date getDatevalue();

  /**
   * Returns the property as a {@link Double} when the declared value type is {@code NUMBER}.
   *
   * @return the numeric value, may be <code>null</code>.
   */
  public Double getNumbervalue();

  /**
   * Returns the property as a {@link String} when the declared value type is {@code STRING}.
   *
   * @return the string value, may be <code>null</code>.
   */
  public String getStringvalue();

  /**
   * Sets the date value of this property.
   *
   * @param val the date value to set; may be <code>null</code>.
   */
  public void setDatevalue(Date val);

  /**
   * Sets the numeric value of this property.
   *
   * @param val the numeric value to set; may be <code>null</code>.
   */
  public void setNumbervalue(Double val);

  /**
   * Sets the string value of this property.
   *
   * @param val the string value to set; may be <code>null</code>.
   */
  public void setStringvalue(String val);

  /**
   * Sets the free-text (long string) value of this property.
   *
   * @param val the text value to set; may be <code>null</code>.
   */
  public void setTextvalue(String val);

  /**
   * The declared value type categories recognised by the metadata indexer. Used by {@link
   * IPSMetadataProperty#getValuetype()} to pick the appropriate underlying column.
   */
  public enum VALUETYPE {
    /** A calendar date / timestamp value. */
    DATE,
    /** A numeric value (stored as a double). */
    NUMBER,
    /** A short string value. */
    STRING,
    /** A long-form / free-text value. */
    TEXT
  }
}
