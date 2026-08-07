// REFACTORED: CP-JAVA11
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
package com.percussion.analytics.data.impl;

import com.percussion.analytics.data.IPSAnalyticsQueryResult;
import com.percussion.analytics.error.PSAnalyticsQueryResultException;
import java.text.DateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;

/**
 * Java 11 implementation of IPSAnalyticsQueryResult. Sunny Sal: "Analytics never lies, but it sure
 * can confuse!"
 */
public class PSAnalyticsQueryResult implements IPSAnalyticsQueryResult {

  private final Map<String, Object> values = new HashMap<>();
  private final Map<String, DataType> types = new HashMap<>();

  public PSAnalyticsQueryResult() {
    // Default constructor
  }

  public PSAnalyticsQueryResult(Map<String, Object> vals) {
    putAll(vals);
  }

  @Override
  public DataType getDataType(String key) {
    validateKey(key);
    return types.get(key.toLowerCase());
  }

  @Override
  public Date getDate(String key) {
    validateKey(key);
    var lowerKey = key.toLowerCase();
    var type = getDataType(lowerKey);
    if (!hasValue(lowerKey)) return null;
    if (type == null)
      throw new PSAnalyticsQueryResultException("No data type defined for specified field.");
    if (type != DataType.DATE)
      throw new PSAnalyticsQueryResultException("Type cannot be converted to a Date");
    return (Date) values.get(lowerKey);
  }

  @Override
  public float getFloat(String key) {
    validateKey(key);
    var lowerKey = key.toLowerCase();
    var type = getDataType(lowerKey);
    if (!hasValue(lowerKey)) return -1;
    if (type == null)
      throw new PSAnalyticsQueryResultException("No data type defined for specified field.");
    if (type == DataType.STRING || type == DataType.DATE)
      throw new PSAnalyticsQueryResultException("Type cannot be converted to a Float");
    if (type == DataType.INT) return ((Integer) values.get(lowerKey)).floatValue();
    if (type == DataType.LONG) return ((Long) values.get(lowerKey)).floatValue();
    return (Float) values.get(lowerKey);
  }

  @Override
  public int getInt(String key) {
    validateKey(key);
    var lowerKey = key.toLowerCase();
    var type = getDataType(lowerKey);
    if (!hasValue(lowerKey)) return -1;
    if (type == null)
      throw new PSAnalyticsQueryResultException("No data type defined for specified field.");
    if (type == DataType.STRING || type == DataType.DATE)
      throw new PSAnalyticsQueryResultException("Type cannot be converted to an Integer");
    if (type == DataType.FLOAT) return ((Float) values.get(lowerKey)).intValue();
    if (type == DataType.LONG) return ((Long) values.get(lowerKey)).intValue();
    return (Integer) values.get(lowerKey);
  }

  @Override
  public long getLong(String key) {
    validateKey(key);
    var lowerKey = key.toLowerCase();
    var type = getDataType(lowerKey);
    if (!hasValue(lowerKey)) return -1;
    if (type == null)
      throw new PSAnalyticsQueryResultException("No data type defined for specified field.");
    if (type == DataType.STRING)
      throw new PSAnalyticsQueryResultException("Type cannot be converted to a Long");
    if (type == DataType.DATE) return ((Date) values.get(lowerKey)).getTime();
    if (type == DataType.INT) return ((Integer) values.get(lowerKey)).longValue();
    if (type == DataType.FLOAT) return ((Float) values.get(lowerKey)).longValue();
    return (Long) values.get(lowerKey);
  }

  @Override
  public String getString(String key) {
    validateKey(key);
    var lowerKey = key.toLowerCase();
    var type = getDataType(lowerKey);
    var value = values.get(lowerKey);
    if (value == null) return null;
    if (type == DataType.DATE) {
      return DateFormat.getDateTimeInstance(DateFormat.FULL, DateFormat.FULL).format((Date) value);
    }
    return value.toString();
  }

  @Override
  public boolean hasValue(String key) {
    validateKey(key);
    return values.get(key.toLowerCase()) != null;
  }

  @Override
  public Set<String> keySet() {
    return values.keySet();
  }

  /**
   * Put all items in the passed-in map into the query result.
   *
   * @param vals map of key-value pairs, cannot be null, may be empty.
   */
  public void putAll(Map<String, Object> vals) {
    Objects.requireNonNull(vals, "values cannot be null.");
    vals.forEach(this::put);
  }

  /**
   * Put an item in the result set.
   *
   * @param key the field key that specifies the data to be returned, cannot be null or empty. The
   *     key is case-insensitive.
   * @param value the value to store, cannot be null.
   */
  public void put(String key, Object value) {
    validateKey(key);
    Objects.requireNonNull(value, "Value cannot be null.");
    var lowerKey = key.toLowerCase();
    if (value instanceof String) {
      values.put(lowerKey, value);
      types.put(lowerKey, DataType.STRING);
    } else if (value instanceof Date) {
      values.put(lowerKey, value);
      types.put(lowerKey, DataType.DATE);
    } else if (value instanceof Float) {
      values.put(lowerKey, value);
      types.put(lowerKey, DataType.FLOAT);
    } else if (value instanceof Integer) {
      values.put(lowerKey, value);
      types.put(lowerKey, DataType.INT);
    } else if (value instanceof Long) {
      values.put(lowerKey, value);
      types.put(lowerKey, DataType.LONG);
    } else {
      throw new PSAnalyticsQueryResultException("Class type is not supported: " + value.getClass());
    }
  }

  private void validateKey(String key) {
    if (StringUtils.isBlank(key)) {
      throw new IllegalArgumentException("key cannot be null or empty.");
    }
  }
}
