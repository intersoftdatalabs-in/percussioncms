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

  CommunityList loadCommunities(GuidList ids, boolean lock, boolean overrideLock)
      throws PSErrorResultsException;

  void saveCommunities(CommunityList communities, boolean release);

  void deleteCommunities(GuidList ids, boolean ignoreDependencies);

  CommunityVisibilityList getVisibilityByCommunity(GuidList ids, ObjectTypeEnum type)
      throws PSErrorResultsException, RemoteException;

  void switchCommunity(String name);
}
