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

package com.percussion.packages.manifest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Parse / validate / round-trip coverage for the Component Package Manifest ship format (issue
 * #2750 / ADR-004 Phase 3 slice 1).
 */
class PSComponentPackageManifestTest {

  private static final String MINIMAL_FIXTURE = "/manifests/minimal-component-package.json";

  @TempDir Path tempDir;

  @Test
  void parseMinimalFixture_populatesIdentityCatalogAndArtifacts() throws Exception {
    PSComponentPackageManifest manifest = parseFixture();

    assertEquals("1.0", manifest.getSchemaVersion());
    assertEquals("perc.widget.title", manifest.getId());
    assertEquals("Title", manifest.getName());
    assertEquals("1.1.4", manifest.getVersion());
    assertNotNull(manifest.getPublisher());
    assertEquals("Percussion Software Inc", manifest.getPublisher().getName());
    assertEquals("https://www.percussion.com", manifest.getPublisher().getUrl());
    assertNotNull(manifest.getCmsVersion());
    assertEquals("1.9.0", manifest.getCmsVersion().getMin());
    assertEquals("9.0.0", manifest.getCmsVersion().getMax());

    assertEquals(1, manifest.getDependencies().size());
    assertEquals("perc.Baseline", manifest.getDependencies().get(0).getName());
    assertFalse(manifest.getDependencies().get(0).isImplied());

    assertNotNull(manifest.getCatalog());
    assertEquals("component", manifest.getCatalog().getKind());
    assertEquals("content", manifest.getCatalog().getCategory());
    assertEquals(Integer.valueOf(780), manifest.getCatalog().getPreferredEditorWidth());
    assertEquals(Boolean.TRUE, manifest.getCatalog().getResponsive());

    assertEquals(1, manifest.getContentTypes().size());
    assertEquals("percTitleAsset", manifest.getContentTypes().get(0).getName());
    assertEquals("contentTypes/percTitleAsset", manifest.getContentTypes().get(0).getRef());

    assertEquals(1, manifest.getTemplates().size());
    PSComponentPackageManifest.TemplateRef tpl = manifest.getTemplates().get(0);
    assertEquals("percTitleSnippet", tpl.getName());
    assertEquals("snippet", tpl.getType());
    assertEquals("velocityAssembler", tpl.getAssembler());
    assertEquals(1, tpl.getBindings().size());
    assertEquals("wrapper", tpl.getBindings().get(0).getVariable());

    assertEquals(1, manifest.getSlots().size());
    assertEquals("titleContent", manifest.getSlots().get(0).getName());
    assertTrue(manifest.getSlots().get(0).getAllowedContentTypes().contains("percTitleAsset"));
    assertEquals("vertical", manifest.getSlots().get(0).getLayout().get("orientation"));

    assertEquals(1, manifest.getResources().size());
    assertEquals("image", manifest.getResources().get(0).getType());

    assertEquals(1, manifest.getUserPreferences().size());
    assertEquals("wrapper", manifest.getUserPreferences().get(0).getName());
    assertEquals(2, manifest.getUserPreferences().get(0).getEnumValues().size());

    assertEquals(1, manifest.getCssPreferences().size());
    assertEquals("rootclass", manifest.getCssPreferences().get(0).getName());
  }

  @Test
  void validateMinimalFixture_passes() throws Exception {
    PSComponentPackageManifest manifest = parseFixture();
    List<String> errors = PSComponentPackageManifestValidator.validateCollecting(manifest);
    assertTrue(errors.isEmpty(), () -> "unexpected errors: " + errors);
    PSComponentPackageManifestValidator.validate(manifest);
  }

  @Test
  void roundTrip_jsonString_preservesModelEquality() throws Exception {
    PSComponentPackageManifest original = parseFixture();
    String json = PSComponentPackageManifestIo.toJson(original);
    PSComponentPackageManifest roundTripped = PSComponentPackageManifestIo.parse(json);
    assertEquals(original, roundTripped);
  }

  @Test
  void roundTrip_pathWriteRead_preservesModelEquality() throws Exception {
    PSComponentPackageManifest original = parseFixture();
    Path out = tempDir.resolve("component-package.json");
    PSComponentPackageManifestIo.write(original, out);
    assertTrue(Files.isRegularFile(out));
    PSComponentPackageManifest roundTripped = PSComponentPackageManifestIo.read(out);
    assertEquals(original, roundTripped);
  }

  @Test
  void validate_rejectsMissingIdentityAndEmptyArtifacts() {
    PSComponentPackageManifest empty = new PSComponentPackageManifest();
    List<String> errors = PSComponentPackageManifestValidator.validateCollecting(empty);
    assertTrue(errors.stream().anyMatch(e -> e.contains("id is required")));
    assertTrue(errors.stream().anyMatch(e -> e.contains("name is required")));
    assertTrue(errors.stream().anyMatch(e -> e.contains("version is required")));
    assertTrue(
        errors.stream()
            .anyMatch(e -> e.contains("at least one contentTypes") || e.contains("templates")));
    assertThrows(
        PSComponentPackageManifestException.class,
        () -> PSComponentPackageManifestValidator.validate(empty));
  }

  @Test
  void validate_rejectsUnsupportedSchemaVersion() throws Exception {
    PSComponentPackageManifest manifest = parseFixture();
    manifest.setSchemaVersion("99.0");
    List<String> errors = PSComponentPackageManifestValidator.validateCollecting(manifest);
    assertTrue(errors.stream().anyMatch(e -> e.contains("unsupported schemaVersion")));
  }

  @Test
  void validate_rejectsAbsoluteOsPathsInResourceRefs() throws Exception {
    PSComponentPackageManifest manifest = parseFixture();
    manifest.getResources().get(0).setPath("C:/Windows/system32/evil.png");
    List<String> driveLetterErrors =
        PSComponentPackageManifestValidator.validateCollecting(manifest);
    assertTrue(
        driveLetterErrors.stream().anyMatch(e -> e.contains("resources[0].path")),
        () -> "expected path error, got: " + driveLetterErrors);

    manifest.getResources().get(0).setPath("resources\\windows\\style.css");
    List<String> backslashErrors =
        PSComponentPackageManifestValidator.validateCollecting(manifest);
    assertTrue(
        backslashErrors.stream().anyMatch(e -> e.contains("resources[0].path")),
        () -> "expected backslash path error, got: " + backslashErrors);

    // Unix absolute path (cross-platform regression)
    manifest.getResources().get(0).setPath("/etc/passwd");
    List<String> unixAbsoluteErrors =
        PSComponentPackageManifestValidator.validateCollecting(manifest);
    assertTrue(
        unixAbsoluteErrors.stream().anyMatch(e -> e.contains("resources[0].path")),
        () -> "expected Unix absolute path error, got: " + unixAbsoluteErrors);

    // Windows UNC path (cross-platform regression)
    manifest.getResources().get(0).setPath("\\\\server\\share\\evil.png");
    List<String> uncErrors = PSComponentPackageManifestValidator.validateCollecting(manifest);
    assertTrue(
        uncErrors.stream().anyMatch(e -> e.contains("resources[0].path")),
        () -> "expected UNC path error, got: " + uncErrors);
  }

  @Test
  void validate_allowsDoubleDotFilenamesButRejectsDotDotSegments() throws Exception {
    PSComponentPackageManifest manifest = parseFixture();
    // Double-dot in a filename is legal (not a path segment)
    manifest.getResources().get(0).setPath("assets/logo..png");
    List<String> ok = PSComponentPackageManifestValidator.validateCollecting(manifest);
    assertTrue(
        ok.stream().noneMatch(e -> e.contains("resources[0].path")),
        () -> "expected logo..png allowed, got: " + ok);

    // Segment-shaped .. must still be rejected
    manifest.getResources().get(0).setPath("assets/../evil.png");
    List<String> segmentErrors =
        PSComponentPackageManifestValidator.validateCollecting(manifest);
    assertTrue(
        segmentErrors.stream().anyMatch(e -> e.contains("resources[0].path")),
        () -> "expected .. segment rejection, got: " + segmentErrors);
  }

  @Test
  void validate_rejectsBlankPublisherUrlWhenPresent() throws Exception {
    PSComponentPackageManifest manifest = parseFixture();
    manifest.getPublisher().setUrl("");
    List<String> errors = PSComponentPackageManifestValidator.validateCollecting(manifest);
    assertTrue(
        errors.stream().anyMatch(e -> e.contains("publisher.url") && e.contains("blank")),
        () -> "expected blank publisher.url error, got: " + errors);
  }

  @Test
  void validate_relativePathErrorUsesTrimmedValue() throws Exception {
    PSComponentPackageManifest manifest = parseFixture();
    // Leading/trailing whitespace: validation trims; error message must quote the trimmed form
    manifest.getResources().get(0).setPath("  /etc/passwd  ");
    List<String> errors = PSComponentPackageManifestValidator.validateCollecting(manifest);
    assertTrue(
        errors.stream().anyMatch(e -> e.contains("'/etc/passwd'")),
        () -> "expected trimmed path in error message, got: " + errors);
    assertTrue(
        errors.stream().noneMatch(e -> e.contains("'  /etc/passwd  '")),
        () -> "untrimmed path should not appear in error, got: " + errors);
  }

  @Test
  void parse_rejectsEmptyAndMalformedJson() {
    assertThrows(
        PSComponentPackageManifestException.class, () -> PSComponentPackageManifestIo.parse(""));
    assertThrows(
        PSComponentPackageManifestException.class,
        () -> PSComponentPackageManifestIo.parse("{not-json"));
  }

  @Test
  void defaultManifestFileName_isComponentPackageJson() {
    assertEquals("component-package.json", PSComponentPackageManifest.DEFAULT_MANIFEST_FILE_NAME);
    assertEquals("1.0", PSComponentPackageManifest.SUPPORTED_SCHEMA_VERSION);
  }

  private static PSComponentPackageManifest parseFixture()
      throws PSComponentPackageManifestException, IOException {
    try (InputStream in = PSComponentPackageManifestTest.class.getResourceAsStream(MINIMAL_FIXTURE)) {
      assertNotNull(in, "classpath fixture missing: " + MINIMAL_FIXTURE);
      String json = new String(in.readAllBytes(), StandardCharsets.UTF_8);
      // Normalize any accidental Windows line endings from the fixture checkout.
      json = json.replace("\r\n", "\n");
      return PSComponentPackageManifestIo.parse(json);
    }
  }
}
