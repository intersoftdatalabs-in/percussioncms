/*
 * Copyright 1999-2026 Percussion Software, Inc.
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

import com.percussion.cms.objectstore.PSActionParameter;
import com.percussion.cms.objectstore.PSActionParameters;
import com.percussion.cms.objectstore.PSActionProperties;
import com.percussion.cms.objectstore.PSActionProperty;
import com.percussion.cms.objectstore.PSActionVisibilityContext;
import com.percussion.cms.objectstore.PSActionVisibilityContexts;
import com.percussion.cms.objectstore.PSDbComponentCollection;
import com.percussion.cms.objectstore.PSMenuModeContextMapping;
import com.percussion.cms.objectstore.PSAction;
import com.percussion.rest.Guid;
import com.percussion.cms.objectstore.PSChildActions;
import com.percussion.cms.objectstore.PSMenuChild;
import com.percussion.rest.actions.ActionMenu;
import com.percussion.rest.actions.ActionMenuList;
import com.percussion.rest.actions.ActionMenuModeUIContext;
import com.percussion.rest.actions.ActionMenuParameter;
import com.percussion.rest.actions.ActionMenuProperty;
import com.percussion.rest.actions.ActionMenuVisibilityContext;
import com.percussion.rest.actions.IActionMenuAdaptor;
import com.percussion.services.catalog.IPSCatalogSummary;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.guidmgr.data.PSGuid;
import com.percussion.services.legacy.IPSCmsObjectMgr;
import com.percussion.services.legacy.PSCmsObjectMgrLocator;
import com.percussion.services.menus.PSActionMenu;
import com.percussion.services.menus.RxmActionMenuConstants;
import com.percussion.services.menus.PSContentTypeActionMenuHelper;
import com.percussion.services.menus.PSTemplateActionMenuHelper;
import com.percussion.share.service.exception.PSDataServiceException;
import com.percussion.system.utils.PSSiteManageBean;
import com.percussion.user.data.PSCurrentUser;
import com.percussion.user.service.IPSUserService;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.utils.request.PSRequestInfo;
import com.percussion.webservices.PSErrorException;
import com.percussion.webservices.PSErrorResultsException;
import com.percussion.webservices.PSErrorsException;
import com.percussion.webservices.PSLockErrorException;
import com.percussion.webservices.ui.IPSUiDesignWs;
import com.percussion.webservices.ui.data.ActionType;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;

@PSSiteManageBean
@Lazy
public class ActionMenuAdaptor implements IActionMenuAdaptor {

  private static final Logger log = LogManager.getLogger(ActionMenuAdaptor.class);

  static final String ADMIN_REQUIRED =
      "Admin role required to create, update, or delete action menus";

  static final String SYSTEM_MENU_WRITE =
      "System action menus cannot be updated or deleted via this API";

  /** JDBC persist marker on REST-created user menus ({@code RXMENUACTIONPROPERTIES}). */
  static final String REST_USER_MENU_PROP = RxmActionMenuConstants.REST_USER_MENU_PROP;

  /**
   * Workbench visibility context names are {@code 1}–{@code 11} ({@link
   * PSActionVisibilityContext} {@code VIS_CONTEXT_*}). Aliases are case-insensitive and
   * ignore spaces, hyphens, and underscores.
   */
  static final Map<String, String> VISIBILITY_CONTEXT_ALIASES =
      Map.ofEntries(
          Map.entry("assignmenttype", PSActionVisibilityContext.VIS_CONTEXT_ASSIGNMENT_TYPE),
          Map.entry("community", PSActionVisibilityContext.VIS_CONTEXT_COMMUNITY),
          Map.entry("contenttype", PSActionVisibilityContext.VIS_CONTEXT_CONTENT_TYPE),
          Map.entry("objecttype", PSActionVisibilityContext.VIS_CONTEXT_OBJECT_TYPE),
          Map.entry("clientcontext", PSActionVisibilityContext.VIS_CONTEXT_CLIENT_CONTEXT),
          Map.entry("checkoutstatus", PSActionVisibilityContext.VIS_CONTEXT_CHECKOUT_STATUS),
          Map.entry("roles", PSActionVisibilityContext.VIS_CONTEXT_ROLES_TYPE),
          Map.entry("role", PSActionVisibilityContext.VIS_CONTEXT_ROLES_TYPE),
          Map.entry("locales", PSActionVisibilityContext.VIS_CONTEXT_LOCALES_TYPE),
          Map.entry("locale", PSActionVisibilityContext.VIS_CONTEXT_LOCALES_TYPE),
          Map.entry("workflows", PSActionVisibilityContext.VIS_CONTEXT_WORKFLOWS_TYPE),
          Map.entry("workflow", PSActionVisibilityContext.VIS_CONTEXT_WORKFLOWS_TYPE),
          Map.entry("publishable", PSActionVisibilityContext.VIS_CONTEXT_PUBLISHABLE_TYPE),
          Map.entry("publishabletype", PSActionVisibilityContext.VIS_CONTEXT_PUBLISHABLE_TYPE),
          Map.entry("foldersecurity", PSActionVisibilityContext.VIS_CONTEXT_FOLDER_SECURITY));

  private final IPSUiDesignWs service;
  private final BooleanSupplier adminChecker;
  private final Supplier<List<PSActionMenu>> hibernateMenus;

  /** Per-request Hibernate catalog index (name/id → menu); cleared in write finally. */
  private static final ThreadLocal<Map<String, PSActionMenu>> REQUEST_HIBERNATE_INDEX =
      new ThreadLocal<>();

  /** Injected by Spring in production; unused when {@link #adminChecker} is overridden in tests. */
  @Autowired(required = false)
  private IPSUserService userService;

  @Autowired
  public ActionMenuAdaptor(IPSUiDesignWs service) {
    this(service, null);
  }

  /** Package-visible for unit tests. */
  ActionMenuAdaptor(IPSUiDesignWs service, BooleanSupplier adminChecker) {
    this(service, adminChecker, null);
  }

  /**
   * Package-visible for unit tests that stub Hibernate {@code RXMENUACTION} catalog
   * (design-WS {@code findActions} can miss rows that GET catalog lists).
   */
  ActionMenuAdaptor(
      IPSUiDesignWs service,
      BooleanSupplier adminChecker,
      Supplier<List<PSActionMenu>> hibernateMenus) {
    this.service = service;
    this.adminChecker = adminChecker != null ? adminChecker : this::isCurrentUserAdmin;
    this.hibernateMenus = hibernateMenus != null ? hibernateMenus : this::loadHibernateMenus;
  }

  @Override
  public List<ActionMenu> findMenus(
      String name, String label, Boolean item, Boolean dynamic, Boolean cascading)
      throws PSErrorResultsException {
    try {
      var mgr = PSCmsObjectMgrLocator.getObjectManager();
      // Nested tree (roots + children) so Explorer ActionToolbar can render
      // cascading MENUs as dropdowns instead of a flat multi-row button dump (#2730).
      List<ActionMenu> tree = ApiUtils.convertPSActionMenuList(mgr.findActionMenusTree());
      return filterMenus(tree, name, label, item, dynamic, cascading);
    } finally {
      clearRequestHibernateIndex();
    }
  }

  /**
   * Applies REST query filters to a converted menu list (roots only; children stay
   * attached for matching cascading roots). Package-visible for unit tests.
   *
   * <ul>
   *   <li>{@code name} / {@code label} — case-insensitive substring match
   *   <li>{@code item} — {@code true} keeps {@code MENUITEM}; {@code false} excludes them
   *   <li>{@code dynamic} — {@code true} keeps {@code MENU} with non-blank URL; {@code false}
   *       keeps non-dynamic menus
   *   <li>{@code cascading} — {@code true} keeps {@code MENU} with blank URL; {@code false}
   *       excludes cascading menus
   * </ul>
   *
   * Null filter args mean "no constraint". All non-null filters AND together.
   */
  static List<ActionMenu> filterMenus(
      List<ActionMenu> menus,
      String name,
      String label,
      Boolean item,
      Boolean dynamic,
      Boolean cascading) {
    if (menus == null || menus.isEmpty()) {
      return menus == null ? Collections.emptyList() : menus;
    }
    if (name == null
        && label == null
        && item == null
        && dynamic == null
        && cascading == null) {
      return menus;
    }
    String nameNeedle = name == null ? null : name.trim().toLowerCase();
    String labelNeedle = label == null ? null : label.trim().toLowerCase();
    List<ActionMenu> out = new ArrayList<>(menus.size());
    for (ActionMenu m : menus) {
      if (m != null && menuMatches(m, nameNeedle, labelNeedle, item, dynamic, cascading)) {
        out.add(m);
      }
    }
    return out;
  }

  private static boolean menuMatches(
      ActionMenu m,
      String nameNeedle,
      String labelNeedle,
      Boolean item,
      Boolean dynamic,
      Boolean cascading) {
    if (nameNeedle != null && !nameNeedle.isEmpty()) {
      String n = m.getName() == null ? "" : m.getName().toLowerCase();
      if (!n.contains(nameNeedle)) {
        return false;
      }
    }
    if (labelNeedle != null && !labelNeedle.isEmpty()) {
      String l = m.getLabel() == null ? "" : m.getLabel().toLowerCase();
      if (!l.contains(labelNeedle)) {
        return false;
      }
    }
    String type = m.getMenuType() == null ? "" : m.getMenuType();
    boolean isItem = "MENUITEM".equalsIgnoreCase(type);
    boolean isMenu = "MENU".equalsIgnoreCase(type);
    String url = m.getUrl() == null ? "" : m.getUrl().trim();
    boolean isDynamicMenu = isMenu && !url.isEmpty();
    boolean isCascadingMenu = isMenu && url.isEmpty();

    if (item != null && item.booleanValue() != isItem) {
      return false;
    }
    if (dynamic != null && dynamic.booleanValue() != isDynamicMenu) {
      return false;
    }
    if (cascading != null && cascading.booleanValue() != isCascadingMenu) {
      return false;
    }
    return true;
  }

  @Override
  public List<ActionMenu> findAllowedTransitions(
      Integer[] contentIds, Integer[] assignmentTypeIds) {
    try {
      return Collections.emptyList();
    } finally {
      clearRequestHibernateIndex();
    }
  }

  @Override
  public List<ActionMenu> findAllowedContentTypes(Integer[] contentIds) {
    try {
      return ApiUtils.convertPSActionMenuList(
          PSContentTypeActionMenuHelper.getInstance().getContentTypeMenus(null));
    } catch (RuntimeException e) {
      log.error("Error finding allowed content-type menus: {}", e.getMessage(), e);
      return Collections.emptyList();
    } finally {
      clearRequestHibernateIndex();
    }
  }

  @Override
  public List<ActionMenu> findAllowedTemplates(Integer contentId, boolean isAA) {
    try {
      if (contentId == null || contentId <= 0) {
        return Collections.emptyList();
      }
      return ApiUtils.convertPSActionMenuList(
          PSTemplateActionMenuHelper.getInstance().getTemplateMenus(contentId, isAA, null));
    } catch (RuntimeException e) {
      log.error(
          "Error finding allowed templates for contentId {}: {}", contentId, e.getMessage(), e);
      return Collections.emptyList();
    } finally {
      clearRequestHibernateIndex();
    }
  }

  @Override
  public ActionMenu findMenuByKey(String idOrName) {
    try {
      if (!isSafeMenuKey(idOrName)) {
        return null;
      }
      String key = idOrName.trim();
      try {
        List<ActionMenu> all = findMenus(null, null, null, null, null);
        ActionMenu catalog = matchMenuInTree(all, key);
        if (catalog != null) {
          catalog.setPartialOverlay(!overlayDesignVisibility(catalog));
        }
        return catalog;
      } catch (PSErrorResultsException e) {
        log.debug("Action menu lookup failed for {}: {}", key, e.toString());
        return null;
      }
    } finally {
      clearRequestHibernateIndex();
    }
  }

  /**
   * GET catalog detail is Hibernate-backed; overlay visibility/uiContexts from an unlocked
   * design load so PUT then GET round-trips Workbench Visibility. Failures leave the catalog
   * DTO collections unchanged (no 409 on GET) and return {@code false} so the wire can set
   * {@code partialOverlay}.
   *
   * @return {@code true} when design collections were copied onto {@code catalog}
   */
  boolean overlayDesignVisibility(ActionMenu catalog) {
    if (catalog == null) {
      return false;
    }
    IPSGuid id = null;
    if (catalog.getGuid() != null && StringUtils.isNotBlank(catalog.getGuid().getStringValue())) {
      id = parseActionGuid(catalog.getGuid().getStringValue());
    }
    if (id == null && catalog.getId() > 0) {
      try {
        id = PSAction.getGuidFromId(catalog.getId());
      } catch (RuntimeException e) {
        return false;
      }
    }
    if (id == null) {
      return false;
    }
    try {
      List<PSAction> loaded =
          service.loadActions(
              List.of(id), false, false, currentSession(), currentUser());
      if (loaded == null || loaded.isEmpty() || loaded.get(0) == null) {
        return false;
      }
      PSAction domain = loaded.get(0);
      catalog.setVisibilityContexts(toDtoVisibilityContexts(domain.getVisibilityContexts()));
      catalog.setUiContexts(toDtoUiContexts(domain.getModeUIContexts()));
      return true;
    } catch (RuntimeException | PSErrorResultsException e) {
      log.warn(
          "Action menu design overlay skipped for {}: {}", catalog.getName(), e.toString());
      return false;
    }
  }

  static ActionMenu matchMenuInTree(List<ActionMenu> menus, String key) {
    if (menus == null || StringUtils.isBlank(key)) {
      return null;
    }
    for (ActionMenu m : menus) {
      ActionMenu hit = matchMenuRecursive(m, key);
      if (hit != null) {
        return hit;
      }
    }
    return null;
  }

  static ActionMenu matchMenuRecursive(ActionMenu m, String key) {
    if (m == null) {
      return null;
    }
    if (menuKeyMatches(m, key)) {
      return m;
    }
    if (m.getChildren() != null) {
      for (ActionMenu child : m.getChildren()) {
        ActionMenu hit = matchMenuRecursive(child, key);
        if (hit != null) {
          return hit;
        }
      }
    }
    return null;
  }

  static boolean menuKeyMatches(ActionMenu m, String key) {
    if (m == null || StringUtils.isBlank(key)) {
      return false;
    }
    if (key.equalsIgnoreCase(m.getName())) {
      return true;
    }
    if (String.valueOf(m.getId()).equals(key)) {
      return true;
    }
    if (m.getGuid() != null) {
      String gsv = m.getGuid().getStringValue();
      if (gsv != null && key.equalsIgnoreCase(gsv.trim())) {
        return true;
      }
    }
    return false;
  }

  @Override
  public ActionMenu createActionMenu(ActionMenu body) {
    requireAdmin();
    requireSessionUserForWrite();
    if (body == null) {
      throw new IllegalArgumentException("body is required");
    }
    String name = requireValidName(body.getName());
    ActionType type = resolveCreateType(body);
    String session = currentSession();
    String user = currentUser();
    try {
      assertNameUnique(name);
      List<PSAction> created = service.createActions(List.of(name), List.of(type), session, user);
      if (created == null || created.isEmpty() || created.get(0) == null) {
        throw new IllegalStateException("Design WS createActions returned empty");
      }
      PSAction domain = created.get(0);
      applyWritableFields(domain, body);
      domain.getProperties().setProperty(REST_USER_MENU_PROP, PSAction.YES);
      service.saveActions(List.of(domain), true, session, user);
      return toDto(domain);
    } catch (WebApplicationException | IllegalStateException e) {
      throw e;
    } catch (IllegalArgumentException e) {
      if (isAlreadyExistsFailure(e)) {
        throw new WebApplicationException("Action menu already exists: " + name, 409);
      }
      throw e;
    } catch (PSLockErrorException e) {
      throw new WebApplicationException(
          "Could not create action menu; design lock required or held by another user", 409);
    } catch (PSErrorsException e) {
      throw mapSaveOrDeleteFailure("create", e);
    } catch (PSErrorException e) {
      log.error("Failed to create action menu {}", name, e);
      throw new IllegalStateException("Failed to create action menu", e);
    } finally {
      clearRequestHibernateIndex();
    }
  }

  @Override
  public ActionMenu saveActionMenu(String idOrName, ActionMenu body) {
    requireAdmin();
    requireSessionUserForWrite();
    if (body == null) {
      throw new IllegalArgumentException("body is required");
    }
    if (!isSafeMenuKey(idOrName)) {
      return null;
    }
    IPSGuid id = null;
    try {
      PSAction existing = findPsActionByKey(idOrName.trim());
      if (existing == null) {
        existing = findHibernateActionByKey(idOrName.trim());
        if (existing == null) {
          return null;
        }
      }
      rejectSystemMenuWrite(existing);
      id = safeGuid(existing);
      if (id == null) {
        throw new WebApplicationException(SYSTEM_MENU_WRITE, 409);
      }
      String session = currentSession();
      String user = currentUser();
      List<PSAction> loaded = service.loadActions(List.of(id), true, false, session, user);
      if (loaded == null || loaded.isEmpty() || loaded.get(0) == null) {
        return null;
      }
      PSAction domain = loaded.get(0);
      applyWritableFields(domain, body);
      service.saveActions(List.of(domain), true, session, user);
      return toDto(domain);
    } catch (WebApplicationException | IllegalArgumentException e) {
      throw e;
    } catch (PSErrorResultsException e) {
      if (id != null && isNotFound(e, id)) {
        return null;
      }
      throw new WebApplicationException(
          "Could not update action menu; design lock required or held by another user", 409);
    } catch (PSErrorsException e) {
      throw mapSaveOrDeleteFailure("update", e);
    } finally {
      clearRequestHibernateIndex();
    }
  }

  @Override
  public ActionMenu saveActionMenuChildren(String idOrName, ActionMenuList children) {
    requireAdmin();
    requireSessionUserForWrite();
    if (!isSafeMenuKey(idOrName)) {
      return null;
    }
    IPSGuid id = null;
    try {
      PSAction existing = findPsActionByKey(idOrName.trim());
      if (existing == null) {
        existing = findHibernateActionByKey(idOrName.trim());
        if (existing == null) {
          return null;
        }
      }
      rejectSystemMenuWrite(existing);
      rejectNonCascadingParent(existing);
      id = safeGuid(existing);
      if (id == null) {
        throw new WebApplicationException(SYSTEM_MENU_WRITE, 409);
      }
      List<PSAction> resolved = resolveChildActions(existing, children);
      String session = currentSession();
      String user = currentUser();
      List<PSAction> loaded = service.loadActions(List.of(id), true, false, session, user);
      if (loaded == null || loaded.isEmpty() || loaded.get(0) == null) {
        return null;
      }
      PSAction domain = loaded.get(0);
      rejectNonCascadingParent(domain);
      replaceChildren(domain, resolved);
      service.saveActions(List.of(domain), true, session, user);
      return toDtoWithChildren(domain, resolved);
    } catch (WebApplicationException | IllegalArgumentException e) {
      throw e;
    } catch (PSErrorResultsException e) {
      if (id != null && isNotFound(e, id)) {
        return null;
      }
      throw new WebApplicationException(
          "Could not update action menu; design lock required or held by another user", 409);
    } catch (PSErrorsException e) {
      throw mapSaveOrDeleteFailure("update", e);
    } finally {
      clearRequestHibernateIndex();
    }
  }

  @Override
  public boolean deleteActionMenu(String idOrName) {
    requireAdmin();
    requireSessionUserForWrite();
    if (!isSafeMenuKey(idOrName)) {
      return false;
    }
    IPSGuid id = null;
    try {
      PSAction existing = findPsActionByKey(idOrName.trim());
      if (existing == null) {
        existing = findHibernateActionByKey(idOrName.trim());
        if (existing == null) {
          return false;
        }
      }
      rejectSystemMenuWrite(existing);
      id = safeGuid(existing);
      if (id == null) {
        throw new WebApplicationException(SYSTEM_MENU_WRITE, 409);
      }
      String session = currentSession();
      String user = currentUser();
      boolean locked = false;
      try {
        List<PSAction> held = service.loadActions(List.of(id), true, false, session, user);
        locked = held != null && !held.isEmpty() && held.get(0) != null;
      } catch (PSErrorResultsException e) {
        if (!(id != null && isNotFound(e, id))) {
          throw new WebApplicationException(
              "Could not delete action menu; design lock required or held by another user", 409);
        }
      }
      if (!locked && !isRestUserMenu(existing)) {
        return false;
      }
      service.deleteActions(List.of(id), false, session, user);
      return true;
    } catch (WebApplicationException e) {
      throw e;
    } catch (PSErrorsException e) {
      throw mapSaveOrDeleteFailure("delete", e);
    } finally {
      clearRequestHibernateIndex();
    }
  }

  /**
   * Single path component / id token only — reject traversal and separators ({@code
   * java/path-injection}).
   */
  static boolean isSafeMenuKey(String key) {
    if (key == null || key.isBlank()) {
      return false;
    }
    return !key.contains("..")
        && key.indexOf('/') < 0
        && key.indexOf('\\') < 0
        && key.indexOf('\0') < 0;
  }

  PSAction findPsActionByKey(String idOrName) {
    if (!isSafeMenuKey(idOrName)) {
      return null;
    }
    String key = idOrName.trim();
    PSAction fromHibernate = findHibernateActionByKey(key);
    if (fromHibernate != null) {
      return fromHibernate;
    }
    try {
      IPSGuid match = matchSummaryGuid(service.findActions(null, null, null), key);
      if (match == null) {
        match = parseActionGuid(key);
      }
      if (match != null) {
        List<PSAction> loaded =
            service.loadActions(
                List.of(match), false, false, currentSession(), currentUser());
        if (loaded != null && !loaded.isEmpty() && loaded.get(0) != null) {
          return loaded.get(0);
        }
      }
    } catch (PSErrorResultsException e) {
      IPSGuid requested = parseActionGuid(key);
      if (requested != null && isNotFound(e, requested)) {
        return findHibernateActionByKey(key);
      }
      log.debug("Action menu design load failed for {}: {}", key, e.toString());
      throw new WebApplicationException(
          "Could not load action menu; design lock required or held by another user", 409);
    } catch (PSErrorException e) {
      log.error("Failed to catalog action menus while resolving {}", key, e);
      throw new IllegalStateException("Failed to catalog action menus", e);
    }
    return null;
  }

  PSAction findHibernateActionByKey(String key) {
    PSActionMenu menu = matchHibernateMenu(requestHibernateMenus(), key);
    return menu == null ? null : psActionFromHibernate(menu);
  }

  List<PSActionMenu> requestHibernateMenus() {
    Map<String, PSActionMenu> index = requestHibernateIndex();
    if (index.isEmpty()) {
      return List.of();
    }
    return new ArrayList<>(index.values());
  }

  Map<String, PSActionMenu> requestHibernateIndex() {
    Map<String, PSActionMenu> cached = REQUEST_HIBERNATE_INDEX.get();
    if (cached == null) {
      cached = indexHibernateMenus(hibernateMenus.get());
      REQUEST_HIBERNATE_INDEX.set(cached);
    }
    return cached;
  }

  static void clearRequestHibernateIndex() {
    REQUEST_HIBERNATE_INDEX.remove();
  }

  static boolean isRequestHibernateIndexBound() {
    return REQUEST_HIBERNATE_INDEX.get() != null;
  }

  static Map<String, PSActionMenu> indexHibernateMenus(List<PSActionMenu> menus) {
    Map<String, PSActionMenu> out = new HashMap<>();
    if (menus == null) {
      return out;
    }
    for (PSActionMenu m : menus) {
      indexHibernateMenuRecursive(m, out);
    }
    return out;
  }

  static void indexHibernateMenuRecursive(PSActionMenu m, Map<String, PSActionMenu> out) {
    if (m == null || out == null) {
      return;
    }
    if (StringUtils.isNotBlank(m.getName())) {
      out.putIfAbsent(m.getName().toLowerCase(), m);
    }
    if (m.getActionId() > 0) {
      out.putIfAbsent(Integer.toString(m.getActionId()), m);
    }
    if (m.getChildren() != null) {
      for (PSActionMenu child : m.getChildren()) {
        indexHibernateMenuRecursive(child, out);
      }
    }
  }

  static PSActionMenu matchHibernateMenu(List<PSActionMenu> menus, String key) {
    if (menus == null || StringUtils.isBlank(key)) {
      return null;
    }
    for (PSActionMenu m : menus) {
      PSActionMenu hit = matchHibernateMenuRecursive(m, key);
      if (hit != null) {
        return hit;
      }
    }
    return null;
  }

  static PSActionMenu matchHibernateMenuRecursive(PSActionMenu m, String key) {
    if (m == null) {
      return null;
    }
    if (hibernateMenuKeyMatches(m, key)) {
      return m;
    }
    if (m.getChildren() != null) {
      for (PSActionMenu child : m.getChildren()) {
        PSActionMenu hit = matchHibernateMenuRecursive(child, key);
        if (hit != null) {
          return hit;
        }
      }
    }
    return null;
  }

  static boolean hibernateMenuKeyMatches(PSActionMenu m, String key) {
    if (m == null || StringUtils.isBlank(key)) {
      return false;
    }
    if (key.equalsIgnoreCase(StringUtils.defaultString(m.getName()))) {
      return true;
    }
    if (String.valueOf(m.getActionId()).equals(key)) {
      return true;
    }
    try {
      IPSGuid guid = PSAction.getGuidFromId(m.getActionId());
      if (guid != null && key.equalsIgnoreCase(guid.toString())) {
        return true;
      }
    } catch (RuntimeException e) {
      return false;
    }
    return false;
  }

  static PSAction psActionFromHibernate(PSActionMenu menu) {
    if (menu == null || StringUtils.isBlank(menu.getName()) || menu.getActionId() <= 0) {
      return null;
    }
    String label = StringUtils.defaultIfBlank(menu.getDisplayName(), menu.getName());
    String type = StringUtils.defaultIfBlank(menu.getType(), PSAction.TYPE_MENU);
    if (!PSAction.TYPE_MENU.equalsIgnoreCase(type)
        && !PSAction.TYPE_MENUITEM.equalsIgnoreCase(type)
        && !PSAction.TYPE_CONTEXTMENU.equalsIgnoreCase(type)) {
      type = PSAction.TYPE_MENU;
    }
    String handler =
        PSAction.HANDLER_CLIENT.equalsIgnoreCase(menu.getHandler())
            ? PSAction.HANDLER_CLIENT
            : PSAction.HANDLER_SERVER;
    PSAction action =
        new PSAction(
            menu.getName(),
            label,
            type,
            StringUtils.defaultString(menu.getUrl()),
            handler,
            menu.getSortOrder());
    action.setGUID(PSAction.getGuidFromId(menu.getActionId()));
    if (menu.getDescription() != null) {
      action.setDescription(menu.getDescription());
    }
    if (hibernateHasRestUserMenu(menu)) {
      action.getProperties().setProperty(REST_USER_MENU_PROP, PSAction.YES);
    }
    return action;
  }

  List<PSActionMenu> loadHibernateMenus() {
    try {
      IPSCmsObjectMgr mgr = PSCmsObjectMgrLocator.getObjectManager();
      if (mgr == null) {
        return Collections.emptyList();
      }
      List<PSActionMenu> tree = mgr.findActionMenusTree();
      return tree == null ? Collections.emptyList() : tree;
    } catch (RuntimeException e) {
      log.debug("Hibernate action menu catalog unavailable: {}", e.toString());
      return Collections.emptyList();
    }
  }

  static IPSGuid matchSummaryGuid(List<IPSCatalogSummary> summaries, String key) {
    if (summaries == null || StringUtils.isBlank(key)) {
      return null;
    }
    for (IPSCatalogSummary summary : summaries) {
      if (summary == null) {
        continue;
      }
      if (key.equalsIgnoreCase(StringUtils.defaultString(summary.getName()))) {
        return summary.getGUID();
      }
      IPSGuid guid = summary.getGUID();
      if (guid == null) {
        continue;
      }
      if (key.equals(String.valueOf(guid.getUUID()))) {
        return guid;
      }
      String gsv = guid.toString();
      if (gsv != null && key.equalsIgnoreCase(gsv.trim())) {
        return guid;
      }
    }
    return null;
  }

  static IPSGuid parseActionGuid(String key) {
    if (StringUtils.isBlank(key)) {
      return null;
    }
    String trimmed = key.trim();
    if (trimmed.chars().allMatch(Character::isDigit)) {
      try {
        return PSAction.getGuidFromId(Integer.parseInt(trimmed));
      } catch (NumberFormatException e) {
        return null;
      }
    }
    try {
      PSGuid guid = new PSGuid(trimmed);
      if (guid.getType() == 0) {
        return new PSGuid(PSTypeEnum.ACTION, guid.getUUID());
      }
      return guid.getType() == PSTypeEnum.ACTION.getOrdinal() ? guid : null;
    } catch (RuntimeException e) {
      return null;
    }
  }

  static void rejectNonCascadingParent(PSAction parent) {
    if (parent == null) {
      return;
    }
    if (!parent.isCascadedMenu()) {
      throw new IllegalArgumentException(
          "Child associations can only be saved on a cascading MENU");
    }
  }

  /**
   * Resolve child catalog keys to assigned {@link PSAction}s. Unknown, duplicate, or cyclic children
   * are HTTP 400 and the caller must not persist a partial graph.
   */
  List<PSAction> resolveChildActions(PSAction parent, ActionMenuList children) {
    if (children == null || children.isEmpty()) {
      return List.of();
    }
    int parentId = parent != null ? parent.getId() : -1;
    List<PSAction> resolved = new ArrayList<>(children.size());
    Set<String> seenKeys = new HashSet<>();
    Set<Integer> seenIds = new HashSet<>();
    for (ActionMenu child : children) {
      String key = childCatalogKey(child);
      if (!isSafeMenuKey(key)) {
        throw new IllegalArgumentException("child name or id is required");
      }
      String keyNorm = key.trim().toLowerCase();
      if (!seenKeys.add(keyNorm)) {
        throw new IllegalArgumentException("duplicate child in payload");
      }
      PSAction found = findPsActionByKey(key.trim());
      if (found == null) {
        found = findHibernateActionByKey(key.trim());
      }
      if (found == null || found.getId() <= 0) {
        throw new IllegalArgumentException("unknown child action menu");
      }
      if (!seenIds.add(found.getId())) {
        throw new IllegalArgumentException("duplicate child in payload");
      }
      if (parentId > 0 && found.getId() == parentId) {
        throw new IllegalArgumentException("cascading menu cycle");
      }
      resolved.add(found);
    }
    rejectChildCycles(parent, resolved);
    return resolved;
  }

  /**
   * Fail closed if attaching {@code children} would create a parent-in-descendants cycle (A→B→A
   * or parent listed as its own child). Does not mutate the graph.
   */
  void rejectChildCycles(PSAction parent, List<PSAction> children) {
    int parentId = parent != null ? parent.getId() : -1;
    if (parentId <= 0 || children == null || children.isEmpty()) {
      return;
    }
    for (PSAction child : children) {
      if (child == null) {
        continue;
      }
      if (child.getId() == parentId
          || descendantContainsParent(child, parentId, new HashSet<>())) {
        throw new IllegalArgumentException("cascading menu cycle");
      }
    }
  }

  /**
   * True when {@code parentId} is already in {@code node}'s existing descendant graph (design-WS
   * children and/or Hibernate nested tree). Walks the in-memory Hibernate index first so cycle
   * checks do not N+1 {@code loadActions} per descendant.
   */
  boolean descendantContainsParent(PSAction node, int parentId, Set<Integer> seen) {
    if (node == null || node.getId() <= 0 || parentId <= 0 || seen == null) {
      return false;
    }
    if (!seen.add(node.getId())) {
      return false;
    }
    PSChildActions kids = node.getChildren();
    if (kids != null) {
      Iterator<?> it = kids.iterator();
      while (it.hasNext()) {
        Object next = it.next();
        int childId = childActionId(next);
        if (childId == parentId) {
          return true;
        }
        if (childId > 0) {
          PSAction child =
              next instanceof PSAction action ? action : resolveDescendantForCycle(childId);
          if (descendantContainsParent(child, parentId, seen)) {
            return true;
          }
        }
      }
    }
    PSActionMenu hibernate = requestHibernateIndex().get(Integer.toString(node.getId()));
    if (hibernate != null && hibernate.getChildren() != null) {
      for (PSActionMenu child : hibernate.getChildren()) {
        if (child == null) {
          continue;
        }
        int childId = child.getActionId();
        if (childId == parentId) {
          return true;
        }
        if (childId > 0
            && descendantContainsParent(psActionFromHibernate(child), parentId, seen)) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Resolve a descendant for cycle walking: Hibernate request index (already loaded) first, then
   * design-WS only if the nested row is missing from that catalog.
   */
  PSAction resolveDescendantForCycle(int childId) {
    if (childId <= 0) {
      return null;
    }
    PSActionMenu hibernate = requestHibernateIndex().get(Integer.toString(childId));
    if (hibernate != null) {
      return psActionFromHibernate(hibernate);
    }
    return findPsActionByKey(Integer.toString(childId));
  }

  static int childActionId(Object child) {
    if (child instanceof PSMenuChild menuChild) {
      try {
        return Integer.parseInt(menuChild.getChildActionId());
      } catch (NumberFormatException e) {
        return -1;
      }
    }
    if (child instanceof PSAction action) {
      return action.getId();
    }
    return -1;
  }

  static String childCatalogKey(ActionMenu child) {
    if (child == null) {
      return null;
    }
    if (StringUtils.isNotBlank(child.getName())) {
      return child.getName().trim();
    }
    if (child.getGuid() != null && StringUtils.isNotBlank(child.getGuid().getStringValue())) {
      return child.getGuid().getStringValue().trim();
    }
    if (child.getId() > 0) {
      return String.valueOf(child.getId());
    }
    return null;
  }

  static void replaceChildren(PSAction parent, List<PSAction> children) {
    if (parent == null) {
      return;
    }
    PSChildActions existing = parent.getChildren();
    List<PSMenuChild> toRemove = new ArrayList<>();
    Iterator<?> it = existing.iterator();
    while (it.hasNext()) {
      Object next = it.next();
      if (next instanceof PSMenuChild menuChild) {
        toRemove.add(menuChild);
      }
    }
    for (PSMenuChild menuChild : toRemove) {
      existing.removeAction(menuChild);
    }
    int sort = 1;
    if (children != null) {
      for (PSAction child : children) {
        if (child == null || child.getId() <= 0) {
          continue;
        }
        child.setSortRank(sort++);
        existing.add(child);
      }
    }
  }

  static ActionMenu toDtoWithChildren(PSAction parent, List<PSAction> children) {
    ActionMenu menu = toDto(parent);
    ActionMenuList kids = new ActionMenuList();
    if (children != null) {
      int parentId = parent != null ? parent.getId() : 0;
      for (PSAction child : children) {
        ActionMenu dto = toDto(child);
        if (parentId > 0) {
          dto.setParentId(parentId);
        }
        kids.add(dto);
      }
    }
    menu.setChildren(kids);
    return menu;
  }

  static void applyWritableFields(PSAction domain, ActionMenu body) {
    if (domain == null || body == null) {
      return;
    }
    if (StringUtils.isNotBlank(body.getLabel())) {
      domain.setLabel(body.getLabel().trim());
    }
    if (body.getDescription() != null) {
      domain.setDescription(body.getDescription());
    }
    if (body.getUrl() != null) {
      domain.setURL(body.getUrl());
    }
    applyMenuType(domain, body.getMenuType());
    applyHandler(domain, body.getHandler());
    applyParameters(domain, body.getParameters());
    applyCommandProperties(domain, body.getProperties());
    applyVisibilityContexts(domain, body.getVisibilityContexts());
    applyUiContexts(domain, body.getUiContexts());
  }

  /**
   * Workbench Usage handler: {@code CLIENT} or {@code SERVER}. Blank leaves the existing
   * handler.
   */
  static void applyHandler(PSAction domain, String raw) {
    if (domain == null || StringUtils.isBlank(raw)) {
      return;
    }
    String handler = raw.trim();
    if (PSAction.HANDLER_CLIENT.equalsIgnoreCase(handler)) {
      domain.setClientAction(true);
      return;
    }
    if (PSAction.HANDLER_SERVER.equalsIgnoreCase(handler)) {
      domain.setClientAction(false);
      return;
    }
    throw new IllegalArgumentException("Invalid handler: " + raw);
  }

  /**
   * Replaces URL parameters when the PUT body includes {@code parameters} (empty array
   * clears). {@code null} leaves the existing collection. Names must be non-blank.
   */
  static void applyParameters(PSAction domain, ActionMenuParameter[] parameters) {
    if (domain == null || parameters == null) {
      return;
    }
    PSActionParameters dest = domain.getParameters();
    Map<String, ActionMenuParameter> incoming = new LinkedHashMap<>();
    for (ActionMenuParameter param : parameters) {
      if (param == null || StringUtils.isBlank(param.getName())) {
        continue;
      }
      incoming.put(param.getName().trim(), param);
    }
    List<String> toRemove = new ArrayList<>();
    Iterator<?> it = dest.iterator();
    while (it.hasNext()) {
      Object next = it.next();
      if (next instanceof PSActionParameter existing) {
        if (!containsIgnoreCase(incoming.keySet(), existing.getName())) {
          toRemove.add(existing.getName());
        }
      }
    }
    for (String name : toRemove) {
      dest.removeParameter(name);
    }
    for (ActionMenuParameter param : incoming.values()) {
      String name = param.getName().trim();
      dest.setParameter(name, param.getValue() == null ? "" : param.getValue());
      PSActionParameter stored = findParameter(dest, name);
      if (stored != null && param.getDescription() != null) {
        stored.setDescription(param.getDescription());
      }
    }
  }

  /**
   * Replaces Workbench Usage/Command properties when the PUT body includes {@code
   * properties} (empty array clears except {@link #REST_USER_MENU_PROP}). {@code null}
   * leaves existing properties. Never overwrites {@link #REST_USER_MENU_PROP}.
   */
  static void applyCommandProperties(PSAction domain, ActionMenuProperty[] properties) {
    if (domain == null || properties == null) {
      return;
    }
    PSActionProperties dest = domain.getProperties();
    Map<String, ActionMenuProperty> incoming = new LinkedHashMap<>();
    for (ActionMenuProperty prop : properties) {
      if (prop == null || StringUtils.isBlank(prop.getName())) {
        continue;
      }
      String name = prop.getName().trim();
      if (REST_USER_MENU_PROP.equalsIgnoreCase(name)) {
        continue;
      }
      incoming.put(name, prop);
    }
    List<String> toRemove = new ArrayList<>();
    Iterator<?> it = dest.iterator();
    while (it.hasNext()) {
      Object next = it.next();
      if (next instanceof PSActionProperty existing) {
        String existingName = existing.getName();
        if (REST_USER_MENU_PROP.equalsIgnoreCase(existingName)) {
          continue;
        }
        if (!containsIgnoreCase(incoming.keySet(), existingName)) {
          toRemove.add(existingName);
        }
      }
    }
    for (String name : toRemove) {
      dest.removeProperty(name);
    }
    for (ActionMenuProperty prop : incoming.values()) {
      String name = prop.getName().trim();
      dest.setProperty(name, prop.getValue() == null ? "" : prop.getValue());
      PSActionProperty stored = findProperty(dest, name);
      if (stored != null && prop.getDescription() != null) {
        stored.setDescription(prop.getDescription());
      }
    }
  }

  /**
   * Persisted RXMENUACTION id for mode-uicontext rows. Never {@code "0"} — that would
   * attach mappings to the wrong menu.
   */
  static String persistedActionId(PSAction domain) {
    if (domain == null) {
      throw new IllegalStateException("Cannot apply uiContexts until the action is persisted");
    }
    // getId() is -1 when unassigned; do not call getGUID() (it synthesizes
    // from that id and PSGuid rejects negative UUIDs).
    int id = domain.getId();
    if (id <= 0) {
      throw new IllegalStateException("Cannot apply uiContexts until the action is persisted");
    }
    return String.valueOf(id);
  }

  static boolean containsIgnoreCase(Iterable<String> names, String needle) {
    if (names == null || StringUtils.isBlank(needle)) {
      return false;
    }
    for (String name : names) {
      if (needle.equalsIgnoreCase(name)) {
        return true;
      }
    }
    return false;
  }

  static PSActionParameter findParameter(PSActionParameters params, String name) {
    if (params == null || StringUtils.isBlank(name)) {
      return null;
    }
    Iterator<?> it = params.iterator();
    while (it.hasNext()) {
      Object next = it.next();
      if (next instanceof PSActionParameter param && name.equalsIgnoreCase(param.getName())) {
        return param;
      }
    }
    return null;
  }

  /**
   * Replaces Workbench Visibility contexts when the PUT body includes {@code
   * visibilityContexts} (empty array clears). {@code null} leaves the existing collection.
   * Each element is one named context plus one value (repeat the name for multiple values).
   * Unknown names are {@link IllegalArgumentException} (HTTP 400).
   */
  static void applyVisibilityContexts(
      PSAction domain, ActionMenuVisibilityContext[] visibilityContexts) {
    if (domain == null || visibilityContexts == null) {
      return;
    }
    LinkedHashMap<String, List<String>> grouped = new LinkedHashMap<>();
    Map<String, String> descriptions = new LinkedHashMap<>();
    for (ActionMenuVisibilityContext dto : visibilityContexts) {
      if (dto == null) {
        continue;
      }
      String name = resolveVisibilityContextName(dto.getName());
      String value = dto.getValue() == null ? "" : dto.getValue();
      grouped.computeIfAbsent(name, key -> new ArrayList<>()).add(value);
      if (dto.getDescription() != null) {
        descriptions.put(name, dto.getDescription());
      }
    }
    PSActionVisibilityContexts dest = domain.getVisibilityContexts();
    List<String> existing = new ArrayList<>();
    Iterator<?> it = dest.iterator();
    while (it.hasNext()) {
      Object next = it.next();
      if (next instanceof PSActionVisibilityContext ctx && StringUtils.isNotBlank(ctx.getName())) {
        existing.add(ctx.getName());
      }
    }
    for (String name : existing) {
      dest.removeContext(name);
    }
    for (Map.Entry<String, List<String>> entry : grouped.entrySet()) {
      String name = entry.getKey();
      String[] values = entry.getValue().toArray(String[]::new);
      dest.add(new PSActionVisibilityContext(name, values, descriptions.get(name)));
    }
  }

  /**
   * Replaces mode-uicontext mappings when the PUT body includes {@code uiContexts} (empty
   * array clears). {@code null} leaves the existing collection. {@code modeId} and {@code
   * contextId} must be numeric. Duplicate mode/context pairs are {@link
   * IllegalArgumentException} (HTTP 400).
   */
  static void applyUiContexts(PSAction domain, ActionMenuModeUIContext[] uiContexts) {
    if (domain == null || uiContexts == null) {
      return;
    }
    LinkedHashMap<String, ActionMenuModeUIContext> unique = new LinkedHashMap<>();
    for (ActionMenuModeUIContext dto : uiContexts) {
      if (dto == null) {
        continue;
      }
      String modeId = requireNumericId(dto.getModeId(), "modeId");
      String contextId = requireNumericId(dto.getContextId(), "contextId");
      String key = modeId + ":" + contextId;
      if (unique.containsKey(key)) {
        throw new IllegalArgumentException("Duplicate uiContext mode/context: " + key);
      }
      unique.put(key, dto);
    }
    PSDbComponentCollection dest = domain.getModeUIContexts();
    List<PSMenuModeContextMapping> existing = new ArrayList<>();
    Iterator<?> it = dest.iterator();
    while (it.hasNext()) {
      Object next = it.next();
      if (next instanceof PSMenuModeContextMapping mapping) {
        existing.add(mapping);
      }
    }
    for (PSMenuModeContextMapping mapping : existing) {
      dest.remove(mapping);
    }
    String actionId = persistedActionId(domain);
    for (ActionMenuModeUIContext dto : unique.values()) {
      PSMenuModeContextMapping mapping =
          new PSMenuModeContextMapping(
              dto.getModeId().trim(), dto.getContextId().trim(), actionId);
      mapping.setModeName(dto.getModeName());
      mapping.setContextName(dto.getContextName());
      dest.add(mapping);
    }
  }

  /**
   * Maps a REST visibility context name to a Workbench {@code VIS_CONTEXT_*} id ({@code
   * 1}–{@code 11}). Package-visible for tests.
   */
  static String resolveVisibilityContextName(String raw) {
    if (StringUtils.isBlank(raw)) {
      throw new IllegalArgumentException("visibility context name is required");
    }
    String trimmed = raw.trim();
    if (trimmed.chars().allMatch(Character::isDigit)) {
      int n;
      try {
        n = Integer.parseInt(trimmed);
      } catch (NumberFormatException e) {
        throw new IllegalArgumentException("Invalid visibility context: " + raw);
      }
      if (n >= 1 && n <= 11) {
        return Integer.toString(n);
      }
      throw new IllegalArgumentException("Invalid visibility context: " + raw);
    }
    String aliasKey =
        trimmed.toLowerCase().replace("_", "").replace("-", "").replace(" ", "");
    String mapped = VISIBILITY_CONTEXT_ALIASES.get(aliasKey);
    if (mapped != null) {
      return mapped;
    }
    throw new IllegalArgumentException("Invalid visibility context: " + raw);
  }

  static String requireNumericId(String raw, String field) {
    if (StringUtils.isBlank(raw)) {
      throw new IllegalArgumentException(field + " is required");
    }
    String trimmed = raw.trim();
    if (!trimmed.chars().allMatch(Character::isDigit)) {
      throw new IllegalArgumentException("Invalid " + field + ": " + raw);
    }
    return trimmed;
  }

  static PSActionProperty findProperty(PSActionProperties props, String name) {
    if (props == null || StringUtils.isBlank(name)) {
      return null;
    }
    Iterator<?> it = props.iterator();
    while (it.hasNext()) {
      Object next = it.next();
      if (next instanceof PSActionProperty prop && name.equalsIgnoreCase(prop.getName())) {
        return prop;
      }
    }
    return null;
  }

  static ActionType resolveCreateType(ActionMenu body) {
    String raw = body == null ? null : body.getMenuType();
    if (StringUtils.isBlank(raw)) {
      if (body != null && StringUtils.isNotBlank(body.getUrl())) {
        return ActionType.DYNAMIC;
      }
      return ActionType.CASCADING;
    }
    String t = raw.trim();
    if (isItemType(t)) {
      return ActionType.ITEM;
    }
    if (isDynamicType(t)) {
      return ActionType.DYNAMIC;
    }
    if (isCascadingType(t) || isContextType(t)) {
      return ActionType.CASCADING;
    }
    throw new IllegalArgumentException("Invalid menu type: " + raw);
  }

  static void applyMenuType(PSAction domain, String raw) {
    if (domain == null || StringUtils.isBlank(raw)) {
      return;
    }
    String t = raw.trim();
    if (isItemType(t)) {
      domain.setMenuType(PSAction.TYPE_MENUITEM);
      domain.setMenuDynamic(false);
      return;
    }
    if (isContextType(t)) {
      domain.setMenuType(PSAction.TYPE_CONTEXTMENU);
      domain.setMenuDynamic(false);
      return;
    }
    if (isDynamicType(t)) {
      domain.setMenuType(PSAction.TYPE_MENU);
      domain.setMenuDynamic(true);
      return;
    }
    if (isCascadingType(t)) {
      domain.setMenuType(PSAction.TYPE_MENU);
      domain.setMenuDynamic(false);
      return;
    }
    throw new IllegalArgumentException("Invalid menu type: " + raw);
  }

  private static boolean isItemType(String t) {
    return "MENUITEM".equalsIgnoreCase(t) || "item".equalsIgnoreCase(t);
  }

  private static boolean isCascadingType(String t) {
    return PSAction.TYPE_MENU.equalsIgnoreCase(t) || "cascading".equalsIgnoreCase(t);
  }

  private static boolean isDynamicType(String t) {
    return "DYNAMICMENU".equalsIgnoreCase(t) || "dynamic".equalsIgnoreCase(t);
  }

  private static boolean isContextType(String t) {
    return PSAction.TYPE_CONTEXTMENU.equalsIgnoreCase(t);
  }

  static ActionMenu toDto(PSAction action) {
    ActionMenu menu = new ActionMenu();
    if (action == null) {
      return menu;
    }
    menu.setName(action.getName());
    menu.setLabel(action.getLabel());
    menu.setDescription(action.getDescription());
    menu.setUrl(action.getURL());
    menu.setMenuType(restMenuType(action));
    menu.setHandler(action.isClientAction() ? PSAction.HANDLER_CLIENT : PSAction.HANDLER_SERVER);
    menu.setSortRank(action.getSortRank());
    menu.setId(action.getId());
    IPSGuid guid = safeGuid(action);
    if (guid != null) {
      menu.setGuid(copyGuid(guid));
    }
    menu.setParameters(toDtoParameters(action.getParameters()));
    menu.setProperties(toDtoProperties(action.getProperties()));
    menu.setVisibilityContexts(toDtoVisibilityContexts(action.getVisibilityContexts()));
    menu.setUiContexts(toDtoUiContexts(action.getModeUIContexts()));
    return menu;
  }

  static ActionMenuParameter[] toDtoParameters(PSActionParameters params) {
    if (params == null || params.size() == 0) {
      return new ActionMenuParameter[0];
    }
    List<ActionMenuParameter> out = new ArrayList<>();
    Iterator<?> it = params.iterator();
    while (it.hasNext()) {
      Object next = it.next();
      if (!(next instanceof PSActionParameter param) || StringUtils.isBlank(param.getName())) {
        continue;
      }
      ActionMenuParameter dto = new ActionMenuParameter();
      dto.setName(param.getName());
      dto.setValue(param.getValue());
      dto.setDescription(param.getDescription());
      out.add(dto);
    }
    return out.toArray(ActionMenuParameter[]::new);
  }

  static ActionMenuProperty[] toDtoProperties(PSActionProperties props) {
    if (props == null || props.size() == 0) {
      return new ActionMenuProperty[0];
    }
    List<ActionMenuProperty> out = new ArrayList<>();
    Iterator<?> it = props.iterator();
    while (it.hasNext()) {
      Object next = it.next();
      if (!(next instanceof PSActionProperty prop) || StringUtils.isBlank(prop.getName())) {
        continue;
      }
      ActionMenuProperty dto = new ActionMenuProperty();
      dto.setName(prop.getName());
      dto.setValue(prop.getValue());
      dto.setDescription(prop.getDescription());
      out.add(dto);
    }
    return out.toArray(ActionMenuProperty[]::new);
  }

  static ActionMenuVisibilityContext[] toDtoVisibilityContexts(
      PSActionVisibilityContexts visibilityContexts) {
    if (visibilityContexts == null || visibilityContexts.size() == 0) {
      return new ActionMenuVisibilityContext[0];
    }
    List<ActionMenuVisibilityContext> out = new ArrayList<>();
    Iterator<?> it = visibilityContexts.iterator();
    while (it.hasNext()) {
      Object next = it.next();
      if (!(next instanceof PSActionVisibilityContext ctx)
          || StringUtils.isBlank(ctx.getName())) {
        continue;
      }
      boolean any = false;
      Iterator<?> values = ctx.iterator();
      while (values.hasNext()) {
        any = true;
        ActionMenuVisibilityContext dto = new ActionMenuVisibilityContext();
        dto.setName(ctx.getName());
        dto.setDescription(ctx.getDescription());
        Object value = values.next();
        dto.setValue(value == null ? "" : String.valueOf(value));
        out.add(dto);
      }
      if (!any) {
        ActionMenuVisibilityContext dto = new ActionMenuVisibilityContext();
        dto.setName(ctx.getName());
        dto.setDescription(ctx.getDescription());
        out.add(dto);
      }
    }
    return out.toArray(ActionMenuVisibilityContext[]::new);
  }

  static ActionMenuModeUIContext[] toDtoUiContexts(PSDbComponentCollection modeUiContexts) {
    if (modeUiContexts == null || modeUiContexts.size() == 0) {
      return new ActionMenuModeUIContext[0];
    }
    List<ActionMenuModeUIContext> out = new ArrayList<>();
    Iterator<?> it = modeUiContexts.iterator();
    while (it.hasNext()) {
      Object next = it.next();
      if (!(next instanceof PSMenuModeContextMapping mapping)) {
        continue;
      }
      ActionMenuModeUIContext dto = new ActionMenuModeUIContext();
      dto.setModeId(mapping.getModeId());
      dto.setModeName(mapping.getModeName());
      dto.setContextId(mapping.getContextId());
      dto.setContextName(mapping.getContextName());
      dto.setDescription(mapping.getDescription());
      out.add(dto);
    }
    return out.toArray(ActionMenuModeUIContext[]::new);
  }

  static String restMenuType(PSAction action) {
    if (action == null) {
      return PSAction.TYPE_MENU;
    }
    if (action.isDynamicMenu()) {
      return "DYNAMICMENU";
    }
    return action.getMenuType();
  }

  static IPSGuid safeGuid(PSAction action) {
    if (action == null) {
      return null;
    }
    try {
      if (action.getId() <= 0) {
        return null;
      }
      return action.getGUID();
    } catch (RuntimeException e) {
      return null;
    }
  }

  private static Guid copyGuid(IPSGuid guid) {
    Guid g = new Guid();
    g.setHostId(guid.getHostId());
    g.setLongValue(guid.longValue());
    g.setStringValue(guid.toString());
    g.setType(guid.getType());
    g.setUuid(guid.getUUID());
    g.setUntypedString(guid.toStringUntyped());
    return g;
  }

  void rejectSystemMenuWrite(PSAction existing) {
    if (existing == null) {
      return;
    }
    if (isSystemMenu(existing)) {
      throw new WebApplicationException(SYSTEM_MENU_WRITE, 409);
    }
  }

  /**
   * Fail-closed write guard: a {@code null} GUID (or unresolvable Workbench path)
   * is treated as a system/packaged menu and yields HTTP 409. Only REST-created
   * user menus ({@link #REST_USER_MENU_PROP}) or a User path segment may be written.
   */
  boolean isSystemMenu(PSAction action) {
    if (isRestUserMenu(action)) {
      return false;
    }
    IPSGuid guid = safeGuid(action);
    if (guid == null) {
      log.info("Action menu GUID is null; treating as system menu (fail-closed write)");
      return true;
    }
    try {
      String path = service.objectIdToPath(guid);
      if (isSystemMenuPath(path)) {
        return true;
      }
      if (isUserMenuPath(path)) {
        return false;
      }
      // Blank/unknown Workbench path: fail closed (packaged Edit is 409, not 204).
      return true;
    } catch (RuntimeException e) {
      // Fail closed: a lookup error must not skip system-menu protection.
      log.warn(
          "Could not resolve Workbench path for action {}; treating as system menu", guid, e);
      return true;
    }
  }

  boolean isRestUserMenu(PSAction action) {
    if (action == null) {
      return false;
    }
    if (PSAction.YES.equalsIgnoreCase(StringUtils.defaultString(action.getProperty(REST_USER_MENU_PROP)))) {
      return true;
    }
    PSActionMenu hibernate = lookupHibernateMenu(action.getName());
    return hibernateHasRestUserMenu(hibernate);
  }

  PSActionMenu lookupHibernateMenu(String name) {
    if (StringUtils.isBlank(name)) {
      return null;
    }
    Map<String, PSActionMenu> index = requestHibernateIndex();
    PSActionMenu byName = index.get(name.toLowerCase());
    if (byName != null) {
      return byName;
    }
    return matchHibernateMenu(hibernateMenus.get(), name);
  }

  static boolean hibernateHasRestUserMenu(PSActionMenu menu) {
    if (menu == null || menu.getProperties() == null) {
      return false;
    }
    for (com.percussion.services.menus.PSActionMenuProperty prop : menu.getProperties()) {
      if (prop == null || prop.getPrimaryKey() == null) {
        continue;
      }
      if (REST_USER_MENU_PROP.equalsIgnoreCase(prop.getPrimaryKey().getPropertyName())
          && PSAction.YES.equalsIgnoreCase(StringUtils.defaultString(prop.getValue()))) {
        return true;
      }
    }
    return false;
  }

  /**
   * Workbench UI Elements {@code Menus/System} (and Menu Entries {@code System}) path segment.
   * Package-visible for unit tests.
   */
  static boolean isSystemMenuPath(String path) {
    return pathHasSegment(path, "system");
  }

  /**
   * Workbench UI Elements {@code Menus/User} path segment for REST-created menus.
   */
  static boolean isUserMenuPath(String path) {
    return pathHasSegment(path, "user");
  }

  static boolean pathHasSegment(String path, String segment) {
    if (StringUtils.isBlank(path) || StringUtils.isBlank(segment)) {
      return false;
    }
    String[] parts = path.split("[/\\\\]+");
    for (String part : parts) {
      if (segment.equalsIgnoreCase(part.trim())) {
        return true;
      }
    }
    return false;
  }

  static String requireValidName(String raw) {
    if (StringUtils.isBlank(raw)) {
      throw new IllegalArgumentException("name is required");
    }
    String name = raw.trim();
    if (containsWhitespace(name)) {
      throw new IllegalArgumentException("name cannot contain whitespace");
    }
    if (name.contains("*") || name.contains("%")) {
      throw new IllegalArgumentException("name must not contain wildcards");
    }
    if (!isSafeMenuKey(name)) {
      throw new IllegalArgumentException("invalid name");
    }
    return name;
  }

  private static boolean containsWhitespace(String name) {
    for (int i = 0; i < name.length(); i++) {
      if (Character.isWhitespace(name.charAt(i))) {
        return true;
      }
    }
    return false;
  }

  private void assertNameUnique(String name) {
    try {
      if (nameExists(service.findActions(name, null, null), name)
          || hibernateNameExists(requestHibernateMenus(), name)) {
        throw new WebApplicationException("Action menu already exists: " + name, 409);
      }
    } catch (WebApplicationException e) {
      throw e;
    } catch (Exception e) {
      log.error("Failed to catalog action menus while checking uniqueness for {}", name, e);
      throw new IllegalStateException("Failed to catalog action menus", e);
    }
  }

  static boolean hibernateNameExists(List<PSActionMenu> menus, String name) {
    return matchHibernateMenu(menus, name) != null;
  }

  static boolean nameExists(List<IPSCatalogSummary> summaries, String name) {
    if (summaries == null || StringUtils.isBlank(name)) {
      return false;
    }
    for (IPSCatalogSummary summary : summaries) {
      if (summary != null
          && name.equalsIgnoreCase(StringUtils.defaultString(summary.getName()))) {
        return true;
      }
    }
    return false;
  }

  private void requireAdmin() {
    boolean allowed;
    try {
      allowed = adminChecker.getAsBoolean();
    } catch (WebApplicationException e) {
      throw e;
    } catch (RuntimeException e) {
      log.debug("Admin check failed: {}", e.getMessage());
      throw new WebApplicationException(ADMIN_REQUIRED, Response.Status.FORBIDDEN);
    }
    if (!allowed) {
      throw new WebApplicationException(ADMIN_REQUIRED, Response.Status.FORBIDDEN);
    }
  }

  boolean isCurrentUserAdmin() {
    if (userService == null) {
      return false;
    }
    try {
      PSCurrentUser current = userService.getCurrentUser();
      if (current == null || StringUtils.isBlank(current.getName())) {
        return false;
      }
      return userService.isAdminUser(current.getName());
    } catch (PSDataServiceException e) {
      log.debug("Unable to resolve current user for Admin check: {}", e.getMessage());
      return false;
    }
  }

  private static void requireSessionUserForWrite() {
    if (StringUtils.isBlank(currentSession()) || StringUtils.isBlank(currentUser())) {
      throw new WebApplicationException(
          "Request session/user required for action menu design write",
          Response.Status.FORBIDDEN);
    }
  }

  private static String currentSession() {
    return (String) PSRequestInfo.getRequestInfo(PSRequestInfo.KEY_JSESSIONID);
  }

  private static String currentUser() {
    return (String) PSRequestInfo.getRequestInfo(PSRequestInfo.KEY_USER);
  }

  static boolean isAlreadyExistsFailure(IllegalArgumentException e) {
    return e != null && StringUtils.containsIgnoreCase(e.getMessage(), "already exists");
  }

  static boolean isNotFound(PSErrorResultsException e, IPSGuid requested) {
    if (e == null || requested == null) {
      return false;
    }
    Map<IPSGuid, Object> errors = e.getErrors();
    Map<IPSGuid, Object> results = e.getResults();
    boolean errored = errors != null && errors.containsKey(requested);
    boolean hasResult = results != null && results.containsKey(requested);
    return errored && !hasResult && !hasLockError(e);
  }

  static boolean hasLockError(PSErrorResultsException e) {
    if (e == null || e.getErrors() == null) {
      return false;
    }
    for (Object err : e.getErrors().values()) {
      if (isLockErrorObject(err)) {
        return true;
      }
    }
    return false;
  }

  static boolean isLockError(PSErrorsException e) {
    if (e == null || e.getErrors() == null) {
      return false;
    }
    for (Object err : e.getErrors().values()) {
      if (isLockErrorObject(err)) {
        return true;
      }
    }
    return false;
  }

  static boolean isDependencyError(PSErrorsException e) {
    if (e == null || e.getErrors() == null) {
      return StringUtils.containsIgnoreCase(e != null ? e.getMessage() : null, "depend");
    }
    for (Object err : e.getErrors().values()) {
      String msg = errorMessage(err);
      if (StringUtils.containsIgnoreCase(msg, "depend")) {
        return true;
      }
    }
    return StringUtils.containsIgnoreCase(e.getMessage(), "depend");
  }

  private static boolean isLockErrorObject(Object err) {
    if (err instanceof PSLockErrorException) {
      return true;
    }
    if (err instanceof PSErrorException pe) {
      String msg = pe.getErrorMessage() != null ? pe.getErrorMessage() : pe.getMessage();
      return StringUtils.containsIgnoreCase(msg, "is not locked")
          || StringUtils.containsIgnoreCase(msg, "not locked for")
          || StringUtils.containsIgnoreCase(msg, "locked by");
    }
    String text = String.valueOf(err);
    return StringUtils.containsIgnoreCase(text, "is not locked")
        || StringUtils.containsIgnoreCase(text, "locked by");
  }

  private static String errorMessage(Object err) {
    if (err instanceof PSErrorException pe) {
      return StringUtils.defaultIfBlank(pe.getErrorMessage(), pe.getMessage());
    }
    return err != null ? String.valueOf(err) : null;
  }

  private RuntimeException mapSaveOrDeleteFailure(String verb, PSErrorsException e) {
    if (isLockError(e)) {
      return new WebApplicationException(
          "Could not " + verb + " action menu; design lock required or held by another user",
          409);
    }
    if (isDependencyError(e)) {
      return new WebApplicationException(
          "Action menu has dependents and cannot be deleted", 409);
    }
    log.error("Failed to {} action menu via UI design WS", verb, e);
    return new IllegalStateException("Failed to " + verb + " action menu", e);
  }

  private ActionMenuParameter[] copyParameters(PSActionParameters parameters) {
    var ret = new ArrayList<ActionMenuParameter>();
    var it = parameters.iterator();
    while (it.hasNext()) {
      var psap = (PSActionParameter) it.next();
      var p = new ActionMenuParameter();
      p.setDescription(psap.getDescription());
      p.setName(psap.getName());
      p.setValue(psap.getValue());
      ret.add(p);
    }
    return ret.toArray(new ActionMenuParameter[0]);
  }

  private ActionMenuProperty[] copyProperties(PSActionProperties properties) {
    var ret = new ArrayList<ActionMenuProperty>();
    var it = properties.iterator();
    while (it.hasNext()) {
      var p = (PSActionProperty) it.next();
      var prop = new ActionMenuProperty();
      prop.setDescription(p.getDescription());
      prop.setValue(p.getValue());
      prop.setName(p.getName());
      ret.add(prop);
    }
    return ret.toArray(new ActionMenuProperty[0]);
  }
}
