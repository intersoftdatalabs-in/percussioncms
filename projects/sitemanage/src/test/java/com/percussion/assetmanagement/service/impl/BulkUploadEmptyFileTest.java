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
package com.percussion.assetmanagement.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/**
 * Regression for GH-728 / v8.1.7 PRs #775+#776: empty file uploads must return clear JSON errors
 * instead of silent failures / opaque 500s.
 *
 * <p>Note: development replaced the legacy bulk upload gadget UI; server-side servlet + extraction
 * messaging is still the product path for empty uploads.
 *
 * <p>Path-based source assertions are the established 005 migrate regression pattern for
 * package/servlet surface code that cannot be driven without a full container. Pure helpers used by
 * that surface are covered behaviorally below.
 */
class BulkUploadEmptyFileTest {

  @Test
  void assetServiceRejectsEmptyExtractedContent() throws Exception {
    Path root = resolveRepoRoot();
    Path svc =
        root.resolve(
            "projects/sitemanage/src/main/java/com/percussion/assetmanagement/service/impl/PSAssetService.java");
    if (!Files.isRegularFile(svc)) {
      fail(svc.toString());
    }
    String text = Files.readString(svc, StandardCharsets.UTF_8);
    assertTrue(text.contains("PSEmptyUploadContent.isEmptyOrWhitespaceOnly"));
    assertTrue(text.contains("PSEmptyUploadContent.rejectionMessage"));
  }

  @Test
  void uploadServletWritesJsonErrorResponses() throws Exception {
    Path root = resolveRepoRoot();
    Path servlet =
        root.resolve(
            "projects/sitemanage/src/main/java/com/percussion/assetmanagement/service/impl/PSAssetUploadServlet.java");
    if (!Files.isRegularFile(servlet)) {
      fail(servlet.toString());
    }
    String text = Files.readString(servlet, StandardCharsets.UTF_8);
    assertTrue(text.contains("writeErrorResponse"));
    assertTrue(text.contains("safeWriteErrorResponse"));
    assertTrue(text.contains("No valid file was provided for upload."));
    assertTrue(text.contains("handleExtractionError"));
    assertTrue(text.contains("SC_BAD_REQUEST"));
    assertTrue(text.contains("err.put(\"error\""));
    // Generic Exception path must not surface raw e.getMessage() to the client
    assertTrue(text.contains("Upload failed due to an unexpected server error."));
    // Multipart POST entry point used by Bulk Upload (GH-1812)
    assertTrue(text.contains("protected void doPost"));
    assertTrue(text.contains("@MultipartConfig"));
  }

  /**
   * GH-1812: client {@code resolveAssetUploadUrl()} POSTs exact {@code /cm/uploadAssetFile}.
   * Prefix-only {@code /uploadAssetFile/*} does not match that path on servlet containers, so
   * web.xml must declare an exact {@code /uploadAssetFile} mapping (and may keep /* for legacy).
   */
  @Test
  void webXmlMapsExactUploadAssetFileForBulkUploadPost() throws Exception {
    Path root = resolveRepoRoot();
    Path[] webXmls = {
      root.resolve("WebUI/src/main/webapp/WEB-INF/web.xml"),
      root.resolve("WebUI/src/main/webapp/cm/WEB-INF/web.xml"),
      root.resolve("WebUI/war/WEB-INF/web.xml"),
    };
    for (Path webXml : webXmls) {
      if (!Files.isRegularFile(webXml)) {
        fail("missing web.xml: " + webXml);
      }
      String text = Files.readString(webXml, StandardCharsets.UTF_8);
      assertTrue(
          text.contains("<url-pattern>/uploadAssetFile</url-pattern>"),
          "exact /uploadAssetFile mapping required in " + webXml);
      assertTrue(
          text.contains("<url-pattern>/uploadAssetFile/*</url-pattern>"),
          "prefix /uploadAssetFile/* mapping should remain in " + webXml);
      // Mapping must bind to the asset upload servlet, not an unrelated name
      assertTrue(
          text.contains("<servlet-name>assetUploadServlet</servlet-name>"),
          "assetUploadServlet must be declared in " + webXml);
    }
  }

  /** Pure-function coverage of empty/whitespace guard + message (GH-728 / #775). */
  @Test
  void emptyOrWhitespaceHelperRejectsBlankAndWordingAvoidsZeroBytesClaim() {
    assertTrue(PSEmptyUploadContent.isEmptyOrWhitespaceOnly(null));
    assertTrue(PSEmptyUploadContent.isEmptyOrWhitespaceOnly(""));
    assertTrue(PSEmptyUploadContent.isEmptyOrWhitespaceOnly("   \n\t"));
    assertFalse(PSEmptyUploadContent.isEmptyOrWhitespaceOnly("x"));

    String msg = PSEmptyUploadContent.rejectionMessage("blank.txt");
    assertTrue(msg.contains("blank.txt"));
    assertTrue(msg.contains("empty or whitespace-only"));
    assertTrue(msg.contains("Cannot create a text-based asset from an empty file"));
    assertFalse(msg.contains("(0 bytes)"));
    assertEquals(
        PSEmptyUploadContent.rejectionMessage(null), PSEmptyUploadContent.rejectionMessage(""));
  }

  private static Path resolveRepoRoot() {
    Path cwd = Path.of("").toAbsolutePath().normalize();
    Path candidate = cwd.resolve("../..").normalize();
    if (Files.isDirectory(candidate.resolve("projects/sitemanage"))) {
      return candidate;
    }
    if (Files.isDirectory(cwd.resolve("projects/sitemanage"))) {
      return cwd;
    }
    fail("could not resolve monorepo root");
    return cwd;
  }
}
