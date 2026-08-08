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

import static org.junit.jupiter.api.Assertions.*;

import com.percussion.services.pipeline.model.BackendTableRefIr;
import com.percussion.services.pipeline.model.MapperStageIr;
import com.percussion.services.pipeline.model.PipelineIrDocument;
import com.percussion.services.pipeline.model.PipelineResourceIr;
import com.percussion.services.pipeline.model.SelectorStageIr;
import com.percussion.services.pipeline.model.WhereClauseIr;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Unit tests for pipeline IR: classic import stage inventory, JSON round-trip, and file load/save.
 */
@DisplayName("Pipeline IR service (Slice A foundation)")
class PSPipelineIrServiceTest {

  private static final String FIXTURE =
      "/com/percussion/services/pipeline/fixtures/sys_adminCataloger.xml";

  @TempDir Path tempDir;

  private IPSPipelineIrService service;

  @BeforeEach
  void setUp() {
    service = new PSPipelineIrService(tempDir);
  }

  @Test
  @DisplayName("classic import: sys_adminCataloger yields query resource with tanks/mapper/selector")
  void importClassicAdminCataloger_stageInventory() throws Exception {
    PipelineIrDocument ir;
    try (InputStream in = openFixture()) {
      ir = service.importClassicXml(in);
    }

    assertEquals(PipelineIrDocument.CURRENT_IR_VERSION, ir.getIrVersion());
    assertEquals(PipelineIrDocument.SOURCE_CLASSIC_IMPORT, ir.getSource());
    assertEquals("sys_adminCataloger", ir.getApp().getName());
    assertEquals("sys_adminCataloger", ir.getApp().getRequestRoot());
    assertTrue(ir.getApp().isEnabled());
    assertFalse(ir.getApp().isHidden());
    assertEquals(1, ir.getResources().size());

    PipelineResourceIr res = ir.findResource("Dataset34");
    assertNotNull(res, "expected Dataset34 resource");
    assertEquals(PipelineResourceIr.KIND_QUERY, res.getKind());
    assertEquals("sys_rxlookup", res.getRequestPage());
    assertEquals("QueryPipe", res.getPipeName());
    assertEquals("none", res.getTransactionMode());

    List<String> inventory = res.presentStageInventory();
    assertTrue(inventory.contains("pageTank"), "page tank present: " + inventory);
    assertTrue(inventory.contains("backendTank"), "backend tank present: " + inventory);
    assertTrue(inventory.contains("mapper"), "mapper present: " + inventory);
    assertTrue(inventory.contains("selector"), "selector present: " + inventory);
    assertFalse(inventory.contains("updater"), "query pipe must not expose updater: " + inventory);

    assertTrue(res.getStages().getPageTank().getSchemaSource().contains("Properties.dtd"));

    List<BackendTableRefIr> tables = res.getStages().getBackendTank().getTables();
    assertEquals(1, tables.size());
    assertEquals("PSX_ADMINLOOKUP", tables.get(0).getAlias());
    assertEquals("PSX_ADMINLOOKUP", tables.get(0).getTable());
    assertEquals(0, res.getStages().getBackendTank().getJoinCount());

    MapperStageIr mapper = res.getStages().getMapper();
    assertEquals(5, mapper.getMappings().size(), "five column mappings in golden fixture");
    assertEquals("Properties/@Type", mapper.getMappings().get(0).getDocumentField());
    assertEquals("PSX_ADMINLOOKUP.TYPE", mapper.getMappings().get(0).getBackend());

    SelectorStageIr selector = res.getStages().getSelector();
    assertEquals(SelectorStageIr.METHOD_WHERE, selector.getMethod());
    assertEquals(1, selector.getWhereClauseCount());
    assertEquals(1, selector.getWhereClauses().size());
    WhereClauseIr where = selector.getWhereClauses().get(0);
    assertEquals(WhereClauseIr.KIND_COLUMN, where.getLeftKind());
    assertEquals("PSX_ADMINLOOKUP.TYPE", where.getLeft());
    assertEquals("=", where.getOperator());
    assertEquals(WhereClauseIr.KIND_PARAM, where.getRightKind());
    assertEquals("sys_key", where.getRight());
    assertFalse(where.isOmitWhenNull());
    assertFalse(selector.isUnique());
  }

  @Test
  @DisplayName("IR JSON encode/decode round-trips golden import")
  void jsonRoundTrip_preservesDocument() throws Exception {
    PipelineIrDocument original;
    try (InputStream in = openFixture()) {
      original = service.importClassicXml(in);
    }

    String json = service.toJson(original);
    assertTrue(json.contains("\"irVersion\""));
    assertTrue(json.contains("sys_adminCataloger"));
    assertTrue(json.contains("Dataset34"));

    PipelineIrDocument decoded = service.fromJson(json);
    assertEquals(original, decoded);
    assertEquals(
        original.findResource("Dataset34").presentStageInventory(),
        decoded.findResource("Dataset34").presentStageInventory());
  }

  @Test
  @DisplayName("native IR save then load round-trips under portable Path store")
  void saveLoad_roundTrip() throws Exception {
    PipelineIrDocument original;
    try (InputStream in = openFixture()) {
      original = service.importClassicXml(in);
    }
    // Mark as native after import for storage
    original.setSource(PipelineIrDocument.SOURCE_NATIVE);

    assertFalse(service.exists("sys_adminCataloger"));
    service.save(original);
    assertTrue(service.exists("sys_adminCataloger"));

    Path stored =
        tempDir.resolve("sys_adminCataloger" + PSPipelineIrFileStore.FILE_SUFFIX);
    assertTrue(Files.isRegularFile(stored), "expected file at " + stored);

    Optional<PipelineIrDocument> loaded = service.load("sys_adminCataloger");
    assertTrue(loaded.isPresent());
    assertEquals(original, loaded.get());
    assertEquals(PipelineIrDocument.SOURCE_NATIVE, loaded.get().getSource());
  }

  @Test
  @DisplayName("load missing name returns empty; unsafe names rejected")
  void load_missingAndUnsafe() throws Exception {
    assertTrue(service.load("no-such-app").isEmpty());
    assertThrows(PSPipelineIrException.class, () -> service.load("../escape"));
    assertThrows(PSPipelineIrException.class, () -> service.load("a/b"));
    assertThrows(PSPipelineIrException.class, () -> service.exists("x\\y"));
  }

  @Test
  @DisplayName("safe application name helper rejects traversal")
  void safeNameHelper() {
    assertTrue(PSPipelineIrFileStore.isSafeApplicationName("sys_adminCataloger"));
    assertFalse(PSPipelineIrFileStore.isSafeApplicationName(""));
    assertFalse(PSPipelineIrFileStore.isSafeApplicationName(null));
    assertFalse(PSPipelineIrFileStore.isSafeApplicationName(".."));
    assertFalse(PSPipelineIrFileStore.isSafeApplicationName("foo/bar"));
    assertFalse(PSPipelineIrFileStore.isSafeApplicationName("foo\\bar"));
  }

  private static InputStream openFixture() {
    InputStream in = PSPipelineIrServiceTest.class.getResourceAsStream(FIXTURE);
    assertNotNull(in, "missing classpath fixture " + FIXTURE);
    return in;
  }
}
