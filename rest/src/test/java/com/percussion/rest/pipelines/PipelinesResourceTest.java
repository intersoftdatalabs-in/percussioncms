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

package com.percussion.rest.pipelines;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.percussion.services.pipeline.model.PipelineExecuteRequest;
import com.percussion.services.pipeline.model.PipelineExecuteResult;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.UriInfo;
import java.lang.reflect.Field;
import java.net.URI;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("UnitTest")
public class PipelinesResourceTest {

  private IPipelinesAdaptor adaptor;
  private PipelinesResource resource;

  @BeforeEach
  public void setUp() throws Exception {
    adaptor = mock(IPipelinesAdaptor.class);
    resource = new PipelinesResource(adaptor);
    UriInfo uriInfo = mock(UriInfo.class);
    when(uriInfo.getBaseUri()).thenReturn(URI.create("http://localhost/services/"));
    Field f = PipelinesResource.class.getDeclaredField("uriInfo");
    f.setAccessible(true);
    f.set(resource, uriInfo);
  }

  @Test
  public void listApplicationsDelegatesToAdaptor() {
    ApplicationSummary a = new ApplicationSummary();
    a.setName("sys_foo");
    when(adaptor.listApplications(any(), isNull(), eq(500), eq(0))).thenReturn(List.of(a));

    List<ApplicationSummary> out = resource.listApplications(null, 500, 0);

    assertEquals(1, out.size());
    assertEquals("sys_foo", out.get(0).getName());
    verify(adaptor).listApplications(any(), isNull(), eq(500), eq(0));
  }

  @Test
  public void listApplicationsPassesNameAndPaging() {
    when(adaptor.listApplications(any(), eq("doc"), eq(10), eq(5))).thenReturn(List.of());

    resource.listApplications("doc", 10, 5);

    verify(adaptor).listApplications(any(), eq("doc"), eq(10), eq(5));
  }

  @Test
  public void listApplicationsWrapsFailures() {
    IllegalStateException boom = new IllegalStateException("boom");
    when(adaptor.listApplications(any(), any(), anyInt(), anyInt())).thenThrow(boom);

    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.listApplications(null, 500, 0));
    assertEquals(500, ex.getResponse().getStatus());
    assertSame(boom, ex.getCause(), "cause chain must preserve the original failure");
  }

  @Test
  public void listApplicationsWithoutInjectionFailsWithDiagnostic() {
    PipelinesResource bare = new PipelinesResource();
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> bare.listApplications(null, 500, 0));
    assertEquals(500, ex.getResponse().getStatus());
    assertInstanceOf(IllegalStateException.class, ex.getCause());
    assertEquals(
        "Pipelines adaptor not configured (resource constructed without injection)",
        ex.getCause().getMessage());
  }

  @Test
  public void getApplicationDelegatesToAdaptor() {
    ApplicationDetail d = new ApplicationDetail();
    d.setName("sys_foo");
    when(adaptor.getApplication(any(), eq("sys_foo"))).thenReturn(d);

    assertEquals("sys_foo", resource.getApplication("sys_foo").getName());
    verify(adaptor).getApplication(any(), eq("sys_foo"));
  }

  @Test
  public void getApplicationNotFound() {
    when(adaptor.getApplication(any(), eq("missing"))).thenReturn(null);

    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.getApplication("missing"));
    assertEquals(404, ex.getResponse().getStatus());
    // Do not echo raw path param (name probing / log injection)
    assertEquals("Application not found", ex.getMessage());
  }

  @Test
  public void getApplicationWrapsUnexpectedFailuresAs500() {
    IllegalStateException boom = new IllegalStateException("object store down");
    when(adaptor.getApplication(any(), eq("sys_foo"))).thenThrow(boom);

    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.getApplication("sys_foo"));
    assertEquals(500, ex.getResponse().getStatus());
    assertSame(boom, ex.getCause(), "cause chain must preserve the original failure");
  }

  @Test
  public void executeDelegatesToAdaptor() {
    PipelineExecuteRequest req = PipelineExecuteRequest.ofParams(Map.of("TYPE", "workflow"));
    PipelineExecuteResult expected = new PipelineExecuteResult();
    expected.setAppName("lookupApp");
    expected.setResourceName("DatasetQ");
    expected.setOperation("query");
    expected.setRows(List.of(Map.of("TYPE", "workflow", "NAME", "wf1")));
    when(adaptor.execute(any(), eq("lookupApp"), eq("DatasetQ"), eq(req))).thenReturn(expected);

    PipelineExecuteResult out = resource.execute("lookupApp", "DatasetQ", req);

    assertEquals("lookupApp", out.getAppName());
    assertEquals("DatasetQ", out.getResourceName());
    assertEquals("query", out.getOperation());
    assertEquals(1, out.getRowCount());
    verify(adaptor).execute(any(), eq("lookupApp"), eq("DatasetQ"), eq(req));
  }

  @Test
  public void executePassesNullBodyThrough() {
    PipelineExecuteResult expected = new PipelineExecuteResult();
    expected.setOperation("query");
    when(adaptor.execute(any(), eq("app"), eq("res"), isNull())).thenReturn(expected);

    assertEquals("query", resource.execute("app", "res", null).getOperation());
    verify(adaptor).execute(any(), eq("app"), eq("res"), isNull());
  }

  @Test
  public void executeRethrowsWebApplicationException() {
    when(adaptor.execute(any(), eq("missing"), eq("r"), any()))
        .thenThrow(new WebApplicationException("Pipeline application or resource not found", 404));

    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> resource.execute("missing", "r", PipelineExecuteRequest.empty()));
    assertEquals(404, ex.getResponse().getStatus());
    assertEquals("Pipeline application or resource not found", ex.getMessage());
  }

  @Test
  public void executeMapsIllegalArgumentTo400() {
    when(adaptor.execute(any(), eq("app"), eq("res"), any()))
        .thenThrow(new IllegalArgumentException("bad params"));

    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> resource.execute("app", "res", PipelineExecuteRequest.empty()));
    assertEquals(400, ex.getResponse().getStatus());
    assertEquals("bad params", ex.getMessage());
  }

  @Test
  public void executeWrapsUnexpectedFailuresAs500() {
    IllegalStateException boom = new IllegalStateException("runtime not configured");
    when(adaptor.execute(any(), eq("app"), eq("res"), any())).thenThrow(boom);

    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> resource.execute("app", "res", PipelineExecuteRequest.empty()));
    assertEquals(500, ex.getResponse().getStatus());
    assertSame(boom, ex.getCause());
  }

  @Test
  public void executeWithoutInjectionFailsWithDiagnostic() {
    PipelinesResource bare = new PipelinesResource();
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> bare.execute("app", "res", PipelineExecuteRequest.empty()));
    assertEquals(500, ex.getResponse().getStatus());
    assertInstanceOf(IllegalStateException.class, ex.getCause());
  }
}
