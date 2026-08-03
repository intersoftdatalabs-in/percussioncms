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

import com.percussion.rx.config.IPSConfigHandler;
import com.percussion.rx.config.IPSConfigHandler.ObjectState;
import com.percussion.rx.config.PSConfigException;
import com.percussion.rx.design.IPSAssociationSet;
import com.percussion.rx.design.IPSDesignModel;
import com.percussion.rx.design.PSDesignModelFactoryLocator;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.error.PSNotFoundException;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.utils.types.PSPair;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class is responsible to load the design objects for each handler and process the handler
 * which actually applies the configuration on to design objects and save the design objects.
 *
 * @author bjoginipally
 */
public class PSConfigMerger {

  /** Default constructor for use by Spring. */
  public PSConfigMerger() {}

  /**
   * Merges the configuration on the design objects and saves them. Gets the design model for each
   * handler. Loads the design object for each handler. Calls the process method on each handler
   * with the design object or <code>null</code>. (<code>null</code> in case if the handler has no
   * type enum and design object name). Saves the design objects. Throws run time exception in case
   * of error.
   *
   * @param cfgHandlers the configure handlers, never <code>null</code>.
   * @param hasPrevProps <code>true</code> if there are previous properties.
   * @param isApplyConfig <code>true</code> if applying the configuration or call {@link
   *     IPSConfigHandler#process(Object, ObjectState, List)} for each handler; otherwise
   *     de-configure or call {@link IPSConfigHandler#unprocess(Object, List)} for each handler.
   * @return a set of IDs of the configured design objects. It may be empty; but never <code>null
   *     </code>.
   */
  public PSPair<Collection<IPSGuid>, PSConfigException> merge(
      List<IPSConfigHandler> cfgHandlers, boolean hasPrevProps, boolean isApplyConfig)
      throws PSNotFoundException {
    if (cfgHandlers == null) throw new IllegalArgumentException("cfgHandlers must not be null");
    var processedGuids = new ArrayList<IPSGuid>();
    PSConfigException exceptionDuringSave = null;
    var dmFactory = PSDesignModelFactoryLocator.getDesignModelFactory();
    // Get the model and load the objects
    for (var handler : cfgHandlers) {
      var type = handler.getType();
      if (type == null) {
        m_handlerData.put(handler, new HandlerData(null, null, null));
        continue;
      }
      var model = dmFactory.getDesignModel(type);
      var objs = getDesignObjectsWithState(type, model, handler, hasPrevProps);
      if (objs.isEmpty()) {
        m_handlerData.put(handler, new HandlerData(null, null, null));
        continue;
      }
      m_handlerData.put(handler, new HandlerData(model, objs, model.getAssociationSets()));
    }

    // Process the handlers
    try {
      List<IPSAssociationSet> assocList;
      for (var handler : cfgHandlers) {
        var data = m_handlerData.get(handler);
        var model = data.mi_model;
        var objs = data.mi_designObjects;
        assocList = data.mi_associationSets;
        if (objs == null || objs.isEmpty()) {
          if (isApplyConfig) handler.process(null, null, assocList);
          else handler.unprocess(null, assocList);
        } else {
          for (var op : objs) {
            var o = op.getFirst();
            var state = op.getSecond();
            if (model != null) {
              boolean processed = false;
              if (isApplyConfig) processed = handler.process(o, state, assocList);
              else processed = handler.unprocess(o, assocList);
              if (processed) {
                var processedGuid = handler.saveResult(model, o, state, assocList);
                if (processedGuid != null) processedGuids.add(processedGuid);
              }
            }
          }
        }
      }
    } catch (PSConfigException e) {
      exceptionDuringSave = e;
    }
    return new PSPair<>(processedGuids, exceptionDuringSave);
  }

  /**
   * Creates a map of name of the property as key and its value as object, if there are any
   * exceptions getting the property values, then the exceptions are added to a collection. Note:
   * The handler processing stops as soon as it hits the error and skips processing rest of the
   * property setters and properties if any.
   *
   * @param cfgHandlers The config handlers with unresolved replacement names.
   * @return PSPair with the first object being Map of property names and values as Objects. The
   *     object could be a String or Map or List and the second object is list of exceptions.
   *     Neither the pair nor the parts are null. The objects of the pairs may be empty.
   */
  public PSPair<Map<String, Object>, List<Exception>> getPropertyDefs(
      List<IPSConfigHandler> cfgHandlers) {
    if (cfgHandlers == null) throw new IllegalArgumentException("cfgHandlers must not be null");
    var props = new HashMap<String, Object>();
    var cfgExceptions = new ArrayList<Exception>();
    var result = new PSPair(props, cfgExceptions);
    for (var handler : cfgHandlers) {
      try {
        var type = handler.getType();
        if (type == null) {
          props.putAll(handler.getPropertyDefs(null));
          continue;
        }
        var obj = getDesignObject(handler);
        props.putAll(handler.getPropertyDefs(obj));
      } catch (Exception e) {
        cfgExceptions.add(e);
      }
    }
    return result;
  }

  /**
   * Convenient method to get the design object from local storage if exists, otherwise loads from
   * the model and returns the object. Updates the local storage.
   *
   * @param type The type enum of the object assumed not <code>null</code>.
   * @param model The model of the object, assumed not <code>null</code>.
   * @param handler the handler that will be used to process the returned Design Objects, assumed
   *     not <code>null</code>.
   * @param hasPrevProps <code>true</code> if there are previous properties.
   * @return the objects, it never <code>null</code>, but may be empty if cannot find any objects
   *     for the given handler.
   */
  private List<PSPair<Object, ObjectState>> getDesignObjectsWithState(
      PSTypeEnum type, IPSDesignModel model, IPSConfigHandler handler, boolean hasPrevProps)
      throws PSNotFoundException {
    var typeMap = m_designObjects.computeIfAbsent(type, k -> new HashMap<>());
    if (handler.isGetDesignObjects()) return handler.getDesignObjects(typeMap);
    var objs = new ArrayList<PSPair<Object, ObjectState>>();
    var name = handler.getName();
    var obj = typeMap.get(name);
    if (obj == null) {
      obj = model.loadModifiable(name);
      typeMap.put(name, obj);
    }
    var state = hasPrevProps ? ObjectState.BOTH : ObjectState.CURRENT;
    objs.add(new PSPair<>(obj, state));
    return objs;
  }

  /**
   * Convenient method to get the design object from local storage if exists, otherwise loads from
   * the model and returns the object. Updates the local storage. If the handlers {@link
   * IPSConfigHandler#isGetDesignObjects()} is <code>true</code> then calls handlers {@link
   * IPSConfigHandler#getDefaultDesignObject(Map)} to get the design object and returns it.
   *
   * @param handler the handler from which the object is loaded.
   * @return the design object, never <code>null</code> throws {@link RuntimeException} if failed to
   *     find the model and the IPSDesignModelFactory throws {@link RuntimeException} if it fails to
   *     load the design object with the given name.
   */
  private Object getDesignObject(IPSConfigHandler handler) throws PSNotFoundException {
    var type = handler.getType();
    var dmFactory = PSDesignModelFactoryLocator.getDesignModelFactory();
    var model = dmFactory.getDesignModel(type);
    if (model == null) {
      throw new PSConfigException(
          "Failed to find the design model for the handler with type \"" + type + "\"");
    }
    var typeMap = m_designObjects.computeIfAbsent(type, k -> new HashMap<>());
    if (handler.isGetDesignObjects()) {
      return handler.getDefaultDesignObject(typeMap);
    }
    var name = handler.getName();
    var obj = typeMap.get(name);
    if (obj == null) {
      obj = model.loadModifiable(name);
      typeMap.put(name, obj);
    }
    return obj;
  }

  /**
   * Helper class to hold data for object handlers.
   *
   * @author bjoginipally
   */
  class HandlerData {
    HandlerData(
        IPSDesignModel model,
        List<PSPair<Object, ObjectState>> designObjects,
        List<IPSAssociationSet> associationSets) {
      mi_model = model;
      mi_designObjects = designObjects;
      mi_associationSets = associationSets;
    }

    IPSDesignModel mi_model;
    List<PSPair<Object, ObjectState>> mi_designObjects;
    List<IPSAssociationSet> mi_associationSets;
  }

  /** Handler data map */
  private Map<IPSConfigHandler, HandlerData> m_handlerData = new HashMap<>();

  /**
   * This is a map of type enum and a map of object name and actual object. If multiple handlers use
   * the same design object, we get the design object from this map.
   */
  private Map<PSTypeEnum, Map<String, Object>> m_designObjects = new HashMap<>();
}
