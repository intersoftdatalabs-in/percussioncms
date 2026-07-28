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
package com.percussion.services.datasource.impl;

import com.percussion.services.PSBaseServiceLocator;
import com.percussion.utils.container.IPSHibernateDialectConfig;
import com.percussion.utils.container.PSContainerUtilsFactory;
import com.percussion.utils.jdbc.IPSConnectionInfo;
import com.percussion.utils.jdbc.IPSDatasourceConfig;
import com.percussion.utils.jdbc.IPSDatasourceManager;
import com.percussion.utils.jdbc.IPSDatasourceResolver;
import com.percussion.utils.jdbc.PSConnectionDetail;
import com.percussion.utils.jdbc.PSJdbcConnectionDiagnostics;
import com.percussion.utils.jdbc.PSJdbcUtils;
import com.percussion.utils.jdbc.PSMissingDatasourceConfigException;
import com.percussion.utils.jndi.PSJndiObjectLocator;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

import javax.naming.NamingException;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * Implementation of the datasource manager service using modern Java 11 patterns.
 *
 * <p>This manager provides comprehensive database connection management with
 * Optional-based safe access, Stream API for efficient processing, and enhanced
 * validation for robust datasource operations.</p>
 *
 * @author Percussion Software
 * @since 6.0
 */
public class PSDatasourceManager implements IPSDatasourceManager {

    private static final Logger log = LogManager.getLogger(PSDatasourceManager.class);

    /**
     * Default hibernate properties for backward compatibility.
     */
    private final Properties defaultHibernateProperties;

    /**
     * Hibernate dialect configuration.
     */
    private IPSHibernateDialectConfig dialectConfig;

    /**
     * Thread-safe reference to the datasource resolver.
     */
    private static final AtomicReference<IPSDatasourceResolver> DATASOURCE_RESOLVER_REF =
        new AtomicReference<>();

    /** Emit pool/H2 diagnostics at most once per JVM (startup noise control). */
    private static final AtomicBoolean REPOSITORY_DIAG_LOGGED = new AtomicBoolean(false);

    /**
     * Default constructor that initializes default hibernate properties.
     */
    public PSDatasourceManager() {
        defaultHibernateProperties = createDefaultHibernateProperties();
    }

    /**
     * Create default hibernate properties with backward compatibility settings.
     *
     * @return Properties configured with default values
     */
    private Properties createDefaultHibernateProperties() {
        var props = new Properties();
        props.put("hibernate.allow_update_outside_transaction", "true");
        return props;
    }

    /**
     * Get the dialect configuration safely.
     *
     * @return Optional containing the dialect config, or empty if not set
     */
    public Optional<IPSHibernateDialectConfig> getDialectConfigSafely() {
        return Optional.ofNullable(dialectConfig);
    }

    /**
     * Get the dialect configuration.
     *
     * @return The dialect config, may be null
     * @deprecated Use {@link #getDialectConfigSafely()} for null-safe access
     */
    @Deprecated
    public IPSHibernateDialectConfig getDialectCfg() {
        return dialectConfig;
    }

    /**
     * Set the dialect configuration with validation.
     *
     * @param dialectConfig The dialect config, may not be null
     */
    public void setDialectCfg(IPSHibernateDialectConfig dialectConfig) {
        this.dialectConfig = Objects.requireNonNull(dialectConfig,
            "dialectConfig may not be null");
    }

    @Override
    public PSConnectionDetail getConnectionDetail(IPSConnectionInfo info)
            throws NamingException, SQLException {

        var dsConfig = getDatasourceResolver().resolveDatasource(info);
        if (dsConfig == null) {
            var dsName = Optional.ofNullable(info)
                .map(IPSConnectionInfo::getDataSource)
                .orElse("(default repository)");
            throw new PSMissingDatasourceConfigException(dsName);
        }
        var dsName = dsConfig.getDataSource();
        var url = getConnectionUrl(dsName);
        var driver = PSJdbcUtils.getDriverFromUrl(url);
        var database = dsConfig.getDatabase();

        // Once: prove whether Hikari configured URL has NON_KEYWORDS and whether the
        // live connection accepts unquoted VALUE (package install / Hibernate probe).
        // DatabaseMetaData.getURL() alone is misleading on H2 (settings stripped).
        if (info == null && REPOSITORY_DIAG_LOGGED.compareAndSet(false, true)) {
            try {
                var ds = getDatasource(dsName);
                log.info(
                    "Repository datasource diagnostics (dsName={}): {}",
                    dsName,
                    PSJdbcConnectionDiagnostics.describeDataSource(ds));
            } catch (Exception e) {
                log.warn(
                    "Repository datasource diagnostics unavailable for {}: {}",
                    dsName,
                    e.toString());
            }
        }

        return new PSConnectionDetail(dsName, driver, database,
            dsConfig.getOrigin(), url);
    }

    /**
     * Get connection URL for the specified datasource with try-with-resources.
     *
     * @param dsName The datasource name
     * @return The connection URL
     * @throws NamingException if datasource lookup fails
     * @throws SQLException if connection fails
     */
    protected String getConnectionUrl(String dsName) throws NamingException, SQLException {
        try (var conn = getDbConnection(dsName)) {
            return conn.getMetaData().getURL();
        }
    }

    @Override
    public Connection getDbConnection(IPSConnectionInfo info)
            throws NamingException, SQLException {

        var dsConfig = resolveDatasource(info);
        var dsName = dsConfig.getDataSource();
        var conn = getDbConnection(dsName);

        // Wrap Oracle connections for enhanced functionality
        if (conn.getMetaData().getURL().contains("oracle")) {
            conn = new PSOracleConnectionWrapper(conn);
        }

        // Set catalog if specified
        configureCatalog(conn, dsConfig.getDatabase());

        return conn;
    }

    /**
     * Configure database catalog if specified.
     *
     * @param conn The connection to configure
     * @param dbName The database name, may be null or empty
     * @throws SQLException if catalog configuration fails
     */
    private void configureCatalog(Connection conn, String dbName) throws SQLException {
        if (StringUtils.isNotBlank(dbName) && !dbName.equals(conn.getCatalog())) {
            conn.setCatalog(dbName);
        }
    }

    @Override
    public List<String> getDatasources() {
        return getDatasourceResolver().getDatasourceConfigurations()
            .stream()
            .map(IPSDatasourceConfig::getName)
            .collect(Collectors.toList());
    }

    @Override
    public String getRepositoryDatasource() {
        return getDatasourceResolver().getRepositoryDatasource();
    }

    /**
     * Get a database connection from the specified datasource.
     *
     * @param dsName The name of the datasource, assumed not null or empty
     * @return A connection, never null
     * @throws NamingException if the datasource is not found
     * @throws SQLException on other database errors
     */
    private Connection getDbConnection(String dsName)
            throws NamingException, SQLException {
        return getDatasource(dsName).getConnection();
    }

    /**
     * Lookup the specified datasource with enhanced error handling.
     *
     * @param dsName The datasource name, assumed not null or empty
     * @return The datasource, never null
     * @throws NamingException if the datasource cannot be resolved
     */
    private DataSource getDatasource(String dsName) throws NamingException {
        var locator = new PSJndiObjectLocator(dsName);
        return locator.lookupDataSource();
    }

    /**
     * Resolve the connection info to a datasource config with enhanced validation.
     *
     * @param info The info to resolve, may be null
     * @return The config, never null
     * @throws PSMissingDatasourceConfigException if the info cannot be resolved
     */
    private IPSDatasourceConfig resolveDatasource(IPSConnectionInfo info)
            throws PSMissingDatasourceConfigException {

        var dsConfig = getDatasourceResolver().resolveDatasource(info);

        if (dsConfig == null) {
            var dsName = Optional.ofNullable(info)
                .map(IPSConnectionInfo::getDataSource)
                .orElse("");
            throw new PSMissingDatasourceConfigException(dsName);
        }

        return dsConfig;
    }

    public Session getHibernateSession() {
        var sessionFactory = (SessionFactory) PSBaseServiceLocator.getBean("sys_sessionFactory");
        return sessionFactory.openSession();
    }

    /**
     * Get the datasource resolver using thread-safe lazy initialization.
     *
     * @return The datasource resolver, never null
     */
    private IPSDatasourceResolver getDatasourceResolver() {
        return DATASOURCE_RESOLVER_REF.updateAndGet(existing ->
            existing != null ? existing :
            PSContainerUtilsFactory.getInstance().getDatasourceResolver());
    }

    @Override
    public Properties getHibernateProperties(IPSConnectionInfo info)
            throws NamingException, SQLException {

        var props = new Properties();
        props.putAll(defaultHibernateProperties);

        if (info != null) {
            configureHibernatePropertiesForConnection(props, info);
        }

        return props;
    }

    /**
     * Configure hibernate properties for a specific connection.
     *
     * @param props The properties to configure
     * @param info The connection info
     * @throws NamingException if datasource lookup fails
     * @throws SQLException if connection details cannot be obtained
     */
    private void configureHibernatePropertiesForConnection(Properties props, IPSConnectionInfo info)
            throws NamingException, SQLException {

        var connDetail = getConnectionDetail(info);

        // Configure datasource name with JNDI prefix
        configureDatasourceName(props, connDetail);

        // Configure catalog and schema
        configureDatabaseProperties(props, connDetail);

        // Configure dialect
        configureDialect(props, connDetail);

        // Configure database-specific properties
        configureDatabaseSpecificProperties(props, connDetail);
    }

    /**
     * Configure datasource name with JNDI prefix.
     */
    private void configureDatasourceName(Properties props, PSConnectionDetail connDetail) {
        var dsName = connDetail.getDatasourceName();
        var jndiPrefix = PSJndiObjectLocator.getPrefix();

        if (StringUtils.isNotBlank(jndiPrefix)) {
            dsName = jndiPrefix + dsName;
        }

        props.setProperty("hibernate.connection.datasource", dsName);
    }

    /**
     * Configure database catalog and schema properties.
     */
    private void configureDatabaseProperties(Properties props, PSConnectionDetail connDetail) {
        Optional.ofNullable(connDetail.getDatabase())
            .filter(StringUtils::isNotBlank)
            .ifPresent(catalog -> props.setProperty("hibernate.default_catalog", catalog));

        Optional.ofNullable(connDetail.getOrigin())
            .filter(StringUtils::isNotBlank)
            .ifPresent(schema -> props.setProperty("hibernate.default_schema", schema));
    }

    /**
     * Configure Hibernate dialect with validation.
     */
    private void configureDialect(Properties props, PSConnectionDetail connDetail) {
        var driver = connDetail.getDriver();
        var dialect = getDialectConfigSafely()
            .flatMap(config -> config instanceof com.percussion.services.datasource.PSHibernateDialectConfig ?
                ((com.percussion.services.datasource.PSHibernateDialectConfig) config).getDialectClassNameSafely(driver) :
                Optional.ofNullable(config.getDialectClassName(driver)))
            .orElseThrow(() -> new RuntimeException(
                "Cannot determine Hibernate SQL dialect for driver: " + driver));

        props.setProperty("hibernate.dialect", dialect);
    }

    /**
     * Configure database-specific properties.
     */
    private void configureDatabaseSpecificProperties(Properties props, PSConnectionDetail connDetail) {
        var driver = connDetail.getDriver();

        // Set transaction isolation for DB2
        if (PSJdbcUtils.DB2.equalsIgnoreCase(driver)) {
            props.setProperty("hibernate.connection.isolation",
                PSJdbcUtils.TRANSACTION_READ_UNCOMMITTED_VALUE);
        }
    }

    public Properties getDefaultHibernateProperties() {
        return new Properties(defaultHibernateProperties); // Return defensive copy
    }

    /**
     * Set default hibernate properties with validation.
     *
     * @param properties The properties to set, may not be null
     */
    public void setDefaultHibernateProperties(Properties properties) {
        Objects.requireNonNull(properties, "properties may not be null");

        defaultHibernateProperties.clear();
        defaultHibernateProperties.putAll(properties);
    }

    @Override
    public String toString() {
        return String.format("PSDatasourceManager{dialectConfig=%s, defaultPropsCount=%d}",
            getDialectConfigSafely().map(Object::getClass).map(Class::getSimpleName).orElse("null"),
            defaultHibernateProperties.size());
    }
}

