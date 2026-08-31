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
import com.percussion.rest.actions.ActionMenu;
import com.percussion.rest.actions.ActionMenuModeUIContext;
import com.percussion.rest.actions.ActionMenuParameter;
import com.percussion.rest.actions.ActionMenuProperty;
import com.percussion.rest.actions.ActionMenuVisibilityContext;
import com.percussion.rest.actions.IActionMenuAdaptor;
import com.percussion.services.catalog.IPSCatalogSummary;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.guidmgr.data.PSGuid;
import com.percussion.services.legacy.PSCmsObjectMgrLocator;
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
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;
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

  private final IPSUiDesignWs service;
  private final BooleanSupplier adminChecker;

  /** Injected by Spring in production; unused when {@link #adminChecker} is overridden in tests. */
  @Autowired(required = false)
  private IPSUserService userService;

  @Autowired
  public ActionMenuAdaptor(IPSUiDesignWs service) {
    this(service, null);
  }

  /** Package-visible for unit tests. */
  ActionMenuAdaptor(IPSUiDesignWs service, BooleanSupplier adminChecker) {
    this.service = service;
    this.adminChecker = adminChecker != null ? adminChecker : this::isCurrentUserAdmin;
  }

  @Override
  public List<ActionMenu> findMenus(
      String name, String label, Boolean item, Boolean dynamic, Boolean cascading)
      throws PSErrorResultsException {
    var mgr = PSCmsObjectMgrLocator.getObjectManager();
    // Nested tree (roots + children) so Explorer ActionToolbar can render
    // cascading MENUs as dropdowns instead of a flat multi-row button dump (#2730).
    List<ActionMenu> tree = ApiUtils.convertPSActionMenuList(mgr.findActionMenusTree());
    return filterMenus(tree, name, label, item, dynamic, cascading);
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
    return Collections.emptyList();
  }

  @Override
  public List<ActionMenu> findAllowedContentTypes(Integer[] contentIds) {
    try {
      return ApiUtils.convertPSActionMenuList(
          PSContentTypeActionMenuHelper.getInstance().getContentTypeMenus(null));
    } catch (RuntimeException e) {
      log.error("Error finding allowed content-type menus: {}", e.getMessage(), e);
      return Collections.emptyList();
    }
  }

  @Override
  public List<ActionMenu> findAllowedTemplates(Integer contentId, boolean isAA) {
    if (contentId == null || contentId <= 0) {
      return Collections.emptyList();
    }
    try {
      return ApiUtils.convertPSActionMenuList(
          PSTemplateActionMenuHelper.getInstance().getTemplateMenus(contentId, isAA, null));
    } catch (RuntimeException e) {
      log.error(
          "Error finding allowed templates for contentId {}: {}", contentId, e.getMessage(), e);
      return Collections.emptyList();
    }
  }

  @Override
  public ActionMenu findMenuByKey(String idOrName) {
    if (!isSafeMenuKey(idOrName)) {
      return null;
    }
    String key = idOrName.trim();
    try {
      List<ActionMenu> all = findMenus(null, null, null, null, null);
      if (all == null) {
        return null;
      }
      for (ActionMenu m : all) {
        if (m == null) {
          continue;
        }
        if (key.equalsIgnoreCase(m.getName())) {
          return m;
        }
        if (String.valueOf(m.getId()).equals(key)) {
          return m;
        }
        if (m.getGuid() != null) {
          String gsv = m.getGuid().getStringValue();
          if (gsv != null && key.equalsIgnoreCase(gsv.trim())) {
            return m;
          }
        }
      }
      return null;
    } catch (PSErrorResultsException e) {
      log.debug("Action menu lookup failed for {}: {}", key, e.toString());
      return null;
    }
  }

  @Override
  public ActionMenu createActionMenu(ActionMenu body) {
    requireAdmin();
    requireSessionUserForWrite();
    if (body == null) {
      throw new IllegalArgumentException("body is required");
    }
    String name = requireValidName(body.getName());
    assertNameUnique(name);
    ActionType type = resolveCreateType(body);
    String session = currentSession();
    String user = currentUser();
    try {
      List<PSAction> created = service.createActions(List.of(name), List.of(type), session, user);
      if (created == null || created.isEmpty() || created.get(0) == null) {
        throw new IllegalStateException("Design WS createActions returned empty");
      }
      PSAction domain = created.get(0);
      applyWritableFields(domain, body);
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
    PSAction existing = findPsActionByKey(idOrName.trim());
    if (existing == null) {
      return null;
    }
    rejectSystemMenuWrite(existing);
    IPSGuid id = safeGuid(existing);
    if (id == null) {
      throw new WebApplicationException(SYSTEM_MENU_WRITE, 409);
    }
    String session = currentSession();
    String user = currentUser();
    try {
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
      if (isNotFound(e, id)) {
        return null;
      }
      throw new WebApplicationException(
          "Could not update action menu; design lock required or held by another user", 409);
    } catch (PSErrorsException e) {
      throw mapSaveOrDeleteFailure("update", e);
    }
  }

  @Override
  public boolean deleteActionMenu(String idOrName) {
    requireAdmin();
    requireSessionUserForWrite();
    if (!isSafeMenuKey(idOrName)) {
      return false;
    }
    PSAction existing = findPsActionByKey(idOrName.trim());
    if (existing == null) {
      return false;
    }
    rejectSystemMenuWrite(existing);
    IPSGuid id = safeGuid(existing);
    if (id == null) {
      throw new WebApplicationException(SYSTEM_MENU_WRITE, 409);
    }
    String session = currentSession();
    String user = currentUser();
    try {
      List<PSAction> locked = service.loadActions(List.of(id), true, false, session, user);
      if (locked == null || locked.isEmpty() || locked.get(0) == null) {
        throw new WebApplicationException(
            "Could not delete action menu; design lock required or held by another user", 409);
      }
      service.deleteActions(List.of(id), false, session, user);
      return true;
    } catch (WebApplicationException e) {
      throw e;
    } catch (PSErrorResultsException e) {
      if (isNotFound(e, id)) {
        return false;
      }
      throw new WebApplicationException(
          "Could not delete action menu; design lock required or held by another user", 409);
    } catch (PSErrorsException e) {
      throw mapSaveOrDeleteFailure("delete", e);
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
    try {
      IPSGuid match = matchSummaryGuid(service.findActions(null, null, null), key);
      if (match == null) {
        match = parseActionGuid(key);
      }
      if (match == null) {
        return null;
      }
      List<PSAction> loaded =
          service.loadActions(
              List.of(match), false, false, currentSession(), currentUser());
      if (loaded == null || loaded.isEmpty() || loaded.get(0) == null) {
        return null;
      }
      return loaded.get(0);
    } catch (PSErrorResultsException e) {
      IPSGuid requested = parseActionGuid(key);
      if (requested != null && isNotFound(e, requested)) {
        return null;
      }
      log.debug("Action menu design load failed for {}: {}", key, e.toString());
      throw new WebApplicationException(
          "Could not load action menu; design lock required or held by another user", 409);
    } catch (PSErrorException e) {
      log.error("Failed to catalog action menus while resolving {}", key, e);
      throw new IllegalStateException("Failed to catalog action menus", e);
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
    return menu;
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

  boolean isSystemMenu(PSAction action) {
    IPSGuid guid = safeGuid(action);
    if (guid == null) {
      return false;
    }
    try {
      String path = service.objectIdToPath(guid);
      return isSystemMenuPath(path);
    } catch (RuntimeException e) {
      // Fail closed: a lookup error must not skip system-menu protection.
      log.warn(
          "Could not resolve Workbench path for action {}; treating as system menu", guid, e);
      return true;
    }
  }

  /**
   * Workbench UI Elements {@code Menus/System} (and Menu Entries {@code System}) path segment.
   * Package-visible for unit tests.
   */
  static boolean isSystemMenuPath(String path) {
    if (StringUtils.isBlank(path)) {
      return false;
    }
    String[] parts = path.split("[/\\\\]+");
    for (String part : parts) {
      if ("system".equalsIgnoreCase(part.trim())) {
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
      if (nameExists(service.findActions(name, null, null), name)) {
        throw new WebApplicationException("Action menu already exists: " + name, 409);
      }
    } catch (WebApplicationException e) {
      throw e;
    } catch (PSErrorException e) {
      log.error("Failed to catalog action menus while checking uniqueness for {}", name, e);
      throw new IllegalStateException("Failed to catalog action menus", e);
    }
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

  private ActionMenuVisibilityContext[] copyVisibilityContexts(
      PSActionVisibilityContexts visibilityContexts) {
    var ctxs = new ArrayList<ActionMenuVisibilityContext>();
    var it = visibilityContexts.iterator();
    while (it.hasNext()) {
      var v = (PSActionVisibilityContext) it.next();
      var amc = new ActionMenuVisibilityContext();
      var values = new ArrayList<>();
      var vit = v.iterator();
      while (vit.hasNext()) {
        values.add(vit.next());
      }
      amc.setDescription(v.getDescription());
      amc.setName(v.getName());
      // TODO: Set values if needed
      ctxs.add(amc);
    }
    return ctxs.toArray(new ActionMenuVisibilityContext[0]);
  }

  private ActionMenuModeUIContext[] copyUIContexts(PSDbComponentCollection modeUIContexts) {
    var uictx = new ArrayList<ActionMenuModeUIContext>();
    var it = modeUIContexts.iterator();
    while (it.hasNext()) {
      var mode = (PSMenuModeContextMapping) it.next();
      var restMode = new ActionMenuModeUIContext();
      restMode.setContextId(mode.getContextId());
      restMode.setContextName(mode.getContextName());
      restMode.setModeId(mode.getModeId());
      restMode.setModeName(mode.getModeName());
      restMode.setDescription(mode.getDescription());
      uictx.add(restMode);
    }
    return uictx.toArray(new ActionMenuModeUIContext[0]);
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
