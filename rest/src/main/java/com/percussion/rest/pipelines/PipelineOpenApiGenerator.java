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

package com.percussion.rest.pipelines;

import com.percussion.services.pipeline.model.MappingEntryIr;
import com.percussion.services.pipeline.model.PipelineIrDocument;
import com.percussion.services.pipeline.model.PipelineResourceIr;
import com.percussion.services.pipeline.model.PipelineStagesIr;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Builds an OpenAPI 3 document from a pipeline IR (native or classic import). Does not publish to
 * an external registry. Path keys use trusted IR resource names only.
 */
public final class PipelineOpenApiGenerator {

  public static final String FORMAT_YAML = "yaml";
  public static final String FORMAT_JSON = "json";

  public static final String MEDIA_TYPE_YAML = "application/yaml";

  private PipelineOpenApiGenerator() {}

  /**
   * @return {@link #FORMAT_YAML} or {@link #FORMAT_JSON}
   * @throws IllegalArgumentException when {@code format} is not yaml/json (blank defaults to yaml)
   */
  public static String normalizeFormat(String format) {
    if (format == null || format.isBlank()) {
      return FORMAT_YAML;
    }
    String f = format.trim().toLowerCase(Locale.ROOT);
    if (FORMAT_YAML.equals(f) || "yml".equals(f)) {
      return FORMAT_YAML;
    }
    if (FORMAT_JSON.equals(f)) {
      return FORMAT_JSON;
    }
    throw new IllegalArgumentException("OpenAPI format must be yaml or json");
  }

  /** OpenAPI 3 object graph (insertion-ordered maps). */
  public static Map<String, Object> toSpec(PipelineIrDocument ir) {
    PipelineIrDocument doc = ir != null ? ir : new PipelineIrDocument();
    String appName = appName(doc);
    String version =
        doc.getIrVersion() != null && !doc.getIrVersion().isBlank()
            ? doc.getIrVersion()
            : PipelineIrDocument.CURRENT_IR_VERSION;
    String source =
        doc.getSource() != null && !doc.getSource().isBlank()
            ? doc.getSource()
            : PipelineIrDocument.SOURCE_NATIVE;

    Map<String, Object> spec = new LinkedHashMap<>();
    spec.put("openapi", "3.0.3");

    Map<String, Object> info = new LinkedHashMap<>();
    info.put("title", appName + " pipeline");
    String desc = doc.getApp() != null ? doc.getApp().getDescription() : null;
    StringBuilder infoDesc = new StringBuilder();
    infoDesc.append("Generated from pipeline IR resources (source ").append(source).append(").");
    infoDesc.append(" Not published to an external registry.");
    if (desc != null && !desc.isBlank()) {
      infoDesc.append(' ').append(desc.trim());
    }
    info.put("description", infoDesc.toString());
    info.put("version", version);
    spec.put("info", info);

    List<Map<String, Object>> servers = new ArrayList<>();
    Map<String, Object> server = new LinkedHashMap<>();
    server.put("url", "/Rhythmyx/services");
    server.put("description", "CMS REST services");
    servers.add(server);
    spec.put("servers", servers);

    Map<String, Object> paths = new LinkedHashMap<>();
    List<PipelineResourceIr> resources = doc.getResources();
    if (resources != null) {
      for (PipelineResourceIr resource : resources) {
        if (resource == null) {
          continue;
        }
        String resName = resource.getName();
        if (!isSafePathSegment(resName) || !isSafePathSegment(appName)) {
          continue;
        }
        String path = "/pipelines/" + appName + "/resources/" + resName.trim() + "/execute";
        paths.put(path, executePathItem(appName, resource));
      }
    }
    spec.put("paths", paths);

    Map<String, Object> components = new LinkedHashMap<>();
    Map<String, Object> schemas = new LinkedHashMap<>();
    schemas.put("PipelineExecuteRequest", executeRequestSchema());
    schemas.put("PipelineExecuteResult", executeResultSchema());
    components.put("schemas", schemas);
    spec.put("components", components);

    Map<String, Object> ext = new LinkedHashMap<>();
    ext.put("source", source);
    ext.put("appName", appName);
    spec.put("x-percussion-pipeline", ext);
    return spec;
  }

  public static String toJson(Map<String, Object> spec) {
    StringBuilder out = new StringBuilder();
    writeJson(out, spec != null ? spec : toSpec(null), 0);
    out.append('\n');
    return out.toString();
  }

  public static String toYaml(Map<String, Object> spec) {
    StringBuilder out = new StringBuilder();
    writeYaml(out, spec != null ? spec : toSpec(null), 0);
    return out.toString();
  }

  public static String render(PipelineIrDocument ir, String format) {
    Map<String, Object> spec = toSpec(ir);
    if (FORMAT_JSON.equals(normalizeFormat(format))) {
      return toJson(spec);
    }
    return toYaml(spec);
  }

  static boolean isSafePathSegment(String name) {
    if (name == null || name.isBlank()) {
      return false;
    }
    String n = name.trim();
    return !n.contains("..")
        && n.indexOf('/') < 0
        && n.indexOf('\\') < 0
        && n.indexOf('\0') < 0
        && n.indexOf('?') < 0
        && n.indexOf('#') < 0
        && n.indexOf('\n') < 0
        && n.indexOf('\r') < 0;
  }

  private static String appName(PipelineIrDocument doc) {
    if (doc.getApp() != null && doc.getApp().getName() != null && !doc.getApp().getName().isBlank()) {
      return doc.getApp().getName().trim();
    }
    return "pipeline";
  }

  private static Map<String, Object> executePathItem(String appName, PipelineResourceIr resource) {
    String resName = resource.getName().trim();
    String kind = resource.getKind() != null ? resource.getKind() : PipelineResourceIr.KIND_UNKNOWN;
    Map<String, Object> post = new LinkedHashMap<>();
    post.put("operationId", operationId(appName, resName));
    post.put("summary", "Execute " + kind + " resource " + resName);
    StringBuilder desc = new StringBuilder();
    desc.append("Native pipeline IR execute for application ").append(appName);
    desc.append(" resource ").append(resName).append(" (kind ").append(kind).append(").");
    if (resource.getRequestPage() != null && !resource.getRequestPage().isBlank()) {
      desc.append(" Request page: ").append(resource.getRequestPage().trim()).append('.');
    }
    if (resource.getDescription() != null && !resource.getDescription().isBlank()) {
      desc.append(' ').append(resource.getDescription().trim());
    }
    String adapter = adapterType(resource);
    if (adapter != null) {
      desc.append(" Backend adapter: ").append(adapter).append('.');
    }
    List<String> fields = documentFields(resource);
    if (!fields.isEmpty()) {
      desc.append(" Document fields: ").append(String.join(", ", fields)).append('.');
    }
    post.put("description", desc.toString());
    List<String> tags = new ArrayList<>();
    tags.add(appName);
    post.put("tags", tags);

    Map<String, Object> reqBody = new LinkedHashMap<>();
    reqBody.put("required", false);
    Map<String, Object> reqContent = new LinkedHashMap<>();
    Map<String, Object> reqJson = new LinkedHashMap<>();
    Map<String, Object> reqSchema = new LinkedHashMap<>();
    reqSchema.put("$ref", "#/components/schemas/PipelineExecuteRequest");
    reqJson.put("schema", reqSchema);
    reqContent.put("application/json", reqJson);
    reqBody.put("content", reqContent);
    post.put("requestBody", reqBody);

    Map<String, Object> responses = new LinkedHashMap<>();
    responses.put("200", response("Execute result", "#/components/schemas/PipelineExecuteResult"));
    responses.put("400", messageResponse("Invalid input or unsupported resource"));
    responses.put("404", messageResponse("Application or resource not found"));
    post.put("responses", responses);

    Map<String, Object> pathItem = new LinkedHashMap<>();
    pathItem.put("post", post);
    return pathItem;
  }

  private static String adapterType(PipelineResourceIr resource) {
    PipelineStagesIr stages = resource.getStages();
    if (stages == null || stages.getBackendTank() == null) {
      return null;
    }
    String adapter = stages.getBackendTank().getAdapterType();
    return adapter != null && !adapter.isBlank() ? adapter.trim() : null;
  }

  private static Map<String, Object> executeRequestSchema() {
    Map<String, Object> schema = new LinkedHashMap<>();
    schema.put("type", "object");
    schema.put(
        "description", "JSON body for POST …/execute (params/rows). Root-wrapped on the wire.");
    Map<String, Object> props = new LinkedHashMap<>();
    props.put("params", mapOf("type", "object", "additionalProperties", true));
    props.put(
        "rows",
        mapOf("type", "array", "items", mapOf("type", "object", "additionalProperties", true)));
    props.put("operation", mapOf("type", "string"));
    schema.put("properties", props);
    return schema;
  }

  private static Map<String, Object> executeResultSchema() {
    Map<String, Object> schema = new LinkedHashMap<>();
    schema.put("type", "object");
    Map<String, Object> props = new LinkedHashMap<>();
    props.put("appName", mapOf("type", "string"));
    props.put("resourceName", mapOf("type", "string"));
    props.put("operation", mapOf("type", "string"));
    props.put("rowCount", mapOf("type", "integer"));
    props.put("affectedRows", mapOf("type", "integer"));
    props.put(
        "rows",
        mapOf("type", "array", "items", mapOf("type", "object", "additionalProperties", true)));
    schema.put("properties", props);
    return schema;
  }

  private static Map<String, Object> response(String description, String ref) {
    Map<String, Object> r = new LinkedHashMap<>();
    r.put("description", description);
    Map<String, Object> content = new LinkedHashMap<>();
    Map<String, Object> json = new LinkedHashMap<>();
    json.put("schema", mapOf("$ref", ref));
    content.put("application/json", json);
    r.put("content", content);
    return r;
  }

  private static Map<String, Object> messageResponse(String description) {
    Map<String, Object> r = new LinkedHashMap<>();
    r.put("description", description);
    return r;
  }

  private static Map<String, Object> mapOf(Object... kv) {
    Map<String, Object> m = new LinkedHashMap<>();
    for (int i = 0; i + 1 < kv.length; i += 2) {
      m.put(String.valueOf(kv[i]), kv[i + 1]);
    }
    return m;
  }

  private static String operationId(String appName, String resName) {
    String raw = "execute_" + appName + "_" + resName;
    StringBuilder b = new StringBuilder();
    for (int i = 0; i < raw.length(); i++) {
      char c = raw.charAt(i);
      if ((c >= 'A' && c <= 'Z')
          || (c >= 'a' && c <= 'z')
          || (c >= '0' && c <= '9')
          || c == '_') {
        b.append(c);
      } else {
        b.append('_');
      }
    }
    return b.toString();
  }

  /**
   * Minimal YAML emitter for maps/lists/scalars used by this generator. Always quotes strings.
   */
  static void writeYaml(StringBuilder out, Object value, int indent) {
    if (value instanceof Map<?, ?> map) {
      if (map.isEmpty()) {
        out.append("{}\n");
        return;
      }
      for (Map.Entry<?, ?> e : map.entrySet()) {
        indent(out, indent);
        out.append(yamlKey(String.valueOf(e.getKey()))).append(':');
        Object child = e.getValue();
        if (child instanceof Map<?, ?> || child instanceof List<?>) {
          if (isEmptyCollection(child)) {
            out.append(' ').append(child instanceof List<?> ? "[]" : "{}").append('\n');
          } else {
            out.append('\n');
            writeYaml(out, child, indent + 2);
          }
        } else {
          out.append(' ');
          writeYamlScalar(out, child);
          out.append('\n');
        }
      }
      return;
    }
    if (value instanceof List<?> list) {
      if (list.isEmpty()) {
        indent(out, indent);
        out.append("[]\n");
        return;
      }
      for (Object item : list) {
        indent(out, indent);
        out.append('-');
        if (item instanceof Map<?, ?> || item instanceof List<?>) {
          if (isEmptyCollection(item)) {
            out.append(' ').append(item instanceof List<?> ? "[]" : "{}").append('\n');
          } else {
            out.append('\n');
            writeYaml(out, item, indent + 2);
          }
        } else {
          out.append(' ');
          writeYamlScalar(out, item);
          out.append('\n');
        }
      }
      return;
    }
    indent(out, indent);
    writeYamlScalar(out, value);
    out.append('\n');
  }

  private static boolean isEmptyCollection(Object child) {
    if (child instanceof Map<?, ?> m) {
      return m.isEmpty();
    }
    if (child instanceof List<?> l) {
      return l.isEmpty();
    }
    return false;
  }

  private static void writeYamlScalar(StringBuilder out, Object value) {
    if (value == null) {
      out.append("null");
      return;
    }
    if (value instanceof Boolean || value instanceof Number) {
      out.append(value);
      return;
    }
    out.append('"').append(escapeYaml(String.valueOf(value))).append('"');
  }

  private static String yamlKey(String key) {
    if (key.matches("[A-Za-z_][A-Za-z0-9_-]*")) {
      return key;
    }
    return '"' + escapeYaml(key) + '"';
  }

  private static String escapeYaml(String s) {
    return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
  }

  private static void indent(StringBuilder out, int n) {
    for (int i = 0; i < n; i++) {
      out.append(' ');
    }
  }

  static void writeJson(StringBuilder out, Object value, int indent) {
    if (value == null) {
      out.append("null");
      return;
    }
    if (value instanceof Map<?, ?> map) {
      out.append('{');
      if (map.isEmpty()) {
        out.append('}');
        return;
      }
      out.append('\n');
      int i = 0;
      int size = map.size();
      for (Map.Entry<?, ?> e : map.entrySet()) {
        indent(out, indent + 2);
        out.append('"').append(escapeJson(String.valueOf(e.getKey()))).append("\": ");
        writeJson(out, e.getValue(), indent + 2);
        i++;
        if (i < size) {
          out.append(',');
        }
        out.append('\n');
      }
      indent(out, indent);
      out.append('}');
      return;
    }
    if (value instanceof List<?> list) {
      out.append('[');
      if (list.isEmpty()) {
        out.append(']');
        return;
      }
      out.append('\n');
      for (int i = 0; i < list.size(); i++) {
        indent(out, indent + 2);
        writeJson(out, list.get(i), indent + 2);
        if (i + 1 < list.size()) {
          out.append(',');
        }
        out.append('\n');
      }
      indent(out, indent);
      out.append(']');
      return;
    }
    if (value instanceof Boolean || value instanceof Number) {
      out.append(value);
      return;
    }
    out.append('"').append(escapeJson(String.valueOf(value))).append('"');
  }

  private static String escapeJson(String s) {
    return s.replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
        .replace("\t", "\\t");
  }

  /** Mapper document fields (unused in schema today; kept for resource-level docs). */
  static List<String> documentFields(PipelineResourceIr resource) {
    List<String> fields = new ArrayList<>();
    if (resource == null || resource.getStages() == null || resource.getStages().getMapper() == null) {
      return fields;
    }
    List<MappingEntryIr> mappings = resource.getStages().getMapper().getMappings();
    if (mappings == null) {
      return fields;
    }
    for (MappingEntryIr m : mappings) {
      if (m != null && m.getDocumentField() != null && !m.getDocumentField().isBlank()) {
        fields.add(m.getDocumentField().trim());
      }
    }
    return fields;
  }
}
