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
package com.percussion.services.utils.general;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

/**
 * This Java bean contains properties to be used by the various service beans
 * that require user modifiable configuration. The bean provides thread-safe
 * access to configuration properties with enhanced validation and modern
 * Java 11 features.
 * <p>
 * All configuration values have sensible defaults and are validated when set
 * to ensure system stability.
 *
 * @author dougrand
 */
public class PSServiceConfigurationBean {

    private static final Logger ms_log = LogManager.getLogger(PSServiceConfigurationBean.class);

    // Configuration constants
    private static final int DEFAULT_QUARTZ_THREAD_COUNT = 3;
    private static final int DEFAULT_PUBLISH_JOB_TIMEOUT = 600;
    private static final int DEFAULT_PUBLISH_QUEUE_TIMEOUT = 10;
    private static final int DEFAULT_MAX_ROWS_PER_PAGE = 300;
    private static final long DEFAULT_MAX_CACHED_CONTENT_NODE_SIZE = 0;

    // Validation ranges
    private static final int MIN_THREAD_COUNT = 1;
    private static final int MAX_THREAD_COUNT = 100;
    private static final int MIN_TIMEOUT = 1;
    private static final int MAX_TIMEOUT = 3600; // 1 hour
    private static final int MIN_ROWS_PER_PAGE = 10;
    private static final int MAX_ROWS_PER_PAGE = 10000;

    /**
     * This property is used by the assembly service to decide if a content node
     * should or should not be cached in the memory cache. If the value is
     * {@code 0}, then no content nodes are cached.
     */
    private volatile long maxCachedContentNodeSize = DEFAULT_MAX_CACHED_CONTENT_NODE_SIZE;

    /**
     * Number of threads for Quartz scheduler processing.
     */
    private volatile int quartzThreadCount = DEFAULT_QUARTZ_THREAD_COUNT;

    /**
     * Timeout for publishing jobs in seconds.
     */
    private volatile int publishJobTimeout = DEFAULT_PUBLISH_JOB_TIMEOUT;

    /**
     * Timeout for publish queue operations in seconds.
     */
    private volatile int publishQueueTimeout = DEFAULT_PUBLISH_QUEUE_TIMEOUT;

    /**
     * Maximum rows per page when viewing publish logs.
     */
    private volatile int maxRowsPerPageInViewPubLog = DEFAULT_MAX_ROWS_PER_PAGE;

    /**
     * Quartz scheduler properties.
     */
    private volatile Properties quartzProperties = new Properties();

    /**
     * Whether to use HTTPS for secure sites.
     */
    private volatile boolean useHttpsForSecureSite = true;

    /**
     * Gets the maximum cached content node size.
     *
     * @return the maximum size in bytes, {@code 0} means no caching
     */
    public long getMaxCachedContentNodeSize() {
        return maxCachedContentNodeSize;
    }

    /**
     * Sets the maximum cached content node size with validation.
     *
     * @param size the maximum size in bytes, must be non-negative
     * @throws IllegalArgumentException if size is negative
     */
    public void setMaxCachedContentNodeSize(long size) {
        if (size < 0) {
            throw new IllegalArgumentException("maxCachedContentNodeSize cannot be negative: " + size);
        }

        ms_log.debug("Setting maxCachedContentNodeSize = {}", size);
        this.maxCachedContentNodeSize = size;
    }

    /**
     * Gets the Quartz thread count.
     *
     * @return the number of threads for Quartz processing
     */
    public int getQuartzThreadCount() {
        return quartzThreadCount;
    }

    /**
     * Sets the Quartz thread count with validation.
     *
     * @param threadCount the number of threads, must be between {@value MIN_THREAD_COUNT}
     *                    and {@value MAX_THREAD_COUNT}
     * @throws IllegalArgumentException if threadCount is out of valid range
     */
    public void setQuartzThreadCount(int threadCount) {
        if (threadCount < MIN_THREAD_COUNT || threadCount > MAX_THREAD_COUNT) {
            throw new IllegalArgumentException(
                String.format("quartzThreadCount must be between %d and %d, got: %d",
                    MIN_THREAD_COUNT, MAX_THREAD_COUNT, threadCount));
        }

        ms_log.debug("Setting quartzThreadCount = {}", threadCount);
        this.quartzThreadCount = threadCount;
    }

    /**
     * Gets the publish job timeout in seconds.
     *
     * @return the timeout value in seconds
     */
    public int getPublishJobTimeout() {
        return publishJobTimeout;
    }

    /**
     * Sets the publish job timeout with validation.
     *
     * @param timeout the timeout in seconds, must be between {@value MIN_TIMEOUT}
     *                and {@value MAX_TIMEOUT}
     * @throws IllegalArgumentException if timeout is out of valid range
     */
    public void setPublishJobTimeout(int timeout) {
        validateTimeout(timeout, "publishJobTimeout");
        ms_log.debug("Setting publishJobTimeout = {}", timeout);
        this.publishJobTimeout = timeout;
    }

    /**
     * Gets the publish queue timeout in seconds.
     *
     * @return the timeout value in seconds
     */
    public int getPublishQueueTimeout() {
        return publishQueueTimeout;
    }

    /**
     * Sets the publish queue timeout with validation.
     *
     * @param timeout the timeout in seconds, must be between {@value MIN_TIMEOUT}
     *                and {@value MAX_TIMEOUT}
     * @throws IllegalArgumentException if timeout is out of valid range
     */
    public void setPublishQueueTimeout(int timeout) {
        validateTimeout(timeout, "publishQueueTimeout");
        ms_log.debug("Setting publishQueueTimeout = {}", timeout);
        this.publishQueueTimeout = timeout;
    }

    /**
     * Gets the maximum rows per page for publish log viewing.
     *
     * @return the maximum number of rows per page
     */
    public int getMaxRowsPerPageInViewPubLog() {
        return maxRowsPerPageInViewPubLog;
    }

    /**
     * Sets the maximum rows per page with validation.
     *
     * @param maxRows the maximum rows per page, must be between {@value MIN_ROWS_PER_PAGE}
     *                and {@value MAX_ROWS_PER_PAGE}
     * @throws IllegalArgumentException if maxRows is out of valid range
     */
    public void setMaxRowsPerPageInViewPubLog(int maxRows) {
        if (maxRows < MIN_ROWS_PER_PAGE || maxRows > MAX_ROWS_PER_PAGE) {
            throw new IllegalArgumentException(
                String.format("maxRowsPerPageInViewPubLog must be between %d and %d, got: %d",
                    MIN_ROWS_PER_PAGE, MAX_ROWS_PER_PAGE, maxRows));
        }

        ms_log.debug("Setting maxRowsPerPageInViewPubLog = {}", maxRows);
        this.maxRowsPerPageInViewPubLog = maxRows;
    }

    /**
     * Gets the Quartz properties safely.
     *
     * @return an Optional containing the Quartz properties, never null
     */
    public Optional<Properties> getQuartzProperties() {
        return Optional.ofNullable(quartzProperties);
    }

    /**
     * Sets the Quartz properties with null safety.
     *
     * @param properties the Quartz properties, may be {@code null}
     */
    public void setQuartzProperties(Properties properties) {
        ms_log.debug("Setting quartzProperties");
        this.quartzProperties = Objects.requireNonNullElse(properties, new Properties());
    }

    /**
     * Gets whether HTTPS should be used for secure sites.
     *
     * @return {@code true} if HTTPS should be used, {@code false} otherwise
     */
    public boolean isUseHttpsForSecureSite() {
        return useHttpsForSecureSite;
    }

    /**
     * Sets whether to use HTTPS for secure sites.
     *
     * @param useHttps {@code true} to use HTTPS, {@code false} otherwise
     */
    public void setUseHttpsForSecureSite(boolean useHttps) {
        ms_log.debug("Setting useHttpsForSecureSite = {}", useHttps);
        this.useHttpsForSecureSite = useHttps;
    }

    /**
     * Gets the publish job timeout as a TimeUnit for better type safety.
     *
     * @param unit the desired time unit
     * @return the timeout value in the specified unit
     * @throws IllegalArgumentException if unit is null
     */
    public long getPublishJobTimeout(TimeUnit unit) {
        Objects.requireNonNull(unit, "TimeUnit cannot be null");
        return unit.convert(publishJobTimeout, TimeUnit.SECONDS);
    }

    /**
     * Gets the publish queue timeout as a TimeUnit for better type safety.
     *
     * @param unit the desired time unit
     * @return the timeout value in the specified unit
     * @throws IllegalArgumentException if unit is null
     */
    public long getPublishQueueTimeout(TimeUnit unit) {
        Objects.requireNonNull(unit, "TimeUnit cannot be null");
        return unit.convert(publishQueueTimeout, TimeUnit.SECONDS);
    }

    /**
     * Validates timeout values using a reusable method.
     *
     * @param timeout the timeout value to validate
     * @param fieldName the name of the field being validated for error messages
     * @throws IllegalArgumentException if timeout is out of valid range
     */
    private void validateTimeout(int timeout, String fieldName) {
        if (timeout < MIN_TIMEOUT || timeout > MAX_TIMEOUT) {
            throw new IllegalArgumentException(
                String.format("%s must be between %d and %d seconds, got: %d",
                    fieldName, MIN_TIMEOUT, MAX_TIMEOUT, timeout));
        }
    }

    /**
     * Creates a configuration summary for debugging and monitoring.
     *
     * @return a string representation of current configuration
     */
    public String getConfigurationSummary() {
        return String.format("""
            PSServiceConfigurationBean Configuration:
            - Max Cached Content Node Size: %d bytes
            - Quartz Thread Count: %d
            - Publish Job Timeout: %d seconds
            - Publish Queue Timeout: %d seconds
            - Max Rows Per Page: %d
            - Use HTTPS for Secure Sites: %s
            - Quartz Properties Count: %d
            """,
            maxCachedContentNodeSize,
            quartzThreadCount,
            publishJobTimeout,
            publishQueueTimeout,
            maxRowsPerPageInViewPubLog,
            useHttpsForSecureSite,
            quartzProperties.size()
        );
    }

    /**
     * Validates the entire configuration for consistency.
     *
     * @return {@code true} if configuration is valid, {@code false} otherwise
     */
    public boolean isConfigurationValid() {
        try {
            validateTimeout(publishJobTimeout, "publishJobTimeout");
            validateTimeout(publishQueueTimeout, "publishQueueTimeout");

            if (quartzThreadCount < MIN_THREAD_COUNT || quartzThreadCount > MAX_THREAD_COUNT) {
                return false;
            }

            if (maxRowsPerPageInViewPubLog < MIN_ROWS_PER_PAGE || maxRowsPerPageInViewPubLog > MAX_ROWS_PER_PAGE) {
                return false;
            }

            return maxCachedContentNodeSize >= 0;
        } catch (IllegalArgumentException e) {
            ms_log.warn("Configuration validation failed: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public String toString() {
        return getConfigurationSummary();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }

        var other = (PSServiceConfigurationBean) obj;
        return maxCachedContentNodeSize == other.maxCachedContentNodeSize &&
               quartzThreadCount == other.quartzThreadCount &&
               publishJobTimeout == other.publishJobTimeout &&
               publishQueueTimeout == other.publishQueueTimeout &&
               maxRowsPerPageInViewPubLog == other.maxRowsPerPageInViewPubLog &&
               useHttpsForSecureSite == other.useHttpsForSecureSite &&
               Objects.equals(quartzProperties, other.quartzProperties);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            maxCachedContentNodeSize,
            quartzThreadCount,
            publishJobTimeout,
            publishQueueTimeout,
            maxRowsPerPageInViewPubLog,
            useHttpsForSecureSite,
            quartzProperties
        );
    }
}
