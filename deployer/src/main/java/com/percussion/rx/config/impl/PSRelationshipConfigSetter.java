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

package com.percussion.rx.config.impl;

import com.percussion.design.objectstore.PSCloneOverrideField;
import com.percussion.design.objectstore.PSCloneOverrideFieldList;
import com.percussion.design.objectstore.PSConditional;
import com.percussion.design.objectstore.PSExtensionCall;
import com.percussion.design.objectstore.PSRelationshipConfig;
import com.percussion.design.objectstore.PSRule;
import com.percussion.rx.config.IPSConfigHandler.ObjectState;
import com.percussion.rx.config.PSConfigException;
import com.percussion.rx.design.IPSAssociationSet;
import com.percussion.services.error.PSNotFoundException;
import com.percussion.util.PSCollection;
import java.text.MessageFormat;
import java.util.*;
import org.apache.commons.lang3.StringUtils;

/**
 * Relationship configuration setter to set shallowCloning, deepCloning, and fieldOverrides.
 *
 * @author bjoginipally
 */
public class PSRelationshipConfigSetter extends PSSimplePropertySetter {

  /** Default constructor for use by Spring. */
  public PSRelationshipConfigSetter() {}

  @Override
  protected boolean applyProperty(
      Object obj,
      ObjectState state,
      List<IPSAssociationSet> aSets,
      String propName,
      Object propValue)
      throws Exception {
    if (!(obj instanceof PSRelationshipConfig))
      throw new IllegalArgumentException("obj type must be PSRelationshipConfig.");
    var relConfig = (PSRelationshipConfig) obj;
    switch (propName) {
      case PROP_SHALLOWCLONING:
        setCloningProperty(relConfig, propValue, true);
        break;
      case PROP_DEEPCLONING:
        setCloningProperty(relConfig, propValue, false);
        break;
      case PROP_FIELDOVERRIDES:
        setCloneFieldOverrides(relConfig, propValue);
        break;
      default:
        super.applyProperty(obj, state, aSets, propName, propValue);
    }
    return true;
  }

  @Override
  protected boolean addPropertyDefs(
      Object obj, String propName, Object pvalue, Map<String, Object> defs)
      throws PSNotFoundException {
    if (super.addPropertyDefs(obj, propName, pvalue, defs)) return true;
    if (!(obj instanceof PSRelationshipConfig))
      throw new IllegalArgumentException("obj type must be PSRelationshipConfig.");
    var relConfig = (PSRelationshipConfig) obj;

    switch (propName) {
      case PROP_SHALLOWCLONING:
        addPropertyDefsForMap(propName, pvalue, getClonningProperty(relConfig, true), defs);
        break;
      case PROP_DEEPCLONING:
        addPropertyDefsForMap(propName, pvalue, getClonningProperty(relConfig, false), defs);
        break;
      case PROP_FIELDOVERRIDES:
        addFixmePropertyDefsForList(propName, pvalue, defs);
        break;
    }
    return true;
  }

  @Override
  protected Object getPropertyValue(Object obj, String propName) throws PSNotFoundException {
    if (!(obj instanceof PSRelationshipConfig))
      throw new IllegalArgumentException("obj type must be PSRelationshipConfig.");
    var relConfig = (PSRelationshipConfig) obj;
    switch (propName) {
      case PROP_SHALLOWCLONING:
        return getClonningProperty(relConfig, true);
      case PROP_DEEPCLONING:
        return getClonningProperty(relConfig, false);
      case PROP_FIELDOVERRIDES:
        return getCloneFieldOverrides(relConfig);
      default:
        return super.getPropertyValue(obj, propName);
    }
  }

  private void setCloneFieldOverrides(PSRelationshipConfig relConfig, Object propValue) {
    if (!(propValue instanceof List))
      throw new IllegalArgumentException(
          PROP_FIELDOVERRIDES + " property value must be a List type");
    var tempMap = (List<Map<String, Object>>) propValue;
    var cofList = new PSCloneOverrideFieldList();
    for (var map : tempMap) {
      var fieldName = (String) map.get(PROP_FIELDNAME);
      if (StringUtils.isBlank(fieldName)) {
        throw new PSConfigException(
            "The fieldOverride is missing required property \"fieldName\".");
      }
      var extName = (String) map.get(PROP_EXTENSION);
      if (StringUtils.isBlank(extName)) {
        throw new PSConfigException(
            "The fieldOverride is missing required property \"extension\".");
      }
      var extParams = (List<String>) map.get("extensionParams");
      var extCall =
          PSConfigUtils.createExtensionCall(
              extName, extParams, "com.percussion.extension.IPSUdfProcessor");
      var condition = map.get(PROP_CONDITION);
      var cofld = new PSCloneOverrideField(fieldName, extCall);
      if (condition != null) {
        var conds = PSConfigUtils.prepareConditions(condition);
        cofld.setRules(conds);
      }
      cofList.add(cofld);
    }
    relConfig.setCloneOverrideFieldList(cofList);
  }

  private Map<String, Object> getClonningProperty(
      PSRelationshipConfig relConfig, boolean isShallow) {
    var result = new HashMap<String, Object>();
    var cloneName = isShallow ? "rs_cloneshallow" : "rs_clonedeep";
    var prCheck = relConfig.getProcessCheck(cloneName);
    var iter = prCheck.getConditions();
    // enable condition
    if (iter.hasNext()) {
      var cond = (PSRule) iter.next();
      var trueCcond = PSConfigUtils.createBooleanCondition(true);
      var condRules = new PSCollection(PSConditional.class);
      condRules.add(trueCcond);
      var trueRule = new PSRule(condRules);
      if (cond.equals(trueRule)) {
        result.put(PROP_ENABLED, Boolean.TRUE);
      } else {
        result.put(PROP_ENABLED, Boolean.FALSE);
      }
    }
    var conditions = new PSCollection(PSConditional.class);
    while (iter.hasNext()) {
      conditions.add((PSConditional) iter.next());
    }
    if (!conditions.isEmpty()) {
      var condDefs = PSConfigUtils.getCondtionsDef(conditions.iterator());
      result.put(PROP_CONDITION, condDefs);
    }
    return result;
  }

  private List<Map<String, Object>> getCloneFieldOverrides(PSRelationshipConfig relConfig) {
    var result = new ArrayList<Map<String, Object>>();
    var cfList = relConfig.getCloneOverrideFieldList();
    var iter = cfList.iterator();
    while (iter.hasNext()) {
      var cf = (PSCloneOverrideField) iter.next();

      var cfEntry = new HashMap<String, Object>();
      var name = cf.getName();
      cfEntry.put(PROP_FIELDNAME, name);
      var extCall = (PSExtensionCall) cf.getReplacementValue();
      cfEntry.putAll(PSConfigUtils.getExtensionCallDef(extCall, PROP_EXTENSION));
      var rules = cf.getRules();
      if (!rules.isEmpty()) {
        var rulesDef = PSConfigUtils.getCondtionsDef(rules.iterator());
        cfEntry.put(PROP_CONDITION, rulesDef);
      }
      result.add(cfEntry);
    }
    return result;
  }

  private void setCloningProperty(
      PSRelationshipConfig relConfig, Object cloningPropValue, boolean isShallow) {
    if (!(cloningPropValue instanceof Map))
      throw new PSConfigException("cloningPropValue must be a \"Map\" type");
    var map = (Map<String, Object>) cloningPropValue;

    var enabled = (String) map.get(PROP_ENABLED);
    var condition = map.get(PROP_CONDITION);
    if (StringUtils.isBlank(enabled)) {
      var msg = "The required property ({0}) is missing for the supplied property ({1}).";
      var type = isShallow ? PROP_SHALLOWCLONING : PROP_DEEPCLONING;
      Object[] args = {PROP_ENABLED, type};
      throw new PSConfigException(MessageFormat.format(msg, args));
    }
    var cloneName = isShallow ? "rs_cloneshallow" : "rs_clonedeep";
    var prCheck = relConfig.getProcessCheck(cloneName);
    var enFlag = Boolean.valueOf(enabled);
    var cond = PSConfigUtils.createBooleanCondition(enFlag);
    var condRules = new PSCollection(PSConditional.class);
    condRules.add(cond);
    var rule = new PSRule(condRules);
    var rules = new PSCollection(PSRule.class);
    rules.add(rule);
    // If enabled and conditions are not null then add the conditions.
    if (enFlag && condition != null) {
      var conds = PSConfigUtils.prepareConditions(condition);
      rules.addAll(conds);
    }
    prCheck.setConditions(rules.iterator());
  }

  // Constants for the names of the properties handled by this setter.
  private static final String PROP_SHALLOWCLONING = "shallowCloning";
  private static final String PROP_DEEPCLONING = "deepCloning";
  private static final String PROP_FIELDOVERRIDES = "fieldOverrides";
  private static final String PROP_ENABLED = "enabled";
  private static final String PROP_CONDITION = "condition";
  private static final String PROP_FIELDNAME = "fieldName";
  private static final String PROP_EXTENSION = "extension";
}
