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
package com.percussion.services.pkginfo;

import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.error.PSDuplicateNameException;
import com.percussion.services.error.PSNotFoundException;
import com.percussion.services.pkginfo.data.PSPkgDependency;
import com.percussion.services.pkginfo.data.PSPkgElement;
import com.percussion.services.pkginfo.data.PSPkgElementDependency;
import com.percussion.services.pkginfo.data.PSPkgInfo;
import com.percussion.utils.guid.IPSGuid;

import java.util.List;

/**
 * Primary interface for the Package Information Service.
 * <p>
 * A Package is a collection of design objects that make up an installable, configurable
 * "solution" such as a site, blog, RSS feed, etc.
 * <p>
 * This service supports three objects:
 * <ul>
 *   <li>Package Information ({@link PSPkgInfo})</li>
 *   <li>Package Element ({@link PSPkgElement})</li>
 *   <li>Package Element Dependency ({@link PSPkgElementDependency})</li>
 * </ul>
 * <p>
 * Methods exist for each object, following the pattern:
 * <ul>
 *   <li>{@code createXXX}</li>
 *   <li>{@code copyXXX}</li>
 *   <li>{@code saveXXX}</li>
 *   <li>{@code deleteXXX}</li>
 *   <li>{@code findXXX}</li>
 *   <li>{@code loadXXX}</li>
 *   <li>{@code loadXXXModifiable}</li>
 * </ul>
 * <p>
 * All {@code loadXXX} and {@code findXXX} methods work with cached objects.
 * Cached objects are shared and must not be modified by the caller.
 * If an object needs to be modified, use {@code loadXXXModifiable}.
 */
public interface IPSPkgInfoService {

  // ---------------------------------------------------------------------------
  // Package Information object: PSPkgInfo
  // ---------------------------------------------------------------------------

  /**
   * Creates a new instance of {@link PSPkgInfo}.
   *
   * @param name The internal name of the newly created object; must not be null or empty.
   * @return The newly created object; never null.
   */
  PSPkgInfo createPkgInfo(String name);

  /**
   * Creates a new instance of {@link PSPkgInfo} and copies the content from the last entry if it exists.
   *
   * @param name The internal name of the newly created object; must not be null or empty.
   * @return The newly created object with the data from the last entry if it exists; never null.
   */
  PSPkgInfo createPkgInfoCopy(String name);

  /**
   * Saves the supplied object to persistent storage.
   *
   * @param obj The {@link PSPkgInfo} object to save; never null.
   * @throws IllegalStateException If the object is found in the cache.
   * @throws PSDuplicateNameException If the name is not unique among all objects of this type.
   */
  void savePkgInfo(PSPkgInfo obj);

  /**
   * Permanently removes the given package's elements and dependency entries from persistent storage (and caches).
   * If the object does not exist, this call has no effect.
   *
   * @param id The ID of the package info to be deleted; never null.
   */
  void deletePkgInfoChildren(IPSGuid id);

  /**
   * Deletes package elements and dependencies by name.
   *
   * @param name The name of the object to permanently remove from persistent storage, case-insensitive; never null or empty.
   */
  void deletePkgInfoChildren(String name);

  /**
   * Permanently removes the given package info from persistent storage (and caches).
   * If the object does not exist, this call has no effect.
   *
   * @param id The ID of the package info to be deleted; never null.
   */
  void deletePkgInfo(IPSGuid id);

  /**
   * Deletes a package info by name.
   *
   * @param name The name of the object to permanently remove from persistent storage, case-insensitive; never null or empty.
   */
  void deletePkgInfo(String name);

  /**
   * Finds the object whose name matches the supplied name, case-insensitive.
   *
   * @param name May be null or empty.
   * @return The object that matches the name, or null if not found.
   */
  PSPkgInfo findPkgInfo(String name);

  /**
   * Finds all {@link PSPkgInfo} objects currently on the system.
   *
   * @return Never null; may be empty.
   */
  List<PSPkgInfo> findAllPkgInfos();

  /**
   * Loads the {@link PSPkgInfo} object with the supplied id.
   *
   * @param id The unique identifier; must not be null.
   * @return The object matching the requested id; never null.
   * @throws PSNotFoundException If a matching object is not found.
   */
  PSPkgInfo loadPkgInfo(IPSGuid id) throws PSNotFoundException;

  /**
   * Loads a {@link PSPkgInfo} object that needs to be modified.
   *
   * @param id Never null.
   * @return The matching object; never null.
   * @throws PSNotFoundException If a matching object is not found.
   */
  PSPkgInfo loadPkgInfoModifiable(IPSGuid id) throws PSNotFoundException;

  // ---------------------------------------------------------------------------
  // Package Element object: PSPkgElement
  // ---------------------------------------------------------------------------

  /**
   * Creates a new instance of {@link PSPkgElement}.
   *
   * @param parentId The GUID of the parent {@link PSPkgInfo} object; must not be null.
   * @return The newly created object; never null.
   */
  PSPkgElement createPkgElement(IPSGuid parentId);

  /**
   * Saves the supplied object to persistent storage.
   *
   * @param obj The {@link PSPkgElement} object to save; never null.
   * @throws IllegalStateException If the object is found in the cache.
   * @throws PSDuplicateNameException If the name is not unique among all objects of this type.
   */
  void savePkgElement(PSPkgElement obj);

  /**
   * Permanently removes the designated object from persistent storage (and caches).
   * If the object does not exist, this call has no effect.
   *
   * @param id The unique identifier; never null.
   */
  void deletePkgElement(IPSGuid id);

  /**
   * Returns the list of GUIDs of {@link PSPkgElement} objects that correspond to the given {@link PSPkgInfo} object.
   *
   * @param parentPkgInfo The GUID of the parent {@link PSPkgInfo} object; never null.
   * @return A list of GUIDs for the {@link PSPkgElement} objects; never null, may be empty.
   */
  List<IPSGuid> findPkgElementGuids(IPSGuid parentPkgInfo);

  /**
   * Finds all children of the supplied package.
   *
   * @param pkgId The id of the parent package; never null.
   * @return All objects that are considered part of the supplied package; never null, may be empty.
   */
  List<PSPkgElement> findPkgElements(IPSGuid pkgId);

  /**
   * Finds an individual package object given its id.
   *
   * @param id The unique identifier; must not be null.
   * @return The object matching the requested id, or null if not found.
   */
  PSPkgElement findPkgElement(IPSGuid id);

  /**
   * Finds the design object in an installed package that matches the supplied GUID.
   *
   * @param objId The object type identifier; must not be null.
   * @return The object whose object GUID matches the supplied GUID, or null if not found.
   */
  PSPkgElement findPkgElementByObject(IPSGuid objId);

  /**
   * Loads all {@link PSPkgElement} objects whose GUIDs are supplied.
   *
   * @param ids The list of {@link PSPkgElement} GUIDs to match; never null, no null entries allowed.
   * @return All objects whose GUID matches the list; never null, never empty.
   * @throws PSNotFoundException If a matching object is not found.
   */
  List<PSPkgElement> loadPkgElements(List<IPSGuid> ids) throws PSNotFoundException;

  /**
   * Convenience method that puts the supplied id in a list and calls {@link #loadPkgElements(List)}.
   *
   * @param id The unique identifier; must not be null.
   * @return The object matching the requested id, or null if not found.
   * @throws PSNotFoundException If a matching object is not found.
   */
  PSPkgElement loadPkgElement(IPSGuid id) throws PSNotFoundException;

  /**
   * Loads a {@link PSPkgElement} object that needs to be modified.
   *
   * @param id Never null.
   * @return The matching object; never null.
   * @throws PSNotFoundException If a matching object is not found.
   */
  PSPkgElement loadPkgElementModifiable(IPSGuid id) throws PSNotFoundException;

  // ******* Package dependency methods

  /**
   * Creates a new instance of {@link PSPkgDependency}.
   *
   * @return The newly created {@link PSPkgDependency} object; never null.
   */
  PSPkgDependency createPkgDependency();

  /**
   * Saves the supplied package dependency object.
   *
   * @param pkgDependency The {@link PSPkgDependency} to save; must not be null.
   */
  void savePkgDependency(PSPkgDependency pkgDependency);

  /**
   * Loads the package dependency objects for the supplied guid of {@link PSPkgInfo}.
   *
   * @param guid The guid of type {@link PSTypeEnum#PACKAGE_INFO}; must not be null.
   * @param depType If true, considers the supplied guid as owner; otherwise as dependent.
   * @return List of {@link PSPkgDependency} objects; never null, may be empty.
   */
  List<PSPkgDependency> loadPkgDependencies(IPSGuid guid, boolean depType);

  /**
   * Loads the modifiable package dependency objects for the supplied guid of type {@link PSTypeEnum#PACKAGE_INFO}.
   *
   * @param guid The guid of type {@link PSTypeEnum#PACKAGE_INFO}; must not be null.
   * @param depType If true, considers the supplied guid as owner; otherwise as dependent.
   * @return List of {@link PSPkgDependency} objects; never null, may be empty.
   */
  List<PSPkgDependency> loadPkgDependenciesModifiable(IPSGuid guid, boolean depType);

  /**
   * Finds and returns the owner package info GUIDs of the supplied package info guid.
   *
   * @param guid GUID of the {@link PSPkgInfo} object; must not be null.
   * @return List of {@link PSPkgInfo} object GUIDs that are owners of the supplied guid; may be empty, never null.
   */
  List<IPSGuid> findOwnerPkgGuids(IPSGuid guid);

  /**
   * Finds and returns the dependent package info GUIDs of the supplied package info guid.
   *
   * @param guid GUID of the {@link PSPkgInfo} object; must not be null.
   * @return List of {@link PSPkgInfo} object GUIDs that are dependents of the supplied guid; may be empty, never null.
   */
  List<IPSGuid> findDependentPkgGuids(IPSGuid guid);

  /**
   * Permanently removes the {@link PSPkgDependency} object corresponding to the supplied id from persistent storage.
   * If the object does not exist, this call has no effect.
   *
   * @param pkgDepId The id of the {@link PSPkgDependency} object to be deleted.
   */
  void deletePkgDependency(long pkgDepId);
}
