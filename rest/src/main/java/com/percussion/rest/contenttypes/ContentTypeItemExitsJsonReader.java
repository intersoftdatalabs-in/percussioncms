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

package com.percussion.rest.contenttypes;

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
 * JSON reader for PUT {@code /contenttypes/{id}/itemExits} (CD-09 / #3895).
 *
 * <p>Live H2 CXF {@code UNWRAP_ROOT_VALUE} can deliver an empty {@link ContentTypeItemExits}
 * (null required lists → 400) for the documented wrap {@code {"ContentTypeItemExits":{…}}}. This
 * reader binds either envelope with a mapper that does <em>not</em> unwrap:
 *
 * <ul>
 *   <li>{@code {"ContentTypeItemExits":{"inputTranslations":[],…}}} (SPA wrap)
 *   <li>{@code {"inputTranslations":[],"outputTranslations":[],"validations":[]}} (flat)
 * </ul>
 *
 * <p>Must be listed on {@code rest-jax-rs} {@code jaxrs:providers} ahead of {@code jacksonProvider}.
 */
@Provider
@Consumes({MediaType.APPLICATION_JSON, "text/json", "application/*+json", MediaType.WILDCARD})
@Priority(Priorities.USER - 100)
@PSSiteManageBean("contentTypeItemExitsJsonReader")
public class ContentTypeItemExitsJsonReader implements MessageBodyReader<ContentTypeItemExits> {

  /** Mapper without UNWRAP_ROOT_VALUE so a flat or nested object is readable. */
  private static final JsonMapper MAPPER = JsonMapper.builder().build();

  /** No-arg ctor for Spring / JAX-RS. */
  public ContentTypeItemExitsJsonReader() {}

  @Override
  public boolean isReadable(
      Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
    if (type == null || !ContentTypeItemExits.class.isAssignableFrom(type)) {
      return false;
    }
    return mediaType == null || isJsonCompatible(mediaType);
  }

  @Override
  public ContentTypeItemExits readFrom(
      Class<ContentTypeItemExits> type,
      Type genericType,
      Annotation[] annotations,
      MediaType mediaType,
      MultivaluedMap<String, String> httpHeaders,
      InputStream entityStream)
      throws IOException {
    if (entityStream == null) {
      return new ContentTypeItemExits();
    }
    byte[] raw = entityStream.readAllBytes();
    if (raw.length == 0) {
      return new ContentTypeItemExits();
    }
    return parse(new String(raw, StandardCharsets.UTF_8));
  }

  /**
   * Bind item-exits JSON. Empty / whitespace is an empty envelope (resource still 400s missing
   * required lists).
   */
  public static ContentTypeItemExits parse(String json) {
    if (json == null || json.isBlank()) {
      return new ContentTypeItemExits();
    }
    JsonNode root;
    try {
      root = MAPPER.readTree(json);
    } catch (JacksonException e) {
      throw new WebApplicationException(
          e.getMessage() != null ? e.getMessage() : "Invalid item-exits body", 400);
    }
    if (root == null || root.isNull() || root.isMissingNode()) {
      return new ContentTypeItemExits();
    }
    if (!root.isObject()) {
      throw new WebApplicationException("Invalid item-exits body", 400);
    }
    JsonNode nested = firstObject(root, "ContentTypeItemExits", "contentTypeItemExits");
    JsonNode fields = nested != null ? nested : root;
    try {
      return MAPPER.treeToValue(fields, ContentTypeItemExits.class);
    } catch (JacksonException e) {
      throw new WebApplicationException(
          e.getMessage() != null ? e.getMessage() : "Invalid item-exits body", 400);
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
}
