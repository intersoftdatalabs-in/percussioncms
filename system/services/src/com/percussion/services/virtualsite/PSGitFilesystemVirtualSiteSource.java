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
 * Phase 1 Virtual Site source: local filesystem tree (Git checkout). Discovers {@code *.md} under
 * each configured version path.
 */
public class PSGitFilesystemVirtualSiteSource implements IPSVirtualSiteSource {

  @Override
  public String sourceType() {
    return VirtualSiteSourceType.GIT_FILESYSTEM.wireName();
  }

  @Override
  public List<VirtualItemRef> discover(VirtualSiteConfig config)
      throws IOException, VirtualSiteException {
    List<VirtualItemRef> refs = new ArrayList<>();
    Map<String, Path> seenIds = new HashMap<>();

    for (VersionSpec version : config.versions()) {
      Path versionRoot = config.root().resolve(version.path());
      if (!Files.isDirectory(versionRoot)) {
        throw new VirtualSiteException("Version path not found: " + versionRoot);
      }
      try (Stream<Path> walk = Files.walk(versionRoot)) {
        List<Path> mdFiles =
            walk.filter(Files::isRegularFile)
                .filter(p -> p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".md"))
                .sorted(Comparator.comparing(p -> p.toString().replace('\\', '/')))
                .toList();
        for (Path abs : mdFiles) {
          Path rel = config.root().relativize(abs);
          Parsed parsed = parseFile(abs, version.id(), rel.toString());
          String idKey = version.id() + "\0" + parsed.frontmatter().id();
          Path previous = seenIds.put(idKey, rel);
          if (previous != null) {
            throw new VirtualSiteException(
                "Duplicate frontmatter id '"
                    + parsed.frontmatter().id()
                    + "' in version "
                    + version.id()
                    + ": "
                    + previous
                    + " and "
                    + rel);
          }
          refs.add(
              new VirtualItemRef(
                  parsed.frontmatter().id(),
                  version.id(),
                  rel,
                  parsed.frontmatter().order(),
                  parsed.frontmatter().title()));
        }
      }
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
    Path abs = config.root().resolve(ref.relativePath()).normalize();
    if (!abs.startsWith(config.root().normalize())) {
      throw new VirtualSiteException("Ref escapes site root: " + ref.relativePath());
    }
    Parsed parsed = parseFile(abs, ref.versionId(), ref.relativePath().toString());
    return new VirtualItem(ref, parsed.frontmatter(), parsed.body(), abs);
  }

  private static Parsed parseFile(Path abs, String versionId, String label)
      throws IOException, VirtualSiteException {
    String text = Files.readString(abs, StandardCharsets.UTF_8);
    return VirtualFrontmatterParser.parse(text, versionId, label);
  }
}
