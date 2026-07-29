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

package com.ibm.cadf.model;

import com.ibm.cadf.exception.CADFException;
import org.apache.commons.lang3.StringUtils;

/**
 * Helper that assembles CADF {@code name?value=&lt;value&gt;} tag strings. Non-instantiable by
 * callers; methods are static.
 */
public class Tag {

  /** Default no-argument constructor for {@link Tag}. */
  public Tag() {}

  /**
   * Builds a CADF tag string of the form {@code name?value=value}.
   *
   * @param name the tag name, never {@code null} or empty.
   * @param value the tag value, never {@code null} or empty.
   * @return the assembled {@code name?value=&lt;value&gt;} string.
   * @throws CADFException when either {@code name} or {@code value} is blank.
   */
  public String generate_name_value_tag(String name, String value) throws CADFException {
    // Generate a CADF tag in the format name?value=<value>
    // param name: name of tag
    // param valuue: optional value tag

    if (StringUtils.isEmpty(name) || StringUtils.isEmpty(value)) {
      throw new CADFException("'Invalid name and/or value. Values cannot be Empty or Null");
    }
    String tag = name + "?value=" + value;
    return tag;
  }

  /**
   * Returns {@code true} when the supplied string is non-empty (not {@code null} and not blank).
   *
   * @param value the string to validate, may be {@code null}.
   * @return {@code true} when {@code value} is non-empty.
   */
  public boolean isValid(String value) {
    return StringUtils.isNotEmpty(value);
  }
}
