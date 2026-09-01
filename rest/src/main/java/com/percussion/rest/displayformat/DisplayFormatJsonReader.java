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

package com.percussion.rest.displayformat;

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
 * JSON reader for {@link DisplayFormat} PUT/POST so an empty {@code allowedCommunities} array
 * stays empty (all communities) instead of becoming {@code null} (omit / unchanged) under CXF
 * {@code UNWRAP_ROOT_VALUE} (#4098).
 *
 * <p>Must be listed on {@code rest-jax-rs} {@code jaxrs:providers} ahead of {@code jacksonProvider}.
 */
@Provider
@Consumes(MediaType.APPLICATION_JSON)
@Priority(Priorities.USER - 100)
@PSSiteManageBean("displayFormatJsonReader")
public class DisplayFormatJsonReader implements MessageBodyReader<DisplayFormat> {

  /** Mapper without UNWRAP_ROOT_VALUE so the envelope is inspected explicitly. */
  private static final JsonMapper MAPPER = JsonMapper.builder().build();

  @Override
  public boolean isReadable(
      Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
    if (type == null || !DisplayFormat.class.isAssignableFrom(type)) {
      return false;
    }
    return mediaType == null || isJsonCompatible(mediaType);
  }

  @Override
  public DisplayFormat readFrom(
      Class<DisplayFormat> type,
      Type genericType,
      Annotation[] annotations,
      MediaType mediaType,
      MultivaluedMap<String, String> httpHeaders,
      InputStream entityStream)
      throws IOException {
    if (entityStream == null) {
      return new DisplayFormat();
    }
    byte[] raw = entityStream.readAllBytes();
    if (raw.length == 0) {
      return new DisplayFormat();
    }
    return parse(new String(raw, StandardCharsets.UTF_8));
  }

  /**
   * Bind DisplayFormat JSON. Empty / whitespace is an empty DTO. Present {@code
   * allowedCommunities: []} is an empty list (all communities), not {@code null}.
   *
   * @param json request body; may be null
   * @return non-null DTO
   */
  public static DisplayFormat parse(String json) {
    if (json == null || json.isBlank()) {
      return new DisplayFormat();
    }
    JsonNode root;
    try {
      root = MAPPER.readTree(json);
    } catch (JacksonException e) {
      throw new WebApplicationException(
          e.getMessage() != null ? e.getMessage() : "Invalid display format", 400);
    }
    if (root == null || root.isNull() || root.isMissingNode()) {
      return new DisplayFormat();
    }
    if (!root.isObject()) {
      throw new WebApplicationException("Invalid display format", 400);
    }
    JsonNode nested = firstObject(root, "DisplayFormat", "displayFormat");
    JsonNode fields = nested != null ? nested : root;
    DisplayFormat out;
    try {
      out = MAPPER.convertValue(fields, DisplayFormat.class);
    } catch (IllegalArgumentException e) {
      throw new WebApplicationException(
          e.getMessage() != null ? e.getMessage() : "Invalid display format", 400);
    }
    if (out == null) {
      out = new DisplayFormat();
    }
    if (fields.has("allowedCommunities") && !fields.get("allowedCommunities").isNull()) {
      out.setAllowedCommunities(communitiesFromNode(fields.get("allowedCommunities")));
    }
    return out;
  }

  static List<DisplayFormatCommunity> communitiesFromNode(JsonNode raw) {
    if (raw == null || raw.isNull() || raw.isMissingNode()) {
      return new ArrayList<>();
    }
    List<DisplayFormatCommunity> out = new ArrayList<>();
    if (raw.isArray()) {
      for (JsonNode n : raw) {
        DisplayFormatCommunity row = communityFromNode(n);
        if (row != null) {
          out.add(row);
        }
      }
      return out;
    }
    if (raw.isObject()) {
      JsonNode wrapped = raw.get("DisplayFormatCommunity");
      if (wrapped == null) {
        wrapped = raw.get("displayFormatCommunity");
      }
      if (wrapped != null) {
        return communitiesFromNode(wrapped);
      }
      DisplayFormatCommunity row = communityFromNode(raw);
      if (row != null) {
        out.add(row);
      }
    }
    return out;
  }

  private static DisplayFormatCommunity communityFromNode(JsonNode n) {
    if (n == null || n.isNull() || !n.isObject()) {
      return null;
    }
    String guid = textField(n, "guid");
    String name = textField(n, "name");
    if ((guid == null || guid.isBlank()) && (name == null || name.isBlank())) {
      return null;
    }
    return new DisplayFormatCommunity(guid, name);
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
