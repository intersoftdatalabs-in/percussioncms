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
package com.percussion.services.schedule.impl;

import com.percussion.services.utils.general.PSServiceConfigurationBean;
import com.percussion.util.PSSqlHelper;
import com.percussion.utils.jdbc.IPSDatasourceManager;
import com.percussion.utils.jdbc.PSConnectionDetail;

import java.sql.SQLException;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import javax.naming.NamingException;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.impl.StdSchedulerFactory;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;

/**
 * Bean to create and configure a Quartz scheduler with Java 11 enhancements.
 * Always returns the same scheduler object with enhanced type safety and error handling.
 *
 * @see Scheduler
 * @author Andriy Palamarchuk
 */
public class PSSchedulerBean implements FactoryBean<Scheduler>, InitializingBean {

    @Override
    public void afterPropertiesSet() throws SchedulerException {
        validateBeanProperties();
        finishConfiguration();
        setConnectionProviderDatasourceManager();

        var factory = new StdSchedulerFactory();
        var configBean = getConfigurationBean();
        if (configBean != null) {
            configBean.getQuartzProperties().ifPresent(props -> m_quartzProperties.putAll(props));
        }
        factory.initialize(m_quartzProperties);
        m_scheduler = factory.getScheduler();

        if (ms_log.isDebugEnabled()) {
            ms_log.debug("Quartz properties: {}", m_quartzProperties);
        }
    }

    /**
     * Shuts down the quartz scheduler. This will be called from spring
     * via "destroy-method" XML attribute. See beans.xml.
     *
     * @throws SchedulerException if shutdown fails
     * @author adamgent
     * @author peterfrontiero
     */
    public void destroy() throws SchedulerException {
        Optional.ofNullable(m_scheduler)
                .ifPresent(scheduler -> {
                    try {
                        scheduler.shutdown();
                    } catch (SchedulerException e) {
                        ms_log.error("Failed to shutdown scheduler: {}", e.getMessage(), e);
                        throw new RuntimeException("Scheduler shutdown failed", e);
                    }
                });
    }

    /**
     * Initializes PSRhythmyxConnectionProvider with the current
     * datasource manager.
     */
    void setConnectionProviderDatasourceManager() {
        PSRhythmyxConnectionProvider.setDatasourceManager(m_datasourceManager);
    }

    /**
     * Finishes generating Quartz configuration.
     */
    private void finishConfiguration() {
        configureDriverDelegate();
        qualifyTableName();
    }

    /**
     * Sets the "tablePrefix" Quartz job store property, so that Quartz generates
     * SQL with fully qualified table names.
     */
    private void qualifyTableName() {
        var tablePrefix = Optional.ofNullable(m_quartzProperties.getProperty(TABLE_PREFIX_PROPERTY))
                .filter(StringUtils::isNotBlank)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Quartz property " + TABLE_PREFIX_PROPERTY + " must have a non-empty value"));

        var connectionDetail = getConnectionDetail();
        var qualifiedPrefix = PSSqlHelper.qualifyTableName(
                tablePrefix,
                connectionDetail.getDatabase(),
                connectionDetail.getOrigin(),
                connectionDetail.getDriver());

        m_quartzProperties.setProperty(TABLE_PREFIX_PROPERTY, qualifiedPrefix);
    }

    /**
     * Configures driver delegate property.
     * Quartz has a number of database driver delegates, which generate
     * database-specific SQL.
     */
    private void configureDriverDelegate() {
        var connectionDetail = getConnectionDetail();
        var driver = connectionDetail.getDriver();

        var delegateClass = Optional.ofNullable(resolveDriverDelegateClass(driver))
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unrecognized database driver: \"" + driver + "\""));

        m_quartzProperties.put("org.quartz.jobStore.driverDelegateClass", delegateClass);
    }

    /**
     * Map a JDBC driver name (or URL-derived form such as {@code h2:file}) to a Quartz JDBC job
     * store delegate class name.
     *
     * <p>H2 file URLs are {@code jdbc:h2:file:...}; if {@link
     * com.percussion.utils.jdbc.PSJdbcUtils#getDriverFromUrl(String)} falls through without an
     * explicit map entry it can yield {@code h2:file}. Normalize that (and {@code h2:mem}, {@code
     * h2:tcp}) to the H2 delegate. PostgreSQL and the default embedded H2 use Quartz's standard
     * JDBC delegate (#548 / #1500).
     *
     * <p>Package-visible for unit tests.
     *
     * @param driver JDBC driver name from connection detail; may be null
     * @return fully qualified Quartz delegate class name, or null if unknown
     */
    static String resolveDriverDelegateClass(String driver) {
        if (StringUtils.isBlank(driver)) {
            return null;
        }
        var key = driver.trim().toLowerCase(java.util.Locale.ROOT);
        var exact = DRIVER_DELEGATES.get(key);
        if (exact != null) {
            return exact;
        }
        // jdbc:h2:file:..., jdbc:h2:mem:..., jdbc:h2:tcp:... → treat as H2
        if (key.equals("h2") || key.startsWith("h2:")) {
            return DRIVER_DELEGATES.get("h2");
        }
        // postgres alias and any postgresql: subprotocol form
        if (key.equals("postgres") || key.startsWith("postgresql")) {
            return DRIVER_DELEGATES.get("postgresql");
        }
        return null;
    }

    /**
     * Database connection detail for the repository data source.
     *
     * @return the database connection detail, never null
     * @throws RuntimeException if connection details cannot be retrieved
     */
    private PSConnectionDetail getConnectionDetail() {
        try {
            return m_datasourceManager.getConnectionDetail(null);
        } catch (NamingException | SQLException e) {
            ms_log.error("Failed to get connection details: {}", e.getMessage(), e);
            throw new RuntimeException("Unable to retrieve database connection details", e);
        }
    }

    /**
     * Validates that all required bean properties have been set.
     *
     * @throws IllegalArgumentException if incomplete data was specified for the bean
     */
    private void validateBeanProperties() {
        if (m_quartzProperties == null || m_quartzProperties.isEmpty()) {
            throw new IllegalArgumentException("Quartz properties were not specified");
        }
        if (m_datasourceManager == null) {
            throw new IllegalArgumentException("Data source manager was not specified");
        }
    }

    /**
     * The Quartz properties to be passed to the scheduler factory.
     * Must be specified in the Spring bean configuration.
     *
     * @param quartzProperties the properties to set, cannot be null or empty
     */
    public void setQuartzProperties(Properties quartzProperties) {
        m_quartzProperties = Objects.requireNonNull(quartzProperties, "Quartz properties cannot be null");
    }

    public Properties getQuartzProperties() {
        return m_quartzProperties;
    }

    @Override
    public Scheduler getObject() {
        return m_scheduler;
    }

    @Override
    public Class<Scheduler> getObjectType() {
        return Scheduler.class;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    /**
     * The datasource manager to use for providing database connections
     * during Quartz initialization.
     *
     * @param datasourceManager the datasource manager to set, cannot be null
     */
    public void setDatasourceManager(IPSDatasourceManager datasourceManager) {
        m_datasourceManager = Objects.requireNonNull(datasourceManager, "Datasource manager cannot be null");
    }

    public PSServiceConfigurationBean getConfigurationBean() {
        return m_configurationBean;
    }

    public void setConfigurationBean(PSServiceConfigurationBean configurationBean) {
        m_configurationBean = Objects.requireNonNull(configurationBean, "Configuration bean cannot be null");
    }

    /**
     * Service configuration bean. It is fired by Spring bean configuration.
     */
    private PSServiceConfigurationBean m_configurationBean;

    /**
     * A mapping between JDBC driver names (lowercase) and Quartz database delegates.
     * H2 and PostgreSQL use the standard JDBC delegate (no product-specific SQL).
     */
    private static final Map<String, String> DRIVER_DELEGATES = Map.of(
            "jtds:sqlserver", "org.quartz.impl.jdbcjobstore.MSSQLDelegate",
            "inetdae7", "org.quartz.impl.jdbcjobstore.MSSQLDelegate",
            "sqlserver", "org.quartz.impl.jdbcjobstore.MSSQLDelegate",
            "oracle:thin", "org.quartz.impl.jdbcjobstore.oracle.OracleDelegate",
            "db2", "org.quartz.impl.jdbcjobstore.DB2v8Delegate",
            "derby", "org.quartz.impl.jdbcjobstore.StdJDBCDelegate",
            "mysql", "org.quartz.impl.jdbcjobstore.StdJDBCDelegate",
            // #548 default embedded H2; #1500 external PostgreSQL (use PG delegate, not Std)
            "h2", "org.quartz.impl.jdbcjobstore.StdJDBCDelegate",
            "postgresql", "org.quartz.impl.jdbcjobstore.PostgreSQLDelegate"
    );

    /**
     * Quartz table prefix property name.
     */
    private static final String TABLE_PREFIX_PROPERTY = "org.quartz.jobStore.tablePrefix";

    /**
     * The Quartz scheduler instance, not null after bean initialization.
     */
    private Scheduler m_scheduler;

    /**
     * Quartz configuration properties.
     */
    private Properties m_quartzProperties;

    /**
     * Datasource manager for database connections.
     */
    private IPSDatasourceManager m_datasourceManager;

    /**
     * Logger for this class.
     */
    private static final Logger ms_log = LogManager.getLogger(PSSchedulerBean.class);
}
