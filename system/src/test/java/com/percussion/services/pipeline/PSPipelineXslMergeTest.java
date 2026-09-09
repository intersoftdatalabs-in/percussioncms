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

package com.percussion.services.pipeline;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.services.pipeline.model.PipelineExecuteRequest;
import com.percussion.services.pipeline.model.PipelineResultPageIr;
import com.percussion.services.pipeline.xsl.PSPipelineResultPagePath;
import com.percussion.services.pipeline.xsl.PSPipelineXslMerge;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Slice D PRE-01: result-page path guard + bundled XSL HTML merge. */
@DisplayName("Pipeline XSL result-page merge (Slice D)")
class PSPipelineXslMergeTest {

  @TempDir Path tempDir;

  @Test
  @DisplayName("path guard: bundled ok; cloud, credentials, traversal rejected")
  void requireSafe_rejectsCloudCredentialsAndTraversal() throws Exception {
    assertEquals(
        PSPipelineResultPagePath.BUNDLED_TOKEN,
        PSPipelineResultPagePath.requireSafeStylesheetUri(PSPipelineResultPagePath.BUNDLED_TOKEN));
    assertEquals(
        PSPipelineResultPagePath.BUNDLED_FILENAME,
        PSPipelineResultPagePath.requireSafeStylesheetUri(
            PSPipelineResultPagePath.BUNDLED_FILENAME));
    assertEquals(
        "pages/result.xsl", PSPipelineResultPagePath.requireSafeStylesheetUri("pages/result.xsl"));

    PSPipelineIrException cloud =
        assertThrows(
            PSPipelineIrException.class,
            () ->
                PSPipelineResultPagePath.requireSafeStylesheetUri(
                    "https://cdn.example/result.xsl"));
    assertTrue(cloud.getMessage().toLowerCase().contains("cloud"), cloud.getMessage());

    PSPipelineIrException userinfo =
        assertThrows(
            PSPipelineIrException.class,
            () ->
                PSPipelineResultPagePath.requireSafeStylesheetUri(
                    "https://user:secret@cdn.example/result.xsl"));
    assertTrue(userinfo.getMessage().toLowerCase().contains("userinfo"), userinfo.getMessage());

    assertThrows(
        PSPipelineIrException.class,
        () -> PSPipelineResultPagePath.requireSafeStylesheetUri("../etc/passwd"));
    assertThrows(
        PSPipelineIrException.class,
        () -> PSPipelineResultPagePath.requireSafeStylesheetUri("/tmp/secret.xsl"));
    assertThrows(
        PSPipelineIrException.class,
        () -> PSPipelineResultPagePath.requireSafeStylesheetUri("C:\\temp\\x.xsl"));
    assertThrows(
        PSPipelineIrException.class, () -> PSPipelineResultPagePath.requireSafeStylesheetUri(""));
    assertThrows(
        PSPipelineIrException.class,
        () -> PSPipelineResultPagePath.requireSafeStylesheetUri("s3://bucket/key.xsl"));
  }

  @Test
  @DisplayName("bundled fixture merge produces HTML marker from row XML")
  void merge_bundledFixtureHtml() throws Exception {
    PipelineResultPageIr page = new PipelineResultPageIr();
    page.setStylesheetUri(PSPipelineResultPagePath.BUNDLED_TOKEN);
    page.setRequestExtension(".html");
    page.setMimeType("text/html");
    Map<String, Object> row = new LinkedHashMap<>();
    row.put("sku", "SKU-1");
    row.put("name", "Loopback Widget");
    String html = PSPipelineXslMerge.merge("sys_cmpDocuments", page, List.of(row), null);
    assertTrue(html.contains(PSPipelineXslMerge.BUNDLED_MARKER), html);
    assertTrue(html.contains("SKU-1"), html);
    assertTrue(html.contains("Loopback Widget"), html);
    assertFalse(html.toLowerCase().contains("invented"), html);
  }

  @Test
  @DisplayName("missing application-owned stylesheet is not invented HTML")
  void merge_missingSandboxFile() {
    PipelineResultPageIr page = new PipelineResultPageIr();
    page.setStylesheetUri("missing-result.xsl");
    PSPipelineIrException ex =
        assertThrows(
            PSPipelineIrException.class,
            () -> PSPipelineXslMerge.merge("sys_cmpDocuments", page, List.of(), tempDir));
    assertTrue(ex.getMessage().toLowerCase().contains("not found"), ex.getMessage());
  }

  @Test
  @DisplayName("wantsHtml honors requestExtension and accept")
  void wantsHtml_requestExtensionAndAccept() {
    PipelineExecuteRequest htmlExt = PipelineExecuteRequest.empty();
    htmlExt.setRequestExtension(".html");
    assertTrue(PSPipelineXslMerge.wantsHtml(htmlExt));

    PipelineExecuteRequest accept = PipelineExecuteRequest.empty();
    accept.setAccept("text/html");
    assertTrue(PSPipelineXslMerge.wantsHtml(accept));

    assertFalse(PSPipelineXslMerge.wantsHtml(PipelineExecuteRequest.empty()));
    assertFalse(PSPipelineXslMerge.wantsHtml(null));
  }
}
