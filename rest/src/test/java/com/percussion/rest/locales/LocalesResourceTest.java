/*
 * Copyright 1999-2026 Percussion Software, Inc.
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
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.UriInfo;
import java.lang.reflect.Field;
import java.net.URI;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("UnitTest")
public class LocalesResourceTest {

  private ILocalesAdaptor adaptor;
  private LocalesResource resource;

  @BeforeEach
  public void setUp() throws Exception {
    adaptor = mock(ILocalesAdaptor.class);
    resource = new LocalesResource(adaptor);
    UriInfo uriInfo = mock(UriInfo.class);
    when(uriInfo.getBaseUri()).thenReturn(URI.create("http://localhost/services/"));
    Field f = LocalesResource.class.getDeclaredField("uriInfo");
    f.setAccessible(true);
    f.set(resource, uriInfo);
  }

  @Test
  public void listLocalesDelegatesToAdaptor() {
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
  }

  @Test
  public void listLocalesWrapsFailures() {
    IllegalStateException boom = new IllegalStateException("boom");
    when(adaptor.listLocales(any())).thenThrow(boom);

    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.listLocales());
    assertEquals(500, ex.getResponse().getStatus());
    assertSame(boom, ex.getCause());
  }

  @Test
  public void listLocalesWithoutInjectionFailsWithDiagnostic() {
    LocalesResource bare = new LocalesResource();
    WebApplicationException ex = assertThrows(WebApplicationException.class, bare::listLocales);
    assertEquals(500, ex.getResponse().getStatus());
    assertInstanceOf(IllegalStateException.class, ex.getCause());
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
  }

  @Test
  public void getLocaleWrapsUnexpectedFailuresAs500() {
    IllegalStateException boom = new IllegalStateException("cms down");
    when(adaptor.getLocale(any(), eq("en-us"))).thenThrow(boom);

    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.getLocale("en-us"));
    assertEquals(500, ex.getResponse().getStatus());
    assertSame(boom, ex.getCause());
  }
}
