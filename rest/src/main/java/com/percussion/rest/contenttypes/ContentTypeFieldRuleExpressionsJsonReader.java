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
 * JSON reader for {@link ContentTypeFieldRuleExpressions} that accepts the Jackson root envelope
 * and a flat object.
 *
 * <p>Live CXF JAXB/Jettison rejects a flat {@code fieldName} body ({@code unexpected element
 * fieldName}) and UNWRAP_ROOT_VALUE can leave the four lists null so PUT returns 400 required.
 * Developer Content Type chrome (#3896) sends either:
 *
 * <ul>
 *   <li>{@code {"ContentTypeFieldRuleExpressions":{…}}} (preferred)
 *   <li>{@code {"fieldName":"sys_title","validation":[],…}} (flat)
 * </ul>
 *
 * <p>Must be listed on {@code rest-jax-rs} {@code jaxrs:providers} ahead of {@code jacksonProvider}.
 */
@Provider
@Consumes(MediaType.APPLICATION_JSON)
@Priority(Priorities.USER - 100)
@PSSiteManageBean("contentTypeFieldRuleExpressionsJsonReader")
public class ContentTypeFieldRuleExpressionsJsonReader
    implements MessageBodyReader<ContentTypeFieldRuleExpressions> {

  private static final JsonMapper MAPPER = JsonMapper.builder().build();

  @Override
  public boolean isReadable(
      Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
    return type != null && ContentTypeFieldRuleExpressions.class.isAssignableFrom(type);
  }

  @Override
  public ContentTypeFieldRuleExpressions readFrom(
      Class<ContentTypeFieldRuleExpressions> type,
      Type genericType,
      Annotation[] annotations,
      MediaType mediaType,
      MultivaluedMap<String, String> httpHeaders,
      InputStream entityStream)
      throws IOException {
    if (entityStream == null) {
      return new ContentTypeFieldRuleExpressions();
    }
    byte[] raw = entityStream.readAllBytes();
    if (raw.length == 0) {
      return new ContentTypeFieldRuleExpressions();
    }
    return parse(new String(raw, StandardCharsets.UTF_8));
  }

  /**
   * Bind PUT JSON. Empty / whitespace is an empty envelope (resource still requires the four
   * lists).
   *
   * @param json request body; may be null
   * @return non-null envelope
   */
  public static ContentTypeFieldRuleExpressions parse(String json) {
    if (json == null || json.isBlank()) {
      return new ContentTypeFieldRuleExpressions();
    }
    JsonNode root;
    try {
      root = MAPPER.readTree(json);
    } catch (JacksonException e) {
      throw new WebApplicationException(
          e.getMessage() != null ? e.getMessage() : "Invalid field rule expressions", 400);
    }
    if (root == null || root.isNull() || root.isMissingNode()) {
      return new ContentTypeFieldRuleExpressions();
    }
    if (!root.isObject()) {
      throw new WebApplicationException("Invalid field rule expressions", 400);
    }
    JsonNode nested =
        firstObject(root, "ContentTypeFieldRuleExpressions", "contentTypeFieldRuleExpressions");
    JsonNode fields = nested != null ? nested : root;
    try {
      ContentTypeFieldRuleExpressions out =
          MAPPER.treeToValue(fields, ContentTypeFieldRuleExpressions.class);
      return out != null ? out : new ContentTypeFieldRuleExpressions();
    } catch (JacksonException e) {
      throw new WebApplicationException(
          e.getMessage() != null ? e.getMessage() : "Invalid field rule expressions", 400);
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
