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
import org.apache.commons.lang3.time.FastDateFormat;
import java.io.IOException;

/**
 * Custom date serializer to output the serialized date in a non-numeric
 * format. Uses the date format of yyyy-MM-dd'T'HH:mm:ss.SSSZ.
 *
 * @author erikserating
 * // REFACTORED: CP-JAVA11
 */
public class PSCustomDateSerializer extends JsonSerializer<Object> {

    private final FastDateFormat formatter =
            FastDateFormat.getInstance("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

    @Override
    public void serialize(Object value, JsonGenerator gen,
                          @SuppressWarnings("unused") SerializerProvider provider)
            throws IOException {
        var formattedDate = formatter.format(value);
        gen.writeString(formattedDate);
    }
}
