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

import com.percussion.util.PSProperties;
import com.percussion.utils.container.config.ContainerConfig;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Collectors;

/**
 * Abstract connectors.
 */
public class PSAbstractConnectors implements IPSConnectorInfo<IPSConnector>, ContainerConfig {

  /** Connectors. */
  private List<IPSConnector> connectors = new ArrayList<>();

  /** Connector file context. */
  private Path connectorFileContext;

  /** Default constructor. */
  public PSAbstractConnectors() {}

  /**
   * Constructor.
   *
   * @param connectorFileContet the context.
   */
  public PSAbstractConnectors(Path connectorFileContet) {
    this.connectorFileContext = connectorFileContet;
  }

  /**
   * Convert to comma string.
   *
   * @param string the collection.
   * @return the string.
   */
  protected static String toCommaString(Collection<String> string) {
    return string.stream().collect(Collectors.joining(","));
  }

  /**
   * Save properties file.
   *
   * @param filePath the path.
   * @param props the properties.
   */
  protected static void savePropertiesFile(Path filePath, Map<String, String> props) {
    if (filePath != null) {
      Properties origProps = loadPropertiesFile(filePath);
      origProps.putAll(props);
      if (!Files.exists(filePath)) {
        try {
          Files.createFile(filePath);
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      }

      try (OutputStream os = Files.newOutputStream(filePath)) {
        origProps.store(os, null);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
  }

  /**
   * Load properties file.
   *
   * @param filePath the path.
   * @return the properties.
   */
  protected static Properties loadPropertiesFile(Path filePath) {
    PSProperties props = new PSProperties();
    if (filePath != null) {
      if (Files.exists(filePath)) {
        try (InputStream is = Files.newInputStream(filePath)) {
          props.load(is);
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      }
    }
    return props;
  }

  /**
   * Save properties.
   *
   * @param properties the properties.
   * @param propertyPath the path.
   */
  protected void saveProperties(Map<String, String> properties, Path propertyPath) {
    savePropertiesFile(propertyPath, properties);
  }

  /**
   * Load properties.
   *
   * @param propertyPath the path.
   * @return the properties.
   */
  protected Map<String, String> loadProperties(Path propertyPath) {
    Map<String, String> properties = new HashMap<>();
    Properties prop = loadPropertiesFile(propertyPath);
    properties.putAll(
        prop.entrySet().stream()
            .collect(
                Collectors.toMap(
                    e -> String.valueOf(e.getKey()), e -> String.valueOf(e.getValue()))));
    return properties;
  }

  @Override
  public Path getConnectorFileContext() {
    return connectorFileContext;
  }

  @Override
  public List<IPSConnector> getConnectors() {
    return this.connectors;
  }

  @Override
  public final void setConnectors(List<IPSConnector> connectors) {
    this.connectors = connectors;
  }

  /**
   * Get the HTTPS connector.
   *
   * @return the connector.
   */
  public final Optional<IPSConnector> getHttpsConnector() {
    return this.connectors.stream().filter(IPSConnector::isHttps).findFirst();
  }

  /**
   * Get the HTTP connector.
   *
   * @return the connector.
   */
  public final Optional<IPSConnector> getHttpConnector() {
    return this.connectors.stream().filter(IPSConnector::isHttp).findFirst();
  }

  /**
   * Merge connectors.
   *
   * @param existingConnectors the existing connectors.
   */
  protected final void mergeConnectors(List<IPSConnector> existingConnectors) {
    List<IPSConnector> newConnectors = new ArrayList<>();
    for (IPSConnector connector : existingConnectors) {
      IPSConnector connectorToAdd = connector;
      if (connector.isHttp()) {
        IPSConnector con =
            getHttpConnector()
                .map(
                    c -> {
                      c.copyFrom(connector);
                      return c;
                    })
                .orElse(connector);
        con.copyFrom(connector);
        connectorToAdd = con;
      } else if (connector.isHttps()) {
        connectorToAdd =
            getHttpsConnector()
                .map(
                    c -> {
                      c.copyFrom(connector);
                      return c;
                    })
                .orElse(connector);
      }

      newConnectors.add(connectorToAdd);
    }
    this.connectors = newConnectors;
  }

  @Override
  public String toString() {
    return "PSAbstractConnectors{" + "connectors=" + connectors + '}';
  }
}
