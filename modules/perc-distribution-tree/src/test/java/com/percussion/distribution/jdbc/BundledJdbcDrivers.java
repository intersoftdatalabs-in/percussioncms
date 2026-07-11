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
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY CONDITIONS OF any kind.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.percussion.distribution.jdbc;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Single source of truth for the bundled JDBC driver filenames in this module.
 *
 * <p>Used by:
 * <ul>
 *   <li>{@code InstallXmlDeleteSetTest} to assert the install script's
 *       {@code <delete>} block contains exactly these exact filenames (no globs).</li>
 *   <li>{@code StagingCleanupAntScriptTest} to assert the staging {@code <copy>}
 *       block's {@code <include>} globs cover exactly the curated set, and
 *       that no non-driver provided-scope dependency is referenced.</li>
 * </ul>
 *
 * <p>The values are pinned to the resolved filenames of the curated
 * {@code provided}-scope JDBC driver dependencies declared in
 * {@code modules/perc-distribution-tree/pom.xml} (lines 147-181). When a driver
 * version is bumped in the parent {@code pom.xml} version properties, this
 * constant and the install script's exact-filename {@code <delete>} list must
 * be updated in lockstep.
 *
 * <p>For feature 002-jdbc-drivers-cleanup. See {@code specs/002-jdbc-drivers-cleanup/data-model.md}
 * entity E1 and E2.
 */
final class BundledJdbcDrivers {

    /** Curated driver filename set (matches data-model.md E2). */
    static final Set<String> EXACT_FILENAMES;

    /**
     * Staging {@code <fileset>} {@code <include>} globs used by
     * {@code installDistributionFiles.xml} lines 707-717 and by
     * {@code verify-jdbc-drivers.sh} (wired into the Maven {@code verify} phase
     * at module {@code pom.xml:737}).
     */
    static final String[] STAGING_GLOBS = {
            "mariadb-java-client-*.jar",
            "derby-*.jar",
            "derbyclient-*.jar",
            "derbynet-*.jar",
            "mssql-jdbc-*.jar",
            "jtds-*.jar",
            "ojdbc17-*.jar"
    };

    /**
     * Map from staging glob → corresponding Maven {@code artifactId}. Some
     * artifactIds contain dashes (e.g. {@code mariadb-java-client},
     * {@code mssql-jdbc}) so the glob→artifactId relation must be declared
     * explicitly rather than computed from the glob string.
     */
    static final String[][] GLOB_TO_ARTIFACT_ID = {
            {"mariadb-java-client-*.jar", "mariadb-java-client"},
            {"derby-*.jar",               "derby"},
            {"derbyclient-*.jar",         "derbyclient"},
            {"derbynet-*.jar",            "derbynet"},
            {"mssql-jdbc-*.jar",          "mssql-jdbc"},
            {"jtds-*.jar",                "jtds"},
            {"ojdbc17-*.jar",             "ojdbc17"}
    };

    static {
        Set<String> filenames = new LinkedHashSet<>();
        filenames.add("mariadb-java-client-3.5.7.jar");
        filenames.add("derby-10.17.1.0.jar");
        filenames.add("derbyclient-10.17.1.0.jar");
        filenames.add("derbynet-10.17.1.0.jar");
        filenames.add("mssql-jdbc-13.3.1.jre11-preview.jar");
        filenames.add("jtds-1.3.1.jar");
        filenames.add("ojdbc17-23.26.0.0.0.jar");
        EXACT_FILENAMES = Collections.unmodifiableSet(filenames);
    }

    private BundledJdbcDrivers() {
        // utility class
    }

    /**
     * Returns the set of {@code artifactId}s that are bundled JDBC drivers.
     * Used by structural tests to distinguish curated driver deps from
     * non-driver provided-scope deps.
     */
    static Set<String> curatedArtifactIds() {
        Set<String> ids = new LinkedHashSet<>();
        for (String[] pair : GLOB_TO_ARTIFACT_ID) {
            ids.add(pair[1]);
        }
        return ids;
    }
}
