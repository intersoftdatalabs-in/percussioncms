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

package com.percussion.rest.communities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.WebApplicationException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("UnitTest")
public class CommunityNewSearchDefaultsResourceTest {

  private ICommunityNewSearchDefaultsAdaptor adaptor;
  private CommunityNewSearchDefaultsResource resource;

  @BeforeEach
  public void setUp() {
    adaptor = mock(ICommunityNewSearchDefaultsAdaptor.class);
    resource = new CommunityNewSearchDefaultsResource(adaptor);
  }

  @Test
  public void getDefaultsEmptyIs200() {
    CommunityNewSearchDefaults empty = new CommunityNewSearchDefaults();
    empty.setCommunityName("Default");
    empty.setSearches(List.of());
    when(adaptor.getDefaults(eq("Default"))).thenReturn(empty);

    CommunityNewSearchDefaults out = resource.getDefaults("Default");
    assertEquals("Default", out.getCommunityName());
    assertTrue(out.getSearches().isEmpty());
  }

  @Test
  public void getDefaultsUnknownCommunityIs404() {
    when(adaptor.getDefaults(eq("missing"))).thenReturn(null);
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.getDefaults("missing"));
    assertEquals(404, ex.getResponse().getStatus());
  }

  @Test
  public void getDefaultsRethrowsForbidden() {
    WebApplicationException forbidden = new WebApplicationException("Admin role required", 403);
    when(adaptor.getDefaults(eq("Default"))).thenThrow(forbidden);
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.getDefaults("Default"));
    assertEquals(403, ex.getResponse().getStatus());
    assertSame(forbidden, ex);
  }

  @Test
  public void missingAdaptorIs503OnGet() {
    CommunityNewSearchDefaultsResource bare = new CommunityNewSearchDefaultsResource();
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> bare.getDefaults("Default"));
    assertEquals(503, ex.getResponse().getStatus());
  }

  @Test
  public void replaceDefaultsSuccess() {
    CommunityNewSearchDefaults body = new CommunityNewSearchDefaults();
    CommunityNewSearchRef ref = new CommunityNewSearchRef();
    ref.setName("SimpleSearch");
    body.setSearches(List.of(ref));
    CommunityNewSearchDefaults stored = new CommunityNewSearchDefaults();
    stored.setCommunityName("Default");
    stored.setSearches(List.of(ref));
    when(adaptor.replaceDefaults(eq("Default"), eq(body))).thenReturn(stored);

    CommunityNewSearchDefaults out = resource.replaceDefaults("Default", body);
    assertEquals(1, out.getSearches().size());
    assertEquals("SimpleSearch", out.getSearches().get(0).getName());
  }

  @Test
  public void replaceDefaultsNullBodyIs400() {
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> resource.replaceDefaults("Default", null));
    assertEquals(400, ex.getResponse().getStatus());
  }

  @Test
  public void replaceDefaultsUnknownSearchIs400() {
    when(adaptor.replaceDefaults(eq("Default"), org.mockito.ArgumentMatchers.any()))
        .thenThrow(new IllegalArgumentException("Unknown search: Nope"));
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> resource.replaceDefaults("Default", new CommunityNewSearchDefaults()));
    assertEquals(400, ex.getResponse().getStatus());
    assertTrue(ex.getMessage().contains("Unknown search"));
  }

  @Test
  public void replaceDefaultsUnknownCommunityIs404() {
    when(adaptor.replaceDefaults(eq("missing"), org.mockito.ArgumentMatchers.any()))
        .thenReturn(null);
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> resource.replaceDefaults("missing", new CommunityNewSearchDefaults()));
    assertEquals(404, ex.getResponse().getStatus());
  }

  @Test
  public void replaceDefaultsRethrowsForbidden() {
    WebApplicationException forbidden = new WebApplicationException("Admin role required", 403);
    when(adaptor.replaceDefaults(eq("Default"), org.mockito.ArgumentMatchers.any()))
        .thenThrow(forbidden);
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> resource.replaceDefaults("Default", new CommunityNewSearchDefaults()));
    assertEquals(403, ex.getResponse().getStatus());
  }

  @Test
  public void missingAdaptorIs503OnPut() {
    CommunityNewSearchDefaultsResource bare = new CommunityNewSearchDefaultsResource();
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> bare.replaceDefaults("Default", new CommunityNewSearchDefaults()));
    assertEquals(503, ex.getResponse().getStatus());
  }
}
