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

import com.percussion.rest.databaseexplorer.DatabaseExplorerDatasource;
import com.percussion.rest.databaseexplorer.DatabaseExplorerTable;
import com.percussion.rest.databaseexplorer.IDatabaseExplorerAdaptor;
import com.percussion.server.PSServer;
import com.percussion.services.datasource.PSDatasourceMgrLocator;
import com.percussion.share.service.exception.PSDataServiceException;
import com.percussion.system.utils.PSSiteManageBean;
import com.percussion.user.data.PSCurrentUser;
import com.percussion.user.service.IPSUserService;
import com.percussion.utils.jdbc.IPSConnectionInfo;
import com.percussion.utils.jdbc.IPSDatasourceManager;
import com.percussion.utils.jdbc.PSConnectionDetail;
import com.percussion.utils.jdbc.PSConnectionHelper;
import com.percussion.utils.jdbc.PSConnectionInfo;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;

/**
 * Database Explorer browse (Workbench §12.2) over operator-configured allow-listed
 * JDBC datasources.
 *
 * <p>Distinct from File Explorer. Catalog ids and CMS datasource names are
 * validated before any JDBC metadata call. Non-allow-listed names never open a
 * connection. Responses never include JDBC URLs or credentials.
 */
@PSSiteManageBean
@Lazy
public class DatabaseExplorerAdaptor implements IDatabaseExplorerAdaptor {

  private static final Logger log = LogManager.getLogger(DatabaseExplorerAdaptor.class);

  static final String ADMIN_REQUIRED = "Admin role required to browse Database Explorer";
  static final String INVALID_DATASOURCE = "Invalid datasource";
  static final String NOT_ALLOW_LISTED = "Datasource is not allow-listed";

  /** server.properties key: {@code id;id2=cmsName;cms=repository}. */
  public static final String PROP_ALLOW_LISTED_DATASOURCES =
      "databaseExplorer.allowListedDatasources";

  /** Maps a catalog id to the CMS repository datasource. */
  public static final String REPOSITORY_TOKEN = "repository";

  private static final Pattern SAFE_CATALOG_ID = Pattern.compile("[A-Za-z][A-Za-z0-9_-]{0,63}");
  private static final Pattern SAFE_CMS_DS_NAME = Pattern.compile("[A-Za-z][A-Za-z0-9_-]{0,127}");
  private static final Pattern SAFE_SQL_IDENT = Pattern.compile("[A-Za-z][A-Za-z0-9_$#]{0,127}");

  private static final String[] TABLE_TYPES = {"TABLE", "VIEW"};

  private final Supplier<Map<String, ConfiguredDatasource>> catalog;
  private final BooleanSupplier adminChecker;
  private final DatasourceRuntime runtime;

  /** Injected by Spring in production; unused when {@link #adminChecker} is overridden in tests. */
  @Autowired(required = false)
  private IPSUserService userService;

  public DatabaseExplorerAdaptor() {
    this(
        DatabaseExplorerAdaptor::loadDatasourcesFromServerProperties,
        null,
        new ProductionDatasourceRuntime());
  }

  /** Package-visible for tests. */
  DatabaseExplorerAdaptor(
      Supplier<Map<String, ConfiguredDatasource>> catalog,
      BooleanSupplier adminChecker,
      DatasourceRuntime runtime) {
    this.catalog = catalog;
    this.adminChecker = adminChecker != null ? adminChecker : this::isCurrentUserAdmin;
    this.runtime = runtime != null ? runtime : new ProductionDatasourceRuntime();
  }

  @Override
  public List<DatabaseExplorerDatasource> listDatasources() {
    requireAdmin();
    List<DatabaseExplorerDatasource> out = new ArrayList<>();
    for (ConfiguredDatasource ds : catalog.get().values()) {
      DatabaseExplorerDatasource dto = new DatabaseExplorerDatasource();
      dto.setId(ds.id());
      dto.setDisplayName(ds.displayName());
      dto.setRepository(ds.repository());
      dto.setAvailable(runtime.isAvailable(ds.cmsDatasourceName()));
      out.add(dto);
    }
    return out;
  }

  @Override
  public List<DatabaseExplorerTable> listTables(String datasourceId) {
    requireAdmin();
    if (!isSafeCatalogId(datasourceId)) {
      throw new IllegalArgumentException(INVALID_DATASOURCE);
    }
    ConfiguredDatasource ds = catalog.get().get(datasourceId);
    if (ds == null) {
      throw new IllegalArgumentException(NOT_ALLOW_LISTED);
    }
    if (!runtime.isAvailable(ds.cmsDatasourceName())) {
      return null;
    }
    return runtime.listTables(ds.cmsDatasourceName());
  }

  private void requireAdmin() {
    boolean allowed;
    try {
      allowed = adminChecker.getAsBoolean();
    } catch (WebApplicationException e) {
      throw e;
    } catch (RuntimeException e) {
      log.error("Admin check failed unexpectedly", e);
      throw new WebApplicationException(e, Response.Status.INTERNAL_SERVER_ERROR);
    }
    if (!allowed) {
      throw new WebApplicationException(ADMIN_REQUIRED, Response.Status.FORBIDDEN);
    }
  }

  boolean isCurrentUserAdmin() {
    if (userService == null) {
      return false;
    }
    try {
      PSCurrentUser current = userService.getCurrentUser();
      if (current == null || StringUtils.isBlank(current.getName())) {
        return false;
      }
      return userService.isAdminUser(current.getName());
    } catch (PSDataServiceException e) {
      log.debug("Unable to resolve current user for Admin check: {}", e.getMessage());
      return false;
    }
  }

  static Map<String, ConfiguredDatasource> loadDatasourcesFromServerProperties() {
    return parseAllowListedDatasources(PSServer.getProperty(PROP_ALLOW_LISTED_DATASOURCES));
  }

  /**
   * Parse {@code id} or {@code id=cmsName} entries separated by {@code ;}. The reserved
   * token {@code repository} maps to the CMS repository datasource. Malformed or unsafe
   * entries are skipped.
   */
  static Map<String, ConfiguredDatasource> parseAllowListedDatasources(String spec) {
    Map<String, ConfiguredDatasource> out = new LinkedHashMap<>();
    if (spec == null || spec.isBlank()) {
      return out;
    }
    String[] entries = spec.split(";");
    for (String entry : entries) {
      if (entry == null) {
        continue;
      }
      String trimmed = entry.trim();
      if (trimmed.isEmpty()) {
        continue;
      }
      String id;
      String cmsName;
      int eq = trimmed.indexOf('=');
      if (eq < 0) {
        id = trimmed;
        cmsName = trimmed;
      } else if (eq == 0 || eq >= trimmed.length() - 1) {
        continue;
      } else {
        id = trimmed.substring(0, eq).trim();
        cmsName = trimmed.substring(eq + 1).trim();
      }
      if (!isSafeCatalogId(id) || !isSafeCmsDatasourceName(cmsName)) {
        continue;
      }
      boolean repository = REPOSITORY_TOKEN.equalsIgnoreCase(cmsName);
      out.putIfAbsent(id, new ConfiguredDatasource(id, id, cmsName, repository));
    }
    return out;
  }

  static boolean isSafeCatalogId(String id) {
    return id != null && SAFE_CATALOG_ID.matcher(id).matches();
  }

  static boolean isSafeCmsDatasourceName(String name) {
    if (name == null || name.isBlank()) {
      return false;
    }
    if (REPOSITORY_TOKEN.equalsIgnoreCase(name)) {
      return true;
    }
    return SAFE_CMS_DS_NAME.matcher(name).matches();
  }

  static boolean isSafeSqlIdent(String name) {
    return name != null && SAFE_SQL_IDENT.matcher(name).matches();
  }

  static boolean isSystemSchema(String schema) {
    if (schema == null || schema.isBlank()) {
      return false;
    }
    String upper = schema.trim().toUpperCase(Locale.ROOT);
    return "INFORMATION_SCHEMA".equals(upper)
        || "SYS".equals(upper)
        || "SYSTEM".equals(upper)
        || upper.startsWith("PG_");
  }

  static String normalizeObjectType(String jdbcType) {
    if (jdbcType == null) {
      return null;
    }
    String t = jdbcType.trim().toUpperCase(Locale.ROOT);
    if ("TABLE".equals(t) || "BASE TABLE".equals(t)) {
      return "TABLE";
    }
    if ("VIEW".equals(t)) {
      return "VIEW";
    }
    return null;
  }

  record ConfiguredDatasource(
      String id, String displayName, String cmsDatasourceName, boolean repository) {}

  /** JDBC catalog runtime — production uses the CMS datasource manager. */
  interface DatasourceRuntime {
    boolean isAvailable(String cmsDatasourceName);

    List<DatabaseExplorerTable> listTables(String cmsDatasourceName);
  }

  static final class ProductionDatasourceRuntime implements DatasourceRuntime {

    @Override
    public boolean isAvailable(String cmsDatasourceName) {
      try {
        IPSDatasourceManager mgr = PSDatasourceMgrLocator.getDatasourceMgr();
        if (mgr == null) {
          return false;
        }
        if (REPOSITORY_TOKEN.equalsIgnoreCase(cmsDatasourceName)) {
          return StringUtils.isNotBlank(mgr.getRepositoryDatasource());
        }
        List<String> names = mgr.getDatasources();
        return names != null && names.contains(cmsDatasourceName);
      } catch (RuntimeException e) {
        log.debug("Database Explorer datasource availability check failed: {}", e.toString());
        return false;
      }
    }

    @Override
    public List<DatabaseExplorerTable> listTables(String cmsDatasourceName) {
      IPSConnectionInfo info =
          REPOSITORY_TOKEN.equalsIgnoreCase(cmsDatasourceName)
              ? null
              : new PSConnectionInfo(cmsDatasourceName);
      try (Connection conn = PSConnectionHelper.getDbConnection(info)) {
        PSConnectionDetail detail = PSConnectionHelper.getConnectionDetail(info);
        String catalogName = detail != null ? emptyToNull(detail.getDatabase()) : null;
        String schema = detail != null ? emptyToNull(detail.getOrigin()) : null;
        return listTablesFromMeta(conn, catalogName, schema);
      } catch (SQLException | javax.naming.NamingException e) {
        log.warn("Failed to list Database Explorer tables: {}", e.toString());
        throw new WebApplicationException(
            "Failed to list tables", e, Response.Status.INTERNAL_SERVER_ERROR);
      }
    }
  }

  static List<DatabaseExplorerTable> listTablesFromMeta(
      Connection conn, String catalogName, String schema) throws SQLException {
    List<DatabaseExplorerTable> out = new ArrayList<>();
    DatabaseMetaData meta = conn.getMetaData();
    try (ResultSet rs = meta.getTables(catalogName, schema, "%", TABLE_TYPES)) {
      if (rs == null) {
        return out;
      }
      while (rs.next()) {
        String name = rs.getString("TABLE_NAME");
        if (!isSafeSqlIdent(name) || name.startsWith("BIN$")) {
          continue;
        }
        String type = normalizeObjectType(rs.getString("TABLE_TYPE"));
        if (type == null) {
          continue;
        }
        String tableSchema = rs.getString("TABLE_SCHEM");
        if (isSystemSchema(tableSchema)) {
          continue;
        }
        DatabaseExplorerTable row = new DatabaseExplorerTable();
        row.setName(name);
        row.setType(type);
        if (isSafeSqlIdent(tableSchema)) {
          row.setSchema(tableSchema);
        }
        out.add(row);
      }
    }
    out.sort(
        Comparator.comparing(
                (DatabaseExplorerTable t) -> "TABLE".equals(t.getType()) ? 0 : 1)
            .thenComparing(
                DatabaseExplorerTable::getName,
                Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));
    return out;
  }

  private static String emptyToNull(String value) {
    return StringUtils.isBlank(value) ? null : value;
  }
}
