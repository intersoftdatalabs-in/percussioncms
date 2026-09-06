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

import com.percussion.services.virtualsite.VirtualSiteConfig.LlmsSpec;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Virtual Site source: local llms.txt fixture ({@code llms-txt}).
 *
 * <p>Default file under the site root: {@code llms.txt}. {@code _config.yaml} {@code llms.file}
 * overrides the filename. {@code llms.url}, Git {@code virtual.remoteUrl}, credential properties,
 * cloud URLs, and markdown link hrefs with a remote/cloud scheme are rejected — this slice is a
 * local fixture only (no live HTTP fetch).
 *
 * <p>Each markdown list link {@code - [title](href)} maps into assemble {@code id}/{@code
 * title}/{@code body}. A fixture with no links still emits one page from the file so CLI assemble
 * writes HTML ({@code pagesWritten > 0}).
 *
 * <p>Stateless: {@link #discover} and {@link #load} always re-read the current local fixture via
 * {@link Files#readString}. No path/mtime parse cache is kept on the instance or in statics — a
 * second build in the same JVM after an llms ({@code llms.file} / default {@code llms.txt}) or
 * {@code _config.yaml} edit must see the new bytes. File watchers are not used; {@code
 * _config.yaml} is reloaded by {@link PSVirtualSiteBuildService}, not this source.
 */
public class PSLlmsTxtVirtualSiteSource implements IPSVirtualSiteSource {

  static final String DEFAULT_LLMS_FILE = "llms.txt";
  static final int MAX_LLMS_BYTES = 2_000_000;

  private static final Pattern LINK_LINE =
      Pattern.compile("^\\s*[-*]\\s+\\[([^\\]]*)\\]\\(([^)]*)\\)(?:\\s*:\\s*(.*))?\\s*$");

  @Override
  public String sourceType() {
    return VirtualSiteSourceType.LLMS_TXT.wireName();
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
      throw new VirtualSiteException("llms-txt item ref is required");
    }
    List<LoadedRow> rows = loadAllRows(config);
    for (LoadedRow row : rows) {
      if (row.ref().versionId().equals(ref.versionId()) && row.ref().id().equals(ref.id())) {
        return new VirtualItem(row.ref(), row.frontmatter(), row.body(), row.sourcePath());
      }
    }
    throw new VirtualSiteException(
        "Unknown llms-txt page id '" + ref.id() + "' in version " + ref.versionId());
  }

  private static List<LoadedRow> loadAllRows(VirtualSiteConfig config)
      throws IOException, VirtualSiteException {
    if (config == null) {
      throw new VirtualSiteException("Virtual Site config is required");
    }
    Path root = config.root();
    if (root == null || !PSVirtualSiteHelper.isSafeRootPath(root)) {
      throw new VirtualSiteException("llms-txt site root is missing or unsafe");
    }
    Path safeRoot = root.normalize();
    LlmsFetch fetch = readLlms(config, safeRoot);
    List<LlmsLink> links = parseLinks(fetch.text());
    List<LoadedRow> pages = toPages(links, fetch, config);
    if (pages.isEmpty()) {
      throw new VirtualSiteException(
          "llms-txt fixture produced no pages: "
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
            "Duplicate llms-txt id '"
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
            "Duplicate llms-txt path '"
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

  private static LlmsFetch readLlms(VirtualSiteConfig config, Path safeRoot)
      throws IOException, VirtualSiteException {
    LlmsSpec spec = config.llms();
    if (spec != null && spec.hasUrl()) {
      throw new VirtualSiteException(
          "llms.url is not supported (local llms.txt fixture only; no live HTTP fetch or remote"
              + " llms.txt URLs)");
    }
    if (spec != null && spec.hasFile()) {
      Path file = resolveLlmsFile(safeRoot, spec.file());
      return new LlmsFetch(readLlmsFile(file), file);
    }
    Path defaultFile = safeRoot.resolve(DEFAULT_LLMS_FILE).normalize();
    if (Files.isRegularFile(defaultFile) && defaultFile.startsWith(safeRoot)) {
      return new LlmsFetch(readLlmsFile(defaultFile), defaultFile);
    }
    throw new VirtualSiteException(
        "llms-txt fixture not found: expected "
            + DEFAULT_LLMS_FILE
            + " under "
            + safeRoot.toAbsolutePath().normalize());
  }

  private static String readLlmsFile(Path file) throws IOException, VirtualSiteException {
    if (!Files.isRegularFile(file)) {
      throw new VirtualSiteException(
          "llms-txt file not found: " + file.toAbsolutePath().normalize());
    }
    long size = Files.size(file);
    if (size > MAX_LLMS_BYTES) {
      throw new VirtualSiteException(
          "llms-txt file exceeds " + MAX_LLMS_BYTES + " bytes: " + file);
    }
    String text = Files.readString(file, StandardCharsets.UTF_8);
    if (text == null || text.isBlank()) {
      throw new VirtualSiteException("llms-txt fixture is empty");
    }
    if (text.indexOf('\0') >= 0) {
      throw new VirtualSiteException("llms-txt file must not contain NUL");
    }
    return text;
  }

  static Path resolveLlmsFile(Path root, String llmsFile) throws VirtualSiteException {
    if (llmsFile == null || llmsFile.isBlank()) {
      throw new VirtualSiteException("llms-txt file is blank");
    }
    if (llmsFile.indexOf('\0') >= 0) {
      throw new VirtualSiteException("llms-txt file must not contain NUL");
    }
    String logical = llmsFile.trim().replace('\\', '/');
    if (logical.startsWith("/") || looksAbsoluteWindows(logical)) {
      throw new VirtualSiteException("llms-txt file must be relative to the site root");
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
          throw new VirtualSiteException("llms-txt file must not contain '..' or NUL segments");
        }
        relative = relative.resolve(seg);
      }
      if (relative.getNameCount() == 0 || relative.toString().isEmpty()) {
        throw new VirtualSiteException("llms-txt file is empty after normalize");
      }
      resolved = safeRoot.resolve(relative).normalize();
    } catch (InvalidPathException e) {
      throw new VirtualSiteException("llms-txt file is not a valid path", e);
    }
    if (!resolved.startsWith(safeRoot)
        || !PSVirtualSiteHelper.isSafeRootPath(resolved)
        || remainingParent(resolved)) {
      throw new VirtualSiteException("llms-txt file escapes the site root");
    }
    return resolved;
  }

  static List<LlmsLink> parseLinks(String text) throws VirtualSiteException {
    if (text == null || text.isBlank()) {
      throw new VirtualSiteException("llms-txt fixture is empty");
    }
    List<LlmsLink> links = new ArrayList<>();
    String[] rawLines = text.split("\\r?\\n", -1);
    int order = 0;
    for (int i = 0; i < rawLines.length; i++) {
      String raw = rawLines[i];
      if (raw == null) {
        continue;
      }
      Matcher m = LINK_LINE.matcher(raw);
      if (!m.matches()) {
        continue;
      }
      String title = m.group(1).trim();
      String href = m.group(2).trim();
      String notes = m.group(3) != null ? m.group(3).trim() : "";
      if (title.isEmpty()) {
        throw new VirtualSiteException("llms-txt link title is required (line " + (i + 1) + ")");
      }
      if (href.isEmpty()) {
        throw new VirtualSiteException("llms-txt link href is required (line " + (i + 1) + ")");
      }
      rejectRemoteOrCloudUrl(href, "llms-txt link");
      order++;
      links.add(new LlmsLink(title, href, notes, order));
    }
    return links;
  }

  static String parseH1(String text) {
    if (text == null || text.isBlank()) {
      return "llms.txt";
    }
    String[] rawLines = text.split("\\r?\\n", -1);
    for (String raw : rawLines) {
      if (raw == null) {
        continue;
      }
      String line = raw.trim();
      if (line.startsWith("# ") && line.length() > 2) {
        String title = line.substring(2).trim();
        if (!title.isEmpty()) {
          return title;
        }
      }
    }
    return "llms.txt";
  }

  private static List<LoadedRow> toPages(
      List<LlmsLink> links, LlmsFetch fetch, VirtualSiteConfig config)
      throws VirtualSiteException {
    if (links.isEmpty()) {
      VersionSpec version = resolveVersion(config, "llms-txt fixture");
      String id = "llms";
      String title = parseH1(fetch.text());
      Path relative = resolvePagePath("", id, version.path(), "llms-txt fixture");
      VirtualItemRef ref = new VirtualItemRef(id, version.id(), relative, 1, title);
      VirtualFrontmatter fm =
          new VirtualFrontmatter(id, title, "", version.id(), true, 1, List.of(), false);
      String body = assembleWholeFileBody(fetch.text());
      return List.of(new LoadedRow(ref, fm, body, fetch.sourcePath()));
    }
    List<LoadedRow> pages = new ArrayList<>(links.size());
    for (LlmsLink link : links) {
      pages.add(toLoadedRow(link, config, fetch.sourcePath()));
    }
    return pages;
  }

  private static LoadedRow toLoadedRow(LlmsLink link, VirtualSiteConfig config, Path sourcePath)
      throws VirtualSiteException {
    String where = "llms-txt link[" + (link.order() - 1) + "]";
    String id = idForLink(link);
    String title = link.title();
    VersionSpec version = resolveVersion(config, where);
    Path relative = resolvePagePath("", slugForPath(id), version.path(), where);
    VirtualItemRef ref = new VirtualItemRef(id, version.id(), relative, link.order(), title);
    VirtualFrontmatter fm =
        new VirtualFrontmatter(id, title, "", version.id(), true, link.order(), List.of(), false);
    return new LoadedRow(ref, fm, assembleLinkBody(link), sourcePath);
  }

  static String idForLink(LlmsLink link) {
    String base = slugForPath(link.title());
    return base + "-" + link.order();
  }

  static String assembleLinkBody(LlmsLink link) {
    StringBuilder body = new StringBuilder();
    body.append("## ").append(link.title()).append("\n\n");
    body.append("Link: ").append(link.href()).append('\n');
    if (!link.notes().isEmpty()) {
      body.append('\n').append(link.notes()).append('\n');
    }
    return body.toString();
  }

  static String assembleWholeFileBody(String raw) {
    return "```\n" + raw.strip() + "\n```\n";
  }

  static String slugForPath(String id) {
    if (id == null || id.isBlank()) {
      return "llms";
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
    return slug.isEmpty() ? "llms" : slug;
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
          "llms-txt config must declare at least one version (" + where + ")");
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
        throw new VirtualSiteException("llms-txt 'path' must not contain NUL in " + where);
      }
      String logical = pathRaw.trim().replace('\\', '/');
      if (logical.startsWith("/") || looksAbsoluteWindows(logical)) {
        throw new VirtualSiteException("llms-txt 'path' must be relative in " + where);
      }
      segments = new ArrayList<>();
      for (String seg : logical.split("/")) {
        if (seg.isEmpty() || ".".equals(seg)) {
          continue;
        }
        if ("..".equals(seg) || seg.indexOf('\0') >= 0) {
          throw new VirtualSiteException(
              "llms-txt 'path' must not contain '..' or NUL segments in " + where);
        }
        segments.add(seg);
      }
      if (segments.isEmpty()) {
        throw new VirtualSiteException("llms-txt 'path' is empty after normalize in " + where);
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
      throw new VirtualSiteException("llms-txt 'path' is not a safe relative path in " + where);
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

  private record LlmsFetch(String text, Path sourcePath) {}

  record LlmsLink(String title, String href, String notes, int order) {}

  private record LoadedRow(
      VirtualItemRef ref, VirtualFrontmatter frontmatter, String body, Path sourcePath) {}
}
