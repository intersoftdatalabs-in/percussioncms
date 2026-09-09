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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.services.pipeline.model.PipelineIrDocument;
import com.percussion.services.pipeline.model.PipelineResourceIr;
import com.percussion.services.pipeline.model.PipelineResultPageIr;
import com.percussion.services.pipeline.xsl.PSPipelineResultPagePath;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Slice D PRE-02: classic PSResultPage / PSResultPageSet → IR inspect pages. */
@DisplayName("Classic pipeline result-page import (Slice D)")
class PSClassicPipelineImporterResultPageTest {

  private static final String FIXTURE =
      "/com/percussion/services/pipeline/fixtures/resultPageImport.xml";

  @Test
  @DisplayName("import maps path-safe result pages and skips traversal/absolute/cloud")
  void importClassic_mapsSafeResultPagesAndSkipsUnsafe() throws Exception {
    PipelineIrDocument ir;
    try (InputStream in = openFixture()) {
      ir = PSClassicPipelineImporter.importFromXml(in);
    }

    assertEquals("pipe_resultPageImport", ir.getApp().getName());
    PipelineResourceIr res = ir.findResource("Dataset34");
    assertNotNull(res);
    List<PipelineResultPageIr> pages = res.getResultPages();
    assertEquals(2, pages.size(), "unsafe pages must be skipped: " + pages);

    PipelineResultPageIr html = pages.get(0);
    assertEquals("pages/result.xsl", html.getStylesheetUri());
    assertEquals(".html", html.getRequestExtension());
    assertEquals("text/html", html.getMimeType());

    PipelineResultPageIr xml = pages.get(1);
    assertEquals("pages/result.xml.xsl", xml.getStylesheetUri());
    assertEquals(".xml", xml.getRequestExtension());
    assertEquals("text/xml", xml.getMimeType());

    assertTrue(res.presentStageInventory().contains("resultPages"));
    assertTrue(res.presentStageInventory().contains("resultPage"));
    assertNotNull(res.getResultPage());
    assertEquals("pages/result.xsl", res.getResultPage().getStylesheetUri());
    assertEquals(".html", res.getResultPage().getRequestExtension());
  }

  @Test
  @DisplayName("classic file: relative stylesheet maps; absolute, traversal, http skipped")
  void tryImportedStylesheetUri_pathSafe() throws Exception {
    assertEquals(
        "pages/result.xsl",
        PSPipelineResultPagePath.tryImportedStylesheetUri(new URL("file:pages/result.xsl")));
    assertEquals(
        "login.xsl", PSPipelineResultPagePath.tryImportedStylesheetUri(new URL("file:login.xsl")));
    assertNull(PSPipelineResultPagePath.tryImportedStylesheetUri(new URL("file:../etc/passwd")));
    assertNull(
        PSPipelineResultPagePath.tryImportedStylesheetUri(new URL("file:///tmp/secret.xsl")));
    assertNull(
        PSPipelineResultPagePath.tryImportedStylesheetUri(
            new URL("https://cdn.example/result.xsl")));
    assertNull(PSPipelineResultPagePath.tryImportedStylesheetUri(null));
    assertEquals(".xml", PSPipelineResultPagePath.tryImportedRequestExtension("xml"));
    assertEquals(".html", PSPipelineResultPagePath.tryImportedRequestExtension(".HTML"));
    assertNull(PSPipelineResultPagePath.tryImportedRequestExtension("../x"));
    assertEquals("application/json", PSPipelineResultPagePath.tryImportedMimeType("application/json"));
    assertNull(PSPipelineResultPagePath.tryImportedMimeType("not-a-mime"));
    assertFalse(PSPipelineResultPagePath.tryImportedRequestExtension("html/../x") != null);
  }

  private static InputStream openFixture() {
    InputStream in = PSClassicPipelineImporterResultPageTest.class.getResourceAsStream(FIXTURE);
    assertNotNull(in, "missing classpath fixture " + FIXTURE);
    return in;
  }
}
