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

import static java.util.Arrays.asList;

import com.percussion.error.PSException;
import com.percussion.services.assembly.PSAssemblyException;
import java.util.Collection;

/**
 * PSFolderRelationshipBuilder class.
 */
public class PSFolderRelationshipBuilder extends PSAbstractRelationshipBuilder {
  /**
   * Creates a new PSFolderRelationshipBuilder.
   */
  public PSFolderRelationshipBuilder() {
    // default
  }


  private String m_jcrQuery;

  /**
   * add operation.
   *
   * @param sourceId the source id
   * @param targetIds the target ids
   * @throws PSAssemblyException if an error occurs
   * @throws PSException if an error occurs
   */
  @Override
  public void add(int sourceId, Collection<Integer> targetIds)
      throws PSAssemblyException, PSException {
    m_relationshipHelperService.addFolderRelationships(targetIds, asList(sourceId));
  }

  /**
   * delete operation.
   *
   * @param sourceId the source id
   * @param targetIds the target ids
   * @throws PSAssemblyException if an error occurs
   * @throws PSException if an error occurs
   */
  @Override
  public void delete(int sourceId, Collection<Integer> targetIds)
      throws PSAssemblyException, PSException {
    m_relationshipHelperService.deleteFolderRelationships(targetIds, asList(sourceId));
  }

  /**
   * retrieve operation.
   *
   * @param sourceId the source id
   * @return the result
   * @throws PSAssemblyException if an error occurs
   * @throws PSException if an error occurs
   */
  public Collection<Integer> retrieve(int sourceId) throws PSAssemblyException, PSException {
    return m_relationshipHelperService.getFolders(sourceId, m_jcrQuery);
  }

  /**
   * Returns the jcr query.
   *
   * @return the result
   */
  public String getJcrQuery() {
    return m_jcrQuery;
  }

  /**
   * Sets the jcr query.
   *
   * @param jcrQuery the jcr query
   */
  public void setJcrQuery(String jcrQuery) {
    m_jcrQuery = jcrQuery;
  }
}
