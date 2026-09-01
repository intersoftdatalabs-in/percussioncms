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
 * JSON reader for PUT {@code /services/communities/bulk}.
 *
 * <p>CXF {@code UNWRAP_ROOT_VALUE} plus {@link CommunityList} extending {@link
 * java.util.ArrayList} yields {@code ClassCastException: Cannot cast java.util.ArrayList to
 * CommunityList} (HTTP 400). A bare JSON array is rejected by Jettison/org.json ({@code
 * JSONObject} must begin with '{'). Peer: {@code AclListJsonReader} (#3391 / #3378).
 *
 * <p>Binds {@code {"CommunityList":[{…}]}}, {@code [{…}]}, or JAXB {@code
 * {"CommunityList":{"Community":[…]}}}.
 */
@Provider
@Consumes({MediaType.APPLICATION_JSON, "text/json", "application/*+json", MediaType.WILDCARD})
@Priority(Priorities.USER - 100)
@PSSiteManageBean("communityListJsonReader")
public class CommunityListJsonReader implements MessageBodyReader<CommunityList> {

  private static final JsonMapper MAPPER = JsonMapper.builder().build();

  @Override
  public boolean isReadable(
      Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
    if (type == null || !CommunityList.class.isAssignableFrom(type)) {
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
  public CommunityList readFrom(
      Class<CommunityList> type,
      Type genericType,
      Annotation[] annotations,
      MediaType mediaType,
      MultivaluedMap<String, String> httpHeaders,
      InputStream entityStream)
      throws IOException {
    if (entityStream == null) {
      return new CommunityList();
    }
    byte[] raw = entityStream.readAllBytes();
    if (raw.length == 0) {
      return new CommunityList();
    }
    return parse(new String(raw, StandardCharsets.UTF_8));
  }

  public static CommunityList parse(String json) {
    if (json == null || json.isBlank()) {
      return new CommunityList();
    }
    JsonNode root;
    try {
      root = MAPPER.readTree(json);
    } catch (JacksonException e) {
      throw new WebApplicationException(
          e.getMessage() != null ? e.getMessage() : "Invalid CommunityList", 400);
    }
    return parseNode(root);
  }

  public static CommunityList parseNode(JsonNode root) {
    if (root == null || root.isNull() || root.isMissingNode()) {
      return new CommunityList();
    }
    JsonNode array = extractCommunityArray(root);
    if (array == null || !array.isArray()) {
      throw new WebApplicationException(
          "CommunityList body must be {\"CommunityList\":[…]} or a JSON array", 400);
    }
    CommunityList list = new CommunityList();
    for (JsonNode item : array) {
      if (item == null || item.isNull()) {
        continue;
      }
      JsonNode body = item;
      if (item.isObject() && item.get("Community") != null && item.get("Community").isObject()) {
        body = item.get("Community");
      }
      try {
        list.add(MAPPER.treeToValue(body, Community.class));
      } catch (JacksonException e) {
        throw new WebApplicationException(
            e.getMessage() != null ? e.getMessage() : "Invalid Community", 400);
      }
    }
    return list;
  }

  private static JsonNode extractCommunityArray(JsonNode root) {
    if (root.isArray()) {
      return root;
    }
    if (!root.isObject()) {
      return null;
    }
    JsonNode nested = firstNonNull(root, "CommunityList", "communityList");
    if (nested == null) {
      return null;
    }
    if (nested.isArray()) {
      return nested;
    }
    if (nested.isObject()) {
      JsonNode jaxb = firstNonNull(nested, "Community", "community");
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
