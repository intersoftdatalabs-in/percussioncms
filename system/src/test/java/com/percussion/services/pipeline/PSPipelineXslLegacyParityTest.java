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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.security.xml.PSSecureXMLUtils;
import com.percussion.services.pipeline.model.BackendTableRefIr;
import com.percussion.services.pipeline.model.BackendTankStageIr;
import com.percussion.services.pipeline.model.MappingEntryIr;
import com.percussion.services.pipeline.model.MapperStageIr;
import com.percussion.services.pipeline.model.PipelineExecuteRequest;
import com.percussion.services.pipeline.model.PipelineExecuteResult;
import com.percussion.services.pipeline.model.PipelineIrDocument;
import com.percussion.services.pipeline.model.PipelineResourceIr;
import com.percussion.services.pipeline.model.PipelineResultPageIr;
import com.percussion.services.pipeline.model.PipelineStagesIr;
import com.percussion.services.pipeline.model.SelectorStageIr;
import com.percussion.services.pipeline.sql.PSJdbcPipelineSqlAdapter;
import com.percussion.services.pipeline.xsl.PSPipelineResultPagePath;
import com.percussion.services.pipeline.xsl.PSPipelineXslMerge;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Slice D residual (#6070): a legacy application-owned stylesheet (classic {@code
 * file:pages/legacy.xsl} form, staged under {@code <irStore>/<app>/}) renders through the
 * runtime without data loss, byte-identical to a reference classic-style transform of the
 * same row document.
 */
@DisplayName("Pipeline legacy XSL result-page parity (Slice D residual)")
class PSPipelineXslLegacyParityTest {

  private static final String APP = "legacyXslApp";

  private static final String RESOURCE = "LegacyQ";

  private static final String LEGACY_URI = "pages/legacy.xsl";

  private static final String LEGACY_MARKER = "LEGACY-RESULT-PAGE";

  private static final String LEGACY_XSL =
      """
      <?xml version="1.0" encoding="UTF-8"?>
      <xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
        <xsl:output method="html" encoding="UTF-8" indent="yes"/>
        <xsl:template match="/">
          <html><head><title>Legacy Result</title></head><body>
            <h1>LEGACY-RESULT-PAGE</h1>
            <table>
              <xsl:for-each select="rows/row">
                <tr>
                  <td class="sku"><xsl:value-of select="sku"/></td>
                  <td class="name"><xsl:value-of select="name"/></td>
                  <td class="note"><xsl:value-of select="note"/></td>
                </tr>
              </xsl:for-each>
            </table>
          </body></html>
        </xsl:template>
      </xsl:stylesheet>
      """;

  @TempDir Path tempDir;

  private String jdbcUrl;
  private IPSPipelineIrService irService;
  private PSJdbcPipelineSqlAdapter sqlAdapter;

  @BeforeEach
  void setUp() throws Exception {
    jdbcUrl = "jdbc:h2:mem:pipeline_legacy_" + System.nanoTime() + ";DB_CLOSE_DELAY=-1";
    try (Connection c = DriverManager.getConnection(jdbcUrl);
        Statement st = c.createStatement()) {
      st.execute(
          "CREATE TABLE \"PSX_LEGACY\" ("
              + "\"SKU\" VARCHAR(64), \"NAME\" VARCHAR(128), \"NOTE\" VARCHAR(256))");
      st.execute(
          "INSERT INTO \"PSX_LEGACY\" (\"SKU\", \"NAME\", \"NOTE\") VALUES "
              + "('SKU-1', 'Loopback Widget', 'plain note'),"
              + "('SKU-2', 'Amp & Quote \"Test\"', 'lt<gt> & caf\u00e9 \u65e5\u672c\u8a9e'),"
              + "('SKU-3', 'O''Brien', NULL)");
    }
    irService = new PSPipelineIrService(tempDir);
    sqlAdapter =
        new PSJdbcPipelineSqlAdapter(
            () -> {
              try {
                return DriverManager.getConnection(jdbcUrl);
              } catch (java.sql.SQLException e) {
                throw new IllegalStateException("H2 connection failed", e);
              }
            });
  }

  @AfterEach
  void tearDown() throws Exception {
    try (Connection c = DriverManager.getConnection(jdbcUrl);
        Statement st = c.createStatement()) {
      st.execute("SHUTDOWN");
    } catch (Exception ignored) {
      // mem DB may already be gone
    }
  }

  @Test
  @DisplayName("legacy app-owned stylesheet renders every row field without data loss")
  void legacyStylesheetRendersWithoutDataLoss() throws Exception {
    stageLegacyStylesheet();
    irService.save(legacyDoc());

    IPSPipelineRuntimeService runtime = new PSPipelineRuntimeService(irService, sqlAdapter);
    PipelineExecuteRequest req = PipelineExecuteRequest.empty();
    req.setRequestExtension(".html");
    PipelineExecuteResult result = runtime.execute(APP, RESOURCE, req);

    String html = result.getHtml();
    assertTrue(html != null && html.contains(LEGACY_MARKER), html);
    assertEquals(Boolean.TRUE, result.getMeta().get("resultPageApplied"));
    assertEquals("text/html", result.getMeta().get("contentType"));
    assertEquals(3, result.getRowCount());
    // No data loss: every field value from every row survives the merge. The HTML output
    // method entity-escapes specials (&eacute; renders as café in the browser), so assert
    // the escaped forms plus the values the serializer leaves literal.
    assertTrue(html.contains("SKU-1"), html);
    assertTrue(html.contains("SKU-2"), html);
    assertTrue(html.contains("SKU-3"), html);
    assertTrue(html.contains("Loopback Widget"), html);
    assertTrue(html.contains("Amp &amp; Quote \"Test\""), html);
    assertTrue(html.contains("lt&lt;gt&gt; &amp; caf&eacute;"), html);
    assertTrue(html.contains("\u65e5\u672c\u8a9e"), html);
    assertTrue(html.contains("O'Brien"), html);
  }

  @Test
  @DisplayName("runtime legacy merge is byte-identical to the reference classic-style transform")
  void legacyMergeMatchesReferenceTransform() throws Exception {
    stageLegacyStylesheet();
    irService.save(legacyDoc());

    IPSPipelineRuntimeService runtime = new PSPipelineRuntimeService(irService, sqlAdapter);
    PipelineExecuteRequest req = PipelineExecuteRequest.empty();
    req.setRequestExtension(".html");
    PipelineExecuteResult result = runtime.execute(APP, RESOURCE, req);

    String reference = referenceTransform(PSPipelineXslMerge.rowsToXml(result.getRows()));
    assertEquals(reference, result.getHtml());
  }

  @Test
  @DisplayName("missing legacy stylesheet fails closed (no invented HTML)")
  void missingLegacyStylesheetFailsClosed() throws Exception {
    irService.save(legacyDoc());

    IPSPipelineRuntimeService runtime = new PSPipelineRuntimeService(irService, sqlAdapter);
    PipelineExecuteRequest req = PipelineExecuteRequest.empty();
    req.setRequestExtension(".html");
    PSPipelineIrException ex =
        assertThrows(
            PSPipelineIrException.class, () -> runtime.execute(APP, RESOURCE, req));
    assertTrue(ex.getMessage().toLowerCase().contains("not found"), ex.getMessage());
  }

  @Test
  @DisplayName("classic file: values normalize; absolute/traversal/cloud still fail closed")
  void normalizeLegacyStylesheetUri() throws Exception {
    assertEquals(
        "pages/result.xsl",
        PSPipelineResultPagePath.normalizeLegacyStylesheetUri("file:pages/result.xsl"));
    assertEquals(
        "login.xsl", PSPipelineResultPagePath.normalizeLegacyStylesheetUri("file:login.xsl"));
    assertEquals(
        "pages/result.xsl",
        PSPipelineResultPagePath.normalizeLegacyStylesheetUri("FILE:pages/result.xsl"));
    assertEquals(
        "pages/result.xsl",
        PSPipelineResultPagePath.normalizeLegacyStylesheetUri("  pages/result.xsl  "));
    assertThrows(
        PSPipelineIrException.class,
        () -> PSPipelineResultPagePath.normalizeLegacyStylesheetUri("file:/tmp/secret.xsl"));
    assertThrows(
        PSPipelineIrException.class,
        () -> PSPipelineResultPagePath.normalizeLegacyStylesheetUri("file:../etc/passwd"));
    assertThrows(
        PSPipelineIrException.class,
        () ->
            PSPipelineResultPagePath.normalizeLegacyStylesheetUri(
                "https://cdn.example/result.xsl"));
    assertThrows(
        PSPipelineIrException.class,
        () -> PSPipelineResultPagePath.normalizeLegacyStylesheetUri(""));
  }

  private void stageLegacyStylesheet() throws Exception {
    Path pagesDir = tempDir.resolve(APP).resolve("pages");
    Files.createDirectories(pagesDir);
    Files.writeString(pagesDir.resolve("legacy.xsl"), LEGACY_XSL, StandardCharsets.UTF_8);
  }

  private static PipelineIrDocument legacyDoc() {
    PipelineIrDocument doc = new PipelineIrDocument();
    doc.setSource(PipelineIrDocument.SOURCE_NATIVE);
    doc.getApp().setName(APP);

    PipelineResourceIr res = new PipelineResourceIr();
    res.setName(RESOURCE);
    res.setKind(PipelineResourceIr.KIND_QUERY);

    PipelineStagesIr stages = new PipelineStagesIr();
    BackendTankStageIr tank = new BackendTankStageIr();
    tank.setPresent(true);
    BackendTableRefIr table = new BackendTableRefIr();
    table.setTable("PSX_LEGACY");
    table.setAlias("PSX_LEGACY");
    tank.setTables(List.of(table));
    stages.setBackendTank(tank);

    MapperStageIr mapper = new MapperStageIr();
    mapper.setPresent(true);
    mapper.setMappings(
        List.of(
            mapping("Properties/@sku", "PSX_LEGACY.SKU"),
            mapping("Properties/@name", "PSX_LEGACY.NAME"),
            mapping("Properties/@note", "PSX_LEGACY.NOTE")));
    stages.setMapper(mapper);

    SelectorStageIr selector = new SelectorStageIr();
    selector.setPresent(true);
    selector.setMethod(SelectorStageIr.METHOD_WHERE);
    stages.setSelector(selector);

    res.setStages(stages);

    PipelineResultPageIr page = new PipelineResultPageIr();
    page.setStylesheetUri(LEGACY_URI);
    page.setRequestExtension(".html");
    page.setMimeType("text/html");
    res.setResultPage(page);

    doc.getResources().add(res);
    return doc;
  }

  private static MappingEntryIr mapping(String docField, String backend) {
    MappingEntryIr m = new MappingEntryIr();
    m.setDocumentField(docField);
    m.setBackend(backend);
    m.setBackendKind(MappingEntryIr.BACKEND_KIND_COLUMN);
    return m;
  }

  /** Reference classic-style transform: same stylesheet, same row document, no runtime path. */
  private static String referenceTransform(String rowXml) throws Exception {
    TransformerFactory tf = PSSecureXMLUtils.getSecuredTransformerFactory();
    Transformer transformer = tf.newTransformer(new StreamSource(new StringReader(LEGACY_XSL)));
    StringWriter out = new StringWriter();
    transformer.transform(new StreamSource(new StringReader(rowXml)), new StreamResult(out));
    return out.toString();
  }

  @Test
  @DisplayName("rowsToXml escapes specials so legacy sheets cannot lose field data")
  void rowsToXmlEscapesSpecials() {
    Map<String, Object> row = new LinkedHashMap<>();
    row.put("note", "lt<gt> & \"quoted\"");
    String xml = PSPipelineXslMerge.rowsToXml(List.of(row));
    assertTrue(xml.contains("lt&lt;gt&gt; &amp; &quot;quoted&quot;"), xml);
  }
}
