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
// REFACTORED: CP-JAVA11

package com.percussion.deployer.server;

import com.percussion.deployer.objectstore.PSDependency;
import com.percussion.deployer.objectstore.PSDependencyContext;
import com.percussion.deployer.objectstore.PSDependencyTreeContext;
import com.percussion.deployer.objectstore.PSDeployableElement;
import com.percussion.deployer.objectstore.PSIdMap;
import com.percussion.deployer.objectstore.PSImportDescriptor;
import com.percussion.deployer.objectstore.PSImportPackage;
import com.percussion.deployer.objectstore.PSValidationResult;
import com.percussion.deployer.objectstore.PSValidationResults;
import com.percussion.utils.collections.PSIteratorUtils;

import java.util.*;

/**
 * Encapsulates various objects used to perform validation across multiple
 * packages.
 */
public class PSValidationCtx {

    /**
     * Construct a validation context with the objects used across packages.
     *
     * @param jobHandle The validation job handle, may not be {@code null}.
     * @param desc      The import descriptor being validated, may not be {@code null}.
     * @param idMap     The current id map, may be {@code null} if no transformation is required.
     */
    public PSValidationCtx(IPSJobHandle jobHandle, PSImportDescriptor desc, PSIdMap idMap) {
        Objects.requireNonNull(jobHandle, "jobHandle may not be null");
        Objects.requireNonNull(desc, "desc may not be null");

        this.m_jobHandle = jobHandle;
        this.m_idMap = idMap;

        // Build "full" tree context from desc so we can check absent ancestors
        m_fullTreeCtx = new PSDependencyTreeContext();
        for (var pkg : desc.getImportPackageList()) {
            m_fullTreeCtx.addPackage(pkg.getPackage(), true);
        }
    }

    /**
     * Add the dependency to this context's list of already validated dependencies.
     *
     * @param dep The validated dependency, may not be {@code null}.
     */
    public void addValidatedDependency(PSDependency dep) {
        Objects.requireNonNull(dep, "dep may not be null");
        // Save in map, only overwrite a local (we want to store a shared over a local)
        var prev = m_validatedDeps.get(dep.getKey());
        if (prev == null || prev.getDependencyType() == PSDependency.TYPE_LOCAL) {
            m_validatedDeps.put(dep.getKey(), dep);
        }
    }

    /**
     * Determine if the supplied dependency has already been validated.
     *
     * @param dep The dependency to check, may not be {@code null}.
     * @return {@code true} if it has already been validated, {@code false} if not.
     */
    public boolean alreadyValidated(PSDependency dep) {
        Objects.requireNonNull(dep, "dep may not be null");
        // If shared, check for results first. If none, then only counts if already validated as a non-local dependency.
        boolean validated = (getValidationResult(dep) != null);
        if (!validated) {
            var valDep = m_validatedDeps.get(dep.getKey());
            validated = (valDep != null && valDep.getDependencyType() != PSDependency.TYPE_LOCAL);
        }
        return validated;
    }

    /**
     * Set whether ancestor validation should be performed.
     *
     * @param doValidate {@code true} to perform ancestor validation, {@code false} to skip it.
     */
    public void setValidateAncestors(boolean doValidate) {
        m_validateAncestors = doValidate;
    }

    /**
     * Get the job handle to use to report job status.
     *
     * @return The job handle, never {@code null}.
     */
    public IPSJobHandle getJobHandle() {
        return m_jobHandle;
    }

    /**
     * Get the current id map to use for transforms.
     *
     * @return The id map, may be {@code null} if transforms are not required.
     */
    public PSIdMap getIdMap() {
        return m_idMap;
    }

    /**
     * Get the current dependency tree context, contains all packages in the import descriptor.
     *
     * @return The tree context, never {@code null}.
     */
    public PSDependencyTreeContext getCurrentTreeCtx() {
        return m_fullTreeCtx;
    }

    /**
     * Determine if ancestor validation should be performed.
     *
     * @return {@code true} if it should, {@code false} if it should be skipped.
     */
    public boolean getValidateAncestors() {
        return m_validateAncestors;
    }

    /**
     * Adds a package to this context so that previous packages' validation results may be retrieved.
     *
     * @param pkg The package to add, may not be {@code null}.
     */
    public void addPackage(PSImportPackage pkg) {
        Objects.requireNonNull(pkg, "pkg may not be null");
        var de = pkg.getPackage();
        m_pkgMap.put(de.getKey(), pkg);
    }

    /**
     * Determine if the supplied dependency is included in any package within the import descriptor being validated.
     *
     * @param dep The dependency to check, may not be {@code null}.
     * @return {@code true} if the archive includes the dependency, {@code false} if not.
     */
    public boolean archiveIncludesDependency(PSDependency dep) {
        Objects.requireNonNull(dep, "dep may not be null");
        var ctx = m_fullTreeCtx.getDependencyCtx(dep.getKey());
        return ctx != null && ctx.isIncluded();
    }

    /**
     * Get any previously added validation result for the supplied dependency.
     *
     * @param dep The dependency to check for, may not be {@code null}.
     * @return The result, may be {@code null} if no result has been added.
     */
    public PSValidationResult getValidationResult(PSDependency dep) {
        Objects.requireNonNull(dep, "dep may not be null");
        for (var pkg : m_pkgMap.values()) {
            var results = pkg.getValidationResults();
            if (results != null) {
                var result = results.getResult(dep);
                if (result != null) {
                    return result;
                }
            }
        }
        return null;
    }

    /**
     * Add an absent ancestor to this context. See {@link #getAbsentAncestors(PSDependency)} for more info.
     *
     * @param dep The dependency for which an absent ancestor is supplied, may not be {@code null}.
     * @param anc The absent ancestor, may not be {@code null}.
     */
    public void addAbsentAncestor(PSDependency dep, PSDependency anc) {
        Objects.requireNonNull(dep, "dep may not be null");
        Objects.requireNonNull(anc, "anc may not be null");
        var ancList = m_absentAncs.computeIfAbsent(dep.getKey(), k -> new ArrayList<>());
        ancList.add(anc);
    }

    /**
     * Get any absent ancestors added for the specified dependency. Absent ancestors are ancestors of the dependency on the target server that are not included in the dependency's package.
     *
     * @param dep The dependency for which ancestors may have been added, may not be {@code null}.
     * @return An iterator over zero or more ancestors, never {@code null}, may be empty.
     */
    public Iterator<PSDependency> getAbsentAncestors(PSDependency dep) {
        Objects.requireNonNull(dep, "dep may not be null");
        var ancList = m_absentAncs.get(dep.getKey());
        if (ancList == null) {
            return PSIteratorUtils.emptyIterator();
        }
        return ancList.iterator();
    }

    // The job handle supplied during construction, never null or modified after that.
    private final IPSJobHandle m_jobHandle;

    // Full tree context containing all packages from the import descriptor supplied during construction.
    private final PSDependencyTreeContext m_fullTreeCtx;

    // The ID Map supplied during construction, may be null.
    private final PSIdMap m_idMap;

    // Flag to indicate if ancestors should be validated. Initially false, modified by setValidateAncestors.
    private boolean m_validateAncestors = false;

    // Map of import packages. Key is the dependency key of the root element of the package as a String, value is the corresponding PSImportPackage.
    private final Map<String, PSImportPackage> m_pkgMap = new HashMap<>();

    // Map of previously validated dependencies. Key is the dependency key of the dependency as a String, value is the PSDependency object.
    private final Map<String, PSDependency> m_validatedDeps = new HashMap<>();

    // Map of absent ancestors. Key is the dependency key of the dependency for which the ancestors were added as a String, value is a List of PSDependency objects.
    private final Map<String, List<PSDependency>> m_absentAncs = new HashMap<>();
}
