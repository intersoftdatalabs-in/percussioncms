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

import com.percussion.extension.PSExtensionException;
import com.percussion.extension.PSExtensionManager;
import com.percussion.extension.PSExtensionRef;
import com.percussion.rx.config.IPSConfigHandler.ObjectState;
import com.percussion.rx.config.PSConfigException;
import com.percussion.rx.design.IPSAssociationSet;
import com.percussion.security.error.PSExceptionUtils;
import com.percussion.server.PSServer;
import com.percussion.services.error.PSNotFoundException;
import com.percussion.services.sitemgr.IPSLocationScheme;
import com.percussion.utils.types.PSPair;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Setter for Location Scheme-specific properties. Assumes the Location Scheme already contains
 * valid Context, Content Type, and Template.
 *
 * @author YuBingChen
 */
public class PSLocationSchemeSetter extends PSSimplePropertySetter {

  @Override
  public boolean applyProperties(Object obj, ObjectState state, List<IPSAssociationSet> aSets) {
    boolean isApplied = processGenerator(obj);
    boolean isSuperApplied = super.applyProperties(obj, state, aSets);
    validateScheme(obj);
    return isApplied || isSuperApplied;
  }

  /**
   * Validates the Location Scheme, ensuring all required properties are present.
   *
   * @param obj the Location Scheme, assumed not null.
   */
  private void validateScheme(Object obj) {
    var scheme = (IPSLocationScheme) obj;
    if (StringUtils.isBlank(scheme.getGenerator())) {
      throw new PSConfigException(
          "Failed to configure Location Scheme \""
              + scheme.getName()
              + "\". Either \""
              + EXPRESSION
              + "\" or \""
              + GENERATOR
              + "\" property must be defined.");
    }
  }

  /**
   * Processes or sets the generator property if defined. Validates that both legacy and JEXL
   * generators are not configured simultaneously.
   *
   * @param obj the Location Scheme to configure, assumed not null.
   * @return true if a new generator was set.
   */
  private boolean processGenerator(Object obj) {
    if (!(obj instanceof IPSLocationScheme)) {
      throw new PSConfigException("obj must be an instance of IPSLocationScheme.");
    }
    var scheme = (IPSLocationScheme) obj;

    var generator = getProperties().get(GENERATOR);
    var expr = getProperties().get(EXPRESSION);
    var params = getProperties().get(GENERATOR_PARAMS);

    if (expr != null && generator != null) {
      throw new PSConfigException(
          "Failed to configure Location Scheme \""
              + scheme.getName()
              + "\". Both \""
              + EXPRESSION
              + "\" and \""
              + GENERATOR
              + "\" properties cannot be specified at the same time.");
    }
    if (expr != null && params != null) {
      throw new PSConfigException(
          "Failed to configure Location Scheme \""
              + scheme.getName()
              + "\". Both \""
              + EXPRESSION
              + "\" and \""
              + GENERATOR_PARAMS
              + "\" properties cannot be specified at the same time.");
    }

    if (generator == null) return false;

    var ext = getExtensionRef((String) generator);
    if (ext == null) {
      throw new PSConfigException("Generator extension \"" + generator + "\" not found.");
    }
    scheme.setGenerator(ext.getFQN());
    return true;
  }

  @Override
  protected boolean applyProperty(
      Object obj,
      ObjectState state,
      List<IPSAssociationSet> aSets,
      String propName,
      Object propValue)
      throws Exception {
    var scheme = (IPSLocationScheme) obj;
    switch (propName) {
      case EXPRESSION:
        setJexlExpression(scheme, propValue);
        break;
      case GENERATOR_PARAMS:
        setGeneratorParams(scheme, propValue);
        break;
      case GENERATOR:
        // Already processed in processGenerator()
        return true;
      default:
        super.applyProperty(scheme, state, aSets, propName, propValue);
    }
    return true;
  }

  @Override
  protected Object getPropertyValue(Object obj, String propName) throws PSNotFoundException {
    if (!(obj instanceof IPSLocationScheme)) {
      throw new PSConfigException("obj must be an instance of IPSLocationScheme.");
    }
    var scheme = (IPSLocationScheme) obj;
    if (EXPRESSION.equals(propName)) {
      return scheme.getParameterValue(EXPRESSION);
    } else if (GENERATOR_PARAMS.equals(propName)) {
      var params = new ArrayList<PSPair<String, String>>();
      for (var n : scheme.getParameterNames()) {
        var value = scheme.getParameterValue(n);
        params.add(new PSPair<>(n, value));
      }
      return params;
    }
    return super.getPropertyValue(obj, propName);
  }

  /** Sets the generator with the JEXL expression. */
  private void setJexlExpression(IPSLocationScheme scheme, Object propValue) {
    if (!(propValue instanceof String)) {
      throw new PSConfigException(EXPRESSION + " property must be a string.");
    }
    var expr = (String) propValue;
    scheme.setGenerator(JEXL_GENERATOR);
    scheme.setParameter(EXPRESSION, "String", expr);
  }

  /** Sets the generator's parameters. */
  private void setGeneratorParams(IPSLocationScheme scheme, Object propValue) {
    var params = filterParameters(scheme.getGenerator(), propValue);

    // Remove existing parameters
    var pnames = new ArrayList<>(scheme.getParameterNames());
    for (var n : pnames) {
      scheme.removeParameter(n);
    }

    // Add new parameters
    for (int i = 0; i < params.size(); i++) {
      var p = params.get(i);
      scheme.addParameter(p.getFirst(), i, "String", p.getSecond());
    }
  }

  /**
   * Filters the given parameters for the supplied Java extension. Removes entries whose keys are
   * not defined as parameters of the slot-filter.
   */
  private List<PSPair<String, String>> filterParameters(String extFQN, Object propValue) {
    if (!(propValue instanceof List)) {
      throw new PSConfigException(
          "\"" + GENERATOR_PARAMS + "\" property must be defined by pvalues, as a list of pairs.");
    }
    var props = (List<PSPair<String, String>>) propValue;
    var params = new ArrayList<PSPair<String, String>>();
    var names = PSConfigUtils.getExtensionParameterNames(extFQN);
    for (var p : props) {
      if (!names.contains(p.getFirst())) {
        var ref = new PSExtensionRef(extFQN);
        log.warn(
            "Skip finder argument \"{}\" since it is not a parameter defined by generator \"{}\".",
            p.getFirst(),
            ref.getExtensionName());
        continue;
      }
      params.add(p);
    }
    return params;
  }

  /**
   * Gets the Extension Reference for the specified generator name.
   *
   * @param extName the extension/generator name (not FQN).
   * @return the extension reference, or null if not found.
   */
  private PSExtensionRef getExtensionRef(String extName) {
    var mgr = (PSExtensionManager) PSServer.getExtensionManager(null);
    try {
      var iterator =
          mgr.getExtensionNames(
              null, null, "com.percussion.extension.IPSAssemblyLocation", extName);
      while (iterator.hasNext()) {
        return (PSExtensionRef) iterator.next();
      }
    } catch (PSExtensionException e) {
      log.error(PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
    }
    return null;
  }

  private static final Logger log = LogManager.getLogger(PSLocationSchemeSetter.class);

  /** The JEXL expression property name. */
  /** JEXL assembly location generator FQN (was on retired JSF PSLocationSchemeEditor). */
  public static final String JEXL_GENERATOR =
      "Java/global/percussion/contentassembler/sys_JexlAssemblyLocation";

  public static final String EXPRESSION = "expression";

  /** The generator name property. */
  public static final String GENERATOR = "generator";

  /** The generator parameter property. */
  public static final String GENERATOR_PARAMS = "generatorParams";
}
