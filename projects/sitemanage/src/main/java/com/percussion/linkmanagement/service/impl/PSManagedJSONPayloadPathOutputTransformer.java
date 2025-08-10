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

package com.percussion.linkmanagement.service.impl;

import com.percussion.data.PSConversionException;
import com.percussion.extension.*;
import com.percussion.linkmanagement.service.IPSManagedLinkService;
import com.percussion.server.IPSRequestContext;
import com.percussion.share.spring.PSSpringWebApplicationContextUtils;
import com.percussion.system.utils.IPSHtmlParameters;
import java.io.File;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

/**
 * Field output transformer to update managed item paths in JSON payloads on edit. Calls the managed
 * link service to do the actual work.
 *
 * @author Nate Chadwick
 */
public class PSManagedJSONPayloadPathOutputTransformer extends PSDefaultExtension
    implements IPSFieldOutputTransformer {

  private static final Logger log =
      LogManager.getLogger(PSManagedJSONPayloadPathOutputTransformer.class);

  private IPSManagedLinkService service;

  @Override
  public Object processUdf(Object[] params, IPSRequestContext request)
      throws PSConversionException {
    var ep = new PSExtensionParams(params);
    var jsonPayload = ep.getStringParam(0, null, true);

    // Fix old data for Image Slider
    if (request != null && "percImageSlider.xml".equalsIgnoreCase(request.getRequestPage())) {
      if (jsonPayload != null) {
        jsonPayload =
            jsonPayload.replaceAll(
                IPSManagedLinkService.PERC_OLD_IMAGE_SLIDER_CONFIG_ATTR,
                IPSManagedLinkService.PERC_CONFIG);
        jsonPayload =
            jsonPayload.replaceAll(
                IPSManagedLinkService.PERC_OLD_IMAGE_SLIDER_IMAGEPATH_ATTR,
                IPSManagedLinkService.PERC_IMAGEPATH);
        log.info("Updated old data in ImageSlider");
      }
    }

    JSONObject object;
    var cid = request.getParameter(IPSHtmlParameters.SYS_CONTENTID);

    if (log.isDebugEnabled()) {
      log.debug("Processing with Content Id: {}", cid);
    }
    try {
      if (log.isDebugEnabled()) {
        log.debug("Parsing JSON Payload {}", jsonPayload);
      }

      if (StringUtils.isEmpty(jsonPayload)) {
        return "";
      }

      object = new JSONObject(jsonPayload);
      if (log.isDebugEnabled()) {
        log.debug("Done parsing payload, parsing {} array.", IPSManagedLinkService.PERC_CONFIG);
      }

      var objectArray = object.getJSONArray(IPSManagedLinkService.PERC_CONFIG);
      if (log.isDebugEnabled()) {
        log.debug("Done parsing payload array");
      }

      for (var i = 0; i < objectArray.length(); i++) {
        var entry = objectArray.getJSONObject(i);

        // Images
        if (entry.has(IPSManagedLinkService.PERC_IMAGEPATH)
            && entry.has(IPSManagedLinkService.PERC_IMAGEPATH_LINKID)) {
          if (!StringUtils.isBlank(entry.getString(IPSManagedLinkService.PERC_IMAGEPATH_LINKID))
              && (!StringUtils.isBlank(cid) || !StringUtils.isNumeric(cid))) {
            var newPath =
                renderItemPath(entry.getString(IPSManagedLinkService.PERC_IMAGEPATH_LINKID));
            entry.put(IPSManagedLinkService.PERC_IMAGEPATH, newPath);
            objectArray.put(i, entry);
          }
        }

        // Files
        if (entry.has(IPSManagedLinkService.PERC_FILEPATH)
            && entry.has(IPSManagedLinkService.PERC_FILEPATH_LINKID)) {
          if (!StringUtils.isBlank(entry.getString(IPSManagedLinkService.PERC_FILEPATH_LINKID))
              && (!StringUtils.isBlank(cid) || !StringUtils.isNumeric(cid))) {
            var newPath =
                renderItemPath(entry.getString(IPSManagedLinkService.PERC_FILEPATH_LINKID));
            entry.put(IPSManagedLinkService.PERC_FILEPATH, newPath);
            objectArray.put(i, entry);
          }
        }

        // Pages
        if (entry.has(IPSManagedLinkService.PERC_PAGEPATH)
            && entry.has(IPSManagedLinkService.PERC_PAGEPATH_LINKID)) {
          if (!StringUtils.isBlank(entry.getString(IPSManagedLinkService.PERC_PAGEPATH_LINKID))
              && (!StringUtils.isBlank(cid) || !StringUtils.isNumeric(cid))) {
            var newPath =
                renderItemPath(entry.getString(IPSManagedLinkService.PERC_PAGEPATH_LINKID));
            entry.put(IPSManagedLinkService.PERC_PAGEPATH, newPath);
            objectArray.put(i, entry);
          }
        }
      }

      if (log.isDebugEnabled()) {
        log.debug("Updating return payload.");
      }
      object.put(IPSManagedLinkService.PERC_CONFIG, objectArray);
      if (log.isDebugEnabled()) {
        log.debug("Done updating.");
      }
    } catch (JSONException ex) {
      log.error("An error occurred while trying to manage links in a JSONPayload field.");
      if (log.isDebugEnabled()) {
        log.debug("Error occurred.  Returning original payload: {}", jsonPayload, ex);
      }
      return jsonPayload;
    }

    if (log.isDebugEnabled()) {
      log.debug("Returning updated payload with any managed path updates: {}", object.toString());
    }
    return object.toString();
  }

  private String renderItemPath(String linkId) {
    return service.renderItemPath(null, linkId);
  }

  @Override
  public void init(IPSExtensionDef def, File codeRoot) throws PSExtensionException {
    super.init(def, codeRoot);
    PSSpringWebApplicationContextUtils.injectDependencies(this);
  }

  /**
   * Setter for dependency injection.
   *
   * @param service the service to set
   */
  public void setService(IPSManagedLinkService service) {
    this.service = service;
  }
}
