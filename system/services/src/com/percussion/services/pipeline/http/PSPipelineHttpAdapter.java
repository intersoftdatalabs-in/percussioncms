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

package com.percussion.services.pipeline.http;

import com.percussion.services.pipeline.PSPipelineIrException;
import com.percussion.services.pipeline.model.BackendTankStageIr;
import com.percussion.services.pipeline.model.MapperStageIr;
import com.percussion.services.pipeline.model.MappingEntryIr;
import com.percussion.services.pipeline.model.PipelineExecuteRequest;
import com.percussion.services.pipeline.model.PipelineResourceIr;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Loopback-only HTTP GET adapter: bundled classpath fixture or live loopback HttpClient fetch.
 *
 * <p>Redirects are refused (open-redirect / SSRF fail-closed). Cloud hosts, userinfo, and
 * non-http(s) schemes are rejected by {@link PSPipelineHttpUrl}.
 */
public class PSPipelineHttpAdapter implements IPSPipelineHttpAdapter {

  static final String BUNDLED_RESOURCE = "pipeline-http-fixture.json";
  static final int MAX_BODY_BYTES = 1_000_000;
  private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
  private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);

  private static final HttpClient HTTP =
      HttpClient.newBuilder()
          .followRedirects(HttpClient.Redirect.NEVER)
          .connectTimeout(CONNECT_TIMEOUT)
          .build();

  @Override
  public List<Map<String, Object>> query(
      PipelineResourceIr resource, PipelineExecuteRequest request) throws PSPipelineIrException {
    Objects.requireNonNull(resource, "resource");
    BackendTankStageIr tank = resource.getStages() != null ? resource.getStages().getBackendTank() : null;
    if (tank == null || !tank.isHttpAdapter()) {
      throw new PSPipelineIrException("Resource is not an HTTP backend tank");
    }
    URL safe = PSPipelineHttpUrl.requireSafe(tank.getUrl());
    String method = tank.getHttpMethod();
    if (method != null
        && !method.isBlank()
        && !"GET".equalsIgnoreCase(method.trim())) {
      throw new PSPipelineIrException("HTTP datasource supports GET only in this slice");
    }
    String body = PSPipelineHttpUrl.isBundledFixture(safe) ? readBundledFixture() : fetchLoopback(safe);
    List<Map<String, Object>> raw = parseRows(body);
    if (raw.isEmpty()) {
      throw new PSPipelineIrException("HTTP datasource returned no rows or document fields");
    }
    return mapRows(resource, raw);
  }

  static String readBundledFixture() throws PSPipelineIrException {
    try (InputStream in = PSPipelineHttpAdapter.class.getResourceAsStream(BUNDLED_RESOURCE)) {
      if (in == null) {
        throw new PSPipelineIrException("Bundled HTTP pipeline fixture is missing");
      }
      byte[] bytes = in.readAllBytes();
      if (bytes.length > MAX_BODY_BYTES) {
        throw new PSPipelineIrException("Bundled HTTP pipeline fixture exceeds size limit");
      }
      return new String(bytes, StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new PSPipelineIrException("Failed to read bundled HTTP pipeline fixture", e);
    }
  }

  static String fetchLoopback(URL validated) throws PSPipelineIrException {
    URI requestUri = PSPipelineHttpUrl.toRequestUri(validated);
    HttpRequest request =
        HttpRequest.newBuilder(requestUri) // codeql[java/ssrf]
            .timeout(REQUEST_TIMEOUT)
            .GET()
            .header("Accept", "application/json, text/plain;q=0.9, */*;q=0.1")
            .build();
    HttpResponse<byte[]> response;
    try {
      response = HTTP.send(request, HttpResponse.BodyHandlers.ofByteArray());
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new PSPipelineIrException(
          "HTTP datasource request interrupted: " + PSPipelineHttpUrl.redact(validated), e);
    } catch (IOException e) {
      throw new PSPipelineIrException(
          "HTTP datasource request failed: " + PSPipelineHttpUrl.redact(validated), e);
    }
    int status = response.statusCode();
    if (status >= 300 && status < 400) {
      throw new PSPipelineIrException(
          "HTTP datasource redirect refused (open redirect / SSRF fail-closed): "
              + PSPipelineHttpUrl.redact(validated)
              + " status "
              + status);
    }
    if (status != 200) {
      throw new PSPipelineIrException(
          "HTTP datasource request failed: "
              + PSPipelineHttpUrl.redact(validated)
              + " status "
              + status);
    }
    byte[] body = response.body() != null ? response.body() : new byte[0];
    if (body.length > MAX_BODY_BYTES) {
      throw new PSPipelineIrException(
          "HTTP datasource body exceeds " + MAX_BODY_BYTES + " bytes from "
              + PSPipelineHttpUrl.redact(validated));
    }
    return new String(body, StandardCharsets.UTF_8);
  }

  public static List<Map<String, Object>> parseRows(String json) throws PSPipelineIrException {
    if (json == null || json.isBlank()) {
      throw new PSPipelineIrException("HTTP datasource JSON is empty");
    }
    String trimmed = json.trim();
    try {
      if (trimmed.startsWith("[")) {
        return arrayToRows(new JSONArray(trimmed));
      }
      JSONObject obj = new JSONObject(trimmed);
      if (obj.has("rows") && obj.get("rows") instanceof JSONArray rows) {
        return arrayToRows(rows);
      }
      Map<String, Object> single = objectToRow(obj);
      if (single.isEmpty()) {
        throw new PSPipelineIrException("HTTP datasource JSON object has no fields");
      }
      return List.of(single);
    } catch (JSONException e) {
      throw new PSPipelineIrException("HTTP datasource body is not JSON", e);
    }
  }

  private static List<Map<String, Object>> arrayToRows(JSONArray array) {
    List<Map<String, Object>> out = new ArrayList<>();
    for (int i = 0; i < array.length(); i++) {
      Object el = array.opt(i);
      if (el instanceof JSONObject obj) {
        Map<String, Object> row = objectToRow(obj);
        if (!row.isEmpty()) {
          out.add(row);
        }
      }
    }
    return out;
  }

  private static Map<String, Object> objectToRow(JSONObject obj) {
    Map<String, Object> row = new LinkedHashMap<>();
    for (String name : obj.keySet()) {
      Object val = obj.opt(name);
      if (val == JSONObject.NULL) {
        row.put(name, null);
      } else if (val instanceof JSONObject || val instanceof JSONArray) {
        row.put(name, val.toString());
      } else {
        row.put(name, val);
      }
    }
    return row;
  }

  static List<Map<String, Object>> mapRows(PipelineResourceIr resource, List<Map<String, Object>> raw) {
    MapperStageIr mapper = resource.getStages() != null ? resource.getStages().getMapper() : null;
    List<MappingEntryIr> mappings =
        mapper != null && mapper.isPresent() && mapper.getMappings() != null
            ? mapper.getMappings()
            : List.of();
    if (mappings.isEmpty()) {
      return raw;
    }
    List<Map<String, Object>> out = new ArrayList<>();
    boolean anyValue = false;
    for (Map<String, Object> src : raw) {
      Map<String, Object> dest = new LinkedHashMap<>();
      for (MappingEntryIr m : mappings) {
        if (m == null || m.getDocumentField() == null || m.getDocumentField().isBlank()) {
          continue;
        }
        Object value = lookupBackend(src, m.getBackend());
        dest.put(m.getDocumentField().trim(), value);
        if (value != null && !(value instanceof String s && s.isBlank())) {
          anyValue = true;
        }
      }
      if (!dest.isEmpty()) {
        out.add(dest);
      }
    }
    // Classic XML mapper columns (e.g. link/@url) do not match HTTP JSON keys —
    // keep the fetched document fields instead of an all-null mapped document.
    return !anyValue || out.isEmpty() ? raw : out;
  }

  private static Object lookupBackend(Map<String, Object> src, String backend) {
    if (src == null || backend == null || backend.isBlank()) {
      return null;
    }
    String key = backend.trim();
    if (src.containsKey(key)) {
      return src.get(key);
    }
    for (Map.Entry<String, Object> e : src.entrySet()) {
      if (e.getKey() != null && e.getKey().equalsIgnoreCase(key)) {
        return e.getValue();
      }
    }
    int dot = key.lastIndexOf('.');
    String tail = dot >= 0 && dot < key.length() - 1 ? key.substring(dot + 1) : key;
    if (!tail.equals(key) && src.containsKey(tail)) {
      return src.get(tail);
    }
    for (Map.Entry<String, Object> e : src.entrySet()) {
      if (e.getKey() != null && e.getKey().equalsIgnoreCase(tail)) {
        return e.getValue();
      }
    }
    return null;
  }

  /** True when {@code adapterType} is HTTP or REST (case-insensitive). */
  public static boolean isHttpAdapterType(String adapterType) {
    if (adapterType == null || adapterType.isBlank()) {
      return false;
    }
    String n = adapterType.trim().toUpperCase(Locale.ROOT);
    return "HTTP".equals(n) || "REST".equals(n);
  }
}
