/*
 * Copyright 1999-2026 Percussion Software, Inc.
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

package com.percussion.ant.install;

import com.percussion.design.objectstore.PSJdbcDriverConfig;
import com.percussion.install.PSLogger;
import com.percussion.security.error.PSExceptionUtils;
import com.percussion.tablefactory.PSJdbcDbmsDef;
import com.percussion.util.PSProperties;
import com.percussion.utils.container.IPSContainerUtils;
import com.percussion.utils.container.IPSJndiDatasource;
import com.percussion.utils.container.PSContainerUtilsFactory;
import com.percussion.utils.container.jetty.PSJettyJndiDatasource;
import com.percussion.utils.jdbc.IPSDatasourceConfig;
import com.percussion.utils.jdbc.IPSDatasourceResolver;
import com.percussion.utils.jdbc.PSDatasourceConfig;
import com.percussion.utils.jdbc.PSJdbcUtils;
import com.percussion.xml.PSXmlDocumentBuilder;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tools.ant.BuildException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * PSConfigureDatasource is a class which configures the default datasource.
 *
 * <p>The rxrepository.properties file is used to gather the appropriate information, then the JNDI
 * datasource and datasource connection are configured.
 *
 * <p>On upgrade, {@code rxconfig/Server/config.xml} is preserved (not overwritten) and may predate
 * product drivers such as H2 (#548) and PostgreSQL (#1500). Before configuring the datasource this
 * task merges any missing product {@code PSXJdbcDriverConfig} entries so the selected repository
 * driver can always be resolved.
 *
 * <br>
 * Example Usage: <br>
 *
 * <pre>
 *
 * First set the taskdef:
 *
 *  <code>
 *  &lt;taskdef name="configureDatasource"
 *              class="com.percussion.ant.PSConfigureDatasource"
 *              classpathref="INSTALL.CLASSPATH"/&gt;
 *  </code>
 *
 * Now use the task to configure the default Rhythmyx datasource.
 *
 *  <code>
 *  &lt;configureDatasource name="RhythmyxData"/&gt;
 *  </code>
 *
 * </pre>
 */
public class PSConfigureDatasource extends PSAction {
  /** Creates a new datasource configuration task. */
  public PSConfigureDatasource() {}

  private static final Logger log = LogManager.getLogger(PSConfigureDatasource.class);

  /**
   * Product JDBC driver catalog (driverName → className + containerTypeMapping). Order matches
   * {@code system/config/config.xml}. Keys are lower-cased for lookup.
   */
  private static final Map<String, ProductJdbcDriver> PRODUCT_JDBC_DRIVERS;

  static {
    Map<String, ProductJdbcDriver> map = new LinkedHashMap<>();
    putDriver(map, "oracle:thin", "oracle.jdbc.OracleDriver", "Oracle8");
    putDriver(map, "jtds:sqlserver", "net.sourceforge.jtds.jdbc.Driver", "MS SQLSERVER2000");
    putDriver(map, "db2", "com.ibm.db2.jcc.DB2Driver", "DB2");
    putDriver(map, "mysql", "com.mysql.jdbc.Driver", "MYSQL");
    putDriver(map, "sqlserver", "com.microsoft.sqlserver.jdbc.SQLServerDriver", "MSSQLSERVER");
    putDriver(map, "derby", "org.apache.derby.jdbc.EmbeddedDriver", "DERBY");
    // #548 H2 default embedded (Derby migration window retains derby entry)
    putDriver(map, PSJdbcUtils.H2_DRIVER, PSJdbcUtils.H2_DRIVER_CLASS, "H2");
    // #1500 PostgreSQL external repository
    putDriver(map, PSJdbcUtils.POSTGRES_DRIVER, PSJdbcUtils.POSTGRES_DRIVER_CLASS, "POSTGRES");
    PRODUCT_JDBC_DRIVERS = Collections.unmodifiableMap(map);
  }

  private static void putDriver(
      Map<String, ProductJdbcDriver> map, String driverName, String className, String typeMapping) {
    map.put(
        driverName.toLowerCase(Locale.ROOT),
        new ProductJdbcDriver(driverName, className, typeMapping));
  }

  public void execute() {
    PSProperties props = null;
    PSJdbcDbmsDef dbmsDef = null;
    List<IPSJndiDatasource> datasources = null;
    IPSJndiDatasource datasrc = null;

    try {
      PSLogger.logInfo("Configuring default datasource");

      // Load repository information from rxrepository.properties
      props = new PSProperties(getRootDir() + File.separator + getRepositoryLocation());
      // props.setProperty(PSJdbcDbmsDef.PWD_ENCRYPTED_PROPERTY, "Y");

      dbmsDef = new PSJdbcDbmsDef(props);

      String user = dbmsDef.getUserId();
      String pass = dbmsDef.getPassword();
      String driver = dbmsDef.getDriver();
      String server = dbmsDef.getServer();
      String datasource = getName();
      String jndiDatasource = "jdbc/" + datasource;
      String origin = dbmsDef.getSchema();
      String database = dbmsDef.getDataBase();

      PSLogger.logInfo("Default datasource is " + datasource);

      // Portable join: constant relative segments (URL-style '/' in the property is not a FS path).
      Path configPath = Path.of(getRootDir(), "rxconfig", "Server", "config.xml");
      String preferredClass = props.getProperty(PSJdbcDbmsDef.DB_DRIVER_CLASS_NAME_PROPERTY, "");
      ResolvedJdbcDriver resolved =
          ensureProductJdbcDriversAndResolve(configPath, driver, preferredClass);

      String containerTypeMap = resolved.containerTypeMapping();
      String driverClassName = resolved.className();

      if (containerTypeMap.trim().isEmpty() || driverClassName.trim().isEmpty()) {
        throw new Exception(
            "A matching driver configuration could not " + "be found for " + dbmsDef.getDriver());
      }

      // Construct default jndi datasource and add
      // can use the same datasource implementation, save differently
      datasrc =
          new PSJettyJndiDatasource(jndiDatasource, driver, driverClassName, server, user, pass);

      datasources = new ArrayList<>();
      datasources.add(datasrc);

      IPSContainerUtils containerUtils = PSContainerUtilsFactory.getInstance();
      containerUtils.setDatasources(datasources);

      PSLogger.logInfo("Configuring datasource connection for " + jndiDatasource);

      // Construct datasource connection and add
      PSDatasourceConfig dsConfig =
          new PSDatasourceConfig(datasource, jndiDatasource, origin, database);

      List<IPSDatasourceConfig> dsConfigs = new ArrayList<>();
      dsConfigs.add(dsConfig);

      IPSDatasourceResolver resolver = containerUtils.getDatasourceResolver();

      resolver.setDatasourceConfigurations(dsConfigs);
      resolver.setRepositoryDatasource(datasource);
      containerUtils.setDatasourceResolver(resolver);

      PSContainerUtilsFactory.getConfigurationContextInstance().save();

    } catch (Exception e) {
      PSLogger.logError(e.getMessage());
      log.error(PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));

      throw new BuildException(e.getLocalizedMessage());
    }
  }

  /**
   * Ensures {@code config.xml} contains all product {@code PSXJdbcDriverConfig} entries, then
   * resolves class name and container type mapping for {@code driverName}.
   *
   * <p>Package-private for unit tests. When the install is an upgrade, {@code config.xml} is
   * preserved and may lack H2/PostgreSQL entries introduced after the site was first installed.
   *
   * @param configXml path to {@code rxconfig/Server/config.xml}
   * @param driverName repository {@code DB_DRIVER_NAME} (e.g. {@code h2})
   * @param preferredClassName optional {@code DB_DRIVER_CLASS_NAME} from repository props; used when
   *     creating a missing product entry for this driver
   * @return resolved class name and container type mapping
   * @throws Exception if the config cannot be read/written or the driver is unknown
   */
  static ResolvedJdbcDriver ensureProductJdbcDriversAndResolve(
      Path configXml, String driverName, String preferredClassName) throws Exception {
    Objects.requireNonNull(configXml, "configXml");
    if (driverName == null || driverName.isBlank()) {
      throw new IllegalArgumentException("driverName may not be null or empty");
    }

    Document driverDoc;
    try (FileInputStream in = new FileInputStream(configXml.toFile())) {
      driverDoc = PSXmlDocumentBuilder.createXmlDocument(in, false);
    }

    Element jdbcDriverConfigs = findOrCreateJdbcDriverConfigs(driverDoc);
    boolean modified = false;

    for (ProductJdbcDriver product : PRODUCT_JDBC_DRIVERS.values()) {
      if (!hasDriverConfig(jdbcDriverConfigs, product.driverName())) {
        String className = product.className();
        // Prefer repository class for the driver we are configuring when supplied
        if (product.driverName().equalsIgnoreCase(driverName)
            && preferredClassName != null
            && !preferredClassName.isBlank()) {
          className = preferredClassName.trim();
        }
        Element el = driverDoc.createElement(PSJdbcDriverConfig.XML_NODE_NAME);
        el.setAttribute("driverName", product.driverName());
        el.setAttribute("className", className);
        el.setAttribute("containerTypeMapping", product.containerTypeMapping());
        jdbcDriverConfigs.appendChild(el);
        modified = true;
        PSLogger.logInfo(
            "Added missing JDBC driver configuration for "
                + product.driverName()
                + " to "
                + configXml);
      }
    }

    if (modified) {
      // Ensure parent directory exists (should already) and write atomically-ish
      Path parent = configXml.getParent();
      if (parent != null) {
        Files.createDirectories(parent);
      }
      try (OutputStream out = new FileOutputStream(configXml.toFile())) {
        PSXmlDocumentBuilder.write(driverDoc, out);
      }
    }

    // Re-resolve from document (includes pre-existing + newly added)
    NodeList driverNodes = driverDoc.getElementsByTagName(PSJdbcDriverConfig.XML_NODE_NAME);
    int len = driverNodes.getLength();
    for (int i = 0; i < len; i++) {
      Element driverNode = (Element) driverNodes.item(i);
      PSJdbcDriverConfig dc = new PSJdbcDriverConfig(driverNode);
      if (dc.getDriverName().equalsIgnoreCase(driverName)) {
        return new ResolvedJdbcDriver(dc.getClassName(), dc.getContainerTypeMapping());
      }
    }

    // Last resort: known product map without requiring a successful XML parse of the entry
    ProductJdbcDriver product = PRODUCT_JDBC_DRIVERS.get(driverName.toLowerCase(Locale.ROOT));
    if (product != null) {
      String className =
          preferredClassName != null && !preferredClassName.isBlank()
              ? preferredClassName.trim()
              : product.className();
      return new ResolvedJdbcDriver(className, product.containerTypeMapping());
    }

    return new ResolvedJdbcDriver("", "");
  }

  private static Element findOrCreateJdbcDriverConfigs(Document doc) {
    NodeList nodes = doc.getElementsByTagName("JdbcDriverConfigs");
    if (nodes.getLength() > 0) {
      return (Element) nodes.item(0);
    }
    Element root = doc.getDocumentElement();
    if (root == null) {
      throw new IllegalStateException("config.xml has no document element");
    }
    Element created = doc.createElement("JdbcDriverConfigs");
    root.appendChild(created);
    PSLogger.logInfo("Created missing JdbcDriverConfigs element in server config.xml");
    return created;
  }

  private static boolean hasDriverConfig(Element jdbcDriverConfigs, String driverName) {
    NodeList children = jdbcDriverConfigs.getElementsByTagName(PSJdbcDriverConfig.XML_NODE_NAME);
    for (int i = 0; i < children.getLength(); i++) {
      Element el = (Element) children.item(i);
      String name = el.getAttribute("driverName");
      if (name != null && name.equalsIgnoreCase(driverName)) {
        return true;
      }
    }
    return false;
  }

  /** Immutable product driver definition. */
  record ProductJdbcDriver(String driverName, String className, String containerTypeMapping) {}

  /** Resolved driver class + container type mapping for datasource setup. */
  record ResolvedJdbcDriver(String className, String containerTypeMapping) {}

  /*************************************************************************
   * Property Accessors and Mutators
   *************************************************************************/

  /**
   * Accessor for the repository location.
   *
   * @return the repository location path
   */
  public String getRepositoryLocation() {
    return m_strRepositoryLocation;
  }

  /**
   * Accessor for the server configuration location.
   *
   * @return the server configuration location path
   */
  public String getServerConfigLocation() {
    return m_strServerConfigLocation;
  }

  /**
   * Accessor for the Jndi datasource configuration location.
   *
   * @return the JNDI datasource configuration location path
   */
  public String getJndiDatasourceConfigLocation() {
    return m_strJndiDatasourceConfigLocation;
  }

  /**
   * Accessor for the login configuration location.
   *
   * @return the login configuration location path
   */
  public static String getLoginConfigLocation() {
    return ms_strLoginConfigLocation;
  }

  /**
   * Returns the name of the default Rhythmyx repository datasource configuration.
   *
   * @return the datasource configuration name
   */
  public String getName() {
    return m_strName;
  }

  /**
   * Sets the name of the default Rhythmyx repository datasource configuration.
   *
   * @param dsConfig the datasource configuration name
   */
  public void setName(String dsConfig) {
    m_strName = dsConfig;
  }

  /**************************************************************************
   * Properties
   *************************************************************************/

  /** The repository location, relative to the Rhythmyx root. */
  private String m_strRepositoryLocation = "rxconfig/Installer/rxrepository.properties";

  /** The server configuration location, relative to the Rhythmyx root. */
  private String m_strServerConfigLocation = "rxconfig/Server/config.xml";

  /** The spring configuration location, relative to the Rhythmyx root. */
  private String m_strSpringConfigLocation =
      "AppServer/server/rx/deploy/rxapp.ear/rxapp.war/WEB-INF/config/spring/" + "server-beans.xml";

  /** The Jndi datasource configuration location, relative to the Rhythmyx root. */
  private String m_strJndiDatasourceConfigLocation = "AppServer/server/rx/deploy/rx-ds.xml";

  /** The login configuration location, relative to the Rhythmyx root. */
  private static String ms_strLoginConfigLocation = "AppServer/server/rx/conf/login-config.xml";

  /** The name of the datasource configuration, defaults to RhythmyxData. */
  private String m_strName = "RhythmyxData";
}
