/*
 * Copyright 1999-2026 Percussion Software, Inc.
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
package com.percussion.services.sitemgr.data;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Maps Java {@link Boolean} to/from RXSITES-style CHAR(1) flags stored as {@code T}/{@code F}.
 *
 * <p>Hibernate would otherwise persist {@code Boolean.toString()} values ({@code "true"} /
 * {@code "false"}), which do not fit in CHAR(1) and fail site create on H2 (and other engines
 * with the same schema). Seed data and column defaults use {@code T}/{@code F}.
 *
 * <p>Reads also accept legacy/other dialects: {@code Y}/{@code N}, {@code 1}/{@code 0}, and the
 * strings {@code true}/{@code false}.
 */
@Converter
public class BooleanToTFCharConverter implements AttributeConverter<Boolean, String> {

  @Override
  public String convertToDatabaseColumn(Boolean attribute) {
    if (attribute == null) {
      return null;
    }
    return attribute ? "T" : "F";
  }

  @Override
  public Boolean convertToEntityAttribute(String dbData) {
    if (dbData == null) {
      return null;
    }
    String v = dbData.trim();
    if (v.isEmpty()) {
      return null;
    }
    return isTruthy(v);
  }

  /**
   * Package-visible helper for String-backed CHAR(1) flag fields on the same entity that are not
   * typed as {@link Boolean}.
   */
  static boolean isTruthy(String v) {
    return "T".equalsIgnoreCase(v)
        || "Y".equalsIgnoreCase(v)
        || "1".equals(v)
        || "true".equalsIgnoreCase(v);
  }

  static String toChar(boolean value) {
    return value ? "T" : "F";
  }
}
