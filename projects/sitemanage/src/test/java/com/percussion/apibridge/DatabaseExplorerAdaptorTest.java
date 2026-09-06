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

package com.percussion.apibridge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.apibridge.DatabaseExplorerAdaptor.ConfiguredDatasource;
import com.percussion.rest.databaseexplorer.DatabaseExplorerDatasource;
import com.percussion.rest.databaseexplorer.DatabaseExplorerTable;
import com.percussion.server.PSServer;
import com.percussion.util.PSProperties;
import jakarta.ws.rs.WebApplicationException;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Path-safe Database Explorer browse: allow-listed datasources only; non-allow-listed
 * names are 400 and never open JDBC.
 */
@Tag("UnitTest")
class DatabaseExplorerAdaptorTest {

  private DatabaseExplorerAdaptor adaptor;
  private PSProperties previousProps;
  private Field propsField;
  private boolean openedJdbc;

  @BeforeEach
  void setUp() throws Exception {
    Map<String, ConfiguredDatasource> catalog = new LinkedHashMap<>();
    catalog.put(
        "cms",
        new ConfiguredDatasource("cms", "cms", DatabaseExplorerAdaptor.REPOSITORY_TOKEN, true));
    openedJdbc = false;
    DatabaseExplorerAdaptor.DatasourceRuntime runtime =
        new DatabaseExplorerAdaptor.DatasourceRuntime() {
          @Override
          public boolean isAvailable(String cmsDatasourceName) {
            return DatabaseExplorerAdaptor.REPOSITORY_TOKEN.equalsIgnoreCase(cmsDatasourceName);
          }

          @Override
          public List<DatabaseExplorerTable> listTables(String cmsDatasourceName) {
            openedJdbc = true;
            DatabaseExplorerTable t = new DatabaseExplorerTable();
            t.setName("CONTENTSTATUS");
            t.setType("TABLE");
            t.setSchema("PUBLIC");
            return List.of(t);
          }
        };
    adaptor = new DatabaseExplorerAdaptor(() -> catalog, () -> true, runtime);

    propsField = PSServer.class.getDeclaredField("ms_serverProps");
    propsField.setAccessible(true);
    previousProps = (PSProperties) propsField.get(null);
    propsField.set(null, new PSProperties());
  }

  @AfterEach
  void restoreServerProps() throws Exception {
    if (propsField != null) {
      propsField.set(null, previousProps);
    }
  }

  @Test
  void listDatasources_returnsIdWithoutJdbcUrl() {
    List<DatabaseExplorerDatasource> list = adaptor.listDatasources();
    assertEquals(1, list.size());
    assertEquals("cms", list.get(0).getId());
    assertEquals(Boolean.TRUE, list.get(0).getRepository());
    assertEquals(Boolean.TRUE, list.get(0).getAvailable());
    assertFalse(String.valueOf(list.get(0).getId()).contains("jdbc:"));
  }

  @Test
  void listDatasources_nonAdminIs403() {
    Map<String, ConfiguredDatasource> catalog = new LinkedHashMap<>();
    catalog.put("cms", new ConfiguredDatasource("cms", "cms", "cms", false));
    adaptor = new DatabaseExplorerAdaptor(() -> catalog, () -> false, unavailableRuntime());
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> adaptor.listDatasources());
    assertEquals(403, ex.getResponse().getStatus());
    assertEquals(DatabaseExplorerAdaptor.ADMIN_REQUIRED, ex.getMessage());
  }

  @Test
  void listTables_listsAllowListed() {
    List<DatabaseExplorerTable> tables = adaptor.listTables("cms");
    assertEquals(1, tables.size());
    assertEquals("CONTENTSTATUS", tables.get(0).getName());
    assertEquals("TABLE", tables.get(0).getType());
    assertTrue(openedJdbc);
  }

  @Test
  void listTables_nonAllowListedIs400AndDoesNotOpenJdbc() {
    openedJdbc = false;
    IllegalArgumentException ex =
        assertThrows(IllegalArgumentException.class, () -> adaptor.listTables("secret_prod"));
    assertEquals(DatabaseExplorerAdaptor.NOT_ALLOW_LISTED, ex.getMessage());
    assertFalse(ex.getMessage().contains("secret"));
    assertFalse(openedJdbc);
  }

  @Test
  void listTables_unsafeIdIs400() {
    IllegalArgumentException ex =
        assertThrows(IllegalArgumentException.class, () -> adaptor.listTables("../cms"));
    assertEquals(DatabaseExplorerAdaptor.INVALID_DATASOURCE, ex.getMessage());
    assertFalse(ex.getMessage().contains(".."));
    assertFalse(openedJdbc);
  }

  @Test
  void listTables_unavailableAllowListedIsNull() {
    Map<String, ConfiguredDatasource> catalog = new LinkedHashMap<>();
    catalog.put("cms", new ConfiguredDatasource("cms", "cms", "missing_ds", false));
    adaptor = new DatabaseExplorerAdaptor(() -> catalog, () -> true, unavailableRuntime());
    assertNull(adaptor.listTables("cms"));
  }

  @Test
  void listTables_nonAdminIs403AndDoesNotOpenJdbc() {
    Map<String, ConfiguredDatasource> catalog = new LinkedHashMap<>();
    catalog.put("cms", new ConfiguredDatasource("cms", "cms", "cms", false));
    adaptor = new DatabaseExplorerAdaptor(() -> catalog, () -> false, unavailableRuntime());
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> adaptor.listTables("cms"));
    assertEquals(403, ex.getResponse().getStatus());
    assertFalse(openedJdbc);
  }

  @Test
  void parseAllowListedDatasources_repositoryTokenAndBareId() {
    Map<String, ConfiguredDatasource> parsed =
        DatabaseExplorerAdaptor.parseAllowListedDatasources("cms=repository;other");
    assertEquals(2, parsed.size());
    assertTrue(parsed.get("cms").repository());
    assertEquals("repository", parsed.get("cms").cmsDatasourceName());
    assertEquals("other", parsed.get("other").cmsDatasourceName());
    assertFalse(parsed.get("other").repository());
  }

  @Test
  void parseAllowListedDatasources_skipsBadIds() {
    Map<String, ConfiguredDatasource> parsed =
        DatabaseExplorerAdaptor.parseAllowListedDatasources(
            "../x=foo;bad/id=bar;ok=repository;empty=;jdbc:h2=nope");
    assertEquals(1, parsed.size());
    assertTrue(parsed.containsKey("ok"));
  }

  @Test
  void parseAllowListedDatasources_emptySpec() {
    assertTrue(DatabaseExplorerAdaptor.parseAllowListedDatasources(null).isEmpty());
    assertTrue(DatabaseExplorerAdaptor.parseAllowListedDatasources("  ").isEmpty());
  }

  @Test
  void loadDatasourcesFromServerProperties_readsPsProperties() throws Exception {
    PSProperties props = (PSProperties) propsField.get(null);
    props.setProperty(DatabaseExplorerAdaptor.PROP_ALLOW_LISTED_DATASOURCES, "cms=repository");
    Map<String, ConfiguredDatasource> parsed =
        DatabaseExplorerAdaptor.parseAllowListedDatasources(
            PSServer.getProperty(DatabaseExplorerAdaptor.PROP_ALLOW_LISTED_DATASOURCES));
    assertEquals(1, parsed.size());
    assertEquals("cms", parsed.get("cms").id());
    assertTrue(parsed.get("cms").repository());
  }

  @Test
  void normalizeObjectType_mapsH2BaseTable() {
    assertEquals("TABLE", DatabaseExplorerAdaptor.normalizeObjectType("BASE TABLE"));
    assertEquals("TABLE", DatabaseExplorerAdaptor.normalizeObjectType("table"));
    assertEquals("VIEW", DatabaseExplorerAdaptor.normalizeObjectType("VIEW"));
    assertNull(DatabaseExplorerAdaptor.normalizeObjectType("SYSTEM TABLE"));
  }

  @Test
  void isSafeCatalogId() {
    assertTrue(DatabaseExplorerAdaptor.isSafeCatalogId("cms"));
    assertTrue(DatabaseExplorerAdaptor.isSafeCatalogId("rx_repo"));
    assertFalse(DatabaseExplorerAdaptor.isSafeCatalogId(""));
    assertFalse(DatabaseExplorerAdaptor.isSafeCatalogId("../x"));
    assertFalse(DatabaseExplorerAdaptor.isSafeCatalogId("a/b"));
    assertFalse(DatabaseExplorerAdaptor.isSafeCatalogId("1bad"));
  }

  @Test
  void listTablesFromMeta_h2MemCatalogIsPathSafe() throws Exception {
    try (Connection conn =
            DriverManager.getConnection("jdbc:h2:mem:dbexptest;DB_CLOSE_DELAY=-1");
        Statement st = conn.createStatement()) {
      st.execute("CREATE TABLE CMS_ITEM (ID INT)");
      st.execute("CREATE VIEW CMS_V AS SELECT ID FROM CMS_ITEM");
      List<DatabaseExplorerTable> tables =
          DatabaseExplorerAdaptor.listTablesFromMeta(conn, null, "PUBLIC");
      assertTrue(tables.stream().anyMatch(t -> "CMS_ITEM".equals(t.getName()) && "TABLE".equals(t.getType())));
      assertTrue(tables.stream().anyMatch(t -> "CMS_V".equals(t.getName()) && "VIEW".equals(t.getType())));
      for (DatabaseExplorerTable t : tables) {
        assertTrue(DatabaseExplorerAdaptor.isSafeSqlIdent(t.getName()));
        assertFalse(t.getName().contains(".."));
        assertFalse(t.getName().contains("/"));
      }
    }
  }

  private DatabaseExplorerAdaptor.DatasourceRuntime unavailableRuntime() {
    return new DatabaseExplorerAdaptor.DatasourceRuntime() {
      @Override
      public boolean isAvailable(String cmsDatasourceName) {
        return false;
      }

      @Override
      public List<DatabaseExplorerTable> listTables(String cmsDatasourceName) {
        openedJdbc = true;
        return List.of();
      }
    };
  }
}
