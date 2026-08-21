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

import com.percussion.services.virtualsite.VirtualSiteConfig.VersionSpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Virtual Site source: CSV files (or a directory of CSVs) under each version path.
 *
 * <p>Column contract: required {@code id}, {@code title}, {@code body} (Markdown); optional {@code
 * path} and {@code order}. Missing required columns or blank {@code id}/{@code title} fail closed
 * with {@link VirtualSiteException}.
 *
 * <p>Stateless: {@link #discover} and {@link #load} always {@link Files#readString} the current
 * CSV bytes. No parse cache is kept on the instance or in statics.
 *
 * <p>Filesystem I/O uses portable NIO {@link Path} / {@link Files} only.
 */
public class PSCsvFilesystemVirtualSiteSource implements IPSVirtualSiteSource {

  @Override
  public String sourceType() {
    return VirtualSiteSourceType.CSV_FILESYSTEM.wireName();
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
      throw new VirtualSiteException("CSV item ref is required");
    }
    List<LoadedRow> rows = loadAllRows(config);
    for (LoadedRow row : rows) {
      if (row.ref().versionId().equals(ref.versionId()) && row.ref().id().equals(ref.id())) {
        Path csvAbs = row.csvFile();
        Path safeRoot = config.root().normalize();
        Path safeCsv = csvAbs.normalize();
        if (!safeCsv.startsWith(safeRoot)) {
          throw new VirtualSiteException("CSV file escapes site root: " + csvAbs);
        }
        return new VirtualItem(row.ref(), row.frontmatter(), row.body(), csvAbs);
      }
    }
    throw new VirtualSiteException(
        "Unknown CSV page id '" + ref.id() + "' in version " + ref.versionId());
  }

  private static List<LoadedRow> loadAllRows(VirtualSiteConfig config)
      throws IOException, VirtualSiteException {
    if (config == null) {
      throw new VirtualSiteException("Virtual Site config is required");
    }
    Path root = config.root();
    if (root == null || !PSVirtualSiteHelper.isSafeRootPath(root)) {
      throw new VirtualSiteException("CSV site root is missing or unsafe");
    }
    Path safeRoot = root.normalize();
    List<LoadedRow> rows = new ArrayList<>();
    Map<String, Path> seenIds = new HashMap<>();

    for (VersionSpec version : config.versions()) {
      Path versionRoot = safeRoot.resolve(version.path()).normalize();
      if (!versionRoot.startsWith(safeRoot)) {
        throw new VirtualSiteException("Version path escapes site root: " + version.path());
      }
      if (!Files.isDirectory(versionRoot)) {
        throw new VirtualSiteException("Version path not found: " + versionRoot);
      }
      try (Stream<Path> walk = Files.walk(versionRoot)) {
        List<Path> csvFiles =
            walk.filter(Files::isRegularFile)
                .filter(p -> p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".csv"))
                .sorted(Comparator.comparing(p -> p.toString().replace('\\', '/')))
                .toList();
        for (Path abs : csvFiles) {
          Path csvAbs = abs.normalize();
          if (!csvAbs.startsWith(safeRoot)) {
            throw new VirtualSiteException("CSV file escapes site root: " + abs);
          }
          Path relCsv = safeRoot.relativize(csvAbs);
          String label = relCsv.toString().replace('\\', '/');
          String text = Files.readString(csvAbs, StandardCharsets.UTF_8);
          List<Map<String, String>> parsed = VirtualCsvParser.parse(text, label);
          for (int i = 0; i < parsed.size(); i++) {
            LoadedRow loaded = toLoadedRow(parsed.get(i), version, csvAbs, label, i + 2);
            String idKey = version.id() + "\0" + loaded.ref().id();
            Path previous = seenIds.put(idKey, loaded.ref().relativePath());
            if (previous != null) {
              throw new VirtualSiteException(
                  "Duplicate CSV id '"
                      + loaded.ref().id()
                      + "' in version "
                      + version.id()
                      + ": "
                      + previous
                      + " and "
                      + loaded.ref().relativePath());
            }
            rows.add(loaded);
          }
        }
      }
    }
    return rows;
  }

  private static LoadedRow toLoadedRow(
      Map<String, String> cells, VersionSpec version, Path csvFile, String label, int line)
      throws VirtualSiteException {
    String id = VirtualCsvParser.cell(cells, VirtualCsvParser.COL_ID).trim();
    String title = VirtualCsvParser.cell(cells, VirtualCsvParser.COL_TITLE).trim();
    String body = VirtualCsvParser.cell(cells, VirtualCsvParser.COL_BODY);
    String pathRaw = VirtualCsvParser.cell(cells, VirtualCsvParser.COL_PATH).trim();
    String orderRaw = VirtualCsvParser.cell(cells, VirtualCsvParser.COL_ORDER).trim();
    String where = label + ":" + line;
    if (id.isEmpty()) {
      throw new VirtualSiteException("CSV 'id' is required in " + where);
    }
    if (title.isEmpty()) {
      throw new VirtualSiteException("CSV 'title' is required in " + where);
    }
    int order = parseOrder(orderRaw, where);
    Path relative = resolvePagePath(pathRaw, id, version.path(), where);
    VirtualItemRef ref = new VirtualItemRef(id, version.id(), relative, order, title);
    VirtualFrontmatter fm =
        new VirtualFrontmatter(id, title, "", version.id(), true, order, List.of(), false);
    return new LoadedRow(ref, fm, body != null ? body : "", csvFile);
  }

  private static int parseOrder(String raw, String where) throws VirtualSiteException {
    if (raw == null || raw.isBlank()) {
      return 0;
    }
    try {
      return Integer.parseInt(raw.trim());
    } catch (NumberFormatException e) {
      throw new VirtualSiteException("CSV 'order' must be an integer in " + where, e);
    }
  }

  /**
   * Map the optional CSV {@code path} column to a site-root-relative Markdown path using NIO {@link
   * Path#resolve} (logical {@code /} in the column is a href-style separator, not a filesystem
   * join).
   */
  static Path resolvePagePath(String pathRaw, String id, String versionPath, String where)
      throws VirtualSiteException {
    List<String> segments;
    if (pathRaw == null || pathRaw.isBlank()) {
      segments = List.of(versionPath, id + ".md");
    } else {
      if (pathRaw.indexOf('\0') >= 0) {
        throw new VirtualSiteException("CSV 'path' must not contain NUL in " + where);
      }
      String logical = pathRaw.trim().replace('\\', '/');
      if (logical.startsWith("/") || looksAbsoluteWindows(logical)) {
        throw new VirtualSiteException("CSV 'path' must be relative in " + where);
      }
      segments = new ArrayList<>();
      for (String seg : logical.split("/")) {
        if (seg.isEmpty() || ".".equals(seg)) {
          continue;
        }
        if ("..".equals(seg) || seg.indexOf('\0') >= 0 || seg.indexOf(':') >= 0) {
          throw new VirtualSiteException(
              "CSV 'path' must not contain '..', drive, or NUL segments in " + where);
        }
        segments.add(seg);
      }
      if (segments.isEmpty()) {
        throw new VirtualSiteException("CSV 'path' is empty after normalize in " + where);
      }
      if (!segments.get(0).equals(versionPath)) {
        segments.add(0, versionPath);
      }
      String last = segments.get(segments.size() - 1);
      if (!last.toLowerCase(Locale.ROOT).endsWith(".md")) {
        segments.set(segments.size() - 1, last + ".md");
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
      throw new VirtualSiteException("CSV 'path' is not a safe relative path in " + where);
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

  private record LoadedRow(
      VirtualItemRef ref, VirtualFrontmatter frontmatter, String body, Path csvFile) {}
}
