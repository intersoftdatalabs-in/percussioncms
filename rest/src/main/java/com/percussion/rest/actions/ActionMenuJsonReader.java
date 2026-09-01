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
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * JSON reader for Admin {@code POST /actions} and {@code PUT /actions/{id}} ({@link ActionMenu}).
 *
 * <p>CXF JAXB / {@code UNWRAP_ROOT_VALUE} can bind collection POST to {@link
 * AllowedWorkflowTransitionsRequest} ({@code allowedWorkflowTransitionsRequest}) when the
 * finder still shares the resource. The SPA posts {@code {"ActionMenu":{…}}} (#4123 / #4112).
 * This reader binds wrap and flat shapes before {@code jacksonProvider}.
 *
 * <p>Must be listed on {@code rest-jax-rs} {@code jaxrs:providers} ahead of {@code jacksonProvider}.
 */
@Provider
@Consumes({MediaType.APPLICATION_JSON, "text/json", "application/*+json", MediaType.WILDCARD})
@Priority(Priorities.USER - 100)
@PSSiteManageBean("actionMenuJsonReader")
public class ActionMenuJsonReader implements MessageBodyReader<ActionMenu> {

  /** Mapper without UNWRAP_ROOT_VALUE so wrap vs flat is inspected explicitly. */
  private static final JsonMapper MAPPER = JsonMapper.builder().build();

  /** No-op constructor. */
  public ActionMenuJsonReader() {}

  @Override
  public boolean isReadable(
      Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
    if (type == null || !ActionMenu.class.isAssignableFrom(type)) {
      return false;
    }
    if (ActionMenuList.class.isAssignableFrom(type)) {
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
  public ActionMenu readFrom(
      Class<ActionMenu> type,
      Type genericType,
      Annotation[] annotations,
      MediaType mediaType,
      MultivaluedMap<String, String> httpHeaders,
      InputStream entityStream)
      throws IOException {
    if (entityStream == null) {
      return new ActionMenu();
    }
    byte[] raw = entityStream.readAllBytes();
    if (raw.length == 0) {
      return new ActionMenu();
    }
    return parse(new String(raw, StandardCharsets.UTF_8));
  }

  /**
   * Bind create/update JSON. Empty / whitespace is an empty DTO (adaptor rejects blank name).
   *
   * @param json request body; may be null
   * @return non-null menu
   */
  public static ActionMenu parse(String json) {
    if (json == null || json.isBlank()) {
      return new ActionMenu();
    }
    JsonNode root;
    try {
      root = MAPPER.readTree(json);
    } catch (JacksonException e) {
      throw new WebApplicationException(
          e.getMessage() != null ? e.getMessage() : "Invalid ActionMenu", 400);
    }
    return parseNode(root);
  }

  /**
   * Bind a parsed JSON tree to {@link ActionMenu}.
   *
   * @param root parsed body; may be null
   * @return non-null menu
   */
  public static ActionMenu parseNode(JsonNode root) {
    if (root == null || root.isNull() || root.isMissingNode()) {
      return new ActionMenu();
    }
    if (!root.isObject()) {
      throw new WebApplicationException("ActionMenu body must be a JSON object", 400);
    }
    JsonNode nested = firstObject(root, "ActionMenu", "actionMenu");
    JsonNode fields = nested != null ? nested : root;
    ActionMenu out = new ActionMenu();
    out.setName(textField(fields, "name"));
    out.setLabel(textField(fields, "label"));
    out.setDescription(textField(fields, "description"));
    out.setMenuType(textField(fields, "menuType"));
    out.setUrl(textField(fields, "url"));
    out.setHandler(textField(fields, "handler"));
    Integer id = intField(fields, "id");
    if (id != null) {
      out.setId(id);
    }
    return out;
  }

  private static String textField(JsonNode node, String name) {
    JsonNode n = node.get(name);
    if (n == null || n.isNull() || n.isMissingNode()) {
      return null;
    }
    if (n.isObject() || n.isArray()) {
      return null;
    }
    String s = n.asString();
    return s != null && !s.isBlank() ? s : null;
  }

  private static Integer intField(JsonNode node, String name) {
    JsonNode n = node.get(name);
    if (n == null || n.isNull() || n.isMissingNode() || !n.isNumber()) {
      return null;
    }
    return n.asInt();
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
