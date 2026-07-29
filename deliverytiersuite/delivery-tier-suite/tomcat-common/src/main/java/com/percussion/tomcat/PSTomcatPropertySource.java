// REFACTORED: CP-JAVA11
/*
 * Copyright 1999-2023 Percussion Software, Inc.
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

package com.percussion.tomcat;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.Properties;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Tomcat {@link org.apache.tomcat.util.IntrospectionUtils.PropertySource} implementation that reads
 * Percussion-specific properties from {@code ${catalina.home}/conf/perc/perc-catalina.properties}
 * and exposes them by name. Lookups that miss in the loaded file return {@code null}.
 */
public class PSTomcatPropertySource
    implements org.apache.tomcat.util.IntrospectionUtils.PropertySource {

  private static final Logger logger = LogManager.getLogger(PSTomcatPropertySource.class);

  /***
   * Default constructor
   */
  public PSTomcatPropertySource() {
    logger.debug("Initialized..");
    /* System.out.println("----Environment ----");
    for(String key : System.getenv().keySet()){
        System.out.println(key + "=" + System.getenv().get(key));
    }
    System.out.println("----End Environment----");

    System.out.println("----System ----");
    System.getProperties().list(System.out);
    System.out.println("----End System----");
    */
  }

  @Override
  public String getProperty(String key) {
    return Optional.ofNullable(getProperties().getProperty(key)).orElse(null);
  }

  private Properties getProperties() {
    var catalinaBase = System.getProperty("catalina.home");
    if (catalinaBase == null) {
      logger.error("Unable to determine catalina.home! Is the environment set?");
      catalinaBase = "";
    }
    logger.debug("Got catalina.home: {}", catalinaBase);
    var props = new Properties();

    var percConfPath = Paths.get(catalinaBase, "conf", "perc", "perc-catalina.properties");

    try (var fs = new FileInputStream(percConfPath.toFile())) {
      props.load(fs);
    } catch (IOException exception) {
      logger.error(
          "Error reading: {} got error {}", percConfPath.toAbsolutePath(), exception.getMessage());
    }

    return props;
  }
}
