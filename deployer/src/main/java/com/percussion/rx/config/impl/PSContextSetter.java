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

import com.percussion.rx.config.IPSConfigHandler.ObjectState;
import com.percussion.rx.config.PSConfigException;
import com.percussion.rx.design.IPSAssociationSet;
import com.percussion.services.error.PSNotFoundException;
import com.percussion.services.sitemgr.IPSPublishingContext;
import java.util.List;
import java.util.Objects;

/**
 * This setter is used to set Context properties.
 *
 * @author YuBingChen
 */
public class PSContextSetter extends PSSimplePropertySetter {

  /** Default constructor for use by Spring. */
  public PSContextSetter() {}

  @Override
  protected boolean applyProperty(
      Object obj,
      ObjectState state,
      List<IPSAssociationSet> aSets,
      String propName,
      Object propValue)
      throws Exception {
    Objects.requireNonNull(obj, "obj must not be null");
    if (!(obj instanceof IPSPublishingContext)) {
      throw new PSConfigException("obj must be an instance of IPSPublishingContext.");
    }
    var context = (IPSPublishingContext) obj;
    if (DEFAULT_SCHEME.equals(propName)) {
      setDefaultScheme(context, propName, propValue);
    } else {
      super.applyProperty(context, state, aSets, propName, propValue);
    }
    return true;
  }

  @Override
  protected Object getPropertyValue(Object obj, String propName) throws PSNotFoundException {
    if (DEFAULT_SCHEME.equals(propName)) {
      var context = (IPSPublishingContext) obj;
      if (context.getDefaultSchemeId() == null) return null;
      var model = PSConfigUtils.getSchemeModel();
      model.setContextId(context.getGUID());
      return model.guidToName(context.getDefaultSchemeId());
    }
    return super.getPropertyValue(obj, propName);
  }

  private void setDefaultScheme(IPSPublishingContext context, String propName, Object propValue)
      throws Exception {
    if (propValue == null || org.apache.commons.lang3.StringUtils.isBlank((String) propValue)) {
      context.setDefaultSchemeId(null);
      return;
    }
    if (!(propValue instanceof String)) {
      throw new PSConfigException(
          "The name of the default Location Scheme must be a string. It cannot be type of \""
              + propValue.getClass().getName()
              + "\".");
    }
    var model = PSConfigUtils.getSchemeModel();
    model.setContextId(context.getGUID());
    var schemeName = (String) propValue;
    var schemeId = model.nameToGuid(schemeName);
    if (schemeId == null) {
      throw new PSConfigException(
          "Failed to set the default Location Scheme for Context \""
              + context.getName()
              + "\". This is because the Location Scheme name, \""
              + schemeName
              + "\" does not exist within the Context.");
    }
    context.setDefaultSchemeId(schemeId);
  }

  /** The property name for the default location scheme. */
  public static final String DEFAULT_SCHEME = "defaultLocationScheme";
}
