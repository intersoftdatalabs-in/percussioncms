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

import com.percussion.rx.config.IPSConfigHandler;
import com.percussion.rx.config.IPSPropertySetter;
import com.percussion.rx.config.PSConfigValidation;
import com.percussion.rx.design.IPSAssociationSet;
import com.percussion.rx.design.IPSDesignModel;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.error.PSNotFoundException;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.utils.types.PSPair;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 * Implementation of configure handler for design objects. Walks through all property setters and
 * calls applyProperties on each setter.
 *
 * @author bjoginipally
 */
public class PSObjectConfigHandler implements IPSConfigHandler {

  /** Default constructor for use by Spring. */
  public PSObjectConfigHandler() {}

  @Override
  public boolean process(Object obj, ObjectState state, List<IPSAssociationSet> aSets) {
    boolean changed = false;
    for (var setter : m_setters) {
      var props = setter.getProperties();
      if (props == null || props.isEmpty()) continue;
      if (setter.applyProperties(obj, state, aSets)) changed = true;
    }
    return changed;
  }

  @Override
  public boolean unprocess(Object obj, List<IPSAssociationSet> aSets) {
    boolean changed = false;
    for (var setter : m_setters) {
      var props = setter.getProperties();
      if (props == null || props.isEmpty()) continue;
      if (setter.deApplyProperties(obj, aSets)) changed = true;
    }
    return changed;
  }

  @Override
  public Map<String, Object> getPropertyDefs(Object obj) throws PSNotFoundException {
    var propDefs = new HashMap<String, Object>();
    for (var setter : m_setters) {
      setter.addPropertyDefs(obj, propDefs);
    }
    return propDefs;
  }

  @Override
  public List<IPSPropertySetter> getPropertySetters() {
    return m_setters;
  }

  @Override
  public void setPropertySetters(List<IPSPropertySetter> propSetters) {
    m_setters = propSetters;
  }

  @Override
  public PSTypeEnum getType() {
    return m_typeEnum;
  }

  /**
   * Sets the type enum of the design object (wired by Spring).
   *
   * @param type the type enum, may be <code>null</code>.
   */
  public void setType(PSTypeEnum type) {
    m_typeEnum = type;
  }

  @Override
  public String getName() {
    return m_name;
  }

  @Override
  public void setName(String name) {
    if (StringUtils.isBlank(name))
      throw new IllegalStateException("name may not be null or empty.");
    m_name = name;
  }

  @Override
  public boolean equals(Object otherObj) {
    if (!(otherObj instanceof PSObjectConfigHandler)) return false;
    var other = (PSObjectConfigHandler) otherObj;
    return new EqualsBuilder()
        .append(m_name, other.m_name)
        .append(m_setters, other.m_setters)
        .isEquals();
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder().append(m_name).append(m_setters).toHashCode();
  }

  @Override
  public List<PSPair<Object, ObjectState>> getDesignObjects(Map<String, Object> cachedObjs)
      throws PSNotFoundException {
    throw new UnsupportedOperationException("getDesignObjects() method is not supported.");
  }

  @Override
  public Object getDefaultDesignObject(Map<String, Object> cachedObjs) throws PSNotFoundException {
    throw new UnsupportedOperationException("getDefaultDesignObject() method is not supported.");
  }

  @Override
  public List<PSPair<String, ObjectState>> getObjectNames() {
    if (isGetDesignObjects()) return getObjectNamesFromHandlerImpl();

    if (getType() == null) return Collections.emptyList();

    var pair = new PSPair<String, ObjectState>(getName(), ObjectState.BOTH);
    return Collections.singletonList(pair);
  }

  /**
   * Gets the object names along with their related state. Only called when isGetDesignObjects()
   * returns true.
   */
  private List<PSPair<String, ObjectState>> getObjectNamesFromHandlerImpl() {
    var result = new ArrayList<PSPair<String, ObjectState>>();
    Collection<String> curNames = getCurNames();
    Collection<String> prevNames = getPrevNames();

    if (curNames.isEmpty() && prevNames.isEmpty()) return Collections.emptyList();

    if (prevNames == null || prevNames.isEmpty()) {
      for (var name : curNames) result.add(new PSPair<>(name, ObjectState.CURRENT));
      return result;
    }

    var names = new ArrayList<String>(curNames);
    names.retainAll(prevNames);
    for (var name : names) result.add(new PSPair<>(name, ObjectState.BOTH));

    names.clear();
    names.addAll(curNames);
    names.removeAll(prevNames);
    for (var name : names) result.add(new PSPair<>(name, ObjectState.CURRENT));

    names.clear();
    names.addAll(prevNames);
    names.removeAll(curNames);
    for (var name : names) result.add(new PSPair<>(name, ObjectState.PREVIOUS));

    return result;
  }

  /**
   * Gets the name of the design object from current properties.
   *
   * @return the collection of current names, never <code>null</code>.
   */
  protected Collection<String> getCurNames() {
    throw new UnsupportedOperationException("getCurNames() method is not supported.");
  }

  /**
   * Gets the name of the design object from previous properties.
   *
   * @return the collection of previous names, never <code>null</code>.
   */
  protected Collection<String> getPrevNames() {
    throw new UnsupportedOperationException("getPrevNames() method is not supported.");
  }

  @Override
  public boolean isGetDesignObjects() {
    return false;
  }

  @Override
  public Map<String, Object> getExtraProperties() {
    return Collections.EMPTY_MAP;
  }

  @Override
  public void setExtraProperties(Map<String, Object> props) {
    throw new UnsupportedOperationException("setExtraProperties() method is not supported.");
  }

  @Override
  public Map<String, Object> getPrevExtraProperties() {
    return m_prevExtraProps;
  }

  @Override
  public void setPrevExtraProperties(Map<String, Object> props) {
    m_prevExtraProps = props;
  }

  @Override
  public IPSGuid saveResult(
      IPSDesignModel model, Object obj, ObjectState state, List<IPSAssociationSet> assocList)
      throws PSNotFoundException {
    model.save(obj, assocList);
    return model.getGuid(obj);
  }

  @Override
  public List<PSConfigValidation> validate(IPSConfigHandler other) {
    var commonNames = getCommonObjectNames(other);
    var result = new ArrayList<PSConfigValidation>();
    for (var pair : commonNames) {
      var subResult = validate(pair.getFirst(), pair.getSecond(), other.getPropertySetters());
      result.addAll(subResult);
    }
    return result;
  }

  /**
   * Validates all setter's properties against the specified setters for a given design object name.
   *
   * @param name the name of the design object being validated, may not be <code>null</code>.
   * @param state the state of the object, may not be <code>null</code>.
   * @param oSetters the other property setters, may not be <code>null</code>.
   * @return the list of validation results, never <code>null</code>.
   */
  protected List<PSConfigValidation> validate(
      String name, ObjectState state, List<IPSPropertySetter> oSetters) {
    var result = new ArrayList<PSConfigValidation>();
    for (var mySetter : getPropertySetters()) {
      for (var oSetter : oSetters) {
        if (mySetter.getClass().equals(oSetter.getClass())) {
          var setterResult = mySetter.validate(name, state, oSetter);
          result.addAll(setterResult);
        }
      }
    }
    for (var v : result) v.setObjectType(getType());
    return result;
  }

  /**
   * Gets the object names (and their state) that are defined in both the current and the specified
   * handler.
   *
   * @param other the other config handler, may not be <code>null</code>.
   * @return the list of common object names, never <code>null</code>.
   */
  protected List<PSPair<String, ObjectState>> getCommonObjectNames(IPSConfigHandler other) {
    if (getType() == null || other.getType() == null || (!getType().equals(other.getType())))
      return Collections.emptyList();

    var myNames = getObjectNames(this);
    if (myNames.isEmpty()) return Collections.emptyList();
    var otherNames = getObjectNames(other);
    if (otherNames.isEmpty()) return Collections.emptyList();

    var names = new ArrayList<String>(myNames.keySet());
    names.retainAll(otherNames.keySet());

    var result = new ArrayList<PSPair<String, ObjectState>>();
    for (var name : names) {
      var pair = new PSPair<>(name, myNames.get(name));
      result.add(pair);
    }
    return result;
  }

  /** Gets the name/state pairs for the specified handler. */
  private Map<String, ObjectState> getObjectNames(IPSConfigHandler h) {
    var pairs = h.getObjectNames();
    var map = new HashMap<String, ObjectState>();
    for (var p : pairs) {
      map.put(p.getFirst(), p.getSecond());
    }
    return map;
  }

  private PSTypeEnum m_typeEnum;
  private String m_name = null;
  private List<IPSPropertySetter> m_setters = new ArrayList<>();
  private Map<String, Object> m_prevExtraProps;
}
