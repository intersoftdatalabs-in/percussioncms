// REFACTORED: CP-JAVA11
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
package com.percussion.services.contentmgr;

import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Modern Java 11 enumeration for content manager loading options.
 *
 * <p>This enum defines configuration options that control how the content manager
 * loads and processes content items. Options can be combined using {@link EnumSet}
 * for efficient bulk operations and type-safe configuration management.
 *
 * <p>Each option has specific implications for performance, memory usage, and
 * data completeness. Choose options based on your specific use case requirements.
 *
 * @author dougrand
 * @since Java 11 Modernization
 */
public enum PSContentMgrOption {

    /**
     * Enables comprehensive field processing including input/output translations,
     * field validations, and initial value assignments.
     *
     * <p>When this option is enabled, the content manager will:
     * <ul>
     *   <li>Process input translations for incoming data</li>
     *   <li>Apply output translations for outgoing data</li>
     *   <li>Execute field validation rules</li>
     *   <li>Set initial values as appropriate</li>
     * </ul>
     *
     * <p><strong>Performance Impact:</strong> High - Full field processing requires
     * additional computation and validation cycles.
     */
    PROCESS_FIELDS("Enables full field processing with translations and validations"),

    /**
     * Enables lazy loading for child content items and related data.
     *
     * <p>When this option is enabled, child content items are loaded on-demand
     * during first access rather than being preloaded with the parent item.
     * This can significantly improve initial load performance for content with
     * many children.
     *
     * <p><strong>Performance Impact:</strong> Variable - Faster initial load,
     * but may cause delays during child access.
     */
    LAZY_LOAD_CHILDREN("Defers child content loading until first access"),

    /**
     * Loads the minimal amount of data required for basic operations.
     *
     * <p>This option optimizes for minimal memory usage and fastest load times
     * by loading only essential content data. Additional data can be loaded
     * on-demand if needed.
     *
     * <p><strong>Performance Impact:</strong> Low - Minimal data loading provides
     * optimal performance for basic content operations.
     */
    LOAD_MINIMAL("Loads only essential data for optimal performance");

    /**
     * Human-readable description of this option's behavior.
     */
    private final String description;

    /**
     * Creates a new content manager option with the specified description.
     *
     * @param description human-readable description of the option's behavior
     */
    PSContentMgrOption(String description) {
        this.description = description;
    }

    /**
     * Gets the human-readable description of this option.
     *
     * @return the option's description, never null
     */
    public String getDescription() {
        return description;
    }

    /**
     * Checks if this option affects performance significantly.
     *
     * @return true if this option has high performance impact, false otherwise
     */
    public boolean isHighPerformanceImpact() {
        return this == PROCESS_FIELDS;
    }

    /**
     * Checks if this option is related to data loading behavior.
     *
     * @return true if this option affects how data is loaded, false otherwise
     */
    public boolean isLoadingOption() {
        return this == LAZY_LOAD_CHILDREN || this == LOAD_MINIMAL;
    }

    /**
     * Creates an EnumSet containing all performance-optimized options.
     *
     * @return an EnumSet with options optimized for performance
     */
    public static EnumSet<PSContentMgrOption> getPerformanceOptimizedOptions() {
        return EnumSet.of(LAZY_LOAD_CHILDREN, LOAD_MINIMAL);
    }

    /**
     * Creates an EnumSet containing all comprehensive processing options.
     *
     * @return an EnumSet with options for complete data processing
     */
    public static EnumSet<PSContentMgrOption> getComprehensiveOptions() {
        return EnumSet.of(PROCESS_FIELDS);
    }

    /**
     * Creates an EnumSet containing the default recommended options.
     *
     * @return an EnumSet with the recommended default configuration
     */
    public static EnumSet<PSContentMgrOption> getDefaultOptions() {
        return EnumSet.of(LOAD_MINIMAL);
    }

    /**
     * Finds an option by its name, case-insensitive.
     *
     * @param name the option name to search for, must not be null
     * @return an Optional containing the matching option, or empty if not found
     * @throws IllegalArgumentException if name is null
     */
    public static Optional<PSContentMgrOption> findByName(String name) {
        if (name == null) {
            throw new IllegalArgumentException("Option name cannot be null");
        }
        return Stream.of(values())
                .filter(option -> option.name().equalsIgnoreCase(name.trim()))
                .findFirst();
    }

    /**
     * Streams all available options for functional-style processing.
     *
     * @return a stream of all PSContentMgrOption values
     */
    public static Stream<PSContentMgrOption> stream() {
        return Stream.of(values());
    }

    /**
     * Gets all options that affect performance.
     *
     * @return a stream of performance-affecting options
     */
    public static Stream<PSContentMgrOption> getPerformanceOptions() {
        return stream().filter(PSContentMgrOption::isHighPerformanceImpact);
    }

    /**
     * Gets all options related to data loading.
     *
     * @return a stream of loading-related options
     */
    public static Stream<PSContentMgrOption> getLoadingOptions() {
        return stream().filter(PSContentMgrOption::isLoadingOption);
    }

    /**
     * Validates that a set of options is compatible.
     *
     * @param options the options to validate, must not be null
     * @return true if the options are compatible, false otherwise
     * @throws IllegalArgumentException if options is null
     */
    public static boolean areOptionsCompatible(Set<PSContentMgrOption> options) {
        if (options == null) {
            throw new IllegalArgumentException("Options set cannot be null");
        }
        // All current options are compatible with each other
        return true;
    }

    @Override
    public String toString() {
        return String.format("%s: %s", name(), description);
    }
}
