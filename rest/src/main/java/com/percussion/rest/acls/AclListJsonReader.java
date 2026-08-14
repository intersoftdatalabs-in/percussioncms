/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * you may obtain a copy of the License at
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
 * JSON reader for PUT {@code /services/acls/bulk}.
 *
 * <p>CXF {@code UNWRAP_ROOT_VALUE} plus {@link AclList} extending {@link java.util.ArrayList}
 * yields {@code ClassCastException: Cannot cast java.util.ArrayList to AclList} (HTTP 400). Display
 * Format Object ACL Save hit that path (#3378 / QA #2640). This reader binds either:
 *
 * <ul>
 *   <li>{@code {"AclList":[{…}]}} (preferred SPA envelope)
 *   <li>{@code [{…}]} (legacy bare array)
 *   <li>{@code {"AclList":{"Acl":[…]}}} (JAXB-style nested)
 * </ul>
 *
 * <p>Must be listed on {@code rest-jax-rs} {@code jaxrs:providers} ahead of {@code
 * jacksonProvider}.
 */
@Provider
@Consumes({MediaType.APPLICATION_JSON, "text/json", "application/*+json", MediaType.WILDCARD})
@Priority(Priorities.USER - 100)
@PSSiteManageBean("aclListJsonReader")
public class AclListJsonReader implements MessageBodyReader<AclList> {

  /** Mapper without UNWRAP_ROOT_VALUE so we can inspect the envelope ourselves. */
  private static final JsonMapper MAPPER = JsonMapper.builder().build();

  @Override
  public boolean isReadable(
      Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
    if (type == null || !AclList.class.isAssignableFrom(type)) {
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
  public AclList readFrom(
      Class<AclList> type,
      Type genericType,
      Annotation[] annotations,
      MediaType mediaType,
      MultivaluedMap<String, String> httpHeaders,
      InputStream entityStream)
      throws IOException {
    if (entityStream == null) {
      return new AclList();
    }
    byte[] raw = entityStream.readAllBytes();
    if (raw.length == 0) {
      return new AclList();
    }
    return parse(new String(raw, StandardCharsets.UTF_8));
  }

  /**
   * Bind an ACL bulk-save body to {@link AclList}.
   *
   * @param json request body; may be null
   * @return non-null list (empty when the body is blank)
   */
  public static AclList parse(String json) {
    if (json == null || json.isBlank()) {
      return new AclList();
    }
    JsonNode root;
    try {
      root = MAPPER.readTree(json);
    } catch (JacksonException e) {
      throw new WebApplicationException(
          e.getMessage() != null ? e.getMessage() : "Invalid AclList", 400);
    }
    return parseNode(root);
  }

  /**
   * Bind a parsed JSON tree to {@link AclList}.
   *
   * @param root parsed body; may be null
   * @return non-null list (empty when the node is null/missing)
   */
  public static AclList parseNode(JsonNode root) {
    if (root == null || root.isNull() || root.isMissingNode()) {
      return new AclList();
    }
    JsonNode array = extractAclArray(root);
    if (array == null || !array.isArray()) {
      throw new WebApplicationException(
          "AclList body must be {\"AclList\":[…]} or a JSON array", 400);
    }
    AclList list = new AclList();
    for (JsonNode item : array) {
      if (item == null || item.isNull()) {
        continue;
      }
      JsonNode aclNode = item;
      if (item.isObject() && item.get("Acl") != null && item.get("Acl").isObject()) {
        aclNode = item.get("Acl");
      }
      try {
        list.add(MAPPER.treeToValue(aclNode, Acl.class));
      } catch (JacksonException e) {
        throw new WebApplicationException(
            e.getMessage() != null ? e.getMessage() : "Invalid Acl", 400);
      }
    }
    return list;
  }

  private static JsonNode extractAclArray(JsonNode root) {
    if (root.isArray()) {
      return root;
    }
    if (!root.isObject()) {
      return null;
    }
    JsonNode nested = firstNonNull(root, "AclList", "aclList");
    if (nested == null) {
      return null;
    }
    if (nested.isArray()) {
      return nested;
    }
    if (nested.isObject()) {
      JsonNode jaxb = firstNonNull(nested, "Acl", "acl");
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
