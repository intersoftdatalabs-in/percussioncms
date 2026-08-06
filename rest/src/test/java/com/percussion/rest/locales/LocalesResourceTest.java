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

package com.percussion.rest.locales;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.List;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("UnitTest")
public class LocalesResourceTest {

  private ILocalesAdaptor adaptor;
  private LocalesResource resource;
  private Logger previousLog;
  private Logger mockLog;

  @BeforeEach
  public void setUp() {
    previousLog = LocalesResource.log;
    mockLog = mock(Logger.class);
    LocalesResource.log = mockLog;

    adaptor = mock(ILocalesAdaptor.class);
    resource = new LocalesResource(adaptor);
    UriInfo uriInfo = mock(UriInfo.class);
    when(uriInfo.getBaseUri()).thenReturn(URI.create("http://localhost/services/"));
    resource.setUriInfo(uriInfo);
  }

  @AfterEach
  public void restoreLog() {
    LocalesResource.log = previousLog;
  }

  @Test
  public void listLocalesSuccess() {
    LocaleSummary s = new LocaleSummary();
    s.setLanguageString("en-us");
    s.setBaseLocale(false);
    s.setHasFormatProfile(true);
    when(adaptor.listLocales(any())).thenReturn(List.of(s));

    List<LocaleSummary> out = resource.listLocales();
    assertEquals(1, out.size());
    assertEquals("en-us", out.get(0).getLanguageString());
    assertEquals(Boolean.TRUE, out.get(0).getHasFormatProfile());
    verify(adaptor).listLocales(any());
    verify(mockLog, never()).error(any(String.class), any(), any(), any());
  }

  @Test
  public void listLocalesEmpty() {
    when(adaptor.listLocales(any())).thenReturn(List.of());
    assertTrue(resource.listLocales().isEmpty());
  }

  @Test
  public void listLocalesWrapsUnexpectedFailuresAs500() {
    IllegalStateException boom = new IllegalStateException("boom");
    when(adaptor.listLocales(any())).thenThrow(boom);

    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.listLocales());
    assertEquals(500, ex.getResponse().getStatus());
    assertSame(boom, ex.getCause());
    verify(mockLog)
        .error(
            eq("Failed to list locales ({}): {}"),
            eq(IllegalStateException.class.getName()),
            eq("boom"),
            same(boom));
  }

  @Test
  public void missingAdaptorReturnsServiceUnavailableOnList() {
    LocalesResource bare = newLocalesResourceWithoutAdaptor();
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, bare::listLocales);
    assertEquals(503, ex.getResponse().getStatus());
    // Misconfiguration path must not log as unexpected failure
    verify(mockLog, never()).error(any(String.class), any(), any(), any());
  }

  @Test
  public void missingAdaptorReturnsServiceUnavailableOnGet() {
    // getLocale must rethrow WebApplicationException from requireAdaptor (not re-wrap as 500)
    LocalesResource bare = newLocalesResourceWithoutAdaptor();
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> bare.getLocale("any"));
    assertEquals(503, ex.getResponse().getStatus());
    verify(mockLog, never()).error(any(String.class), any(), any(), any());
  }

  @Test
  public void getLocaleRethrowsWebApplicationException() {
    WebApplicationException mapped =
        new WebApplicationException("from adaptor", 404);
    when(adaptor.getLocale(any(), eq("xx"))).thenThrow(mapped);

    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.getLocale("xx"));
    assertSame(mapped, ex);
    assertEquals(404, ex.getResponse().getStatus());
    verify(mockLog, never()).error(any(String.class), any(), any(), any());
  }

  @Test
  public void getLocaleDelegatesToAdaptor() {
    LocaleDetail d = new LocaleDetail();
    d.setLanguageString("en-us");
    when(adaptor.getLocale(any(), eq("en-us"))).thenReturn(d);

    assertEquals("en-us", resource.getLocale("en-us").getLanguageString());
    verify(adaptor).getLocale(any(), eq("en-us"));
  }

  @Test
  public void getLocaleNotFoundIsGeneric404() {
    when(adaptor.getLocale(any(), eq("xx"))).thenReturn(null);

    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.getLocale("xx"));
    assertEquals(404, ex.getResponse().getStatus());
    assertEquals("Locale not found", ex.getMessage());
    verify(mockLog, never()).error(any(String.class), any(), any(), any());
  }

  @Test
  public void getLocaleWrapsUnexpectedFailuresAs500() {
    IllegalStateException boom = new IllegalStateException("cms down");
    when(adaptor.getLocale(any(), eq("en-us"))).thenThrow(boom);

    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.getLocale("en-us"));
    assertEquals(500, ex.getResponse().getStatus());
    assertSame(boom, ex.getCause());
    verify(mockLog)
        .error(
            eq("Failed to load locale {} ({}): {}"),
            eq("en-us"),
            eq(IllegalStateException.class.getName()),
            eq("cms down"),
            same(boom));
  }

  private static LocalesResource newLocalesResourceWithoutAdaptor() {
    LocalesResource bare = new LocalesResource();
    UriInfo uriInfo = mock(UriInfo.class);
    when(uriInfo.getBaseUri()).thenReturn(URI.create("http://localhost/services/"));
    bare.setUriInfo(uriInfo);
    return bare;
  }
}
