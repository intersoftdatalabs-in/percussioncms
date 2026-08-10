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
import java.nio.charset.StandardCharsets;
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
 * In-memory virtual participant registry with optional JSON-lines flush for offline builds.
 *
 * <p>Not a Hibernate entity — Phase 1 avoids CMS content IDs and {@code PSX_MANAGEDLINK}.
 */
public class PSInMemoryVirtualParticipantService implements IPSVirtualParticipantService {

  private final Map<String, Map<String, VirtualParticipant>> bySite = new ConcurrentHashMap<>();
  private final Path flushDirectory;

  public PSInMemoryVirtualParticipantService() {
    this(null);
  }

  /**
   * @param flushDirectory if non-null, {@link #flush(String)} writes {@code participants-<site>.jsonl}
   */
  public PSInMemoryVirtualParticipantService(Path flushDirectory) {
    this.flushDirectory = flushDirectory;
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
    if (flushDirectory == null) {
      return;
    }
    Files.createDirectories(flushDirectory);
    Path out = flushDirectory.resolve("participants-" + sanitize(siteKey) + ".jsonl");
    Collection<VirtualParticipant> all = list(siteKey);
    List<String> lines = new ArrayList<>();
    for (VirtualParticipant p : all) {
      lines.add(toJsonLine(p));
    }
    Files.write(out, lines, StandardCharsets.UTF_8);
  }

  private static String sanitize(String siteKey) {
    return siteKey.replaceAll("[^a-zA-Z0-9._-]", "_");
  }

  private static String toJsonLine(VirtualParticipant p) {
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

  private static String esc(String s) {
    if (s == null) {
      return "";
    }
    return s.replace("\\", "\\\\").replace("\"", "\\\"");
  }

  /** Snapshot of all sites (testing). */
  public Map<String, Map<String, VirtualParticipant>> snapshot() {
    Map<String, Map<String, VirtualParticipant>> copy = new LinkedHashMap<>();
    for (Map.Entry<String, Map<String, VirtualParticipant>> e : bySite.entrySet()) {
      copy.put(e.getKey(), Map.copyOf(e.getValue()));
    }
    return copy;
  }
}
