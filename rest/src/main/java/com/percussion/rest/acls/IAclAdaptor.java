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

// REFACTORED: CP-JAVA11
package com.percussion.rest.acls;

import com.percussion.rest.Guid;
import com.percussion.rest.GuidList;
import com.percussion.rest.ObjectTypeEnum;
import com.percussion.services.security.PSSecurityException;
import java.util.List;

/** Adaptor interface for ACL operations. */
public interface IAclAdaptor {

  /** Gets the user's access level for the specified object. */
  UserAccessLevel getUserAccessLevel(Guid objectGuid);

  /** Calculates the user's access level for the specified ACL GUID. */
  UserAccessLevel calculateUserAccessLevel(String aclGuid);

  /** Creates an ACL for the given object and owner. */
  Acl createAcl(Guid objGuid, TypedPrincipal owner);

  /** Loads ACLs for the given GUIDs. */
  AclList loadAcls(GuidList aclGuids) throws PSSecurityException;

  /** Loads a single ACL by GUID. */
  Acl loadAcl(Guid aclGuid) throws PSSecurityException;

  /** Loads ACLs for the given object GUIDs. */
  AclList loadAclsForObjects(GuidList objectGuids);

  /** Loads the ACL for a specific object. */
  Acl loadAclForObject(Guid objectGuid);

  /** Saves the provided ACL list. */
  void saveAcls(AclList aclList) throws PSSecurityException;

  /** Deletes the ACL for the given GUID. */
  void deleteAcl(Guid aclGuid) throws PSSecurityException;

  /** Filters ACLs by community names. */
  GuidList filterByCommunities(GuidList aclList, List<String> communityNames);

  /** Finds objects visible to the given communities and object type. */
  GuidList findObjectsVisibleToCommunities(List<String> communityNames, ObjectTypeEnum objectType);
}
