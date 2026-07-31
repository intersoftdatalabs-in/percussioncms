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

import com.percussion.share.service.exception.PSDataServiceException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;

/** Jackson deserializer for LocalDateTime. Handles ISO-8601 and common variants. */
public class LocalDateDeserializer extends ValueDeserializer<LocalDateTime> {

  private static final Logger log = LogManager.getLogger(LocalDateDeserializer.class);

  @Override
  public LocalDateTime deserialize(JsonParser parser, DeserializationContext ctxt)
      throws JacksonException {
    var dateInStringFormat = parser.getText();
    try {
      return Optional.ofNullable(dateInStringFormat)
          .map(String::trim)
          .filter(s -> !s.isEmpty())
          .map(
              s -> {
                try {
                  return LocalDateTime.parse(s, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                } catch (Exception e) {
                  var fixed =
                      s.replaceAll("T(\\d:)", "T0$1")
                          .replaceAll(":(\\d:)", ":0$1")
                          .replaceAll(":(\\d{1,2})\\.", ":$1.");
                  try {
                    return LocalDateTime.parse(fixed, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                  } catch (Exception ex) {
                    log.error("Failed to parse LocalDateTime: '{}'", s, ex);
                    return null;
                  }
                }
              })
          .orElse(null);
    } catch (Exception e) {
      log.error(
          "Exception occurred while parsing: '{}'",
          dateInStringFormat,
          new PSDataServiceException(e.getMessage()));
      return null;
    }
  }
}
