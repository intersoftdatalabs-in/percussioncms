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

import com.percussion.services.virtualsite.VirtualSiteConfig.JsonSchemaSpec;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

class PSJsonSchemaVirtualSiteSourceTest {

  @TempDir Path tempDir;

  @Test
  void discoverAndLoadFromLocalJsonSchemaFixture() throws Exception {
    Path root = writeSite(tempDir.resolve("js-file"), "schema.json");
    Files.writeString(
        root.resolve("schema.json"),
        sampleSchema("sku", "SKU", "Hello-from-jsonschema"),
        StandardCharsets.UTF_8);
    PSJsonSchemaVirtualSiteSource source = new PSJsonSchemaVirtualSiteSource();
    assertEquals(VirtualSiteSourceType.JSON_SCHEMA.wireName(), source.sourceType());
    VirtualSiteConfig cfg = config(root, "schema.json");
    List<VirtualItemRef> refs = source.discover(cfg);
    assertEquals(1, refs.size());
    assertEquals("sku-1", refs.get(0).id());
    assertEquals("SKU", refs.get(0).title());
    assertEquals(Path.of("8.2", "sku-1.html"), refs.get(0).relativePath());
    VirtualItem item = source.load(cfg, refs.get(0));
    assertTrue(item.markdownBody().contains("Hello-from-jsonschema"), item.markdownBody());
    assertTrue(item.markdownBody().contains("sku"), item.markdownBody());
    assertEquals("schema.json", item.absolutePath().getFileName().toString());
  }

  @Test
  void omittedJsonSchemaMappingDefaultsToSchemaJson() throws Exception {
    Path root = writeSite(tempDir.resolve("default-js"), null);
    Files.writeString(
        root.resolve(PSJsonSchemaVirtualSiteSource.DEFAULT_JSON_SCHEMA_FILE),
        sampleSchema("guide", "Guide", "notes"),
        StandardCharsets.UTF_8);
    VirtualSiteConfig cfg =
        VirtualSiteConfigLoader.load(
            root, VirtualSiteConfigLoader.DEFAULT_CONFIG_FILE, "jsonschema-docs");
    List<VirtualItemRef> refs = new PSJsonSchemaVirtualSiteSource().discover(cfg);
    assertEquals(1, refs.size());
    assertEquals("guide-1", refs.get(0).id());
  }

  @Test
  void titleOnlyFixtureStillEmitsOnePage() throws Exception {
    Path root = writeSite(tempDir.resolve("title-only"), "schema.json");
    Files.writeString(
        root.resolve("schema.json"),
        """
        {
          "title": "Product catalog schema",
          "description": "Fallback page",
          "type": "object"
        }
        """,
        StandardCharsets.UTF_8);
    List<VirtualItemRef> refs =
        new PSJsonSchemaVirtualSiteSource().discover(config(root, "schema.json"));
    assertEquals(1, refs.size());
    assertEquals("jsonschema", refs.get(0).id());
    assertEquals("Product catalog schema", refs.get(0).title());
    VirtualItem item =
        new PSJsonSchemaVirtualSiteSource().load(config(root, "schema.json"), refs.get(0));
    assertTrue(item.markdownBody().contains("Product catalog schema"), item.markdownBody());
    assertTrue(item.markdownBody().contains("Fallback page"), item.markdownBody());
  }

  @Test
  void mapsPropertiesAndDefsToPages() throws Exception {
    Path root = writeSite(tempDir.resolve("multi-def"), "schema.json");
    Files.writeString(
        root.resolve("schema.json"),
        """
        {
          "title": "Catalog",
          "type": "object",
          "properties": {
            "sku": { "type": "string", "title": "SKU" },
            "name": { "type": "string", "title": "Name" }
          },
          "$defs": {
            "Address": { "type": "object", "title": "Address", "description": "Postal" }
          }
        }
        """,
        StandardCharsets.UTF_8);
    List<VirtualItemRef> refs =
        new PSJsonSchemaVirtualSiteSource().discover(config(root, "schema.json"));
    assertEquals(3, refs.size());
    assertEquals("name-1", refs.get(0).id());
    assertEquals("sku-2", refs.get(1).id());
    assertEquals("Address-3", refs.get(2).id());
  }

  @Test
  void unknownIdOnLoadFailsClosed() throws Exception {
    Path root = writeSite(tempDir.resolve("unknown-id"), "schema.json");
    Files.writeString(
        root.resolve("schema.json"), sampleSchema("sku", "SKU", ""), StandardCharsets.UTF_8);
    PSJsonSchemaVirtualSiteSource source = new PSJsonSchemaVirtualSiteSource();
    VirtualSiteConfig cfg = config(root, "schema.json");
    source.discover(cfg);
    VirtualItemRef fake =
        new VirtualItemRef("missing-id", "8.2", Path.of("8.2", "missing-id.html"), 0, "x");
    VirtualSiteException ex = assertThrows(VirtualSiteException.class, () -> source.load(cfg, fake));
    assertTrue(ex.getMessage().contains("Unknown"), ex.getMessage());
    assertTrue(ex.getMessage().contains("missing-id"), ex.getMessage());
  }

  @Test
  void jsonschemaUrlIsRejected() throws Exception {
    Path root = writeSite(tempDir.resolve("remote-url"), "schema.json");
    Files.writeString(
        root.resolve("schema.json"), sampleSchema("sku", "SKU", ""), StandardCharsets.UTF_8);
    VirtualSiteConfig cfg =
        new VirtualSiteConfig(
            root,
            "JSON Schema Docs",
            "",
            "page.html",
            List.of(new VirtualSiteConfig.VersionSpec("8.2", "8.2", "8.2", true)),
            List.of(),
            "jsonschema-docs",
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            new JsonSchemaSpec("https://example.com/schema.json", "schema.json"));
    VirtualSiteException ex =
        assertThrows(
            VirtualSiteException.class, () -> new PSJsonSchemaVirtualSiteSource().discover(cfg));
    assertTrue(ex.getMessage().contains("jsonschema.url"), ex.getMessage());
    assertTrue(
        ex.getMessage().toLowerCase().contains("live") || ex.getMessage().contains("remote"),
        ex.getMessage());
  }

  @Test
  void remoteRefFailsClosed() throws Exception {
    Path root = writeSite(tempDir.resolve("remote-ref"), "schema.json");
    Files.writeString(
        root.resolve("schema.json"),
        """
        {
          "type": "object",
          "properties": {
            "sku": { "$ref": "https://example.com/sku.json" }
          }
        }
        """,
        StandardCharsets.UTF_8);
    VirtualSiteException ex =
        assertThrows(
            VirtualSiteException.class,
            () -> new PSJsonSchemaVirtualSiteSource().discover(config(root, "schema.json")));
    assertTrue(
        ex.getMessage().toLowerCase().contains("$ref") || ex.getMessage().contains("ref"),
        ex.getMessage());
    assertTrue(ex.getMessage().toLowerCase().contains("live"), ex.getMessage());
  }

  @Test
  void remoteIdHttpFailsClosed() throws Exception {
    Path root = writeSite(tempDir.resolve("remote-id"), "schema.json");
    Files.writeString(
        root.resolve("schema.json"),
        """
        {
          "$id": "https://example.com/schema.json",
          "type": "object",
          "properties": {
            "sku": { "type": "string" }
          }
        }
        """,
        StandardCharsets.UTF_8);
    VirtualSiteException ex =
        assertThrows(
            VirtualSiteException.class,
            () -> new PSJsonSchemaVirtualSiteSource().discover(config(root, "schema.json")));
    assertTrue(ex.getMessage().contains("$id"), ex.getMessage());
    assertTrue(ex.getMessage().toLowerCase().contains("live"), ex.getMessage());
  }

  @Test
  void invalidJsonFailsClosed() throws Exception {
    Path root = writeSite(tempDir.resolve("not-json"), "schema.json");
    Files.writeString(root.resolve("schema.json"), "not a json schema", StandardCharsets.UTF_8);
    VirtualSiteException ex =
        assertThrows(
            VirtualSiteException.class,
            () -> new PSJsonSchemaVirtualSiteSource().discover(config(root, "schema.json")));
    assertTrue(ex.getMessage().toLowerCase().contains("json"), ex.getMessage());
  }

  @Test
  void pathTraversalAndAbsolutePathFailClosed() {
    VirtualSiteException rel =
        assertThrows(
            VirtualSiteException.class,
            () ->
                PSJsonSchemaVirtualSiteSource.resolvePagePath(
                    "../outside.html", "esc", "8.2", "test"));
    assertTrue(rel.getMessage().contains("path"), rel.getMessage());
    VirtualSiteException abs =
        assertThrows(
            VirtualSiteException.class,
            () ->
                PSJsonSchemaVirtualSiteSource.resolvePagePath(
                    "/etc/passwd.html", "esc", "8.2", "test"));
    assertTrue(abs.getMessage().toLowerCase().contains("relative"), abs.getMessage());
  }

  @Test
  void jsonSchemaFileTraversalFailsClosed() {
    VirtualSiteException ex =
        assertThrows(
            VirtualSiteException.class,
            () ->
                PSJsonSchemaVirtualSiteSource.resolveJsonSchemaFile(
                    tempDir.resolve("q-escape"), "../outside.json"));
    assertTrue(ex.getMessage().contains("file") || ex.getMessage().contains(".."), ex.getMessage());
  }

  @Test
  @EnabledOnOs(OS.WINDOWS)
  void windowsAbsoluteJsonSchemaFileRejected() {
    VirtualSiteException ex =
        assertThrows(
            VirtualSiteException.class,
            () ->
                PSJsonSchemaVirtualSiteSource.resolveJsonSchemaFile(
                    tempDir.resolve("win-root"), "C:/docs/evil.json"));
    assertTrue(ex.getMessage().toLowerCase().contains("relative"), ex.getMessage());
  }

  @Test
  @EnabledOnOs({OS.LINUX, OS.MAC})
  void unixAbsoluteJsonSchemaFileRejected() {
    VirtualSiteException ex =
        assertThrows(
            VirtualSiteException.class,
            () ->
                PSJsonSchemaVirtualSiteSource.resolveJsonSchemaFile(
                    tempDir.resolve("unix-root"), "/etc/passwd.json"));
    assertTrue(ex.getMessage().toLowerCase().contains("relative"), ex.getMessage());
  }

  @Test
  void loadRereadsLocalFileWithoutCache() throws Exception {
    Path root = writeSite(tempDir.resolve("live"), "schema.json");
    Path spec = root.resolve("schema.json");
    Files.writeString(spec, sampleSchema("firstOp", "First", "token-AAA"), StandardCharsets.UTF_8);
    PSJsonSchemaVirtualSiteSource source = new PSJsonSchemaVirtualSiteSource();
    VirtualSiteConfig cfg = config(root, "schema.json");
    VirtualItem first = source.load(cfg, source.discover(cfg).get(0));
    assertTrue(
        first.markdownBody().contains("firstOp") || first.frontmatter().title().contains("First"));
    Files.writeString(spec, sampleSchema("secondOp", "Second", "token-BBB"), StandardCharsets.UTF_8);
    VirtualItem second = source.load(cfg, source.discover(cfg).get(0));
    assertEquals("Second", second.frontmatter().title());
    assertTrue(second.markdownBody().contains("secondOp"));
    assertFalse(second.markdownBody().contains("firstOp"));
  }

  @Test
  void factorySelectsJsonSchemaAndLeavesPeersUnchanged() throws Exception {
    IPSVirtualSiteSource source =
        PSVirtualSiteSourceFactory.create(VirtualSiteSourceType.JSON_SCHEMA);
    assertInstanceOf(PSJsonSchemaVirtualSiteSource.class, source);
    assertEquals("json-schema", source.sourceType());
    IPSVirtualSiteSource byName = PSVirtualSiteSourceFactory.createFromWireName("json-schema");
    assertInstanceOf(PSJsonSchemaVirtualSiteSource.class, byName);
    assertInstanceOf(
        PSGraphQlSdlVirtualSiteSource.class,
        PSVirtualSiteSourceFactory.createFromWireName("graphql-sdl"));
    assertInstanceOf(
        PSAsyncApiYamlVirtualSiteSource.class,
        PSVirtualSiteSourceFactory.createFromWireName("asyncapi-yaml"));
    assertInstanceOf(
        PSOpenApiYamlVirtualSiteSource.class,
        PSVirtualSiteSourceFactory.createFromWireName("openapi-yaml"));
    VirtualSiteException ex =
        assertThrows(
            VirtualSiteException.class,
            () -> PSVirtualSiteSourceFactory.createFromWireName("sql-api"));
    assertTrue(ex.getMessage().contains("sql-api"));
    assertTrue(ex.getMessage().contains("json-schema"));
    assertTrue(ex.getMessage().contains("graphql-sdl"));
  }

  @Test
  void buildServiceFactoryWiresJsonSchemaAndEmitsHtml() throws Exception {
    Path root = writeSite(tempDir.resolve("build-js"), "schema.json");
    Files.writeString(
        root.resolve("schema.json"),
        sampleSchema("sku", "SKU", "Hello-from-jsonschema"),
        StandardCharsets.UTF_8);
    Files.writeString(
        root.resolve("_theme").resolve("page.html"),
        "<html><body><h1>${pageTitle}</h1>${content}</body></html>",
        StandardCharsets.UTF_8);
    Path out = tempDir.resolve("build-out");
    PSVirtualSiteBuildService service =
        PSVirtualSiteBuildService.forSourceType(VirtualSiteSourceType.JSON_SCHEMA);
    assertInstanceOf(PSJsonSchemaVirtualSiteSource.class, service.source());

    PSVirtualSiteBuildResult result = service.build(root, out, "jsonschema-docs");
    assertTrue(result.pageCount() > 0);
    Path html = out.resolve("8.2").resolve("sku-1.html");
    assertTrue(Files.isRegularFile(html), "missing " + html);
    String body = Files.readString(html, StandardCharsets.UTF_8);
    assertTrue(body.contains("SKU") || body.contains("sku"), body);
  }

  @Test
  void secondBuildAfterJsonSchemaAndConfigEditEmitsUpdatedHtmlWithoutRestart() throws Exception {
    Path root = writeSite(tempDir.resolve("js-rebuild"), "schema.json");
    writeJsonSchemaYaml(root, "First Site Title", "schema.json");
    Files.writeString(
        root.resolve("_theme").resolve("page.html"),
        "<html><body><h1>${siteTitle}</h1><h2>${pageTitle}</h2>${content}</body></html>",
        StandardCharsets.UTF_8);
    Path spec = root.resolve("schema.json");
    Files.writeString(spec, sampleSchema("firstOp", "First", "unique-token-AAA"), StandardCharsets.UTF_8);

    Path out = tempDir.resolve("js-rebuild-out");
    PSJsonSchemaVirtualSiteSource source = new PSJsonSchemaVirtualSiteSource();
    PSVirtualSiteBuildService service =
        new PSVirtualSiteBuildService(source, new PSInMemoryVirtualParticipantService());

    PSVirtualSiteBuildResult first = service.build(root, out, "jsonschema-docs");
    assertEquals(1, first.pageCount());
    Path firstHtmlPath = out.resolve("8.2").resolve("firstOp-1.html");
    assertTrue(Files.isRegularFile(firstHtmlPath), "missing " + firstHtmlPath);
    String firstHtml = Files.readString(firstHtmlPath, StandardCharsets.UTF_8);
    assertTrue(firstHtml.contains("First Site Title"), firstHtml);

    Files.writeString(
        spec, sampleSchema("secondOp", "Second", "unique-token-BBB"), StandardCharsets.UTF_8);
    writeJsonSchemaYaml(root, "Second Site Title", "schema.json");

    VirtualSiteConfig reloaded =
        VirtualSiteConfigLoader.load(
            root, VirtualSiteConfigLoader.DEFAULT_CONFIG_FILE, "jsonschema-docs");
    assertEquals("Second Site Title", reloaded.siteTitle());
    assertEquals("schema.json", reloaded.jsonschema().file());
    List<VirtualItemRef> refs = source.discover(reloaded);
    assertEquals(1, refs.size());
    assertEquals("secondOp-1", refs.get(0).id());
    VirtualItem loaded = source.load(reloaded, refs.get(0));
    assertEquals("Second", loaded.frontmatter().title());

    PSVirtualSiteBuildResult second = service.build(root, out, "jsonschema-docs");
    assertEquals(1, second.pageCount());
    Path secondHtmlPath = out.resolve("8.2").resolve("secondOp-1.html");
    assertTrue(Files.isRegularFile(secondHtmlPath), "missing " + secondHtmlPath);
    String secondHtml = Files.readString(secondHtmlPath, StandardCharsets.UTF_8);
    assertTrue(secondHtml.contains("Second Site Title"), secondHtml);
    assertFalse(
        Files.exists(firstHtmlPath), "stale " + firstHtmlPath + " should be cleared on full rebuild");
    assertFalse(secondHtml.contains("First Site Title"), secondHtml);
    assertNotEquals(firstHtml, secondHtml);
  }

  @Test
  void yamlLoaderParsesJsonSchemaSpec() throws Exception {
    Path root = writeSite(tempDir.resolve("yaml-js"), "custom-schema.json");
    Files.writeString(
        root.resolve("custom-schema.json"),
        sampleSchema("guide", "Guide", ""),
        StandardCharsets.UTF_8);
    VirtualSiteConfig loaded =
        VirtualSiteConfigLoader.load(
            root, VirtualSiteConfigLoader.DEFAULT_CONFIG_FILE, "jsonschema-docs");
    assertEquals("custom-schema.json", loaded.jsonschema().file());
  }

  @Test
  void omittedPathDefaultsToIdHtmlUnderVersion() throws Exception {
    Path relative = PSJsonSchemaVirtualSiteSource.resolvePagePath("", "plain-id", "8.2", "test");
    assertEquals(Path.of("8.2", "plain-id.html"), relative);
  }

  @Test
  void slugForPathStripsUnsafeChars() {
    assertEquals("sku", PSJsonSchemaVirtualSiteSource.slugForPath("sku"));
    assertEquals("Product.sku", PSJsonSchemaVirtualSiteSource.slugForPath("Product.sku"));
    assertEquals("jsonschema", PSJsonSchemaVirtualSiteSource.slugForPath(":::"));
  }

  private static Path writeSite(Path root, String file) throws Exception {
    Files.createDirectories(root.resolve("8.2"));
    Files.createDirectories(root.resolve("_theme"));
    String jsonschemaBlock;
    if (file != null && !file.isBlank()) {
      jsonschemaBlock =
          """
          jsonschema:
            file: %s
          """
              .formatted(file);
    } else {
      jsonschemaBlock = "";
    }
    Files.writeString(
        root.resolve("_config.yaml"),
        """
        site:
          title: JSON Schema Docs
        versions:
          - id: "8.2"
            label: "8.2"
            path: "8.2"
            default: true
        theme:
          layout: page.html
        %s
        """
            .formatted(jsonschemaBlock),
        StandardCharsets.UTF_8);
    return root;
  }

  private static void writeJsonSchemaYaml(Path root, String siteTitle, String file)
      throws Exception {
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
        jsonschema:
          file: %s
        """
            .formatted(siteTitle, file),
        StandardCharsets.UTF_8);
  }

  private static VirtualSiteConfig config(Path root, String file) {
    JsonSchemaSpec spec = file != null ? new JsonSchemaSpec(null, file) : null;
    return new VirtualSiteConfig(
        root,
        "JSON Schema Docs",
        "",
        "page.html",
        List.of(new VirtualSiteConfig.VersionSpec("8.2", "8.2", "8.2", true)),
        List.of(),
        "jsonschema-docs",
        null,
        null,
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

  private static String sampleSchema(String propertyName, String title, String description) {
    String descJson =
        description == null || description.isBlank()
            ? ""
            : ", \"description\": " + jsonString(description);
    return """
        {
          "title": "Catalog",
          "type": "object",
          "properties": {
            %s: { "type": "string", "title": %s%s }
          }
        }
        """
        .formatted(jsonString(propertyName), jsonString(title), descJson);
  }

  private static String jsonString(String value) {
    return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
  }
}
