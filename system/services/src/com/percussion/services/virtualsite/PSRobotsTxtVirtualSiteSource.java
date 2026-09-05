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

import com.percussion.services.virtualsite.VirtualSiteConfig.RobotsSpec;
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

/**
 * Virtual Site source: local robots.txt fixture ({@code robots-txt}).
 *
 * <p>Default file under the site root: {@code robots.txt}. {@code _config.yaml} {@code
 * robots.file} overrides the filename. {@code robots.url}, Git {@code virtual.remoteUrl},
 * credential properties, cloud URLs, and non-local {@code Sitemap:} values are rejected — this
 * slice is a local fixture only (no live crawl, no robots.txt fetch over the network).
 *
 * <p>Each {@code User-agent} group maps into assemble {@code id}/{@code title}/{@code body}. A
 * fixture with no {@code User-agent} still emits one page from the file so CLI assemble writes
 * HTML ({@code pagesWritten > 0}).
 *
 * <p>Stateless: {@link #discover} and {@link #load} always re-read the current local fixture via
 * {@link Files#readString}. No path/mtime parse cache is kept on the instance or in statics — a
 * second build in the same JVM after a robots ({@code robots.file} / default {@code robots.txt})
 * or {@code _config.yaml} edit must see the new bytes. File watchers are not used; {@code
 * _config.yaml} is reloaded by {@link PSVirtualSiteBuildService}, not this source.
 */
public class PSRobotsTxtVirtualSiteSource implements IPSVirtualSiteSource {

  static final String DEFAULT_ROBOTS_FILE = "robots.txt";
  static final int MAX_ROBOTS_BYTES = 2_000_000;

  @Override
  public String sourceType() {
    return VirtualSiteSourceType.ROBOTS_TXT.wireName();
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
      throw new VirtualSiteException("robots-txt item ref is required");
    }
    List<LoadedRow> rows = loadAllRows(config);
    for (LoadedRow row : rows) {
      if (row.ref().versionId().equals(ref.versionId()) && row.ref().id().equals(ref.id())) {
        return new VirtualItem(row.ref(), row.frontmatter(), row.body(), row.sourcePath());
      }
    }
    throw new VirtualSiteException(
        "Unknown robots-txt page id '" + ref.id() + "' in version " + ref.versionId());
  }

  private static List<LoadedRow> loadAllRows(VirtualSiteConfig config)
      throws IOException, VirtualSiteException {
    if (config == null) {
      throw new VirtualSiteException("Virtual Site config is required");
    }
    Path root = config.root();
    if (root == null || !PSVirtualSiteHelper.isSafeRootPath(root)) {
      throw new VirtualSiteException("robots-txt site root is missing or unsafe");
    }
    Path safeRoot = root.normalize();
    RobotsFetch fetch = readRobots(config, safeRoot);
    List<AgentGroup> groups = parseGroups(fetch.text());
    List<LoadedRow> pages = toPages(groups, fetch, config);
    if (pages.isEmpty()) {
      throw new VirtualSiteException(
          "robots-txt fixture produced no pages: "
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
            "Duplicate robots-txt id '"
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
            "Duplicate robots-txt path '"
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

  private static RobotsFetch readRobots(VirtualSiteConfig config, Path safeRoot)
      throws IOException, VirtualSiteException {
    RobotsSpec spec = config.robots();
    if (spec != null && spec.hasUrl()) {
      throw new VirtualSiteException(
          "robots.url is not supported (local robots.txt fixture only; no live crawl or remote"
              + " robots.txt URLs)");
    }
    if (spec != null && spec.hasFile()) {
      Path file = resolveRobotsFile(safeRoot, spec.file());
      return new RobotsFetch(readRobotsFile(file), file);
    }
    Path defaultFile = safeRoot.resolve(DEFAULT_ROBOTS_FILE).normalize();
    if (Files.isRegularFile(defaultFile) && defaultFile.startsWith(safeRoot)) {
      return new RobotsFetch(readRobotsFile(defaultFile), defaultFile);
    }
    throw new VirtualSiteException(
        "robots-txt fixture not found: expected "
            + DEFAULT_ROBOTS_FILE
            + " under "
            + safeRoot.toAbsolutePath().normalize());
  }

  private static String readRobotsFile(Path file) throws IOException, VirtualSiteException {
    if (!Files.isRegularFile(file)) {
      throw new VirtualSiteException(
          "robots-txt file not found: " + file.toAbsolutePath().normalize());
    }
    long size = Files.size(file);
    if (size > MAX_ROBOTS_BYTES) {
      throw new VirtualSiteException(
          "robots-txt file exceeds " + MAX_ROBOTS_BYTES + " bytes: " + file);
    }
    String text = Files.readString(file, StandardCharsets.UTF_8);
    if (text == null || text.isBlank()) {
      throw new VirtualSiteException("robots-txt fixture is empty");
    }
    if (text.indexOf('\0') >= 0) {
      throw new VirtualSiteException("robots-txt file must not contain NUL");
    }
    return text;
  }

  static Path resolveRobotsFile(Path root, String robotsFile) throws VirtualSiteException {
    if (robotsFile == null || robotsFile.isBlank()) {
      throw new VirtualSiteException("robots-txt file is blank");
    }
    if (robotsFile.indexOf('\0') >= 0) {
      throw new VirtualSiteException("robots-txt file must not contain NUL");
    }
    String logical = robotsFile.trim().replace('\\', '/');
    if (logical.startsWith("/") || looksAbsoluteWindows(logical)) {
      throw new VirtualSiteException("robots-txt file must be relative to the site root");
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
          throw new VirtualSiteException("robots-txt file must not contain '..' or NUL segments");
        }
        relative = relative.resolve(seg);
      }
      if (relative.getNameCount() == 0 || relative.toString().isEmpty()) {
        throw new VirtualSiteException("robots-txt file is empty after normalize");
      }
      resolved = safeRoot.resolve(relative).normalize();
    } catch (InvalidPathException e) {
      throw new VirtualSiteException("robots-txt file is not a valid path", e);
    }
    if (!resolved.startsWith(safeRoot)
        || !PSVirtualSiteHelper.isSafeRootPath(resolved)
        || remainingParent(resolved)) {
      throw new VirtualSiteException("robots-txt file escapes the site root");
    }
    return resolved;
  }

  static List<AgentGroup> parseGroups(String text) throws VirtualSiteException {
    if (text == null || text.isBlank()) {
      throw new VirtualSiteException("robots-txt fixture is empty");
    }
    List<String> sitemaps = new ArrayList<>();
    List<AgentGroup> groups = new ArrayList<>();
    List<String> agents = new ArrayList<>();
    List<String> rules = new ArrayList<>();
    int order = 0;
    String[] rawLines = text.split("\\r?\\n", -1);
    for (int i = 0; i < rawLines.length; i++) {
      String raw = rawLines[i];
      if (raw == null) {
        continue;
      }
      String line = stripComment(raw).trim();
      if (line.isEmpty()) {
        continue;
      }
      int colon = fieldColon(line);
      if (colon < 0) {
        throw new VirtualSiteException(
            "robots-txt line must be field: value (line " + (i + 1) + ")");
      }
      String name = line.substring(0, colon).trim();
      String value = line.substring(colon + 1).trim();
      if (name.equalsIgnoreCase("Sitemap")) {
        rejectRemoteOrCloudUrl(value, "robots-txt Sitemap");
        if (value.isEmpty()) {
          throw new VirtualSiteException("robots-txt Sitemap value is required");
        }
        sitemaps.add(value);
        continue;
      }
      if (name.equalsIgnoreCase("User-agent")) {
        if (value.isEmpty()) {
          throw new VirtualSiteException("robots-txt User-agent is required");
        }
        if (!agents.isEmpty() && !rules.isEmpty()) {
          order++;
          groups.add(new AgentGroup(List.copyOf(agents), List.copyOf(rules), order));
          agents = new ArrayList<>();
          rules = new ArrayList<>();
        }
        agents.add(value);
        continue;
      }
      if (agents.isEmpty()) {
        throw new VirtualSiteException(
            "robots-txt rule before User-agent (line " + (i + 1) + ")");
      }
      rules.add(name + ": " + value);
    }
    if (!agents.isEmpty()) {
      order++;
      groups.add(new AgentGroup(List.copyOf(agents), List.copyOf(rules), order));
    }
    if (!sitemaps.isEmpty() && !groups.isEmpty()) {
      List<AgentGroup> withMaps = new ArrayList<>(groups.size());
      for (AgentGroup g : groups) {
        withMaps.add(g.withSitemaps(sitemaps));
      }
      return withMaps;
    }
    if (groups.isEmpty() && !sitemaps.isEmpty()) {
      return List.of(new AgentGroup(List.of("robots"), List.of(), 1, List.copyOf(sitemaps), true));
    }
    return groups;
  }

  private static List<LoadedRow> toPages(
      List<AgentGroup> groups, RobotsFetch fetch, VirtualSiteConfig config)
      throws VirtualSiteException {
    if (groups.isEmpty()) {
      VersionSpec version = resolveVersion(config, "robots-txt fixture");
      String id = "robots";
      String title = "robots.txt";
      Path relative = resolvePagePath("", id, version.path(), "robots-txt fixture");
      VirtualItemRef ref = new VirtualItemRef(id, version.id(), relative, 1, title);
      VirtualFrontmatter fm =
          new VirtualFrontmatter(id, title, "", version.id(), true, 1, List.of(), false);
      String body = assembleWholeFileBody(fetch.text());
      return List.of(new LoadedRow(ref, fm, body, fetch.sourcePath()));
    }
    List<LoadedRow> pages = new ArrayList<>(groups.size());
    for (AgentGroup group : groups) {
      pages.add(toLoadedRow(group, config, fetch.sourcePath(), fetch.text()));
    }
    return pages;
  }

  private static LoadedRow toLoadedRow(
      AgentGroup group, VirtualSiteConfig config, Path sourcePath, String rawText)
      throws VirtualSiteException {
    String where = "robots-txt User-agent[" + (group.order() - 1) + "]";
    String id = group.wholeFile() ? "robots" : idForGroup(group);
    String title = group.wholeFile() ? "robots.txt" : titleForGroup(group);
    VersionSpec version = resolveVersion(config, where);
    Path relative = resolvePagePath("", slugForPath(id), version.path(), where);
    VirtualItemRef ref = new VirtualItemRef(id, version.id(), relative, group.order(), title);
    VirtualFrontmatter fm =
        new VirtualFrontmatter(id, title, "", version.id(), true, group.order(), List.of(), false);
    String body =
        group.wholeFile()
            ? assembleWholeFileBody(rawText)
            : assembleGroupBody(group);
    return new LoadedRow(ref, fm, body, sourcePath);
  }

  static String idForGroup(AgentGroup group) {
    StringBuilder joined = new StringBuilder();
    for (String agent : group.agents()) {
      if (joined.length() > 0) {
        joined.append('-');
      }
      joined.append(slugForPath(agent));
    }
    String base = joined.length() == 0 ? "agent" : joined.toString();
    return base + "-" + group.order();
  }

  static String titleForGroup(AgentGroup group) {
    return "User-agent: " + String.join(", ", group.agents());
  }

  static String assembleGroupBody(AgentGroup group) {
    StringBuilder body = new StringBuilder();
    body.append("## ").append(titleForGroup(group)).append("\n\n");
    if (group.rules().isEmpty()) {
      body.append("(no Allow / Disallow rules)\n");
    } else {
      for (String rule : group.rules()) {
        body.append("- ").append(rule).append('\n');
      }
    }
    if (!group.sitemaps().isEmpty()) {
      body.append('\n');
      for (String sitemap : group.sitemaps()) {
        body.append("Sitemap: ").append(sitemap).append('\n');
      }
    }
    return body.toString();
  }

  static String assembleWholeFileBody(String raw) {
    return "```\n" + raw.strip() + "\n```\n";
  }

  static String slugForPath(String id) {
    if (id == null || id.isBlank()) {
      return "agent";
    }
    String candidate = id.trim();
    if ("*".equals(candidate)) {
      return "star";
    }
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
    return slug.isEmpty() ? "agent" : slug;
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
              + " must be a local path or site-relative value (no live crawl). Rejected: "
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
                + " must be a local path or site-relative value (no live crawl). Rejected: "
                + trimmed);
      }
    }
  }

  static String stripComment(String line) {
    boolean inQuote = false;
    for (int i = 0; i < line.length(); i++) {
      char c = line.charAt(i);
      if (c == '"') {
        inQuote = !inQuote;
      } else if (c == '#' && !inQuote) {
        return line.substring(0, i);
      }
    }
    return line;
  }

  static int fieldColon(String line) {
    for (int i = 0; i < line.length(); i++) {
      char c = line.charAt(i);
      if (c == ':') {
        return i;
      }
      if (c == ' ' || c == '\t') {
        return -1;
      }
    }
    return -1;
  }

  private static VersionSpec resolveVersion(VirtualSiteConfig config, String where)
      throws VirtualSiteException {
    List<VersionSpec> versions = config.versions();
    if (versions == null || versions.isEmpty()) {
      throw new VirtualSiteException(
          "robots-txt config must declare at least one version (" + where + ")");
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
        throw new VirtualSiteException("robots-txt 'path' must not contain NUL in " + where);
      }
      String logical = pathRaw.trim().replace('\\', '/');
      if (logical.startsWith("/") || looksAbsoluteWindows(logical)) {
        throw new VirtualSiteException("robots-txt 'path' must be relative in " + where);
      }
      segments = new ArrayList<>();
      for (String seg : logical.split("/")) {
        if (seg.isEmpty() || ".".equals(seg)) {
          continue;
        }
        if ("..".equals(seg) || seg.indexOf('\0') >= 0) {
          throw new VirtualSiteException(
              "robots-txt 'path' must not contain '..' or NUL segments in " + where);
        }
        segments.add(seg);
      }
      if (segments.isEmpty()) {
        throw new VirtualSiteException("robots-txt 'path' is empty after normalize in " + where);
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
      throw new VirtualSiteException("robots-txt 'path' is not a safe relative path in " + where);
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

  private record RobotsFetch(String text, Path sourcePath) {}

  record AgentGroup(
      List<String> agents,
      List<String> rules,
      int order,
      List<String> sitemaps,
      boolean wholeFile) {
    AgentGroup(List<String> agents, List<String> rules, int order) {
      this(agents, rules, order, List.of(), false);
    }

    AgentGroup withSitemaps(List<String> maps) {
      return new AgentGroup(agents, rules, order, List.copyOf(maps), wholeFile);
    }
  }

  private record LoadedRow(
      VirtualItemRef ref, VirtualFrontmatter frontmatter, String body, Path sourcePath) {}
}
