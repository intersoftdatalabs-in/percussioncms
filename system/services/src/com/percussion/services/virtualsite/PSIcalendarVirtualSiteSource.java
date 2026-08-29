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

import com.percussion.services.virtualsite.VirtualSiteConfig.IcalendarSpec;
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
import java.util.Map;

/**
 * Virtual Site source: local RFC 5545 iCalendar fixture ({@code calendar.ics}).
 *
 * <p>Default file under the site root: {@code calendar.ics}. {@code _config.yaml} {@code
 * icalendar.file} overrides the filename. {@code icalendar.url}, CalDAV, Git {@code
 * virtual.remoteUrl}, and credential properties are rejected — this slice is a local fixture
 * only (no live remotes, no secrets).
 *
 * <p>Each {@code VEVENT} maps {@code UID} + {@code SUMMARY} + {@code DTSTART} + {@code
 * DESCRIPTION} into assemble {@code id}/{@code title}/{@code body} like csv-filesystem /
 * rss-atom.
 *
 * <p>Stateless: {@link #discover} and {@link #load} always re-read the current local fixture via
 * {@link Files#readString}. No path/mtime parse cache is kept on the instance or in statics — a
 * second build in the same JVM after a calendar ({@code icalendar.file} / default {@code
 * calendar.ics}) or {@code _config.yaml} edit must see the new bytes. File watchers are not used;
 * {@code _config.yaml} is reloaded by {@link PSVirtualSiteBuildService}, not this source.
 */
public class PSIcalendarVirtualSiteSource implements IPSVirtualSiteSource {

  static final String DEFAULT_ICS_FILE = "calendar.ics";
  static final int MAX_CALENDAR_BYTES = 2_000_000;

  @Override
  public String sourceType() {
    return VirtualSiteSourceType.ICALENDAR.wireName();
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
      throw new VirtualSiteException("icalendar item ref is required");
    }
    List<LoadedRow> rows = loadAllRows(config);
    for (LoadedRow row : rows) {
      if (row.ref().versionId().equals(ref.versionId()) && row.ref().id().equals(ref.id())) {
        return new VirtualItem(row.ref(), row.frontmatter(), row.body(), row.sourcePath());
      }
    }
    throw new VirtualSiteException(
        "Unknown icalendar page id '" + ref.id() + "' in version " + ref.versionId());
  }

  private static List<LoadedRow> loadAllRows(VirtualSiteConfig config)
      throws IOException, VirtualSiteException {
    if (config == null) {
      throw new VirtualSiteException("Virtual Site config is required");
    }
    Path root = config.root();
    if (root == null || !PSVirtualSiteHelper.isSafeRootPath(root)) {
      throw new VirtualSiteException("icalendar site root is missing or unsafe");
    }
    Path safeRoot = root.normalize();
    CalendarFetch fetch = readCalendar(config, safeRoot);
    List<VEvent> events = parseVEvents(fetch.icsText());
    if (events.isEmpty()) {
      throw new VirtualSiteException(
          "icalendar fixture has no VEVENT components: "
              + fetch.sourcePath().toAbsolutePath().normalize());
    }
    List<LoadedRow> rows = new ArrayList<>();
    Map<String, Path> seenIds = new HashMap<>();
    Map<String, Path> seenPaths = new HashMap<>();
    for (VEvent event : events) {
      LoadedRow loaded = toLoadedRow(event, config, fetch.sourcePath());
      String idKey = loaded.ref().versionId() + "\0" + loaded.ref().id();
      Path previousId = seenIds.put(idKey, loaded.ref().relativePath());
      if (previousId != null) {
        throw new VirtualSiteException(
            "Duplicate icalendar id '"
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
            "Duplicate icalendar path '"
                + loaded.ref().relativePath()
                + "' in version "
                + loaded.ref().versionId());
      }
      rows.add(loaded);
    }
    return rows;
  }

  private static CalendarFetch readCalendar(VirtualSiteConfig config, Path safeRoot)
      throws IOException, VirtualSiteException {
    IcalendarSpec spec = config.icalendar();
    if (spec != null && spec.hasUrl()) {
      throw new VirtualSiteException(
          "icalendar.url is not supported (local calendar.ics fixture only; no CalDAV or live"
              + " remote .ics URLs)");
    }
    if (spec != null && spec.hasFile()) {
      Path file = resolveCalendarFile(safeRoot, spec.file());
      return new CalendarFetch(readCalendarFile(file), file);
    }
    Path defaultFile = safeRoot.resolve(DEFAULT_ICS_FILE).normalize();
    if (Files.isRegularFile(defaultFile) && defaultFile.startsWith(safeRoot)) {
      return new CalendarFetch(readCalendarFile(defaultFile), defaultFile);
    }
    throw new VirtualSiteException(
        "icalendar feed not found: expected "
            + DEFAULT_ICS_FILE
            + " under "
            + safeRoot.toAbsolutePath().normalize());
  }

  private static String readCalendarFile(Path file) throws IOException, VirtualSiteException {
    if (!Files.isRegularFile(file)) {
      throw new VirtualSiteException(
          "icalendar file not found: " + file.toAbsolutePath().normalize());
    }
    long size = Files.size(file);
    if (size > MAX_CALENDAR_BYTES) {
      throw new VirtualSiteException(
          "icalendar file exceeds " + MAX_CALENDAR_BYTES + " bytes: " + file);
    }
    return Files.readString(file, StandardCharsets.UTF_8);
  }

  static Path resolveCalendarFile(Path root, String calendarFile) throws VirtualSiteException {
    if (calendarFile == null || calendarFile.isBlank()) {
      throw new VirtualSiteException("icalendar file is blank");
    }
    if (calendarFile.indexOf('\0') >= 0) {
      throw new VirtualSiteException("icalendar file must not contain NUL");
    }
    String logical = calendarFile.trim().replace('\\', '/');
    if (logical.startsWith("/") || looksAbsoluteWindows(logical)) {
      throw new VirtualSiteException("icalendar file must be relative to the site root");
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
          throw new VirtualSiteException("icalendar file must not contain '..' or NUL segments");
        }
        relative = relative.resolve(seg);
      }
      if (relative.getNameCount() == 0 || relative.toString().isEmpty()) {
        throw new VirtualSiteException("icalendar file is empty after normalize");
      }
      resolved = safeRoot.resolve(relative).normalize();
    } catch (InvalidPathException e) {
      throw new VirtualSiteException("icalendar file is not a valid path", e);
    }
    if (!resolved.startsWith(safeRoot)
        || !PSVirtualSiteHelper.isSafeRootPath(resolved)
        || remainingParent(resolved)) {
      throw new VirtualSiteException("icalendar file escapes the site root");
    }
    return resolved;
  }

  private static List<VEvent> parseVEvents(String icsText) throws VirtualSiteException {
    if (icsText == null || icsText.isBlank()) {
      throw new VirtualSiteException("icalendar fixture is empty");
    }
    List<String> lines = unfoldLines(icsText);
    List<VEvent> events = new ArrayList<>();
    boolean inEvent = false;
    String uid = "";
    String summary = "";
    String dtstart = "";
    String description = "";
    int index = 0;
    for (String line : lines) {
      if (line.isEmpty()) {
        continue;
      }
      String name = propertyName(line);
      if ("BEGIN".equalsIgnoreCase(name) && "VEVENT".equalsIgnoreCase(propertyValue(line))) {
        if (inEvent) {
          throw new VirtualSiteException("icalendar nested VEVENT is not supported");
        }
        inEvent = true;
        uid = "";
        summary = "";
        dtstart = "";
        description = "";
        continue;
      }
      if ("END".equalsIgnoreCase(name) && "VEVENT".equalsIgnoreCase(propertyValue(line))) {
        if (!inEvent) {
          throw new VirtualSiteException("icalendar END:VEVENT without BEGIN:VEVENT");
        }
        inEvent = false;
        index++;
        events.add(
            new VEvent(uid, summary, dtstart, description, index, "icalendar VEVENT[" + (index - 1) + "]"));
        continue;
      }
      if (!inEvent) {
        continue;
      }
      if ("UID".equalsIgnoreCase(name)) {
        uid = unescapeText(propertyValue(line));
      } else if ("SUMMARY".equalsIgnoreCase(name)) {
        summary = unescapeText(propertyValue(line));
      } else if ("DTSTART".equalsIgnoreCase(name)) {
        dtstart = unescapeText(propertyValue(line));
      } else if ("DESCRIPTION".equalsIgnoreCase(name)) {
        description = unescapeText(propertyValue(line));
      }
    }
    if (inEvent) {
      throw new VirtualSiteException("icalendar VEVENT is not closed with END:VEVENT");
    }
    return events;
  }

  /**
   * RFC 5545 line unfolding: a line that starts with space or tab continues the previous line.
   */
  static List<String> unfoldLines(String icsText) {
    String[] raw = icsText.split("\\r?\\n", -1);
    List<String> out = new ArrayList<>();
    StringBuilder current = new StringBuilder();
    boolean have = false;
    for (String line : raw) {
      if (line == null) {
        continue;
      }
      if (!line.isEmpty() && (line.charAt(0) == ' ' || line.charAt(0) == '\t')) {
        if (!have) {
          current.append(line.substring(1));
          have = true;
        } else {
          current.append(line.substring(1));
        }
        continue;
      }
      if (have) {
        out.add(current.toString());
        current.setLength(0);
      }
      current.append(line);
      have = true;
    }
    if (have) {
      out.add(current.toString());
    }
    return out;
  }

  static String propertyName(String line) {
    int colon = line.indexOf(':');
    String left = colon >= 0 ? line.substring(0, colon) : line;
    int semi = left.indexOf(';');
    String name = semi >= 0 ? left.substring(0, semi) : left;
    return name.trim();
  }

  static String propertyValue(String line) {
    int colon = line.indexOf(':');
    if (colon < 0 || colon + 1 >= line.length()) {
      return "";
    }
    return line.substring(colon + 1);
  }

  static String unescapeText(String raw) {
    if (raw == null || raw.isEmpty()) {
      return "";
    }
    StringBuilder sb = new StringBuilder(raw.length());
    for (int i = 0; i < raw.length(); i++) {
      char c = raw.charAt(i);
      if (c == '\\' && i + 1 < raw.length()) {
        char next = raw.charAt(i + 1);
        if (next == 'n' || next == 'N') {
          sb.append('\n');
          i++;
          continue;
        }
        if (next == '\\' || next == ';' || next == ',') {
          sb.append(next);
          i++;
          continue;
        }
      }
      sb.append(c);
    }
    return sb.toString().trim();
  }

  private static LoadedRow toLoadedRow(VEvent event, VirtualSiteConfig config, Path sourcePath)
      throws VirtualSiteException {
    String where = event.where();
    String id = event.uid() == null ? "" : event.uid().trim();
    String title = event.summary() == null ? "" : event.summary().trim();
    if (id.isEmpty()) {
      throw new VirtualSiteException("icalendar UID is required in " + where);
    }
    if (title.isEmpty()) {
      throw new VirtualSiteException("icalendar SUMMARY is required in " + where);
    }
    VersionSpec version = resolveVersion(config, where);
    Path relative = resolvePagePath("", slugForPath(id), version.path(), where);
    VirtualItemRef ref = new VirtualItemRef(id, version.id(), relative, event.order(), title);
    String dtstart = event.dtstart() == null ? "" : event.dtstart().trim();
    String description = event.description() == null ? "" : event.description();
    VirtualFrontmatter fm =
        new VirtualFrontmatter(id, title, dtstart, version.id(), true, event.order(), List.of(), false);
    String body = assembleBody(dtstart, description);
    return new LoadedRow(ref, fm, body, sourcePath);
  }

  static String assembleBody(String dtstart, String description) {
    StringBuilder body = new StringBuilder();
    if (dtstart != null && !dtstart.isBlank()) {
      body.append("Starts: ").append(dtstart.trim()).append("\n\n");
    }
    if (description != null && !description.isBlank()) {
      body.append(description);
    }
    return body.toString();
  }

  static String slugForPath(String id) {
    if (id == null || id.isBlank()) {
      return "event";
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
    return slug.isEmpty() ? "event" : slug;
  }

  private static VersionSpec resolveVersion(VirtualSiteConfig config, String where)
      throws VirtualSiteException {
    List<VersionSpec> versions = config.versions();
    if (versions == null || versions.isEmpty()) {
      throw new VirtualSiteException(
          "icalendar config must declare at least one version (" + where + ")");
    }
    for (VersionSpec v : versions) {
      if (v.defaultVersion()) {
        return v;
      }
    }
    return versions.get(0);
  }

  /**
   * Map the optional page path to a site-root-relative path using NIO {@link Path#resolve}. Logical
   * {@code /} is a href-style separator. Omitted path defaults to {@code {id}.html} under the
   * version folder.
   */
  static Path resolvePagePath(String pathRaw, String id, String versionPath, String where)
      throws VirtualSiteException {
    List<String> segments;
    if (pathRaw == null || pathRaw.isBlank()) {
      segments = List.of(versionPath, id + ".html");
    } else {
      if (pathRaw.indexOf('\0') >= 0) {
        throw new VirtualSiteException("icalendar 'path' must not contain NUL in " + where);
      }
      String logical = pathRaw.trim().replace('\\', '/');
      if (logical.startsWith("/") || looksAbsoluteWindows(logical)) {
        throw new VirtualSiteException("icalendar 'path' must be relative in " + where);
      }
      segments = new ArrayList<>();
      for (String seg : logical.split("/")) {
        if (seg.isEmpty() || ".".equals(seg)) {
          continue;
        }
        if ("..".equals(seg) || seg.indexOf('\0') >= 0) {
          throw new VirtualSiteException(
              "icalendar 'path' must not contain '..' or NUL segments in " + where);
        }
        segments.add(seg);
      }
      if (segments.isEmpty()) {
        throw new VirtualSiteException("icalendar 'path' is empty after normalize in " + where);
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
      throw new VirtualSiteException("icalendar 'path' is not a safe relative path in " + where);
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

  private record CalendarFetch(String icsText, Path sourcePath) {}

  private record VEvent(
      String uid, String summary, String dtstart, String description, int order, String where) {}

  private record LoadedRow(
      VirtualItemRef ref, VirtualFrontmatter frontmatter, String body, Path sourcePath) {}
}
