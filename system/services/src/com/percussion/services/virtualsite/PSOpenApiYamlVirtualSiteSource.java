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
package com.percussion.services.virtualsite;

import com.percussion.services.virtualsite.VirtualSiteConfig.OpenApiSpec;
import com.percussion.services.virtualsite.VirtualSiteConfig.VersionSpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

/**
 * Virtual Site source: local OpenAPI 3 YAML fixture ({@code openapi-yaml}).
 *
 * <p>Default file under the site root: {@code openapi.yaml}. {@code _config.yaml} {@code
 * openapi.file} overrides the filename. {@code openapi.url}, Git {@code virtual.remoteUrl},
 * credential properties, cloud URLs, remote {@code $ref} values, and live HTTP spec fetch are
 * rejected — this slice is a local fixture only.
 *
 * <p>Each path/operation maps into assemble {@code id}/{@code title}/{@code body}. A fixture with
 * no operations still emits one page from {@code info} so CLI assemble writes HTML ({@code
 * pagesWritten > 0}).
 *
 * <p>Stateless: {@link #discover} and {@link #load} always re-read the current local fixture via
 * {@link Files#readString}. No path/mtime parse cache is kept on the instance or in statics — a
 * second build in the same JVM after an OpenAPI ({@code openapi.file} / default {@code
 * openapi.yaml}) or {@code _config.yaml} edit must see the new bytes. File watchers are not used;
 * {@code _config.yaml} is reloaded by {@link PSVirtualSiteBuildService}, not this source.
 */
public class PSOpenApiYamlVirtualSiteSource implements IPSVirtualSiteSource {

  static final String DEFAULT_OPENAPI_FILE = "openapi.yaml";
  static final int MAX_OPENAPI_BYTES = 2_000_000;

  private static final List<String> HTTP_METHODS =
      List.of("get", "put", "post", "delete", "options", "head", "patch", "trace");

  @Override
  public String sourceType() {
    return VirtualSiteSourceType.OPENAPI_YAML.wireName();
  }

  @Override
  public List<VirtualItemRef> discover(VirtualSiteConfig config)
      throws IOException, VirtualSiteException {
    List<LoadedRow> rows = loadAllRows(config);
    List<VirtualItemRef> refs = new ArrayList<>(rows.size());
    for (LoadedRow row : rows) {
      refs.add(row.ref());
    }
    refs.sort(
        Comparator.comparing(VirtualItemRef::versionId)
            .thenComparingInt(VirtualItemRef::order)
            .thenComparing(r -> r.relativePath().toString().replace('\\', '/')));
    return refs;
  }

  @Override
  public VirtualItem load(VirtualSiteConfig config, VirtualItemRef ref)
      throws IOException, VirtualSiteException {
    if (ref == null) {
      throw new VirtualSiteException("openapi-yaml item ref is required");
    }
    List<LoadedRow> rows = loadAllRows(config);
    for (LoadedRow row : rows) {
      if (row.ref().versionId().equals(ref.versionId()) && row.ref().id().equals(ref.id())) {
        return new VirtualItem(row.ref(), row.frontmatter(), row.body(), row.sourcePath());
      }
    }
    throw new VirtualSiteException(
        "Unknown openapi-yaml page id '" + ref.id() + "' in version " + ref.versionId());
  }

  private static List<LoadedRow> loadAllRows(VirtualSiteConfig config)
      throws IOException, VirtualSiteException {
    if (config == null) {
      throw new VirtualSiteException("Virtual Site config is required");
    }
    Path root = config.root();
    if (root == null || !PSVirtualSiteHelper.isSafeRootPath(root)) {
      throw new VirtualSiteException("openapi-yaml site root is missing or unsafe");
    }
    Path safeRoot = root.normalize();
    OpenApiFetch fetch = readOpenApi(config, safeRoot);
    List<LoadedOp> ops = parseOperations(fetch.text());
    List<LoadedRow> pages = toPages(ops, fetch, config);
    if (pages.isEmpty()) {
      throw new VirtualSiteException(
          "openapi-yaml fixture produced no pages: "
              + fetch.sourcePath().toAbsolutePath().normalize());
    }
    List<LoadedRow> rows = new ArrayList<>();
    Map<String, Path> seenIds = new HashMap<>();
    Map<String, Path> seenPaths = new HashMap<>();
    for (LoadedRow loaded : pages) {
      String idKey = loaded.ref().versionId() + "\0" + loaded.ref().id();
      Path previousId = seenIds.put(idKey, loaded.ref().relativePath());
      if (previousId != null) {
        throw new VirtualSiteException(
            "Duplicate openapi-yaml id '"
                + loaded.ref().id()
                + "' in version "
                + loaded.ref().versionId()
                + ": "
                + previousId
                + " and "
                + loaded.ref().relativePath());
      }
      String pathKey =
          loaded.ref().versionId()
              + "\0"
              + loaded.ref().relativePath().toString().replace('\\', '/');
      Path previousPath = seenPaths.put(pathKey, loaded.ref().relativePath());
      if (previousPath != null) {
        throw new VirtualSiteException(
            "Duplicate openapi-yaml path '"
                + loaded.ref().relativePath()
                + "' in version "
                + loaded.ref().versionId()
                + ": "
                + previousPath
                + " and "
                + loaded.ref().relativePath());
      }
      rows.add(loaded);
    }
    return rows;
  }

  private static OpenApiFetch readOpenApi(VirtualSiteConfig config, Path safeRoot)
      throws IOException, VirtualSiteException {
    OpenApiSpec spec = config.openapi();
    if (spec != null && spec.hasUrl()) {
      throw new VirtualSiteException(
          "openapi.url is not supported (local openapi.yaml fixture only; no live HTTP fetch or"
              + " remote OpenAPI URLs)");
    }
    if (spec != null && spec.hasFile()) {
      Path file = resolveOpenApiFile(safeRoot, spec.file());
      return new OpenApiFetch(readOpenApiFile(file), file);
    }
    Path defaultFile = safeRoot.resolve(DEFAULT_OPENAPI_FILE).normalize();
    if (Files.isRegularFile(defaultFile) && defaultFile.startsWith(safeRoot)) {
      return new OpenApiFetch(readOpenApiFile(defaultFile), defaultFile);
    }
    throw new VirtualSiteException(
        "openapi-yaml fixture not found: expected "
            + DEFAULT_OPENAPI_FILE
            + " under "
            + safeRoot.toAbsolutePath().normalize());
  }

  private static String readOpenApiFile(Path file) throws IOException, VirtualSiteException {
    if (!Files.isRegularFile(file)) {
      throw new VirtualSiteException(
          "openapi-yaml file not found: " + file.toAbsolutePath().normalize());
    }
    long size = Files.size(file);
    if (size > MAX_OPENAPI_BYTES) {
      throw new VirtualSiteException(
          "openapi-yaml file exceeds " + MAX_OPENAPI_BYTES + " bytes: " + file);
    }
    String text = Files.readString(file, StandardCharsets.UTF_8);
    if (text == null || text.isBlank()) {
      throw new VirtualSiteException("openapi-yaml fixture is empty");
    }
    if (text.indexOf('\0') >= 0) {
      throw new VirtualSiteException("openapi-yaml file must not contain NUL");
    }
    return text;
  }

  static Path resolveOpenApiFile(Path root, String openApiFile) throws VirtualSiteException {
    if (openApiFile == null || openApiFile.isBlank()) {
      throw new VirtualSiteException("openapi-yaml file is blank");
    }
    if (openApiFile.indexOf('\0') >= 0) {
      throw new VirtualSiteException("openapi-yaml file must not contain NUL");
    }
    String logical = openApiFile.trim().replace('\\', '/');
    if (logical.startsWith("/") || looksAbsoluteWindows(logical)) {
      throw new VirtualSiteException("openapi-yaml file must be relative to the site root");
    }
    Path safeRoot = root.normalize();
    Path resolved;
    try {
      Path relative = Path.of("");
      for (String seg : logical.split("/")) {
        if (seg.isEmpty() || ".".equals(seg)) {
          continue;
        }
        if ("..".equals(seg) || seg.indexOf('\0') >= 0) {
          throw new VirtualSiteException(
              "openapi-yaml file must not contain '..' or NUL segments");
        }
        relative = relative.resolve(seg);
      }
      if (relative.getNameCount() == 0 || relative.toString().isEmpty()) {
        throw new VirtualSiteException("openapi-yaml file is empty after normalize");
      }
      resolved = safeRoot.resolve(relative).normalize();
    } catch (InvalidPathException e) {
      throw new VirtualSiteException("openapi-yaml file is not a valid path", e);
    }
    if (!resolved.startsWith(safeRoot)
        || !PSVirtualSiteHelper.isSafeRootPath(resolved)
        || remainingParent(resolved)) {
      throw new VirtualSiteException("openapi-yaml file escapes the site root");
    }
    return resolved;
  }

  @SuppressWarnings("unchecked")
  static List<LoadedOp> parseOperations(String text) throws VirtualSiteException {
    if (text == null || text.isBlank()) {
      throw new VirtualSiteException("openapi-yaml fixture is empty");
    }
    Object loaded;
    try {
      Yaml yaml = new Yaml(new SafeConstructor(new LoaderOptions()));
      loaded = yaml.load(text);
    } catch (Exception e) {
      throw new VirtualSiteException("openapi-yaml fixture is not valid YAML", e);
    }
    if (!(loaded instanceof Map<?, ?> map)) {
      throw new VirtualSiteException("openapi-yaml root must be a YAML mapping");
    }
    Map<String, Object> root = (Map<String, Object>) map;
    rejectRemoteRefs(root, "openapi-yaml");
    String version = stringVal(root.get("openapi"));
    if (version == null || version.isBlank()) {
      throw new VirtualSiteException(
          "openapi-yaml fixture must declare OpenAPI 3 (openapi: 3.x.x); live spec fetch is not"
              + " supported");
    }
    if (!version.startsWith("3")) {
      throw new VirtualSiteException(
          "openapi-yaml fixture must be OpenAPI 3 (found openapi: " + version + ")");
    }
    Object pathsObj = root.get("paths");
    List<LoadedOp> ops = new ArrayList<>();
    if (pathsObj instanceof Map<?, ?> pathsMap) {
      int order = 0;
      for (Map.Entry<?, ?> pathEntry : pathsMap.entrySet()) {
        if (pathEntry.getKey() == null) {
          continue;
        }
        String path = String.valueOf(pathEntry.getKey()).trim();
        if (path.isEmpty()) {
          continue;
        }
        rejectRemoteOrCloudUrl(path, "openapi-yaml path");
        if (!(pathEntry.getValue() instanceof Map<?, ?> pathItemRaw)) {
          continue;
        }
        Map<String, Object> pathItem = (Map<String, Object>) pathItemRaw;
        if (pathItem.containsKey("$ref")) {
          throw new VirtualSiteException(
              "openapi-yaml path $ref is not supported (inline operations only; no live HTTP"
                  + " fetch). Path: "
                  + path);
        }
        for (String method : HTTP_METHODS) {
          Object opObj = pathItem.get(method);
          if (!(opObj instanceof Map<?, ?> opMapRaw)) {
            continue;
          }
          Map<String, Object> opMap = (Map<String, Object>) opMapRaw;
          if (opMap.containsKey("$ref")) {
            throw new VirtualSiteException(
                "openapi-yaml operation $ref is not supported (inline operations only; no live"
                    + " HTTP fetch). Path: "
                    + method.toUpperCase(Locale.ROOT)
                    + " "
                    + path);
          }
          order++;
          String operationId = stringVal(opMap.get("operationId"));
          String summary = stringVal(opMap.get("summary"));
          String description = stringVal(opMap.get("description"));
          ops.add(
              new LoadedOp(
                  path, method.toUpperCase(Locale.ROOT), operationId, summary, description, order));
        }
      }
    } else if (pathsObj != null) {
      throw new VirtualSiteException("openapi-yaml paths: must be a mapping");
    }
    return ops;
  }

  static String infoTitle(String text) {
    try {
      Yaml yaml = new Yaml(new SafeConstructor(new LoaderOptions()));
      Object loaded = yaml.load(text);
      if (loaded instanceof Map<?, ?> map) {
        Object infoObj = map.get("info");
        if (infoObj instanceof Map<?, ?> info) {
          String title = stringVal(info.get("title"));
          if (title != null && !title.isBlank()) {
            return title;
          }
        }
      }
    } catch (Exception ignored) {
      // fallback title
    }
    return "OpenAPI";
  }

  static String infoDescription(String text) {
    try {
      Yaml yaml = new Yaml(new SafeConstructor(new LoaderOptions()));
      Object loaded = yaml.load(text);
      if (loaded instanceof Map<?, ?> map) {
        Object infoObj = map.get("info");
        if (infoObj instanceof Map<?, ?> info) {
          String description = stringVal(info.get("description"));
          return description != null ? description : "";
        }
      }
    } catch (Exception ignored) {
      return "";
    }
    return "";
  }

  private static List<LoadedRow> toPages(
      List<LoadedOp> ops, OpenApiFetch fetch, VirtualSiteConfig config)
      throws VirtualSiteException {
    if (ops.isEmpty()) {
      VersionSpec version = resolveVersion(config, "openapi-yaml fixture");
      String title = infoTitle(fetch.text());
      String id = "openapi";
      Path relative = resolvePagePath("", id, version.path(), "openapi-yaml fixture");
      VirtualItemRef ref = new VirtualItemRef(id, version.id(), relative, 1, title);
      VirtualFrontmatter fm =
          new VirtualFrontmatter(id, title, "", version.id(), true, 1, List.of(), false);
      String body = assembleInfoBody(title, infoDescription(fetch.text()), fetch.text());
      return List.of(new LoadedRow(ref, fm, body, fetch.sourcePath()));
    }
    List<LoadedRow> pages = new ArrayList<>(ops.size());
    for (LoadedOp op : ops) {
      pages.add(toLoadedRow(op, config, fetch.sourcePath()));
    }
    return pages;
  }

  private static LoadedRow toLoadedRow(LoadedOp op, VirtualSiteConfig config, Path sourcePath)
      throws VirtualSiteException {
    String where = "openapi-yaml operation[" + (op.order() - 1) + "]";
    String id = idForOp(op);
    String title = titleForOp(op);
    VersionSpec version = resolveVersion(config, where);
    Path relative = resolvePagePath("", slugForPath(id), version.path(), where);
    VirtualItemRef ref = new VirtualItemRef(id, version.id(), relative, op.order(), title);
    VirtualFrontmatter fm =
        new VirtualFrontmatter(id, title, "", version.id(), true, op.order(), List.of(), false);
    return new LoadedRow(ref, fm, assembleOpBody(op), sourcePath);
  }

  static String idForOp(LoadedOp op) {
    if (op.operationId() != null && !op.operationId().isBlank()) {
      String slug = slugForPath(op.operationId());
      return slug + "-" + op.order();
    }
    String slug = slugForPath(op.method() + "-" + op.path());
    return slug + "-" + op.order();
  }

  static String titleForOp(LoadedOp op) {
    if (op.summary() != null && !op.summary().isBlank()) {
      return op.summary();
    }
    if (op.operationId() != null && !op.operationId().isBlank()) {
      return op.operationId();
    }
    return op.method() + " " + op.path();
  }

  static String assembleOpBody(LoadedOp op) {
    StringBuilder body = new StringBuilder();
    body.append("## ").append(titleForOp(op)).append("\n\n");
    body.append("Method: ").append(op.method()).append('\n');
    body.append("Path: ").append(op.path()).append('\n');
    if (op.operationId() != null && !op.operationId().isBlank()) {
      body.append("operationId: ").append(op.operationId()).append('\n');
    }
    if (op.description() != null && !op.description().isBlank()) {
      body.append('\n').append(op.description()).append('\n');
    }
    return body.toString();
  }

  static String assembleInfoBody(String title, String description, String raw) {
    StringBuilder body = new StringBuilder();
    body.append("## ").append(title).append("\n\n");
    if (description != null && !description.isBlank()) {
      body.append(description).append("\n\n");
    }
    body.append("```\n").append(raw.strip()).append("\n```\n");
    return body.toString();
  }

  static String slugForPath(String id) {
    if (id == null || id.isBlank()) {
      return "openapi";
    }
    String candidate = id.trim();
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < candidate.length(); i++) {
      char c = candidate.charAt(i);
      if (Character.isLetterOrDigit(c) || c == '.' || c == '_' || c == '-') {
        sb.append(c);
      } else if (sb.length() > 0 && sb.charAt(sb.length() - 1) != '-') {
        sb.append('-');
      }
    }
    String slug = sb.toString();
    while (slug.startsWith("-") || slug.startsWith(".")) {
      slug = slug.substring(1);
    }
    while (slug.endsWith("-") || slug.endsWith(".")) {
      slug = slug.substring(0, slug.length() - 1);
    }
    return slug.isEmpty() ? "openapi" : slug;
  }

  static void rejectRemoteRefs(Object node, String where) throws VirtualSiteException {
    if (node instanceof Map<?, ?> map) {
      for (Map.Entry<?, ?> e : map.entrySet()) {
        if (e.getKey() != null && "$ref".equals(String.valueOf(e.getKey()))) {
          String ref = stringVal(e.getValue());
          if (ref != null && !ref.isBlank()) {
            if (!ref.startsWith("#")) {
              throw new VirtualSiteException(
                  where
                      + " $ref must be a local fragment (no live HTTP fetch). Rejected: "
                      + ref);
            }
          }
        }
        rejectRemoteRefs(e.getValue(), where);
      }
    } else if (node instanceof List<?> list) {
      for (Object item : list) {
        rejectRemoteRefs(item, where);
      }
    }
  }

  static void rejectRemoteOrCloudUrl(String raw, String where) throws VirtualSiteException {
    if (raw == null || raw.isBlank()) {
      return;
    }
    String trimmed = raw.trim();
    String lower = trimmed.toLowerCase(Locale.ROOT);
    if (lower.contains("://")) {
      throw new VirtualSiteException(
          where
              + " must be a local path or site-relative value (no live HTTP fetch). Rejected: "
              + trimmed);
    }
    int colon = trimmed.indexOf(':');
    if (colon > 1) {
      String scheme = trimmed.substring(0, colon);
      boolean uriScheme =
          !scheme.isEmpty()
              && scheme
                  .chars()
                  .allMatch(
                      ch -> Character.isLetterOrDigit(ch) || ch == '+' || ch == '.' || ch == '-');
      if (uriScheme) {
        throw new VirtualSiteException(
            where
                + " must be a local path or site-relative value (no live HTTP fetch). Rejected: "
                + trimmed);
      }
    }
  }

  private static VersionSpec resolveVersion(VirtualSiteConfig config, String where)
      throws VirtualSiteException {
    List<VersionSpec> versions = config.versions();
    if (versions == null || versions.isEmpty()) {
      throw new VirtualSiteException(
          "openapi-yaml config must declare at least one version (" + where + ")");
    }
    for (VersionSpec v : versions) {
      if (v.defaultVersion()) {
        return v;
      }
    }
    return versions.get(0);
  }

  static Path resolvePagePath(String pathRaw, String id, String versionPath, String where)
      throws VirtualSiteException {
    List<String> segments;
    if (pathRaw == null || pathRaw.isBlank()) {
      segments = List.of(versionPath, id + ".html");
    } else {
      if (pathRaw.indexOf('\0') >= 0) {
        throw new VirtualSiteException("openapi-yaml 'path' must not contain NUL in " + where);
      }
      String logical = pathRaw.trim().replace('\\', '/');
      if (logical.startsWith("/") || looksAbsoluteWindows(logical)) {
        throw new VirtualSiteException("openapi-yaml 'path' must be relative in " + where);
      }
      segments = new ArrayList<>();
      for (String seg : logical.split("/")) {
        if (seg.isEmpty() || ".".equals(seg)) {
          continue;
        }
        if ("..".equals(seg) || seg.indexOf('\0') >= 0) {
          throw new VirtualSiteException(
              "openapi-yaml 'path' must not contain '..' or NUL segments in " + where);
        }
        segments.add(seg);
      }
      if (segments.isEmpty()) {
        throw new VirtualSiteException("openapi-yaml 'path' is empty after normalize in " + where);
      }
      if (!segments.get(0).equals(versionPath)) {
        segments.add(0, versionPath);
      }
      String last = segments.get(segments.size() - 1);
      if (!last.contains(".")) {
        segments.set(segments.size() - 1, last + ".html");
      }
    }
    Path p = Path.of(segments.get(0));
    for (int i = 1; i < segments.size(); i++) {
      p = p.resolve(segments.get(i));
    }
    Path normalized = p.normalize();
    if (!PSVirtualSiteHelper.isSafeRootPath(normalized)
        || normalized.isAbsolute()
        || remainingParent(normalized)) {
      throw new VirtualSiteException("openapi-yaml 'path' is not a safe relative path in " + where);
    }
    return normalized;
  }

  private static boolean looksAbsoluteWindows(String logical) {
    return logical.length() >= 2
        && Character.isLetter(logical.charAt(0))
        && logical.charAt(1) == ':';
  }

  private static boolean remainingParent(Path path) {
    for (Path part : path) {
      if ("..".equals(part.toString())) {
        return true;
      }
    }
    return false;
  }

  private static String stringVal(Object o) {
    if (o == null) {
      return null;
    }
    if (o instanceof String s) {
      return s.trim();
    }
    return String.valueOf(o).trim();
  }

  private record OpenApiFetch(String text, Path sourcePath) {}

  record LoadedOp(
      String path, String method, String operationId, String summary, String description, int order) {}

  private record LoadedRow(
      VirtualItemRef ref, VirtualFrontmatter frontmatter, String body, Path sourcePath) {}
}
