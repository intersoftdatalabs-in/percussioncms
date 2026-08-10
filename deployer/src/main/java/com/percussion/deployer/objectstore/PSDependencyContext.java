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

package com.percussion.deployer.objectstore;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Represents and manages the selected state for all dependency instances referencing the same
 * actual object on the server.
 */
public class PSDependencyContext {
  /**
   * Construct a context from a dependency key.
   *
   * @param depKey The key of the dependencies this context will represent, may not be <code>null
   *     </code> or empty.
   * @param treeCtx The tree context of this dependency context, may not be <code>null</code>.
   */
  public PSDependencyContext(String depKey, PSDependencyTreeContext treeCtx) {
    if (depKey == null || depKey.trim().length() == 0)
      throw new IllegalArgumentException("depKey may not be null or empty");

    if (treeCtx == null) throw new IllegalArgumentException("treeCtx may not be null");

    m_depKey = depKey;
    m_treeCtx = treeCtx;
  }

  /**
   * Determines if this context contains the supplied dependency.
   *
   * @param dep The dep to check, may not be <code>null</code>.
   * @return <code>true</code> if a reference to the actual instance of this dependency is found in
   *     this context, <code>false</code> if not.
   */
  public boolean containsDependency(PSDependency dep) {
    if (dep == null) {
      throw new IllegalArgumentException("dep may not be null");
    }
    for (Iterator<PSDependency> it = getDependencies(); it.hasNext(); ) {
      if (it.next() == dep) return true;
    }
    return false;
  }

  /**
   * Add the supplied dependency to this context.
   *
   * @param dep The dependency to add, may not be <code>null</code>, must match the context's
   *     dependency key, and cannot be included if context returns <code>false</code> for {@link
   *     #canBeIncluded()}.
   * @param pkg The package containing the dependency, may not be <code>null</code>.
   */
  public void addDependency(PSDependency dep, PSDeployableElement pkg) {
    if (dep == null || pkg == null) {
      throw new IllegalArgumentException("dep and pkg may not be null");
    }
    if (!dep.getKey().equals(m_depKey)) {
      throw new IllegalArgumentException("dep key does not match context key");
    }
    var depList = m_depMap.computeIfAbsent(pkg.getKey(), k -> new ArrayList<>());
    depList.add(dep);
    m_pkgMap.put(pkg.getKey(), pkg);
    if (!isMulti() && depList.size() > 1) {
      m_treeCtx.notifyCtxChangeListeners(this);
    }
  }

  /**
   * Removes the supplied dependency, possibly de-seleting the other deps if it is an included local
   * dependency, and all others are shared.
   *
   * @param dep The dependency to remove, may not be <code>null</code> and must be contained by this
   *     context.
   * @param removeLocal <code>true</code> to de-select other deps if removing the last local
   *     dependency, <code>false</code> to leave other dependencies unmodified.
   */
  public void removeDependency(PSDependency dep, boolean removeLocal) {
    if (dep == null) throw new IllegalArgumentException("dep may not be null");

    // get the package && remove the dependency
    boolean isMulti = isMulti();

    Entry<String, List<PSDependency>> entry = getPkgEntry(dep);
    if (entry == null) {
      throw new IllegalArgumentException("dep not found in this ctx");
    }

    String pkgKey = entry.getKey();
    PSDeployableElement pkg = m_pkgMap.get(pkgKey);
    List<PSDependency> depList = entry.getValue();

    // remove same instance from the list and pkg from map if empty
    Iterator<PSDependency> deps = depList.iterator();
    while (deps.hasNext()) {
      if (dep == deps.next()) {
        deps.remove();
        break;
      }
    }

    if (depList.size() == 0) {
      m_pkgMap.remove(pkgKey);
      m_depMap.remove(pkgKey);
    }

    // now deselect others if necessary
    if (removeLocal) {
      Map<String, List<PSDependency>> removeMap = new HashMap<>();
      checkRemoveLocal(dep, pkg, removeMap);
      Iterator<PSDependency> affected = getValueLists(removeMap);
      while (affected.hasNext()) {
        PSDependency affectedDep = affected.next();
        affectedDep.setIsIncluded(false);
      }
    }

    // update the included state
    m_isIncluded = checkIncludedState();

    // if switching from multi to single, counts as a change
    if (isMulti && !isMulti()) m_treeCtx.notifyCtxChangeListeners(this);
  }

  /**
   * Add the child dependencies of the supplied dependency to this context.
   *
   * @param dep The dependency, must have already been added to this context, may not be <code>null
   *     </code>.
   */
  public void addChildDependencies(PSDependency dep) {
    if (dep == null) throw new IllegalArgumentException("dep may not be null");

    Entry<String, List<PSDependency>> pkgEntry = getPkgEntry(dep);
    if (pkgEntry == null) {
      throw new IllegalArgumentException("dep not found in this ctx");
    }

    Iterator<PSDependency> childDeps = dep.getDependencies();
    if (childDeps == null) return;

    String pkgKey = pkgEntry.getKey();
    PSDeployableElement pkg = m_pkgMap.get(pkgKey);
    while (childDeps.hasNext()) {
      PSDependency childDep = childDeps.next();
      m_treeCtx.addDependency(childDep, pkg);
    }
  }

  /**
   * Determine if the dependencies represented by this context are selected (included) in the
   * archive.
   *
   * @return <code>true</code> if they are included, <code>false</code> otherwise.
   */
  public boolean isIncluded() {
    return m_isIncluded;
  }

  /**
   * Determine if the selected state of the dependency represented by this context may be modified.
   * This means that for all dependencies represented by this context {@link
   * PSDependency#canBeIncludedExcluded()} returns <code>true</code>.
   *
   * @return <code>true</code> if this context references dependencies that can be
   *     included/excluded, <code>false</code> if not.
   */
  public boolean canBeSelected() {
    return canBeIncludedExcluded(false);
  }

  /**
   * Determine if the dependency represented by this context may be included in an archive. This
   * means that for all dependencies represented by this context either {@link
   * PSDependency#canBeIncludedExcluded()} returns <code>true</code>, or the dependency is of type
   * {@link PSDependency#TYPE_LOCAL}.
   *
   * @return <code>true</code> if this context references dependencies that can be included, <code>
   *     false</code> otherwise.
   */
  public boolean canBeIncluded() {
    return canBeIncludedExcluded(true);
  }

  /**
   * Determine if the dependency represented by this context may be included in or excluded from an
   * archive. This means that for all dependencies represented by this context either {@link
   * PSDependency#canBeIncludedExcluded()} returns <code>true</code>, or <code>allowLocal</code> is
   * <code>true</code> and the dependency is of type {@link PSDependency#TYPE_LOCAL}.
   *
   * @param allowLocal <code>true</code> to consider local dependencies as includable/excludable,
   *     <code>false</code> to consider them as not.
   * @return <code>true</code> if this context references dependencies that can be
   *     included/excluded, <code>false</code> if not.
   */
  private boolean canBeIncludedExcluded(boolean allowLocal) {
    boolean canInclude = true;

    Iterator<PSDependency> deps = getDependencies();
    while (deps.hasNext() && canInclude) {
      PSDependency dep = deps.next();
      canInclude =
          (dep.canBeIncludedExcluded()
              || (allowLocal && dep.getDependencyType() == PSDependency.TYPE_LOCAL));
    }

    return canInclude;
  }

  /**
   * Determine if the dependencies represented by this context are selected (included) in the
   * archive (all must have the same selected state) by walking all dependencies and checking their
   * included state.
   *
   * @return <code>true</code> if they are included, <code>false</code> otherwise.
   */
  private boolean checkIncludedState() {
    boolean isIncluded = false;

    // need to check dependencies within the context of their package
    Iterator<Entry<String, List<PSDependency>>> entries = m_depMap.entrySet().iterator();
    while (entries.hasNext() && !isIncluded) {
      Entry<String, List<PSDependency>> entry = entries.next();
      String pkgKey = entry.getKey();
      PSDeployableElement pkg = m_pkgMap.get(pkgKey);
      Iterator<PSDependency> deps = entry.getValue().iterator();
      while (deps.hasNext()) {
        PSDependency dep = deps.next();
        // must check if specific instance of this dep is included as pkg
        // may include other instances of the same dep that have not yet
        // been added to the context
        isIncluded = pkg.includesDependency(dep, true);
      }
    }

    return isIncluded;
  }

  /**
   * Determine if this context represents more than one dependency instance.
   *
   * @return <code>true</code> if it does, <code>false</code> otherwise.
   */
  public boolean isMulti() {
    boolean isMulti = false;
    boolean hasOne = false;

    for (Iterator<PSDependency> deps = getDependencies(); deps.hasNext() && !isMulti; deps.next()) {
      if (hasOne) isMulti = true;
      else hasOne = true;
    }

    return isMulti;
  }

  /**
   * Sets all dependencies as included/excluded.
   *
   * @param isIncluded <code>true</code> to set them all as included, <code>false</code> to set them
   *     all as excluded.
   * @return <code>true</code> if the setting could be made, <code>false</code> if not (e.g. could
   *     not set a system dependency as included).
   */
  public boolean setIncluded(boolean isIncluded) {
    boolean canSet = canBeIncluded() && (isIncluded != isIncluded());

    if (canSet) {
      Iterator<PSDependency> deps = getDependencies();
      while (deps.hasNext()) {
        PSDependency dep = deps.next();
        if (dep.isIncluded() ^ isIncluded) dep.setIsIncluded(isIncluded);
      }
      m_isIncluded = isIncluded;
    }

    return canSet;
  }

  /**
   * Get an iterator over all dependencies this context represents.
   *
   * @return The iterator, never <code>null</code>, may be empty.
   */
  private Iterator<PSDependency> getDependencies() {
    return m_depMap.values().stream().flatMap(Collection::stream).iterator();
  }

  /**
   * If the supplied dependency is an included local dependency of the supplied package, determines
   * if the dependencies from any other packages included in this context are only shared deps, and
   * if so adds them to the supplied map, preserving any existing entries in the map. If any other
   * packages contain a matching included local dependency, nothing is added to the map.
   *
   * @param dep The dependency to check, may not be <code>null</code> and must have a key matching
   *     this context's key.
   * @param pkg The package from which the dependency is to be removed, may not be <code>null</code>
   *     and must contain the supplied dependency.
   * @param resultMap Map of local dependencies that appear only as included shared dependencies in
   *     other packages. Key is the dependency key of the package as a <code>String</code>, value is
   *     a list of matching shared dependencies found in that package as <code>PSDependency</code>
   *     objects. May not be <code>null</code>.
   */
  public void checkRemoveLocal(
      PSDependency dep, PSDeployableElement pkg, Map<String, List<PSDependency>> resultMap) {
    if (dep == null) throw new IllegalArgumentException("dep may not be null");

    if (pkg == null) throw new IllegalArgumentException("pkg may not be null");

    if (resultMap == null) throw new IllegalArgumentException("resultMap may not be null");

    if (!dep.getKey().equals(m_depKey))
      throw new IllegalArgumentException("dep key does not match context key");

    // nothing to do if not an included local dependency
    if (!(dep.getDependencyType() == PSDependency.TYPE_LOCAL && isIncluded())) {
      return;
    }

    // check other packages
    Map<String, List<PSDependency>> tmpMap = new HashMap<>();
    boolean allShared = true;
    Iterator<Entry<String, List<PSDependency>>> entries = m_depMap.entrySet().iterator();
    while (entries.hasNext() && allShared) {
      Entry<String, List<PSDependency>> entry = entries.next();
      String pkgKey = entry.getKey();

      // skip package being removed
      if (pkgKey.equals(pkg.getKey())) continue;

      // build combined list of deps already in result map for this pkg
      List<PSDependency> shared = new ArrayList<>();
      List<PSDependency> resultList = resultMap.get(pkgKey);
      if (resultList != null) shared.addAll(resultList);
      Iterator<PSDependency> deps = entry.getValue().iterator();
      while (deps.hasNext() && allShared) {
        PSDependency test = deps.next();
        if (test.canBeIncludedExcluded()) shared.add(test);
        else allShared = false;
      }

      if (allShared) tmpMap.put(pkgKey, shared);
    }

    // if our results are valid, replace entries in map (we've already
    // merged with previous results for same pkg.
    if (allShared) resultMap.putAll(tmpMap);
  }

  /**
   * Get the key of the dependencies this context represents.
   *
   * @return The key supplied during construction, never <code>null</code> or empty.
   */
  public String getKey() {
    return m_depKey;
  }

  /**
   * Given a map where the entry values are <code>List</code> objects, get an iterator over all
   * objects in all lists.
   *
   * @param valMap The map, assumed not <code>null</code> and to have all <code>List</code> values.
   * @return The iterator over all list entries, never <code>null</code>.
   */
  private Iterator<PSDependency> getValueLists(Map<String, List<PSDependency>> valMap) {
    return valMap.values().stream().flatMap(Collection::stream).iterator();
  }

  /**
   * Get the entry from the {@link #m_depMap} that contains the supplied dependency in its value
   * list.
   *
   * @param dep The dependency to check for, assumed not <code>null</code>.
   * @return The entry, or <code>null</code> if not found. See {@link #m_depMap} for more info.
   */
  private Entry<String, List<PSDependency>> getPkgEntry(PSDependency dep) {
    Entry<String, List<PSDependency>> pkgEntry = null;
    Iterator<Entry<String, List<PSDependency>>> entries = m_depMap.entrySet().iterator();
    while (entries.hasNext() && pkgEntry == null) {
      Entry<String, List<PSDependency>> entry = entries.next();
      List<PSDependency> testDeps = entry.getValue();
      Iterator<PSDependency> deps = testDeps.iterator();
      while (deps.hasNext()) {
        PSDependency testdep = deps.next();
        if (dep == testdep) {
          pkgEntry = entry;
          break;
        }
      }
    }

    return pkgEntry;
  }

  /**
   * The dependency key of all dependencies this context represents. Supplied during construction,
   * never <code>null</code> or empty or modified after that.
   */
  private String m_depKey;

  /**
   * The tree context of this dependency context, never <code>null</code> or modified after
   * contstruction.
   */
  private PSDependencyTreeContext m_treeCtx;

  /**
   * Map of depenencies by package, never <code>null</code> or modified. Key is the dependency key
   * of the package containing the dependency, value is a <code>List</code> of {@link PSDependency}
   * objects.
   */
  private Map<String, List<PSDependency>> m_depMap = new HashMap<>();

  /**
   * Map of package objects by package key, never <code>null</code> or modified. Key is the
   * package's dependency key as a <code>String</code>, value is the matching {@link
   * PSDeployableElement} object.
   */
  private Map<String, PSDeployableElement> m_pkgMap = new HashMap<>();

  /**
   * Determines if the dependencies represented by this context are included in the archive. <code>
   * true</code> if they are included, <code>false</code> otherwise. Modified by {@link
   * #addDependency(PSDependency, PSDeployableElement)}, {@link #removeDependency(PSDependency,
   * boolean)}, and {@link #setIncluded(boolean)}.
   */
  private boolean m_isIncluded = false;
}
