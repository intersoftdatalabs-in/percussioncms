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

package com.percussion.rest.slots;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.UriInfo;
import java.net.URI;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("UnitTest")
public class SlotsResourceDetailTest {

  private ISlotsAdaptor adaptor;
  private SlotsResource resource;

  @BeforeEach
  public void setUp() {
    adaptor = mock(ISlotsAdaptor.class);
    resource = new SlotsResource(adaptor);
    UriInfo uriInfo = mock(UriInfo.class);
    when(uriInfo.getBaseUri()).thenReturn(URI.create("http://localhost/services/"));
    resource.setUriInfo(uriInfo);
  }

  @Test
  public void getSlotReturnsDetailByName() {
    SlotDetail d = new SlotDetail();
    d.setName("target");
    when(adaptor.getSlot(any(), eq("target"))).thenReturn(d);
    assertEquals("target", resource.getSlot("target").getName());
  }

  @Test
  public void getSlotReturnsDetailByNumericId() {
    SlotDetail d = new SlotDetail();
    d.setName("numeric");
    when(adaptor.getSlot(any(), eq("42"))).thenReturn(d);
    assertEquals("numeric", resource.getSlot("42").getName());
  }

  @Test
  public void getSlotReturnsDetailByGuidShape() {
    SlotDetail d = new SlotDetail();
    d.setName("by-guid");
    when(adaptor.getSlot(any(), eq("0-5-42"))).thenReturn(d);
    assertEquals("by-guid", resource.getSlot("0-5-42").getName());
  }

  @Test
  public void getSlotNotFound() {
    when(adaptor.getSlot(any(), eq("missing"))).thenReturn(null);
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.getSlot("missing"));
    assertEquals(404, ex.getResponse().getStatus());
  }

  @Test
  public void getSlotBlankTreatedAsNotFound() {
    when(adaptor.getSlot(any(), isNull())).thenReturn(null);
    when(adaptor.getSlot(any(), eq("   "))).thenReturn(null);
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.getSlot("   "));
    assertEquals(404, ex.getResponse().getStatus());
  }

  @Test
  public void getSlotWrapsUnexpectedFailures() {
    RuntimeException cause = new IllegalStateException("Failed to load slot");
    when(adaptor.getSlot(any(), eq("boom"))).thenThrow(cause);
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.getSlot("boom"));
    assertEquals(500, ex.getResponse().getStatus());
    assertSame(cause, ex.getCause());
  }
}
