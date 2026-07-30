/*
 * Copyright 1999-2026 Percussion Software, Inc.
 */

package com.percussion.apibridge;

import com.percussion.design.objectstore.PSControlMeta;
import com.percussion.design.objectstore.PSControlParameter;
import com.percussion.rest.cecontrols.ControlDef;
import com.percussion.rest.cecontrols.ControlParameter;
import com.percussion.rest.cecontrols.IControlAdaptor;
import com.percussion.server.PSCustomControlManager;
import com.percussion.server.PSSystemControlManager;
import com.percussion.system.utils.PSSiteManageBean;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.annotation.Lazy;

/** CE control catalog adaptor (UI-01 read) over system + custom control managers. */
@PSSiteManageBean
@Lazy
public class ControlAdaptor implements IControlAdaptor {

  private static final Logger log = LogManager.getLogger(ControlAdaptor.class);

  private static final List<String> DESIGN_GAPS =
      List.of(
          "User control create / edit / delete not supported via this API",
          "Control XSL source editing not supported via this API",
          "System controls are read-only packaged defaults");

  public ControlAdaptor() {}

  @Override
  public List<ControlDef> listControls() {
    Map<String, ControlDef> byName = new LinkedHashMap<>();
    for (ControlDef c : loadScope("system")) {
      if (c.getName() != null) {
        byName.putIfAbsent(c.getName().toLowerCase(), c);
      }
    }
    for (ControlDef c : loadScope("user")) {
      if (c.getName() != null) {
        // User overrides system when same name appears in both catalogs.
        byName.put(c.getName().toLowerCase(), c);
      }
    }
    return new ArrayList<>(byName.values());
  }

  @Override
  public ControlDef findControlByName(String name) {
    if (!isSafeControlKey(name)) {
      return null;
    }
    String key = name.trim();
    for (ControlDef c : listControls()) {
      if (c != null && key.equalsIgnoreCase(c.getName())) {
        return c;
      }
    }
    return null;
  }

  private List<ControlDef> loadScope(String scope) {
    List<ControlDef> out = new ArrayList<>();
    try {
      List<PSControlMeta> metas;
      if ("system".equals(scope)) {
        metas = PSSystemControlManager.getInstance().getAllControls();
      } else {
        metas = PSCustomControlManager.getInstance().getAllControls();
      }
      if (metas == null) {
        return out;
      }
      for (PSControlMeta meta : metas) {
        if (meta != null) {
          out.add(copyMeta(meta, scope));
        }
      }
    } catch (IllegalStateException e) {
      // Managers require server init; empty catalog rather than 500 in partial boot.
      log.debug("CE control manager not initialized for scope {}: {}", scope, e.getMessage());
    } catch (RuntimeException e) {
      log.warn("Failed to load {} CE controls: {}", scope, e.getMessage());
      log.debug(e);
    }
    return out;
  }

  private ControlDef copyMeta(PSControlMeta meta, String scope) {
    ControlDef d = new ControlDef();
    d.setName(meta.getName());
    d.setDisplayName(meta.getDisplayName());
    d.setDescription(meta.getDescription());
    d.setDimension(meta.getDimension());
    d.setChoiceSet(meta.getChoiceSet());
    d.setScope(scope);
    d.setDeprecated(meta.isDeprecated());
    d.setDeprecatedReplacement(meta.getDeprecatedReplacementName());
    d.setParameters(copyParams(meta.getParams()));
    d.setDesignGaps(new ArrayList<>(DESIGN_GAPS));
    return d;
  }

  private static List<ControlParameter> copyParams(List<?> params) {
    List<ControlParameter> out = new ArrayList<>();
    if (params == null) {
      return out;
    }
    for (Object o : params) {
      if (o instanceof PSControlParameter p) {
        ControlParameter cp = new ControlParameter();
        cp.setName(p.getName());
        cp.setDescription(p.getDescription());
        cp.setDataType(p.getDataType());
        cp.setParamType(p.getParamType());
        cp.setDefaultValue(p.getDefaultValue());
        cp.setRequired(p.isRequired());
        out.add(cp);
      }
    }
    return out;
  }

  /**
   * Single path segment name only — ASCII identifier characters used by CE
   * control names ({@code sys_EditBox}, {@code myCustomControl}). Rejects
   * traversal, separators, and any non-identifier Unicode.
   */
  static boolean isSafeControlKey(String key) {
    if (key == null || key.isBlank()) {
      return false;
    }
    // Control names are conventional ASCII identifiers with _ . - only.
    return key.matches("[A-Za-z0-9_.-]+");
  }
}
