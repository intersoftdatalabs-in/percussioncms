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
package com.percussion.install;

import com.percussion.tablefactory.PSJdbcDbmsDef;
import com.percussion.tablefactory.PSJdbcTableFactory;
import com.percussion.tablefactory.tools.PSCatalogTableData;
import com.percussion.utils.jdbc.PSJdbcUtils;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * Derby → H2 (or any TableFactory backend) data transfer via <strong>export XML → import
 * XML</strong> (#548 T058).
 *
 * <p>Does <em>not</em> invent a JDBC pump. Uses product TableFactory:
 *
 * <ol>
 *   <li>{@link PSCatalogTableData#exportDatabase} — catalog schema+data to staging XML
 *   <li>{@link PSJdbcTableFactory#importDatabase} — apply schema/data to target with datatype map
 * </ol>
 *
 * <p>Config cutover remains separate ({@link PSConfigCutover}).
 */
public final class PSTableFactoryMigrationTransfer {

  private static final Logger LOG =
      Logger.getLogger(PSTableFactoryMigrationTransfer.class.getName());

  private PSTableFactoryMigrationTransfer() {}

  /**
   * Transfer result.
   *
   * @param stagingDir export XML root
   * @param tablesExported tables written to tableDef.xml
   * @param tablesImported tables processed on import (fail-fast)
   */
  public record Result(Path stagingDir, int tablesExported, int tablesImported) {}

  /**
   * Export all tables from source props, then import into target props.
   *
   * @param sourceRepositoryProps rxrepository-style props for source (Derby)
   * @param targetRepositoryProps rxrepository-style props for target (H2)
   * @param stagingDir working directory for defData/binaryData (created)
   * @return summary
   * @throws Exception on export/import failure (target may be partial — do not cut over)
   */
  public static Result exportThenImport(
      Properties sourceRepositoryProps, Properties targetRepositoryProps, Path stagingDir)
      throws Exception {
    Objects.requireNonNull(sourceRepositoryProps, "sourceRepositoryProps");
    Objects.requireNonNull(targetRepositoryProps, "targetRepositoryProps");
    Objects.requireNonNull(stagingDir, "stagingDir");

    Files.createDirectories(stagingDir);
    File storageRoot = stagingDir.toFile();

    Properties sourceProps = toTableFactoryProps(sourceRepositoryProps);
    Properties targetProps = toTableFactoryProps(targetRepositoryProps);

    PSJdbcDbmsDef sourceDef = new PSJdbcDbmsDef(sourceProps);
    LOG.info(() -> "TableFactory export from backend=" + sourceDef.getBackEndDB());
    int exported = PSCatalogTableData.exportDatabase(sourceDef, storageRoot);
    LOG.info(() -> "TableFactory export complete tables=" + exported + " dir=" + stagingDir);

    PSJdbcDbmsDef targetDef = new PSJdbcDbmsDef(targetProps);
    LOG.info(() -> "TableFactory import to backend=" + targetDef.getBackEndDB());
    // failFast=true: migration must not cut over after a partial import
    int imported = PSJdbcTableFactory.importDatabase(targetDef, storageRoot, true);
    LOG.info(() -> "TableFactory import complete tables=" + imported);

    return new Result(stagingDir, exported, imported);
  }

  /**
   * Normalize repository properties for {@link PSJdbcDbmsDef} (ensure backend label from driver
   * when missing; mark password not encrypted when plain).
   */
  public static Properties toTableFactoryProps(Properties repositoryProperties) {
    Properties p = new Properties();
    p.putAll(repositoryProperties);

    String driver = p.getProperty(PSJdbcDbmsDef.DB_DRIVER_NAME_PROPERTY);
    if (driver != null
        && (p.getProperty(PSJdbcDbmsDef.DB_BACKEND_PROPERTY) == null
            || p.getProperty(PSJdbcDbmsDef.DB_BACKEND_PROPERTY).isBlank())) {
      p.setProperty(
          PSJdbcDbmsDef.DB_BACKEND_PROPERTY, PSJdbcUtils.getDBBackendForDriver(driver.trim()));
    }
    if (p.getProperty(PSJdbcDbmsDef.PWD_ENCRYPTED_PROPERTY) == null) {
      p.setProperty(PSJdbcDbmsDef.PWD_ENCRYPTED_PROPERTY, "N");
    }
    if (p.getProperty(PSJdbcDbmsDef.UID_PROPERTY) == null) {
      p.setProperty(PSJdbcDbmsDef.UID_PROPERTY, "");
    }
    if (p.getProperty(PSJdbcDbmsDef.PWD_PROPERTY) == null) {
      p.setProperty(PSJdbcDbmsDef.PWD_PROPERTY, "");
    }
    if (p.getProperty(PSJdbcDbmsDef.DB_NAME_PROPERTY) == null) {
      p.setProperty(PSJdbcDbmsDef.DB_NAME_PROPERTY, "");
    }
    return p;
  }
}
