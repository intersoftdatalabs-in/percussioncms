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
package com.percussion.rx.config.impl;

import com.percussion.rx.config.IPSConfigHandler.ObjectState;
import com.percussion.rx.config.PSConfigException;
import com.percussion.rx.config.PSConfigValidation;
import com.percussion.rx.design.IPSAssociationSet;
import com.percussion.services.error.PSNotFoundException;
import com.percussion.services.sitemgr.IPSSite;
import com.percussion.services.sitemgr.IPSSiteManager;
import com.percussion.services.sitemgr.PSSiteManagerLocator;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.utils.types.PSPair;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;

/**
 * This setter is used to set Site properties.
 *
 * @author YuBingChen
 */
// REFACTORED: CP-JAVA11

public class PSSiteSetter extends PSPropertySetterWithValidation {

  /** Default constructor for use by Spring. */
  public PSSiteSetter() {}

  @Override
  protected boolean applyProperty(
      Object obj,
      ObjectState state,
      @SuppressWarnings("unused") List<IPSAssociationSet> aSets,
      String propName,
      Object propValue)
      throws Exception {
    // validate the arguments.
    if (!(obj instanceof IPSSite)) {
      throw new PSConfigException("obj must be an instance of IPSSite.");
    }
    var site = (IPSSite) obj;
    if (ms_propNameMap.get(propName) != null) {
      super.applyProperty(site, state, aSets, ms_propNameMap.get(propName), propValue);
    } else if (VARIABLES.equals(propName)) {
      applySiteVariables(site, state, propValue);
    } else {
      super.applyProperty(site, state, aSets, propName, propValue);
    }
    return true;
  }

  /*
   * //see base class method for details
   */
  @Override
  protected boolean addPropertyDefs(
      Object obj, String propName, Object pvalue, Map<String, Object> defs)
      throws PSNotFoundException {
    if (super.addPropertyDefs(obj, propName, pvalue, defs)) return true;

    if (VARIABLES.equals(propName)) {
      addFixmePropertyDefsForList(propName, pvalue, defs);
    }
    return true;
  }

  /*
   * //see base class method for details
   */
  @Override
  protected Object getPropertyValue(Object obj, String propName) throws PSNotFoundException {
    if (!(obj instanceof IPSSite))
      throw new PSConfigException("obj must be an instance of IPSSite.");

    if (VARIABLES.equals(propName)) {
      var site = (IPSSite) obj;
      var mgr = PSSiteManagerLocator.getSiteManager();

      var result = new ArrayList<Map<String, String>>();
      for (var ctx : mgr.findAllContexts()) {
        for (var pname : site.getPropertyNames(ctx.getGUID())) {
          var sp = new HashMap<String, String>();
          var value = site.getProperty(pname, ctx.getGUID());
          sp.put(NAME, pname);
          sp.put(CONTEXT, ctx.getName());
          sp.put(VALUE, value);
          result.add(sp);
        }
      }
      return result;
    }

    return super.getPropertyValue(obj, propName);
  }

  /*
   * //see base class method for details
   */
  @Override
  protected List<PSConfigValidation> validate(
      String objName, ObjectState state, String propName, Object propValue, Object otherValue)
      throws PSNotFoundException {
    if (!VARIABLES.equals(propName))
      return super.validate(objName, state, propName, propValue, otherValue);

    var myVars = convertObjectToMaps(propValue);
    if (myVars.isEmpty() || state.equals(ObjectState.PREVIOUS)) return Collections.emptyList();

    var otherVars = convertObjectToMaps(otherValue);
    if (otherVars.isEmpty()) return Collections.emptyList();

    PSConfigValidation vError;
    var result = new ArrayList<PSConfigValidation>();
    for (var var : myVars) {
      var pair = getSiteVariableNameCtx(var);
      var myVarName = pair.getFirst();
      if (StringUtils.isBlank(myVarName)) continue;
      for (var other : otherVars) {
        pair = getSiteVariableNameCtx(other);
        if (myVarName.equalsIgnoreCase(pair.getFirst())) {
          var msg = " the Site Variable \"" + myVarName + "\" is already configured.";
          vError = new PSConfigValidation(objName, VARIABLES, true, msg);
          result.add(vError);
        }
      }
    }
    return result;
  }

  /*
   * //see base class method for details
   */
  @Override
  protected boolean deApplyProperty(
      Object obj,
      @SuppressWarnings("unused") List<IPSAssociationSet> aSets,
      String propName,
      Object propValue)
      throws Exception {
    if (!(obj instanceof IPSSite)) {
      throw new PSConfigException("obj must be an instance of IPSSite.");
    }
    var site = (IPSSite) obj;
    if (VARIABLES.equals(propName)) {
      return deleteSiteVariables(site, convertObjectToMaps(propValue));
    }
    return false;
  }

  /**
   * Apply the supplied a specified Site Variable to a Site.
   *
   * @param site the Site that need to apply the Site Variable to, assumed not <code>null</code> or
   *     empty.
   * @param state the state of the Site, assumed not <code>null</code>.
   * @param propValue the value of the site variable.
   * @return <code>true</code> if the object has been modified.
   */
  private boolean applySiteVariables(IPSSite site, ObjectState state, Object propValue)
      throws PSNotFoundException {
    if (state.equals(ObjectState.PREVIOUS)) {
      return deleteSiteVariables(site, getPrevSiteVariables());
    } else if (state.equals(ObjectState.CURRENT)) {
      return mergeSiteVariables(site, propValue);
    } else // ObjectState.BOTH
    {
      return mergeAndDeleteSiteVariables(site, propValue);
    }
  }

  /**
   * Merges current Site Variables into the given Site, and removes the Site Variables defined in
   * previous configuration, but not in current properties.
   *
   * @param site the Site to merge the variable into, assumed not <code>null</code>.
   * @param propValue the map contains 0 or more merged Site Variables, it may be <code>null</code>
   *     or empty if there is nothing to merge.
   * @return <code>true</code> if the object has been modified.
   */
  private boolean mergeAndDeleteSiteVariables(IPSSite site, Object propValue)
      throws PSNotFoundException {
    var isChanged = mergeSiteVariables(site, propValue);
    var prevVars = getPrevSiteVariables();
    if (prevVars.isEmpty()) return isChanged;

    // collect variables in previous, but not in current
    var curVars = convertObjectToMaps(propValue);
    var deletedVars = new ArrayList<Map<String, Object>>();
    for (var var : prevVars) {
      var found = false;
      var vname = getSiteVariableNameCtx(var).getFirst();
      for (var curVar : curVars) {
        var curName = getSiteVariableNameCtx(curVar).getFirst();
        if (vname.equalsIgnoreCase(curName)) {
          found = true;
          continue;
        }
      }
      if (!found) deletedVars.add(var);
    }
    // remove the collected variables
    return deleteSiteVariables(site, deletedVars) ? true : isChanged;
  }

  /**
   * Converts the given object to a list of maps.
   *
   * @param propValue the object in question, may be <code>null</code>.
   * @return the converted list of maps, never <code>null</code>, may be empty.
   */
  private List<Map<String, Object>> convertObjectToMaps(Object propValue) {
    if (propValue == null) return Collections.emptyList();

    if (!(propValue instanceof List))
      throw new PSConfigException(
          "The type of property \"" + VARIABLES + "\" of the Site Variable Setter must be List.");

    return (List<Map<String, Object>>) propValue;
  }

  /**
   * Gets the {@link #VARIABLES} property value.
   *
   * @return the property value, may be <code>null</code> or empty if it is undefined.
   */
  private List<Map<String, Object>> getPrevSiteVariables() {
    var props = getPrevProperties();
    if (props == null || props.isEmpty()) return Collections.emptyList();

    return convertObjectToMaps(props.get(VARIABLES));
  }

  /**
   * Deletes the Site Variables that were applied in previous configuration.
   *
   * @param site the Site object with state as {@link ObjectState#PREVIOUS}.
   * @param vars the Site Variables were applied in previous configuration.
   * @return <code>true</code> if the object has been modified.
   */
  private boolean deleteSiteVariables(IPSSite site, List<Map<String, Object>> vars)
      throws PSNotFoundException {
    if (vars.isEmpty()) return false;

    for (var var : vars) {
      deleteSiteVariable(site, var);
    }
    return true;
  }

  /**
   * Merges a list of Site Variables into the given Site.
   *
   * @param site the Site to merge the variable into, assumed not <code>null</code>.
   * @param propValue the map contains 0 or more merged Site Variables, it may be <code>null</code>
   *     or empty if there is nothing to merge.
   * @return <code>true</code> if the object has been modified.
   */
  private boolean mergeSiteVariables(IPSSite site, Object propValue) throws PSNotFoundException {
    // apply the property
    var vars = convertObjectToMaps(propValue);
    if (vars.isEmpty()) return false;

    for (var var : vars) {
      mergeSiteVariable(site, var);
    }
    return true;
  }

  /**
   * Merges the one Site Variable into the given Site.
   *
   * @param site the Site to merge the variable into, assumed not <code>null</code>.
   * @param props the map contains the merged Site Variable properties, assumed not <code>null
   *     </code> or empty.
   */
  private void mergeSiteVariable(IPSSite site, Map<String, Object> props)
      throws PSNotFoundException {
    if (props == null || props.isEmpty())
      throw new PSConfigException("Properties of Site Variable cannot be null or empty.");

    var pair = getSiteVariableNameCtx(props);
    site.setProperty(pair.getFirst(), pair.getSecond(), (String) props.get(VALUE));
  }

  /**
   * Deletes the supplied Site Variable for the given Site.
   *
   * @param site the Site to delete the variable from, assumed not <code>null</code>.
   * @param props the map contains the merged Site Variable properties, assumed not <code>null
   *     </code> or empty.
   */
  private void deleteSiteVariable(IPSSite site, Map<String, Object> props)
      throws PSNotFoundException {
    if (props == null || props.isEmpty())
      throw new PSConfigException("Properties of Site Variable cannot be null or empty.");

    var pair = getSiteVariableNameCtx(props);
    site.removeProperty(pair.getFirst(), pair.getSecond());
  }

  /**
   * Gets the values of {@link #NAME} and {@link #CONTEXT} properties from the given property map
   *
   * @param props the map contains the retrieved properties, assumed not <code>null</code>.
   * @return the property values in a pair, where 1st value is the name; 2nd value is the context
   *     ID. Never <code>null</code>.
   */
  private PSPair<String, IPSGuid> getSiteVariableNameCtx(Map<String, Object> props)
      throws PSNotFoundException {
    var name = (String) props.get(NAME);
    if (name == null || StringUtils.isBlank(name))
      throw new PSConfigException("The property \"" + NAME + "\" cannot be null or empty.");

    var context = (String) props.get(CONTEXT);
    if (context == null || StringUtils.isBlank(context))
      throw new PSConfigException("The property \"" + CONTEXT + "\" cannot be null or empty.");

    var ctx = getSiteMgr().loadContext(context);

    return new PSPair<>(name, ctx.getGUID());
  }

  /**
   * Gets the Site Manager service instance.
   *
   * @return the Site Manager service instance, never <code>null</code>.
   */
  private IPSSiteManager getSiteMgr() {
    if (m_siteMgr != null) return m_siteMgr;

    m_siteMgr = PSSiteManagerLocator.getSiteManager();
    return m_siteMgr;
  }

  /** The cached Site Manager service instance. Default to <code>null</code>. */
  private IPSSiteManager m_siteMgr = null;

  /** Property name: site name. */
  public static final String NAME = "name";

  /** Property name: site context. */
  public static final String CONTEXT = "context";

  /** Property name: site property value. */
  public static final String VALUE = "value";

  /** Property name: site variables collection. */
  public static final String VARIABLES = "variables";

  private static final Map<String, String> ms_propNameMap = new HashMap<>();

  static {
    ms_propNameMap.put("siteFolderPath", "folderRoot");
    ms_propNameMap.put("publishedPath", "root");
    ms_propNameMap.put("publishedUrl", "baseUrl");
    ms_propNameMap.put("ftpAddress", "ipAddress");
    ms_propNameMap.put("ftpPort", "port");
    ms_propNameMap.put("ftpUser", "userId");
    ms_propNameMap.put("ftpPassword", "password");
  }
}
