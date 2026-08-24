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

package com.percussion.rest.translations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.UriInfo;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * HTTP-layer tests for {@link ContentTranslationsResource} (#2429). Domain behaviour is covered by
 * sitemanage apibridge unit tests.
 */
@ExtendWith(MockitoExtension.class)
@Tag("UnitTest")
class ContentTranslationsResourceTest {

  @Mock private IContentTranslationsAdaptor adaptor;

  @Mock private UriInfo uriInfo;

  private ContentTranslationsResource resource;

  @BeforeEach
  void init() {
    resource = new ContentTranslationsResource(adaptor);
    resource.setUriInfo(uriInfo);
    org.mockito.Mockito.lenient()
        .when(uriInfo.getBaseUri())
        .thenReturn(UriBuilder.fromUri("http://localhost/rest").build());
  }

  @Test
  void createDelegatesToAdaptor() {
    CreateTranslationsRequest body = new CreateTranslationsRequest();
    body.setItemIds(List.of(100L));
    body.setLocales(List.of("fr-fr"));

    TranslationVariant created = new TranslationVariant();
    created.setContentId(200L);
    created.setLocale("fr-fr");
    created.setRole("translation");
    CreateTranslationsResult expected = new CreateTranslationsResult(List.of(created));
    when(adaptor.createTranslations(any(), eq(body))).thenReturn(expected);

    CreateTranslationsResult out = resource.createTranslations(body);

    assertEquals(1, out.getCreated().size());
    assertEquals(200L, out.getCreated().get(0).getContentId());
    verify(adaptor).createTranslations(any(), eq(body));
  }

  @Test
  void createNullBodyIs400() {
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.createTranslations(null));
    assertEquals(400, ex.getResponse().getStatus());
  }

  @Test
  void createIllegalArgumentMapsTo400() {
    CreateTranslationsRequest body = new CreateTranslationsRequest();
    when(adaptor.createTranslations(any(), any()))
        .thenThrow(new IllegalArgumentException("itemIds is required"));

    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.createTranslations(body));
    assertEquals(400, ex.getResponse().getStatus());
    assertEquals("itemIds is required", String.valueOf(ex.getResponse().getEntity()));
  }

  @Test
  void createSecurityMapsTo403() {
    CreateTranslationsRequest body = new CreateTranslationsRequest();
    body.setItemIds(List.of(1L));
    when(adaptor.createTranslations(any(), any()))
        .thenThrow(new SecurityException("not allowed"));

    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.createTranslations(body));
    assertEquals(403, ex.getResponse().getStatus());
  }

  @Test
  void listDelegatesToAdaptor() {
    ItemTranslationVariants expected = new ItemTranslationVariants();
    expected.setItemId(100L);
    expected.setLocale("en-us");
    when(adaptor.listItemVariants(any(), eq("100"))).thenReturn(expected);

    ItemTranslationVariants out = resource.listItemVariants("100");

    assertEquals(100L, out.getItemId());
    assertEquals("en-us", out.getLocale());
    verify(adaptor).listItemVariants(any(), eq("100"));
  }

  @Test
  void listPassesHyphenatedGuidToAdaptor() {
    ItemTranslationVariants expected = new ItemTranslationVariants();
    expected.setItemId(551L);
    expected.setLocale("en-us");
    when(adaptor.listItemVariants(any(), eq("16777215-101-551"))).thenReturn(expected);

    ItemTranslationVariants out = resource.listItemVariants("16777215-101-551");

    assertEquals(551L, out.getItemId());
    assertEquals("en-us", out.getLocale());
    verify(adaptor).listItemVariants(any(), eq("16777215-101-551"));
  }

  @Test
  void listNullFromAdaptorIs404() {
    when(adaptor.listItemVariants(any(), eq("missing"))).thenReturn(null);

    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.listItemVariants("missing"));
    assertEquals(404, ex.getResponse().getStatus());
  }

  @Test
  void listSecurityMapsTo403() {
    when(adaptor.listItemVariants(any(), eq("private")))
        .thenThrow(new SecurityException("denied"));

    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.listItemVariants("private"));
    assertEquals(403, ex.getResponse().getStatus());
  }

  @Test
  void missingAdaptorIs503() {
    ContentTranslationsResource bare = new ContentTranslationsResource();
    bare.setUriInfo(uriInfo);

    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> bare.createTranslations(new CreateTranslationsRequest()));
    assertEquals(503, ex.getResponse().getStatus());
  }
}
