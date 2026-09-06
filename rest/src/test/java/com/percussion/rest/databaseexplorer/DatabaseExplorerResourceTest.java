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

package com.percussion.rest.databaseexplorer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("UnitTest")
public class DatabaseExplorerResourceTest {

  private IDatabaseExplorerAdaptor adaptor;
  private DatabaseExplorerResource resource;

  @BeforeEach
  public void setUp() {
    adaptor = mock(IDatabaseExplorerAdaptor.class);
    resource = new DatabaseExplorerResource(adaptor);
  }

  @Test
  public void listDatasourcesDelegates() {
    DatabaseExplorerDatasource ds = new DatabaseExplorerDatasource();
    ds.setId("cms");
    when(adaptor.listDatasources()).thenReturn(List.of(ds));
    List<DatabaseExplorerDatasource> out = resource.listDatasources();
    assertEquals(1, out.size());
    assertEquals("cms", out.get(0).getId());
    verify(adaptor).listDatasources();
  }

  @Test
  public void listDatasourcesNullSafe() {
    when(adaptor.listDatasources()).thenReturn(null);
    assertTrue(resource.listDatasources().isEmpty());
  }

  @Test
  public void listDatasourcesWrapsUnexpectedAs500() {
    IllegalStateException boom = new IllegalStateException("boom");
    when(adaptor.listDatasources()).thenThrow(boom);
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.listDatasources());
    assertEquals(500, ex.getResponse().getStatus());
    assertSame(boom, ex.getCause());
  }

  @Test
  public void listDatasourcesNonAdminIs403() {
    when(adaptor.listDatasources())
        .thenThrow(new WebApplicationException("Admin role required", Response.Status.FORBIDDEN));
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.listDatasources());
    assertEquals(403, ex.getResponse().getStatus());
  }

  @Test
  public void missingAdaptorReturnsServiceUnavailableOnListDatasources() {
    DatabaseExplorerResource bare = new DatabaseExplorerResource();
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, bare::listDatasources);
    assertEquals(503, ex.getResponse().getStatus());
  }

  @Test
  public void listTablesDelegates() {
    DatabaseExplorerTable table = new DatabaseExplorerTable();
    table.setName("CONTENTSTATUS");
    table.setType("TABLE");
    when(adaptor.listTables(eq("cms"))).thenReturn(List.of(table));
    List<DatabaseExplorerTable> out = resource.listTables("cms");
    assertEquals(1, out.size());
    assertEquals("CONTENTSTATUS", out.get(0).getName());
    verify(adaptor).listTables("cms");
  }

  @Test
  public void listTablesUnknownIsGeneric404WithoutRawName() {
    when(adaptor.listTables(eq("missing"))).thenReturn(null);
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.listTables("missing"));
    assertEquals(404, ex.getResponse().getStatus());
    assertEquals(DatabaseExplorerResource.DATASOURCE_NOT_FOUND, ex.getMessage());
    assertFalse(ex.getMessage().contains("missing"));
  }

  @Test
  public void listTablesNonAllowListedIs400WithoutEcho() {
    when(adaptor.listTables(eq("secret_prod")))
        .thenThrow(new IllegalArgumentException("Datasource is not allow-listed"));
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.listTables("secret_prod"));
    assertEquals(400, ex.getResponse().getStatus());
    assertEquals("Datasource is not allow-listed", ex.getMessage());
    assertFalse(ex.getMessage().contains("secret_prod"));
  }

  @Test
  public void listTablesIllegalArgumentWithJdbcUrlIsSanitized() {
    when(adaptor.listTables(any()))
        .thenThrow(new IllegalArgumentException("jdbc:h2:mem:secret;PASSWORD=x"));
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.listTables("cms"));
    assertEquals(400, ex.getResponse().getStatus());
    assertEquals(DatabaseExplorerResource.INVALID_DATASOURCE, ex.getMessage());
    assertFalse(ex.getMessage().contains("jdbc"));
    assertFalse(ex.getMessage().contains("PASSWORD"));
  }

  @Test
  public void listTablesWrapsUnexpectedAs500() {
    IllegalStateException boom = new IllegalStateException("down");
    when(adaptor.listTables(eq("cms"))).thenThrow(boom);
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.listTables("cms"));
    assertEquals(500, ex.getResponse().getStatus());
    assertSame(boom, ex.getCause());
  }

  @Test
  public void looksLikeRawSecretOrSql() {
    assertTrue(DatabaseExplorerResource.looksLikeRawSecretOrSql("jdbc:h2:mem:x"));
    assertTrue(DatabaseExplorerResource.looksLikeRawSecretOrSql("password=secret"));
    assertTrue(DatabaseExplorerResource.looksLikeRawSecretOrSql("C:\\Windows\\secret"));
    assertFalse(DatabaseExplorerResource.looksLikeRawSecretOrSql("Invalid datasource"));
    assertFalse(DatabaseExplorerResource.looksLikeRawSecretOrSql("Datasource is not allow-listed"));
  }

  @Test
  public void missingAdaptorReturnsServiceUnavailableOnListTables() {
    DatabaseExplorerResource bare = new DatabaseExplorerResource();
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> bare.listTables("cms"));
    assertEquals(503, ex.getResponse().getStatus());
    verify(adaptor, never()).listTables(any());
  }
}
