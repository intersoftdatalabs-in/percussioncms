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
package com.percussion.deployer.install;

import com.percussion.design.objectstore.PSRelationshipConfig;
import com.percussion.install.IPSUpgradeModule;
import com.percussion.install.IPSUpgradePlugin;
import com.percussion.install.PSPluginResponse;
import com.percussion.install.PSUpgradePluginRelationship;
import com.percussion.install.RxUpgrade;
import com.percussion.security.error.PSExceptionUtils;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.pkginfo.PSPkgInfoServiceLocator;
import com.percussion.services.pkginfo.utils.PSIdNameHelper;
import com.percussion.util.IOTools;
import com.percussion.xml.PSXmlDocumentBuilder;
import java.io.PrintStream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Element;

/** Updates the package element version information for each of the system relationships. */
public class PSUpgradePluginRelationshipVersions implements IPSUpgradePlugin {

  private static final Logger log = LogManager.getLogger(PSUpgradePluginRelationshipVersions.class);

  /** Default constructor. */
  public PSUpgradePluginRelationshipVersions() {}

  /**
   * Perform updates.
   *
   * @param config the upgrade module, may not be <code>null</code>.
   * @param elemData the configuration element data, may not be <code>null</code>.
   * @return the plugin response indicating success or failure, never <code>null</code>.
   */
  public PSPluginResponse process(IPSUpgradeModule config, Element elemData) {
    int respType = PSPluginResponse.SUCCESS;
    String respMsg = "";
    PrintStream logger = config.getLogStream();

    var pkgInfoSvc = PSPkgInfoServiceLocator.getPkgInfoService();
    try (var conn = RxUpgrade.getJdbcConnection()) {
      var relPlugin = new PSUpgradePluginRelationship();
      relPlugin.setDbProperties(RxUpgrade.getRxRepositoryProps());

      var cfgDoc = relPlugin.getRelationshipConfigs(logger, conn);
      var cfgSet = relPlugin.getConfigSet(cfgDoc);

      var cfgStream = (java.util.stream.Stream<PSRelationshipConfig>) cfgSet.stream();
      cfgStream
          .filter(PSRelationshipConfig::isSystem)
          .forEach(
              relConfig -> {
                try {
                  var doc = PSXmlDocumentBuilder.createXmlDocument();
                  var version =
                      IOTools.getChecksum(PSXmlDocumentBuilder.toString(relConfig.toXml(doc)));

                  var relName = relConfig.getName();
                  var relGuid = PSIdNameHelper.getGuid(relName, PSTypeEnum.RELATIONSHIP_CONFIGNAME);
                  var foundElem = pkgInfoSvc.findPkgElementByObject(relGuid);
                  var pkgElem =
                      foundElem != null
                          ? pkgInfoSvc.loadPkgElementModifiable(foundElem.getGuid())
                          : null;

                  if (pkgElem != null) {
                    pkgElem.setVersion(version);
                    pkgInfoSvc.savePkgElement(pkgElem);
                    logger.println(
                        "Updated package element version for system relationship '"
                            + relName
                            + "'");
                  } else {
                    logger.println(
                        "Could not find package element for system relationship '" + relName + "'");
                  }
                } catch (Exception e) {
                  log.error(PSExceptionUtils.getMessageForLog(e));
                }
              });
    } catch (Exception e) {
      respType = PSPluginResponse.EXCEPTION;
      respMsg = e.getMessage();
      log.error(PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
      log.debug(logger);
      log.error(logger);
    }

    return new PSPluginResponse(respType, respMsg);
  }
}
