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

import com.percussion.services.virtualsite.VirtualSiteConfig.AsyncApiSpec;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

class PSAsyncApiYamlVirtualSiteSourceTest {

  @TempDir Path tempDir;

  @Test
  void discoverAndLoadFromLocalAsyncApiFixture() throws Exception {
    Path root = writeSite(tempDir.resolve("aa-file"), "asyncapi.yaml");
    Files.writeString(
        root.resolve("asyncapi.yaml"),
        sampleSpecV2("Inform about lighting", "onLightMeasured", "Hello-from-asyncapi"),
        StandardCharsets.UTF_8);
    PSAsyncApiYamlVirtualSiteSource source = new PSAsyncApiYamlVirtualSiteSource();
    assertEquals(VirtualSiteSourceType.ASYNCAPI_YAML.wireName(), source.sourceType());
    VirtualSiteConfig cfg = config(root, "asyncapi.yaml");
    List<VirtualItemRef> refs = source.discover(cfg);
    assertEquals(1, refs.size());
    assertEquals("onLightMeasured-1", refs.get(0).id());
    assertEquals("Inform about lighting", refs.get(0).title());
    assertEquals(Path.of("8.2", "onLightMeasured-1.html"), refs.get(0).relativePath());
    VirtualItem item = source.load(cfg, refs.get(0));
    assertEquals("", item.frontmatter().description());
    assertTrue(item.markdownBody().contains("Hello-from-asyncapi"), item.markdownBody());
    assertTrue(item.markdownBody().contains("PUBLISH"), item.markdownBody());
    assertTrue(item.markdownBody().contains("light/measured"), item.markdownBody());
    assertEquals("asyncapi.yaml", item.absolutePath().getFileName().toString());
  }

  @Test
  void omittedAsyncApiMappingDefaultsToAsyncapiYaml() throws Exception {
    Path root = writeSite(tempDir.resolve("default-aa"), null);
    Files.writeString(
        root.resolve(PSAsyncApiYamlVirtualSiteSource.DEFAULT_ASYNCAPI_FILE),
        sampleSpecV2("Guide", "getGuide", "notes"),
        StandardCharsets.UTF_8);
    VirtualSiteConfig cfg =
        VirtualSiteConfigLoader.load(
            root, VirtualSiteConfigLoader.DEFAULT_CONFIG_FILE, "asyncapi-docs");
    List<VirtualItemRef> refs = new PSAsyncApiYamlVirtualSiteSource().discover(cfg);
    assertEquals(1, refs.size());
    assertEquals("getGuide-1", refs.get(0).id());
  }

  @Test
  void infoOnlyFixtureStillEmitsOnePage() throws Exception {
    Path root = writeSite(tempDir.resolve("info-only"), "asyncapi.yaml");
    Files.writeString(
        root.resolve("asyncapi.yaml"),
        """
        asyncapi: 2.6.0
        info:
          title: Product Events
          description: no operations configured yet
          version: "1.0.0"
        channels: {}
        """,
        StandardCharsets.UTF_8);
    List<VirtualItemRef> refs =
        new PSAsyncApiYamlVirtualSiteSource().discover(config(root, "asyncapi.yaml"));
    assertEquals(1, refs.size());
    assertEquals("asyncapi", refs.get(0).id());
    assertEquals("Product Events", refs.get(0).title());
    VirtualItem item =
        new PSAsyncApiYamlVirtualSiteSource().load(config(root, "asyncapi.yaml"), refs.get(0));
    assertTrue(item.markdownBody().contains("no operations configured yet"), item.markdownBody());
  }

  @Test
  void mapsMultipleChannelOperationsToPages() throws Exception {
    Path root = writeSite(tempDir.resolve("multi-op"), "asyncapi.yaml");
    Files.writeString(
        root.resolve("asyncapi.yaml"),
        """
        asyncapi: 2.6.0
        info:
          title: Lights
          version: "1.0.0"
        channels:
          light/measured:
            publish:
              summary: Inform about lighting
              operationId: onLightMeasured
            subscribe:
              summary: Receive lighting
              operationId: receiveLight
          light/turn/on:
            subscribe:
              summary: Turn on
              operationId: turnOn
        """,
        StandardCharsets.UTF_8);
    List<VirtualItemRef> refs =
        new PSAsyncApiYamlVirtualSiteSource().discover(config(root, "asyncapi.yaml"));
    assertEquals(3, refs.size());
    assertEquals("onLightMeasured-1", refs.get(0).id());
    assertEquals("receiveLight-2", refs.get(1).id());
    assertEquals("turnOn-3", refs.get(2).id());
  }

  @Test
  void mapsAsyncApi3OperationsToPages() throws Exception {
    Path root = writeSite(tempDir.resolve("v3-ops"), "asyncapi.yaml");
    Files.writeString(
        root.resolve("asyncapi.yaml"),
        """
        asyncapi: 3.0.0
        info:
          title: User signup
          version: "1.0.0"
        channels:
          userSignedUp:
            address: user/signedup
        operations:
          onUserSignedUp:
            action: receive
            channel:
              $ref: '#/channels/userSignedUp'
            summary: On user signed up
            description: Hello-from-asyncapi3
        """,
        StandardCharsets.UTF_8);
    List<VirtualItemRef> refs =
        new PSAsyncApiYamlVirtualSiteSource().discover(config(root, "asyncapi.yaml"));
    assertEquals(1, refs.size());
    assertEquals("onUserSignedUp-1", refs.get(0).id());
    assertEquals("On user signed up", refs.get(0).title());
    VirtualItem item =
        new PSAsyncApiYamlVirtualSiteSource().load(config(root, "asyncapi.yaml"), refs.get(0));
    assertTrue(item.markdownBody().contains("Hello-from-asyncapi3"), item.markdownBody());
    assertTrue(item.markdownBody().contains("RECEIVE"), item.markdownBody());
    assertTrue(item.markdownBody().contains("userSignedUp"), item.markdownBody());
  }

  @Test
  void emptyAsyncApiFailsClosed() throws Exception {
    Path root = writeSite(tempDir.resolve("empty-aa"), "asyncapi.yaml");
    Files.writeString(root.resolve("asyncapi.yaml"), "   \n", StandardCharsets.UTF_8);
    VirtualSiteException ex =
        assertThrows(
            VirtualSiteException.class,
            () -> new PSAsyncApiYamlVirtualSiteSource().discover(config(root, "asyncapi.yaml")));
    assertTrue(ex.getMessage().toLowerCase().contains("empty"), ex.getMessage());
  }

  @Test
  void unknownIdOnLoadFailsClosed() throws Exception {
    Path root = writeSite(tempDir.resolve("unknown-id"), "asyncapi.yaml");
    Files.writeString(
        root.resolve("asyncapi.yaml"),
        sampleSpecV2("Inform about lighting", "onLightMeasured", ""),
        StandardCharsets.UTF_8);
    PSAsyncApiYamlVirtualSiteSource source = new PSAsyncApiYamlVirtualSiteSource();
    VirtualSiteConfig cfg = config(root, "asyncapi.yaml");
    source.discover(cfg);
    VirtualItemRef fake =
        new VirtualItemRef("missing-id", "8.2", Path.of("8.2", "missing-id.html"), 0, "x");
    VirtualSiteException ex = assertThrows(VirtualSiteException.class, () -> source.load(cfg, fake));
    assertTrue(ex.getMessage().contains("Unknown"), ex.getMessage());
    assertTrue(ex.getMessage().contains("missing-id"), ex.getMessage());
  }

  @Test
  void asyncApiUrlIsRejected() throws Exception {
    Path root = writeSite(tempDir.resolve("remote-url"), "asyncapi.yaml");
    Files.writeString(
        root.resolve("asyncapi.yaml"),
        sampleSpecV2("Inform about lighting", "onLightMeasured", ""),
        StandardCharsets.UTF_8);
    VirtualSiteConfig cfg =
        new VirtualSiteConfig(
            root,
            "AsyncAPI Docs",
            "",
            "page.html",
            List.of(new VirtualSiteConfig.VersionSpec("8.2", "8.2", "8.2", true)),
            List.of(),
            "asyncapi-docs",
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            new AsyncApiSpec("https://example.com/asyncapi.yaml", "asyncapi.yaml"));
    VirtualSiteException ex =
        assertThrows(
            VirtualSiteException.class, () -> new PSAsyncApiYamlVirtualSiteSource().discover(cfg));
    assertTrue(ex.getMessage().contains("asyncapi.url"), ex.getMessage());
    assertTrue(
        ex.getMessage().toLowerCase().contains("live") || ex.getMessage().contains("remote"),
        ex.getMessage());
  }

  @Test
  void remoteRefFailsClosed() throws Exception {
    Path root = writeSite(tempDir.resolve("remote-ref"), "asyncapi.yaml");
    Files.writeString(
        root.resolve("asyncapi.yaml"),
        """
        asyncapi: 2.6.0
        info:
          title: Lights
          version: "1.0.0"
        channels:
          light/measured:
            $ref: https://example.com/channels/light.yaml
        """,
        StandardCharsets.UTF_8);
    VirtualSiteException ex =
        assertThrows(
            VirtualSiteException.class,
            () -> new PSAsyncApiYamlVirtualSiteSource().discover(config(root, "asyncapi.yaml")));
    assertTrue(
        ex.getMessage().toLowerCase().contains("$ref") || ex.getMessage().contains("ref"),
        ex.getMessage());
    assertTrue(ex.getMessage().toLowerCase().contains("live"), ex.getMessage());
  }

  @Test
  void asyncApi1FailsClosed() throws Exception {
    Path root = writeSite(tempDir.resolve("asyncapi1"), "asyncapi.yaml");
    Files.writeString(
        root.resolve("asyncapi.yaml"),
        """
        asyncapi: "1.2.0"
        info:
          title: Legacy
          version: "1.0.0"
        topics:
          light.measured:
            publish:
              summary: List
        """,
        StandardCharsets.UTF_8);
    VirtualSiteException ex =
        assertThrows(
            VirtualSiteException.class,
            () -> new PSAsyncApiYamlVirtualSiteSource().discover(config(root, "asyncapi.yaml")));
    assertTrue(ex.getMessage().toLowerCase().contains("asyncapi"), ex.getMessage());
  }

  @Test
  void pathTraversalAndAbsolutePathFailClosed() {
    VirtualSiteException rel =
        assertThrows(
            VirtualSiteException.class,
            () ->
                PSAsyncApiYamlVirtualSiteSource.resolvePagePath(
                    "../outside.html", "esc", "8.2", "test"));
    assertTrue(rel.getMessage().contains("path"), rel.getMessage());
    VirtualSiteException abs =
        assertThrows(
            VirtualSiteException.class,
            () ->
                PSAsyncApiYamlVirtualSiteSource.resolvePagePath(
                    "/etc/passwd.html", "esc", "8.2", "test"));
    assertTrue(abs.getMessage().toLowerCase().contains("relative"), abs.getMessage());
  }

  @Test
  void asyncApiFileTraversalFailsClosed() {
    VirtualSiteException ex =
        assertThrows(
            VirtualSiteException.class,
            () ->
                PSAsyncApiYamlVirtualSiteSource.resolveAsyncApiFile(
                    tempDir.resolve("q-escape"), "../outside.yaml"));
    assertTrue(ex.getMessage().contains("file") || ex.getMessage().contains(".."), ex.getMessage());
  }

  @Test
  @EnabledOnOs(OS.WINDOWS)
  void windowsAbsoluteAsyncApiFileRejected() {
    VirtualSiteException ex =
        assertThrows(
            VirtualSiteException.class,
            () ->
                PSAsyncApiYamlVirtualSiteSource.resolveAsyncApiFile(
                    tempDir.resolve("win-root"), "C:/docs/evil.yaml"));
    assertTrue(ex.getMessage().toLowerCase().contains("relative"), ex.getMessage());
  }

  @Test
  @EnabledOnOs({OS.LINUX, OS.MAC})
  void unixAbsoluteAsyncApiFileRejected() {
    VirtualSiteException ex =
        assertThrows(
            VirtualSiteException.class,
            () ->
                PSAsyncApiYamlVirtualSiteSource.resolveAsyncApiFile(
                    tempDir.resolve("unix-root"), "/etc/passwd.yaml"));
    assertTrue(ex.getMessage().toLowerCase().contains("relative"), ex.getMessage());
  }

  @Test
  void loadRereadsLocalFileWithoutCache() throws Exception {
    Path root = writeSite(tempDir.resolve("live"), "asyncapi.yaml");
    Path spec = root.resolve("asyncapi.yaml");
    Files.writeString(
        spec, sampleSpecV2("First", "firstOp", "token-AAA"), StandardCharsets.UTF_8);
    PSAsyncApiYamlVirtualSiteSource source = new PSAsyncApiYamlVirtualSiteSource();
    VirtualSiteConfig cfg = config(root, "asyncapi.yaml");
    VirtualItem first = source.load(cfg, source.discover(cfg).get(0));
    assertTrue(first.markdownBody().contains("token-AAA"));
    Files.writeString(
        spec, sampleSpecV2("Second", "secondOp", "token-BBB"), StandardCharsets.UTF_8);
    VirtualItem second = source.load(cfg, source.discover(cfg).get(0));
    assertEquals("Second", second.frontmatter().title());
    assertTrue(second.markdownBody().contains("token-BBB"));
    assertFalse(second.markdownBody().contains("token-AAA"));
  }

  @Test
  void factorySelectsAsyncApiYamlAndLeavesPeersUnchanged() throws Exception {
    IPSVirtualSiteSource source =
        PSVirtualSiteSourceFactory.create(VirtualSiteSourceType.ASYNCAPI_YAML);
    assertInstanceOf(PSAsyncApiYamlVirtualSiteSource.class, source);
    assertEquals("asyncapi-yaml", source.sourceType());
    IPSVirtualSiteSource byName = PSVirtualSiteSourceFactory.createFromWireName("asyncapi-yaml");
    assertInstanceOf(PSAsyncApiYamlVirtualSiteSource.class, byName);
    assertInstanceOf(
        PSOpenApiYamlVirtualSiteSource.class,
        PSVirtualSiteSourceFactory.createFromWireName("openapi-yaml"));
    assertInstanceOf(
        PSGitFilesystemVirtualSiteSource.class,
        PSVirtualSiteSourceFactory.createFromWireName("git-filesystem"));
    assertInstanceOf(
        PSLlmsTxtVirtualSiteSource.class,
        PSVirtualSiteSourceFactory.createFromWireName("llms-txt"));
    VirtualSiteException ex =
        assertThrows(
            VirtualSiteException.class,
            () -> PSVirtualSiteSourceFactory.createFromWireName("sql-api"));
    assertTrue(ex.getMessage().contains("sql-api"));
    assertTrue(ex.getMessage().contains("asyncapi-yaml"));
    assertTrue(ex.getMessage().contains("openapi-yaml"));
  }

  @Test
  void buildServiceFactoryWiresAsyncApiYamlAndEmitsHtml() throws Exception {
    Path root = writeSite(tempDir.resolve("build-aa"), "asyncapi.yaml");
    Files.writeString(
        root.resolve("asyncapi.yaml"),
        sampleSpecV2("Inform about lighting", "onLightMeasured", "Hello-from-asyncapi"),
        StandardCharsets.UTF_8);
    Files.writeString(
        root.resolve("_theme").resolve("page.html"),
        "<html><body><h1>${pageTitle}</h1>${content}</body></html>",
        StandardCharsets.UTF_8);
    Path out = tempDir.resolve("build-out");
    PSVirtualSiteBuildService service =
        PSVirtualSiteBuildService.forSourceType(VirtualSiteSourceType.ASYNCAPI_YAML);
    assertInstanceOf(PSAsyncApiYamlVirtualSiteSource.class, service.source());

    PSVirtualSiteBuildResult result = service.build(root, out, "asyncapi-docs");
    assertTrue(result.pageCount() > 0);
    Path html = out.resolve("8.2").resolve("onLightMeasured-1.html");
    assertTrue(Files.isRegularFile(html), "missing " + html);
    String body = Files.readString(html, StandardCharsets.UTF_8);
    assertTrue(body.contains("Inform about lighting"), body);
    assertTrue(body.contains("Hello-from-asyncapi"), body);
  }

  @Test
  void secondBuildAfterAsyncApiAndConfigEditEmitsUpdatedHtmlWithoutRestart() throws Exception {
    Path root = writeSite(tempDir.resolve("aa-rebuild"), "asyncapi.yaml");
    writeAsyncApiYaml(root, "First Site Title", "asyncapi.yaml");
    Files.writeString(
        root.resolve("_theme").resolve("page.html"),
        "<html><body><h1>${siteTitle}</h1><h2>${pageTitle}</h2>${content}</body></html>",
        StandardCharsets.UTF_8);
    Path spec = root.resolve("asyncapi.yaml");
    Files.writeString(
        spec, sampleSpecV2("First", "firstOp", "unique-token-AAA"), StandardCharsets.UTF_8);

    Path out = tempDir.resolve("aa-rebuild-out");
    PSAsyncApiYamlVirtualSiteSource source = new PSAsyncApiYamlVirtualSiteSource();
    PSVirtualSiteBuildService service =
        new PSVirtualSiteBuildService(source, new PSInMemoryVirtualParticipantService());

    PSVirtualSiteBuildResult first = service.build(root, out, "asyncapi-docs");
    assertEquals(1, first.pageCount());
    Path firstHtmlPath = out.resolve("8.2").resolve("firstOp-1.html");
    assertTrue(Files.isRegularFile(firstHtmlPath), "missing " + firstHtmlPath);
    String firstHtml = Files.readString(firstHtmlPath, StandardCharsets.UTF_8);
    assertTrue(firstHtml.contains("First Site Title"), firstHtml);
    assertTrue(firstHtml.contains("unique-token-AAA"), firstHtml);

    Files.writeString(
        spec, sampleSpecV2("Second", "secondOp", "unique-token-BBB"), StandardCharsets.UTF_8);
    writeAsyncApiYaml(root, "Second Site Title", "asyncapi.yaml");

    VirtualSiteConfig reloaded =
        VirtualSiteConfigLoader.load(
            root, VirtualSiteConfigLoader.DEFAULT_CONFIG_FILE, "asyncapi-docs");
    assertEquals("Second Site Title", reloaded.siteTitle());
    assertEquals("asyncapi.yaml", reloaded.asyncapi().file());
    List<VirtualItemRef> refs = source.discover(reloaded);
    assertEquals(1, refs.size());
    assertEquals("secondOp-1", refs.get(0).id());
    VirtualItem loaded = source.load(reloaded, refs.get(0));
    assertEquals("Second", loaded.frontmatter().title());
    assertTrue(loaded.markdownBody().contains("unique-token-BBB"), loaded.markdownBody());
    assertFalse(loaded.markdownBody().contains("unique-token-AAA"), loaded.markdownBody());

    PSVirtualSiteBuildResult second = service.build(root, out, "asyncapi-docs");
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
  void yamlLoaderParsesAsyncApiSpec() throws Exception {
    Path root = writeSite(tempDir.resolve("yaml-aa"), "custom-asyncapi.yaml");
    Files.writeString(
        root.resolve("custom-asyncapi.yaml"),
        sampleSpecV2("Guide", "getGuide", ""),
        StandardCharsets.UTF_8);
    VirtualSiteConfig loaded =
        VirtualSiteConfigLoader.load(
            root, VirtualSiteConfigLoader.DEFAULT_CONFIG_FILE, "asyncapi-docs");
    assertEquals("custom-asyncapi.yaml", loaded.asyncapi().file());
  }

  @Test
  void omittedPathDefaultsToIdHtmlUnderVersion() throws Exception {
    Path relative = PSAsyncApiYamlVirtualSiteSource.resolvePagePath("", "plain-id", "8.2", "test");
    assertEquals(Path.of("8.2", "plain-id.html"), relative);
  }

  @Test
  void slugForPathStripsUnsafeChars() {
    assertEquals("onLightMeasured", PSAsyncApiYamlVirtualSiteSource.slugForPath("onLightMeasured"));
    assertEquals("PUBLISH-light-measured", PSAsyncApiYamlVirtualSiteSource.slugForPath("PUBLISH light/measured"));
    assertEquals("asyncapi", PSAsyncApiYamlVirtualSiteSource.slugForPath(":::"));
  }

  private static Path writeSite(Path root, String file) throws Exception {
    Files.createDirectories(root.resolve("8.2"));
    Files.createDirectories(root.resolve("_theme"));
    String asyncapiBlock;
    if (file != null && !file.isBlank()) {
      asyncapiBlock =
          """
          asyncapi:
            file: %s
          """
              .formatted(file);
    } else {
      asyncapiBlock = "";
    }
    Files.writeString(
        root.resolve("_config.yaml"),
        """
        site:
          title: AsyncAPI Docs
        versions:
          - id: "8.2"
            label: "8.2"
            path: "8.2"
            default: true
        theme:
          layout: page.html
        %s
        """
            .formatted(asyncapiBlock),
        StandardCharsets.UTF_8);
    return root;
  }

  private static void writeAsyncApiYaml(Path root, String siteTitle, String file) throws Exception {
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
        asyncapi:
          file: %s
        """
            .formatted(siteTitle, file),
        StandardCharsets.UTF_8);
  }

  private static VirtualSiteConfig config(Path root, String file) {
    AsyncApiSpec spec = file != null ? new AsyncApiSpec(null, file) : null;
    return new VirtualSiteConfig(
        root,
        "AsyncAPI Docs",
        "",
        "page.html",
        List.of(new VirtualSiteConfig.VersionSpec("8.2", "8.2", "8.2", true)),
        List.of(),
        "asyncapi-docs",
        null,
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

  private static String sampleSpecV2(String summary, String operationId, String description) {
    return """
        asyncapi: 2.6.0
        info:
          title: Streetlights
          version: "1.0.0"
        channels:
          "light/measured":
            publish:
              summary: %s
              operationId: %s
              description: %s
        """
        .formatted(summary, operationId, description == null ? "" : description);
  }
}
