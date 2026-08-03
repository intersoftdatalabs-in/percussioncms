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

import com.percussion.cms.objectstore.PSAction;
import com.percussion.cms.objectstore.PSActionParameter;
import com.percussion.cms.objectstore.PSActionVisibilityContext;
import com.percussion.rx.config.IPSConfigHandler.ObjectState;
import com.percussion.rx.config.PSConfigException;
import com.percussion.rx.design.IPSAssociationSet;
import com.percussion.rx.design.PSDesignModelFactoryLocator;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.error.PSNotFoundException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Element;

/**
 * Property setter for {@link PSAction} objects. Handles setting URL parameters and visibility
 * contexts on actions during local config apply.
 */
public class PSActionSetter extends PSSimplePropertySetter {

  /** Default constructor for use by Spring and the framework. */
  public PSActionSetter() {}

  @Override
  protected boolean applyProperty(
      Object obj,
      ObjectState state,
      List<IPSAssociationSet> aSets,
      String propName,
      Object propValue)
      throws Exception {
    if (!(obj instanceof PSAction))
      throw new IllegalArgumentException("obj type must be PSAction.");

    var action = (PSAction) obj;
    m_actionName = action.getName();
    if (URL_PARAMS.equals(propName)) {
      setUrlParams(action, propValue);
    } else if (VISIBILITY.equals(propName)) {
      setVisibility(action, propValue);
    } else {
      super.applyProperty(obj, state, aSets, propName, propValue);
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
    if (!(obj instanceof PSAction))
      throw new IllegalArgumentException("obj type must be PSAction.");
    var action = (PSAction) obj;
    if (URL_PARAMS.equals(propName)) {
      addPropertyDefsForMap(propName, pvalue, getUrlParams(action), defs);
    } else if (VISIBILITY.equals(propName)) {
      addPropertyDefsForMap(propName, pvalue, getActionVisibility(action), defs);
    }
    return true;
  }

  @Override
  protected Object getPropertyValue(Object obj, String propName) throws PSNotFoundException {
    if (!(obj instanceof PSAction))
      throw new IllegalArgumentException("obj type must be PSAction.");
    var action = (PSAction) obj;
    m_actionName = action.getName();
    if (propName.equalsIgnoreCase("URL")) {
      return action.getURL();
    } else if (URL_PARAMS.equals(propName)) {
      return getUrlParams(action);
    } else if (VISIBILITY.equals(propName)) {
      return getActionVisibility(action);
    }
    return super.getPropertyValue(obj, propName);
  }

  /**
   * Sets the url parameters on the action. It is a full replacement. Adds all input parameters and
   * removes any parameter that does not exist in the supplied list.
   *
   * @param action The action object assumed not <code>null</code>.
   * @param propValue The url parameter values object assumed not <code>null</code>, and expects to
   *     be an instance of Map.
   */
  private void setUrlParams(PSAction action, Object propValue) {
    if (!(propValue instanceof Map)) {
      var msg =
          "The type of propValue object is not valid for " + "this setter. Expected type is Map.";
      throwError(msg, null);
    }
    var aps = action.getParameters();
    var params = (Map<String, String>) propValue;
    for (var name : params.keySet()) {
      aps.setParameter(name, params.get(name));
    }
    // Prepare a delete list by getting current params
    List<PSActionParameter> deleteList = new ArrayList<>();
    Iterator<?> apsIter = aps.iterator();
    while (apsIter.hasNext()) {
      var param = (PSActionParameter) apsIter.next();
      if (params.get(param.getName()) == null) deleteList.add(param);
    }
    // Delete the params in delete list if not empty
    if (!deleteList.isEmpty()) {
      for (var parameter : deleteList) {
        aps.remove(parameter);
      }
    }
  }

  /**
   * Convenient method to get the Url params of the action as Object.
   *
   * @param action the Action object must not be <code>null</code>.
   * @return The object corresponding to the url params. It is a map of name of the parameter(String
   *     and the value of it(String).
   */
  private Map<String, Object> getUrlParams(PSAction action) {
    var params = new HashMap<String, Object>();
    var aps = action.getParameters();
    Iterator<?> apsIter2 = aps.iterator();
    while (apsIter2.hasNext()) {
      var param = (PSActionParameter) apsIter2.next();
      params.put(param.getName(), param.getValue());
    }
    return params;
  }

  /**
   * Convenient method to get the visibility contexts of the action as Object.
   *
   * @param action the Action object must not be <code>null</code>.
   * @return The object corresponding to the Action Visibility Contexts. It is a map of name of the
   *     parameter(String and the value of it(String).
   */
  private Map<String, Object> getActionVisibility(PSAction action) {
    var propValue = new HashMap<String, Object>();
    var actionContexts = action.getVisibilityContexts();
    var ctxIter = actionContexts.iterator();
    String avCxtName = "";
    var contexts = getResourceLookupData(VISIBILITY_CONTEXTS_LOOKUP_KEY);
    Map<String, String> revContexts = (Map<String, String>) getReverseMap(contexts);
    initVisibilityContexts();
    List<String> processedContexts = new ArrayList<>();
    while (ctxIter.hasNext()) {
      var avContext = (PSActionVisibilityContext) ctxIter.next();
      avCxtName = avContext.getName();
      processedContexts.add(avCxtName);
      var propsIter = avContext.iterator();
      List<String> values = new ArrayList<>();
      var resource = m_vcResources.get(avCxtName);
      Map<String, String> supportedVals = new HashMap<>();
      if (resource != null) supportedVals = getReverseMap(getResourceLookupData(resource));
      while (propsIter.hasNext()) {
        String val = (String) propsIter.next();
        String remapped = supportedVals.get(val);
        if (remapped != null) {
          val = remapped;
        }
        values.add(val);
      }
      propValue.put(revContexts.get(avCxtName), values);
    }
    for (var cxt : revContexts.keySet()) {
      if (!processedContexts.contains(cxt)) {
        propValue.put(revContexts.get(cxt), null);
      }
    }
    return propValue;
  }

  /**
   * Sets the visibility contexts on the action. It is a full replacement for a given context. If
   * the supported values of context are static for example the values of Assignment Types context
   * are none, reader, assignee, admin. Then the supplied values are validated against these values
   * and throws {@link PSConfigException} if not valid. If the values are dynamic, like content
   * types etc. then if the object corresponding to any value does not exist in the system, then
   * that value is ignored.
   *
   * @param action The action object assumed not <code>null</code>.
   * @param propValue Must be a map of String or List of Strings.
   */
  private void setVisibility(PSAction action, Object propValue) {
    if (!(propValue instanceof Map)) {
      var msg =
          "The type of propValue object is not valid for " + "this setter. Expected type is Map.";
      throwError(msg, null);
    }
    var visContexts = getNormalizedMaps(getResourceLookupData(VISIBILITY_CONTEXTS_LOOKUP_KEY));
    var actionContexts = action.getVisibilityContexts();
    var params = (Map<String, Object>) propValue;
    var iter = params.keySet().iterator();
    initVisibilityContexts();
    while (iter.hasNext()) {
      var context = iter.next();
      var contextVal = visContexts.get(StringUtils.trimToEmpty(context).toLowerCase());
      if (contextVal == null) {
        var msg = "Supplied visibility context ({0}) is invalid.";
        Object[] args = {context};
        throwError(msg, args);
      }
      var values = params.get(context);
      if (!(values instanceof String || values instanceof List)) {
        var msg = "Unsupported object supplied for values of visibility " + "context ({0}).";
        Object[] args = {context};
        throwError(msg, args);
      }
      List<String> valList =
          values instanceof String
              ? Collections.singletonList(((String) values))
              : (List<String>) values;
      var validValues = getValidValues(context, contextVal, valList);
      // Delete the ones that do not exist in the current list
      var cxt = actionContexts.getContext(contextVal);
      if (cxt == null) {
        actionContexts.addContext(contextVal, validValues);
        continue;
      }
      List<String> delList = new ArrayList<>();
      Iterator<?> cxtIter = cxt.iterator();
      while (cxtIter.hasNext()) {
        String val = (String) cxtIter.next();
        if (!ArrayUtils.contains(validValues, val)) {
          delList.add(val);
        }
      }
      for (var val : delList) {
        cxt.remove(val);
      }
      // Add the valid context values.
      actionContexts.addContext(contextVal, validValues);
    }
  }

  /**
   * Returns the valid values from the list of passed in values, if the context supports only the
   * static values and if the supplied value is not supported then throws {@link PSConfigException}
   * otherwise ignores the value.
   *
   * @param contextVal The visibility context value, assumed not <code>null</code> and a valid
   *     context.
   * @param values List of values that needs to be validated assumed not <code>null</code>.
   * @return String array of valid values, never <code>null</code>, may be empty.
   */
  private String[] getValidValues(String context, String contextVal, List<String> values) {
    List<String> results = new ArrayList<>();
    Map<String, String> supportedVals = new HashMap<>();
    if (contextVal.equals(PSActionVisibilityContext.VIS_CONTEXT_CONTENT_TYPE)) {
      var f = PSDesignModelFactoryLocator.getDesignModelFactory();
      var dm = f.getDesignModel(PSTypeEnum.NODEDEF);
      for (var val : values) {
        try {
          var cguid = dm.nameToGuid(val);
          results.add(cguid.getUUID() + "");
        } catch (Exception e) {
          var msg =
              "Unsupported value ({0}) is supplied for "
                  + "context ({1}). Skipping the visibility context "
                  + "setting for action ({2}).";
          Object[] args = {val, context, m_actionName};
          ms_logger.warn(MessageFormat.format(msg, args));
          continue;
        }
      }
    } else {
      supportedVals = getNormalizedMaps(getResourceLookupData(m_vcResources.get(contextVal)));
      for (var val : values) {
        var sVal = supportedVals.get(StringUtils.trimToEmpty(val).toLowerCase());
        if (sVal == null) {
          if (ArrayUtils.contains(m_staticValues, contextVal)) {
            var msg = "Unsupported value ({0}) is supplied for " + "context ({1}).";
            Object[] args = {val, context};
            throwError(msg, args);
          } else {
            var msg =
                "Unsupported value ({0}) is supplied for "
                    + "context ({1}). Skipping the visibility context "
                    + "setting for action ({2}).";
            Object[] args = {val, context, m_actionName};
            ms_logger.warn(MessageFormat.format(msg, args));
            continue;
          }
        }
        results.add(sVal);
      }
    }
    return results.toArray(new String[0]);
  }

  /**
   * Returns a map with trimmed and lowercased keys.
   *
   * @param inputMap The input map assumed not <code>null</code>.
   * @return The normalized map, never <code>null</code>, may be empty.
   */
  private Map<String, String> getNormalizedMaps(Map<String, String> inputMap) {
    var normalizedMap = new HashMap<String, String>();
    for (var key : inputMap.keySet()) {
      normalizedMap.put(StringUtils.trimToEmpty(key).toLowerCase(), inputMap.get(key));
    }
    return normalizedMap;
  }

  /**
   * Gets the xml document corresponding to the supplied resource and then creates the map of the
   * name and value. Expects the result of the document follows sys_lookup.dtd.
   *
   * @param resource The name of the resource, if number treats it as lookup id.
   * @return map, never <code>null</code>, may be empty.
   */
  private static Map<String, String> getResourceLookupData(String resource) {
    var data = new HashMap<String, String>();
    var params = new HashMap();
    try {
      int lookupId = Integer.parseInt(resource);
      params.put(RXLOOKUP_KEY_PARAM, lookupId);
      resource = LOOKUP_RESOURCE;
    } catch (NumberFormatException e) {
      // Treat this as resource.
    }
    var doc = PSConfigUtils.getDocument(resource, params, false);
    var elem = doc.getDocumentElement();
    var nL = elem.getElementsByTagName(PSXENTRY);
    int sz = nL.getLength();
    for (int k = 0; k < sz; k++) {
      var psxentry = (Element) nL.item(k);
      var key = psxentry.getElementsByTagName(KEY).item(0).getFirstChild().getNodeValue();
      var value = psxentry.getElementsByTagName(VALUE).item(0).getFirstChild().getNodeValue();
      data.put(value, key);
    }
    return data;
  }

  /** Initializes the supported visibility contexts. */
  private void initVisibilityContexts() {
    m_vcResources.put(PSActionVisibilityContext.VIS_CONTEXT_ASSIGNMENT_TYPE, "121");
    m_vcResources.put(
        PSActionVisibilityContext.VIS_CONTEXT_OBJECT_TYPE,
        "sys_psxContentEditorCataloger/getObjectTypes");
    m_vcResources.put(PSActionVisibilityContext.VIS_CONTEXT_CHECKOUT_STATUS, "168");
    m_vcResources.put(PSActionVisibilityContext.VIS_CONTEXT_PUBLISHABLE_TYPE, "172");
    m_vcResources.put(PSActionVisibilityContext.VIS_CONTEXT_FOLDER_SECURITY, "24");
    m_vcResources.put(
        PSActionVisibilityContext.VIS_CONTEXT_CONTENT_TYPE,
        "sys_psxContentEditorCataloger/ContentTypeLookup");
    m_vcResources.put(
        PSActionVisibilityContext.VIS_CONTEXT_ROLES_TYPE,
        "sys_psxContentEditorCataloger/RolesLookup");
    m_vcResources.put(
        PSActionVisibilityContext.VIS_CONTEXT_LOCALES_TYPE,
        "sys_psxContentEditorCataloger/LocaleLookup");
    m_vcResources.put(
        PSActionVisibilityContext.VIS_CONTEXT_WORKFLOWS_TYPE,
        "sys_psxContentEditorCataloger/WorkflowLookup");
  }

  /**
   * Utility method to reverse the key valuse of a map.
   *
   * @param map The input map that needs to be reversed, assumed not <code>null</code>.
   * @return Reversed map, never <code>null</code>, may be empty.
   */
  private Map getReverseMap(Map<String, String> map) {
    var revMap = new HashMap<String, String>();
    for (var entryObj : map.entrySet()) {
      var entry = (Map.Entry) entryObj;
      revMap.put((String) entry.getValue(), (String) entry.getKey());
    }
    return revMap;
  }

  /**
   * Convenient method to throw error for this action.
   *
   * @param msg assumed not <code>null</code>.
   * @param args may be <code>null</code>.
   */
  private void throwError(String msg, Object[] args) {
    String m1 = "Failed to set the properties for action '" + m_actionName + "'.\n";
    String m2 = args == null ? msg : MessageFormat.format(msg, args);
    throw new PSConfigException(m1 + m2);
  }

  /** Array of visibility contexts whose values are static. */
  private static String[] m_staticValues = {
    PSActionVisibilityContext.VIS_CONTEXT_ASSIGNMENT_TYPE,
    PSActionVisibilityContext.VIS_CONTEXT_OBJECT_TYPE,
    PSActionVisibilityContext.VIS_CONTEXT_CHECKOUT_STATUS,
    PSActionVisibilityContext.VIS_CONTEXT_PUBLISHABLE_TYPE,
    PSActionVisibilityContext.VIS_CONTEXT_FOLDER_SECURITY
  };

  /** The map of visibility contexts and lookup key. */
  private Map<String, String> m_vcResources = new HashMap<String, String>();

  /** The property name for the keyword choices. */
  public static final String URL_PARAMS = "urlParams";

  /** The property name for the keyword choices. */
  public static final String VISIBILITY = "visibility";

  /**
   * The key for the possible visibility contexts for action menus. This is a key into the RXLOOKUP
   * table. See also .
   */
  private static final String VISIBILITY_CONTEXTS_LOOKUP_KEY = "157";

  /** The logger for this class. */
  private static final Logger ms_logger = LogManager.getLogger("PSActionSetter");

  /** Name of the action for logging purpose, initialized in apply property method. */
  private String m_actionName = "";

  // Resources
  private static final String LOOKUP_RESOURCE = "sys_ceSupport/lookup";

  /**
   * The HTML parameter name used to supply the key parameter to the lookup resource for global
   * keywords from t the RXLOOKUP table. Never <code>null</code>, empty or changed.
   */
  private static final String RXLOOKUP_KEY_PARAM = "key";

  // XMLNODENAMES
  private static final String PSXENTRY = "PSXEntry";

  private static final String KEY = "Value";

  private static final String VALUE = "PSXDisplayText";
}
