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
package com.percussion.rest;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ValueDeserializer;

/**
 * Instantiates {@link GuidList} instead of a raw {@code ArrayList} after CXF UNWRAP_ROOT_VALUE.
 * Peer {@link com.percussion.rest.acls.AclListDeserializer}.
 */
public class GuidListDeserializer extends ValueDeserializer<GuidList> {

  @Override
  public GuidList deserialize(JsonParser p, DeserializationContext ctxt) throws JacksonException {
    if (p == null) {
      return new GuidList();
    }
    JsonToken token = p.currentToken();
    if (token == null) {
      token = p.nextToken();
    }
    if (token == null || token == JsonToken.VALUE_NULL) {
      return new GuidList();
    }
    JsonNode node = ctxt != null ? ctxt.readTree(p) : p.readValueAsTree();
    return GuidListJsonReader.parseNode(node);
  }

  @Override
  public GuidList getNullValue(DeserializationContext ctxt) {
    return new GuidList();
  }
}
