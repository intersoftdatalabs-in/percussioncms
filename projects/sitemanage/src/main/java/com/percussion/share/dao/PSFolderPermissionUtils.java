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
package com.percussion.share.dao;

import static com.percussion.role.service.IPSRoleService.ADMINISTRATOR_ROLE;
import static com.percussion.role.service.IPSRoleService.DESIGNER_ROLE;
import static org.apache.commons.lang3.Validate.isTrue;
import static org.apache.commons.lang3.Validate.notNull;

import com.percussion.cms.objectstore.PSFolder;
import com.percussion.cms.objectstore.PSObjectAcl;
import com.percussion.cms.objectstore.PSObjectAclEntry;
import com.percussion.pathmanagement.data.PSFolderPermission;
import com.percussion.pathmanagement.data.PSFolderPermission.Access;
import com.percussion.pathmanagement.data.PSFolderPermission.Principal;
import com.percussion.pathmanagement.data.PSFolderPermission.PrincipalType;
import com.percussion.utils.types.PSPair;
import java.util.*;
import org.apache.commons.lang3.Validate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.transaction.annotation.Transactional;

/**
 * A utility class used to retrieve and store {@link PSFolderPermission} from {@link PSFolder}
 *
 * @author yubingchen
 */
@Transactional
public class PSFolderPermissionUtils {
  /** The equivalent of ADMIN permission in legacy access term, ADMIN, WRITE &amp; READ access */
  public static int ADMIN_ACCESS =
      PSObjectAclEntry.ACCESS_ADMIN | PSObjectAclEntry.ACCESS_WRITE | PSObjectAclEntry.ACCESS_READ;

  /** The equivalent of WRITE permission in legacy access term, WRITE &amp; READ access */
  public static int WRITE_ACCESS = PSObjectAclEntry.ACCESS_WRITE | PSObjectAclEntry.ACCESS_READ;

  /** The equivalent of READ permission in legacy access term, READ access only */
  public static int READ_ACCESS = PSObjectAclEntry.ACCESS_READ;

  /** Constant for role name Editor */
  public static String EDITOR_ROLE_NAME = "Editor";

  /** The logger */
  public static final Logger log = LogManager.getLogger(PSFolderPermissionUtils.class);

  /**
   * Gets the permission for the specified folder.
   *
   * @param folder the folder in question, not <code>null</code>.
   * @return the permission of the folder, never <code>null</code>.
   */
  public static PSFolderPermission getFolderPermission(PSFolder folder) {
    notNull(folder);

    var permission = new PSFolderPermission();
    var acl = folder.getAcl();
    // Empty/null ACL → default ADMIN so Folder Security open never returns a
    // half-built DTO that later fails setFolderPermission on save (#2749).
    if (acl == null) {
      permission.setAccessLevel(Access.ADMIN);
      return permission;
    }
    setAclEntry(acl, PSObjectAclEntry.ACL_ENTRY_TYPE_ROLE, DESIGNER_ROLE, ADMIN_ACCESS);
    var aclEntry = getVirtualAcl(acl);

    if (aclEntry != null) {
      if (aclEntry.hasAdminAccess()) permission.setAccessLevel(Access.ADMIN);
      else if (aclEntry.hasWriteAccess()) permission.setAccessLevel(Access.WRITE);
      else if (aclEntry.hasReadAccess()) permission.setAccessLevel(Access.READ);
      else permission.setAccessLevel(Access.ADMIN);
    } else {
      // No virtual Everyone entry — treat as ADMIN (legacy default).
      permission.setAccessLevel(Access.ADMIN);
    }

    var adminPrincipals = new ArrayList<Principal>();
    var readPrincipals = new ArrayList<Principal>();
    var writePrincipals = new ArrayList<Principal>();

    var iter = acl.iterator();
    while (iter.hasNext()) {
      var entry = iter.next();
      if (entry == null || !entry.isUser()) {
        continue;
      }
      var p = new Principal();
      p.setName(entry.getName());
      p.setType(PrincipalType.USER);
      if (entry.hasAdminAccess()) {
        adminPrincipals.add(p);
      } else if (entry.hasWriteAccess()) {
        writePrincipals.add(p);
      } else if (entry.hasReadAccess()) {
        readPrincipals.add(p);
      }
    }

    if (!adminPrincipals.isEmpty()) {
      permission.setAdminPrincipals(adminPrincipals);
    }
    if (!writePrincipals.isEmpty()) {
      permission.setWritePrincipals(writePrincipals);
    }
    if (!readPrincipals.isEmpty()) {
      permission.setReadPrincipals(readPrincipals);
    }

    return permission;
  }

  /**
   * Gets the access level of the specified folder for the specified user and roles. It returns the
   * access level of the virtual ACL entry if the folder does not contain an ACL entry with the
   * specified user name. It defaults to {@link PSFolderPermission.Access#ADMIN} if the ACL is not
   * defined in the given folder.
   *
   * @param folder the folder in question, never <code>null</code>.
   * @param userName the user name, may be <code>null</code> or empty.
   * @param roles the roles that the user is a member of, not <code>null</code>, but may be empty.
   * @return the access level described above, never <code>null</code>.
   */
  public static PSPair<Access, Boolean> getUserAcl(
      PSFolder folder, String userName, Collection<String> roles) {
    notNull(folder);
    notNull(roles);

    var folderAcl = folder.getAcl();

    if (folderAcl == null) return new PSPair<>(Access.ADMIN, false);

    boolean didAdd = ensureAdminAccess(folder);

    if (roles.contains(ADMINISTRATOR_ROLE)) return new PSPair<>(Access.ADMIN, didAdd);

    var userAcl = getUserAcl(folderAcl, userName);
    for (var role : roles) {
      var roleAcl = getRoleAcl(folderAcl, role);
      userAcl = comparePermissions(roleAcl, userAcl);
    }

    if (userAcl != null) {
      return new PSPair<>(userAcl, didAdd);
    }

    var virtualAcl = getVirtualAcl(folderAcl);
    userAcl = (virtualAcl == null) ? Access.ADMIN : convertAcl(virtualAcl);

    return new PSPair<>(userAcl, didAdd);
  }

  /**
   * Compare the two permissions and return the one with greater access. <code>null</code> entries
   * are not considered.
   *
   * @param acl1 May be <code>null</code>.
   * @param acl2 May be <code>null</code>.
   * @return The greater permission, or <code>null</code> if both are <code>null</code>. If either
   *     are <code>null</code>, the other is returned.
   */
  private static Access comparePermissions(Access acl1, Access acl2) {
    if (acl1 == null) return acl2;
    if (acl2 == null) return acl1;
    if (acl1.equals(Access.ADMIN) || acl2.equals(Access.ADMIN)) return Access.ADMIN;
    if (acl1.equals(Access.WRITE) || acl2.equals(Access.WRITE)) return Access.ADMIN;
    return Access.READ;
  }

  /**
   * Make sure the ACL contains an "Admin" role entry with ADMIN access, add "Designer" role entry
   * w/ADMIN access if not there
   *
   * @param folder The folder to check, not <code>null</code>.
   */
  public static boolean ensureAdminAccess(PSFolder folder) {
    Validate.notNull(folder);

    boolean isAdd =
        setAclEntry(
            folder.getAcl(), PSObjectAclEntry.ACL_ENTRY_TYPE_ROLE, DESIGNER_ROLE, ADMIN_ACCESS);
    var adminEntry =
        getAclEntry(folder.getAcl(), PSObjectAclEntry.ACL_ENTRY_TYPE_ROLE, ADMINISTRATOR_ROLE);
    if (adminEntry == null) {
      log.debug(
          "Folder (id={}), name={}) does not have ACL entry with Admin role.",
          folder.getLocator().getId(),
          folder.getName());
    } else {
      notNull(adminEntry);
      isTrue(adminEntry.hasAdminAccess());
    }
    return isAdd;
  }

  /**
   * Sets the specified access level to the given folder. This will set the access level for the
   * given folder only and remove all other permission properties, such as user or role permissions
   * of the folder.
   *
   * <p>Note, The folder will always has an entry with ADMIN permission for the "Admin" and
   * "Designer" roles. However, the role ACL entry is not exposed in {@link PSFolderPermission} yet.
   *
   * @param folder the folder, never <code>null</code>.
   * @param acl the new access level of the folder, never <code>null</code>
   */
  public static void setFolderPermission(PSFolder folder, PSFolderPermission.Access acl) {
    notNull(folder);
    notNull(acl);

    setFolderAccessLevel(folder, acl);
    removeNonVirtualAclEntries(folder.getAcl());
    setAclEntry(
        folder.getAcl(), PSObjectAclEntry.ACL_ENTRY_TYPE_ROLE, ADMINISTRATOR_ROLE, ADMIN_ACCESS);
    setAclEntry(folder.getAcl(), PSObjectAclEntry.ACL_ENTRY_TYPE_ROLE, DESIGNER_ROLE, ADMIN_ACCESS);
  }

  /**
   * Sets the specified access level and user permissions to the given folder. This will set the
   * access level and user permissions for the given folder only and remove all other permission
   * properties of the folder.
   *
   * <p>Note, The folder will always has an entry with ADMIN permission for "Admin" role. However,
   * the role ACL entry is not exposed in {@link PSFolderPermission} yet.
   *
   * @param folder the folder, never <code>null</code>.
   * @param perm the new permissions of the folder, never <code>null</code>
   */
  public static void setFolderPermission(PSFolder folder, PSFolderPermission perm) {
    notNull(folder);
    notNull(perm);

    // Jackson / partial clients may omit accessLevel; default ADMIN rather than
    // Validate.notNull → "The validated object is null" (#2749).
    Access level = perm.getAccessLevel() != null ? perm.getAccessLevel() : Access.ADMIN;
    setFolderPermission(folder, level);

    var acl = folder.getAcl();
    if (acl == null) {
      // PSFolder.getAcl() is documented never-null; guard for defensive completeness.
      return;
    }

    setUserAcls(acl, perm.getReadPrincipals(), READ_ACCESS);
    setUserAcls(acl, perm.getWritePrincipals(), WRITE_ACCESS);
    setUserAcls(acl, perm.getAdminPrincipals(), ADMIN_ACCESS);
  }

  /**
   * Sets the access level of an ACL entry within the specified ACL. The ACL entry will be added
   * into the specified ACL if it does not exist; otherwise the ACL entry will be updated with the
   * specified permission.
   *
   * @param acl the ACL in question, which may or may not contain the specified ACL entry, assumed
   *     not <code>null</code>.
   * @param type the type of the ACL entry.
   * @param name the name of the ACL entry, assumed not blank.
   * @param permission the new access level of the ACL entry.
   * @return <code>true</code> if the entry was added, <code>false</code> if it was already there.
   */
  private static boolean setAclEntry(PSObjectAcl acl, int type, String name, int permission) {
    boolean isAdd = false;
    var aclEntry = getAclEntry(acl, type, name);
    if (aclEntry == null) {
      isAdd = true;
      aclEntry = new PSObjectAclEntry(type, name, permission);
    }
    aclEntry.setPermissions(permission);

    if (isAdd) {
      acl.add(aclEntry);
    }
    return isAdd;
  }

  /**
   * Sets the access level of an ACL entry within the specified ACL, where the type of the entry is
   * {@link PSObjectAclEntry#ACL_ENTRY_TYPE_USER} and the name of the entry is the specified user
   * name.
   *
   * @param acl the ACL in question, assumed not <code>null</code>.
   * @param userName the name of the user in question, it may be <code>null</code> or empty.
   * @param permission the new access level for the user.
   */
  private static void setUserAcl(PSObjectAcl acl, String userName, int permission) {
    setAclEntry(acl, PSObjectAclEntry.ACL_ENTRY_TYPE_USER, userName, permission);
  }

  /**
   * Sets the access level of ACL entries within the specified ACL, where the type of the entry is
   * {@link PSObjectAclEntry#ACL_ENTRY_TYPE_USER} and the name of the entry is specified in the
   * given principals.
   *
   * @param acl the ACL in question, assumed not <code>null</code>.
   * @param users the list of principals in question, it may be <code>null</code> or empty.
   * @param permission the new access level for the users.
   */
  private static void setUserAcls(PSObjectAcl acl, List<Principal> users, int permission) {
    if (users != null) {
      users.forEach(user -> setUserAcl(acl, user.getName(), permission));
    }
  }

  /**
   * Sets the specified access level to the given folder.
   *
   * @param folder the folder, assumed not <code>null</code>.
   * @param acl the new access level of the folder, assumed not <code>null</code>
   */
  private static void setFolderAccessLevel(PSFolder folder, PSFolderPermission.Access acl) {
    int legacyAcl = ADMIN_ACCESS;
    int permValue = acl.ordinal();
    if (permValue == Access.READ.ordinal()) legacyAcl = READ_ACCESS;
    else if (permValue == Access.WRITE.ordinal()) legacyAcl = WRITE_ACCESS;

    setAclEntry(
        folder.getAcl(),
        PSObjectAclEntry.ACL_ENTRY_TYPE_VIRTUAL,
        PSObjectAclEntry.ACL_ENTRY_EVERYONE,
        legacyAcl);
  }

  /**
   * Gets the virtual ACL entry contained in the specified ACL.
   *
   * @param acl the ACL that may contain a virtual entry, it may be <code>null</code>.
   * @return the virtual ACL entry. It may be <code>null</code> if the virtual ACL entry does not
   *     exist.
   */
  private static PSObjectAclEntry getVirtualAcl(PSObjectAcl acl) {
    return getAclEntry(acl, PSObjectAclEntry.ACL_ENTRY_TYPE_VIRTUAL, null);
  }

  /**
   * Converts the (legacy) ACL entry to the folder access level.
   *
   * @param entry the (legacy) ACL entry, assumed not <code>null</code>.
   * @return the converted access level, defaults to {@link PSFolderPermission.Access#ADMIN} if the
   *     folder has no "admin", "read" or "write" access.
   */
  private static PSFolderPermission.Access convertAcl(PSObjectAclEntry entry) {
    if (entry.hasAdminAccess()) return Access.ADMIN;
    else if (entry.hasWriteAccess()) return Access.WRITE;
    else if (entry.hasReadAccess()) return Access.READ;
    else return Access.ADMIN;
  }

  /**
   * Gets the access level of an ACL entry within the specified ACL, where the type of the entry is
   * {@link PSObjectAclEntry#ACL_ENTRY_TYPE_USER} and the name of the entry is the specified user
   * name.
   *
   * @param acl the ACL in question, assumed not <code>null</code>.
   * @param userName the name of the user in question, it may be <code>null</code> or empty.
   * @return the access level described above. It may be <code>null</code> if such ACL entry does
   *     not exist in the given ACL.
   */
  private static PSFolderPermission.Access getUserAcl(PSObjectAcl acl, String userName) {
    if (acl == null) {
      return Access.ADMIN;
    }
    var aclEntry = getUserAclEntry(acl, userName);
    return (aclEntry != null) ? convertAcl(aclEntry) : null;
  }

  /**
   * Gets the access level of an ACL entry within the specified ACL, where the type of the entry is
   * {@link PSObjectAclEntry#ACL_ENTRY_TYPE_ROLE} and the name of the entry is the specified role
   * name.
   *
   * @param acl the ACL in question, assumed not <code>null</code>.
   * @param roleName the name of the role in question, it may be <code>null</code> or empty.
   * @return the access level described above. It may be <code>null</code> if such ACL entry does
   *     not exist in the given ACL.
   */
  private static PSFolderPermission.Access getRoleAcl(PSObjectAcl acl, String roleName) {
    if (acl == null) {
      return Access.ADMIN;
    }
    var aclEntry = getAclEntry(acl, PSObjectAclEntry.ACL_ENTRY_TYPE_ROLE, roleName);
    return (aclEntry != null) ? convertAcl(aclEntry) : null;
  }

  /**
   * Gets the ACL entry within the specified ACL, where the type of the entry is {@link
   * PSObjectAclEntry#ACL_ENTRY_TYPE_USER} and the name of the entry is the specified user name.
   *
   * @param acl the ACL in question, assumed not <code>null</code>.
   * @param userName the name of the user in question, it may be <code>null</code> or empty.
   * @return the ACL entry described above. It may be <code>null</code> if such ACL entry does not
   *     exist in the given ACL.
   */
  private static PSObjectAclEntry getUserAclEntry(PSObjectAcl acl, String userName) {
    return getAclEntry(acl, PSObjectAclEntry.ACL_ENTRY_TYPE_USER, userName);
  }

  /**
   * Gets the specified ACL entry within the specified ACL.
   *
   * @param acl the ACL in question, it may be <code>null</code>.
   * @param type the type of the ACL entry in question, assumed it is one of the valid types.
   * @param name the name of the ACL entry in question, it may be <code>null</code> if it is
   *     ignored.
   * @return the ACL entry described above. It may be <code>null</code> if such ACL entry does not
   *     exist in the given ACL.
   */
  private static PSObjectAclEntry getAclEntry(PSObjectAcl acl, int type, String name) {
    if (acl == null) return null;
    var iter = acl.iterator();
    while (iter.hasNext()) {
      var entry = iter.next();
      if (entry.getType() == type && (name == null || entry.getName().equalsIgnoreCase(name))) {
        return entry;
      }
    }
    return null;
  }

  /**
   * Removes all non-virtual ACL entries from the specified ACL.
   *
   * @param acl the ACL in question, it may be <code>null</code>.
   */
  private static void removeNonVirtualAclEntries(PSObjectAcl acl) {
    if (acl == null) return;
    var entries = new ArrayList<PSObjectAclEntry>();
    var iter = acl.iterator();
    while (iter.hasNext()) {
      var entry = iter.next();
      if (!entry.isVirtual()) entries.add(entry);
    }
    entries.forEach(acl::remove);
  }
}
