/*
 * Copyright 1999-2025 Percussion Software, Inc.
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

package com.percussion.utils.container;

import com.percussion.install.PSLogger;
import com.percussion.utils.container.config.ContainerConfig;
import java.io.File;
import java.nio.file.Path;

/**
 * Delivery Tier Service configuration resolved from the installation directory.
 */
public class PSDtsConfig implements IPSDtsConfig, ContainerConfig {

  boolean localStagingDTSEnabled = false;
  boolean localDTSEnabled = false;

  private Path rxDir;

  private static String PROD_PATH = "Deployment";
  private static String STAGING_PATH = "Staging/Deployment";
  private PSAbstractXmlConnectors dtsConnector;
  private PSAbstractXmlConnectors stagingDtsConnector;

  /**
   * Creates a DTS configuration for the supplied installation root.
   *
   * @param rxDir the installation root directory, never <code>null</code>
   */
  public PSDtsConfig(Path rxDir) {

    this.rxDir = rxDir;
    File dtsRoot = new File(rxDir.toAbsolutePath().toString(), PROD_PATH);

    if (dtsRoot.exists()) {
      localDTSEnabled = true;
      PSLogger.logInfo("localDTSEnabled *********: " + dtsRoot.getAbsolutePath());
      dtsConnector = new PSTomcatConnectors(rxDir, dtsRoot.toPath());
    }
    File dtsStagingRoot = new File(rxDir.toAbsolutePath().toString(), STAGING_PATH);
    if (dtsStagingRoot.exists()) {
      localStagingDTSEnabled = true;
      PSLogger.logInfo("localStagingDTSEnabled *********: " + dtsStagingRoot.getAbsolutePath());
      stagingDtsConnector = new PSTomcatConnectors(rxDir, dtsStagingRoot.toPath());
    }
  }

  /**
   * Returns the installation root directory.
   *
   * @return the installation root, never <code>null</code>
   */
  public Path getRxDir() {
    return rxDir;
  }

  @Override
  public boolean isLocalStagingDTSEnabled() {
    return localStagingDTSEnabled;
  }

  @Override
  public boolean isLocalDTSEnabled() {
    return localDTSEnabled;
  }

  @Override
  public PSAbstractConnectors getDtsConnectorInfo() {
    return dtsConnector;
  }

  @Override
  public PSAbstractConnectors getStagingDtsConnectorInfo() {
    return stagingDtsConnector;
  }

  @Override
  public String toString() {
    return "PSDtsConfig{"
        + "localStatingDTSEnabled="
        + localStagingDTSEnabled
        + ", localDTSEnabled="
        + localDTSEnabled
        + ", rxDir="
        + rxDir
        + ", dtsConnector="
        + dtsConnector
        + ", stagingDtsConnector="
        + stagingDtsConnector
        + '}';
  }

  /**
   * Loads connector state from the installation directory.
   */
  public void load() {
    if (localDTSEnabled) dtsConnector.load();
    if (localStagingDTSEnabled) stagingDtsConnector.load();
  }

  /**
   * Saves connector state to the installation directory.
   */
  public void save() {
    if (localDTSEnabled) dtsConnector.save();
    if (localStagingDTSEnabled) stagingDtsConnector.save();
  }
}
