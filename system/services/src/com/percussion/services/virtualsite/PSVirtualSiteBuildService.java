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

import com.percussion.services.assembly.impl.plugin.PSTextAssemblerSupport;
import com.percussion.services.virtualsite.VirtualSiteConfig.VersionSpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * End-to-end Virtual Site build: discover → assemble Markdown → layout → static emit → register
 * participants → link check.
 *
 * <p>Does not require Spring or a CMS repository. Safe for offline / CI dogfood.
 */
public class PSVirtualSiteBuildService {

  private final IPSVirtualSiteSource source;
  private final IPSVirtualParticipantService participants;

  public PSVirtualSiteBuildService() {
    this(new PSGitFilesystemVirtualSiteSource(), new PSInMemoryVirtualParticipantService());
  }

  public PSVirtualSiteBuildService(
      IPSVirtualSiteSource source, IPSVirtualParticipantService participants) {
    this.source = source != null ? source : new PSGitFilesystemVirtualSiteSource();
    this.participants =
        participants != null ? participants : new PSInMemoryVirtualParticipantService();
  }

  /**
   * Build a Virtual Site from a filesystem root into {@code outputRoot}.
   *
   * @param siteRoot source tree (contains {@code _config.yaml})
   * @param outputRoot destination for HTML + assets
   * @param siteKey participant key
   * @return build result
   */
  public PSVirtualSiteBuildResult build(Path siteRoot, Path outputRoot, String siteKey)
      throws IOException, VirtualSiteException {
    VirtualSiteConfig config =
        VirtualSiteConfigLoader.load(siteRoot, VirtualSiteConfigLoader.DEFAULT_CONFIG_FILE, siteKey);
    return build(config, outputRoot);
  }

  public PSVirtualSiteBuildResult build(VirtualSiteConfig config, Path outputRoot)
      throws IOException, VirtualSiteException {
    Files.createDirectories(outputRoot);

    // Full rebuild replaces this site's registry so removed pages do not linger and all current
    // frontmatter ids are re-registered (see IPSVirtualParticipantService lifetime notes).
    participants.clear(config.siteKey());

    List<VirtualItemRef> refs = source.discover(config);
    List<VirtualItem> items = new ArrayList<>();
    for (VirtualItemRef ref : refs) {
      items.add(source.load(config, ref));
    }

    // ids / paths per version for link checks and participants
    Map<String, Map<String, String>> idsByVersion = new HashMap<>();
    Map<String, Set<String>> pathsByVersion = new HashMap<>();
    for (VirtualItem item : items) {
      String ver = item.ref().versionId();
      String href = VirtualNavBuilder.toHref(item.ref().relativePath());
      idsByVersion
          .computeIfAbsent(ver, k -> new HashMap<>())
          .put(item.frontmatter().id(), href);
      pathsByVersion.computeIfAbsent(ver, k -> new LinkedHashSet<>()).add(href);
    }

    Map<String, List<VirtualNavNode>> navByVersion = new HashMap<>();
    for (VersionSpec v : config.versions()) {
      List<VirtualItemRef> versionRefs =
          refs.stream().filter(r -> r.versionId().equals(v.id())).toList();
      navByVersion.put(v.id(), VirtualNavBuilder.build(v.path(), versionRefs, config));
    }

    List<String> written = new ArrayList<>();
    List<String> linkProblems = new ArrayList<>();

    for (VirtualItem item : items) {
      String sourcePath = item.ref().relativePath().toString().replace('\\', '/');
      Map<String, String> ids =
          idsByVersion.getOrDefault(item.ref().versionId(), Map.of());
      String rewrittenBody =
          VirtualMarkdownLinkRewriter.rewrite(item.markdownBody(), sourcePath, ids);
      String contentHtml = PSTextAssemblerSupport.renderMarkdown(rewrittenBody, Map.of());
      VersionSpec version = findVersion(config, item.ref().versionId());
      String navHtml =
          PSVirtualSiteLayoutRenderer.renderNavHtml(
              navByVersion.getOrDefault(item.ref().versionId(), List.of()));
      String versionSwitcher =
          PSVirtualSiteLayoutRenderer.renderVersionSwitcher(config, item.ref().versionId());
      String html =
          PSVirtualSiteLayoutRenderer.render(
              config.themeDir(),
              config.layoutFile(),
              config.siteTitle(),
              item.frontmatter().title(),
              item.frontmatter().description(),
              contentHtml,
              navHtml,
              version != null ? version.label() : item.ref().versionId(),
              versionSwitcher);

      String href = VirtualNavBuilder.toHref(item.ref().relativePath());
      Path outFile = resolveHref(outputRoot, href);
      Files.createDirectories(outFile.getParent());
      Files.writeString(outFile, html, StandardCharsets.UTF_8);
      written.add(href);

      participants.upsert(
          new VirtualParticipant(
              config.siteKey(),
              item.frontmatter().id(),
              item.ref().versionId(),
              href,
              sourcePath));

      linkProblems.addAll(
          VirtualLinkChecker.checkPage(
              config.siteKey(),
              item.ref().versionId(),
              sourcePath,
              item.markdownBody(),
              ids,
              pathsByVersion.getOrDefault(item.ref().versionId(), Set.of())));
    }

    copyAssets(config.assetsDir(), outputRoot.resolve("assets"));
    if (Files.isDirectory(config.themeDir().resolve("assets"))) {
      copyAssets(config.themeDir().resolve("assets"), outputRoot.resolve("assets"));
    }

    participants.flush(config.siteKey());

    // Write link report
    Path report = outputRoot.resolve("link-report.txt");
    if (linkProblems.isEmpty()) {
      Files.writeString(report, "OK: no link problems\n", StandardCharsets.UTF_8);
    } else {
      Files.writeString(
          report, String.join("\n", linkProblems) + "\n", StandardCharsets.UTF_8);
    }
    written.add("link-report.txt");

    return new PSVirtualSiteBuildResult(outputRoot, items.size(), linkProblems, written);
  }

  private static VersionSpec findVersion(VirtualSiteConfig config, String id) {
    for (VersionSpec v : config.versions()) {
      if (v.id().equals(id)) {
        return v;
      }
    }
    return null;
  }

  static Path resolveHref(Path outputRoot, String href) {
    Path p = outputRoot;
    for (String seg : href.split("/")) {
      if (!seg.isEmpty()) {
        p = p.resolve(seg);
      }
    }
    return p;
  }

  private static void copyAssets(Path from, Path to) throws IOException {
    if (!Files.isDirectory(from)) {
      return;
    }
    Files.createDirectories(to);
    Files.walkFileTree(
        from,
        new SimpleFileVisitor<>() {
          @Override
          public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
              throws IOException {
            Path rel = from.relativize(dir);
            Path dest = to.resolve(rel.toString());
            Files.createDirectories(dest);
            return FileVisitResult.CONTINUE;
          }

          @Override
          public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            Path rel = from.relativize(file);
            Path dest = to.resolve(rel.toString());
            Files.createDirectories(dest.getParent());
            Files.copy(file, dest, StandardCopyOption.REPLACE_EXISTING);
            return FileVisitResult.CONTINUE;
          }
        });
  }
}
