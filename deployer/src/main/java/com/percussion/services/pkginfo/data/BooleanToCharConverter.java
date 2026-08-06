/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
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

package com.percussion.services.pkginfo.data;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Converts a Boolean Java type to/from a CHAR(1) database column. This converter maps: - true ->
 * 'Y' - false -> 'N' - null -> null
 *
 * <p>This is necessary because the PSX_PKG_DEPENDENCY.IMPLIED_DEP column is defined as CHAR(1) but
 * the JPA entity uses a Boolean field. Storing a Boolean directly would result in "true" or "false"
 * (5+ characters) which cannot fit in CHAR(1).
 */
@Converter
public class BooleanToCharConverter implements AttributeConverter<Boolean, String> {

  /** Default constructor for JPA. */
  public BooleanToCharConverter() {}

  /**
   * Converts the Boolean attribute to a String for storing in the database.
   *
   * @param attribute the Boolean value from the entity
   * @return 'Y' for true, 'N' for false, null for null
   */
  @Override
  public String convertToDatabaseColumn(Boolean attribute) {
    if (attribute == null) {
      return null;
    }
    return attribute ? "Y" : "N";
  }

  /**
   * Converts the String from the database column to a Boolean attribute.
   *
   * @param dbData the String value from the database column
   * @return true for 'Y', false for 'N', null for null
   */
  @Override
  public Boolean convertToEntityAttribute(String dbData) {
    if (dbData == null) {
      return null;
    }
    return "Y".equalsIgnoreCase(dbData)
        || "1".equalsIgnoreCase(dbData)
        || "T".equalsIgnoreCase(dbData);
  }
}
