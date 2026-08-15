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

package com.percussion.apibridge;

import com.google.gdata.data.DateTime;
import com.percussion.rest.Guid;
import com.percussion.rest.GuidList;
import com.percussion.rest.ObjectLockSummary;
import com.percussion.rest.ObjectSummary;
import com.percussion.rest.ObjectSummaryList;
import com.percussion.rest.ObjectTypeEnum;
import com.percussion.rest.PermissionList;
import com.percussion.rest.Permissions;
import com.percussion.rest.acls.Acl;
import com.percussion.rest.acls.AclEntry;
import com.percussion.rest.acls.AclEntryList;
import com.percussion.rest.acls.AclList;
import com.percussion.rest.acls.TypedPrincipal;
import com.percussion.rest.acls.UserAccessLevel;
import com.percussion.rest.acls.UserAccessLevelList;
import com.percussion.rest.actions.ActionMenu;
import com.percussion.rest.actions.ActionMenuList;
import com.percussion.rest.actions.ActionMenuParameter;
import com.percussion.rest.actions.ActionMenuProperty;
import com.percussion.rest.actions.ActionMenuVisibilityContext;
import com.percussion.rest.actions.UIContext;
import com.percussion.rest.communities.Community;
import com.percussion.rest.communities.CommunityList;
import com.percussion.rest.communities.CommunityRole;
import com.percussion.rest.communities.CommunityRoleList;
import com.percussion.rest.communities.CommunityVisibility;
import com.percussion.rest.communities.CommunityVisibilityList;
import com.percussion.rest.contenttypes.ContentType;
import com.percussion.rest.locationscheme.LocationScheme;
import com.percussion.rest.locationscheme.LocationSchemeParameter;
import com.percussion.rest.locationscheme.LocationSchemeParameterList;
import com.percussion.rest.preferences.UserPreference;
import com.percussion.rest.preferences.UserPreferenceList;
import com.percussion.rest.roles.Role;
import com.percussion.rest.sites.Site;
import com.percussion.rest.sites.SiteList;
import com.percussion.rest.templates.TemplateSummary;
import com.percussion.role.data.PSRole;
import com.percussion.security.IPSTypedPrincipal;
import com.percussion.server.PSPersistentProperty;
import com.percussion.server.PSPersistentPropertyManager;
import com.percussion.server.PSPersistentPropertyMeta;
import com.percussion.services.assembly.IPSAssemblyTemplate;
import com.percussion.services.catalog.IPSCatalogSummary;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.catalog.data.PSObjectSummary;
import com.percussion.services.guidmgr.PSGuidManagerLocator;
import com.percussion.services.guidmgr.data.PSGuid;
import com.percussion.services.locking.data.PSObjectLockSummary;
import com.percussion.services.menus.PSActionMenu;
import com.percussion.services.menus.PSActionMenuParam;
import com.percussion.services.menus.PSActionMenuProperty;
import com.percussion.services.menus.PSActionMenuVisibility;
import com.percussion.services.menus.PSUiContext;
import com.percussion.services.security.IPSAcl;
import com.percussion.services.security.IPSAclEntry;
import com.percussion.services.security.PSPermissions;
import com.percussion.services.security.PSTypedPrincipal;
import com.percussion.services.security.data.PSAccessLevelImpl;
import com.percussion.services.security.data.PSAclEntryImpl;
import com.percussion.services.security.data.PSAclImpl;
import com.percussion.services.security.data.PSCommunity;
import com.percussion.services.security.data.PSCommunityRoleAssociation;
import com.percussion.services.security.data.PSCommunityVisibility;
import com.percussion.services.security.data.PSUserAccessLevel;
import com.percussion.services.sitemgr.IPSLocationScheme;
import com.percussion.sitemanage.data.PSSiteSummary;
import com.percussion.utils.guid.IPSGuid;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

public class ApiUtils {

  /***
   * Converts a system IPSGuid to a rest compatible Guid.
   *
   * @param guid the system GUID, may be {@code null}
   * @return the rest compatible Guid, never {@code null}
   */
  public static Guid convertGuid(IPSGuid guid) {
    var ret = new Guid();
    if (guid != null) {
      ret.setHostId(guid.getHostId());
      ret.setLongValue(guid.longValue());
      ret.setType(guid.getType());
      ret.setUntypedString(guid.toStringUntyped());
      ret.setUuid(guid.getUUID());
      ret.setStringValue(guid.toString());
    }
    return ret;
  }

  /**
   * Converts a rest compatible Guid to a system IPSGuid.
   *
   * @param guid the rest compatible Guid
   * @return the corresponding system IPSGuid
   */
  public static IPSGuid convertGuid(Guid guid) {
    if (guid == null) {
      return null;
    }
    String raw = guid.getStringValue().orElse(null);
    if (raw != null && !raw.isBlank()) {
      return PSGuidManagerLocator.getGuidMgr().makeGuid(raw);
    }
    // SPA GET often serializes hostId/type/uuid without stringValue. makeGuid(null)
    // throws "raw may not be blank" (#3391 persist after AclList bind).
    if (guid.getType() != 0 && guid.getUuid() != 0) {
      PSTypeEnum type = PSTypeEnum.valueOf(guid.getType());
      if (type != null) {
        return new PSGuid(guid.getHostId(), type, guid.getUuid());
      }
    }
    return null;
  }

  /**
   * Helper that returns the contained value or null.
   *
   * @param opt the optional to unwrap, may be {@code null}
   * @param <T> the contained type
   * @return the contained value or {@code null}
   */
  public static <T> T orNull(Optional<T> opt) {
    return opt == null ? null : opt.orElse(null);
  }

  /**
   * Return an empty collection if the Optional is empty or null.
   *
   * @param opt the optional collection, may be {@code null}
   * @param <T> the element type
   * @return the contained collection or an empty one
   */
  public static <T> Collection<T> orEmpty(Optional<? extends Collection<T>> opt) {
    if (opt == null || opt.isEmpty()) {
      return List.of();
    }
    return opt.get();
  }

  /**
   * Generic unwrapping helper. If the supplied object is an {@link Optional} it will return the
   * contained value or null; otherwise the value is returned unchanged. This allows callers to
   * uniformly access getters which may or may not return Optionals without needing to know the
   * return type.
   *
   * @param <T> the type parameter of the contained value
   * @param o the value to unwrap
   * @return the contained value, or the input unchanged when it is not an Optional
   */
  @SuppressWarnings("unchecked")
  public static <T> T unwrap(Object o) {
    if (o instanceof Optional) {
      return ((Optional<T>) o).orElse(null);
    }
    return (T) o;
  }

  /**
   * Converts a PSCommunity to a Community for return to the REST API.
   *
   * @param c A valid community
   * @return The converted community
   */
  public static Community convertPSCommunity(PSCommunity c) {
    Community ret =
        new Community(
            c.getId(), convertGuid(c.getGUID()), c.getName(), c.getDescription(), c.getLabel());
    // unwrap Optional GUID for convenience
    var optGuid = ret.getGuid().orElse(null);

    ArrayList<CommunityRole> roles = new ArrayList<>();
    // iterate role associations from PSCommunity
    for (IPSGuid roleGuid : c.getRoleAssociations()) {
      CommunityRole assoc = new CommunityRole();
      assoc.setCommunityGuid(optGuid);
      assoc.setCommunityId(optGuid != null ? optGuid.getLongValue() : 0L);
      assoc.setRoleId(roleGuid.longValue());
      assoc.setRoleGuid(convertGuid(roleGuid));
      roles.add(assoc);
    }
    ret.setRoleList(new CommunityRoleList(roles));

    return ret;
  }

  /**
   * Takes a list of Guids and returns a list of IPSGuids.
   *
   * @param ids the rest compatible Guid list, never {@code null}
   * @return the converted list of system IPSGuids, never {@code null}
   */
  public static List<IPSGuid> convertGuids(GuidList ids) {
    var ret = new ArrayList<IPSGuid>();
    for (var g : ids) {
      var ps_g = new PSGuid();
      ps_g.setHostId(g.getHostId());
      ps_g.setType(g.getType());
      ps_g.setUUID(g.getUuid());
      ret.add(ps_g);
    }
    return ret;
  }

  /**
   * Takes a list of PSCommunity instances and returns a CommunityList.
   *
   * @param ps_communities the list of PSCommunity instances, never {@code null}
   * @return the corresponding CommunityList, never {@code null}
   */
  public static CommunityList convertPSCommunities(List<PSCommunity> ps_communities) {

    ArrayList<Community> communities = new ArrayList<>();
    for (PSCommunity p : ps_communities) {
      communities.add(convertPSCommunity(p));
    }
    return new CommunityList(communities);
  }

  /**
   * Takes a CommunityList and returns a list of PSCommunity objects.
   *
   * @param communities the CommunityList, never {@code null}
   * @return the corresponding list of PSCommunity objects, never {@code null}
   */
  public static List<PSCommunity> convertCommunityList(CommunityList communities) {

    ArrayList<PSCommunity> ret = new ArrayList<>();
    for (Community c : communities) {
      ret.add(convertCommunity(c));
    }

    return ret;
  }

  /**
   * Takes a Community and returns a PSCommunity.
   *
   * @param c the rest compatible Community, never {@code null}
   * @return the corresponding PSCommunity, never {@code null}
   */
  public static PSCommunity convertCommunity(Community c) {
    PSCommunity p = new PSCommunity();

    p.setDescription(orNull(c.getDescription()));
    p.setName(orNull(c.getName()));
    p = (PSCommunity) p.tuneClone(c.getId());

    for (CommunityRole cr : orEmpty(c.getRoleList())) {
      p.addRoleAssociation(convertGuid(orNull(cr.getRoleGuid())));
    }

    return p;
  }

  /**
   * Takes a community role list and returns a List of PSCommunityRoleAssociation objects.
   *
   * @param roleList the rest compatible CommunityRoleList, may be {@code null}
   * @return the corresponding list of PSCommunityRoleAssociation objects, never {@code null}
   */
  public static Collection<PSCommunityRoleAssociation> convertCommunityRoleList(
      CommunityRoleList roleList) {

    ArrayList<PSCommunityRoleAssociation> ret = new ArrayList<>();
    if (roleList != null) {
      for (CommunityRole r : roleList) {
        PSCommunityRoleAssociation p_r =
            new PSCommunityRoleAssociation(
                convertGuid(orNull(r.getCommunityGuid())), convertGuid(orNull(r.getRoleGuid())));
        p_r.setRoleName(orNull(r.getRoleName()));
        ret.add(p_r);
      }
    }
    return ret;
  }

  /**
   * Converts an ObjectTypeEnum to a PSTypeEnum.
   *
   * @param type the rest compatible ObjectTypeEnum, never {@code null}
   * @return the corresponding PSTypeEnum
   */
  public static PSTypeEnum convertObjectTypeEnum(ObjectTypeEnum type) {
    return PSTypeEnum.valueOf(type.name());
  }

  /**
   * Takes a list of PSCommunityVisibilities and returns a CommunityVisibilityList.
   *
   * @param ps_visibilities the list of PSCommunityVisibility objects, never {@code null}
   * @return the corresponding CommunityVisibilityList, never {@code null}
   */
  public static Collection<? extends CommunityVisibility> convertPSCommunityVisibilities(
      List<PSCommunityVisibility> ps_visibilities) {
    var visibilities = new ArrayList<CommunityVisibility>();
    for (var pv : ps_visibilities) {
      visibilities.add(convertPSCommunityVisibility(pv));
    }
    return new CommunityVisibilityList(visibilities);
  }

  /**
   * Converts a single PSCommunityVisibility to its REST representation.
   *
   * @param pv the PSCommunityVisibility, never {@code null}
   * @return the corresponding CommunityVisibility, never {@code null}
   */
  public static CommunityVisibility convertPSCommunityVisibility(PSCommunityVisibility pv) {

    CommunityVisibility ret =
        new CommunityVisibility(pv.getGUID().longValue(), convertGuid(pv.getGUID()));

    ArrayList<ObjectSummary> visObjects = new ArrayList<>();
    for (PSObjectSummary s : pv.getVisibleObjects()) {
      visObjects.add(convertPSObjectSummary(s));
    }

    ret.setVisibleObjects(new ObjectSummaryList(visObjects));
    return ret;
  }

  public static ObjectSummary convertPSObjectSummary(PSObjectSummary s) {
    var ret = new ObjectSummary();
    ret.setDescription(s.getDescription());
    if (s.getGUID() != null) {
      ret.setGuid(convertGuid(s.getGUID()));
      ret.setId(s.getGUID().longValue());
    }
    ret.setLabel(s.getLabel());
    ret.setName(s.getName());
    ret.setType(s.getType() == null ? null : ObjectTypeEnum.valueOf(s.getType()));
    ret.setObjectLocked(s.isLocked());
    ret.setLockSummary(convertPSObjectLockSummary(s.getLocked()));
    ret.setPermissions(convertPSUserAccessLevel(s.getPermissions()));
    return ret;
  }

  /***
   * Takes a PSUserAccessLevel and returns a UserAccessLevel
   * @param permissions
   *
   */
  public static UserAccessLevel convertPSUserAccessLevel(PSUserAccessLevel permissions) {
    UserAccessLevel ret = new UserAccessLevel();

    HashSet<Permissions> perms = new HashSet<>();
    for (PSPermissions p : permissions.getPermissions()) {
      perms.add(convertPSPermissions(p));
    }
    ret.setPermissions(new PermissionList(perms));
    return ret;
  }

  /***
   * Takes a PSPermissions and returns a Permissions
   * @param p
   *
   */
  public static Permissions convertPSPermissions(PSPermissions p) {

    return Permissions.valueOf(p.name());
  }

  /***
   * Takes a PSObjectLockSummary and returns an ObjectLockSummary
   * @param locked
   *
   */
  public static ObjectLockSummary convertPSObjectLockSummary(PSObjectLockSummary locked) {

    ObjectLockSummary sum = new ObjectLockSummary();
    if (locked != null) {
      sum.setCallerAccessTime(DateTime.now().toString());
      sum.setLocker(locked.getLocker());
      sum.setRemainingTime(locked.getRemainingTime());
      sum.setSession(locked.getSession());
    }
    return sum;
  }

  public static LocationScheme copyLocationScheme(IPSLocationScheme scheme) {
    LocationScheme ret = new LocationScheme();

    ret.setDescription(scheme.getDescription());
    ret.setName(scheme.getName());
    ret.setSchemeId(ApiUtils.convertGuid(scheme.getGUID()));
    ret.setContentTypeId(scheme.getContentTypeId());
    ret.setTemplateId(scheme.getTemplateId());
    ret.setContext(convertGuid(scheme.getGUID()));
    ret.setLocationSchemeGenerator(scheme.getGenerator());
    ret.setParameters(convertLocationSchemeParameters(scheme));

    return ret;
  }

  /****
   * Takes a location scheme and returns a parameter list
   * @param scheme
   *
   */
  public static LocationSchemeParameterList convertLocationSchemeParameters(
      IPSLocationScheme scheme) {

    LocationSchemeParameterList ret = null;
    if (scheme != null) {

      List<String> p_params = scheme.getParameterNames();
      ArrayList<LocationSchemeParameter> params = new ArrayList<>();
      for (String s : p_params) {
        LocationSchemeParameter p = new LocationSchemeParameter();
        p.setName(s);
        p.setSequence(scheme.getParameterSequence(s));
        p.setType(scheme.getParameterType(s));
        p.setValue(scheme.getParameterValue(s));
        params.add(p);
      }
      ret = new LocationSchemeParameterList(params);
    }
    return ret;
  }

  /***
   * Takes a PSRole and returns a Role.
   * @param p_role
   *
   */
  public static Role convertRole(PSRole p_role) {

    Role ret = null;

    if (p_role != null) {
      ret = new Role();
      ret.setDescription(p_role.getDescription());
      ret.setHomePage(p_role.getHomepage());
      ret.setName(p_role.getName());
      if (p_role.getUsers() != null) {
        ret.setUsers(p_role.getUsers());
      }
    }

    return ret;
  }

  /***
   * Takes a Role and Converts it to a PSRole
   * @param role
   *
   */
  public static PSRole convertRole(Role role) {
    PSRole ret = new PSRole();

    ret.setDescription(role.getDescription());
    ret.setHomepage(role.getHomePage());
    ret.setName(role.getName());
    ret.setUsers(role.getUsers());

    return ret;
  }

  /***
   * Given an ACL returns an IPSAcl
   * @param acl
   *
   */
  public static PSAclImpl convertAcl(Acl acl) {

    PSAclImpl p_acl = new PSAclImpl();

    p_acl.setId(acl.getId());
    p_acl.setDescription(orNull(acl.getDescription()));
    String name = orNull(acl.getName());
    if (name == null || name.isBlank()) {
      name = "ACL";
    }
    p_acl.setName(name);
    var convertedGuid = convertGuid(orNull(acl.getGuid()));
    if (convertedGuid != null) {
      p_acl.setGUID(convertedGuid);
    }
    applyAclObjectIdentity(acl, p_acl);
    p_acl.setEntries(convertAclEntries(orNull(acl.getAclEntries())));

    return p_acl;
  }

  /**
   * Copy objectId / objectType onto {@link PSAclImpl}. When the REST body omits those primitives
   * (GET historically skipped {@code objectType}), derive them from {@code objectGuid} so {@link
   * PSAclImpl#getObjectGuid()} still resolves after save (#3378).
   */
  static void applyAclObjectIdentity(Acl acl, PSAclImpl p_acl) {
    int objectType = acl.getObjectType();
    long objectId = acl.getObjectId();
    Guid objectGuid = orNull(acl.getObjectGuid());
    if (objectGuid != null) {
      String stringValue = orNull(objectGuid.getStringValue());
      if (stringValue != null
          && !stringValue.isBlank()
          && (objectGuid.getType() == 0 || objectGuid.getUuid() == 0)) {
        try {
          objectGuid = new Guid(stringValue);
        } catch (RuntimeException ignored) {
          // keep the original Guid if stringValue is not a PSGuid
        }
      }
      if (objectType <= 0 && objectGuid.getType() != 0) {
        objectType = objectGuid.getType();
      }
      if (objectId <= 0 && objectGuid.getUuid() != 0) {
        objectId = Integer.toUnsignedLong(objectGuid.getUuid());
      }
    }
    p_acl.setObjectType(objectType);
    p_acl.setObjectId(objectId);
  }

  /***
   * Takes an AclEntry List and returns a collection of PSAclEntryImpls
   * @param aclEntries
   *
   */
  public static Collection<PSAclEntryImpl> convertAclEntries(AclEntryList aclEntries) {
    HashSet<PSAclEntryImpl> ret = new HashSet<>();
    if (aclEntries == null) {
      return ret;
    }

    for (AclEntry entry : aclEntries) {
      PSAclEntryImpl p_entry = new PSAclEntryImpl();

      p_entry.setId(entry.getId());
      p_entry.setName(orNull(entry.getName()));
      p_entry.setAclId(entry.getAclId());

      var principal = orNull(entry.getPrincipal());
      if (principal != null && principal.getName() != null) {
        p_entry.setPrincipal(convertPrincipal(principal));
      } else if (p_entry.getName() == null
          && entry.getType().isPresent()
          && entry.getType().get().getName() != null) {
        // Load path historically stored principal name on TypedPrincipal.name
        p_entry.setName(entry.getType().get().getName());
      }

      if (entry.getType().isPresent()) {
        TypedPrincipal tp = entry.getType().get();
        if (tp.getType() != null) {
          p_entry.setType(tp.getType());
        } else if (tp.getName() != null) {
          // Legacy clients sometimes put PrincipalTypes on type.name
          try {
            p_entry.setType(IPSTypedPrincipal.PrincipalTypes.valueOf(tp.getName()));
          } catch (IllegalArgumentException ignored) {
            // type.name is a principal name, not a PrincipalTypes enum constant
          }
        }
      }

      for (UserAccessLevel p : orEmpty(entry.getPermissions())) {
        if (p.getPermission().isPresent()) {
          p_entry.addPermission(convertPermissions(p));
        }
      }

      ret.add(p_entry);
    }

    return ret;
  }

  public static PSAccessLevelImpl convertPermissions(UserAccessLevel p) {
    PSAccessLevelImpl p_a = new PSAccessLevelImpl();

    Permissions perm = orNull(p.getPermission());
    if (perm != null) {
      p_a.setPermission(PSPermissions.valueOf(perm.name()));
    }
    p_a.setId(p.getId());
    return p_a;
  }

  /***
   * Given a rest Principal returns an rx Principal
   * @param principal
   *
   */
  public static Principal convertPrincipal(com.percussion.rest.acls.Principal principal) {

    return new Principal() {
      @Override
      public String getName() {
        return principal.getName();
      }
    };
  }

  public static AclList convertAcls(List<IPSAcl> loadAcls) {
    var acls = new ArrayList<Acl>();
    for (var p_acl : loadAcls) {
      acls.add(convertAcl((PSAclImpl) p_acl));
    }
    return new AclList(acls);
  }

  public static Acl convertAcl(PSAclImpl p_acl) {
    Acl ret = null;
    if (p_acl != null) {
      ret = new Acl();
      ret.setName(p_acl.getName());
      ret.setGuid(convertGuid(p_acl.getGUID()));
      ret.setId(p_acl.getId());
      ret.setObjectGuid(convertGuid(p_acl.getObjectGuid()));
      ret.setObjectId(p_acl.getObjectId());
      ret.setObjectType(p_acl.getObjectType());
      ret.setAclEntries(convertAclEntries(p_acl.getEntries()));
    }
    return ret;
  }

  /***
   * Takes a lost of IPSAclEntry and returns a list of ACLEntries
   * @param p_entries
   *
   */
  public static AclEntryList convertAclEntries(Collection<IPSAclEntry> p_entries) {
    var entries = new ArrayList<AclEntry>();
    for (var p_e : p_entries) {
      entries.add(convertAclEntry((PSAclEntryImpl) p_e));
    }
    return new AclEntryList(entries);
  }

  public static AclEntry convertAclEntry(PSAclEntryImpl p_e) {

    AclEntry ret = new AclEntry();

    ret.setName(p_e.getName());
    ret.setAclId(p_e.getAclId());
    ret.setId(p_e.getId());
    ret.setPermissions(convertPermissions(p_e.getPermissions()));

    ret.setType(convertPrincipalType(p_e.getTypedPrincipal()));
    if (p_e.getName() != null) {
      ret.setPrincipal(new com.percussion.rest.acls.Principal(p_e.getName()));
    }

    return ret;
  }

  public static UserAccessLevelList convertPermissions(Collection<PSAccessLevelImpl> permissions) {

    ArrayList<UserAccessLevel> access = new ArrayList<>();
    for (PSAccessLevelImpl p_a : permissions) {
      UserAccessLevel u = new UserAccessLevel();
      u.setId(p_a.getId());
      u.setPermission(convertPSPermissions(p_a.getPermission()));
      access.add(u);
    }
    return new UserAccessLevelList(access);
  }

  public static TypedPrincipal convertPrincipalType(IPSTypedPrincipal typedPrincipal) {
    TypedPrincipal ret = new TypedPrincipal();
    if (typedPrincipal == null) {
      return ret;
    }
    ret.setName(typedPrincipal.getName());
    ret.setType(typedPrincipal.getPrincipalType());
    return ret;
  }

  public static List<IPSAcl> convertAcls(AclList aclList) {
    var p_acls = new ArrayList<IPSAcl>();
    if (aclList == null) {
      return p_acls;
    }
    for (var a : aclList) {
      if (a != null) {
        p_acls.add(convertAcl(a));
      }
    }
    return p_acls;
  }

  public static GuidList convertGuids(Collection<IPSGuid> p_guids) {
    var guids = new ArrayList<Guid>();
    for (var p_g : p_guids) {
      var g = new Guid();
      g.setUntypedString(p_g.toStringUntyped());
      g.setUuid(p_g.getUUID());
      g.setType(p_g.getType());
      g.setStringValue(p_g.toString());
      g.setLongValue(p_g.longValue());
      g.setHostId(p_g.getHostId());
      guids.add(g);
    }
    return new GuidList(guids);
  }

  public static UserPreference convertPSPersistentProperty(PSPersistentProperty prop) {

    UserPreference up = new UserPreference();

    up.setCategory(prop.getCategory());
    up.setContext(prop.getContext());
    up.setExtraParam(prop.getExtraParam());
    up.setName(prop.getName());
    up.setUserName(prop.getUserName());
    up.setValue(prop.getValue());

    return up;
  }

  public static PSPersistentProperty convertUserPreference(UserPreference u) {
    PSPersistentProperty p =
        new PSPersistentProperty(
            u.getUserName(), u.getName(), u.getCategory(), u.getContext(), u.getValue());

    return p;
  }

  public static UserPreferenceList convertUserProperties(
      Collection<PSPersistentProperty> userProperties) {
    ArrayList<UserPreference> up = new ArrayList<>();
    for (PSPersistentProperty prop : userProperties) {
      up.add(ApiUtils.convertPSPersistentProperty(prop));
    }
    return new UserPreferenceList(up);
  }

  public static Collection<PSPersistentProperty> convertUserPreferences(UserPreferenceList prefs) {
    ArrayList<PSPersistentProperty> ret = new ArrayList<>();
    for (UserPreference up : prefs) {
      ret.add(ApiUtils.convertUserPreference(up));
    }
    return ret;
  }

  /**
   * Convert a persisted property to a REST {@link UserPreference}.
   *
   * <p>Must include {@code value} — {@link PreferencesAdaptor#loadPreference} uses this path for
   * {@code GET /preferences/{name}}. Dropping value caused Developer default ACL template
   * {@code RUNTIME_VISIBLE} (and any other stored payload) to be lost on reload (#2948).
   *
   * <p>Delegates to {@link #convertPSPersistentProperty} so list/get/save converters stay aligned.
   */
  public static UserPreference convertUserProperty(PSPersistentProperty p) {
    return convertPSPersistentProperty(p);
  }

  public static IPSTypedPrincipal convertPrincipalType(TypedPrincipal owner) {
    PSTypedPrincipal ret =
        new PSTypedPrincipal(
            owner.getName(), IPSTypedPrincipal.PrincipalTypes.valueOf(owner.getType().name()));
    return ret;
  }

  public static PSPersistentPropertyMeta convertUserPreferenceToMeta(UserPreference pref) {

    return new PSPersistentPropertyMeta(
        PSPersistentPropertyManager.SYS_USER,
        pref.getName(),
        pref.getCategory(),
        1,
        true,
        true,
        null);
  }

  public static List<ActionMenu> convertPSActionMenuList(List<PSActionMenu> actionMenus) {
    var ret = new ArrayList<ActionMenu>();
    if (actionMenus == null) {
      return ret;
    }
    for (var pa : actionMenus) {
      if (pa != null) {
        ret.add(convertPSActionMenu(pa));
      }
    }
    return ret;
  }

  public static ActionMenu convertPSActionMenu(PSActionMenu pa) {
    ActionMenu ret = new ActionMenu();

    ret.setId(pa.getActionId());
    // Persisted menus have a native action id; Object ACL binds
    // {@code 0-107-{actionId}} ({@link PSTypeEnum#ACTION}) (#3380).
    if (pa.getActionId() > 0) {
      ret.setGuid(convertGuid(new PSGuid(0, PSTypeEnum.ACTION, pa.getActionId())));
    }
    ret.setName(pa.getName());
    ret.setDescription(pa.getDescription());
    ret.setLabel(pa.getDisplayName());
    ret.setUrl(pa.getUrl());
    ret.setSortRank(pa.getSortOrder());
    ret.setMenuType(pa.getType());
    ret.setHandler(pa.getHandler());

    // Preserve cascading children for nested Explorer toolbar dropdowns (#2730).
    // Without this, structured menus (content-type submenus / RXMENUACTIONRELATION
    // trees) lose hierarchy and ActionToolbar dumps every leaf as a flat button.
    // Stamp parentId on each child so the SPA can reconstruct if a serializer
    // flattens the tree (#3379).
    List<PSActionMenu> childMenus = pa.getChildren();
    if (childMenus != null && !childMenus.isEmpty()) {
      ActionMenuList converted = new ActionMenuList(convertPSActionMenuList(childMenus));
      for (ActionMenu child : converted) {
        if (child != null && child.getParentId() == 0) {
          child.setParentId(ret.getId());
        }
      }
      ret.setChildren(converted);
    }

    ArrayList<ActionMenuProperty> props = new ArrayList<>();
    if (pa.getProperties() != null) {
      for (PSActionMenuProperty pap : pa.getProperties()) {
        ActionMenuProperty p = new ActionMenuProperty();
        p.setActionId(pap.getPrimaryKey().getActionId());
        p.setName(pap.getPrimaryKey().getPropertyName());
        p.setDescription(pap.getDescription());
        p.setValue(pap.getValue());
        props.add(p);
      }
    }
    ActionMenuProperty[] prop_array = new ActionMenuProperty[props.size()];
    ret.setProperties(props.toArray(prop_array));

    ArrayList<ActionMenuParameter> params = new ArrayList<>();
    if (pa.getParameters() != null) {
      for (PSActionMenuParam psparam : pa.getParameters()) {
        ActionMenuParameter p = new ActionMenuParameter();
        p.setDescription(psparam.getDescription());
        p.setName(psparam.getActionParamPK().getParamName());
        p.setValue(psparam.getParamValue());
        params.add(p);
      }
    }
    ActionMenuParameter[] param_array = new ActionMenuParameter[params.size()];
    ret.setParameters(params.toArray(param_array));

    ArrayList<ActionMenuVisibilityContext> vis = new ArrayList<>();
    if (pa.getVisibility() != null) {
      for (PSActionMenuVisibility v : pa.getVisibility()) {

        ActionMenuVisibilityContext vc = new ActionMenuVisibilityContext();

        vc.setDescription(v.getPrimaryKey().getDescription());
        vc.setValue(v.getPrimaryKey().getValue());
        vc.setUiContext(copyUIContext(v.getContext()));
        vis.add(vc);
      }
    }

    ActionMenuVisibilityContext[] ctxes = new ActionMenuVisibilityContext[vis.size()];
    ret.setVisibilityContexts(vis.toArray(ctxes));

    return ret;
  }

  private static UIContext copyUIContext(PSUiContext context) {

    UIContext ctx = new UIContext();

    ctx.setId(context.getId());
    ctx.setDescription(context.getDescription());
    ctx.setDisplayName(context.getDisplayName());
    ctx.setName(context.getName());
    return ctx;
  }

  public static ContentType convertContentType(IPSCatalogSummary s) {
    ContentType ret = new ContentType();

    ret.setName(s.getName());
    ret.setDescription(s.getDescription());
    ret.setLabel(s.getLabel());
    ret.setGuid(convertGuid(s.getGUID()));
    return ret;
  }

  public static SiteList convertSiteSummaryList(List<PSSiteSummary> list) {
    SiteList ret = new SiteList();

    for (PSSiteSummary s : list) {
      Site newSite = new Site();
      newSite.setBaseUrl(orNull(s.getBaseUrl()));
      newSite.setCanonical(s.isCanonical());
      newSite.setCanonicalDist(s.getCanonicalDist());
      newSite.setCanonicalReplace(s.isCanonicalReplace());
      newSite.setDefaultDocument(s.getDefaultDocument());
      newSite.setDefaultFileExtention(orNull(s.getDefaultFileExtention()));
      newSite.setDescription(orNull(s.getDescription()));
      newSite.setGuid(convertGuid(new PSGuid(orNull(s.getGuid()))));
      newSite.setName(s.getName());
      newSite.setOverrideSystemFoundation(s.getOverrideSystemFoundation());
      newSite.setOverrideSystemJQuery(s.getOverrideSystemJQuery());
      newSite.setOverrideSystemJQueryUI(s.getOverrideSystemJQueryUI());
      newSite.setPageBasedSite(s.isCM1Site());
      newSite.setSiteAdditionalHeadContent(orNull(s.getSiteAdditionalHeadContent()));
      newSite.setSiteAfterBodyOpenContent(orNull(s.getSiteAfterBodyOpenContent()));
      newSite.setSiteBeforeBodyCloseContent(orNull(s.getSiteBeforeBodyCloseContent()));
      newSite.setSiteProtocol(s.getSiteProtocol());
      newSite.setManagedNavigation(s.getManagedNavigation());

      ret.add(newSite);
    }
    return ret;
  }

  public static TemplateSummary convertTemplateSummary(IPSCatalogSummary sum) {
    TemplateSummary ret = new TemplateSummary();

    ret.setTemplateDescription(sum.getDescription());
    ret.setTemplateName(sum.getName());
    ret.setTemplateId(sum.getGUID().getUUID());
    ret.setTemplateLabel(sum.getLabel());

    return ret;
  }

  public static TemplateSummary convertTemplateSummary(IPSAssemblyTemplate t) {
    TemplateSummary ret = new TemplateSummary();

    ret.setTemplateDescription(t.getDescription());
    ret.setTemplateName(t.getName());
    ret.setTemplateId(t.getGUID().getUUID());
    ret.setTemplateLabel(t.getLabel());

    return ret;
  }
}
