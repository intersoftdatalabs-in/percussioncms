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
package com.percussion.pso.utils;

// REFACTORED: CP-JAVA11
import com.percussion.security.error.PSExceptionUtils;
import com.percussion.server.PSServer;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Properties;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/***
 * Contains the basic proxy client configuration
 *
 * This file is read from rxconfig/Server/clientproxy.properties
 * proxyserver=
 * proxyport=
 *
 * @author natechadwick
 *
 * <p>This file is read from rxconfig/Server/clientproxy.properties proxyserver= proxyport=
 *
 * @author natechadwick
 */
public class HTTPProxyClientConfig {

  private static final Logger log = LogManager.getLogger(HTTPProxyClientConfig.class);

  private String proxyServer;
  private String proxyPort;

  /**
   * Sets the proxy server.
   *
   * @param proxyServer the proxy server
   */
  public void setProxyServer(String proxyServer) {
    this.proxyServer = proxyServer;
  }

  /**
   * Returns the proxy server.
   *
   * @return the result
   */
  public String getProxyServer() {
    return proxyServer;
  }

  /**
   * Sets the proxy port.
   *
   * @param proxyPort the proxy port
   */
  public void setProxyPort(String proxyPort) {
    this.proxyPort = proxyPort;
  }

  /**
   * Returns the proxy port.
   *
   * @return the result
   */
  public String getProxyPort() {
    return proxyPort;
  }

  /***
   * Loads the properties file and initializes the properties.  This could be made more efficient by
   * moving it out of the constructor but this way the file can be changed without restarting the server.
   */
  public HTTPProxyClientConfig() {
    String propFile =
        PSServer.getRxFile(PSServer.BASE_CONFIG_DIR + "/Server/clientproxy.properties");
    Properties props = new Properties();
    try {
      props.load(new FileInputStream(propFile));

      if (props.containsKey("proxyserver")) {
        this.proxyServer = props.getProperty("proxyserver").trim();
      } else {
        this.proxyServer = "";
      }

      if (props.containsKey("proxyport")) {
        this.proxyPort = props.getProperty("proxyport").trim();
      } else {
        this.proxyPort = "";
      }

    } catch (FileNotFoundException e) {
      log.debug(
          PSServer.BASE_CONFIG_DIR
              + "/Server/clientproxy.properties Configuration file not found.");
    } catch (Exception e) {
      log.error(PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
    }
  }
}
