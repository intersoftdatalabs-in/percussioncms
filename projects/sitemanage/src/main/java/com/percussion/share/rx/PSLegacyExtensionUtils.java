// REFACTORED: CP-JAVA11
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
package com.percussion.share.rx;

import static org.apache.commons.lang3.Validate.notNull;

import com.percussion.extension.IPSExtensionDef;
import com.percussion.server.IPSRequestContext;
import com.percussion.services.guidmgr.data.PSLegacyGuid;
import java.util.*;
import org.apache.commons.collections.CollectionUtils;

/**
 * Utility methods for working with legacy Percussion CM System extensions. Converts parameter
 * objects like {@link IPSRequestContext} into a {@link Map}. Once converted, you can use Apache
 * Commons MapUtils or BeanUtils.
 *
 * @author adamgent
 */
public class PSLegacyExtensionUtils {

  /**
   * Adds parameters specified by the extension def and contained in the parameters array to the
   * given map.
   *
   * @param paramMap never null
   * @param def never null
   * @param params never null
   */
  public static void addParameters(
      Map<String, String> paramMap, IPSExtensionDef def, Object[] params) {
    addParameters(paramMap, getParameterNames(def), params);
  }

  /** Returns a GUID string for the given id. If the id is already a GUID, returns it as-is. */
  public static String getGUID(String id) {
    if (id != null && id.contains("-")) {
      return id;
    }
    var guid = new PSLegacyGuid(Integer.parseInt(id), 1);
    return guid.toString();
  }

  /**
   * Returns a GUID string for the given id and revision. If the id is already a GUID, returns it
   * as-is.
   */
  public static String getGUID(String id, String revId) {
    if (id != null && id.contains("-")) {
      return id;
    }
    var guid = new PSLegacyGuid(Integer.parseInt(id), Integer.parseInt(revId));
    return guid.toString();
  }

  /**
   * Adds object parameters to a map.
   *
   * @param paramMap never null
   * @param parameterNames the keys of the map
   * @param parameters the values of the map
   */
  public static void addParameters(
      Map<String, String> paramMap, List<String> parameterNames, Object[] parameters) {
    notNull(paramMap, "paramMap");
    notNull(parameterNames, "parameterNames");
    notNull(parameters, "parameters");
    for (var i = 0; i < parameters.length; i++) {
      if (parameterNames.size() > i && parameters[i] != null) {
        paramMap.put(parameterNames.get(i), parameters[i].toString());
      }
    }
  }

  /**
   * Add parameters from request context.
   *
   * @param paramMap never null
   * @param request never null
   */
  public static void addParameters(Map<String, String> paramMap, IPSRequestContext request) {
    notNull(paramMap, "paramMap");
    notNull(request, "request");
    var iterator = request.getParametersIterator();
    while (iterator.hasNext()) {
      var entry = iterator.next();
      var key = entry.getKey();
      var value = request.getParameter(key);
      paramMap.put(key, value);
    }
  }

  /**
   * Get parameter names from the request.
   *
   * @param request never null
   * @return never null
   */
  public static List<String> getParameterNames(IPSRequestContext request) {
    notNull(request, "request");
    var iterator = request.getParametersIterator();
    var parameterNames = new ArrayList<String>();
    while (iterator.hasNext()) {
      var key = iterator.next().getKey();
      parameterNames.add(key);
    }
    return parameterNames;
  }

  /**
   * Gets the parameter names from an extension definition.
   *
   * @param extensionDef never null
   * @return never null
   */
  public static List<String> getParameterNames(IPSExtensionDef extensionDef) {
    notNull(extensionDef, "extensionDef");
    var rvalue = new ArrayList<String>();
    var it = extensionDef.getRuntimeParameterNames();
    CollectionUtils.addAll(rvalue, it);
    return rvalue;
  }
}
