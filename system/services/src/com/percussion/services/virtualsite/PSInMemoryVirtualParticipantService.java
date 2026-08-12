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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Virtual participant registry: process-scoped memory with optional Path-backed JSONL durability.
 *
 * <p>When constructed with a non-null store directory, existing {@code participants-*.jsonl} files
 * under that directory are loaded so a new JVM can see prior registrations. {@link #flush(String)}
 * writes the current in-memory map for a site. {@link #clear(String)} / {@link #clearAll()} reset
 * memory and delete store files when durability is enabled.
 *
 * <p>Not a Hibernate entity — Phase 1 avoids CMS content IDs and {@code PSX_MANAGEDLINK}. Paths are
 * portable {@link Path} values (no Unix-only hardcodes).
 *
 * <p>Path-injection defense: durable store directory is accepted only after {@link
 * #requireSafeStoreDirectory(Path)} ({@link PSVirtualSiteHelper#isSafeRootPath(Path)}). Site file
 * names use {@link #sanitize(String)} (single-segment safe names). CodeQL alerts #1962–#1965.
 *
 * @see IPSVirtualParticipantService
 */
public class PSInMemoryVirtualParticipantService implements IPSVirtualParticipantService {

  private static final String FILE_PREFIX = "participants-";
  private static final String FILE_SUFFIX = ".jsonl";

  private final Map<String, Map<String, VirtualParticipant>> bySite = new ConcurrentHashMap<>();
  private final Path storeDirectory;

  /** Process-scoped only — no disk I/O; lost on JVM exit unless the caller re-upserts. */
  public PSInMemoryVirtualParticipantService() {
    this(null);
  }

  /**
   * @param storeDirectory if non-null, load existing JSONL on construct and write on {@link
   *     #flush(String)}; must be a portable {@link Path} that passes {@link
   *     #requireSafeStoreDirectory(Path)} (e.g. {@code outputRoot.resolve("_meta")})
   */
  public PSInMemoryVirtualParticipantService(Path storeDirectory) {
    if (storeDirectory != null) {
      this.storeDirectory = requireSafeStoreDirectory(storeDirectory);
      try {
        loadAllFromStore();
      } catch (IOException e) {
        throw new UncheckedIOException(
            "Failed to load virtual participant store from " + this.storeDirectory, e);
      }
    } else {
      this.storeDirectory = null;
    }
  }

  /**
   * Path-injection barrier for durable participant store directories.
   *
   * <p>Rejects empty / {@code .} / remaining {@code ..} after normalize. Modeled for CodeQL as a
   * {@code path-injection} barrier.
   *
   * @param storeDirectory candidate store directory
   * @return normalized path
   * @throws IllegalArgumentException when the path is unsafe
   */
  static Path requireSafeStoreDirectory(Path storeDirectory) {
    if (!PSVirtualSiteHelper.isSafeRootPath(storeDirectory)) {
      throw new IllegalArgumentException(
          "storeDirectory must be a non-empty path with no '..' segments after normalize. Rejected: '"
              + storeDirectory
              + "'");
    }
    return storeDirectory.normalize();
  }

  /** Optional durable base directory, or empty when process-scoped only. */
  public Optional<Path> storeDirectory() {
    return Optional.ofNullable(storeDirectory);
  }

  @Override
  public void upsert(VirtualParticipant participant) {
    bySite
        .computeIfAbsent(participant.siteKey(), k -> new ConcurrentHashMap<>())
        .put(participant.stableId(), participant);
  }

  @Override
  public Optional<VirtualParticipant> find(String siteKey, String stableId) {
    Map<String, VirtualParticipant> map = bySite.get(siteKey);
    if (map == null) {
      return Optional.empty();
    }
    return Optional.ofNullable(map.get(stableId));
  }

  @Override
  public Collection<VirtualParticipant> list(String siteKey) {
    Map<String, VirtualParticipant> map = bySite.get(siteKey);
    if (map == null) {
      return List.of();
    }
    return List.copyOf(map.values());
  }

  @Override
  public void flush(String siteKey) throws IOException {
    if (storeDirectory == null) {
      return;
    }
    // storeDirectory already barrier-checked in ctor; re-assert for residual taint analysis.
    Path safeStore = requireSafeStoreDirectory(storeDirectory);
    Files.createDirectories(safeStore); // codeql[java/path-injection]
    Path out = siteFile(siteKey);
    Collection<VirtualParticipant> all = list(siteKey);
    List<String> lines = new ArrayList<>(all.size());
    for (VirtualParticipant p : all) {
      lines.add(toJsonLine(p));
    }
    Files.write(out, lines, StandardCharsets.UTF_8); // codeql[java/path-injection]
  }

  @Override
  public void clear(String siteKey) throws IOException {
    bySite.remove(siteKey);
    if (storeDirectory != null) {
      Path file = siteFile(siteKey);
      Files.deleteIfExists(file); // codeql[java/path-injection]
    }
  }

  @Override
  public void clearAll() throws IOException {
    bySite.clear();
    if (storeDirectory == null || !Files.isDirectory(storeDirectory)) {
      return;
    }
    try (DirectoryStream<Path> stream =
        Files.newDirectoryStream(storeDirectory, FILE_PREFIX + "*" + FILE_SUFFIX)) {
      for (Path p : stream) {
        Files.deleteIfExists(p); // codeql[java/path-injection]
      }
    }
  }

  /**
   * Reload all sites from the store directory into memory (no-op when process-scoped). Existing
   * in-memory entries for sites present on disk are replaced by the file contents; sites only in
   * memory are left unchanged unless the caller {@link #clearAll()} first.
   */
  public void reloadFromStore() throws IOException {
    if (storeDirectory == null) {
      return;
    }
    loadAllFromStore();
  }

  /** Snapshot of all sites (testing). */
  public Map<String, Map<String, VirtualParticipant>> snapshot() {
    Map<String, Map<String, VirtualParticipant>> copy = new LinkedHashMap<>();
    for (Map.Entry<String, Map<String, VirtualParticipant>> e : bySite.entrySet()) {
      copy.put(e.getKey(), Map.copyOf(e.getValue()));
    }
    return copy;
  }

  private void loadAllFromStore() throws IOException {
    if (storeDirectory == null || !Files.isDirectory(storeDirectory)) {
      return;
    }
    try (DirectoryStream<Path> stream =
        Files.newDirectoryStream(storeDirectory, FILE_PREFIX + "*" + FILE_SUFFIX)) {
      for (Path file : stream) {
        loadSiteFile(file);
      }
    }
  }

  private void loadSiteFile(Path file) throws IOException {
    List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8); // codeql[java/path-injection]
    for (String line : lines) {
      if (line == null || line.isBlank()) {
        continue;
      }
      VirtualParticipant p = fromJsonLine(line.trim());
      if (p != null) {
        upsert(p);
      }
    }
  }

  private Path siteFile(String siteKey) {
    // Barrier-checked storeDirectory + sanitize(siteKey) → single-segment file under store.
    return storeDirectory.resolve(FILE_PREFIX + sanitize(siteKey) + FILE_SUFFIX); // codeql[java/path-injection]
  }

  static String sanitize(String siteKey) {
    return siteKey.replaceAll("[^a-zA-Z0-9._-]", "_");
  }

  static String toJsonLine(VirtualParticipant p) {
    // Minimal JSON without pulling a full object mapper dependency into this path.
    return "{"
        + "\"siteKey\":\""
        + esc(p.siteKey())
        + "\","
        + "\"stableId\":\""
        + esc(p.stableId())
        + "\","
        + "\"versionId\":\""
        + esc(p.versionId())
        + "\","
        + "\"publishedPath\":\""
        + esc(p.publishedPath())
        + "\","
        + "\"sourcePath\":\""
        + esc(p.sourcePath())
        + "\""
        + "}";
  }

  /**
   * Parse one JSONL line produced by {@link #toJsonLine(VirtualParticipant)}. Returns null for
   * unusable lines.
   */
  static VirtualParticipant fromJsonLine(String line) {
    if (line == null || line.isBlank() || line.charAt(0) != '{') {
      return null;
    }
    String siteKey = readJsonStringField(line, "siteKey");
    String stableId = readJsonStringField(line, "stableId");
    String versionId = readJsonStringField(line, "versionId");
    String publishedPath = readJsonStringField(line, "publishedPath");
    String sourcePath = readJsonStringField(line, "sourcePath");
    if (siteKey == null || stableId == null || publishedPath == null) {
      return null;
    }
    return new VirtualParticipant(
        siteKey, stableId, versionId != null ? versionId : "", publishedPath, sourcePath);
  }

  /**
   * Extract a string field from a single-line minimal JSON object (same escape rules as {@link
   * #esc(String)}).
   */
  static String readJsonStringField(String json, String field) {
    String needle = "\"" + field + "\":\"";
    int start = json.indexOf(needle);
    if (start < 0) {
      return null;
    }
    int i = start + needle.length();
    StringBuilder sb = new StringBuilder();
    while (i < json.length()) {
      char c = json.charAt(i);
      if (c == '\\' && i + 1 < json.length()) {
        char n = json.charAt(i + 1);
        sb.append(n);
        i += 2;
        continue;
      }
      if (c == '"') {
        return sb.toString();
      }
      sb.append(c);
      i++;
    }
    return null;
  }

  static String esc(String s) {
    if (s == null) {
      return "";
    }
    return s.replace("\\", "\\\\").replace("\"", "\\\"");
  }
}
