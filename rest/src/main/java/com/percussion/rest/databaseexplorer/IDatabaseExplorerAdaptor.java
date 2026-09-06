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

import java.util.List;

/**
 * Adaptor for Database Explorer catalog browse (Workbench §12.2). Read-only JDBC
 * catalog of allow-listed datasources and their tables/views. Distinct from File
 * Explorer (§12.1).
 */
public interface IDatabaseExplorerAdaptor {

  /**
   * List configured allow-listed datasources (catalog ids only — never JDBC URLs
   * or credentials).
   *
   * @return never {@code null}; empty when no datasources are configured
   */
  List<DatabaseExplorerDatasource> listDatasources();

  /**
   * List tables and views for an allow-listed datasource catalog id.
   *
   * @param datasourceId allow-listed catalog id (not a JDBC URL)
   * @return tables/views, or {@code null} when the datasource is allow-listed but
   *     unavailable (HTTP 404)
   */
  List<DatabaseExplorerTable> listTables(String datasourceId);
}
