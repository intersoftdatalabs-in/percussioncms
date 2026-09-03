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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * JSON reader for Admin {@code PUT /actions/{idOrName}/children} ({@link ActionMenuList}).
 *
 * <p>CXF JAXB / {@code UNWRAP_ROOT_VALUE} rejects a bare array and can drop {@code
 * ActionMenuList} ArrayList subclasses. This reader binds wrap, {@code children}, and flat
 * arrays before {@code jacksonProvider}.
 *
 * <p>Must be listed on {@code rest-jax-rs} {@code jaxrs:providers} ahead of {@code jacksonProvider}.
 */
@Provider
@Consumes({MediaType.APPLICATION_JSON, "text/json", "application/*+json", MediaType.WILDCARD})
@Priority(Priorities.USER - 100)
@PSSiteManageBean("actionMenuListJsonReader")
public class ActionMenuListJsonReader implements MessageBodyReader<ActionMenuList> {

  /** Mapper without UNWRAP_ROOT_VALUE so wrap vs array is inspected explicitly. */
  private static final JsonMapper MAPPER = JsonMapper.builder().build();

  /** Logger for malformed JSON (details stay off the 400 body). */
  private static final Logger log = LogManager.getLogger(ActionMenuListJsonReader.class);

  /** Client 400 for malformed JSON; Jackson parse details stay server-side. */
  static final String INVALID_JSON = "Invalid ActionMenuList JSON";

  /** No-op constructor. */
  public ActionMenuListJsonReader() {}

  @Override
  public boolean isReadable(
      Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
    if (type == null || !ActionMenuList.class.isAssignableFrom(type)) {
      return false;
    }
    return mediaType == null || ActionMenuJsonReader.isJsonCompatible(mediaType);
  }

  @Override
  public ActionMenuList readFrom(
      Class<ActionMenuList> type,
      Type genericType,
      Annotation[] annotations,
      MediaType mediaType,
      MultivaluedMap<String, String> httpHeaders,
      InputStream entityStream)
      throws IOException {
    if (entityStream == null) {
      return new ActionMenuList();
    }
    byte[] raw = entityStream.readAllBytes();
    if (raw.length == 0) {
      return new ActionMenuList();
    }
    return parse(new String(raw, StandardCharsets.UTF_8));
  }

  /**
   * Bind children PUT JSON. Empty / whitespace is an empty list (clears associations).
   *
   * @param json request body; may be null
   * @return non-null list
   */
  public static ActionMenuList parse(String json) {
    if (json == null || json.isBlank()) {
      return new ActionMenuList();
    }
    JsonNode root;
    try {
      root = MAPPER.readTree(json);
    } catch (JacksonException e) {
      log.debug(INVALID_JSON, e);
      throw new WebApplicationException(INVALID_JSON, 400);
    }
    return parseNode(root);
  }

  /**
   * Bind a parsed JSON tree to {@link ActionMenuList}.
   *
   * @param root parsed body; may be null
   * @return non-null list
   */
  public static ActionMenuList parseNode(JsonNode root) {
    if (root == null || root.isNull() || root.isMissingNode()) {
      return new ActionMenuList();
    }
    if (root.isArray()) {
      return ActionMenuJsonReader.childrenField(root);
    }
    if (!root.isObject()) {
      throw new WebApplicationException("ActionMenuList body must be a JSON array or object", 400);
    }
    JsonNode nested = firstArray(root, "ActionMenuList", "actionMenuList", "children");
    if (nested != null) {
      return ActionMenuJsonReader.childrenField(nested);
    }
    JsonNode menu = root.get("ActionMenu");
    if (menu != null && menu.isObject()) {
      ActionMenuList fromMenu = ActionMenuJsonReader.childrenField(menu.get("children"));
      return fromMenu != null ? fromMenu : new ActionMenuList();
    }
    ActionMenuList fromFlat = ActionMenuJsonReader.childrenField(root.get("children"));
    return fromFlat != null ? fromFlat : new ActionMenuList();
  }

  private static JsonNode firstArray(JsonNode root, String... names) {
    for (String name : names) {
      JsonNode n = root.get(name);
      if (n != null && n.isArray()) {
        return n;
      }
    }
    return null;
  }
}
