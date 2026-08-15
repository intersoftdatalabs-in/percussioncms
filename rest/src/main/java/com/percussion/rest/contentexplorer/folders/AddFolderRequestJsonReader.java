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

package com.percussion.rest.contentexplorer.folders;

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
 * JSON reader for {@code POST /content-explorer/folders}.
 *
 * <p>CXF JAXB / {@code UNWRAP_ROOT_VALUE} rejects a bare {@code name} field ({@code unexpected
 * element local:"name"}, expected {@code AddFolderRequest}). Explorer folder create with {@code
 * rxFolderMutations=1} (#3360 / #3361) historically POSTed that flat shape. This reader binds
 * either:
 *
 * <ul>
 *   <li>{@code {"AddFolderRequest":{"name":"…","parentPath":"…","sourcePath":"…"}}} (preferred)
 *   <li>{@code {"name":"…","parentPath":"…"}} (live SPA / QA residual)
 * </ul>
 *
 * <p>Must be listed on {@code rest-jax-rs} {@code jaxrs:providers} ahead of {@code jacksonProvider}
 * so it wins over JAXB / UNWRAP_ROOT_VALUE.
 */
@Provider
@Consumes({MediaType.APPLICATION_JSON, "text/json", "application/*+json", MediaType.WILDCARD})
@Priority(Priorities.USER - 100)
@PSSiteManageBean("addFolderRequestJsonReader")
public class AddFolderRequestJsonReader implements MessageBodyReader<AddFolderRequest> {

  /** Mapper without UNWRAP_ROOT_VALUE so a flat name/parentPath object is readable. */
  private static final JsonMapper MAPPER = JsonMapper.builder().build();

  @Override
  public boolean isReadable(
      Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
    if (type == null || !AddFolderRequest.class.isAssignableFrom(type)) {
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
  public AddFolderRequest readFrom(
      Class<AddFolderRequest> type,
      Type genericType,
      Annotation[] annotations,
      MediaType mediaType,
      MultivaluedMap<String, String> httpHeaders,
      InputStream entityStream)
      throws IOException {
    if (entityStream == null) {
      return new AddFolderRequest();
    }
    byte[] raw = entityStream.readAllBytes();
    if (raw.length == 0) {
      return new AddFolderRequest();
    }
    return parse(new String(raw, StandardCharsets.UTF_8));
  }

  /**
   * Bind add-folder JSON. Empty / whitespace is an empty request (resource still 400s on missing
   * fields in the adaptor).
   *
   * @param json request body; may be null
   * @return non-null request
   */
  public static AddFolderRequest parse(String json) {
    if (json == null || json.isBlank()) {
      return new AddFolderRequest();
    }
    JsonNode root;
    try {
      root = MAPPER.readTree(json);
    } catch (JacksonException e) {
      throw new WebApplicationException(
          e.getMessage() != null ? e.getMessage() : "Invalid AddFolderRequest", 400);
    }
    return parseNode(root);
  }

  /**
   * Bind a parsed JSON tree to {@link AddFolderRequest}.
   *
   * @param root parsed body; may be null
   * @return non-null request
   */
  public static AddFolderRequest parseNode(JsonNode root) {
    if (root == null || root.isNull() || root.isMissingNode()) {
      return new AddFolderRequest();
    }
    if (!root.isObject()) {
      throw new WebApplicationException("AddFolderRequest body must be a JSON object", 400);
    }
    JsonNode nested = firstObject(root, "AddFolderRequest", "addFolderRequest");
    JsonNode fields = nested != null ? nested : root;
    AddFolderRequest out = new AddFolderRequest();
    out.setName(textField(fields, "name"));
    out.setParentPath(textField(fields, "parentPath"));
    out.setSourcePath(textField(fields, "sourcePath"));
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
    if (n == null || n.isNull()) {
      return null;
    }
    if (n.isString() || n.isNumber() || n.isBoolean()) {
      String v = n.asString();
      return v != null && !v.isBlank() ? v : null;
    }
    return null;
  }
}
