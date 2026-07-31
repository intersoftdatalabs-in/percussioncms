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

import org.apache.commons.lang3.time.FastDateFormat;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;

/**
 * Custom date serializer to put the serialized date into a non numeric format. Uses the date format
 * of yyyy-MM-dd'T'HH:mm:ssZ
 *
 * @author erikserating
 */
public class PSCustomDateSerializer extends ValueSerializer<Object> {

  private final FastDateFormat formatter = FastDateFormat.getInstance("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

  public PSCustomDateSerializer() {}

  @Override
  public void serialize(
      Object value, JsonGenerator gen, @SuppressWarnings("unused") SerializationContext provider)
      throws JacksonException {
    String formattedDate = formatter.format(value);
    gen.writeString(formattedDate);
  }
}
