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
import com.percussion.extension.*;
import com.percussion.linkmanagement.service.IPSManagedLinkService;
import com.percussion.server.IPSRequestContext;
import com.percussion.share.spring.PSSpringWebApplicationContextUtils;
import com.percussion.util.IPSHtmlParameters;
import org.apache.commons.lang.StringUtils;

import java.io.File;

/**
 * Field output transformer to update managed links on edit.
 * Calls the managed link service to do the actual work.
 * @author BJoginipally
 */
public class PSManagedLinkOutputTransformer extends PSDefaultExtension implements IPSFieldOutputTransformer {

    private IPSManagedLinkService service;

    @Override
    public Object processUdf(Object[] params, IPSRequestContext request) throws PSConversionException {
        var ep = new PSExtensionParams(params);
        var value = ep.getStringParam(0, null, true);
        if (StringUtils.isBlank(value)) {
            return value;
        }
        var cid = request.getParameter(IPSHtmlParameters.SYS_CONTENTID);
        if (StringUtils.isBlank(cid) || !StringUtils.isNumeric(cid)) {
            return value;
        }
        value = service.renderLinks(null, value, Integer.parseInt(cid));
        return value;
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
