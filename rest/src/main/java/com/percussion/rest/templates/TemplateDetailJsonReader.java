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

package com.percussion.rest.templates;

import com.percussion.rest.Guid;
import com.percussion.rest.contenttypes.NamedObjectRef;
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
import java.util.function.Function;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

/**
 * JSON reader for {@link TemplateDetail} PUT so {@code associatedContentTypes: []} stays an empty
 * list (clear) instead of {@code null} (omit / unchanged) under CXF {@code UNWRAP_ROOT_VALUE}
 * (#4465).
 *
 * <p>Must be listed on {@code rest-jax-rs} {@code jaxrs:providers} ahead of {@code jacksonProvider}.
 */
@Provider
@Consumes(MediaType.APPLICATION_JSON)
@Priority(Priorities.USER - 100)
@PSSiteManageBean("templateDetailJsonReader")
public class TemplateDetailJsonReader implements MessageBodyReader<TemplateDetail> {

  /** Mapper without UNWRAP_ROOT_VALUE so the envelope is inspected explicitly. */
  private static final JsonMapper MAPPER = JsonMapper.builder().build();

  @Override
  public boolean isReadable(
      Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
    if (type == null || !TemplateDetail.class.isAssignableFrom(type)) {
      return false;
    }
    return mediaType == null || isJsonCompatible(mediaType);
  }

  @Override
  public TemplateDetail readFrom(
      Class<TemplateDetail> type,
      Type genericType,
      Annotation[] annotations,
      MediaType mediaType,
      MultivaluedMap<String, String> httpHeaders,
      InputStream entityStream)
      throws IOException {
    if (entityStream == null) {
      return new TemplateDetail();
    }
    byte[] raw = entityStream.readAllBytes();
    if (raw.length == 0) {
      return new TemplateDetail();
    }
    return parse(new String(raw, StandardCharsets.UTF_8));
  }

  /**
   * Bind TemplateDetail JSON. Present {@code associatedContentTypes: []} is an empty list (clear),
   * not {@code null} (omit).
   *
   * @param json request body; may be null
   * @return non-null DTO
   */
  public static TemplateDetail parse(String json) {
    if (json == null || json.isBlank()) {
      return new TemplateDetail();
    }
    JsonNode root = readRoot(json);
    if (root == null) {
      return new TemplateDetail();
    }
    JsonNode fields = resolveFields(root);
    TemplateDetail out = convertDetail(stripListSections(fields));
    applyAssociationLists(out, fields);
    return out;
  }

  /** Parses the body and validates the envelope is a JSON object; null for JSON null. */
  private static JsonNode readRoot(String json) {
    JsonNode root;
    try {
      root = MAPPER.readTree(json);
    } catch (JacksonException e) {
      throw badRequest(e.getMessage());
    }
    if (root == null || root.isNull() || root.isMissingNode()) {
      return null;
    }
    if (!root.isObject()) {
      throw badRequest(null);
    }
    return root;
  }

  /** Unwraps the {@code TemplateDetail} envelope when present, else the root itself. */
  private static JsonNode resolveFields(JsonNode root) {
    JsonNode nested = firstObject(root, "TemplateDetail", "templateDetail");
    return nested != null ? nested : root;
  }

  /**
   * Copies the fields for DTO binding with the replace-semantics lists and read-only output
   * stripped, so an omitted property stays {@code null} (leave unchanged).
   */
  private static JsonNode stripListSections(JsonNode fields) {
    if (!fields.isObject()) {
      return fields;
    }
    ObjectNode copy = (ObjectNode) fields.deepCopy();
    // Defaults on these lists are "replace". Strip them so an omitted property stays null.
    copy.remove("associatedContentTypes");
    copy.remove("bindings");
    copy.remove("slots");
    // Read-only output: PUT never flows sites (adaptor ignores it too).
    copy.remove("associatedSites");
    return copy;
  }

  private static TemplateDetail convertDetail(JsonNode fieldsForDto) {
    TemplateDetail out;
    try {
      out = MAPPER.convertValue(fieldsForDto, TemplateDetail.class);
    } catch (IllegalArgumentException e) {
      throw badRequest(e.getMessage());
    }
    return out != null ? out : new TemplateDetail();
  }

  private static void applyAssociationLists(TemplateDetail out, JsonNode fields) {
    if (fields.has("associatedContentTypes") && !fields.get("associatedContentTypes").isNull()) {
      out.setAssociatedContentTypes(refsFromNode(fields.get("associatedContentTypes")));
    }
    out.setBindings(optionalList(fields, "bindings", TemplateDetailJsonReader::bindingsFromNode));
    out.setSlots(optionalList(fields, "slots", TemplateDetailJsonReader::slotsFromNode));
  }

  private static WebApplicationException badRequest(String message) {
    return new WebApplicationException(
        message != null ? message : "Invalid template detail", 400);
  }

  /**
   * {@code null} when the property is omitted or JSON null (leave unchanged). A present value is
   * the replacement list, including empty.
   */
  private static <T> List<T> optionalList(
      JsonNode fields, String name, java.util.function.Function<JsonNode, List<T>> parse) {
    if (fields == null || !fields.has(name) || fields.get(name).isNull()) {
      return null;
    }
    return parse.apply(fields.get(name));
  }

  static List<TemplateBindingSummary> bindingsFromNode(JsonNode raw) {
    return collectList(raw, "TemplateBinding", "Binding", TemplateDetailJsonReader::bindingFromNode);
  }

  private static TemplateBindingSummary bindingFromNode(JsonNode n) {
    if (n == null || n.isNull() || !n.isObject()) {
      return null;
    }
    if (!n.has("variable") && !n.has("expression") && !n.has("executionOrder")) {
      return null;
    }
    return MAPPER.convertValue(n, TemplateBindingSummary.class);
  }

  static List<TemplateSlotSummary> slotsFromNode(JsonNode raw) {
    return collectList(raw, "TemplateSlot", "Slot", TemplateDetailJsonReader::slotFromNode);
  }

  private static TemplateSlotSummary slotFromNode(JsonNode n) {
    if (n == null || n.isNull() || !n.isObject()) {
      return null;
    }
    if (!n.has("name") && !n.has("guid") && !n.has("label")) {
      return null;
    }
    return MAPPER.convertValue(n, TemplateSlotSummary.class);
  }

  static List<NamedObjectRef> refsFromNode(JsonNode raw) {
    return collectList(raw, "NamedObjectRef", "namedObjectRef", TemplateDetailJsonReader::refFromNode);
  }

  /**
   * Shared array-or-wrapped-object list reader behind bindings/slots/refs. Accepts an array, a
   * single row object, or a wrapper object holding the rows under either envelope name.
   */
  private static <T> List<T> collectList(
      JsonNode raw, String wrapA, String wrapB, Function<JsonNode, T> rowParser) {
    if (raw == null || raw.isNull() || raw.isMissingNode()) {
      return new ArrayList<>();
    }
    if (raw.isArray()) {
      List<T> out = new ArrayList<>();
      for (JsonNode n : raw) {
        T row = rowParser.apply(n);
        if (row != null) {
          out.add(row);
        }
      }
      return out;
    }
    return collectSingle(raw, wrapA, wrapB, rowParser);
  }

  private static <T> List<T> collectSingle(
      JsonNode raw, String wrapA, String wrapB, Function<JsonNode, T> rowParser) {
    if (!raw.isObject()) {
      return new ArrayList<>();
    }
    JsonNode wrapped = wrappedChild(raw, wrapA, wrapB);
    if (wrapped != null) {
      return collectList(wrapped, wrapA, wrapB, rowParser);
    }
    T row = rowParser.apply(raw);
    List<T> single = new ArrayList<>();
    if (row != null) {
      single.add(row);
    }
    return single;
  }

  private static JsonNode wrappedChild(JsonNode raw, String wrapA, String wrapB) {
    JsonNode wrapped = raw.get(wrapA);
    return wrapped != null ? wrapped : raw.get(wrapB);
  }

  private static NamedObjectRef refFromNode(JsonNode n) {
    if (n == null || n.isNull() || !n.isObject()) {
      return null;
    }
    NamedObjectRef ref = new NamedObjectRef();
    String name = textField(n, "name");
    String label = textField(n, "label");
    if (name != null) {
      ref.setName(name);
    }
    if (label != null) {
      ref.setLabel(label);
    }
    Guid guid = guidFromNode(n.get("guid"));
    if (guid != null) {
      ref.setGuid(guid);
    }
    if (ref.getName() == null && ref.getGuid() == null) {
      return null;
    }
    return ref;
  }

  /** Parses the {@code guid} child; null when absent or carrying no identity. */
  private static Guid guidFromNode(JsonNode guidNode) {
    if (guidNode == null || guidNode.isNull() || !guidNode.isObject()) {
      return null;
    }
    Guid guid = new Guid();
    String sv = textField(guidNode, "stringValue");
    if (sv != null) {
      guid.setStringValue(sv);
    }
    JsonNode uuid = guidNode.get("uuid");
    if (uuid != null && uuid.isNumber()) {
      guid.setUuid(uuid.asInt());
    }
    if (guid.getStringValue() == null && guid.getUuid() <= 0) {
      return null;
    }
    return guid;
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
