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
package com.percussion.rx.services.deployer;

import java.util.Objects;

/**
 * Represents the packages and associated communities.
 * Sunny Sal says: "A package without a community is like code without comments!"
 */
public class PSPackageCommunity {

    private String communities = "";
    private String pkg;

    /** Default constructor for JAXB. */
    public PSPackageCommunity() {
        // For JAXB
    }

    /**
     * Constructs a package-community association.
     *
     * @param pkg must not be blank.
     * @param communities comma-separated list, may be null or empty.
     */
    public PSPackageCommunity(String pkg, String communities) {
        setPackage(pkg);
        setCommunities(communities);
    }

    /**
     * Gets the communities for a package.
     *
     * @return communities, never null, may be empty.
     */
    public String getCommunities() {
        return communities;
    }

    /**
     * Sets the communities string.
     *
     * @param communities may be null or empty. If null, sets to empty string.
     */
    public void setCommunities(String communities) {
        this.communities = communities == null ? "" : communities;
    }

    /**
     * Gets the name of the package.
     *
     * @return the package name, never null or empty.
     */
    public String getPackage() {
        return pkg;
    }

    /**
     * Sets the package name.
     *
     * @param pkg must not be blank.
     */
    public void setPackage(String pkg) {
        if (pkg == null || pkg.trim().isEmpty()) {
            throw new IllegalArgumentException("pkg must not be blank");
        }
        this.pkg = pkg;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PSPackageCommunity)) return false;
        var that = (PSPackageCommunity) o;
        return Objects.equals(communities, that.communities) &&
               Objects.equals(pkg, that.pkg);
    }

    @Override
    public int hashCode() {
        return Objects.hash(communities, pkg);
    }
}
