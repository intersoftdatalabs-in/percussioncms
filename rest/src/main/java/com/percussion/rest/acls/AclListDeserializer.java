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
package com.percussion.rest.acls;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ValueDeserializer;

/**
 * Forces PUT {@code /services/acls/bulk} to instantiate {@link AclList}, not a raw {@link
 * java.util.ArrayList}.
 *
 * <p>CXF {@code JacksonJsonProvider} builds a collection {@code JavaType} whose default impl is
 * {@code ArrayList}. {@code UNWRAP_ROOT_VALUE} then returns that list and CXF casts it to {@link
 * AclList} — {@code ClassCastException} HTTP 400 (#3391 / #3378). {@link AclListJsonReader} is the
 * preferred JAX-RS reader, but the live pipeline may still select Jackson; this deserializer is the
 * Jackson-side barrier.
 */
public class AclListDeserializer extends ValueDeserializer<AclList> {

  @Override
  public AclList deserialize(JsonParser p, DeserializationContext ctxt) throws JacksonException {
    if (p == null) {
      return new AclList();
    }
    JsonToken token = p.currentToken();
    if (token == null) {
      token = p.nextToken();
    }
    if (token == null || token == JsonToken.VALUE_NULL) {
      return new AclList();
    }
    JsonNode node = ctxt != null ? ctxt.readTree(p) : p.readValueAsTree();
    return AclListJsonReader.parseNode(node);
  }

  @Override
  public AclList getNullValue(DeserializationContext ctxt) {
    return new AclList();
  }
}
