/*
 * Copyright 1999-2023 Percussion Software, Inc.
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
package com.percussion.delivery.services;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import java.io.IOException;
import org.apache.commons.lang3.time.FastDateFormat;

/**
 * Custom date serializer to put the serialized date into a non numeric format. Uses the date format
 * of yyyy-MM-dd'T'HH:mm:ssZ
 *
 * @author erikserating
 */
public class PSCustomDateSerializer extends JsonSerializer<Object> {

  private final FastDateFormat formatter = FastDateFormat.getInstance("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

  /**
   * Default constructor. The {@link SerializerProvider} argument is not currently used by this
   * implementation; it is a placeholder for future enhancements such as locale-aware formatting.
   */
  public PSCustomDateSerializer() {}

  /**
   * Serializes the supplied date value as a string using {@code yyyy-MM-dd'T'HH:mm:ss.SSSZ}.
   *
   * @param value the value to serialize; expected to be a {@link java.util.Date}.
   * @param gen the Jackson generator to write to, never <code>null</code>.
   * @param provider the serializer provider, reserved for future use.
   * @throws IOException if writing to the generator fails.
   */
  @Override
  public void serialize(
      Object value, JsonGenerator gen, @SuppressWarnings("unused") SerializerProvider provider)
      throws IOException {
    String formattedDate = formatter.format(value);

    gen.writeString(formattedDate);
  }
}
