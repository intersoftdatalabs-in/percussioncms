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

package com.percussion.rest.locales;

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
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * JSON reader for PUT {@code /locales/auto-translations} (CD-18 / #4028).
 *
 * <p>CXF {@code UNWRAP_ROOT_VALUE} rejects a bare JSON array ({@code JSONObject text must begin
 * with '{'}) and can bind {@code {"AutoTranslationRow":[…]}} as an empty list. This reader binds
 * both envelopes with a mapper that does <em>not</em> unwrap:
 *
 * <ul>
 *   <li>{@code []} (documented empty-clear)
 *   <li>{@code {"AutoTranslationRow":[]}}
 *   <li>{@code {"AutoTranslationRow":[{…},{…}]}}
 *   <li>{@code {"AutoTranslationRow":{…}}} (single row)
 * </ul>
 *
 * <p>Must be listed on {@code rest-jax-rs} {@code jaxrs:providers} ahead of {@code jacksonProvider}.
 */
@Provider
@Consumes({MediaType.APPLICATION_JSON, "text/json", "application/*+json", MediaType.WILDCARD})
@Priority(Priorities.USER - 100)
@PSSiteManageBean("autoTranslationRowsJsonReader")
public class AutoTranslationRowsJsonReader implements MessageBodyReader<List<AutoTranslationRow>> {

  private static final JsonMapper MAPPER = JsonMapper.builder().build();

  public AutoTranslationRowsJsonReader() {}

  @Override
  public boolean isReadable(
      Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
    if (!isAutoTranslationRowList(type, genericType)) {
      return false;
    }
    return mediaType == null || isJsonCompatible(mediaType);
  }

  @Override
  public List<AutoTranslationRow> readFrom(
      Class<List<AutoTranslationRow>> type,
      Type genericType,
      Annotation[] annotations,
      MediaType mediaType,
      MultivaluedMap<String, String> httpHeaders,
      InputStream entityStream)
      throws IOException {
    if (entityStream == null) {
      return List.of();
    }
    byte[] raw = entityStream.readAllBytes();
    if (raw.length == 0) {
      return List.of();
    }
    return parse(new String(raw, StandardCharsets.UTF_8));
  }

  /** Bind auto-translation PUT JSON. Empty / whitespace is an empty list (clears the set). */
  public static List<AutoTranslationRow> parse(String json) {
    if (json == null || json.isBlank()) {
      return List.of();
    }
    JsonNode root;
    try {
      root = MAPPER.readTree(json);
    } catch (JacksonException e) {
      throw new WebApplicationException(
          e.getMessage() != null ? e.getMessage() : "Invalid auto-translations body", 400);
    }
    if (root == null || root.isNull() || root.isMissingNode()) {
      return List.of();
    }
    if (root.isArray()) {
      return rowsFromArray(root);
    }
    if (!root.isObject()) {
      throw new WebApplicationException("Invalid auto-translations body", 400);
    }
    JsonNode nested =
        firstPresent(
            root, "AutoTranslationRow", "autoTranslationRow", "AutoTranslations", "autoTranslations");
    if (nested != null) {
      if (nested.isArray()) {
        return rowsFromArray(nested);
      }
      if (nested.isObject()) {
        AutoTranslationRow one = rowFromObject(nested);
        return one == null ? List.of() : List.of(one);
      }
      if (nested.isNull()) {
        return List.of();
      }
    }
    AutoTranslationRow one = rowFromObject(root);
    if (one != null) {
      return List.of(one);
    }
    return List.of();
  }

  static boolean isAutoTranslationRowList(Class<?> type, Type genericType) {
    if (type == null || !List.class.isAssignableFrom(type)) {
      return false;
    }
    if (genericType instanceof ParameterizedType pt) {
      Type[] args = pt.getActualTypeArguments();
      return args.length == 1 && args[0] == AutoTranslationRow.class;
    }
    return false;
  }

  private static List<AutoTranslationRow> rowsFromArray(JsonNode array) {
    List<AutoTranslationRow> out = new ArrayList<>();
    for (JsonNode n : array) {
      if (n == null || n.isNull() || !n.isObject()) {
        continue;
      }
      AutoTranslationRow row = rowFromObject(n);
      if (row != null) {
        out.add(row);
      }
    }
    return out;
  }

  private static AutoTranslationRow rowFromObject(JsonNode node) {
    if (node == null || !node.isObject()) {
      return null;
    }
    try {
      return MAPPER.treeToValue(node, AutoTranslationRow.class);
    } catch (JacksonException e) {
      throw new WebApplicationException(
          e.getMessage() != null ? e.getMessage() : "Invalid auto-translation row", 400);
    }
  }

  private static JsonNode firstPresent(JsonNode root, String... names) {
    for (String name : names) {
      JsonNode n = root.get(name);
      if (n != null && !n.isMissingNode()) {
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
