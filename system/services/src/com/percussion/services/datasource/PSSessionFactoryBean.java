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
package com.percussion.services.datasource;

import com.percussion.system.utils.PSBaseBean;
import com.percussion.utils.jdbc.IPSConnectionInfo;
import com.percussion.utils.jdbc.IPSDatasourceManager;
import com.percussion.utils.jdbc.PSConnectionDetail;
import com.percussion.utils.jdbc.PSJdbcUtils;
import com.percussion.utils.jndi.PSJndiObjectLocator;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.HibernateException;
import org.hibernate.boot.model.naming.ImplicitNamingStrategyLegacyHbmImpl;
import org.springframework.context.annotation.Configuration;
// Use Spring ORM LocalSessionFactoryBean (compatibility shim in Spring 7)
import org.springframework.orm.jpa.hibernate.LocalSessionFactoryBean;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.naming.NamingException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Extends Spring framework hibernate session factory to dynamically modify the
 * configuration before the session factory is created using modern Java 11 patterns.
 *
 * <p>This session factory provides thread-safe access to Hibernate configuration
 * with Optional-based safe access, enhanced validation, and comprehensive error handling.</p>
 *
 * @author Percussion Software
 * @since 6.0
 */
@Configuration
@EnableTransactionManagement
@PSBaseBean("sys_sessionFactory")
public class PSSessionFactoryBean extends LocalSessionFactoryBean {

    /**
     * Thread-safe reference to the configured instance for content repository access.
     * Uses AtomicReference for modern concurrency patterns.
     */
    private static final AtomicReference<PSSessionFactoryBean> INSTANCE_REF =
        new AtomicReference<>();

    /**
     * Default constructor that registers this instance using thread-safe patterns.
     */
    public PSSessionFactoryBean() {
        INSTANCE_REF.set(this);
    }

    /**
     * Get the static instance safely with Optional wrapper.
     *
     * @return Optional containing the static instance, or empty if not initialized
     */
    public static Optional<PSSessionFactoryBean> getInstanceSafely() {
        return Optional.ofNullable(INSTANCE_REF.get());
    }

    /**
     * Get the static instance, but does not initialize - which is done through
     * the spring framework.
     *
     * @return the static instance, never <code>null</code> after spring is
     *         initialized.
     * @deprecated Use {@link #getInstanceSafely()} for null-safe access
     */
    @Deprecated
    public static PSSessionFactoryBean getInstance() {
        return getInstanceSafely().orElse(null);
    }

    /**
     * Get the hibernate properties for the supplied info object with enhanced validation.
     *
     * @param info Specifies the datasource configuration to use, may be
     *           <code>null</code> to use the repository datasource.
     *
     * @return The properties, never <code>null</code>, will contain the
     *         datasource specific properties derived from the supplied
     *         connection info as well as any other properties specified by the
     *         server's configuration.
     *
     * @throws IllegalStateException if datasource manager has not been set
     * @throws NamingException If there is an error looking up the datasource
     * @throws SQLException If there is an error obtaining the connection details
     */
    public Properties getHibernateProperties(IPSConnectionInfo info)
            throws NamingException, SQLException {

        if (datasourceManager == null) {
            throw new IllegalStateException("Datasource Manager must be set");
        }

        var props = new Properties();
        props.putAll(hibernateProperties);

        var connDetail = datasourceManager.getConnectionDetail(info);

        // Configure datasource name with JNDI prefix
        configureDatasourceName(props, connDetail);

        // Configure database catalog and schema
        configureDatabaseProperties(props, connDetail);

        // Configure dialect with validation
        configureDialect(props, connDetail);

        // Configure database-specific properties
        configureDatabaseSpecificProperties(props, connDetail);

        // Configure cache properties
        configureCacheProperties(props);

        return props;
    }

    /**
     * Configure datasource name with JNDI prefix support.
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
        var dialect = dialectConfig.getDialectClassNameSafely(driver)
            .orElseThrow(() -> new RuntimeException(
                "Cannot determine Hibernate SQL dialect for driver: " + driver));

        props.setProperty("hibernate.dialect", dialect);
    }

    /**
     * Configure database-specific properties for DB2, Derby, and H2.
     *
     * <p>H2 (#548) does <strong>not</strong> inherit Derby {@code true=T,false=F} substitutions;
     * it uses native BOOLEAN. Isolation READ_UNCOMMITTED is only applied for DB2/Derby.
     */
    private void configureDatabaseSpecificProperties(Properties props, PSConnectionDetail connDetail) {
        var driver = connDetail.getDriver();

        // Set transaction isolation for DB2 & Derby (not H2)
        if (PSJdbcUtils.DB2.equalsIgnoreCase(driver) ||
            PSJdbcUtils.DERBY_DRIVER.equalsIgnoreCase(driver)) {
            props.setProperty("hibernate.connection.isolation",
                PSJdbcUtils.TRANSACTION_READ_UNCOMMITTED_VALUE);
        }

        // Configure Derby-specific query substitutions (do not apply to H2)
        if (PSJdbcUtils.DERBY_DRIVER.equalsIgnoreCase(driver)) {
            props.setProperty("hibernate.query.substitutions",
                "true=T,false=F,yes=Y,no=N");
        }
    }

    /**
     * Configure cache properties for optimal performance.
     */
    private void configureCacheProperties(Properties props) {
        props.setProperty("hibernate.cache.use_query_cache", "true");
        props.setProperty("hibernate.cache.ehcache.missing_cache_strategy", "create");
    }

    @Override
    public void setHibernateProperties(Properties props) {
        Objects.requireNonNull(props, "props may not be null");

        hibernateProperties.clear();
        hibernateProperties.putAll(props);

        super.setHibernateProperties(props);
        super.setImplicitNamingStrategy(new ImplicitNamingStrategyLegacyHbmImpl());
        super.setPhysicalNamingStrategy(new UpperCaseNamingStrategy());
    }

    @Override
    public void afterPropertiesSet() throws IllegalArgumentException, HibernateException {
        try {
            hibernateProperties.putAll(getHibernateProperties(null));
            super.setHibernateProperties(hibernateProperties);
            super.afterPropertiesSet();

            // TODO: Re-enable interceptor support for Hibernate 7.x
            // Interceptor functionality needs to be refactored for Hibernate 7.x compatibility
            // this.getConfiguration().setInterceptor(
            //     new PSHibernateInterceptor(interceptEvents));

        } catch (Exception e) {
            var errorMsg = "Failed to initialize the hibernate configuration: " +
                e.getLocalizedMessage();
            throw new RuntimeException(errorMsg, e);
        }
    }

    /**
     * Sets the datasource manager to use override the configuration.
     *
     * @param datasourceManager The datasource manager, may not be <code>null</code>.
     */
    public void setDatasourceManager(IPSDatasourceManager datasourceManager) {
        this.datasourceManager = Objects.requireNonNull(datasourceManager,
            "datasourceManager may not be null");
    }

    /**
     * Get the datasource manager safely.
     *
     * @return Optional containing the datasource manager, or empty if not set
     */
    public Optional<IPSDatasourceManager> getDatasourceManagerSafely() {
        return Optional.ofNullable(datasourceManager);
    }

    /**
     * The map of dialects to use when overriding the configuration.
     *
     * @param dialects The dialect config. May not be <code>null</code> or empty.
     */
    public void setDialects(PSHibernateDialectConfig dialects) {
        Objects.requireNonNull(dialects, "dialects may not be null");

        if (dialects.getDialects().isEmpty()) {
            throw new IllegalArgumentException("dialects may not be empty");
        }

        this.dialectConfig = dialects;
    }

    /**
     * Get the intercept events used for debugging.
     *
     * @return The intercept events list, never null
     */
    public List<String> getInterceptEvents() {
        return List.copyOf(interceptEvents); // Return immutable copy
    }

    /**
     * Set the intercept events for debugging purposes.
     *
     * @param interceptEvents The intercept events to set, may not be null
     */
    public void setInterceptEvents(List<String> interceptEvents) {
        Objects.requireNonNull(interceptEvents, "interceptEvents may not be null");
        this.interceptEvents = new ArrayList<>(interceptEvents);
    }

    @Override
    public String toString() {
        return String.format("PSSessionFactoryBean{datasourceManager=%s, dialectCount=%d, interceptEventCount=%d}",
            getDatasourceManagerSafely().map(Object::getClass).map(Class::getSimpleName).orElse("null"),
            dialectConfig.getDialects().size(),
            interceptEvents.size());
    }

    /**
     * The datasource manager to use to override the configuration.
     */
    private IPSDatasourceManager datasourceManager;

    /**
     * Configuration of JDBC driver name to hibernate SQL dialect.
     */
    private PSHibernateDialectConfig dialectConfig = new PSHibernateDialectConfig();

    /**
     * The list of events to be intercepted and logged for debugging.
     */
    private List<String> interceptEvents = new ArrayList<>();

    /**
     * Cached hibernate properties, thread-safe access.
     */
    private final Properties hibernateProperties = new Properties();
}
