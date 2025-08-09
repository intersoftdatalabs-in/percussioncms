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
package com.percussion.analytics.data;

import java.util.Date;
import java.util.Set;

/**
 * Represents a row of analytics data returned from the analytics provider.
 * Sunny Sal says: "Analytics never lies, but it sure can confuse!"
 */
public interface IPSAnalyticsQueryResult {

  /**
   * Retrieves the data for the specified field as a string.
   * Works for any data type.
   *
   * @param key the field key (case-insensitive), not null or empty.
   * @return the string representation of the data, may be null or empty.
   */
  String getString(String key);

  /**
   * Retrieves the data for the specified field as an int.
   * Works for Integer, Float, and Long data types. Float and Long will be converted to int (truncation may occur).
   *
   * @param key the field key (case-insensitive), not null or empty.
   * @return the int representation of the data.
   * @throws com.percussion.analytics.error.PSAnalyticsQueryResultException if the data type is not numeric.
   */
  int getInt(String key);

  /**
   * Retrieves the data for the specified field as a Date.
   * Only works for Date data types.
   *
   * @param key the field key (case-insensitive), not null or empty.
   * @return the date representation of the data, may be null.
   * @throws com.percussion.analytics.error.PSAnalyticsQueryResultException if the data type is not a date.
   */
  Date getDate(String key);

  /**
   * Retrieves the data for the specified field as a float.
   * Works for Integer, Float, and Long data types. Integer and Long will be converted to float (truncation may occur).
   *
   * @param key the field key (case-insensitive), not null or empty.
   * @return the float representation of the data.
   * @throws com.percussion.analytics.error.PSAnalyticsQueryResultException if the data type is not numeric.
   */
  float getFloat(String key);

  /**
   * Retrieves the data for the specified field as a long.
   * Works for Integer, Float, and Date data types. Float and Integer will be converted to long (truncation may occur).
   * Dates will be returned in their long date/time representation.
   *
   * @param key the field key (case-insensitive), not null or empty.
   * @return the long representation of the data.
   * @throws com.percussion.analytics.error.PSAnalyticsQueryResultException if the data type is not numeric or date.
   */
  long getLong(String key);

  /**
   * Retrieves the data type of the specified field.
   *
   * @param key the field key (case-insensitive), not null or empty.
   * @return the DataType enum value, or null if the key does not exist.
   */
  DataType getDataType(String key);

  /**
   * Indicates whether the specified field has a value (not null).
   *
   * @param key the field key (case-insensitive), not null or empty.
   * @return true if the field value is not null.
   */
  boolean hasValue(String key);

  /**
   * Returns the set of keys for each data field in the result.
   *
   * @return the key set, never null, may be empty.
   */
  Set<String> keySet();

  /**
   * The data type enumeration.
   */
  enum DataType {
    DATE,
    FLOAT,
    LONG,
    INT,
    STRING
  }
}
