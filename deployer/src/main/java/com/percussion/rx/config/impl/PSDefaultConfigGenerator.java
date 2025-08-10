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
// REFACTORED: CP-JAVA11

package com.percussion.rx.config.impl;

import com.percussion.rx.config.IPSConfigHandler;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Generates the default configuration for the supplied config def file path.
 *
 * @author bjoginipally
 */
public class PSDefaultConfigGenerator {

  /** Private constructor to avoid instantiation. */
  private PSDefaultConfigGenerator() {}

  /**
   * Returns the singleton instance of this class.
   *
   * @return singleton instance
   */
  public static PSDefaultConfigGenerator getInstance() {
    if (msInstance == null) {
      msInstance = new PSDefaultConfigGenerator();
    }
    return msInstance;
  }

  /**
   * Generates the default configuration file and returns it as String for the supplied config def
   * file.
   *
   * @param publisherName The publisher name is added to the default configuration file as an
   *     attribute to the root element, must not be empty.
   * @param publisherPrefix The publisher prefix must not be empty. It will be added to the root
   *     element as a publisherPrefix attribute. The replacement names that do not start with this
   *     name are not processed.
   * @param solutionName The solution name is added as value of name attribute of element
   *     SolutionConfig, must not be empty.
   * @param configDefPath the path of the config definition file from which the default config is
   *     generated, must not be empty.
   * @return String representation of default config xml document, that gets built as per the <code>
   *     localConfig.xsd</code> schema.
   */
  public String generateDefaultConfig(
      String publisherName, String publisherPrefix, String solutionName, String configDefPath) {
    if (StringUtils.isBlank(configDefPath)) {
      throw new IllegalArgumentException("configDefPath must not be empty");
    }
    if (StringUtils.isBlank(publisherName)) {
      throw new IllegalArgumentException("publisherName must not be empty");
    }
    if (StringUtils.isBlank(publisherPrefix)) {
      throw new IllegalArgumentException("publisherPrefix must not be empty");
    }
    if (StringUtils.isBlank(solutionName)) {
      throw new IllegalArgumentException("solutionName must not be empty");
    }
    var packageName = publisherPrefix + "." + solutionName;
    msLogger.info("Generating default configuration file for package \"{}\"", packageName);
    String defaultConfig;

    var loader = new PSImplConfigLoader(configDefPath);
    var handlers = new ArrayList<IPSConfigHandler>();
    for (var bean : loader.getAllBeanNames()) {
      handlers.add(loader.getBean(bean));
    }
    var merger = new PSConfigMerger();
    var propertyDefs = merger.getPropertyDefs(handlers);
    var cfgDeNormalizer = PSConfigDeNormalizer.getInstance();
    // Log the errors if any and pass the error messages to normalizer to put in the output xml
    // file.
    List<String> errors = null;
    if (!propertyDefs.getSecond().isEmpty()) {
      errors = new ArrayList<>();
      msLogger.error(
          "The following errors occurred while generating the default configuration file for"
              + " package \"{}\"",
          packageName);
      for (var exception : propertyDefs.getSecond()) {
        msLogger.error(exception);
        errors.add(exception.getLocalizedMessage());
      }
    }
    defaultConfig =
        cfgDeNormalizer.getDeNormalizedXml(
            propertyDefs.getFirst(), errors, publisherName, publisherPrefix, solutionName);
    msLogger.info("Finished generating default configuration file for package \"{}\"", packageName);

    return defaultConfig;
  }

  /** The singleton instance of this class initialized in {@link #getInstance()} method. */
  private static PSDefaultConfigGenerator msInstance = null;

  /** The logger for this class. */
  private static final Logger msLogger = LogManager.getLogger("PSDefaultConfigGenerator");
}
