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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.services.virtualsite.VirtualSiteConfig.RobotsSpec;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

class PSRobotsTxtVirtualSiteSourceTest {

  @TempDir Path tempDir;

  @Test
  void discoverAndLoadFromLocalRobotsFixture() throws Exception {
    Path root = writeSite(tempDir.resolve("robots-file"), "robots.txt");
    Files.writeString(
        root.resolve("robots.txt"),
        robotsGroup("*", "/Hello-from-robots"),
        StandardCharsets.UTF_8);
    PSRobotsTxtVirtualSiteSource source = new PSRobotsTxtVirtualSiteSource();
    assertEquals(VirtualSiteSourceType.ROBOTS_TXT.wireName(), source.sourceType());
    VirtualSiteConfig cfg = config(root, "robots.txt");
    List<VirtualItemRef> refs = source.discover(cfg);
    assertEquals(1, refs.size());
    assertEquals("star-1", refs.get(0).id());
    assertEquals("User-agent: *", refs.get(0).title());
    assertEquals(Path.of("8.2", "star-1.html"), refs.get(0).relativePath());
    VirtualItem item = source.load(cfg, refs.get(0));
    assertEquals("", item.frontmatter().description());
    assertTrue(item.markdownBody().contains("Disallow: /Hello-from-robots"), item.markdownBody());
    assertEquals("robots.txt", item.absolutePath().getFileName().toString());
  }

  @Test
  void omittedRobotsMappingDefaultsToRobotsTxt() throws Exception {
    Path root = writeSite(tempDir.resolve("default-robots"), null);
    Files.writeString(
        root.resolve(PSRobotsTxtVirtualSiteSource.DEFAULT_ROBOTS_FILE),
        robotsGroup("*", "/private"),
        StandardCharsets.UTF_8);
    VirtualSiteConfig cfg =
        VirtualSiteConfigLoader.load(root, VirtualSiteConfigLoader.DEFAULT_CONFIG_FILE, "rb-docs");
    List<VirtualItemRef> refs = new PSRobotsTxtVirtualSiteSource().discover(cfg);
    assertEquals(1, refs.size());
    assertEquals("star-1", refs.get(0).id());
  }

  @Test
  void commentsOnlyFixtureStillEmitsOnePage() throws Exception {
    Path root = writeSite(tempDir.resolve("comments-only"), "robots.txt");
    Files.writeString(
        root.resolve("robots.txt"),
        "# operators: no crawlers configured yet\n",
        StandardCharsets.UTF_8);
    List<VirtualItemRef> refs = new PSRobotsTxtVirtualSiteSource().discover(config(root, "robots.txt"));
    assertEquals(1, refs.size());
    assertEquals("robots", refs.get(0).id());
    VirtualItem item =
        new PSRobotsTxtVirtualSiteSource().load(config(root, "robots.txt"), refs.get(0));
    assertTrue(item.markdownBody().contains("no crawlers configured yet"), item.markdownBody());
  }

  @Test
  void blankUserAgentFailsClosed() throws Exception {
    Path root = writeSite(tempDir.resolve("blank-ua"), "robots.txt");
    Files.writeString(
        root.resolve("robots.txt"),
        "User-agent:\nDisallow: /\n",
        StandardCharsets.UTF_8);
    VirtualSiteException ex =
        assertThrows(
            VirtualSiteException.class,
            () -> new PSRobotsTxtVirtualSiteSource().discover(config(root, "robots.txt")));
    assertTrue(ex.getMessage().contains("User-agent"), ex.getMessage());
  }

  @Test
  void emptyRobotsFailsClosed() throws Exception {
    Path root = writeSite(tempDir.resolve("empty-rb"), "robots.txt");
    Files.writeString(root.resolve("robots.txt"), "   \n", StandardCharsets.UTF_8);
    VirtualSiteException ex =
        assertThrows(
            VirtualSiteException.class,
            () -> new PSRobotsTxtVirtualSiteSource().discover(config(root, "robots.txt")));
    assertTrue(ex.getMessage().toLowerCase().contains("empty"), ex.getMessage());
  }

  @Test
  void unknownIdOnLoadFailsClosed() throws Exception {
    Path root = writeSite(tempDir.resolve("unknown-id"), "robots.txt");
    Files.writeString(
        root.resolve("robots.txt"),
        robotsGroup("*", "/"),
        StandardCharsets.UTF_8);
    PSRobotsTxtVirtualSiteSource source = new PSRobotsTxtVirtualSiteSource();
    VirtualSiteConfig cfg = config(root, "robots.txt");
    source.discover(cfg);
    VirtualItemRef fake =
        new VirtualItemRef("missing-id", "8.2", Path.of("8.2", "missing-id.html"), 0, "x");
    VirtualSiteException ex = assertThrows(VirtualSiteException.class, () -> source.load(cfg, fake));
    assertTrue(ex.getMessage().contains("Unknown"), ex.getMessage());
    assertTrue(ex.getMessage().contains("missing-id"), ex.getMessage());
  }

  @Test
  void robotsUrlIsRejected() throws Exception {
    Path root = writeSite(tempDir.resolve("remote-url"), "robots.txt");
    Files.writeString(
        root.resolve("robots.txt"),
        robotsGroup("*", "/"),
        StandardCharsets.UTF_8);
    VirtualSiteConfig cfg =
        new VirtualSiteConfig(
            root,
            "Robots Docs",
            "",
            "page.html",
            List.of(new VirtualSiteConfig.VersionSpec("8.2", "8.2", "8.2", true)),
            List.of(),
            "rb-docs",
            null,
            null,
            null,
            null,
            null,
            null,
            new RobotsSpec("https://example.com/robots.txt", "robots.txt"));
    VirtualSiteException ex =
        assertThrows(
            VirtualSiteException.class, () -> new PSRobotsTxtVirtualSiteSource().discover(cfg));
    assertTrue(ex.getMessage().contains("robots.url"), ex.getMessage());
    assertTrue(
        ex.getMessage().toLowerCase().contains("live") || ex.getMessage().contains("remote"),
        ex.getMessage());
  }

  @Test
  void remoteSitemapDirectiveFailsClosed() throws Exception {
    Path root = writeSite(tempDir.resolve("remote-sitemap"), "robots.txt");
    Files.writeString(
        root.resolve("robots.txt"),
        """
        User-agent: *
        Disallow: /admin
        Sitemap: https://example.com/sitemap.xml
        """,
        StandardCharsets.UTF_8);
    VirtualSiteException ex =
        assertThrows(
            VirtualSiteException.class,
            () -> new PSRobotsTxtVirtualSiteSource().discover(config(root, "robots.txt")));
    assertTrue(ex.getMessage().toLowerCase().contains("sitemap"), ex.getMessage());
    assertTrue(ex.getMessage().toLowerCase().contains("live"), ex.getMessage());
  }

  @Test
  void localSitemapDirectiveIsDocumentedNotFetched() throws Exception {
    Path root = writeSite(tempDir.resolve("local-sitemap"), "robots.txt");
    Files.writeString(
        root.resolve("robots.txt"),
        """
        User-agent: *
        Disallow: /admin
        Sitemap: sitemap.xml
        """,
        StandardCharsets.UTF_8);
    VirtualItem item =
        new PSRobotsTxtVirtualSiteSource()
            .load(
                config(root, "robots.txt"),
                new PSRobotsTxtVirtualSiteSource().discover(config(root, "robots.txt")).get(0));
    assertTrue(item.markdownBody().contains("Sitemap: sitemap.xml"), item.markdownBody());
  }

  @Test
  void pathTraversalAndAbsolutePathFailClosed() {
    VirtualSiteException rel =
        assertThrows(
            VirtualSiteException.class,
            () ->
                PSRobotsTxtVirtualSiteSource.resolvePagePath(
                    "../outside.html", "esc", "8.2", "test"));
    assertTrue(rel.getMessage().contains("path"), rel.getMessage());
    VirtualSiteException abs =
        assertThrows(
            VirtualSiteException.class,
            () ->
                PSRobotsTxtVirtualSiteSource.resolvePagePath(
                    "/etc/passwd.html", "esc", "8.2", "test"));
    assertTrue(abs.getMessage().toLowerCase().contains("relative"), abs.getMessage());
  }

  @Test
  void robotsFileTraversalFailsClosed() {
    VirtualSiteException ex =
        assertThrows(
            VirtualSiteException.class,
            () ->
                PSRobotsTxtVirtualSiteSource.resolveRobotsFile(
                    tempDir.resolve("q-escape"), "../outside.txt"));
    assertTrue(ex.getMessage().contains("file") || ex.getMessage().contains(".."), ex.getMessage());
  }

  @Test
  @EnabledOnOs(OS.WINDOWS)
  void windowsAbsoluteRobotsFileRejected() {
    VirtualSiteException ex =
        assertThrows(
            VirtualSiteException.class,
            () ->
                PSRobotsTxtVirtualSiteSource.resolveRobotsFile(
                    tempDir.resolve("win-root"), "C:/docs/evil.txt"));
    assertTrue(ex.getMessage().toLowerCase().contains("relative"), ex.getMessage());
  }

  @Test
  @EnabledOnOs({OS.LINUX, OS.MAC})
  void unixAbsoluteRobotsFileRejected() {
    VirtualSiteException ex =
        assertThrows(
            VirtualSiteException.class,
            () ->
                PSRobotsTxtVirtualSiteSource.resolveRobotsFile(
                    tempDir.resolve("unix-root"), "/etc/passwd.txt"));
    assertTrue(ex.getMessage().toLowerCase().contains("relative"), ex.getMessage());
  }

  @Test
  void loadRereadsLocalFileWithoutCache() throws Exception {
    Path root = writeSite(tempDir.resolve("live"), "robots.txt");
    Path robots = root.resolve("robots.txt");
    Files.writeString(
        robots, robotsGroup("*", "/token-AAA"), StandardCharsets.UTF_8);
    PSRobotsTxtVirtualSiteSource source = new PSRobotsTxtVirtualSiteSource();
    VirtualSiteConfig cfg = config(root, "robots.txt");
    VirtualItem first = source.load(cfg, source.discover(cfg).get(0));
    assertTrue(first.markdownBody().contains("token-AAA"));
    Files.writeString(
        robots, robotsGroup("Googlebot", "/token-BBB"), StandardCharsets.UTF_8);
    VirtualItem second = source.load(cfg, source.discover(cfg).get(0));
    assertEquals("User-agent: Googlebot", second.frontmatter().title());
    assertTrue(second.markdownBody().contains("token-BBB"));
    assertFalse(second.markdownBody().contains("token-AAA"));
  }

  @Test
  void factorySelectsRobotsTxtAndLeavesPeersUnchanged() throws Exception {
    IPSVirtualSiteSource source =
        PSVirtualSiteSourceFactory.create(VirtualSiteSourceType.ROBOTS_TXT);
    assertInstanceOf(PSRobotsTxtVirtualSiteSource.class, source);
    assertEquals("robots-txt", source.sourceType());
    IPSVirtualSiteSource byName = PSVirtualSiteSourceFactory.createFromWireName("robots-txt");
    assertInstanceOf(PSRobotsTxtVirtualSiteSource.class, byName);
    assertInstanceOf(
        PSGitFilesystemVirtualSiteSource.class,
        PSVirtualSiteSourceFactory.createFromWireName("git-filesystem"));
    assertInstanceOf(
        PSCsvFilesystemVirtualSiteSource.class,
        PSVirtualSiteSourceFactory.createFromWireName("csv-filesystem"));
    assertInstanceOf(
        PSSqlDatabaseVirtualSiteSource.class,
        PSVirtualSiteSourceFactory.createFromWireName("sql-database"));
    assertInstanceOf(
        PSHttpJsonVirtualSiteSource.class,
        PSVirtualSiteSourceFactory.createFromWireName("http-json"));
    assertInstanceOf(
        PSObjectStorageVirtualSiteSource.class,
        PSVirtualSiteSourceFactory.createFromWireName("object-storage"));
    assertInstanceOf(
        PSRssAtomVirtualSiteSource.class,
        PSVirtualSiteSourceFactory.createFromWireName("rss-atom"));
    assertInstanceOf(
        PSIcalendarVirtualSiteSource.class,
        PSVirtualSiteSourceFactory.createFromWireName("icalendar"));
    assertInstanceOf(
        PSSitemapXmlVirtualSiteSource.class,
        PSVirtualSiteSourceFactory.createFromWireName("sitemap-xml"));
    VirtualSiteException ex =
        assertThrows(
            VirtualSiteException.class,
            () -> PSVirtualSiteSourceFactory.createFromWireName("sql-api"));
    assertTrue(ex.getMessage().contains("sql-api"));
    assertTrue(ex.getMessage().contains("robots-txt"));
    assertTrue(ex.getMessage().contains("sitemap-xml"));
  }

  @Test
  void buildServiceFactoryWiresRobotsTxtAndEmitsHtml() throws Exception {
    Path root = writeSite(tempDir.resolve("build-rb"), "robots.txt");
    Files.writeString(
        root.resolve("robots.txt"),
        robotsGroup("*", "/Hello-from-robots"),
        StandardCharsets.UTF_8);
    Files.writeString(
        root.resolve("_theme").resolve("page.html"),
        "<html><body><h1>${pageTitle}</h1>${content}</body></html>",
        StandardCharsets.UTF_8);
    Path out = tempDir.resolve("build-out");
    PSVirtualSiteBuildService service =
        PSVirtualSiteBuildService.forSourceType(VirtualSiteSourceType.ROBOTS_TXT);
    assertInstanceOf(PSRobotsTxtVirtualSiteSource.class, service.source());

    PSVirtualSiteBuildResult result = service.build(root, out, "rb-docs");
    assertTrue(result.pageCount() > 0);
    Path html = out.resolve("8.2").resolve("star-1.html");
    assertTrue(Files.isRegularFile(html), "missing " + html);
    String body = Files.readString(html, StandardCharsets.UTF_8);
    assertTrue(body.contains("User-agent: *"), body);
    assertTrue(body.contains("Hello-from-robots"), body);
  }

  @Test
  void secondBuildAfterRobotsAndConfigEditEmitsUpdatedHtmlWithoutRestart() throws Exception {
    Path root = writeSite(tempDir.resolve("rb-rebuild"), "robots.txt");
    writeRobotsYaml(root, "First Site Title", "robots.txt");
    Files.writeString(
        root.resolve("_theme").resolve("page.html"),
        "<html><body><h1>${siteTitle}</h1><h2>${pageTitle}</h2>${content}</body></html>",
        StandardCharsets.UTF_8);
    Path robots = root.resolve("robots.txt");
    Files.writeString(
        robots, robotsGroup("*", "/unique-token-AAA"), StandardCharsets.UTF_8);

    Path out = tempDir.resolve("rb-rebuild-out");
    PSRobotsTxtVirtualSiteSource source = new PSRobotsTxtVirtualSiteSource();
    PSVirtualSiteBuildService service =
        new PSVirtualSiteBuildService(source, new PSInMemoryVirtualParticipantService());

    PSVirtualSiteBuildResult first = service.build(root, out, "rb-docs");
    assertEquals(1, first.pageCount());
    Path firstHtmlPath = out.resolve("8.2").resolve("star-1.html");
    assertTrue(Files.isRegularFile(firstHtmlPath), "missing " + firstHtmlPath);
    String firstHtml = Files.readString(firstHtmlPath, StandardCharsets.UTF_8);
    assertTrue(firstHtml.contains("First Site Title"), firstHtml);
    assertTrue(firstHtml.contains("unique-token-AAA"), firstHtml);

    Files.writeString(
        robots, robotsGroup("Googlebot", "/unique-token-BBB"), StandardCharsets.UTF_8);
    writeRobotsYaml(root, "Second Site Title", "robots.txt");

    VirtualSiteConfig reloaded =
        VirtualSiteConfigLoader.load(root, VirtualSiteConfigLoader.DEFAULT_CONFIG_FILE, "rb-docs");
    assertEquals("Second Site Title", reloaded.siteTitle());
    assertEquals("robots.txt", reloaded.robots().file());
    List<VirtualItemRef> refs = source.discover(reloaded);
    assertEquals(1, refs.size());
    assertEquals("Googlebot-1", refs.get(0).id());
    VirtualItem loaded = source.load(reloaded, refs.get(0));
    assertEquals("User-agent: Googlebot", loaded.frontmatter().title());
    assertTrue(loaded.markdownBody().contains("unique-token-BBB"), loaded.markdownBody());
    assertFalse(loaded.markdownBody().contains("unique-token-AAA"), loaded.markdownBody());

    PSVirtualSiteBuildResult second = service.build(root, out, "rb-docs");
    assertEquals(1, second.pageCount());
    Path secondHtmlPath = out.resolve("8.2").resolve("Googlebot-1.html");
    assertTrue(Files.isRegularFile(secondHtmlPath), "missing " + secondHtmlPath);
    String secondHtml = Files.readString(secondHtmlPath, StandardCharsets.UTF_8);
    assertTrue(secondHtml.contains("Second Site Title"), secondHtml);
    assertTrue(secondHtml.contains("unique-token-BBB"), secondHtml);
    assertFalse(
        Files.exists(firstHtmlPath), "stale " + firstHtmlPath + " should be cleared on full rebuild");
    assertFalse(secondHtml.contains("unique-token-AAA"), secondHtml);
    assertFalse(secondHtml.contains("First Site Title"), secondHtml);
    assertNotEquals(firstHtml, secondHtml);
  }

  @Test
  void yamlLoaderParsesRobotsSpec() throws Exception {
    Path root = writeSite(tempDir.resolve("yaml-rb"), "custom-robots.txt");
    Files.writeString(
        root.resolve("custom-robots.txt"),
        robotsGroup("*", "/"),
        StandardCharsets.UTF_8);
    VirtualSiteConfig loaded =
        VirtualSiteConfigLoader.load(root, VirtualSiteConfigLoader.DEFAULT_CONFIG_FILE, "rb-docs");
    assertEquals("custom-robots.txt", loaded.robots().file());
  }

  @Test
  void omittedPathDefaultsToIdHtmlUnderVersion() throws Exception {
    Path relative = PSRobotsTxtVirtualSiteSource.resolvePagePath("", "plain-id", "8.2", "test");
    assertEquals(Path.of("8.2", "plain-id.html"), relative);
  }

  @Test
  void slugForPathStripsUnsafeChars() {
    assertEquals("star", PSRobotsTxtVirtualSiteSource.slugForPath("*"));
    assertEquals("Googlebot", PSRobotsTxtVirtualSiteSource.slugForPath("Googlebot"));
    assertEquals("bot-name", PSRobotsTxtVirtualSiteSource.slugForPath("bot name"));
    assertEquals("agent", PSRobotsTxtVirtualSiteSource.slugForPath(":::"));
  }

  private static Path writeSite(Path root, String file) throws Exception {
    Files.createDirectories(root.resolve("8.2"));
    Files.createDirectories(root.resolve("_theme"));
    String robotsBlock;
    if (file != null && !file.isBlank()) {
      robotsBlock =
          """
          robots:
            file: %s
          """
              .formatted(file);
    } else {
      robotsBlock = "";
    }
    Files.writeString(
        root.resolve("_config.yaml"),
        """
        site:
          title: Robots Docs
        versions:
          - id: "8.2"
            label: "8.2"
            path: "8.2"
            default: true
        theme:
          layout: page.html
        %s
        """
            .formatted(robotsBlock),
        StandardCharsets.UTF_8);
    return root;
  }

  private static void writeRobotsYaml(Path root, String siteTitle, String file) throws Exception {
    Files.writeString(
        root.resolve("_config.yaml"),
        """
        site:
          title: %s
        versions:
          - id: "8.2"
            label: "8.2"
            path: "8.2"
            default: true
        theme:
          layout: page.html
        robots:
          file: %s
        """
            .formatted(siteTitle, file),
        StandardCharsets.UTF_8);
  }

  private static VirtualSiteConfig config(Path root, String file) {
    RobotsSpec spec = file != null ? new RobotsSpec(null, file) : null;
    return new VirtualSiteConfig(
        root,
        "Robots Docs",
        "",
        "page.html",
        List.of(new VirtualSiteConfig.VersionSpec("8.2", "8.2", "8.2", true)),
        List.of(),
        "rb-docs",
        null,
        null,
        null,
        null,
        null,
        null,
        spec);
  }

  private static String robotsGroup(String userAgent, String disallow) {
    return """
        User-agent: %s
        Disallow: %s
        """
        .formatted(userAgent, disallow);
  }
}
