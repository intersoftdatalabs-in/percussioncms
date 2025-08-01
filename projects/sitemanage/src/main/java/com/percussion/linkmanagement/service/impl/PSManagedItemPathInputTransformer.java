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
package com.percussion.linkmanagement.service.impl;

import com.percussion.data.PSConversionException;
import com.percussion.design.objectstore.PSLocator;
import com.percussion.extension.*;
import com.percussion.linkmanagement.service.IPSManagedLinkService;
import com.percussion.server.IPSRequestContext;
import com.percussion.services.guidmgr.PSGuidManagerLocator;
import com.percussion.share.spring.PSSpringWebApplicationContextUtils;
import com.percussion.util.IPSHtmlParameters;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.util.Optional;

/**
 * Field input transformer to process/update an item path.
 * Expects an item path as input, calls the managed link service to manage the link, and returns the resulting link id.
 * If no link is created, returns an empty string. Optionally takes a link id as a second input for validation.
 * For new items, the item id is not yet created, so PSManagedItemPathPreProcessor should be used to initialize new item links.
 * The managed link post processor checks a request private object and updates the parent id if needed.
 *
 * @author JaySeletz
 */
public class PSManagedItemPathInputTransformer extends PSDefaultExtension implements IPSFieldInputTransformer {

    private IPSManagedLinkService service;

    @Override
    public void init(IPSExtensionDef def, File codeRoot) throws PSExtensionException {
        super.init(def, codeRoot);
        PSSpringWebApplicationContextUtils.injectDependencies(this);
    }

    @Override
    public Object processUdf(Object[] params, IPSRequestContext request) throws PSConversionException {
        var ep = new PSExtensionParams(params);
        var path = ep.getStringParam(0, null, true);
        var linkId = ep.getStringParam(1, null, false);

        if (StringUtils.isBlank(path)) {
            return "";
        }

        var cid = request.getParameter(IPSHtmlParameters.SYS_CONTENTID);
        String result;
        if (StringUtils.isBlank(cid) || !StringUtils.isNumeric(cid)) {
            result = service.manageItemPath(null, path, linkId);
            request.setPrivateObject(PSManagedLinksPostProcessor.PERC_UPDATE_NEW_MANAGED_LINKS, true);
        } else {
            cid = PSGuidManagerLocator.getGuidMgr().makeGuid(new PSLocator(cid)).toString();
            result = service.manageItemPath(cid, path, linkId);
        }
        return Optional.ofNullable(result).orElse("");
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
