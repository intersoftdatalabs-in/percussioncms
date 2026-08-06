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

package com.percussion.deployer.server.dependencies;

import com.percussion.deployer.objectstore.PSDependency;
import com.percussion.deployer.objectstore.PSDependencyFile;
import com.percussion.deployer.objectstore.PSIdMap;
import com.percussion.deployer.objectstore.PSIdMapping;
import com.percussion.deployer.server.PSArchiveHandler;
import com.percussion.deployer.server.PSDependencyDef;
import com.percussion.deployer.server.PSDependencyMap;
import com.percussion.deployer.server.PSImportCtx;
import com.percussion.error.IPSDeploymentErrors;
import com.percussion.error.PSDeployException;
import com.percussion.security.PSSecurityToken;
import com.percussion.services.assembly.PSAssemblyException;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.error.PSNotFoundException;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * Defines the contract for classes that discover, package, and install a single type of dependency
 * during deployment. Concrete implementations are produced via {@link #getHandlerInstance} from a
 * {@link PSDependencyDef} and operate against a shared {@link PSDependencyMap}.
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public interface IPSDependencyHandler {

  /**
   * Gets a handler instance using the supplied def
   *
   * @param def The def for which the appropriate handler type should be returned. May not be <code>
   *     null</code>.
   * @param map The dependency map, may not be <code>null</code>.
   * @return The handler, never <code>null</code>.
   * @throws IllegalArgumentException if <code>def</code> is <code>null</code>.
   * @throws PSDeployException if there are any errors.
   */
  static PSDependencyHandler getHandlerInstance(PSDependencyDef def, PSDependencyMap map)
      throws PSDeployException {
    if (def == null || map == null) {
      throw new IllegalArgumentException("def and map may not be null");
    }

    var className = def.getHandlerClassName();
    try {
      var handlerClass = Class.forName(className);
      var handlerCtor = handlerClass.getConstructor(PSDependencyDef.class, PSDependencyMap.class);
      return (PSDependencyHandler) handlerCtor.newInstance(def, map);
    } catch (ClassNotFoundException
        | InstantiationException
        | IllegalAccessException
        | NoSuchMethodException e) {
      throw new PSDeployException(
          IPSDeploymentErrors.DEPENDENCY_HANDLER_INIT,
          new Object[] {className, e.getLocalizedMessage()});
    } catch (InvocationTargetException e) {
      var origException = e.getTargetException();
      throw new PSDeployException(
          IPSDeploymentErrors.DEPENDENCY_HANDLER_INIT,
          new Object[] {
            className,
            origException.getClass().getName() + ": " + origException.getLocalizedMessage()
          });
    }
  }

  /**
   * Determines whether this handler supports the supplied child dependency type.
   *
   * @param child The child dependency to test, may not be <code>null</code>.
   * @return <code>true</code> if this handler can process the child dependency, <code>false</code>
   *     otherwise.
   */
  boolean isChildTypeSupported(PSDependency child);

  /**
   * Gets all dependencies that are child dependecies of the supplied dependency. Note: Add IDType
   * dependencies this method
   *
   * @param tok The security token to use if objectstore access is required, may not be <code>null
   *     </code>.
   * @param dep A dependency of the type defined by this handler, may not be <code>null</code>.
   * @return iterator over zero or more <code>PSDependency</code> objects, never <code>null</code>,
   *     may be empty.
   * @throws IllegalArgumentException if dep is invalid.
   * @throws PSDeployException if there are any errors.
   */
  Iterator<PSDependency> getChildDependencies(PSSecurityToken tok, PSDependency dep)
      throws PSDeployException, PSNotFoundException;

  /**
   * Gets the dependency files that belong to the supplied dependency.
   *
   * @param tok The security token to use if objectstore access is required, may not be <code>null
   *     </code>.
   * @param dep A dependency of the type defined by this handler, may not be <code>null</code>.
   * @return An iterator over zero or more <code>PSDependencyFile</code> objects, never <code>null
   *     </code>, may be empty.
   * @throws PSDeployException if there are any errors.
   */
  Iterator<PSDependencyFile> getDependencyFiles(PSSecurityToken tok, PSDependency dep)
      throws PSDeployException, PSNotFoundException;

  /**
   * Installs the files contained in the supplied archive as part of the supplied dependency.
   *
   * @param tok The security token to use if objectstore access is required, may not be <code>null
   *     </code>.
   * @param archive The archive handler supplying the files to install, may not be <code>null
   *     </code>.
   * @param dep The dependency being installed, may not be <code>null</code>.
   * @param ctx The import context, may not be <code>null</code>.
   * @throws PSDeployException if there are any errors.
   * @throws PSAssemblyException if an assembly error occurs during installation.
   */
  void installDependencyFiles(
      PSSecurityToken tok, PSArchiveHandler archive, PSDependency dep, PSImportCtx ctx)
      throws PSDeployException, PSAssemblyException, PSNotFoundException;

  /**
   * Gets all dependencies of this type that exist on the Rhythmyx server.
   *
   * @param tok The security token to use if objectstore access is required, may not be <code>null
   *     </code>.
   * @return An iterator over zero or more <code>PSDependency</code> objects.
   * @throws IllegalArgumentException if <code>tok</code> is invalid.
   * @throws PSDeployException if there are any errors.
   */
  Iterator<PSDependency> getDependencies(PSSecurityToken tok)
      throws PSDeployException, PSNotFoundException;

  /**
   * Gets all dependencies of this type whose parent matches the supplied parent type and id.
   *
   * @param tok The security token to use if objectstore access is required, may not be <code>null
   *     </code>.
   * @param parentType The type of the parent dependency, may not be <code>null</code> or empty.
   * @param parentId The id of the parent dependency, may not be <code>null</code> or empty.
   * @return An iterator over zero or more <code>PSDependency</code> objects, never <code>null
   *     </code>, may be empty.
   * @throws PSDeployException if there are any errors.
   */
  Iterator getDependencies(PSSecurityToken tok, String parentType, String parentId)
      throws PSDeployException;

  /**
   * Gets the dependency of this type with the supplied id.
   *
   * @param tok The security token to use if objectstore access is required, may not be <code>null
   *     </code>.
   * @param id The id of the dependency to retrieve, may not be <code>null</code> or empty.
   * @return The matching dependency, may be <code>null</code> if no dependency with the supplied id
   *     exists.
   * @throws PSDeployException if there are any errors.
   */
  PSDependency getDependency(PSSecurityToken tok, String id)
      throws PSDeployException, PSNotFoundException;

  /**
   * Gets the dependency of this type with the supplied id under the supplied parent.
   *
   * @param tok The security token to use if objectstore access is required, may not be <code>null
   *     </code>.
   * @param id The id of the dependency to retrieve, may not be <code>null</code> or empty.
   * @param parentType The type of the parent dependency, may not be <code>null</code> or empty.
   * @param parentId The id of the parent dependency, may not be <code>null</code> or empty.
   * @return The matching dependency, may be <code>null</code> if no dependency with the supplied id
   *     exists under the given parent.
   * @throws PSDeployException if there are any errors.
   */
  PSDependency getDependency(PSSecurityToken tok, String id, String parentType, String parentId)
      throws PSDeployException;

  /**
   * Adds ACL dependencies for the supplied dependency to the supplied collection of child
   * dependencies.
   *
   * @param tok The security token to use if objectstore access is required, may not be <code>null
   *     </code>.
   * @param key The type enum that identifies the dependency, may not be <code>null</code>.
   * @param dep The dependency whose ACLs are being added, may not be <code>null</code>.
   * @param childDeps The collection to which discovered ACL dependencies are added, may not be
   *     <code>null</code>.
   * @throws PSDeployException if there are any errors.
   */
  void addAclDependency(
      PSSecurityToken tok, PSTypeEnum key, PSDependency dep, Collection<PSDependency> childDeps)
      throws PSDeployException, PSNotFoundException;

  /**
   * Derived classes must override this method to provide the list of child dependency types they
   * can discover.
   *
   * @return An iterator over zero or more types as <code>String</code> objects, never <code>null
   *     </code>, does not contain <code>null</code> or empty entries.
   */
  Iterator getChildTypes();

  /**
   * Must be overriden by derived classes to supply the correct type.
   *
   * @return the type of dependency supported by this handler, never <code>null</code> or empty.
   */
  String getType();

  /**
   * Returns the type identifier of the parent dependency for dependencies handled by this handler.
   *
   * @return The parent dependency type, may be <code>null</code> if the handler does not have a
   *     parent type.
   */
  String getParentType();

  /**
   * Determines whether a dependency with the supplied id exists.
   *
   * @param tok The security token to use if objectstore access is required, may not be <code>null
   *     </code>.
   * @param id The id to check for, may not be <code>null</code> or empty.
   * @return <code>true</code> if a matching dependency exists, <code>false</code> otherwise.
   * @throws PSDeployException if there are any errors.
   */
  boolean doesDependencyExist(PSSecurityToken tok, String id)
      throws PSDeployException, PSNotFoundException;

  /**
   * Determines whether a child dependency with the supplied id exists under the supplied parent.
   *
   * @param tok The security token to use if objectstore access is required, may not be <code>null
   *     </code>.
   * @param id The child id to check for, may not be <code>null</code> or empty.
   * @param parentId The id of the parent dependency, may not be <code>null</code> or empty.
   * @return <code>true</code> if a matching child dependency exists, <code>false</code> otherwise.
   * @throws PSDeployException if there are any errors.
   */
  boolean doesDependencyExist(PSSecurityToken tok, String id, String parentId)
      throws PSDeployException;

  /**
   * Reserves a new id for the supplied dependency in the supplied id map.
   *
   * @param dep The dependency for which a new id is being reserved, may not be <code>null</code>.
   * @param idMap The id map in which the new id is reserved, may not be <code>null</code>.
   * @throws PSDeployException if there are any errors.
   */
  void reserveNewId(PSDependency dep, PSIdMap idMap) throws PSDeployException;

  /**
   * Indicates whether installation of dependencies of this type should be deferred to a later phase
   * of the import process.
   *
   * @return <code>true</code> if installation should be deferred, <code>false</code> otherwise.
   */
  boolean shouldDeferInstallation();

  /**
   * Indicates whether this handler delegates id mapping to a sub-handler.
   *
   * @return <code>true</code> if id mapping is delegated, <code>false</code> otherwise.
   */
  boolean delegatesIdMapping();

  /**
   * Returns the id mapping type used by this handler.
   *
   * @return The id mapping type, never <code>null</code> or empty.
   */
  String getIdMappingType();

  /**
   * Returns the id mapping type of the parent dependency for this handler.
   *
   * @return The parent id mapping type, may be <code>null</code> if the handler has no parent type.
   */
  String getParentIdMappingType();

  /**
   * Indicates whether the supplied child type is required by this handler.
   *
   * @param type The child type to test, may not be <code>null</code> or empty.
   * @return <code>true</code> if the child type is required, <code>false</code> otherwise.
   */
  boolean isRequiredChild(String type);

  /**
   * Indicates whether this handler overwrites an existing dependency on install.
   *
   * @return <code>true</code> if installation overwrites the existing dependency, <code>false
   *     </code> otherwise.
   */
  boolean overwritesOnInstall();

  /**
   * Returns the target id from the supplied id mapping for the supplied source id.
   *
   * @param mapping The id mapping to consult, may not be <code>null</code>.
   * @param id The source id to translate, may not be <code>null</code> or empty.
   * @return The target id, never <code>null</code> or empty.
   * @throws PSDeployException if there are any errors.
   */
  String getTargetId(PSIdMapping mapping, String id) throws PSDeployException;

  /**
   * Returns the list of external DBMS information objects for the supplied dependency.
   *
   * @param tok The security token to use if objectstore access is required, may not be <code>null
   *     </code>.
   * @param dep The dependency whose external DBMS info is being requested, may not be <code>null
   *     </code>.
   * @return A list of external DBMS info objects, never <code>null</code>, may be empty.
   * @throws PSDeployException if there are any errors.
   */
  List getExternalDbmsInfoList(PSSecurityToken tok, PSDependency dep) throws PSDeployException;

  /**
   * Returns the id mapping for the supplied id under the supplied parent.
   *
   * @param idMap The id map to consult, may not be <code>null</code>.
   * @param id The id whose mapping is requested, may not be <code>null</code> or empty.
   * @param type The type of the dependency, may not be <code>null</code> or empty.
   * @param parentId The id of the parent dependency, may not be <code>null</code> or empty.
   * @param parentType The type of the parent dependency, may not be <code>null</code> or empty.
   * @return The id mapping, may be <code>null</code> if no mapping exists.
   * @throws PSDeployException if there are any errors.
   */
  PSIdMapping getIdMapping(
      PSIdMap idMap, String id, String type, String parentId, String parentType)
      throws PSDeployException;

  /**
   * Adds a transaction log entry for the supplied dependency using the supplied type enum.
   *
   * @param dep The dependency being logged, may not be <code>null</code>.
   * @param ctx The import context, may not be <code>null</code>.
   * @param type The type enum identifying the dependency, may not be <code>null</code>.
   * @param isNew <code>true</code> if this is a new dependency being added, <code>false</code>
   *     otherwise.
   * @throws PSDeployException if there are any errors.
   */
  void addTransactionLogEntryByGuidType(
      PSDependency dep, PSImportCtx ctx, PSTypeEnum type, boolean isNew) throws PSDeployException;

  /**
   * Adds a transaction log entry for the supplied dependency using the supplied element name and
   * type.
   *
   * @param dep The dependency being logged, may not be <code>null</code>.
   * @param ctx The import context, may not be <code>null</code>.
   * @param elementName The name of the element being logged, may not be <code>null</code> or empty.
   * @param elementType The type of the element being logged, may not be <code>null</code> or empty.
   * @param action The action code being logged.
   * @throws PSDeployException if there are any errors.
   */
  void addTransactionLogEntry(
      PSDependency dep, PSImportCtx ctx, String elementName, String elementType, int action)
      throws PSDeployException;
}
