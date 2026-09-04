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

package com.percussion.rest.applicationfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.WebApplicationException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("UnitTest")
public class ApplicationFilesResourceTest {

  private IApplicationFileAdaptor adaptor;
  private ApplicationFilesResource resource;

  @BeforeEach
  public void setUp() {
    adaptor = mock(IApplicationFileAdaptor.class);
    resource = new ApplicationFilesResource(adaptor);
  }

  @Test
  public void listFilesDelegates() {
    ApplicationFileSummary s = new ApplicationFileSummary();
    s.setPath("ApplicationFiles/style.css");
    when(adaptor.listFiles(eq("sys_resources"))).thenReturn(List.of(s));

    List<ApplicationFileSummary> out = resource.listFiles("sys_resources");
    assertEquals(1, out.size());
    assertEquals("ApplicationFiles/style.css", out.get(0).getPath());
    verify(adaptor).listFiles("sys_resources");
  }

  @Test
  public void listFilesUnknownAppIsGeneric404() {
    when(adaptor.listFiles(eq("missing"))).thenReturn(null);
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.listFiles("missing"));
    assertEquals(404, ex.getResponse().getStatus());
    assertEquals("Application not found", ex.getMessage());
  }

  @Test
  public void listFilesWrapsUnexpectedAs500() {
    IllegalStateException boom = new IllegalStateException("boom");
    when(adaptor.listFiles(eq("sys_resources"))).thenThrow(boom);
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.listFiles("sys_resources"));
    assertEquals(500, ex.getResponse().getStatus());
    assertSame(boom, ex.getCause());
  }

  @Test
  public void getFileDelegates() {
    ApplicationFileSummary s = new ApplicationFileSummary();
    s.setPath("ApplicationFiles/a.txt");
    s.setContent("hello");
    when(adaptor.getFile(eq("sys_resources"), eq("ApplicationFiles/a.txt"))).thenReturn(s);

    ApplicationFileSummary out = resource.getFile("sys_resources", "ApplicationFiles/a.txt");
    assertEquals("hello", out.getContent());
    verify(adaptor).getFile("sys_resources", "ApplicationFiles/a.txt");
  }

  @Test
  public void getFileNotFoundIsGeneric404() {
    when(adaptor.getFile(eq("sys_resources"), eq("../escape"))).thenReturn(null);
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> resource.getFile("sys_resources", "../escape"));
    assertEquals(404, ex.getResponse().getStatus());
    assertEquals("Application file not found", ex.getMessage());
  }

  @Test
  public void putFileDelegates() {
    ApplicationFileSummary body = new ApplicationFileSummary();
    body.setContent("updated");
    ApplicationFileSummary saved = new ApplicationFileSummary();
    saved.setPath("ApplicationFiles/a.txt");
    saved.setContent("updated");
    when(adaptor.putFile(eq("sys_resources"), eq("ApplicationFiles/a.txt"), eq(body)))
        .thenReturn(saved);

    ApplicationFileSummary out = resource.putFile("sys_resources", "ApplicationFiles/a.txt", body);
    assertEquals("updated", out.getContent());
    verify(adaptor).putFile("sys_resources", "ApplicationFiles/a.txt", body);
  }

  @Test
  public void putFileNullBodyIs400() {
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> resource.putFile("sys_resources", "ApplicationFiles/a.txt", null));
    assertEquals(400, ex.getResponse().getStatus());
    verify(adaptor, never()).putFile(eq("sys_resources"), eq("ApplicationFiles/a.txt"), isNull());
  }

  @Test
  public void putFileNullPathIs400() {
    ApplicationFileSummary body = new ApplicationFileSummary();
    body.setContent("x");
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> resource.putFile("sys_resources", null, body));
    assertEquals(400, ex.getResponse().getStatus());
    assertEquals("path is required", ex.getMessage());
    verify(adaptor, never()).putFile(eq("sys_resources"), isNull(), eq(body));
  }

  @Test
  public void putFileBlankPathIs400() {
    ApplicationFileSummary body = new ApplicationFileSummary();
    body.setContent("x");
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> resource.putFile("sys_resources", "  ", body));
    assertEquals(400, ex.getResponse().getStatus());
    assertEquals("path is required", ex.getMessage());
    verify(adaptor, never()).putFile(eq("sys_resources"), eq("  "), eq(body));
  }

  @Test
  public void putFileUnknownIsGeneric404() {
    ApplicationFileSummary body = new ApplicationFileSummary();
    body.setContent("x");
    when(adaptor.putFile(eq("sys_resources"), eq("nope.txt"), eq(body))).thenReturn(null);
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> resource.putFile("sys_resources", "nope.txt", body));
    assertEquals(404, ex.getResponse().getStatus());
    assertEquals("Application file not found", ex.getMessage());
  }

  @Test
  public void putFileRethrowsAdaptor403() {
    ApplicationFileSummary body = new ApplicationFileSummary();
    body.setContent("x");
    WebApplicationException mapped = new WebApplicationException("Admin role required", 403);
    when(adaptor.putFile(eq("sys_resources"), eq("a.txt"), eq(body))).thenThrow(mapped);

    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> resource.putFile("sys_resources", "a.txt", body));
    assertSame(mapped, ex);
    assertEquals(403, ex.getResponse().getStatus());
  }

  @Test
  public void missingAdaptorReturnsServiceUnavailableOnList() {
    ApplicationFilesResource bare = new ApplicationFilesResource();
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> bare.listFiles("any"));
    assertEquals(503, ex.getResponse().getStatus());
  }

  @Test
  public void missingAdaptorReturnsServiceUnavailableOnGet() {
    ApplicationFilesResource bare = new ApplicationFilesResource();
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> bare.getFile("any", "a.txt"));
    assertEquals(503, ex.getResponse().getStatus());
  }

  @Test
  public void mapWriteFailurePreservesWebApplicationException() {
    WebApplicationException mapped = new WebApplicationException("x", 403);
    assertSame(mapped, ApplicationFilesResource.mapWriteFailure(mapped));
  }

  @Test
  public void mapWriteFailureMapsIllegalArgumentTo400() {
    WebApplicationException ex =
        ApplicationFilesResource.mapWriteFailure(new IllegalArgumentException("bad"));
    assertEquals(400, ex.getResponse().getStatus());
    assertEquals("bad", ex.getMessage());
  }
}
