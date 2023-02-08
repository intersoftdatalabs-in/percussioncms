/*
 * Copyright 1999-2023 Percussion Software, Inc.
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

import com.percussion.rest.contexts.Context;
import com.percussion.rest.contexts.IContextsAdaptor;
import com.percussion.rest.errors.BackendException;
import com.percussion.rest.locationscheme.LocationScheme;
import com.percussion.services.error.PSNotFoundException;
import com.percussion.services.guidmgr.data.PSGuid;
import com.percussion.services.sitemgr.IPSLocationScheme;
import com.percussion.services.sitemgr.IPSPublishingContext;
import com.percussion.services.sitemgr.IPSSiteManager;
import com.percussion.services.sitemgr.PSSiteManagerLocator;
import com.percussion.services.sitemgr.data.PSPublishingContext;
import com.percussion.util.PSSiteManageBean;
import org.apache.commons.lang.StringUtils;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

@PSSiteManageBean
public class ContextAdaptor implements IContextsAdaptor {

    private IPSSiteManager siteManager;

    public ContextAdaptor(){
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
            PSGuid guid = new PSGuid(id);
            IPSPublishingContext context = siteManager.loadContext(guid);
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
            PSGuid guid = new PSGuid(id);
            IPSPublishingContext context = siteManager.loadContext(guid);

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
            List<Context> ret = new ArrayList<>();
            List<IPSPublishingContext> contexts = siteManager.findAllContexts();

            for (IPSPublishingContext c : contexts) {
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
            IPSPublishingContext ctx = null;
            if (context.getId() == null || StringUtils.isBlank(context.getId().getStringValue())) {
                ctx = siteManager.createContext();
            } else {
                PSGuid guid = new PSGuid(context.getId().getStringValue());
                ctx = siteManager.loadContext(guid);
            }

            return copyContext(ctx);
        } catch (PSNotFoundException e) {
            throw new BackendException(e);
        }
    }

    private Context copyContext(IPSPublishingContext context) {
        Context ret = new Context();

        ret.setId(ApiUtils.convertGuid(context.getGUID()));
        IPSLocationScheme scheme = context.getDefaultScheme();
        if (scheme != null) {
            ret.setDefaultScheme(ApiUtils.copyLocationScheme(scheme));
            if(scheme.getDescription()!=null) {
                ret.setDescription(context.getDescription());
            }
            if(scheme.getName() != null) {
                ret.setName(scheme.getName());
            }
        }
        List<IPSLocationScheme> schemesByContextId = siteManager.findSchemesByContextId(context.getGUID());

        List<LocationScheme> schemes = new ArrayList<>();
        if (schemesByContextId != null) {
            for(IPSLocationScheme s : schemesByContextId){
                schemes.add(ApiUtils.copyLocationScheme(s));
            }
        }
        ret.setLocationSchemes(schemes);
        return ret;
    }

    private IPSPublishingContext copyContext(Context context) {
        IPSPublishingContext ret = new PSPublishingContext();

        PSGuid guid = new PSGuid(context.getId().getStringValue());
        ((PSPublishingContext) ret).setGUID(guid);
        ret.setName(context.getName());
        ret.setDescription(context.getDescription());

        PSGuid schemeGuid = new PSGuid(context.getDefaultScheme().getSchemeId().getStringValue());
        ret.setDefaultSchemeId(schemeGuid);


        //List<IPSLocationScheme> schemesByContextId = siteManager.findSchemesByContextId(context.getGUID());

//        List<LocationScheme> schemes = new ArrayList<LocationScheme>();
  //      for(IPSLocationScheme s : schemesByContextId){
    //        schemes.add(LocationSchemeAdaptor.copyLocationScheme(s));
      //  }

        return ret;
    }
}
