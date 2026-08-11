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

package com.percussion.pso.relationshipbuilder;

import com.percussion.error.PSException;
import com.percussion.services.assembly.PSAssemblyException;
import java.util.Collection;

// TODO JAVADOC
/**
 * IPSRelationshipHelperService interface.
 */
public interface IPSRelationshipHelperService {

  /**
   * Gets all folder that have the item AND are in the results of the provided query.
   *
   * @param itemId the id of the item.
   * @param jcrQuery the jcr query to find the folders. Make sure you select on sys_folder.
   * @return the ids of the folders.
   * @throws IllegalArgumentException if the query is bad.
   */
  public abstract Collection<Integer> getFolders(int itemId, String jcrQuery);

  /**
   * Returns the owners.
   *
   * @param dependentId the dependent id
   * @param slotName the slot name
   * @param templateName the template name
   * @return the result
   * @throws PSException if an error occurs
   */
  public abstract Collection<Integer> getOwners(
      int dependentId, String slotName, String templateName) throws PSException;

  /**
   * Returns the dependents.
   *
   * @param ownerId the owner id
   * @param slotName the slot name
   * @param templateName the template name
   * @return the result
   * @throws PSException if an error occurs
   */
  public abstract Collection<Integer> getDependents(
      int ownerId, String slotName, String templateName) throws PSException;

  /**
   * deleteRelationships operation.
   *
   * @param owners the owners
   * @param dependents the dependents
   * @param slotName the slot name
   * @param templateName the template name
   * @throws PSException if an error occurs
   */
  public abstract void deleteRelationships(
      Collection<Integer> owners,
      Collection<Integer> dependents,
      String slotName,
      String templateName)
      throws PSException;

  /**
   * deleteFolderRelationships operation.
   *
   * @param folderIds the folder ids
   * @param itemIds the item ids
   * @throws PSException if an error occurs
   */
  public abstract void deleteFolderRelationships(
      Collection<Integer> folderIds, Collection<Integer> itemIds) throws PSException;

  /**
   * addRelationships operation.
   *
   * @param ownerIds the owner ids
   * @param dependentIds the dependent ids
   * @param slotName the slot name
   * @param templateName the template name
   * @throws PSAssemblyException if an error occurs
   * @throws PSException if an error occurs
   */
  public abstract void addRelationships(
      Collection<Integer> ownerIds,
      Collection<Integer> dependentIds,
      String slotName,
      String templateName)
      throws PSAssemblyException, PSException;

  /**
   * addFolderRelationships operation.
   *
   * @param folderIds the folder ids
   * @param itemIds the item ids
   * @throws PSException if an error occurs
   */
  public abstract void addFolderRelationships(
      Collection<Integer> folderIds, Collection<Integer> itemIds) throws PSException;
}
