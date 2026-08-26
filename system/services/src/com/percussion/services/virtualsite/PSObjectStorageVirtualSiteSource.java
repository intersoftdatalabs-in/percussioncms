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

import com.percussion.services.virtualsite.VirtualFrontmatterParser.Parsed;
import com.percussion.services.virtualsite.VirtualSiteConfig.ObjectsSpec;
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
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Virtual Site source: a local directory treated as an object-key bucket.
 *
 * <p>Object keys are portable relative paths under {@code virtual.rootPath} (logical {@code /},
 * NIO {@link Path} / {@link Files} only). Discover walks version folders for Markdown, HTML, and
 * JSON (catalog or single-page object), or uses an optional {@code objects.keys} list in {@code
 * _config.yaml}. Page {@code id} comes from frontmatter / JSON {@code id} or the filename stem;
 * {@code title} + {@code body} assemble like http-json / csv-filesystem.
 *
 * <p>No cloud SDK, access keys, or network. Git remotes are not used.
 *
 * <p>Stateless: {@link #discover} and {@link #load} always re-read current file bytes. No
 * path/mtime parse cache is kept on the instance or in statics.
 */
public class PSObjectStorageVirtualSiteSource implements IPSVirtualSiteSource {

  static final int MAX_OBJECT_BYTES = 2_000_000;
  private static final Set<String> PAGE_EXTENSIONS = Set.of(".md", ".html", ".htm", ".json");
  private static final Pattern HTML_TITLE =
      Pattern.compile("(?is)<title[^>]*>(.*?)</title>");

  @Override
  public String sourceType() {
    return VirtualSiteSourceType.OBJECT_STORAGE.wireName();
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
      throw new VirtualSiteException("object-storage item ref is required");
    }
    List<LoadedRow> rows = loadAllRows(config);
    for (LoadedRow row : rows) {
      if (row.ref().versionId().equals(ref.versionId()) && row.ref().id().equals(ref.id())) {
        return new VirtualItem(row.ref(), row.frontmatter(), row.body(), row.sourcePath());
      }
    }
    throw new VirtualSiteException(
        "Unknown object-storage page id '" + ref.id() + "' in version " + ref.versionId());
  }

  private static List<LoadedRow> loadAllRows(VirtualSiteConfig config)
      throws IOException, VirtualSiteException {
    if (config == null) {
      throw new VirtualSiteException("Virtual Site config is required");
    }
    Path root = config.root();
    if (root == null || !PSVirtualSiteHelper.isSafeRootPath(root)) {
      throw new VirtualSiteException("object-storage site root is missing or unsafe");
    }
    Path safeRoot = root.normalize();
    List<Path> objectFiles = listObjectFiles(config, safeRoot);
    List<LoadedRow> rows = new ArrayList<>();
    Map<String, Path> seenIds = new HashMap<>();
    for (Path abs : objectFiles) {
      List<LoadedRow> loaded = loadObjectFile(config, safeRoot, abs);
      for (LoadedRow row : loaded) {
        String idKey = row.ref().versionId() + "\0" + row.ref().id();
        Path previous = seenIds.put(idKey, row.ref().relativePath());
        if (previous != null) {
          throw new VirtualSiteException(
              "Duplicate object-storage id '"
                  + row.ref().id()
                  + "' in version "
                  + row.ref().versionId()
                  + ": "
                  + previous
                  + " and "
                  + row.ref().relativePath());
        }
        rows.add(row);
      }
    }
    return rows;
  }

  private static List<Path> listObjectFiles(VirtualSiteConfig config, Path safeRoot)
      throws IOException, VirtualSiteException {
    ObjectsSpec spec = config.objects();
    if (spec != null && spec.hasKeys()) {
      List<Path> files = new ArrayList<>();
      for (String key : spec.keys()) {
        Path abs = resolveObjectKey(safeRoot, key);
        if (!Files.isRegularFile(abs)) {
          throw new VirtualSiteException(
              "object-storage key not found: " + abs.toAbsolutePath().normalize());
        }
        files.add(abs);
      }
      return files;
    }
    List<Path> files = new ArrayList<>();
    List<VersionSpec> versions = config.versions();
    if (versions == null || versions.isEmpty()) {
      throw new VirtualSiteException("object-storage config must declare at least one version");
    }
    for (VersionSpec version : versions) {
      Path versionRoot = safeRoot.resolve(version.path()).normalize();
      if (!versionRoot.startsWith(safeRoot) || remainingParent(versionRoot)) {
        throw new VirtualSiteException("Version path escapes site root: " + version.path());
      }
      if (!Files.isDirectory(versionRoot)) {
        throw new VirtualSiteException("Version path not found: " + versionRoot);
      }
      try (Stream<Path> walk = Files.walk(versionRoot)) {
        walk.filter(Files::isRegularFile)
            .map(Path::normalize)
            .filter(p -> p.startsWith(safeRoot))
            .filter(p -> isPageObject(safeRoot, p))
            .sorted(Comparator.comparing(p -> p.toString().replace('\\', '/')))
            .forEach(files::add);
      }
    }
    return files;
  }

  private static boolean isPageObject(Path safeRoot, Path abs) {
    Path rel = safeRoot.relativize(abs);
    for (Path part : rel) {
      String name = part.toString();
      if (name.startsWith("_")
          || name.startsWith(".")
          || "assets".equalsIgnoreCase(name)) {
        return false;
      }
    }
    String fileName = abs.getFileName().toString().toLowerCase(Locale.ROOT);
    int dot = fileName.lastIndexOf('.');
    if (dot < 0) {
      return false;
    }
    return PAGE_EXTENSIONS.contains(fileName.substring(dot));
  }

  private static List<LoadedRow> loadObjectFile(
      VirtualSiteConfig config, Path safeRoot, Path abs)
      throws IOException, VirtualSiteException {
    Path safeAbs = abs.normalize();
    if (!safeAbs.startsWith(safeRoot)
        || !PSVirtualSiteHelper.isSafeRootPath(safeAbs)
        || remainingParent(safeAbs)) {
      throw new VirtualSiteException("object-storage file escapes the site root: " + abs);
    }
    if (!Files.isRegularFile(safeAbs)) {
      throw new VirtualSiteException(
          "object-storage object not found: " + safeAbs.toAbsolutePath().normalize());
    }
    long size = Files.size(safeAbs);
    if (size > MAX_OBJECT_BYTES) {
      throw new VirtualSiteException(
          "object-storage object exceeds " + MAX_OBJECT_BYTES + " bytes: " + safeAbs);
    }
    String text = Files.readString(safeAbs, StandardCharsets.UTF_8);
    Path rel = safeRoot.relativize(safeAbs);
    String label = rel.toString().replace('\\', '/');
    String ext = extension(safeAbs);
    if (".json".equals(ext)) {
      return loadJsonObject(config, text, safeAbs, label);
    }
    return List.of(loadTextObject(config, text, safeAbs, rel, label, ext));
  }

  private static LoadedRow loadTextObject(
      VirtualSiteConfig config,
      String text,
      Path sourcePath,
      Path relative,
      String label,
      String ext)
      throws VirtualSiteException {
    String stem = fileStem(sourcePath);
    VersionSpec version = versionForRelative(config, relative);
    Path pagePath = pageRelativePath(relative, ext);
    if (hasFrontmatter(text)) {
      Parsed parsed = VirtualFrontmatterParser.parse(text, version.id(), label);
      VirtualFrontmatter fm = parsed.frontmatter();
      VirtualItemRef ref =
          new VirtualItemRef(fm.id(), version.id(), pagePath, fm.order(), fm.title());
      return new LoadedRow(ref, fm, parsed.body(), sourcePath);
    }
    String title;
    if (".html".equals(ext) || ".htm".equals(ext)) {
      title = htmlTitleOrStem(text, stem);
    } else {
      title = firstHeadingOrStem(text, stem);
    }
    if (title.isBlank()) {
      throw new VirtualSiteException("object-storage 'title' is required in " + label);
    }
    VirtualItemRef ref = new VirtualItemRef(stem, version.id(), pagePath, 0, title);
    VirtualFrontmatter fm =
        new VirtualFrontmatter(stem, title, "", version.id(), true, 0, List.of(), false);
    return new LoadedRow(ref, fm, text != null ? text : "", sourcePath);
  }

  private static List<LoadedRow> loadJsonObject(
      VirtualSiteConfig config, String text, Path sourcePath, String label)
      throws VirtualSiteException {
    if (text == null || text.isBlank()) {
      throw new VirtualSiteException("object-storage JSON is empty: " + label);
    }
    JSONObject root;
    try {
      root = new JSONObject(text);
    } catch (JSONException e) {
      throw new VirtualSiteException(
          "object-storage JSON is not an object in " + label + ": " + e.getMessage(), e);
    }
    if (root.has("pages") && !root.isNull("pages")) {
      JSONArray pages;
      try {
        pages = root.getJSONArray("pages");
      } catch (JSONException e) {
        throw new VirtualSiteException("object-storage pages must be a JSON array in " + label, e);
      }
      List<LoadedRow> rows = new ArrayList<>(pages.length());
      for (int i = 0; i < pages.length(); i++) {
        Object item = pages.opt(i);
        if (!(item instanceof JSONObject page)) {
          throw new VirtualSiteException(
              "object-storage pages[" + i + "] must be a JSON object in " + label);
        }
        rows.add(toCatalogRow(page, config, sourcePath, label + " pages[" + i + "]"));
      }
      return rows;
    }
    return List.of(toCatalogRow(root, config, sourcePath, label));
  }

  private static LoadedRow toCatalogRow(
      JSONObject page, VirtualSiteConfig config, Path sourcePath, String where)
      throws VirtualSiteException {
    String id = stringField(page, "id").trim();
    String title = stringField(page, "title").trim();
    String body = stringField(page, "body");
    String pathRaw = stringField(page, "path").trim();
    String orderRaw = stringField(page, "order").trim();
    String versionRaw = stringField(page, "version").trim();
    if (id.isEmpty()) {
      throw new VirtualSiteException("object-storage 'id' is required in " + where);
    }
    if (title.isEmpty()) {
      throw new VirtualSiteException("object-storage 'title' is required in " + where);
    }
    VersionSpec version = resolveVersion(config, versionRaw, where);
    int order = parseOrder(orderRaw, where);
    Path relative = resolvePagePath(pathRaw, id, version.path(), where);
    VirtualItemRef ref = new VirtualItemRef(id, version.id(), relative, order, title);
    VirtualFrontmatter fm =
        new VirtualFrontmatter(id, title, "", version.id(), true, order, List.of(), false);
    return new LoadedRow(ref, fm, body != null ? body : "", sourcePath);
  }

  /**
   * Resolve an operator-supplied object key under the site root. Logical {@code /} is the key
   * separator; remaining {@code ..}, NUL, and absolute roots are rejected.
   */
  static Path resolveObjectKey(Path root, String objectKey) throws VirtualSiteException {
    if (objectKey == null || objectKey.isBlank()) {
      throw new VirtualSiteException("object-storage key is blank");
    }
    if (objectKey.indexOf('\0') >= 0) {
      throw new VirtualSiteException("object-storage key must not contain NUL");
    }
    String logical = objectKey.trim().replace('\\', '/');
    if (logical.startsWith("/") || looksAbsoluteWindows(logical)) {
      throw new VirtualSiteException("object-storage key must be relative to the site root");
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
              "object-storage key must not contain '..' or NUL segments");
        }
        relative = relative.resolve(seg);
      }
      if (relative.getNameCount() == 0 || relative.toString().isEmpty()) {
        throw new VirtualSiteException("object-storage key is empty after normalize");
      }
      resolved = safeRoot.resolve(relative).normalize();
    } catch (InvalidPathException e) {
      throw new VirtualSiteException("object-storage key is not a valid path", e);
    }
    if (!resolved.startsWith(safeRoot)
        || !PSVirtualSiteHelper.isSafeRootPath(resolved)
        || remainingParent(resolved)) {
      throw new VirtualSiteException("object-storage key escapes the site root");
    }
    return resolved;
  }

  /**
   * Map optional JSON {@code path} to a site-root-relative page path. Omitted path defaults to
   * {@code {version}/{id}.html}.
   */
  static Path resolvePagePath(String pathRaw, String id, String versionPath, String where)
      throws VirtualSiteException {
    List<String> segments;
    if (pathRaw == null || pathRaw.isBlank()) {
      segments = List.of(versionPath, id + ".html");
    } else {
      if (pathRaw.indexOf('\0') >= 0) {
        throw new VirtualSiteException("object-storage 'path' must not contain NUL in " + where);
      }
      String logical = pathRaw.trim().replace('\\', '/');
      if (logical.startsWith("/") || looksAbsoluteWindows(logical)) {
        throw new VirtualSiteException("object-storage 'path' must be relative in " + where);
      }
      segments = new ArrayList<>();
      for (String seg : logical.split("/")) {
        if (seg.isEmpty() || ".".equals(seg)) {
          continue;
        }
        if ("..".equals(seg) || seg.indexOf('\0') >= 0) {
          throw new VirtualSiteException(
              "object-storage 'path' must not contain '..' or NUL segments in " + where);
        }
        segments.add(seg);
      }
      if (segments.isEmpty()) {
        throw new VirtualSiteException(
            "object-storage 'path' is empty after normalize in " + where);
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
      throw new VirtualSiteException(
          "object-storage 'path' is not a safe relative path in " + where);
    }
    return normalized;
  }

  private static VersionSpec resolveVersion(
      VirtualSiteConfig config, String versionRaw, String where) throws VirtualSiteException {
    List<VersionSpec> versions = config.versions();
    if (versions == null || versions.isEmpty()) {
      throw new VirtualSiteException("object-storage config must declare at least one version");
    }
    if (versionRaw == null || versionRaw.isBlank()) {
      return defaultVersion(versions);
    }
    String wanted = versionRaw.trim();
    for (VersionSpec v : versions) {
      if (wanted.equals(v.id())) {
        return v;
      }
    }
    throw new VirtualSiteException(
        "object-storage version '" + wanted + "' is not declared in _config.yaml (" + where + ")");
  }

  private static VersionSpec versionForRelative(VirtualSiteConfig config, Path relative)
      throws VirtualSiteException {
    List<VersionSpec> versions = config.versions();
    if (versions == null || versions.isEmpty()) {
      throw new VirtualSiteException("object-storage config must declare at least one version");
    }
    if (relative.getNameCount() > 0) {
      String first = relative.getName(0).toString();
      for (VersionSpec v : versions) {
        if (first.equals(v.path()) || first.equals(v.id())) {
          return v;
        }
      }
    }
    return defaultVersion(versions);
  }

  private static VersionSpec defaultVersion(List<VersionSpec> versions) {
    for (VersionSpec v : versions) {
      if (v.defaultVersion()) {
        return v;
      }
    }
    return versions.get(0);
  }

  private static int parseOrder(String raw, String where) throws VirtualSiteException {
    if (raw == null || raw.isBlank()) {
      return 0;
    }
    try {
      return Integer.parseInt(raw.trim());
    } catch (NumberFormatException e) {
      throw new VirtualSiteException("object-storage 'order' must be an integer in " + where, e);
    }
  }

  private static String stringField(JSONObject page, String key) {
    if (page == null || !page.has(key) || page.isNull(key)) {
      return "";
    }
    Object value = page.opt(key);
    if (value == null) {
      return "";
    }
    if (value instanceof Number n) {
      if (n.doubleValue() == n.longValue()) {
        return Long.toString(n.longValue());
      }
      return n.toString();
    }
    return String.valueOf(value).trim();
  }

  static String fileStem(Path file) {
    String name = file.getFileName().toString();
    int dot = name.lastIndexOf('.');
    if (dot <= 0) {
      return name;
    }
    return name.substring(0, dot);
  }

  static String firstHeadingOrStem(String text, String stem) {
    if (text == null) {
      return stem;
    }
    for (String line : text.replace("\r\n", "\n").replace('\r', '\n').split("\n", -1)) {
      String t = line.trim();
      if (t.startsWith("#")) {
        String heading = t.replaceFirst("^#+\\s*", "").trim();
        if (!heading.isEmpty()) {
          return heading;
        }
      }
    }
    return stem;
  }

  static String htmlTitleOrStem(String html, String stem) {
    if (html == null || html.isBlank()) {
      return stem;
    }
    Matcher m = HTML_TITLE.matcher(html);
    if (m.find()) {
      String title = m.group(1).replaceAll("\\s+", " ").trim();
      if (!title.isEmpty()) {
        return title;
      }
    }
    return stem;
  }

  private static boolean hasFrontmatter(String text) {
    if (text == null) {
      return false;
    }
    String normalized = text.replace("\r\n", "\n").replace('\r', '\n');
    return normalized.startsWith("---\n") || normalized.equals("---");
  }

  private static Path pageRelativePath(Path relative, String ext) {
    if (!".json".equals(ext)) {
      return relative;
    }
    Path parent = relative.getParent();
    String htmlName = fileStem(relative) + ".html";
    return parent == null ? Path.of(htmlName) : parent.resolve(htmlName);
  }

  private static String extension(Path file) {
    String name = file.getFileName().toString().toLowerCase(Locale.ROOT);
    int dot = name.lastIndexOf('.');
    return dot < 0 ? "" : name.substring(dot);
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

  private record LoadedRow(
      VirtualItemRef ref, VirtualFrontmatter frontmatter, String body, Path sourcePath) {}
}
