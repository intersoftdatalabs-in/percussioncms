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
import com.percussion.rest.actions.ActionMenu;
import com.percussion.rest.actions.ActionMenuModeUIContext;
import com.percussion.rest.actions.ActionMenuParameter;
import com.percussion.rest.actions.ActionMenuProperty;
import com.percussion.rest.actions.ActionMenuVisibilityContext;
import com.percussion.rest.actions.IActionMenuAdaptor;
import com.percussion.services.legacy.PSCmsObjectMgrLocator;
import com.percussion.services.menus.PSContentTypeActionMenuHelper;
import com.percussion.services.menus.PSTemplateActionMenuHelper;
import com.percussion.system.utils.PSSiteManageBean;
import com.percussion.webservices.PSErrorResultsException;
import com.percussion.webservices.ui.IPSUiDesignWs;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;

@PSSiteManageBean
@Lazy
public class ActionMenuAdaptor implements IActionMenuAdaptor {

  private static final Logger log = LogManager.getLogger(ActionMenuAdaptor.class);

  private IPSUiDesignWs service;

  @Autowired
  public ActionMenuAdaptor(IPSUiDesignWs service) {
    this.service = service;
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
    return ApiUtils.convertPSActionMenuList(
        PSContentTypeActionMenuHelper.getInstance().getContentTypeMenus(null));
  }

  @Override
  public List<ActionMenu> findAllowedTemplates(Integer contentId, boolean isAA) {
    return ApiUtils.convertPSActionMenuList(
        PSTemplateActionMenuHelper.getInstance().getTemplateMenus(contentId, isAA, null));
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
      }
      return null;
    } catch (PSErrorResultsException e) {
      log.debug("Action menu lookup failed for {}: {}", key, e.toString());
      return null;
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
