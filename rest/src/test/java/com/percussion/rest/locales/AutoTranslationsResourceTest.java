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
public class AutoTranslationsResourceTest {

  private IAutoTranslationsAdaptor adaptor;
  private AutoTranslationsResource resource;
  private Logger previousLog;
  private Logger mockLog;

  @BeforeEach
  public void setUp() {
    previousLog = AutoTranslationsResource.log;
    mockLog = mock(Logger.class);
    AutoTranslationsResource.log = mockLog;

    adaptor = mock(IAutoTranslationsAdaptor.class);
    resource = new AutoTranslationsResource(adaptor);
    UriInfo uriInfo = mock(UriInfo.class);
    when(uriInfo.getBaseUri()).thenReturn(URI.create("http://localhost/services/"));
    resource.setUriInfo(uriInfo);
  }

  @AfterEach
  public void restoreLog() {
    AutoTranslationsResource.log = previousLog;
  }

  @Test
  public void getAutoTranslationsSuccess() {
    AutoTranslationRow row = new AutoTranslationRow();
    row.setLocale("fr-fr");
    row.setContentTypeName("percPage");
    when(adaptor.getAutoTranslations(any())).thenReturn(List.of(row));

    List<AutoTranslationRow> out = resource.getAutoTranslations();
    assertEquals(1, out.size());
    assertEquals("fr-fr", out.get(0).getLocale());
    verify(adaptor).getAutoTranslations(any());
    verify(mockLog, never()).error(any(String.class), any(), any(), any());
  }

  @Test
  public void getAutoTranslationsEmpty() {
    when(adaptor.getAutoTranslations(any())).thenReturn(List.of());
    assertTrue(resource.getAutoTranslations().isEmpty());
  }

  @Test
  public void getAutoTranslationsForbiddenWhenNotAdmin() {
    when(adaptor.getAutoTranslations(any()))
        .thenThrow(new WebApplicationException("Admin role required", 403));
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.getAutoTranslations());
    assertEquals(403, ex.getResponse().getStatus());
    verify(mockLog, never()).error(any(String.class), any(), any(), any());
  }

  @Test
  public void getAutoTranslationsWrapsUnexpectedFailuresAs500() {
    IllegalStateException boom = new IllegalStateException("cms down");
    when(adaptor.getAutoTranslations(any())).thenThrow(boom);

    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.getAutoTranslations());
    assertEquals(500, ex.getResponse().getStatus());
    assertSame(boom, ex.getCause());
    verify(mockLog)
        .error(
            eq("Failed to load auto-translations ({}): {}"),
            eq(IllegalStateException.class.getName()),
            eq("cms down"),
            same(boom));
  }

  @Test
  public void missingAdaptorReturnsServiceUnavailableOnGet() {
    AutoTranslationsResource bare = newAutoTranslationsResourceWithoutAdaptor();
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, bare::getAutoTranslations);
    assertEquals(503, ex.getResponse().getStatus());
    verify(mockLog, never()).error(any(String.class), any(), any(), any());
  }

  @Test
  public void saveAutoTranslationsSuccess() {
    AutoTranslationRow body = new AutoTranslationRow();
    body.setLocale("fr-fr");
    body.setContentTypeName("percPage");
    when(adaptor.saveAutoTranslations(any(), any())).thenReturn(List.of(body));

    List<AutoTranslationRow> out = resource.saveAutoTranslations(List.of(body));
    assertEquals(1, out.size());
    assertEquals("fr-fr", out.get(0).getLocale());
    verify(adaptor).saveAutoTranslations(any(), eq(List.of(body)));
  }

  @Test
  public void saveAutoTranslationsEmptyClears() {
    when(adaptor.saveAutoTranslations(any(), eq(List.of()))).thenReturn(List.of());
    assertTrue(resource.saveAutoTranslations(List.of()).isEmpty());
    verify(adaptor).saveAutoTranslations(any(), eq(List.of()));
  }

  @Test
  public void saveAutoTranslationsUnknownLocaleIs400() {
    when(adaptor.saveAutoTranslations(any(), any()))
        .thenThrow(new IllegalArgumentException("unknown locale: xx-xx"));
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> resource.saveAutoTranslations(List.of(new AutoTranslationRow())));
    assertEquals(400, ex.getResponse().getStatus());
  }

  @Test
  public void saveAutoTranslationsUnknownTypeIs400() {
    when(adaptor.saveAutoTranslations(any(), any()))
        .thenThrow(new IllegalArgumentException("unknown content type: missing"));
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> resource.saveAutoTranslations(List.of(new AutoTranslationRow())));
    assertEquals(400, ex.getResponse().getStatus());
  }

  @Test
  public void saveAutoTranslationsLockConflictIs409() {
    when(adaptor.saveAutoTranslations(any(), any()))
        .thenThrow(
            new AutoTranslationDesignLockException(
                "Could not save auto-translations; locked by other"));
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> resource.saveAutoTranslations(List.of()));
    assertEquals(409, ex.getResponse().getStatus());
  }

  @Test
  public void saveAutoTranslationsForbiddenWhenNotAdmin() {
    when(adaptor.saveAutoTranslations(any(), any()))
        .thenThrow(new WebApplicationException("Admin role required", 403));
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> resource.saveAutoTranslations(List.of()));
    assertEquals(403, ex.getResponse().getStatus());
  }

  @Test
  public void saveAutoTranslationsWrapsUnexpectedFailures() {
    IllegalStateException boom = new IllegalStateException("design ws down");
    when(adaptor.saveAutoTranslations(any(), any())).thenThrow(boom);
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> resource.saveAutoTranslations(List.of()));
    assertEquals(500, ex.getResponse().getStatus());
    assertSame(boom, ex.getCause());
  }

  @Test
  public void missingAdaptorReturnsServiceUnavailableOnSave() {
    AutoTranslationsResource bare = newAutoTranslationsResourceWithoutAdaptor();
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> bare.saveAutoTranslations(List.of()));
    assertEquals(503, ex.getResponse().getStatus());
  }

  @Test
  public void mapWriteFailureLockMessageOnIllegalStateIs409() {
    WebApplicationException ex =
        AutoTranslationsResource.mapWriteFailure(
            new IllegalStateException("object is not locked"));
    assertEquals(409, ex.getResponse().getStatus());
  }

  private static AutoTranslationsResource newAutoTranslationsResourceWithoutAdaptor() {
    AutoTranslationsResource bare = new AutoTranslationsResource();
    UriInfo uriInfo = mock(UriInfo.class);
    when(uriInfo.getBaseUri()).thenReturn(URI.create("http://localhost/services/"));
    bare.setUriInfo(uriInfo);
    return bare;
  }
}
