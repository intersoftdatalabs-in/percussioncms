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
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.ArgumentCaptor;

import com.percussion.design.objectstore.PSApplication;
import com.percussion.design.objectstore.PSApplicationType;
import com.percussion.design.objectstore.PSDataSet;
import com.percussion.design.objectstore.PSRequestor;
import com.percussion.design.objectstore.server.PSApplicationSummary;
import com.percussion.rest.pipelines.ApplicationDetail;
import com.percussion.rest.pipelines.ApplicationSummary;
import com.percussion.rest.pipelines.ApplicationValidationProblem;
import com.percussion.rest.pipelines.ApplicationValidationResult;
import com.percussion.rest.pipelines.PipelineHttpBackendTank;
import com.percussion.security.PSSecurityToken;
import com.percussion.server.PSRequest;
import com.percussion.services.pipeline.IPSPipelineRuntimeService;
import java.util.Optional;
import com.percussion.services.pipeline.http.PSPipelineHttpUrl;
import com.percussion.services.pipeline.model.BackendTankStageIr;
import com.percussion.services.pipeline.model.PipelineIrDocument;
import com.percussion.services.pipeline.IPSPipelineIrService;
import com.percussion.services.pipeline.PSPipelineIrException;
import com.percussion.services.pipeline.model.PipelineExecuteRequest;
import com.percussion.services.pipeline.model.PipelineExecuteResult;
import com.percussion.servlets.PSSecurityFilter;
import com.percussion.util.PSCollection;
import jakarta.ws.rs.WebApplicationException;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

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
    assertTrue(
        detail.getDesignGaps().stream().noneMatch(g -> g.toLowerCase().contains("start")),
        "start/stop shipped — designGaps must not claim start/stop unsupported");
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

    PipelinesAdaptor adaptor = new PipelinesAdaptor(tok -> new PSApplicationSummary[0], () -> runtime);
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

    PipelinesAdaptor adaptor = new PipelinesAdaptor(tok -> new PSApplicationSummary[0], () -> runtime);
    assertEquals(
        "query",
        adaptor.execute(URI.create("http://localhost/"), "app", "res", null).getOperation());
  }

  @Test
  void execute_rejectsUnsafeAppOrResourceNames() {
    IPSPipelineRuntimeService runtime = mock(IPSPipelineRuntimeService.class);
    PipelinesAdaptor adaptor = new PipelinesAdaptor(tok -> new PSApplicationSummary[0], () -> runtime);

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
    PipelinesAdaptor adaptor = new PipelinesAdaptor(tok -> new PSApplicationSummary[0], () -> runtime);

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
    PipelinesAdaptor adaptor = new PipelinesAdaptor(tok -> new PSApplicationSummary[0], () -> runtime);

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

  @Test
  void startApplication_requiresAdmin() {
    PSApplicationSummary sum = summary(7, "sys_cmpDocuments", "docs", true, "r", false, false);
    PipelinesAdaptor adaptor =
        new PipelinesAdaptor(
            tok -> new PSApplicationSummary[] {sum},
            () -> mock(IPSPipelineRuntimeService.class),
            () -> false,
            noopLifecycle(),
            (name, tok) -> detailNamed(name, true));

    try (MockedStatic<PSSecurityFilter> security = mockStatic(PSSecurityFilter.class)) {
      stubCurrentRequest(security);
      WebApplicationException ex =
          assertThrows(
              WebApplicationException.class,
              () ->
                  adaptor.startApplication(
                      URI.create("http://localhost/services/"), "sys_cmpDocuments"));
      assertEquals(403, ex.getResponse().getStatus());
      assertEquals(PipelinesAdaptor.ADMIN_REQUIRED, ex.getMessage());
    }
  }

  @Test
  void startApplication_adminCheckUnexpectedRuntimeMapsTo500() {
    PSApplicationSummary sum = summary(7, "sys_cmpDocuments", "docs", true, "r", false, false);
    PipelinesAdaptor adaptor =
        new PipelinesAdaptor(
            tok -> new PSApplicationSummary[] {sum},
            () -> mock(IPSPipelineRuntimeService.class),
            () -> {
              throw new IllegalStateException("user service exploded");
            },
            noopLifecycle(),
            (name, tok) -> detailNamed(name, true));

    try (MockedStatic<PSSecurityFilter> security = mockStatic(PSSecurityFilter.class)) {
      stubCurrentRequest(security);
      WebApplicationException ex =
          assertThrows(
              WebApplicationException.class,
              () ->
                  adaptor.startApplication(
                      URI.create("http://localhost/services/"), "sys_cmpDocuments"));
      assertEquals(500, ex.getResponse().getStatus());
      assertEquals("Admin authorization check failed", ex.getMessage());
    }
  }

  @Test
  void startApplication_startsWhenStopped() throws Exception {
    PSApplicationSummary sum = summary(7, "sys_cmpDocuments", "docs", true, "r", false, false);
    AtomicBoolean active = new AtomicBoolean(false);
    AtomicBoolean started = new AtomicBoolean(false);
    PipelinesAdaptor.ApplicationLifecycleOps ops =
        new PipelinesAdaptor.ApplicationLifecycleOps() {
          @Override
          public boolean isActive(String appName) {
            return active.get();
          }

          @Override
          public void start(String appName) {
            started.set(true);
            active.set(true);
          }

          @Override
          public boolean stop(String appName) {
            return false;
          }
        };
    PipelinesAdaptor adaptor =
        new PipelinesAdaptor(
            tok -> new PSApplicationSummary[] {sum},
            () -> mock(IPSPipelineRuntimeService.class),
            () -> true,
            ops,
            (name, tok) -> detailNamed(name, active.get()));

    try (MockedStatic<PSSecurityFilter> security = mockStatic(PSSecurityFilter.class)) {
      stubCurrentRequest(security);
      ApplicationDetail out =
          adaptor.startApplication(URI.create("http://localhost/services/"), "sys_cmpDocuments");
      assertNotNull(out);
      assertEquals("sys_cmpDocuments", out.getName());
      assertEquals(Boolean.TRUE, out.getActive());
      assertTrue(started.get());
    }
  }

  @Test
  void startApplication_idempotentWhenAlreadyRunning() throws Exception {
    PSApplicationSummary sum = summary(7, "sys_cmpDocuments", "docs", true, "r", false, false);
    PipelinesAdaptor.ApplicationLifecycleOps ops = mock(PipelinesAdaptor.ApplicationLifecycleOps.class);
    when(ops.isActive("sys_cmpDocuments")).thenReturn(true);
    PipelinesAdaptor adaptor =
        new PipelinesAdaptor(
            tok -> new PSApplicationSummary[] {sum},
            () -> mock(IPSPipelineRuntimeService.class),
            () -> true,
            ops,
            (name, tok) -> detailNamed(name, true));

    try (MockedStatic<PSSecurityFilter> security = mockStatic(PSSecurityFilter.class)) {
      stubCurrentRequest(security);
      ApplicationDetail out =
          adaptor.startApplication(URI.create("http://localhost/services/"), "7");
      assertEquals(Boolean.TRUE, out.getActive());
      verify(ops, never()).start(anyString());
    }
  }

  @Test
  void startApplication_rejectsHiddenAndDisabled() {
    PSApplicationSummary hidden = summary(1, "hiddenApp", "h", true, "r", false, true);
    PSApplicationSummary disabled = summary(2, "disabledApp", "d", false, "r", false, false);
    PipelinesAdaptor.ApplicationLifecycleOps ops = noopLifecycle();

    try (MockedStatic<PSSecurityFilter> security = mockStatic(PSSecurityFilter.class)) {
      stubCurrentRequest(security);

      PipelinesAdaptor hiddenAdaptor =
          new PipelinesAdaptor(
              tok -> new PSApplicationSummary[] {hidden},
              () -> mock(IPSPipelineRuntimeService.class),
              () -> true,
              ops,
              (name, tok) -> detailNamed(name, false));
      WebApplicationException hiddenEx =
          assertThrows(
              WebApplicationException.class,
              () ->
                  hiddenAdaptor.startApplication(
                      URI.create("http://localhost/"), "hiddenApp"));
      assertEquals(400, hiddenEx.getResponse().getStatus());
      assertEquals(PipelinesAdaptor.HIDDEN_NOT_ALLOWED, hiddenEx.getMessage());

      PipelinesAdaptor disabledAdaptor =
          new PipelinesAdaptor(
              tok -> new PSApplicationSummary[] {disabled},
              () -> mock(IPSPipelineRuntimeService.class),
              () -> true,
              ops,
              (name, tok) -> detailNamed(name, false));
      WebApplicationException disabledEx =
          assertThrows(
              WebApplicationException.class,
              () ->
                  disabledAdaptor.startApplication(
                      URI.create("http://localhost/"), "disabledApp"));
      assertEquals(400, disabledEx.getResponse().getStatus());
      assertEquals(PipelinesAdaptor.DISABLED_NOT_ALLOWED, disabledEx.getMessage());
    }
  }

  @Test
  void startApplication_unknownReturnsNull() {
    PipelinesAdaptor adaptor =
        new PipelinesAdaptor(
            tok -> new PSApplicationSummary[0],
            () -> mock(IPSPipelineRuntimeService.class),
            () -> true,
            noopLifecycle(),
            (name, tok) -> null);

    try (MockedStatic<PSSecurityFilter> security = mockStatic(PSSecurityFilter.class)) {
      stubCurrentRequest(security);
      assertNull(
          adaptor.startApplication(URI.create("http://localhost/"), "missing"));
    }
  }

  @Test
  void stopApplication_stopsWhenRunningAndIdempotentWhenStopped() throws Exception {
    PSApplicationSummary sum = summary(7, "sys_cmpDocuments", "docs", true, "r", false, false);
    AtomicBoolean active = new AtomicBoolean(true);
    AtomicBoolean stopped = new AtomicBoolean(false);
    PipelinesAdaptor.ApplicationLifecycleOps ops =
        new PipelinesAdaptor.ApplicationLifecycleOps() {
          @Override
          public boolean isActive(String appName) {
            return active.get();
          }

          @Override
          public void start(String appName) {}

          @Override
          public boolean stop(String appName) {
            stopped.set(true);
            active.set(false);
            return true;
          }
        };
    PipelinesAdaptor adaptor =
        new PipelinesAdaptor(
            tok -> new PSApplicationSummary[] {sum},
            () -> mock(IPSPipelineRuntimeService.class),
            () -> true,
            ops,
            (name, tok) -> detailNamed(name, active.get()));

    try (MockedStatic<PSSecurityFilter> security = mockStatic(PSSecurityFilter.class)) {
      stubCurrentRequest(security);
      ApplicationDetail out =
          adaptor.stopApplication(URI.create("http://localhost/services/"), "sys_cmpDocuments");
      assertEquals(Boolean.FALSE, out.getActive());
      assertTrue(stopped.get());

      // Second stop is idempotent — already stopped, do not call stop again meaningfully.
      AtomicBoolean secondStop = new AtomicBoolean(false);
      PipelinesAdaptor.ApplicationLifecycleOps stoppedOps =
          new PipelinesAdaptor.ApplicationLifecycleOps() {
            @Override
            public boolean isActive(String appName) {
              return false;
            }

            @Override
            public void start(String appName) {}

            @Override
            public boolean stop(String appName) {
              secondStop.set(true);
              return false;
            }
          };
      PipelinesAdaptor idempotent =
          new PipelinesAdaptor(
              tok -> new PSApplicationSummary[] {sum},
              () -> mock(IPSPipelineRuntimeService.class),
              () -> true,
              stoppedOps,
              (name, tok) -> detailNamed(name, false));
      ApplicationDetail again =
          idempotent.stopApplication(URI.create("http://localhost/services/"), "sys_cmpDocuments");
      assertEquals(Boolean.FALSE, again.getActive());
      assertFalse(secondStop.get());
    }
  }

  @Test
  void stopApplication_rejectsHiddenAndRequiresAdmin() {
    PSApplicationSummary hidden = summary(1, "hiddenApp", "h", true, "r", false, true);

    try (MockedStatic<PSSecurityFilter> security = mockStatic(PSSecurityFilter.class)) {
      stubCurrentRequest(security);

      PipelinesAdaptor forbidden =
          new PipelinesAdaptor(
              tok -> new PSApplicationSummary[] {hidden},
              () -> mock(IPSPipelineRuntimeService.class),
              () -> false,
              noopLifecycle(),
              (name, tok) -> detailNamed(name, true));
      WebApplicationException adminEx =
          assertThrows(
              WebApplicationException.class,
              () -> forbidden.stopApplication(URI.create("http://localhost/"), "hiddenApp"));
      assertEquals(403, adminEx.getResponse().getStatus());

      PipelinesAdaptor hiddenAdaptor =
          new PipelinesAdaptor(
              tok -> new PSApplicationSummary[] {hidden},
              () -> mock(IPSPipelineRuntimeService.class),
              () -> true,
              noopLifecycle(),
              (name, tok) -> detailNamed(name, true));
      WebApplicationException hiddenEx =
          assertThrows(
              WebApplicationException.class,
              () ->
                  hiddenAdaptor.stopApplication(URI.create("http://localhost/"), "hiddenApp"));
      assertEquals(400, hiddenEx.getResponse().getStatus());
      assertEquals(PipelinesAdaptor.HIDDEN_NOT_ALLOWED, hiddenEx.getMessage());
    }
  }

  @Test
  void getValidation_requiresAdminAndRejectsHidden() {
    PSApplicationSummary visible = summary(7, "sys_cmpDocuments", "docs", true, "r", false, false);
    PSApplicationSummary hidden = summary(1, "hiddenApp", "h", true, "r", false, true);
    PipelinesAdaptor.ApplicationValidationOps ops =
        (name, sum, tok) -> PipelinesAdaptor.toValidationResult(sum.getId(), name, List.of());

    try (MockedStatic<PSSecurityFilter> security = mockStatic(PSSecurityFilter.class)) {
      stubCurrentRequest(security);

      PipelinesAdaptor forbidden =
          new PipelinesAdaptor(
              tok -> new PSApplicationSummary[] {visible},
              () -> mock(IPSPipelineRuntimeService.class),
              () -> false,
              noopLifecycle(),
              (name, tok) -> detailNamed(name, false),
              ops);
      WebApplicationException adminEx =
          assertThrows(
              WebApplicationException.class,
              () ->
                  forbidden.getValidation(
                      URI.create("http://localhost/"), "sys_cmpDocuments"));
      assertEquals(403, adminEx.getResponse().getStatus());
      assertEquals(PipelinesAdaptor.ADMIN_REQUIRED, adminEx.getMessage());

      PipelinesAdaptor hiddenAdaptor =
          new PipelinesAdaptor(
              tok -> new PSApplicationSummary[] {hidden},
              () -> mock(IPSPipelineRuntimeService.class),
              () -> true,
              noopLifecycle(),
              (name, tok) -> detailNamed(name, false),
              ops);
      WebApplicationException hiddenEx =
          assertThrows(
              WebApplicationException.class,
              () -> hiddenAdaptor.getValidation(URI.create("http://localhost/"), "hiddenApp"));
      assertEquals(400, hiddenEx.getResponse().getStatus());
      assertEquals(PipelinesAdaptor.HIDDEN_NOT_ALLOWED, hiddenEx.getMessage());
    }
  }

  @Test
  void getValidation_returnsProblemsSummaryForTrustedCatalogName() {
    PSApplicationSummary sum = summary(7, "sys_cmpDocuments", "docs", true, "r", false, false);
    ApplicationValidationProblem error = new ApplicationValidationProblem();
    error.setSeverity(CollectingApplicationValidator.SEVERITY_ERROR);
    error.setCode("1301");
    error.setMessage("Missing requestor");
    error.setResource("contenteditor");
    error.setPath("PSDataSet#1[contenteditor]");
    ApplicationValidationProblem warning = new ApplicationValidationProblem();
    warning.setSeverity(CollectingApplicationValidator.SEVERITY_WARNING);
    warning.setCode("1400");
    warning.setMessage("Inefficient mapping");

    PipelinesAdaptor.ApplicationValidationOps ops =
        (name, summary, tok) ->
            PipelinesAdaptor.toValidationResult(summary.getId(), name, List.of(error, warning));
    PipelinesAdaptor adaptor =
        new PipelinesAdaptor(
            tok -> new PSApplicationSummary[] {sum},
            () -> mock(IPSPipelineRuntimeService.class),
            () -> true,
            noopLifecycle(),
            (name, tok) -> detailNamed(name, false),
            ops);

    try (MockedStatic<PSSecurityFilter> security = mockStatic(PSSecurityFilter.class)) {
      stubCurrentRequest(security);
      ApplicationValidationResult out =
          adaptor.getValidation(URI.create("http://localhost/services/"), "SYS_CMPDOCUMENTS");
      assertNotNull(out);
      assertEquals(7, out.getId());
      assertEquals("sys_cmpDocuments", out.getName());
      assertEquals(Boolean.FALSE, out.getValid());
      assertEquals(1, out.getErrorCount());
      assertEquals(1, out.getWarningCount());
      assertEquals(2, out.getProblems().size());
      assertEquals("1301", out.getProblems().get(0).getCode());
    }
  }

  @Test
  void getValidation_unknownReturnsNullAndRejectsPathTraversal() {
    PipelinesAdaptor.ApplicationValidationOps ops =
        (name, sum, tok) -> PipelinesAdaptor.toValidationResult(1, name, List.of());
    PipelinesAdaptor adaptor =
        new PipelinesAdaptor(
            tok -> new PSApplicationSummary[0],
            () -> mock(IPSPipelineRuntimeService.class),
            () -> true,
            noopLifecycle(),
            (name, tok) -> null,
            ops);

    try (MockedStatic<PSSecurityFilter> security = mockStatic(PSSecurityFilter.class)) {
      stubCurrentRequest(security);
      assertNull(adaptor.getValidation(URI.create("http://localhost/"), "missing"));
      assertNull(adaptor.getValidation(URI.create("http://localhost/"), "../evil"));
    }
  }

  @Test
  void toValidationResult_countsSeveritiesAndMarksValidWhenNoErrors() {
    ApplicationValidationProblem warning = new ApplicationValidationProblem();
    warning.setSeverity(CollectingApplicationValidator.SEVERITY_WARNING);
    warning.setCode("1");
    warning.setMessage("warn");

    ApplicationValidationResult validOnlyWarnings =
        PipelinesAdaptor.toValidationResult(3, "app", List.of(warning));
    assertEquals(Boolean.TRUE, validOnlyWarnings.getValid());
    assertEquals(0, validOnlyWarnings.getErrorCount());
    assertEquals(1, validOnlyWarnings.getWarningCount());

    ApplicationValidationResult empty = PipelinesAdaptor.toValidationResult(3, "app", List.of());
    assertEquals(Boolean.TRUE, empty.getValid());
    assertEquals(0, empty.getErrorCount());
    assertEquals(0, empty.getWarningCount());
  }

  @Test
  void defaultDesignGaps_doesNotClaimValidationReadUnsupported() {
    List<String> gaps = PipelinesAdaptor.defaultDesignGaps();
    assertTrue(
        gaps.stream().noneMatch(g -> g.toLowerCase().contains("validation")),
        "validation/problems read shipped — designGaps must not claim it unsupported");
    assertTrue(
        gaps.stream().anyMatch(g -> g.toLowerCase().contains("graph edit")),
        "graph edit / IR write should remain listed as a gap");
    assertTrue(
        gaps.stream().anyMatch(g -> g.toLowerCase().contains("backendtank")),
        "HTTP backend tank persist should be called out as the native write that shipped");
  }

  @Test
  void putHttpBackendTank_requiresAdmin() {
    PSApplicationSummary sum = summary(7, "sys_cmpDocuments", "docs", true, "r", false, false);
    PipelinesAdaptor adaptor =
        new PipelinesAdaptor(
            tok -> new PSApplicationSummary[] {sum},
            () -> mock(IPSPipelineRuntimeService.class),
            () -> mock(IPSPipelineIrService.class),
            (name, tok) -> mock(PSApplication.class),
            () -> false,
            noopLifecycle(),
            (name, tok) -> detailNamed(name, true),
            null);

    try (MockedStatic<PSSecurityFilter> security = mockStatic(PSSecurityFilter.class)) {
      stubCurrentRequest(security);
      PipelineHttpBackendTank body = new PipelineHttpBackendTank();
      body.setAdapterType("HTTP");
      body.setUrl(PSPipelineHttpUrl.BUNDLED_FIXTURE_URL);
      WebApplicationException ex =
          assertThrows(
              WebApplicationException.class,
              () ->
                  adaptor.putHttpBackendTank(
                      URI.create("http://localhost/"), "sys_cmpDocuments", "items", body));
      assertEquals(403, ex.getResponse().getStatus());
    }
  }

  @Test
  void putHttpBackendTank_rejectsCloudUrl() {
    PSApplicationSummary sum = summary(7, "sys_cmpDocuments", "docs", true, "r", false, false);
    PipelinesAdaptor adaptor =
        new PipelinesAdaptor(
            tok -> new PSApplicationSummary[] {sum},
            () -> mock(IPSPipelineRuntimeService.class),
            () -> mock(IPSPipelineIrService.class),
            (name, tok) -> mock(PSApplication.class),
            () -> true,
            noopLifecycle(),
            (name, tok) -> detailNamed(name, true),
            null);

    try (MockedStatic<PSSecurityFilter> security = mockStatic(PSSecurityFilter.class)) {
      stubCurrentRequest(security);
      PipelineHttpBackendTank body = new PipelineHttpBackendTank();
      body.setAdapterType("HTTP");
      body.setUrl("https://erp.example/api/items");
      WebApplicationException ex =
          assertThrows(
              WebApplicationException.class,
              () ->
                  adaptor.putHttpBackendTank(
                      URI.create("http://localhost/"), "sys_cmpDocuments", "items", body));
      assertEquals(400, ex.getResponse().getStatus());
      assertTrue(ex.getMessage().toLowerCase().contains("loopback"), ex.getMessage());
    }
  }

  @Test
  void putHttpBackendTank_savesNativeIrWithoutClassicXmlWrite() throws Exception {
    PSApplicationSummary sum = summary(7, "sys_cmpDocuments", "docs", true, "r", false, false);
    IPSPipelineIrService ir = mock(IPSPipelineIrService.class);
    when(ir.load("sys_cmpDocuments")).thenReturn(Optional.empty());
    PipelineIrDocument imported = new PipelineIrDocument();
    imported.setSource(PipelineIrDocument.SOURCE_CLASSIC_IMPORT);
    imported.getApp().setName("sys_cmpDocuments");
    PSApplication app = mock(PSApplication.class);
    when(ir.importClassicApplication(app)).thenReturn(imported);

    PipelinesAdaptor adaptor =
        new PipelinesAdaptor(
            tok -> new PSApplicationSummary[] {sum},
            () -> mock(IPSPipelineRuntimeService.class),
            () -> ir,
            (name, tok) -> app,
            () -> true,
            noopLifecycle(),
            (name, tok) -> detailNamed(name, true),
            null);

    try (MockedStatic<PSSecurityFilter> security = mockStatic(PSSecurityFilter.class)) {
      stubCurrentRequest(security);
      PipelineHttpBackendTank body = new PipelineHttpBackendTank();
      body.setAdapterType("HTTP");
      body.setUrl(PSPipelineHttpUrl.BUNDLED_FIXTURE_URL);
      PipelineHttpBackendTank saved =
          adaptor.putHttpBackendTank(
              URI.create("http://localhost/"), "sys_cmpDocuments", "items", body);
      assertEquals("HTTP", saved.getAdapterType());
      assertEquals(PSPipelineHttpUrl.BUNDLED_FIXTURE_URL, saved.getUrl());
      ArgumentCaptor<PipelineIrDocument> cap = ArgumentCaptor.forClass(PipelineIrDocument.class);
      verify(ir).save(cap.capture());
      PipelineIrDocument persisted = cap.getValue();
      assertEquals(PipelineIrDocument.SOURCE_NATIVE, persisted.getSource());
      BackendTankStageIr tank = persisted.findResource("items").getStages().getBackendTank();
      assertTrue(tank.isHttpAdapter());
      assertEquals(PSPipelineHttpUrl.BUNDLED_FIXTURE_URL, tank.getUrl());
      verify(app, never()).setName(any());
    }
  }

  private static void stubCurrentRequest(MockedStatic<PSSecurityFilter> security) {
    PSRequest req = mock(PSRequest.class);
    PSSecurityToken tok = mock(PSSecurityToken.class);
    when(req.getSecurityToken()).thenReturn(tok);
    security.when(PSSecurityFilter::getCurrentRequest).thenReturn(req);
  }

  private static ApplicationDetail detailNamed(String name, boolean active) {
    ApplicationDetail d = new ApplicationDetail();
    d.setName(name);
    d.setActive(active);
    d.setEnabled(true);
    d.setHidden(false);
    d.setDesignGaps(PipelinesAdaptor.defaultDesignGaps());
    return d;
  }

  private static PipelinesAdaptor.ApplicationLifecycleOps noopLifecycle() {
    return new PipelinesAdaptor.ApplicationLifecycleOps() {
      @Override
      public boolean isActive(String appName) {
        return false;
      }

      @Override
      public void start(String appName) {}

      @Override
      public boolean stop(String appName) {
        return false;
      }
    };
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
    when(sum.isActive()).thenReturn(false);
    when(sum.getAppType()).thenReturn(PSApplicationType.USER);
    when(sum.getVersion()).thenReturn(null);
    return sum;
  }

  @Test
  void getPipelineIr_returnsNativeIrWhenPresent() throws Exception {
    PSApplicationSummary sum = summary(7, "sys_cmpDocuments", "docs", true, "r", false, false);
    IPSPipelineIrService ir = mock(IPSPipelineIrService.class);
    PipelineIrDocument doc = new PipelineIrDocument();
    doc.getApp().setName("sys_cmpDocuments");
    when(ir.load("sys_cmpDocuments")).thenReturn(Optional.of(doc));
    AtomicBoolean classicLoaded = new AtomicBoolean(false);

    PipelinesAdaptor adaptor =
        new PipelinesAdaptor(
            tok -> new PSApplicationSummary[] {sum},
            () -> mock(IPSPipelineRuntimeService.class),
            () -> ir,
            (name, tok) -> {
              classicLoaded.set(true);
              return mock(PSApplication.class);
            });

    try (MockedStatic<PSSecurityFilter> security = mockStatic(PSSecurityFilter.class)) {
      stubCurrentRequest(security);
      PipelineIrDocument out =
          adaptor.getPipelineIr(URI.create("http://localhost/services/"), "sys_cmpDocuments");
      assertEquals("sys_cmpDocuments", out.getApp().getName());
      assertFalse(classicLoaded.get(), "classic import must not run when native IR exists");
      verify(ir).load("sys_cmpDocuments");
      verify(ir, never()).importClassicApplication(any());
    }
  }

  @Test
  void getPipelineIr_fallsBackToClassicImport() throws Exception {
    PSApplicationSummary sum = summary(7, "sys_cmpDocuments", "docs", true, "r", false, false);
    IPSPipelineIrService ir = mock(IPSPipelineIrService.class);
    when(ir.load("sys_cmpDocuments")).thenReturn(Optional.empty());
    PSApplication app = mock(PSApplication.class);
    PipelineIrDocument imported = new PipelineIrDocument();
    imported.setSource(PipelineIrDocument.SOURCE_CLASSIC_IMPORT);
    imported.getApp().setName("sys_cmpDocuments");
    when(ir.importClassicApplication(app)).thenReturn(imported);

    PipelinesAdaptor adaptor =
        new PipelinesAdaptor(
            tok -> new PSApplicationSummary[] {sum},
            () -> mock(IPSPipelineRuntimeService.class),
            () -> ir,
            (name, tok) -> {
              assertEquals("sys_cmpDocuments", name);
              return app;
            });

    try (MockedStatic<PSSecurityFilter> security = mockStatic(PSSecurityFilter.class)) {
      stubCurrentRequest(security);
      PipelineIrDocument out =
          adaptor.getPipelineIr(URI.create("http://localhost/services/"), "7");
      assertEquals(PipelineIrDocument.SOURCE_CLASSIC_IMPORT, out.getSource());
      verify(ir).importClassicApplication(app);
      verify(ir, never()).save(any());
    }
  }

  @Test
  void getPipelineIr_unknownOrUnsafeNameReturnsNull() {
    PSApplicationSummary sum = summary(7, "sys_cmpDocuments", "docs", true, "r", false, false);
    IPSPipelineIrService ir = mock(IPSPipelineIrService.class);
    PipelinesAdaptor adaptor =
        new PipelinesAdaptor(
            tok -> new PSApplicationSummary[] {sum},
            () -> mock(IPSPipelineRuntimeService.class),
            () -> ir,
            (name, tok) -> mock(PSApplication.class));

    try (MockedStatic<PSSecurityFilter> security = mockStatic(PSSecurityFilter.class)) {
      stubCurrentRequest(security);
      assertNull(adaptor.getPipelineIr(URI.create("http://localhost/"), "missing"));
      assertNull(adaptor.getPipelineIr(URI.create("http://localhost/"), "../evil"));
      assertNull(adaptor.getPipelineIr(URI.create("http://localhost/"), ""));
    }
  }

  @Test
  void getPipelineIr_mapsImportFailureTo400WithoutEcho() throws Exception {
    PSApplicationSummary sum = summary(7, "sys_cmpDocuments", "docs", true, "r", false, false);
    IPSPipelineIrService ir = mock(IPSPipelineIrService.class);
    when(ir.load("sys_cmpDocuments")).thenReturn(Optional.empty());
    when(ir.importClassicApplication(any()))
        .thenThrow(new PSPipelineIrException("Classic import failed: unsupported pipe"));

    PipelinesAdaptor adaptor =
        new PipelinesAdaptor(
            tok -> new PSApplicationSummary[] {sum},
            () -> mock(IPSPipelineRuntimeService.class),
            () -> ir,
            (name, tok) -> mock(PSApplication.class));

    try (MockedStatic<PSSecurityFilter> security = mockStatic(PSSecurityFilter.class)) {
      stubCurrentRequest(security);
      WebApplicationException ex =
          assertThrows(
              WebApplicationException.class,
              () ->
                  adaptor.getPipelineIr(
                      URI.create("http://localhost/"), "sys_cmpDocuments"));
      assertEquals(400, ex.getResponse().getStatus());
      assertTrue(ex.getMessage().contains("Classic import failed"));
      assertFalse(ex.getMessage().contains("../"));
    }
  }

}
