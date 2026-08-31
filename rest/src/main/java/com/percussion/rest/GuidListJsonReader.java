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
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * JSON reader for GuidList bodies (DELETE {@code /services/communities/bulk} and other bulk
 * GuidList operations).
 *
 * <p>Same ArrayList-subclass ClassCast / Jettison JSONObject barrier as {@code
 * CommunityListJsonReader}. Binds {@code {"GuidList":[{…}]}}, a bare array, or JAXB nested
 * {@code Guid}.
 */
@Provider
@Consumes({MediaType.APPLICATION_JSON, "text/json", "application/*+json", MediaType.WILDCARD})
@Priority(Priorities.USER - 100)
@PSSiteManageBean("guidListJsonReader")
public class GuidListJsonReader implements MessageBodyReader<GuidList> {

  private static final JsonMapper MAPPER = JsonMapper.builder().build();

  @Override
  public boolean isReadable(
      Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
    if (type == null || !GuidList.class.isAssignableFrom(type)) {
      return false;
    }
    return mediaType == null || isJsonCompatible(mediaType);
  }

  static boolean isJsonCompatible(MediaType mediaType) {
    if (mediaType == null || mediaType.isWildcardType() || mediaType.isWildcardSubtype()) {
      return true;
    }
    String subtype = mediaType.getSubtype();
    if (subtype == null) {
      return false;
    }
    String lower = subtype.toLowerCase();
    return "json".equals(lower) || lower.endsWith("+json");
  }

  @Override
  public GuidList readFrom(
      Class<GuidList> type,
      Type genericType,
      Annotation[] annotations,
      MediaType mediaType,
      MultivaluedMap<String, String> httpHeaders,
      InputStream entityStream)
      throws IOException {
    if (entityStream == null) {
      return new GuidList();
    }
    byte[] raw = entityStream.readAllBytes();
    if (raw.length == 0) {
      return new GuidList();
    }
    return parse(new String(raw, StandardCharsets.UTF_8));
  }

  public static GuidList parse(String json) {
    if (json == null || json.isBlank()) {
      return new GuidList();
    }
    JsonNode root;
    try {
      root = MAPPER.readTree(json);
    } catch (JacksonException e) {
      throw new WebApplicationException(
          e.getMessage() != null ? e.getMessage() : "Invalid GuidList", 400);
    }
    return parseNode(root);
  }

  public static GuidList parseNode(JsonNode root) {
    if (root == null || root.isNull() || root.isMissingNode()) {
      return new GuidList();
    }
    JsonNode array = extractGuidArray(root);
    if (array == null || !array.isArray()) {
      throw new WebApplicationException(
          "GuidList body must be {\"GuidList\":[…]} or a JSON array", 400);
    }
    GuidList list = new GuidList();
    for (JsonNode item : array) {
      if (item == null || item.isNull()) {
        continue;
      }
      JsonNode body = item;
      if (item.isObject() && item.get("Guid") != null && item.get("Guid").isObject()) {
        body = item.get("Guid");
      }
      try {
        list.add(MAPPER.treeToValue(body, Guid.class));
      } catch (JacksonException e) {
        throw new WebApplicationException(
            e.getMessage() != null ? e.getMessage() : "Invalid Guid", 400);
      }
    }
    return list;
  }

  private static JsonNode extractGuidArray(JsonNode root) {
    if (root.isArray()) {
      return root;
    }
    if (!root.isObject()) {
      return null;
    }
    JsonNode nested = firstNonNull(root, "GuidList", "guidList");
    if (nested == null) {
      return null;
    }
    if (nested.isArray()) {
      return nested;
    }
    if (nested.isObject()) {
      JsonNode jaxb = firstNonNull(nested, "Guid", "guid");
      if (jaxb != null && jaxb.isArray()) {
        return jaxb;
      }
      if (jaxb != null && jaxb.isObject()) {
        var arr = MAPPER.createArrayNode();
        arr.add(jaxb);
        return arr;
      }
    }
    return null;
  }

  private static JsonNode firstNonNull(JsonNode root, String... names) {
    for (String name : names) {
      JsonNode n = root.get(name);
      if (n != null && !n.isNull()) {
        return n;
      }
    }
    return null;
  }
}
