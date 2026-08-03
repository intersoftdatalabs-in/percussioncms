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
import com.percussion.rx.design.PSDesignModelFactoryLocator;
import com.percussion.services.assembly.IPSAssemblyService;
import com.percussion.services.assembly.IPSAssemblyTemplate;
import com.percussion.services.assembly.PSAssemblyServiceLocator;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.error.PSNotFoundException;
import com.percussion.services.sitemgr.IPSSite;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

// REFACTORED: CP-JAVA11

/**
 * Setter for site and template visibility associations.
 *
 * @author Percussion Software
 */
public class PSSiteTemplateVisibilitySetter extends PSPropertySetterWithValidation {

  /** Default constructor for use by Spring. */
  public PSSiteTemplateVisibilitySetter() {}

  @Override
  protected boolean applyProperty(
      Object obj,
      ObjectState state,
      @SuppressWarnings("unused") List<IPSAssociationSet> aSets,
      String propName,
      Object propValue)
      throws Exception {
    return applyToSite(getSite(obj, propName), state, propValue);
  }

  /*
   * //see base class method for details
   */
  @Override
  protected boolean addPropertyDefs(
      Object obj, String propName, Object pvalue, Map<String, Object> defs)
      throws PSNotFoundException {
    if (super.addPropertyDefs(obj, propName, pvalue, defs)) return true;

    if (VISIBILITY.equals(propName)) {
      addFixmePropertyDefsForList(propName, pvalue, defs);
    }
    return true;
  }

  /*
   * //see base class method for details
   */
  @Override
  protected Object getPropertyValue(Object obj, String propName) throws PSNotFoundException {
    if (VISIBILITY.equals(propName)) {
      var site = getSite(obj, propName);
      var templates = new ArrayList<String>();
      for (var t : site.getAssociatedTemplates()) {
        templates.add(t.getName());
      }
      return templates;
    }

    return super.getPropertyValue(obj, propName);
  }

  /**
   * Validates the specified object and property name.
   *
   * @param obj the site object in question, it must be an instance of {@link IPSSite}.
   * @param propName the property name, it must be {@link #VISIBILITY}.
   * @return the site object, never <code>null</code>.
   */
  private IPSSite getSite(Object obj, String propName) {
    // validate the arguments.
    if (!(obj instanceof IPSSite)) {
      throw new PSConfigException("obj must be an instance of IPSSite.");
    }
    if (!VISIBILITY.equals(propName)) {
      throw new PSConfigException("Unknow property name, \"" + propName + "\".");
    }

    return (IPSSite) obj;
  }

  @Override
  protected boolean deApplyProperty(
      Object obj,
      @SuppressWarnings("unused") List<IPSAssociationSet> aSets,
      String propName,
      Object propValue) {
    var site = getSite(obj, propName);
    var curList = convertObjectToList(propValue);

    if (curList.isEmpty()) return false;

    mergeOrRemoveTemplates(site, curList, true);
    return true;
  }

  @Override
  protected List<PSConfigValidation> validate(
      String objName, ObjectState state, String propName, Object propValue, Object otherValue)
      throws PSNotFoundException {
    if (!VISIBILITY.equals(propName))
      return super.validate(objName, state, propName, propValue, otherValue);

    var curList = convertObjectToList(propValue);
    var otherList = convertObjectToList(otherValue);
    if (curList.isEmpty() || otherList.isEmpty()) return Collections.emptyList();

    var commons = new ArrayList<String>();
    commons.addAll(curList);
    commons.retainAll(otherList);
    if (commons.isEmpty()) return Collections.emptyList();

    PSConfigValidation vError;
    var msg =
        " Site \""
            + objName
            + "\" associates with Templates \""
            + curList.toString()
            + "\" is already configured.";
    vError = new PSConfigValidation(objName, VISIBILITY, true, msg);
    return Collections.singletonList(vError);
  }

  /**
   * Apply the given property value to the specific site.
   *
   * @param site the Site object, assumed not <code>null</code>.
   * @param state the state of the site, assumed not <code>null</code>.
   * @param propValue the property value, may be <code>null</code>.
   * @return <code>true</code> if the Site was modified by this method.
   * @throws Exception if an error occurs.
   */
  private boolean applyToSite(IPSSite site, ObjectState state, Object propValue) throws Exception {
    var curList = convertObjectToList(propValue);
    var prevList = getPrevTemplates();

    if (curList.isEmpty() && prevList.isEmpty()) return false;

    if (state.equals(ObjectState.PREVIOUS)) {
      mergeOrRemoveTemplates(site, prevList, true);
      return true;
    }

    var templates = new ArrayList<String>();
    templates.addAll(prevList);
    templates.removeAll(curList);
    mergeOrRemoveTemplates(site, templates, true);

    mergeOrRemoveTemplates(site, curList, false);

    return true;
  }

  /**
   * Converts the specified object to a list of string.
   *
   * @param propValue the object in question, it may be <code>null</code>.
   * @return the converted list, never <code>null</code>, may be empty.
   */
  private Collection<String> convertObjectToList(Object propValue) {
    if (propValue == null) return Collections.emptyList();

    var dmFactory = PSDesignModelFactoryLocator.getDesignModelFactory();
    var model = dmFactory.getDesignModel(PSTypeEnum.TEMPLATE);
    return PSConfigUtils.getObjectNames(propValue, model, VISIBILITY);
  }

  /**
   * Gets the template list specified in previous properties.
   *
   * @return the template list, never <code>null</code>, but may be empty.
   */
  private Collection<String> getPrevTemplates() {
    var props = getPrevProperties();
    if (props == null) return Collections.emptyList();

    return convertObjectToList(props.get(VISIBILITY));
  }

  /**
   * Merge or remove a list of Templates from the specified Site.
   *
   * @param site the Site in question, assumed not <code>null</code>.
   * @param tgtNames the list of name of the Templates that will be merged or removed from the site,
   *     assumed not <code>null</code> or empty.
   * @param isRemove <code>true</code> if remove the above Templates from the Site; otherwise, merge
   *     the Templates into the Site association.
   * @return <code>true</code> if have done merge or remove operation.
   */
  private boolean mergeOrRemoveTemplates(
      IPSSite site, Collection<String> tgtNames, boolean isRemove) {
    var isModified = false;

    var srv = PSAssemblyServiceLocator.getAssemblyService();
    for (var name : tgtNames) {
      var t = getNamedTemplate(name, site.getAssociatedTemplates());
      if (t != null) {
        if (isRemove) {
          site.getAssociatedTemplates().remove(t);
          isModified = true;
        }
        continue;
      } else if (isRemove) {
        continue;
      }

      // add the template.
      t = getTemplate(srv, name);
      if (t == null) continue;

      site.getAssociatedTemplates().add(t);
      isModified = true;
    }
    return isModified;
  }

  /**
   * Gets the template with the specified name from a list of templates.
   *
   * @param name the lookup name, assumed not <code>null</code> or empty.
   * @param templates the list of templates, assumed not <code>null</code>, but may be empty.
   * @return the template with the name. It may be <code>null</code> if cannot find one.
   */
  private IPSAssemblyTemplate getNamedTemplate(
      String name, Collection<IPSAssemblyTemplate> templates) {
    for (var t : templates) {
      if (t.getName().equalsIgnoreCase(name)) return t;
    }
    return null;
  }

  /**
   * Finds a template with the specified name.
   *
   * @param srv the service to retrieve the template, assumed not <code>null</code>.
   * @param name the template name, assumed not <code>null</code>.
   * @return the template with the name. It may be <code>null</code> if cannot find the template.
   */
  private IPSAssemblyTemplate getTemplate(IPSAssemblyService srv, String name) {
    try {
      var t = srv.findTemplateByName(name);
      return t;
    } catch (Exception e) {
      ms_log.error("Failed to load Template \"" + name + "\".", e);
    }
    return null;
  }

  /** The property name for this setter. */
  public static final String VISIBILITY = "templateVisibility";

  private static final Logger ms_log = LogManager.getLogger("PSSiteTemplateVisibilitySetter");
}
