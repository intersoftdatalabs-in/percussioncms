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
package com.percussion.rest.slotrelationships;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("UnitTest")
public class SlotRelationshipResourceTest {

  private ISlotRelationshipAdaptor adaptor;
  private SlotRelationshipResource resource;

  @BeforeEach
  public void setUp() {
    adaptor = mock(ISlotRelationshipAdaptor.class);
    resource = new SlotRelationshipResource(adaptor);
  }

  @Test
  public void canvasSuccess() {
    SlotCanvas canvas = new SlotCanvas(10, 7, List.of());
    when(adaptor.canvas(10, 7)).thenReturn(canvas);
    assertSame(canvas, resource.canvas(10, 7));
    verify(adaptor).canvas(10, 7);
  }

  @Test
  public void canvasMissingOwnerIs400() {
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.canvas(null, 7));
    assertEquals(400, ex.getResponse().getStatus());
  }

  @Test
  public void addSuccess() {
    SlotAddRequest req = new SlotAddRequest();
    req.setOwnerId(10);
    req.setDependentId(20);
    req.setSlotId(3);
    req.setTemplateId(4);
    SlotRelationship created = new SlotRelationship(99, 10, 20, 3, 4, 0);
    when(adaptor.add(req)).thenReturn(created);
    assertSame(created, resource.add(req));
  }

  @Test
  public void addMissingIdsIs400() {
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.add(new SlotAddRequest()));
    assertEquals(400, ex.getResponse().getStatus());
  }

  @Test
  public void addMissingItemIs404() {
    SlotAddRequest req = new SlotAddRequest();
    req.setOwnerId(10);
    req.setDependentId(20);
    req.setSlotId(3);
    req.setTemplateId(4);
    when(adaptor.add(req)).thenReturn(null);
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.add(req));
    assertEquals(404, ex.getResponse().getStatus());
  }

  @Test
  public void removeSuccess() {
    Response out = resource.remove(99);
    assertEquals(204, out.getStatus());
    verify(adaptor).remove(99);
  }

  @Test
  public void removeInvalidIdIs400() {
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.remove(0));
    assertEquals(400, ex.getResponse().getStatus());
  }

  @Test
  public void moveRequiresDirection() {
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.move(1, new SlotMoveRequest()));
    assertEquals(400, ex.getResponse().getStatus());
  }

  @Test
  public void moveSuccess() {
    SlotMoveRequest req = new SlotMoveRequest();
    req.setDirection("UP");
    Response out = resource.move(8, req);
    assertEquals(204, out.getStatus());
    verify(adaptor).move(8, req);
  }

  @Test
  public void changeTemplateSlotSuccess() {
    SlotTemplateSlotRequest req = new SlotTemplateSlotRequest();
    req.setSlotId(3);
    req.setTemplateId(4);
    SlotRelationship updated = new SlotRelationship(8, 10, 20, 3, 4, 0);
    when(adaptor.changeTemplateSlot(8, req)).thenReturn(updated);
    assertSame(updated, resource.changeTemplateSlot(8, req));
  }

  @Test
  public void changeTemplateSlotMissingIs404() {
    SlotTemplateSlotRequest req = new SlotTemplateSlotRequest();
    req.setSlotId(3);
    req.setTemplateId(4);
    when(adaptor.changeTemplateSlot(8, req)).thenReturn(null);
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.changeTemplateSlot(8, req));
    assertEquals(404, ex.getResponse().getStatus());
  }

  @Test
  public void allowedTypesSuccess() {
    SlotAllowedChoiceList list = new SlotAllowedChoiceList(List.of());
    when(adaptor.allowedTypes(3)).thenReturn(list);
    assertSame(list, resource.allowedTypes(3));
  }

  @Test
  public void allowedTemplatesRequiresSlot() {
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.allowedTemplates(null, 1));
    assertEquals(400, ex.getResponse().getStatus());
  }

  @Test
  public void missingAdaptorIs503() {
    SlotRelationshipResource bare = new SlotRelationshipResource();
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> bare.canvas(1, 2));
    assertEquals(503, ex.getResponse().getStatus());
  }

  @Test
  public void unexpectedFailureIs500() {
    IllegalStateException boom = new IllegalStateException("boom");
    when(adaptor.canvas(1, 2)).thenThrow(boom);
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.canvas(1, 2));
    assertEquals(500, ex.getResponse().getStatus());
    assertSame(boom, ex.getCause());
  }
}
