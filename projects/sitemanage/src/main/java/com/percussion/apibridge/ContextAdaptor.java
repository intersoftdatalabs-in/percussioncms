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

package com.percussion.apibridge;

import com.percussion.rest.Guid;
import com.percussion.rest.contexts.Context;
import com.percussion.rest.contexts.IContextsAdaptor;
import com.percussion.rest.errors.BackendException;
import com.percussion.rest.locationscheme.LocationScheme;
import com.percussion.services.error.PSNotFoundException;
import com.percussion.services.guidmgr.data.PSGuid;
import com.percussion.services.sitemgr.IPSPublishingContext;
import com.percussion.services.sitemgr.IPSSiteManager;
import com.percussion.services.sitemgr.PSSiteManagerLocator;
import com.percussion.services.sitemgr.data.PSPublishingContext;
import com.percussion.system.utils.PSSiteManageBean;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;

@PSSiteManageBean
public class ContextAdaptor implements IContextsAdaptor {

  private IPSSiteManager siteManager;

  public ContextAdaptor() {
    siteManager = PSSiteManagerLocator.getSiteManager();
  }

  /***
   * Delete a publishing Context by id
   * @param baseURI referring url
   * @param id id of the Context to delete
   */
  @Override
  public void deleteContext(URI baseURI, String id) throws BackendException {
    try {
      var guid = new PSGuid(id);
      var context = siteManager.loadContext(guid);
      siteManager.deleteContext(context);
    } catch (PSNotFoundException e) {
      throw new BackendException(e);
    }
  }

  /***
   * Get a publishing context by it's ID
   * @param baseUri referring uri
   * @param id id of the context to lookup
   * @return The publishing Conext
   */
  @Override
  public Context getContextById(URI baseUri, String id) throws BackendException {
    try {
      var guid = new PSGuid(id);
      var context = siteManager.loadContext(guid);
      return copyContext(context);
    } catch (PSNotFoundException e) {
      throw new BackendException(e);
    }
  }

  /***
   * List all publishing contexts configured on the system
   * @param baseURI
   * @return a list of publishing contexts
   */
  @Override
  public List<Context> listContexts(URI baseURI) throws BackendException {
    try {
      var ret = new ArrayList<Context>();
      var contexts = siteManager.findAllContexts();
      for (var c : contexts) {
        ret.add(copyContext(c));
      }
      return ret;
    } catch (PSNotFoundException e) {
      throw new BackendException(e);
    }
  }

  /***
   * Create or update a publishing context
   * @param baseURI referring url
   * @param context a fully initialized Context
   * @return The updated context
   */
  @Override
  public Context createOrUpdateContext(URI baseURI, Context context) throws BackendException {
    try {
      IPSPublishingContext ctx;
      Guid idGuid = context.getId();
      String idStr = (idGuid == null) ? null : idGuid.getStringValue();
      if (idStr == null || StringUtils.isBlank(idStr)) {
        ctx = siteManager.createContext();
      } else {
        var guid = new PSGuid(idStr);
        ctx = siteManager.loadContext(guid);
      }
      return copyContext(ctx);
    } catch (PSNotFoundException e) {
      throw new BackendException(e);
    }
  }

  private Context copyContext(IPSPublishingContext context) {
    var ret = new Context();
    ret.setId(ApiUtils.convertGuid(context.getGUID()));
    var scheme = context.getDefaultScheme();
    if (scheme != null) {
      ret.setDefaultScheme(ApiUtils.copyLocationScheme(scheme));
      if (scheme.getDescription() != null) {
        ret.setDescription(context.getDescription());
      }
      if (scheme.getName() != null) {
        ret.setName(scheme.getName());
      }
    }
    var schemesByContextId = siteManager.findSchemesByContextId(context.getGUID());
    var schemes = new ArrayList<LocationScheme>();
    if (schemesByContextId != null) {
      for (var s : schemesByContextId) {
        schemes.add(ApiUtils.copyLocationScheme(s));
      }
    }
    ret.setLocationSchemes(schemes);
    return ret;
  }

  private IPSPublishingContext copyContext(Context context) {
    var ret = new PSPublishingContext();
    Guid idGuid = context.getId();
    if (idGuid != null) {
      String idStr = idGuid.getStringValue();
      if (idStr != null) {
        var guid = new PSGuid(idStr);
        ret.setGUID(guid);
      }
    }
    ret.setName(context.getName());
    ret.setDescription(context.getDescription());
    Guid schemeId = null;
    LocationScheme defaultScheme = context.getDefaultScheme();
    if (defaultScheme != null) {
      schemeId = defaultScheme.getSchemeId();
    }
    if (schemeId != null) {
      String schemeStr = schemeId.getStringValue();
      if (schemeStr != null) {
        ret.setDefaultSchemeId(new PSGuid(schemeStr));
      }
    }
    return ret;
  }
}
