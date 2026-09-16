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

package com.percussion.rest.relationshiptypes;

import com.percussion.system.utils.PSSiteManageBean;
import jakarta.annotation.Priority;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.MessageBodyReader;
import jakarta.ws.rs.ext.Provider;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

/**
 * JSON reader for {@link RelationshipType} PUT so {@code cloneOverrides: []} stays an empty list
 * (clear) instead of {@code null} (omit / unchanged) under CXF {@code UNWRAP_ROOT_VALUE} (#4470).
 *
 * <p>Must be listed on {@code rest-jax-rs} {@code jaxrs:providers} ahead of {@code jacksonProvider}.
 */
@Provider
@Consumes(MediaType.APPLICATION_JSON)
@Priority(Priorities.USER - 100)
@PSSiteManageBean("relationshipTypeJsonReader")
public class RelationshipTypeJsonReader implements MessageBodyReader<RelationshipType> {

  /** Mapper without UNWRAP_ROOT_VALUE so the envelope is inspected explicitly. */
  private static final JsonMapper MAPPER = JsonMapper.builder().build();

  @Override
  public boolean isReadable(
      Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
    if (type == null || !RelationshipType.class.isAssignableFrom(type)) {
      return false;
    }
    return mediaType == null || isJsonCompatible(mediaType);
  }

  @Override
  public RelationshipType readFrom(
      Class<RelationshipType> type,
      Type genericType,
      Annotation[] annotations,
      MediaType mediaType,
      MultivaluedMap<String, String> httpHeaders,
      InputStream entityStream)
      throws IOException {
    if (entityStream == null) {
      return new RelationshipType();
    }
    byte[] raw = entityStream.readAllBytes();
    if (raw.length == 0) {
      return new RelationshipType();
    }
    return parse(new String(raw, StandardCharsets.UTF_8));
  }

  /**
   * Bind RelationshipType JSON. Present {@code cloneOverrides: []} is an empty list (clear), not
   * {@code null} (omit).
   *
   * @param json request body; may be null
   * @return non-null DTO
   */
  public static RelationshipType parse(String json) {
    if (json == null || json.isBlank()) {
      return new RelationshipType();
    }
    JsonNode root;
    try {
      root = MAPPER.readTree(json);
    } catch (JacksonException e) {
      throw new WebApplicationException(
          e.getMessage() != null ? e.getMessage() : "Invalid relationship type", 400);
    }
    if (root == null || root.isNull() || root.isMissingNode()) {
      return new RelationshipType();
    }
    if (!root.isObject()) {
      throw new WebApplicationException("Invalid relationship type", 400);
    }
    JsonNode nested = firstObject(root, "RelationshipType", "relationshipType");
    JsonNode fields = nested != null ? nested : root;
    JsonNode fieldsForDto = fields;
    if (fields.isObject() && fields.has("cloneOverrides")) {
      ObjectNode copy = (ObjectNode) fields.deepCopy();
      copy.remove("cloneOverrides");
      fieldsForDto = copy;
    }
    RelationshipType out;
    try {
      out = MAPPER.convertValue(fieldsForDto, RelationshipType.class);
    } catch (IllegalArgumentException e) {
      throw new WebApplicationException(
          e.getMessage() != null ? e.getMessage() : "Invalid relationship type", 400);
    }
    if (out == null) {
      out = new RelationshipType();
    }
    if (fields.has("cloneOverrides") && !fields.get("cloneOverrides").isNull()) {
      out.setCloneOverrides(overridesFromNode(fields.get("cloneOverrides")));
    }
    return out;
  }

  static List<RelationshipTypeCloneOverride> overridesFromNode(JsonNode raw) {
    List<RelationshipTypeCloneOverride> out = new ArrayList<>();
    if (raw == null || raw.isNull() || raw.isMissingNode()) {
      return out;
    }
    if (raw.isArray()) {
      for (JsonNode n : raw) {
        RelationshipTypeCloneOverride row = overrideFromNode(n);
        if (row != null) {
          out.add(row);
        }
      }
      return out;
    }
    if (raw.isObject()) {
      if (raw.isEmpty()) {
        return out;
      }
      JsonNode wrapped = raw.get("RelationshipTypeCloneOverride");
      if (wrapped == null) {
        wrapped = raw.get("relationshipTypeCloneOverride");
      }
      if (wrapped != null) {
        return overridesFromNode(wrapped);
      }
      RelationshipTypeCloneOverride row = overrideFromNode(raw);
      if (row != null) {
        out.add(row);
      }
    }
    return out;
  }

  private static RelationshipTypeCloneOverride overrideFromNode(JsonNode n) {
    if (n == null || n.isNull() || !n.isObject() || n.isEmpty()) {
      return null;
    }
    RelationshipTypeCloneOverride row = new RelationshipTypeCloneOverride();
    row.setFieldName(textField(n, "fieldName"));
    row.setExtensionRef(textField(n, "extensionRef"));
    if (row.getFieldName() == null && row.getExtensionRef() == null) {
      return null;
    }
    JsonNode params = n.get("extensionParams");
    if (params != null && !params.isNull()) {
      List<String> values = new ArrayList<>();
      if (params.isArray()) {
        for (JsonNode p : params) {
          values.add(p == null || p.isNull() ? "" : p.asString());
        }
      } else if (params.isString() || params.isNumber()) {
        values.add(params.asString());
      }
      row.setExtensionParams(values);
    }
    return row;
  }

  private static JsonNode firstObject(JsonNode root, String... names) {
    for (String name : names) {
      JsonNode n = root.get(name);
      if (n != null && n.isObject()) {
        return n;
      }
    }
    return null;
  }

  private static String textField(JsonNode node, String name) {
    JsonNode n = node.get(name);
    if (n == null || n.isNull()) {
      return null;
    }
    if (n.isString() || n.isNumber()) {
      String v = n.asString();
      return v != null && !v.isBlank() ? v : null;
    }
    return null;
  }

  private static boolean isJsonCompatible(MediaType mediaType) {
    if (mediaType == null) {
      return true;
    }
    String subtype = mediaType.getSubtype();
    return "json".equalsIgnoreCase(subtype)
        || (subtype != null && subtype.toLowerCase().endsWith("+json"));
  }
}
