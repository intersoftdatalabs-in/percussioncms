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

package com.percussion.apibridge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.percussion.design.objectstore.PSApplication;
import com.percussion.design.objectstore.PSApplicationType;
import com.percussion.design.objectstore.PSDataSet;
import com.percussion.design.objectstore.PSRequestor;
import com.percussion.design.objectstore.server.PSApplicationSummary;
import com.percussion.rest.pipelines.ApplicationDetail;
import com.percussion.rest.pipelines.ApplicationSummary;
import com.percussion.services.pipeline.IPSPipelineRuntimeService;
import com.percussion.services.pipeline.PSPipelineIrException;
import com.percussion.services.pipeline.model.PipelineExecuteRequest;
import com.percussion.services.pipeline.model.PipelineExecuteResult;
import com.percussion.util.PSCollection;
import jakarta.ws.rs.WebApplicationException;
import java.net.URI;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Pure unit tests for {@link PipelinesAdaptor} mapping / filter / page helpers (no object-store
 * singleton).
 */
@Tag("UnitTest")
class PipelinesAdaptorTest {

  @Test
  void mapFilterSortLimit_mapsFieldsIncludingHiddenAndAppType() {
    PSApplicationSummary src =
        summary(7, "sys_cmpDocuments", "System docs", true, "sys_cmpDocuments", false, false);
    when(src.getAppType()).thenReturn(PSApplicationType.CONTENT_EDITOR);
    when(src.getVersion()).thenReturn("8.2");

    List<ApplicationSummary> out =
        PipelinesAdaptor.mapFilterSortLimit(new PSApplicationSummary[] {src}, null, 500, 0);

    assertEquals(1, out.size());
    ApplicationSummary dto = out.get(0);
    assertEquals(7, dto.getId());
    assertEquals("sys_cmpDocuments", dto.getName());
    assertEquals("System docs", dto.getDescription());
    assertEquals(Boolean.TRUE, dto.getEnabled());
    assertEquals("sys_cmpDocuments", dto.getAppRoot());
    assertEquals("CONTENT_EDITOR", dto.getAppType());
    assertEquals("8.2", dto.getVersion());
    assertEquals(Boolean.FALSE, dto.getEmpty());
    assertEquals(Boolean.FALSE, dto.getHidden());
  }

  @Test
  void mapFilterSortLimit_filtersByNameAndDescriptionCaseInsensitive() {
    PSApplicationSummary a = summary(1, "AlphaApp", "first", true, "a", false, false);
    PSApplicationSummary b = summary(2, "Beta", "Has DOCS inside", true, "b", false, false);
    PSApplicationSummary c = summary(3, "gamma", "other", true, "c", false, false);
    PSApplicationSummary[] sums = {a, b, c};

    List<ApplicationSummary> byName = PipelinesAdaptor.mapFilterSortLimit(sums, "ALPHA", 500, 0);
    assertEquals(1, byName.size());
    assertEquals("AlphaApp", byName.get(0).getName());

    List<ApplicationSummary> byDesc = PipelinesAdaptor.mapFilterSortLimit(sums, "docs", 500, 0);
    assertEquals(1, byDesc.size());
    assertEquals("Beta", byDesc.get(0).getName());
  }

  @Test
  void mapFilterSortLimit_sortsByNameCaseInsensitive() {
    PSApplicationSummary z = summary(1, "zeta", null, true, "z", false, false);
    PSApplicationSummary a = summary(2, "Alpha", null, true, "a", false, false);
    PSApplicationSummary m = summary(3, "mid", null, true, "m", false, false);

    List<ApplicationSummary> out =
        PipelinesAdaptor.mapFilterSortLimit(new PSApplicationSummary[] {z, a, m}, null, 500, 0);

    assertEquals(
        List.of("Alpha", "mid", "zeta"), out.stream().map(ApplicationSummary::getName).toList());
  }

  @Test
  void mapFilterSortLimit_clampsLimitToMaxAndDefaultsNonPositive() {
    PSApplicationSummary[] sums = new PSApplicationSummary[5];
    for (int i = 0; i < 5; i++) {
      sums[i] = summary(i, "app" + i, null, true, "r" + i, false, false);
    }

    List<ApplicationSummary> defaulted = PipelinesAdaptor.mapFilterSortLimit(sums, null, 0, 0);
    assertEquals(5, defaulted.size());

    List<ApplicationSummary> negativeLimit = PipelinesAdaptor.mapFilterSortLimit(sums, null, -3, 0);
    assertEquals(5, negativeLimit.size());

    // Hard cap: even if limit is huge, at most MAX_LIMIT — with only 5 rows, still 5
    List<ApplicationSummary> huge =
        PipelinesAdaptor.mapFilterSortLimit(sums, null, PipelinesAdaptor.MAX_LIMIT + 500, 0);
    assertEquals(5, huge.size());
    assertEquals(1000, PipelinesAdaptor.MAX_LIMIT);
    assertEquals(500, PipelinesAdaptor.DEFAULT_LIMIT);
  }

  @Test
  void mapFilterSortLimit_appliesOffsetAndLimitWindow() {
    PSApplicationSummary[] sums = new PSApplicationSummary[4];
    for (int i = 0; i < 4; i++) {
      // names force sort order app0..app3
      sums[i] = summary(i, "app" + i, null, true, "r" + i, false, false);
    }

    List<ApplicationSummary> page = PipelinesAdaptor.mapFilterSortLimit(sums, null, 2, 1);
    assertEquals(2, page.size());
    assertEquals("app1", page.get(0).getName());
    assertEquals("app2", page.get(1).getName());
  }

  @Test
  void mapFilterSortLimit_offsetPastEndReturnsEmpty() {
    PSApplicationSummary only = summary(1, "solo", null, true, "s", false, false);
    List<ApplicationSummary> out =
        PipelinesAdaptor.mapFilterSortLimit(new PSApplicationSummary[] {only}, null, 10, 5);
    assertTrue(out.isEmpty());
  }

  @Test
  void toDetail_mapsAppMetaAndDataSets() {
    PSApplication app = mock(PSApplication.class);
    when(app.getId()).thenReturn(42);
    when(app.getName()).thenReturn("sys_cmpDocuments");
    when(app.getDescription()).thenReturn("Docs app");
    when(app.isEnabled()).thenReturn(true);
    when(app.isHidden()).thenReturn(false);
    when(app.getRequestRoot()).thenReturn("sys_cmpDocuments");
    when(app.getApplicationType()).thenReturn(PSApplicationType.CONTENT_EDITOR);
    when(app.getVersion()).thenReturn("8.2");

    PSDataSet ds = mock(PSDataSet.class);
    when(ds.getName()).thenReturn("contenteditor");
    when(ds.getDescription()).thenReturn("CE");
    PSRequestor req = mock(PSRequestor.class);
    when(req.getRequestPage()).thenReturn("contenteditor.html");
    when(ds.getRequestor()).thenReturn(req);

    PSCollection coll = new PSCollection(PSDataSet.class);
    coll.add(ds);
    when(app.getDataSets()).thenReturn(coll);

    ApplicationDetail detail = PipelinesAdaptor.toDetail(app);
    assertEquals(42, detail.getId());
    assertEquals("sys_cmpDocuments", detail.getName());
    assertEquals("CONTENT_EDITOR", detail.getAppType());
    assertEquals(1, detail.getDataSets().size());
    assertEquals("contenteditor", detail.getDataSets().get(0).getName());
    assertEquals("contenteditor.html", detail.getDataSets().get(0).getRequestPage());
    assertEquals("DATASET", detail.getDataSets().get(0).getKind());
    assertNotNull(detail.getDesignGaps());
    assertFalse(detail.getDesignGaps().isEmpty());
  }

  @Test
  void mapFilterSortLimit_skipsNullEntriesAndNullArray() {
    assertTrue(PipelinesAdaptor.mapFilterSortLimit(null, null, 10, 0).isEmpty());

    PSApplicationSummary real = summary(1, "real", null, true, "r", false, false);
    List<ApplicationSummary> out =
        PipelinesAdaptor.mapFilterSortLimit(
            new PSApplicationSummary[] {null, real, null}, null, 10, 0);
    assertEquals(1, out.size());
    assertEquals("real", out.get(0).getName());
  }

  @Test
  void mapFilterSortLimit_mapsHiddenFlag() {
    PSApplicationSummary hidden = summary(9, "secret", "hidden app", false, "sec", true, true);

    List<ApplicationSummary> out =
        PipelinesAdaptor.mapFilterSortLimit(new PSApplicationSummary[] {hidden}, null, 10, 0);

    assertEquals(1, out.size());
    assertEquals(Boolean.TRUE, out.get(0).getHidden());
    assertEquals(Boolean.TRUE, out.get(0).getEmpty());
    assertEquals(Boolean.FALSE, out.get(0).getEnabled());
  }

  @Test
  void matchesNameFilter_checksNameOrDescription() {
    ApplicationSummary dto = new ApplicationSummary();
    dto.setName("Sys_Foo");
    dto.setDescription("Pipeline package");
    assertTrue(PipelinesAdaptor.matchesNameFilter(dto, "sys_"));
    assertTrue(PipelinesAdaptor.matchesNameFilter(dto, "package"));
    assertFalse(PipelinesAdaptor.matchesNameFilter(dto, "missing"));
  }

  @Test
  void isSafeApplicationName_rejectsPathTraversal() {
    assertTrue(PipelinesAdaptor.isSafeApplicationName("sys_cmpDocuments"));
    assertTrue(PipelinesAdaptor.isSafeApplicationName("42"));
    assertFalse(PipelinesAdaptor.isSafeApplicationName("../etc/passwd"));
    assertFalse(PipelinesAdaptor.isSafeApplicationName("foo/bar"));
    assertFalse(PipelinesAdaptor.isSafeApplicationName("foo\\bar"));
    assertFalse(PipelinesAdaptor.isSafeApplicationName(""));
    assertFalse(PipelinesAdaptor.isSafeApplicationName(null));
  }

  @Test
  void resolveApplicationName_returnsTrustedCatalogNameOnly() {
    PSApplicationSummary a = summary(7, "sys_cmpDocuments", "docs", true, "r", false, false);
    PSApplicationSummary[] sums = {a};

    // by name (case-insensitive) → catalog name, not raw user casing
    assertEquals(
        "sys_cmpDocuments", PipelinesAdaptor.resolveApplicationName("SYS_CMPDOCUMENTS", sums));
    // by id
    assertEquals("sys_cmpDocuments", PipelinesAdaptor.resolveApplicationName("7", sums));
    // unknown / path injection attempts
    assertNull(PipelinesAdaptor.resolveApplicationName("missing", sums));
    assertNull(PipelinesAdaptor.resolveApplicationName("../sys_cmpDocuments", sums));
    assertNull(PipelinesAdaptor.resolveApplicationName("sys_cmpDocuments/../other", sums));
    assertNull(PipelinesAdaptor.resolveApplicationName("99", sums));
  }

  @Test
  void execute_delegatesToRuntimeService() throws Exception {
    IPSPipelineRuntimeService runtime = mock(IPSPipelineRuntimeService.class);
    PipelineExecuteResult expected = new PipelineExecuteResult();
    expected.setAppName("lookupApp");
    expected.setResourceName("DatasetQ");
    expected.setOperation("query");
    expected.setRows(List.of(Map.of("TYPE", "workflow")));
    PipelineExecuteRequest req = PipelineExecuteRequest.ofParams(Map.of("TYPE", "workflow"));
    when(runtime.execute(eq("lookupApp"), eq("DatasetQ"), eq(req))).thenReturn(expected);

    PipelinesAdaptor adaptor =
        new PipelinesAdaptor(tok -> new PSApplicationSummary[0], () -> runtime);
    PipelineExecuteResult out =
        adaptor.execute(URI.create("http://localhost/services/"), "lookupApp", "DatasetQ", req);

    assertEquals("query", out.getOperation());
    assertEquals(1, out.getRowCount());
    verify(runtime).execute(eq("lookupApp"), eq("DatasetQ"), eq(req));
  }

  @Test
  void execute_nullBodyBecomesEmptyRequest() throws Exception {
    IPSPipelineRuntimeService runtime = mock(IPSPipelineRuntimeService.class);
    PipelineExecuteResult expected = new PipelineExecuteResult();
    expected.setOperation("query");
    when(runtime.execute(eq("app"), eq("res"), any(PipelineExecuteRequest.class)))
        .thenReturn(expected);

    PipelinesAdaptor adaptor =
        new PipelinesAdaptor(tok -> new PSApplicationSummary[0], () -> runtime);
    assertEquals(
        "query",
        adaptor.execute(URI.create("http://localhost/"), "app", "res", null).getOperation());
  }

  @Test
  void execute_rejectsUnsafeAppOrResourceNames() {
    IPSPipelineRuntimeService runtime = mock(IPSPipelineRuntimeService.class);
    PipelinesAdaptor adaptor =
        new PipelinesAdaptor(tok -> new PSApplicationSummary[0], () -> runtime);

    WebApplicationException badApp =
        assertThrows(
            WebApplicationException.class,
            () ->
                adaptor.execute(
                    URI.create("http://localhost/"),
                    "../evil",
                    "res",
                    PipelineExecuteRequest.empty()));
    assertEquals(400, badApp.getResponse().getStatus());

    WebApplicationException badRes =
        assertThrows(
            WebApplicationException.class,
            () ->
                adaptor.execute(
                    URI.create("http://localhost/"),
                    "app",
                    "foo/bar",
                    PipelineExecuteRequest.empty()));
    assertEquals(400, badRes.getResponse().getStatus());
  }

  @Test
  void execute_mapsNotFoundIrTo404() throws Exception {
    IPSPipelineRuntimeService runtime = mock(IPSPipelineRuntimeService.class);
    when(runtime.execute(anyString(), anyString(), any(PipelineExecuteRequest.class)))
        .thenThrow(new PSPipelineIrException("Pipeline IR not found: missing"));
    PipelinesAdaptor adaptor =
        new PipelinesAdaptor(tok -> new PSApplicationSummary[0], () -> runtime);

    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () ->
                adaptor.execute(
                    URI.create("http://localhost/"),
                    "missing",
                    "res",
                    PipelineExecuteRequest.empty()));
    assertEquals(404, ex.getResponse().getStatus());
    assertEquals("Pipeline application or resource not found", ex.getMessage());
  }

  @Test
  void execute_mapsPlannerFailureTo400() throws Exception {
    IPSPipelineRuntimeService runtime = mock(IPSPipelineRuntimeService.class);
    when(runtime.execute(anyString(), anyString(), any(PipelineExecuteRequest.class)))
        .thenThrow(new PSPipelineIrException("Insert requires request.rows or request.params"));
    PipelinesAdaptor adaptor =
        new PipelinesAdaptor(tok -> new PSApplicationSummary[0], () -> runtime);

    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () ->
                adaptor.execute(
                    URI.create("http://localhost/"),
                    "updApp",
                    "Ins",
                    PipelineExecuteRequest.empty()));
    assertEquals(400, ex.getResponse().getStatus());
    assertTrue(ex.getMessage().contains("Insert requires"));
  }

  @Test
  void isNotFoundMessage_detectsRuntimeNotFoundWording() {
    assertTrue(PipelinesAdaptor.isNotFoundMessage("Pipeline IR not found: x"));
    assertTrue(PipelinesAdaptor.isNotFoundMessage("Resource not found in IR x: y"));
    assertFalse(PipelinesAdaptor.isNotFoundMessage("Insert requires request.rows"));
    assertFalse(PipelinesAdaptor.isNotFoundMessage(null));
  }

  private static PSApplicationSummary summary(
      int id,
      String name,
      String description,
      boolean enabled,
      String appRoot,
      boolean empty,
      boolean hidden) {
    PSApplicationSummary sum = mock(PSApplicationSummary.class);
    when(sum.getId()).thenReturn(id);
    when(sum.getName()).thenReturn(name);
    when(sum.getDescription()).thenReturn(description);
    when(sum.isEnabled()).thenReturn(enabled);
    when(sum.getAppRoot()).thenReturn(appRoot);
    when(sum.isEmpty()).thenReturn(empty);
    when(sum.isHidden()).thenReturn(hidden);
    when(sum.getAppType()).thenReturn(PSApplicationType.USER);
    when(sum.getVersion()).thenReturn(null);
    return sum;
  }
}
