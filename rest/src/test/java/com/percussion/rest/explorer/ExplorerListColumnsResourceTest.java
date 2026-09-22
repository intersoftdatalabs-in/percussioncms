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
package com.percussion.rest.explorer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.ws.rs.WebApplicationException;
import java.util.List;
import org.junit.jupiter.api.Test;

class ExplorerListColumnsResourceTest {

  @Test
  void saveThenGetKeepsColumnsForThatUserAndFolder() {
    ExplorerListColumnsResource resource = resource("Admin");
    ExplorerListColumns body = new ExplorerListColumns();
    body.setFolderPath("//Sites/Demo");
    body.setColumns(List.of("sys_title", "sys_workflow"));

    ExplorerListColumns saved = resource.saveColumns(body);
    assertEquals(List.of("sys_title", "sys_workflow"), saved.getColumns());

    ExplorerListColumns loaded = resource.getColumns("//Sites/Demo");
    assertEquals(List.of("sys_title", "sys_workflow"), loaded.getColumns());
    assertTrue(resource.getColumns("//Sites/Other").getColumns().isEmpty());
  }

  @Test
  void otherUserDoesNotSeeSavedColumns() {
    ExplorerListColumnStore store = new ExplorerListColumnStore();
    ExplorerListColumnsResource admin = new ExplorerListColumnsResource(store, () -> "Admin");
    ExplorerListColumns body = new ExplorerListColumns("//Sites/Demo", List.of("sys_title"));
    admin.saveColumns(body);

    ExplorerListColumnsResource other = new ExplorerListColumnsResource(store, () -> "Editor");
    assertTrue(other.getColumns("//Sites/Demo").getColumns().isEmpty());
  }

  @Test
  void unknownColumnIs400() {
    ExplorerListColumnsResource resource = resource("Admin");
    ExplorerListColumns body =
        new ExplorerListColumns("//Sites/Demo", List.of("sys_title", "not_a_field"));
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.saveColumns(body));
    assertEquals(400, ex.getResponse().getStatus());
  }

  @Test
  void missingTitleIs400() {
    ExplorerListColumnsResource resource = resource("Admin");
    ExplorerListColumns body = new ExplorerListColumns("//Sites/Demo", List.of("sys_workflow"));
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.saveColumns(body));
    assertEquals(400, ex.getResponse().getStatus());
  }

  @Test
  void traversalPathIs400() {
    ExplorerListColumnsResource resource = resource("Admin");
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.getColumns("//Sites/../etc"));
    assertEquals(400, ex.getResponse().getStatus());
  }

  @Test
  void missingUserIs403() {
    ExplorerListColumnsResource resource = resource("  ");
    ExplorerListColumns body = new ExplorerListColumns("//Sites/Demo", List.of("sys_title"));
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.saveColumns(body));
    assertEquals(403, ex.getResponse().getStatus());
  }

  @Test
  void nullBodyIs400() {
    ExplorerListColumnsResource resource = resource("Admin");
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.saveColumns(null));
    assertEquals(400, ex.getResponse().getStatus());
  }

  private static ExplorerListColumnsResource resource(String user) {
    return new ExplorerListColumnsResource(new ExplorerListColumnStore(), () -> user);
  }
}
