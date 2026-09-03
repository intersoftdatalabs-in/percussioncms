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

package com.percussion.rest.roles;

import com.percussion.rest.errors.BackendException;
import java.net.URI;
import java.util.List;

/** Adaptor interface for Role operations. Sunny Sal: "Role ka adaptor, permissions ka factor!" */
public interface IRoleAdaptor {

  /** Gets a role by name. */
  Role getRole(URI baseUri, String roleName) throws BackendException;

  /** Updates a role. */
  Role updateRole(URI baseUri, Role role);

  /** Creates a role. */
  Role createRole(URI baseUri, Role role) throws BackendException;

  /** Deletes a role by name. */
  void deleteRole(URI baseUri, String roleName) throws BackendException;

  /** Finds roles by pattern. */
  List<Role> findRoles(URI baseUri, String pattern) throws BackendException;

  /**
   * Admin SE-03 browse catalog: roles with community / workflow / unassigned grouping metadata.
   *
   * @param baseUri request base URI (unused today; kept for adaptor parity)
   * @param groupFilter optional {@link RoleBrowseGroup} wire value; blank means all groups
   * @return catalog never null; empty roles when none match
   * @throws jakarta.ws.rs.WebApplicationException 403 when caller is not Admin; 400 when filter is
   *     invalid
   */
  RoleBrowseCatalog browseRoles(URI baseUri, String groupFilter);
}
