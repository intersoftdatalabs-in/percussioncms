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
import com.percussion.services.filter.PSFilterException;
import com.percussion.services.filter.PSFilterServiceLocator;
import com.percussion.services.publisher.IPSContentList;
import com.percussion.services.publisher.PSPublisherServiceLocator;
import com.percussion.system.utils.IPSHtmlParameters;
import com.percussion.system.utils.PSUrlUtils;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * The setter for configuring the properties of {@link IPSContentList} object.
 *
 * @author YuBingChen
 */
public class PSContentListSetter extends PSSimplePropertySetter {

  /** Default constructor for use by Spring. */
  public PSContentListSetter() {}

  @Override
  protected boolean applyProperty(
      Object obj,
      ObjectState state,
      List<IPSAssociationSet> aSets,
      String propName,
      Object propValue)
      throws Exception {
    Objects.requireNonNull(obj, "obj must not be null");
    if (!(obj instanceof IPSContentList)) {
      throw new IllegalArgumentException("obj type must be IPSContentList.");
    }
    var cList = (IPSContentList) obj;
    switch (propName) {
      case DELIVERY_TYPE:
        setDeliveryType(cList, propValue);
        break;
      case EXPANDER_PARAMS:
        setExpanderParams(cList, propValue);
        break;
      case GEN_PARAMS:
        setGeneratorParams(cList, propValue);
        break;
      case FILTER:
        setFilter(cList, propValue);
        break;
      default:
        super.applyProperty(cList, state, aSets, propName, propValue);
    }
    return true;
  }

  @Override
  protected boolean addPropertyDefs(
      Object obj, String propName, Object pvalue, Map<String, Object> defs)
      throws PSNotFoundException {
    if (super.addPropertyDefs(obj, propName, pvalue, defs)) return true;

    var cList = (IPSContentList) obj;
    if (EXPANDER_PARAMS.equals(propName)) {
      var params = new HashMap<String, Object>(cList.getExpanderParams());
      addPropertyDefsForMap(propName, pvalue, params, defs);
    } else if (GEN_PARAMS.equals(propName)) {
      var params = new HashMap<String, Object>(cList.getGeneratorParams());
      addPropertyDefsForMap(propName, pvalue, params, defs);
    }
    return true;
  }

  @Override
  protected Object getPropertyValue(Object obj, String propName) throws PSNotFoundException {
    var cList = (IPSContentList) obj;
    switch (propName) {
      case DELIVERY_TYPE:
        var url = cList.getUrl();
        return PSUrlUtils.getUrlParameterValue(url, IPSHtmlParameters.SYS_DELIVERYTYPE);
      case EXPANDER_PARAMS:
        return cList.getExpanderParams();
      case GEN_PARAMS:
        return cList.getGeneratorParams();
      case FILTER:
        var srv = PSFilterServiceLocator.getFilterService();
        var filter = srv.findFilterByID(cList.getFilterId());
        return filter == null ? null : filter.getName();
      default:
        return super.getPropertyValue(obj, propName);
    }
  }

  /** Sets the {@link #DELIVERY_TYPE} property. */
  private void setDeliveryType(IPSContentList cList, Object value) throws PSNotFoundException {
    var deliveryName =
        Objects.requireNonNull(value, "deliveryType value must not be null").toString();
    var srv = PSPublisherServiceLocator.getPublisherService();
    srv.loadDeliveryType(deliveryName);
    var url =
        PSUrlUtils.replaceUrlParameterValue(
            cList.getUrl(), IPSHtmlParameters.SYS_DELIVERYTYPE, deliveryName);
    cList.setUrl(url);
  }

  /** Sets the {@link #EXPANDER_PARAMS} property. */
  private void setExpanderParams(IPSContentList cList, Object value) {
    if (!(value instanceof Map)) {
      throw new PSConfigException(
          "The type of property \"" + EXPANDER_PARAMS + "\" must be a Map.");
    }
    cList.setExpanderParams((Map<String, String>) value);
  }

  /** Sets the {@link #GEN_PARAMS} property. */
  private void setGeneratorParams(IPSContentList cList, Object value) {
    if (!(value instanceof Map)) {
      throw new PSConfigException("The type of property \"" + GEN_PARAMS + "\" must be a Map.");
    }
    cList.setGeneratorParams((Map<String, String>) value);
  }

  /** Sets the {@link #FILTER} property. */
  private void setFilter(IPSContentList cList, Object value) throws PSFilterException {
    var srv = PSFilterServiceLocator.getFilterService();
    var filter = srv.findFilterByName((String) value);
    if (filter == null) {
      throw new PSConfigException("Filter with name \"" + value + "\" does not exist.");
    }
    cList.setFilterId(filter.getGUID());
  }

  /** The property name for the expander parameters. */
  public static final String EXPANDER_PARAMS = "expanderParams";

  /** The property name for the generator parameters. */
  public static final String GEN_PARAMS = "generatorParams";

  /** The property name for the delivery type. */
  public static final String DELIVERY_TYPE = "deliveryType";

  /** The property name for the filter. */
  public static final String FILTER = "filter";
}
