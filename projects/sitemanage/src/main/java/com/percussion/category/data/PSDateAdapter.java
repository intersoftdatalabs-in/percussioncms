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

package com.percussion.category.data;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Optional;
import javax.xml.bind.annotation.adapters.XmlAdapter;

/** JAXB adapter for serializing/deserializing LocalDateTime as ISO-8601 strings. */
public class PSDateAdapter extends XmlAdapter<String, LocalDateTime> {

  @Override
  public String marshal(LocalDateTime date) {
    return Optional.ofNullable(date).map(LocalDateTime::toString).orElse(null);
  }

  @Override
  public LocalDateTime unmarshal(String date) {
    try {
      return Optional.ofNullable(date)
          .map(String::trim)
          .filter(s -> !s.isEmpty())
          .map(LocalDateTime::parse)
          .orElse(null);
    } catch (DateTimeParseException e) {
      // Log or handle parse exception as needed
      return null;
    }
  }
}
