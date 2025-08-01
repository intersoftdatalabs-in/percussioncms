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
package com.percussion.services.contentmgr;

import com.percussion.utils.jsr170.IPSPropertyInterceptor;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Modern Java 11 configuration object that controls content manager behavior.
 *
 * <p>This configuration is primarily used by the assembly service to adjust
 * retrieved property values and control content processing behavior. It provides:
 * <ul>
 *   <li>Configurable content manager options with type-safe enum operations</li>
 *   <li>Property interceptors for body access, namespace cleanup, and div tag processing</li>
 *   <li>Optional-based safe access methods for all interceptors</li>
 *   <li>Stream-based operations for efficient option processing</li>
 * </ul>
 *
 * <p>All operations follow Java 11 best practices with null safety, immutable
 * collections where appropriate, and comprehensive validation.
 *
 * @see PSContentMgrOption for details on available configuration options
 * @author dougrand
 * @since Java 11 Modernization
 */
public final class PSContentMgrConfig {

    /**
     * The options controlling various aspects of content retrieval and processing.
     * Uses EnumSet for optimal performance and type safety.
     */
    private final Set<PSContentMgrOption> options = EnumSet.noneOf(PSContentMgrOption.class);

    /**
     * Optional interceptor that filters all access to body fields.
     * Takes a {@link javax.jcr.Property} argument when processing.
     */
    private IPSPropertyInterceptor bodyAccess;

    /**
     * Optional interceptor for properties with the <i>cleanupNamespaces</i> field property.
     * Instantiated and called for namespace cleanup operations.
     */
    private IPSPropertyInterceptor namespaceCleanup;

    /**
     * Optional interceptor that filters out div tags with class attribute 'rxbodyfield'.
     * The children of filtered div tags are preserved in the output.
     */
    private IPSPropertyInterceptor divTagCleanup;

    /**
     * Creates a new configuration with default options.
     * Initializes with {@link PSContentMgrOption#LOAD_MINIMAL} as the default behavior.
     */
    public PSContentMgrConfig() {
        addOption(PSContentMgrOption.LOAD_MINIMAL);
    }

    /**
     * Copy constructor for creating configuration instances from existing ones.
     *
     * @param source the source configuration to copy from, must not be null
     * @throws IllegalArgumentException if source is null
     */
    public PSContentMgrConfig(PSContentMgrConfig source) {
        Objects.requireNonNull(source, "Source configuration cannot be null");
        this.options.addAll(source.options);
        this.bodyAccess = source.bodyAccess;
        this.namespaceCleanup = source.namespaceCleanup;
        this.divTagCleanup = source.divTagCleanup;
    }

    /**
     * Gets the body access interceptor for filtering body field access.
     *
     * @return the body access interceptor, may be null if not configured
     */
    public IPSPropertyInterceptor getBodyAccess() {
        return bodyAccess;
    }

    /**
     * Safely gets the body access interceptor with Optional wrapper.
     *
     * @return an Optional containing the body access interceptor, or empty if not configured
     */
    public Optional<IPSPropertyInterceptor> getBodyAccessSafely() {
        return Optional.ofNullable(bodyAccess);
    }

    /**
     * Sets the body access interceptor for filtering body field access.
     *
     * @param bodyAccess the body access interceptor, may be null to disable
     */
    public void setBodyAccess(IPSPropertyInterceptor bodyAccess) {
        this.bodyAccess = bodyAccess;
    }

    /**
     * Gets an immutable view of the configured options.
     *
     * @return an immutable set of options, never null but may be empty
     */
    public Set<PSContentMgrOption> getOptions() {
        return Collections.unmodifiableSet(options);
    }

    /**
     * Streams the configured options for efficient processing.
     *
     * @return a stream of configured options, never null but may be empty
     */
    public Stream<PSContentMgrOption> streamOptions() {
        return options.stream();
    }

    /**
     * Checks if a specific option is enabled in this configuration.
     *
     * @param option the option to check, must not be null
     * @return true if the option is enabled, false otherwise
     * @throws IllegalArgumentException if option is null
     */
    public boolean hasOption(PSContentMgrOption option) {
        Objects.requireNonNull(option, "Option cannot be null");
        return options.contains(option);
    }

    /**
     * Gets the count of enabled options.
     *
     * @return the number of enabled options
     */
    public int getOptionCount() {
        return options.size();
    }

    /**
     * Adds the specified option to the configuration.
     *
     * @param option the option to add, must not be null
     * @return true if the option was added (wasn't already present), false otherwise
     * @throws IllegalArgumentException if option is null
     */
    public boolean addOption(PSContentMgrOption option) {
        Objects.requireNonNull(option, "Option cannot be null");
        return options.add(option);
    }

    /**
     * Removes the specified option from the configuration.
     *
     * @param option the option to remove, must not be null
     * @return true if the option was removed (was present), false otherwise
     * @throws IllegalArgumentException if option is null
     */
    public boolean removeOption(PSContentMgrOption option) {
        Objects.requireNonNull(option, "Option cannot be null");
        return options.remove(option);
    }

    /**
     * Adds multiple options to the configuration in a single operation.
     *
     * @param optionsToAdd the options to add, must not be null or contain null elements
     * @throws IllegalArgumentException if optionsToAdd is null or contains null elements
     */
    public void addOptions(PSContentMgrOption... optionsToAdd) {
        Objects.requireNonNull(optionsToAdd, "Options array cannot be null");
        for (var option : optionsToAdd) {
            addOption(option);
        }
    }

    /**
     * Removes multiple options from the configuration in a single operation.
     *
     * @param optionsToRemove the options to remove, must not be null or contain null elements
     * @throws IllegalArgumentException if optionsToRemove is null or contains null elements
     */
    public void removeOptions(PSContentMgrOption... optionsToRemove) {
        Objects.requireNonNull(optionsToRemove, "Options array cannot be null");
        for (var option : optionsToRemove) {
            removeOption(option);
        }
    }

    /**
     * Clears all configured options.
     */
    public void clearOptions() {
        options.clear();
    }

    /**
     * Gets the namespace cleanup interceptor.
     *
     * @return the namespace cleanup interceptor, may be null if not configured
     */
    public IPSPropertyInterceptor getNamespaceCleanup() {
        return namespaceCleanup;
    }

    /**
     * Safely gets the namespace cleanup interceptor with Optional wrapper.
     *
     * @return an Optional containing the namespace cleanup interceptor, or empty if not configured
     */
    public Optional<IPSPropertyInterceptor> getNamespaceCleanupSafely() {
        return Optional.ofNullable(namespaceCleanup);
    }

    /**
     * Sets the namespace cleanup interceptor for fields with the cleanupNamespaces property.
     *
     * @param namespaceCleanup the namespace cleanup interceptor, may be null to disable
     */
    public void setNamespaceCleanup(IPSPropertyInterceptor namespaceCleanup) {
        this.namespaceCleanup = namespaceCleanup;
    }

    /**
     * Gets the div tag cleanup interceptor.
     *
     * @return the div tag cleanup interceptor, may be null if not configured
     */
    public IPSPropertyInterceptor getDivTagCleanup() {
        return divTagCleanup;
    }

    /**
     * Safely gets the div tag cleanup interceptor with Optional wrapper.
     *
     * @return an Optional containing the div tag cleanup interceptor, or empty if not configured
     */
    public Optional<IPSPropertyInterceptor> getDivTagCleanupSafely() {
        return Optional.ofNullable(divTagCleanup);
    }

    /**
     * Sets the div tag cleanup interceptor for filtering rxbodyfield div tags.
     *
     * @param divTagCleanup the div tag cleanup interceptor, may be null to disable
     */
    public void setDivTagCleanup(IPSPropertyInterceptor divTagCleanup) {
        this.divTagCleanup = divTagCleanup;
    }

    /**
     * Checks if any interceptors are configured.
     *
     * @return true if at least one interceptor is configured, false otherwise
     */
    public boolean hasInterceptors() {
        return bodyAccess != null || namespaceCleanup != null || divTagCleanup != null;
    }

    /**
     * Checks if this configuration is in minimal loading mode.
     *
     * @return true if LOAD_MINIMAL option is enabled, false otherwise
     */
    public boolean isMinimalLoading() {
        return hasOption(PSContentMgrOption.LOAD_MINIMAL);
    }

    /**
     * Creates a new configuration with minimal loading enabled.
     *
     * @return a new configuration instance with LOAD_MINIMAL option
     */
    public static PSContentMgrConfig createMinimal() {
        return new PSContentMgrConfig(); // Default constructor adds LOAD_MINIMAL
    }

    /**
     * Creates a new configuration with specified options.
     *
     * @param options the options to enable, must not be null or contain null elements
     * @return a new configuration instance with the specified options
     * @throws IllegalArgumentException if options is null or contains null elements
     */
    public static PSContentMgrConfig createWithOptions(PSContentMgrOption... options) {
        var config = new PSContentMgrConfig();
        config.clearOptions(); // Remove default LOAD_MINIMAL
        config.addOptions(options);
        return config;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof PSContentMgrConfig)) {
            return false;
        }
        var other = (PSContentMgrConfig) obj;
        return Objects.equals(options, other.options)
                && Objects.equals(bodyAccess, other.bodyAccess)
                && Objects.equals(namespaceCleanup, other.namespaceCleanup)
                && Objects.equals(divTagCleanup, other.divTagCleanup);
    }

    @Override
    public int hashCode() {
        return Objects.hash(options, bodyAccess, namespaceCleanup, divTagCleanup);
    }

    @Override
    public String toString() {
        return String.format("PSContentMgrConfig{options=%s, bodyAccess=%s, namespaceCleanup=%s, divTagCleanup=%s}",
                options, bodyAccess != null, namespaceCleanup != null, divTagCleanup != null);
    }
}
