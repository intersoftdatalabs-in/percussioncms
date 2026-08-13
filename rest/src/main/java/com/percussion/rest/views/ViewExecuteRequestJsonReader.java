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

package com.percussion.rest.views;

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
 * JSON reader for {@link ViewExecuteRequest} that accepts both the JAXB / Jackson
 * root envelope and a flat paging object.
 *
 * <p>CXF {@code UNWRAP_ROOT_VALUE} / JAXB rejects a bare {@code startIndex} field
 * ({@code unexpected element startIndex}, expected {@code ViewExecuteRequest}).
 * Explorer Inbox (#3323) historically POSTed that flat shape. This reader binds
 * either:
 *
 * <ul>
 *   <li>{@code {"ViewExecuteRequest":{"startIndex":1,"maxResults":50}}} (preferred)
 *   <li>{@code {"startIndex":1,"maxResults":50}} (Inbox / QA residual)
 * </ul>
 *
 * <p>Must be listed on {@code rest-jax-rs} {@code jaxrs:providers} ahead of
 * {@code jacksonProvider} so it wins over UNWRAP_ROOT_VALUE.
 */
@Provider
@Consumes(MediaType.APPLICATION_JSON)
@Priority(Priorities.USER - 100)
@PSSiteManageBean("viewExecuteRequestJsonReader")
public class ViewExecuteRequestJsonReader implements MessageBodyReader<ViewExecuteRequest> {

  /** Mapper without UNWRAP_ROOT_VALUE so a flat startIndex object is readable. */
  private static final JsonMapper MAPPER = JsonMapper.builder().build();

  @Override
  public boolean isReadable(
      Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
    return type != null && ViewExecuteRequest.class.isAssignableFrom(type);
  }

  @Override
  public ViewExecuteRequest readFrom(
      Class<ViewExecuteRequest> type,
      Type genericType,
      Annotation[] annotations,
      MediaType mediaType,
      MultivaluedMap<String, String> httpHeaders,
      InputStream entityStream)
      throws IOException {
    if (entityStream == null) {
      return new ViewExecuteRequest();
    }
    byte[] raw = entityStream.readAllBytes();
    if (raw.length == 0) {
      return new ViewExecuteRequest();
    }
    return parse(new String(raw, StandardCharsets.UTF_8));
  }

  /**
   * Bind execute JSON. Empty / whitespace is an empty request (design defaults).
   *
   * @param json request body; may be null
   * @return non-null request
   */
  public static ViewExecuteRequest parse(String json) {
    if (json == null || json.isBlank()) {
      return new ViewExecuteRequest();
    }
    JsonNode root;
    try {
      root = MAPPER.readTree(json);
    } catch (JacksonException e) {
      throw new WebApplicationException(
          e.getMessage() != null ? e.getMessage() : "Invalid execute request", 400);
    }
    if (root == null || root.isNull() || root.isMissingNode()) {
      return new ViewExecuteRequest();
    }
    if (!root.isObject()) {
      throw new WebApplicationException("Invalid execute request", 400);
    }
    JsonNode nested = firstObject(root, "ViewExecuteRequest", "viewExecuteRequest");
    JsonNode fields = nested != null ? nested : root;
    ViewExecuteRequest out = new ViewExecuteRequest();
    out.setFolderPath(textField(fields, "folderPath"));
    out.setStartIndex(intField(fields, "startIndex"));
    out.setMaxResults(intField(fields, "maxResults"));
    out.setSortColumn(textField(fields, "sortColumn"));
    out.setSortOrder(textField(fields, "sortOrder"));
    return out;
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
    if (n == null || n.isNull() || !n.isString()) {
      return null;
    }
    String v = n.asString();
    return v != null && !v.isBlank() ? v : null;
  }

  private static Integer intField(JsonNode node, String name) {
    JsonNode n = node.get(name);
    if (n == null || n.isNull() || !n.isNumber()) {
      return null;
    }
    return n.intValue();
  }
}
