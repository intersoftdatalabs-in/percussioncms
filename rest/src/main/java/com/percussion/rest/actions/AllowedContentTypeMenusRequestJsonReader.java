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

package com.percussion.rest.actions;

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

/**
 * JSON reader for {@code POST /actions/find/types}.
 *
 * <p>CXF {@code UNWRAP_ROOT_VALUE} rejects a bare {@code contentIds} field (expected {@code
 * AllowedContentTypeMenusRequest}). Explorer posts that flat shape, and display-format rows may
 * send GUID strings such as {@code 16777215-101-551} instead of ints (#3855 / parent #3716). This
 * reader binds either:
 *
 * <ul>
 *   <li>{@code {"AllowedContentTypeMenusRequest":{"contentIds":[551]}}} (preferred)
 *   <li>{@code {"contentIds":[551]}} (live SPA / QA residual)
 *   <li>GUID last-segment or Jackson {@code {stringValue}} tokens in {@code contentIds}
 * </ul>
 *
 * <p>Must be listed on {@code rest-jax-rs} {@code jaxrs:providers} ahead of {@code jacksonProvider}
 * so it wins over JAXB / UNWRAP_ROOT_VALUE.
 */
@Provider
@Consumes({MediaType.APPLICATION_JSON, "text/json", "application/*+json", MediaType.WILDCARD})
@Priority(Priorities.USER - 100)
@PSSiteManageBean("allowedContentTypeMenusRequestJsonReader")
public class AllowedContentTypeMenusRequestJsonReader
    implements MessageBodyReader<AllowedContentTypeMenusRequest> {

  /** Mapper without UNWRAP_ROOT_VALUE so a flat contentIds object is readable. */
  private static final JsonMapper MAPPER = JsonMapper.builder().build();

  /** No-op constructor. */
  public AllowedContentTypeMenusRequestJsonReader() {}

  /** CMS content ids are signed 32-bit locators. */
  private static final int MAX_CMS_CONTENT_ID = Integer.MAX_VALUE;

  @Override
  public boolean isReadable(
      Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
    if (type == null || !AllowedContentTypeMenusRequest.class.isAssignableFrom(type)) {
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
  public AllowedContentTypeMenusRequest readFrom(
      Class<AllowedContentTypeMenusRequest> type,
      Type genericType,
      Annotation[] annotations,
      MediaType mediaType,
      MultivaluedMap<String, String> httpHeaders,
      InputStream entityStream)
      throws IOException {
    if (entityStream == null) {
      return new AllowedContentTypeMenusRequest();
    }
    byte[] raw = entityStream.readAllBytes();
    if (raw.length == 0) {
      return new AllowedContentTypeMenusRequest();
    }
    return parse(new String(raw, StandardCharsets.UTF_8));
  }

  /**
   * Bind find/types JSON. Empty / whitespace is an empty request (resource treats missing ids as
   * an empty catalog, HTTP 200).
   *
   * @param json request body; may be null
   * @return non-null request
   */
  public static AllowedContentTypeMenusRequest parse(String json) {
    if (json == null || json.isBlank()) {
      return new AllowedContentTypeMenusRequest();
    }
    JsonNode root;
    try {
      root = MAPPER.readTree(json);
    } catch (JacksonException e) {
      throw new WebApplicationException(
          e.getMessage() != null ? e.getMessage() : "Invalid AllowedContentTypeMenusRequest",
          400);
    }
    return parseNode(root);
  }

  /**
   * Bind a parsed JSON tree to {@link AllowedContentTypeMenusRequest}.
   *
   * @param root parsed body; may be null
   * @return non-null request
   */
  public static AllowedContentTypeMenusRequest parseNode(JsonNode root) {
    if (root == null || root.isNull() || root.isMissingNode()) {
      return new AllowedContentTypeMenusRequest();
    }
    if (!root.isObject()) {
      throw new WebApplicationException(
          "AllowedContentTypeMenusRequest body must be a JSON object", 400);
    }
    JsonNode nested =
        firstObject(root, "AllowedContentTypeMenusRequest", "allowedContentTypeMenusRequest");
    JsonNode fields = nested != null ? nested : root;
    AllowedContentTypeMenusRequest out = new AllowedContentTypeMenusRequest();
    JsonNode idsNode = fields.get("contentIds");
    if (idsNode == null) {
      idsNode = fields.get("ContentIds");
    }
    out.setContentIds(parseContentIds(idsNode));
    return out;
  }

  /**
   * Coerce {@code contentIds} JSON to positive ints. Accepts ints, numeric strings, GUID {@code
   * host-type-uuid} last segments, and Jackson GUID objects.
   *
   * @param node array, scalar, or null
   * @return never null; invalid tokens are skipped
   */
  static int[] parseContentIds(JsonNode node) {
    if (node == null || node.isNull() || node.isMissingNode()) {
      return new int[0];
    }
    if (!node.isArray()) {
      int one = parseContentIdToken(node);
      return one > 0 ? new int[] {one} : new int[0];
    }
    List<Integer> ids = new ArrayList<>();
    for (JsonNode el : node) {
      int id = parseContentIdToken(el);
      if (id > 0) {
        ids.add(id);
      }
    }
    int[] out = new int[ids.size()];
    for (int i = 0; i < ids.size(); i++) {
      out[i] = ids.get(i);
    }
    return out;
  }

  static int parseContentIdToken(JsonNode node) {
    if (node == null || node.isNull() || node.isMissingNode()) {
      return 0;
    }
    if (node.isNumber() || node.isString() || node.isBoolean()) {
      return parseContentIdString(node.asString());
    }
    if (node.isObject()) {
      JsonNode stringValue = node.get("stringValue");
      if (stringValue == null) {
        stringValue = node.get("string_value");
      }
      if (stringValue != null && !stringValue.isNull()) {
        int fromSv = parseContentIdString(stringValue.asString());
        if (fromSv > 0) {
          return fromSv;
        }
      }
      JsonNode uuid = node.get("uuid");
      if (uuid != null && !uuid.isNull()) {
        return parseContentIdString(uuid.asString());
      }
    }
    return 0;
  }

  /**
   * Parse a numeric content id or {@code host-type-uuid} last segment.
   *
   * @param raw token; may be null
   * @return positive content id, or {@code 0} if unusable
   */
  static int parseContentIdString(String raw) {
    if (raw == null) {
      return 0;
    }
    String s = raw.trim();
    if (s.isEmpty()) {
      return 0;
    }
    if (isAllDigits(s)) {
      return asPositiveInt(s);
    }
    String[] parts = s.split("-");
    if (parts.length == 3
        && isAllDigits(parts[0])
        && isAllDigits(parts[1])
        && isAllDigits(parts[2])) {
      return asPositiveInt(parts[2]);
    }
    return 0;
  }

  private static boolean isAllDigits(String s) {
    if (s == null || s.isEmpty()) {
      return false;
    }
    for (int i = 0; i < s.length(); i++) {
      if (!Character.isDigit(s.charAt(i))) {
        return false;
      }
    }
    return true;
  }

  private static int asPositiveInt(String digits) {
    try {
      int n = Integer.parseInt(digits);
      return n > 0 && n <= MAX_CMS_CONTENT_ID ? n : 0;
    } catch (NumberFormatException e) {
      return 0;
    }
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
}
