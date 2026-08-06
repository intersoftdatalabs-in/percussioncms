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
package com.percussion.delivery.utils.spring;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.support.XmlWebApplicationContext;

/**
 * Configurable Spring {@link XmlWebApplicationContext} used as the {@code contextClass} in DTS
 * service {@code web.xml} files.
 *
 * <p>Resolves the Spring bean definition location from, in order:
 *
 * <ol>
 *   <li>{@code ${catalina.base}/conf/perc/perc-context.properties} ({@code contextLocation})
 *   <li>{@code /WEB-INF/perc-context.properties} on the webapp
 *   <li>Default {@code /WEB-INF/beans.xml}
 * </ol>
 *
 * <p>Restored to {@code src/main} after it was accidentally left only under the test package, which
 * caused {@code ClassNotFoundException:
 * com.percussion.delivery.utils.spring.PSConfigurableApplicationContext} on every DTS WAR deploy.
 */
@Configuration
public class PSConfigurableApplicationContext extends XmlWebApplicationContext {

  private static final String DEFAULT_CONTEXT_CONFIG = "/WEB-INF/beans.xml";
  private static final String PERC_CONTEXT_PROPS = "/WEB-INF/perc-context.properties";
  private static final String PERC_CONTEXT_PROPS_USER_REL = "conf/perc/perc-context.properties";
  private static final String PERC_CONTEXT_LOC = "contextLocation";
  private static final String CATALINA_BASE = "catalina.base";

  private static final Logger log = LogManager.getLogger(PSConfigurableApplicationContext.class);

  /** Public no-arg constructor required for Tomcat/Spring {@code contextClass} reflection. */
  public PSConfigurableApplicationContext() {
    super();
  }

  /**
   * Initialize the bean definition reader used for loading the bean definitions of this context.
   *
   * @param beanDefinitionReader the bean definition reader used by this context
   */
  @Override
  protected void initBeanDefinitionReader(XmlBeanDefinitionReader beanDefinitionReader) {
    beanDefinitionReader.setValidationMode(XmlBeanDefinitionReader.VALIDATION_NONE);
    beanDefinitionReader.setValidating(false);

    super.initBeanDefinitionReader(beanDefinitionReader);
  }

  /**
   * Convenience method for unit tests that need to exercise multiple context locations. Call prior
   * to loading the context in a given test.
   *
   * @param location The location to be set, e.g. {@code /WEB-INF/beans_mongodb.xml}
   */
  public static void switchContextLocation(String location) throws IOException, URISyntaxException {

    Properties p = new Properties();
    try (InputStream rs =
        PSConfigurableApplicationContext.class.getResourceAsStream(PERC_CONTEXT_PROPS)) {
      if (rs == null) {
        throw new IOException("Resource not found: " + PERC_CONTEXT_PROPS);
      }
      p.load(rs);
      p.setProperty(PERC_CONTEXT_LOC, location);
    }

    URL url = PSConfigurableApplicationContext.class.getResource(PERC_CONTEXT_PROPS);
    if (url == null) {
      throw new IOException("Resource not found: " + PERC_CONTEXT_PROPS);
    }
    try (OutputStream fs = Files.newOutputStream(Path.of(url.toURI()))) {
      p.store(fs, null);
    }
  }

  @Override
  public String[] getConfigLocations() {
    return getDefaultConfigLocations();
  }

  @Override
  protected String[] getDefaultConfigLocations() {

    Properties props = new Properties();
    String targetContext = null;

    String tomcatBase = System.getProperty(CATALINA_BASE);

    // User-configured properties under catalina.base (portable Path join)
    if (tomcatBase != null && !tomcatBase.isBlank()) {
      Path userProps = Path.of(tomcatBase).resolve(PERC_CONTEXT_PROPS_USER_REL);
      if (Files.isRegularFile(userProps)) {
        try (InputStream fs = Files.newInputStream(userProps)) {
          props.load(fs);
          targetContext = props.getProperty(PERC_CONTEXT_LOC, null);
        } catch (IOException e) {
          log.info(e.getMessage());
        }
      }
    }

    if (targetContext == null) {
      // WEB-INF properties
      try {
        var servletContext = this.getServletContext();
        if (servletContext != null) {
          try (InputStream in = servletContext.getResourceAsStream(PERC_CONTEXT_PROPS)) {
            if (in != null) {
              props.load(in);
              targetContext = props.getProperty(PERC_CONTEXT_LOC, null);
              log.info("Selected {} from {}", targetContext, PERC_CONTEXT_LOC);
            }
          }
        }
      } catch (IOException e) {
        log.info(e.getMessage());
      }
    }

    // Fall back to defaults if none of the properties are found.
    if (targetContext == null || targetContext.isEmpty()) {
      log.info(
          "Unable to find a configured ContextLocation - selecting default: {}",
          DEFAULT_CONTEXT_CONFIG);
      targetContext = DEFAULT_CONTEXT_CONFIG;
    }

    return new String[] {targetContext};
  }
}
