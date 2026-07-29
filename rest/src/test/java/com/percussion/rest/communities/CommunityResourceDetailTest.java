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

package com.percussion.rest.communities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.WebApplicationException;
import java.lang.reflect.Field;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("UnitTest")
public class CommunityResourceDetailTest {

  private ICommunityAdaptor adaptor;
  private CommunityResource resource;

  @BeforeEach
  public void setUp() throws Exception {
    adaptor = mock(ICommunityAdaptor.class);
    resource = new CommunityResource();
    Field f = CommunityResource.class.getDeclaredField("adaptor");
    f.setAccessible(true);
    f.set(resource, adaptor);
  }

  @Test
  public void getCommunityByName() {
    Community c = new Community();
    c.setName("Default");
    c.setLabel("Default");
    when(adaptor.getCommunity(eq("Default"))).thenReturn(c);
    assertEquals("Default", resource.getCommunity("Default").getName().orElse(null));
  }

  @Test
  public void getCommunityByNumericId() {
    Community c = new Community();
    c.setName("ById");
    when(adaptor.getCommunity(eq("10"))).thenReturn(c);
    assertEquals("ById", resource.getCommunity("10").getName().orElse(null));
  }

  @Test
  public void getCommunityByGuidString() {
    Community c = new Community();
    c.setName("ByGuid");
    when(adaptor.getCommunity(eq("0-13-10"))).thenReturn(c);
    assertEquals("ByGuid", resource.getCommunity("0-13-10").getName().orElse(null));
  }

  @Test
  public void getCommunityNotFound() {
    when(adaptor.getCommunity(eq("missing"))).thenReturn(null);
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.getCommunity("missing"));
    assertEquals(404, ex.getResponse().getStatus());
  }

  @Test
  public void getCommunityServerError() {
    when(adaptor.getCommunity(eq("boom"))).thenThrow(new RuntimeException("db down"));
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.getCommunity("boom"));
    assertEquals(500, ex.getResponse().getStatus());
  }
}
