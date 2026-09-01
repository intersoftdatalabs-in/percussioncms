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
package com.percussion.rest.communities;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ValueDeserializer;

/**
 * Forces PUT {@code /services/communities/bulk} to instantiate {@link CommunityList}, not a raw
 * {@link java.util.ArrayList}.
 *
 * <p>CXF {@code JacksonJsonProvider} + {@code UNWRAP_ROOT_VALUE} otherwise ClassCasts HTTP 400
 * (peer {@code AclListDeserializer} #3391). {@link CommunityListJsonReader} is the preferred
 * JAX-RS reader; this deserializer is the Jackson-side barrier for skip-image-build hot-deploys.
 */
public class CommunityListDeserializer extends ValueDeserializer<CommunityList> {

  @Override
  public CommunityList deserialize(JsonParser p, DeserializationContext ctxt)
      throws JacksonException {
    if (p == null) {
      return new CommunityList();
    }
    JsonToken token = p.currentToken();
    if (token == null) {
      token = p.nextToken();
    }
    if (token == null || token == JsonToken.VALUE_NULL) {
      return new CommunityList();
    }
    JsonNode node = ctxt != null ? ctxt.readTree(p) : p.readValueAsTree();
    return CommunityListJsonReader.parseNode(node);
  }

  @Override
  public CommunityList getNullValue(DeserializationContext ctxt) {
    return new CommunityList();
  }
}
