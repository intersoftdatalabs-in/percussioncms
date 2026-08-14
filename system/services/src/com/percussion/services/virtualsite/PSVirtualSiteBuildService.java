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
 *
 * <p>Path-injection defense: every NIO write under {@code outputRoot} runs only after {@link
 * #requireSafeBuildRoot(Path)} (delegates to {@link PSVirtualSiteHelper#isSafeRootPath(Path)} —
 * rejects empty / {@code .} / remaining {@code ..}). Href segments are validated before resolve.
 * CodeQL alerts #1956–#1960 / #1966.
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
   * <p>Every invocation reloads {@code _config.yaml}, re-discovers and re-loads Markdown from the
   * current tree, then overwrites emitted HTML. The same service instance does not reuse parsed
   * pages from a previous build — operators do not need a JVM restart after {@code git pull} or a
   * local edit.
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
    // Barrier: only paths that pass requireSafeBuildRoot reach NIO create/write sinks.
    Path safeOut = requireSafeBuildRoot(outputRoot);
    Files.createDirectories(safeOut); // codeql[java/path-injection]

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
      Path outFile = resolveHref(safeOut, href);
      Files.createDirectories(outFile.getParent()); // codeql[java/path-injection]
      Files.writeString(outFile, html, StandardCharsets.UTF_8); // codeql[java/path-injection]
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

    copyAssets(config.assetsDir(), safeOut.resolve("assets"));
    if (Files.isDirectory(config.themeDir().resolve("assets"))) {
      copyAssets(config.themeDir().resolve("assets"), safeOut.resolve("assets"));
    }

    participants.flush(config.siteKey());

    // Write link report
    Path report = safeOut.resolve("link-report.txt"); // codeql[java/path-injection]
    if (linkProblems.isEmpty()) {
      Files.writeString(report, "OK: no link problems\n", StandardCharsets.UTF_8); // codeql[java/path-injection]
    } else {
      Files.writeString(
          report, String.join("\n", linkProblems) + "\n", StandardCharsets.UTF_8); // codeql[java/path-injection]
    }
    written.add("link-report.txt");

    return new PSVirtualSiteBuildResult(safeOut, items.size(), linkProblems, written);
  }

  private static VersionSpec findVersion(VirtualSiteConfig config, String id) {
    for (VersionSpec v : config.versions()) {
      if (v.id().equals(id)) {
        return v;
      }
    }
    return null;
  }

  /**
   * Path-injection barrier for Virtual Site build output roots.
   *
   * <p>Rejects empty / {@code .} / remaining {@code ..} name elements after normalize (delegates to
   * {@link PSVirtualSiteHelper#isSafeRootPath(Path)}). Modeled for CodeQL as a {@code
   * path-injection} barrier; callers must not use the input path after a failed check.
   *
   * @param outputRoot candidate output root
   * @return normalized path after validation
   * @throws VirtualSiteException when the path is unsafe
   */
  static Path requireSafeBuildRoot(Path outputRoot) throws VirtualSiteException {
    if (!PSVirtualSiteHelper.isSafeRootPath(outputRoot)) {
      throw new VirtualSiteException(
          "outputRoot must be a non-empty path with no '..' segments after normalize. Rejected: '"
              + outputRoot
              + "'");
    }
    return outputRoot.normalize();
  }

  /**
   * Resolve a published href under {@code outputRoot}, rejecting {@code ..} / absolute segments and
   * ensuring the result stays under the (already barrier-checked) root.
   */
  static Path resolveHref(Path outputRoot, String href) throws VirtualSiteException {
    Path safeRoot = requireSafeBuildRoot(outputRoot);
    Path p = safeRoot;
    if (href != null) {
      for (String seg : href.split("/")) {
        if (seg.isEmpty() || ".".equals(seg)) {
          continue;
        }
        if ("..".equals(seg) || seg.indexOf('\0') >= 0) {
          throw new VirtualSiteException(
              "href must not contain '..' or NUL segments. Rejected segment in: '" + href + "'");
        }
        // Reject Windows drive / absolute-looking segments
        if (seg.indexOf(':') >= 0 || seg.indexOf('\\') >= 0) {
          throw new VirtualSiteException(
              "href must not contain absolute or Windows path segments. Rejected: '" + href + "'");
        }
        p = p.resolve(seg);
      }
    }
    Path normalized = p.normalize();
    if (!normalized.startsWith(safeRoot)) {
      throw new VirtualSiteException(
          "Resolved href escapes outputRoot. href='" + href + "' root='" + safeRoot + "'");
    }
    return normalized;
  }

  private static void copyAssets(Path from, Path to) throws IOException {
    if (!Files.isDirectory(from)) {
      return;
    }
    // Barrier: dest must be a safe root path (no empty / '.' / remaining '..').
    Path safeTo;
    try {
      safeTo = requireSafeBuildRoot(to);
    } catch (VirtualSiteException e) {
      throw new IOException(e.getMessage(), e);
    }
    Files.createDirectories(safeTo); // codeql[java/path-injection]
    Files.walkFileTree(
        from,
        new SimpleFileVisitor<>() {
          @Override
          public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
              throws IOException {
            Path rel = from.relativize(dir);
            Path dest = resolveUnder(safeTo, rel);
            Files.createDirectories(dest); // codeql[java/path-injection]
            return FileVisitResult.CONTINUE;
          }

          @Override
          public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            Path rel = from.relativize(file);
            Path dest = resolveUnder(safeTo, rel);
            Files.createDirectories(dest.getParent()); // codeql[java/path-injection]
            Files.copy(file, dest, StandardCopyOption.REPLACE_EXISTING); // codeql[java/path-injection]
            return FileVisitResult.CONTINUE;
          }
        });
  }

  /** Resolve a relative path under base; reject escaping {@code ..} name elements. */
  private static Path resolveUnder(Path base, Path relative) throws IOException {
    Path p = base;
    for (Path part : relative) {
      String name = part.toString();
      if (name.isEmpty() || ".".equals(name)) {
        continue;
      }
      if ("..".equals(name) || name.indexOf('\0') >= 0) {
        throw new IOException("Asset relative path escapes base: " + relative);
      }
      p = p.resolve(name);
    }
    Path normalized = p.normalize();
    if (!normalized.startsWith(base.normalize())) {
      throw new IOException("Asset path escapes base directory: " + relative);
    }
    return normalized;
  }
}
