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

  /**
   * Updates an existing role's description only. Membership, home page, and name are left as
   * stored. Does not create a missing role and does not rename.
   *
   * @throws jakarta.ws.rs.WebApplicationException 400 when the name is blank or the description is
   *     longer than 255 characters, or when the role service rejects the update; 403 when the
   *     caller is not Admin; 404 when no role has that exact name
   */
  Role updateRole(URI baseUri, Role role);

  /**
   * Creates a role via the role service create path.
   *
   * @throws jakarta.ws.rs.WebApplicationException 400 when the name is blank or rejected by role
   *     validation (including a duplicate name); 403 when the caller is not Admin
   */
  Role createRole(URI baseUri, Role role) throws BackendException;

  /**
   * Whether a role with this exact name is already defined.
   *
   * @param roleName role name; blank is never defined
   * @return {@code true} when the security catalog has that exact name
   * @throws jakarta.ws.rs.WebApplicationException 500 when the catalog lookup fails
   */
  boolean roleExists(URI baseUri, String roleName);

  /**
   * Deletes one CMS role. A directory-backed group loses only the CMS link.
   *
   * @throws jakarta.ws.rs.WebApplicationException 400 when the name is blank or the role is a
   *     system role ({@code System} or {@code Default}); 403 when the caller is not Admin; 404
   *     when no role has that exact name; 409 when delete would strand users or a workflow still
   *     assigns the role
   */
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
