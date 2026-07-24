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
     * Filenames from the immediately-preceding release of this codebase.
     * The install script's delete set is the union of
     * {@link #EXACT_FILENAMES} and this set, so an upgrade from N-1 to N
     * purges the prior version's JARs from {@code jetty/base/lib/jdbc/}
     * (preventing duplicate-driver-version classpath issues) while still
     * preserving any integrator-supplied drivers whose names do not match
     * this union.
     *
     * <p>When a driver version is bumped in the parent {@code pom.xml}, the
     * old filename MUST be moved from {@link #EXACT_FILENAMES} to this set
     * in the same commit, and the install script's delete block must be
     * updated in lockstep.
     */
    static final Set<String> PRIOR_FILENAMES;

    /**
     * Staging {@code <fileset>} {@code <include>} globs used by
     * {@code installDistributionFiles.xml} lines 707-717 and by
     * {@code verify-jdbc-drivers.py} (cross-platform Python port; the
     * canonical implementation is the
     * {@code com.percussion.distribution.install.VerifyJdbcDrivers} Java
     * main wired into the Maven {@code verify} phase at module
     * {@code pom.xml}).
     */
    static final String[] STAGING_GLOBS = {
            "mariadb-java-client-*.jar",
            "mysql-connector-j-*.jar",
            // #548 default embedded engine
            "h2-*.jar",
            // Derby retained for migration / legacy
            "derby-*.jar",
            "derbyclient-*.jar",
            "derbynet-*.jar",
            // Derby 10.15+ splits the engine across multiple JARs. The embedded
            // driver (derby.jar) requires derbyshared.jar (StandardException,
            // shared i18n, etc.) and derbytools.jar (utility classes). The glob
            // pattern "derby-*.jar" does NOT match these (no hyphen after
            // "derby"), so they MUST appear as separate <include> entries.
            // Mirrors modules/perc-distribution-tree/src/main/resources/
            // installDistributionFiles.xml:712-726 and pom.xml:167-177.
            "derbyshared-*.jar",
            "derbytools-*.jar",
            "mssql-jdbc-*.jar",
            "jtds-*.jar",
            "ojdbc17-*.jar",
            // #1500 PostgreSQL external CMS repository
            "postgresql-*.jar"
    };

    /**
     * Map from staging glob → corresponding Maven {@code artifactId}. Some
     * artifactIds contain dashes (e.g. {@code mariadb-java-client},
     * {@code mssql-jdbc}) so the glob→artifactId relation must be declared
     * explicitly rather than computed from the glob string.
     */
    static final String[][] GLOB_TO_ARTIFACT_ID = {
            {"mariadb-java-client-*.jar", "mariadb-java-client"},
            {"mysql-connector-j-*.jar",   "mysql-connector-j"},
            {"h2-*.jar",                  "h2"},
            {"derby-*.jar",               "derby"},
            {"derbyclient-*.jar",         "derbyclient"},
            {"derbynet-*.jar",            "derbynet"},
            {"derbyshared-*.jar",         "derbyshared"},
            {"derbytools-*.jar",          "derbytools"},
            {"mssql-jdbc-*.jar",          "mssql-jdbc"},
            {"jtds-*.jar",                "jtds"},
            {"ojdbc17-*.jar",             "ojdbc17"},
            {"postgresql-*.jar",          "postgresql"}
    };

    static {
        Set<String> filenames = new LinkedHashSet<>();
        filenames.add("mariadb-java-client-3.5.7.jar");
        filenames.add("mysql-connector-j-8.4.0.jar");
        filenames.add("h2-2.3.232.jar");
        filenames.add("derby-10.17.1.0.jar");
        filenames.add("derbyclient-10.17.1.0.jar");
        filenames.add("derbynet-10.17.1.0.jar");
        filenames.add("mssql-jdbc-13.3.1.jre11-preview.jar");
        filenames.add("jtds-1.3.1.jar");
        filenames.add("ojdbc17-23.26.0.0.0.jar");
        filenames.add("postgresql-42.7.7.jar");
        EXACT_FILENAMES = Collections.unmodifiableSet(filenames);

        // Prior release of this codebase: the versions immediately preceding
        // the current ones in the development branch's recent history.
        // (derby.version bumped 10.16.1.1 -> 10.17.1.0; mssql.version bumped
        // 13.3.0.jre11-preview -> 13.3.1.jre11-preview; mariadb / ojdbc17 /
        // jtds are unchanged across the recent history of the development
        // branch and so have no prior entry.)
        Set<String> prior = new LinkedHashSet<>();
        prior.add("derby-10.16.1.1.jar");
        prior.add("derbyclient-10.16.1.1.jar");
        prior.add("derbynet-10.16.1.1.jar");
        prior.add("mssql-jdbc-13.3.0.jre11-preview.jar");
        PRIOR_FILENAMES = Collections.unmodifiableSet(prior);
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
