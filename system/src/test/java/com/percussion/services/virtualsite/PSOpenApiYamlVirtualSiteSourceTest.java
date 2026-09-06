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

import com.percussion.services.virtualsite.VirtualSiteConfig.OpenApiSpec;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

class PSOpenApiYamlVirtualSiteSourceTest {

  @TempDir Path tempDir;

  @Test
  void discoverAndLoadFromLocalOpenApiFixture() throws Exception {
    Path root = writeSite(tempDir.resolve("oa-file"), "openapi.yaml");
    Files.writeString(
        root.resolve("openapi.yaml"),
        sampleSpec("List pets", "listPets", "Hello-from-openapi"),
        StandardCharsets.UTF_8);
    PSOpenApiYamlVirtualSiteSource source = new PSOpenApiYamlVirtualSiteSource();
    assertEquals(VirtualSiteSourceType.OPENAPI_YAML.wireName(), source.sourceType());
    VirtualSiteConfig cfg = config(root, "openapi.yaml");
    List<VirtualItemRef> refs = source.discover(cfg);
    assertEquals(1, refs.size());
    assertEquals("listPets-1", refs.get(0).id());
    assertEquals("List pets", refs.get(0).title());
    assertEquals(Path.of("8.2", "listPets-1.html"), refs.get(0).relativePath());
    VirtualItem item = source.load(cfg, refs.get(0));
    assertEquals("", item.frontmatter().description());
    assertTrue(item.markdownBody().contains("Hello-from-openapi"), item.markdownBody());
    assertTrue(item.markdownBody().contains("GET"), item.markdownBody());
    assertTrue(item.markdownBody().contains("/pets"), item.markdownBody());
    assertEquals("openapi.yaml", item.absolutePath().getFileName().toString());
  }

  @Test
  void omittedOpenApiMappingDefaultsToOpenapiYaml() throws Exception {
    Path root = writeSite(tempDir.resolve("default-oa"), null);
    Files.writeString(
        root.resolve(PSOpenApiYamlVirtualSiteSource.DEFAULT_OPENAPI_FILE),
        sampleSpec("Guide", "getGuide", "notes"),
        StandardCharsets.UTF_8);
    VirtualSiteConfig cfg =
        VirtualSiteConfigLoader.load(
            root, VirtualSiteConfigLoader.DEFAULT_CONFIG_FILE, "openapi-docs");
    List<VirtualItemRef> refs = new PSOpenApiYamlVirtualSiteSource().discover(cfg);
    assertEquals(1, refs.size());
    assertEquals("getGuide-1", refs.get(0).id());
  }

  @Test
  void infoOnlyFixtureStillEmitsOnePage() throws Exception {
    Path root = writeSite(tempDir.resolve("info-only"), "openapi.yaml");
    Files.writeString(
        root.resolve("openapi.yaml"),
        """
        openapi: 3.0.3
        info:
          title: Product API
          description: no operations configured yet
          version: "1.0.0"
        paths: {}
        """,
        StandardCharsets.UTF_8);
    List<VirtualItemRef> refs =
        new PSOpenApiYamlVirtualSiteSource().discover(config(root, "openapi.yaml"));
    assertEquals(1, refs.size());
    assertEquals("openapi", refs.get(0).id());
    assertEquals("Product API", refs.get(0).title());
    VirtualItem item =
        new PSOpenApiYamlVirtualSiteSource().load(config(root, "openapi.yaml"), refs.get(0));
    assertTrue(item.markdownBody().contains("no operations configured yet"), item.markdownBody());
  }

  @Test
  void mapsMultipleOperationsToPages() throws Exception {
    Path root = writeSite(tempDir.resolve("multi-op"), "openapi.yaml");
    Files.writeString(
        root.resolve("openapi.yaml"),
        """
        openapi: 3.0.3
        info:
          title: Pets
          version: "1.0.0"
        paths:
          /pets:
            get:
              summary: List pets
              operationId: listPets
            post:
              summary: Create pet
              operationId: createPet
          /pets/{id}:
            get:
              summary: Get pet
              operationId: getPet
        """,
        StandardCharsets.UTF_8);
    List<VirtualItemRef> refs =
        new PSOpenApiYamlVirtualSiteSource().discover(config(root, "openapi.yaml"));
    assertEquals(3, refs.size());
    assertEquals("listPets-1", refs.get(0).id());
    assertEquals("createPet-2", refs.get(1).id());
    assertEquals("getPet-3", refs.get(2).id());
  }

  @Test
  void emptyOpenApiFailsClosed() throws Exception {
    Path root = writeSite(tempDir.resolve("empty-oa"), "openapi.yaml");
    Files.writeString(root.resolve("openapi.yaml"), "   \n", StandardCharsets.UTF_8);
    VirtualSiteException ex =
        assertThrows(
            VirtualSiteException.class,
            () -> new PSOpenApiYamlVirtualSiteSource().discover(config(root, "openapi.yaml")));
    assertTrue(ex.getMessage().toLowerCase().contains("empty"), ex.getMessage());
  }

  @Test
  void unknownIdOnLoadFailsClosed() throws Exception {
    Path root = writeSite(tempDir.resolve("unknown-id"), "openapi.yaml");
    Files.writeString(
        root.resolve("openapi.yaml"),
        sampleSpec("List pets", "listPets", ""),
        StandardCharsets.UTF_8);
    PSOpenApiYamlVirtualSiteSource source = new PSOpenApiYamlVirtualSiteSource();
    VirtualSiteConfig cfg = config(root, "openapi.yaml");
    source.discover(cfg);
    VirtualItemRef fake =
        new VirtualItemRef("missing-id", "8.2", Path.of("8.2", "missing-id.html"), 0, "x");
    VirtualSiteException ex = assertThrows(VirtualSiteException.class, () -> source.load(cfg, fake));
    assertTrue(ex.getMessage().contains("Unknown"), ex.getMessage());
    assertTrue(ex.getMessage().contains("missing-id"), ex.getMessage());
  }

  @Test
  void openApiUrlIsRejected() throws Exception {
    Path root = writeSite(tempDir.resolve("remote-url"), "openapi.yaml");
    Files.writeString(
        root.resolve("openapi.yaml"),
        sampleSpec("List pets", "listPets", ""),
        StandardCharsets.UTF_8);
    VirtualSiteConfig cfg =
        new VirtualSiteConfig(
            root,
            "OpenAPI Docs",
            "",
            "page.html",
            List.of(new VirtualSiteConfig.VersionSpec("8.2", "8.2", "8.2", true)),
            List.of(),
            "openapi-docs",
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            new OpenApiSpec("https://example.com/openapi.yaml", "openapi.yaml"));
    VirtualSiteException ex =
        assertThrows(
            VirtualSiteException.class, () -> new PSOpenApiYamlVirtualSiteSource().discover(cfg));
    assertTrue(ex.getMessage().contains("openapi.url"), ex.getMessage());
    assertTrue(
        ex.getMessage().toLowerCase().contains("live") || ex.getMessage().contains("remote"),
        ex.getMessage());
  }

  @Test
  void remoteRefFailsClosed() throws Exception {
    Path root = writeSite(tempDir.resolve("remote-ref"), "openapi.yaml");
    Files.writeString(
        root.resolve("openapi.yaml"),
        """
        openapi: 3.0.3
        info:
          title: Pets
          version: "1.0.0"
        paths:
          /pets:
            $ref: https://example.com/paths/pets.yaml
        """,
        StandardCharsets.UTF_8);
    VirtualSiteException ex =
        assertThrows(
            VirtualSiteException.class,
            () -> new PSOpenApiYamlVirtualSiteSource().discover(config(root, "openapi.yaml")));
    assertTrue(ex.getMessage().toLowerCase().contains("$ref") || ex.getMessage().contains("ref"),
        ex.getMessage());
    assertTrue(ex.getMessage().toLowerCase().contains("live"), ex.getMessage());
  }

  @Test
  void swagger2FailsClosed() throws Exception {
    Path root = writeSite(tempDir.resolve("swagger2"), "openapi.yaml");
    Files.writeString(
        root.resolve("openapi.yaml"),
        """
        swagger: "2.0"
        info:
          title: Legacy
          version: "1.0.0"
        paths:
          /pets:
            get:
              summary: List
        """,
        StandardCharsets.UTF_8);
    VirtualSiteException ex =
        assertThrows(
            VirtualSiteException.class,
            () -> new PSOpenApiYamlVirtualSiteSource().discover(config(root, "openapi.yaml")));
    assertTrue(ex.getMessage().toLowerCase().contains("openapi"), ex.getMessage());
  }

  @Test
  void pathTraversalAndAbsolutePathFailClosed() {
    VirtualSiteException rel =
        assertThrows(
            VirtualSiteException.class,
            () ->
                PSOpenApiYamlVirtualSiteSource.resolvePagePath(
                    "../outside.html", "esc", "8.2", "test"));
    assertTrue(rel.getMessage().contains("path"), rel.getMessage());
    VirtualSiteException abs =
        assertThrows(
            VirtualSiteException.class,
            () ->
                PSOpenApiYamlVirtualSiteSource.resolvePagePath(
                    "/etc/passwd.html", "esc", "8.2", "test"));
    assertTrue(abs.getMessage().toLowerCase().contains("relative"), abs.getMessage());
  }

  @Test
  void openApiFileTraversalFailsClosed() {
    VirtualSiteException ex =
        assertThrows(
            VirtualSiteException.class,
            () ->
                PSOpenApiYamlVirtualSiteSource.resolveOpenApiFile(
                    tempDir.resolve("q-escape"), "../outside.yaml"));
    assertTrue(ex.getMessage().contains("file") || ex.getMessage().contains(".."), ex.getMessage());
  }

  @Test
  @EnabledOnOs(OS.WINDOWS)
  void windowsAbsoluteOpenApiFileRejected() {
    VirtualSiteException ex =
        assertThrows(
            VirtualSiteException.class,
            () ->
                PSOpenApiYamlVirtualSiteSource.resolveOpenApiFile(
                    tempDir.resolve("win-root"), "C:/docs/evil.yaml"));
    assertTrue(ex.getMessage().toLowerCase().contains("relative"), ex.getMessage());
  }

  @Test
  @EnabledOnOs({OS.LINUX, OS.MAC})
  void unixAbsoluteOpenApiFileRejected() {
    VirtualSiteException ex =
        assertThrows(
            VirtualSiteException.class,
            () ->
                PSOpenApiYamlVirtualSiteSource.resolveOpenApiFile(
                    tempDir.resolve("unix-root"), "/etc/passwd.yaml"));
    assertTrue(ex.getMessage().toLowerCase().contains("relative"), ex.getMessage());
  }

  @Test
  void loadRereadsLocalFileWithoutCache() throws Exception {
    Path root = writeSite(tempDir.resolve("live"), "openapi.yaml");
    Path spec = root.resolve("openapi.yaml");
    Files.writeString(
        spec, sampleSpec("First", "firstOp", "token-AAA"), StandardCharsets.UTF_8);
    PSOpenApiYamlVirtualSiteSource source = new PSOpenApiYamlVirtualSiteSource();
    VirtualSiteConfig cfg = config(root, "openapi.yaml");
    VirtualItem first = source.load(cfg, source.discover(cfg).get(0));
    assertTrue(first.markdownBody().contains("token-AAA"));
    Files.writeString(
        spec, sampleSpec("Second", "secondOp", "token-BBB"), StandardCharsets.UTF_8);
    VirtualItem second = source.load(cfg, source.discover(cfg).get(0));
    assertEquals("Second", second.frontmatter().title());
    assertTrue(second.markdownBody().contains("token-BBB"));
    assertFalse(second.markdownBody().contains("token-AAA"));
  }

  @Test
  void factorySelectsOpenApiYamlAndLeavesPeersUnchanged() throws Exception {
    IPSVirtualSiteSource source =
        PSVirtualSiteSourceFactory.create(VirtualSiteSourceType.OPENAPI_YAML);
    assertInstanceOf(PSOpenApiYamlVirtualSiteSource.class, source);
    assertEquals("openapi-yaml", source.sourceType());
    IPSVirtualSiteSource byName = PSVirtualSiteSourceFactory.createFromWireName("openapi-yaml");
    assertInstanceOf(PSOpenApiYamlVirtualSiteSource.class, byName);
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
    assertInstanceOf(
        PSRobotsTxtVirtualSiteSource.class,
        PSVirtualSiteSourceFactory.createFromWireName("robots-txt"));
    assertInstanceOf(
        PSLlmsTxtVirtualSiteSource.class,
        PSVirtualSiteSourceFactory.createFromWireName("llms-txt"));
    VirtualSiteException ex =
        assertThrows(
            VirtualSiteException.class,
            () -> PSVirtualSiteSourceFactory.createFromWireName("sql-api"));
    assertTrue(ex.getMessage().contains("sql-api"));
    assertTrue(ex.getMessage().contains("openapi-yaml"));
    assertTrue(ex.getMessage().contains("llms-txt"));
  }

  @Test
  void buildServiceFactoryWiresOpenApiYamlAndEmitsHtml() throws Exception {
    Path root = writeSite(tempDir.resolve("build-oa"), "openapi.yaml");
    Files.writeString(
        root.resolve("openapi.yaml"),
        sampleSpec("List pets", "listPets", "Hello-from-openapi"),
        StandardCharsets.UTF_8);
    Files.writeString(
        root.resolve("_theme").resolve("page.html"),
        "<html><body><h1>${pageTitle}</h1>${content}</body></html>",
        StandardCharsets.UTF_8);
    Path out = tempDir.resolve("build-out");
    PSVirtualSiteBuildService service =
        PSVirtualSiteBuildService.forSourceType(VirtualSiteSourceType.OPENAPI_YAML);
    assertInstanceOf(PSOpenApiYamlVirtualSiteSource.class, service.source());

    PSVirtualSiteBuildResult result = service.build(root, out, "openapi-docs");
    assertTrue(result.pageCount() > 0);
    Path html = out.resolve("8.2").resolve("listPets-1.html");
    assertTrue(Files.isRegularFile(html), "missing " + html);
    String body = Files.readString(html, StandardCharsets.UTF_8);
    assertTrue(body.contains("List pets"), body);
    assertTrue(body.contains("Hello-from-openapi"), body);
  }

  @Test
  void secondBuildAfterOpenApiAndConfigEditEmitsUpdatedHtmlWithoutRestart() throws Exception {
    Path root = writeSite(tempDir.resolve("oa-rebuild"), "openapi.yaml");
    writeOpenApiYaml(root, "First Site Title", "openapi.yaml");
    Files.writeString(
        root.resolve("_theme").resolve("page.html"),
        "<html><body><h1>${siteTitle}</h1><h2>${pageTitle}</h2>${content}</body></html>",
        StandardCharsets.UTF_8);
    Path spec = root.resolve("openapi.yaml");
    Files.writeString(
        spec, sampleSpec("First", "firstOp", "unique-token-AAA"), StandardCharsets.UTF_8);

    Path out = tempDir.resolve("oa-rebuild-out");
    PSOpenApiYamlVirtualSiteSource source = new PSOpenApiYamlVirtualSiteSource();
    PSVirtualSiteBuildService service =
        new PSVirtualSiteBuildService(source, new PSInMemoryVirtualParticipantService());

    PSVirtualSiteBuildResult first = service.build(root, out, "openapi-docs");
    assertEquals(1, first.pageCount());
    Path firstHtmlPath = out.resolve("8.2").resolve("firstOp-1.html");
    assertTrue(Files.isRegularFile(firstHtmlPath), "missing " + firstHtmlPath);
    String firstHtml = Files.readString(firstHtmlPath, StandardCharsets.UTF_8);
    assertTrue(firstHtml.contains("First Site Title"), firstHtml);
    assertTrue(firstHtml.contains("unique-token-AAA"), firstHtml);

    Files.writeString(
        spec, sampleSpec("Second", "secondOp", "unique-token-BBB"), StandardCharsets.UTF_8);
    writeOpenApiYaml(root, "Second Site Title", "openapi.yaml");

    VirtualSiteConfig reloaded =
        VirtualSiteConfigLoader.load(
            root, VirtualSiteConfigLoader.DEFAULT_CONFIG_FILE, "openapi-docs");
    assertEquals("Second Site Title", reloaded.siteTitle());
    assertEquals("openapi.yaml", reloaded.openapi().file());
    List<VirtualItemRef> refs = source.discover(reloaded);
    assertEquals(1, refs.size());
    assertEquals("secondOp-1", refs.get(0).id());
    VirtualItem loaded = source.load(reloaded, refs.get(0));
    assertEquals("Second", loaded.frontmatter().title());
    assertTrue(loaded.markdownBody().contains("unique-token-BBB"), loaded.markdownBody());
    assertFalse(loaded.markdownBody().contains("unique-token-AAA"), loaded.markdownBody());

    PSVirtualSiteBuildResult second = service.build(root, out, "openapi-docs");
    assertEquals(1, second.pageCount());
    Path secondHtmlPath = out.resolve("8.2").resolve("secondOp-1.html");
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
  void yamlLoaderParsesOpenApiSpec() throws Exception {
    Path root = writeSite(tempDir.resolve("yaml-oa"), "custom-openapi.yaml");
    Files.writeString(
        root.resolve("custom-openapi.yaml"),
        sampleSpec("Guide", "getGuide", ""),
        StandardCharsets.UTF_8);
    VirtualSiteConfig loaded =
        VirtualSiteConfigLoader.load(
            root, VirtualSiteConfigLoader.DEFAULT_CONFIG_FILE, "openapi-docs");
    assertEquals("custom-openapi.yaml", loaded.openapi().file());
  }

  @Test
  void omittedPathDefaultsToIdHtmlUnderVersion() throws Exception {
    Path relative = PSOpenApiYamlVirtualSiteSource.resolvePagePath("", "plain-id", "8.2", "test");
    assertEquals(Path.of("8.2", "plain-id.html"), relative);
  }

  @Test
  void slugForPathStripsUnsafeChars() {
    assertEquals("listPets", PSOpenApiYamlVirtualSiteSource.slugForPath("listPets"));
    assertEquals("GET-pets", PSOpenApiYamlVirtualSiteSource.slugForPath("GET /pets"));
    assertEquals("openapi", PSOpenApiYamlVirtualSiteSource.slugForPath(":::"));
  }

  private static Path writeSite(Path root, String file) throws Exception {
    Files.createDirectories(root.resolve("8.2"));
    Files.createDirectories(root.resolve("_theme"));
    String openapiBlock;
    if (file != null && !file.isBlank()) {
      openapiBlock =
          """
          openapi:
            file: %s
          """
              .formatted(file);
    } else {
      openapiBlock = "";
    }
    Files.writeString(
        root.resolve("_config.yaml"),
        """
        site:
          title: OpenAPI Docs
        versions:
          - id: "8.2"
            label: "8.2"
            path: "8.2"
            default: true
        theme:
          layout: page.html
        %s
        """
            .formatted(openapiBlock),
        StandardCharsets.UTF_8);
    return root;
  }

  private static void writeOpenApiYaml(Path root, String siteTitle, String file) throws Exception {
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
        openapi:
          file: %s
        """
            .formatted(siteTitle, file),
        StandardCharsets.UTF_8);
  }

  private static VirtualSiteConfig config(Path root, String file) {
    OpenApiSpec spec = file != null ? new OpenApiSpec(null, file) : null;
    return new VirtualSiteConfig(
        root,
        "OpenAPI Docs",
        "",
        "page.html",
        List.of(new VirtualSiteConfig.VersionSpec("8.2", "8.2", "8.2", true)),
        List.of(),
        "openapi-docs",
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        spec);
  }

  private static String sampleSpec(String summary, String operationId, String description) {
    String descLine =
        description == null || description.isBlank()
            ? ""
            : "\n      description: " + description;
    return """
        openapi: 3.0.3
        info:
          title: Pets API
          version: "1.0.0"
        paths:
          /pets:
            get:
              summary: %s
              operationId: %s%s
        """
        .formatted(summary, operationId, descLine);
  }
}
