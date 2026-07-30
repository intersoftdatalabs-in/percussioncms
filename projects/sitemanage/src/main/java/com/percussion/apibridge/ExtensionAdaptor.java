/*
 * Copyright 1999-2026 Percussion Software, Inc.
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

package com.percussion.apibridge;

import com.percussion.extension.*;
import com.percussion.extensions.IPSExtensionService;
import com.percussion.rest.extensions.*;
import com.percussion.system.utils.PSSiteManageBean;
import java.net.URI;
import java.net.URL;
import java.util.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

/** Adaptor for managing Extensions in Percussion CMS. */
@PSSiteManageBean
public class ExtensionAdaptor implements IExtensionAdaptor {

  private final IPSExtensionService extensionService;
  private final Logger log = LogManager.getLogger(ExtensionAdaptor.class);

  @Autowired
  public ExtensionAdaptor(IPSExtensionService extensionService) {
    this.extensionService = extensionService;
  }

  private Extension copyExtensionRef(PSExtensionRef ref) {
    var ret = new Extension();
    ret.setCategory(ref.getCategory());
    ret.setContext(ref.getContext());
    ret.setExtensionName(ref.getExtensionName());
    ret.setHandlerName(ref.getHandlerName());

    try {
      var def = extensionService.getExtensionDef(ref);

      ret.setDeprecated(def.isDeprecated());
      ret.setJexlExtension(def.isJexlExtension());
      ret.setVersion(def.getVersion());

      // Copy interfaces
      var interfaces = new ArrayList<String>();
      def.getInterfaces().forEachRemaining(o -> interfaces.add((String) o));
      ret.setSupportedInterfaces(interfaces);

      // Init params
      var initParams = new HashMap<String, String>();
      def.getInitParameterNames()
          .forEachRemaining(name -> initParams.put(name, def.getInitParameter(name)));
      ret.setInitParameters(initParams);

      // Methods
      var methods = new HashMap<String, ExtensionMethod>();
      def.getMethods()
          .forEachRemaining(
              methodObj -> {
                var defMethod = (PSExtensionMethod) methodObj;
                var meth = new ExtensionMethod();
                meth.setName(defMethod.getName());
                meth.setDescription(defMethod.getDescription());

                var methParams = new ArrayList<ExtensionParameter>();
                defMethod
                    .getParameters()
                    .forEachRemaining(
                        paramObj -> {
                          var emp = (PSExtensionMethodParam) paramObj;
                          var ep = new ExtensionParameter();
                          ep.setDataType(emp.getType());
                          ep.setDescription(emp.getDescription());
                          ep.setName(emp.getName());
                          methParams.add(ep);
                        });
                meth.setParameters(methParams);
                methods.put(defMethod.getName(), meth);
              });
      ret.setMethods(methods);

      // Required applications
      var apps = new ArrayList<String>();
      def.getRequiredApplications().forEachRemaining(app -> apps.add(app.toString()));
      ret.setRequiredApplications(apps);

      // Resource locations
      var resources = new ArrayList<String>();
      def.getResourceLocations().forEachRemaining(res -> resources.add(((URL) res).toString()));
      ret.setResourceLocations(resources);

      // Supplied resources
      var supplied = new ArrayList<String>();
      def.getSuppliedResources().forEachRemaining(res -> supplied.add(((URL) res).toString()));
      ret.setSuppliedResources(supplied);

      // Runtime parameters
      var runParams = new ArrayList<ExtensionParameter>();
      def.getRuntimeParameterNames()
          .forEachRemaining(
              name -> {
                var runP = new ExtensionParameter();
                var defParam = def.getRuntimeParameter(name);
                runP.setName(defParam.getName());
                runP.setDescription(defParam.getDescription());
                runP.setDataType(defParam.getDataType());
                runParams.add(runP);
              });
      ret.setRuntimeParameters(runParams);

    } finally {
      return ret;
    }
  }

  /** Gets all extensions based on the specified ExtensionFilterOptions. */
  @Override
  public List<Extension> getExtensions(URI baseURI, ExtensionFilterOptions filter) {
    var response = new ArrayList<Extension>();
    try {
      var it =
          extensionService.getExtensionNames(
              ApiUtils.orNull(filter.getHandlerNamePattern()),
              ApiUtils.orNull(filter.getContext()),
              ApiUtils.orNull(filter.getInterfacePattern()),
              ApiUtils.orNull(filter.getExtensionNamePattern()));
      while (it.hasNext()) {
        var ref = (PSExtensionRef) it.next();
        response.add(copyExtensionRef(ref));
      }
    } catch (PSExtensionException e) {
      log.error("Error getting getExtensionNames", e);
    } finally {
      return response;
    }
  }
  @Override
  public List<Extension> listExtensions(URI baseURI) {
    return getExtensions(baseURI, new ExtensionFilterOptions());
  }

  @Override
  public Extension findExtensionByKey(URI baseURI, String idOrName) {
    if (!isSafeExtensionKey(idOrName)) {
      return null;
    }
    String key = idOrName.trim();
    List<Extension> all = listExtensions(baseURI);
    if (all == null) {
      return null;
    }
    for (Extension e : all) {
      if (e == null) {
        continue;
      }
      if (key.equalsIgnoreCase(e.getFqn()) || key.equalsIgnoreCase(e.getExtensionName())) {
        return e;
      }
    }
    return null;
  }

  /**
   * Allow FQN-style keys (may contain '/'). Reject traversal and backslash/null.
   */
  static boolean isSafeExtensionKey(String key) {
    if (key == null || key.isBlank()) {
      return false;
    }
    if (key.contains("..")) {
      return false;
    }
    return key.indexOf('\\') < 0 && key.indexOf('\0') < 0;
  }
}
