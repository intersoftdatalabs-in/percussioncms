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

package com.percussion.rest.communities;

import com.percussion.rest.GuidList;
import com.percussion.rest.ObjectTypeEnum;
import com.percussion.webservices.PSErrorResultsException;
import java.rmi.RemoteException;
import java.util.List;

/** Adaptor interface for Community operations. */
public interface ICommunityAdaptor {

  CommunityList createCommunities(List<String> names);

  CommunityList findCommunities(String name);

  /**
   * Load one community by numeric id, GUID string, or exact name. Includes role associations when
   * available. Returns {@code null} when not found.
   */
  Community getCommunity(String idOrName);

  /**
   * List all security roles available for community membership (id/name/guid). Used by the
   * Developer module role picker.
   */
  CommunityRoleList listAvailableRoles();

  /**
   * Assign and unassign roles for a community by replacing the full membership set (SE-02). Include
   * a role to assign it; omit a previously associated role to unassign it; empty list clears all.
   * Each entry needs {@code roleGuid} or {@code roleId}. Returns the reloaded community detail, or
   * {@code null} if the community was not found.
   */
  Community updateCommunityRoles(String idOrName, CommunityRoleList roles);

  /**
   * Rename one community. Blank or longer than {@code 50} characters is {@link
   * IllegalArgumentException} (HTTP 400). A case-insensitive name owned by a different community
   * is HTTP 409. Non-Admin or a missing session/user is HTTP 403. Returns {@code null} when the
   * community does not exist. The same name after trim does not write. Description and role
   * membership are unchanged. Design-lock conflicts are HTTP 409 and do not rename.
   */
  Community renameCommunity(String idOrName, String newName);

  CommunityList loadCommunities(GuidList ids, boolean lock, boolean overrideLock)
      throws PSErrorResultsException;

  void saveCommunities(CommunityList communities, boolean release);

  void deleteCommunities(GuidList ids, boolean ignoreDependencies);

  CommunityVisibilityList getVisibilityByCommunity(GuidList ids, ObjectTypeEnum type)
      throws PSErrorResultsException, RemoteException;

  void switchCommunity(String name);
}
