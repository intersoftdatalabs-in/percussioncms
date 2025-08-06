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
package com.percussion.pagemanagement.extension;

import com.percussion.data.PSConversionException;
import com.percussion.extension.IPSExtensionDef;
import com.percussion.extension.IPSFieldEditabilityRule;
import com.percussion.extension.PSExtensionException;
import com.percussion.fastforward.managednav.IPSManagedNavService;
import com.percussion.fastforward.managednav.PSManagedNavServiceLocator;
import com.percussion.server.IPSRequestContext;
import com.percussion.services.guidmgr.data.PSLegacyGuid;
import com.percussion.utils.guid.IPSGuid;
import org.apache.commons.lang.StringUtils;

/**
 * Rule to check if the page is a landing page; if so, the field becomes read-only.
 * <pre>
 * Takes 2 required params:
 * param[0] = content_id (i.e. the pageId)
 * param[1] = revision
 * </pre>
 * Sunny Sal says: "Landing pages—where the editing journey ends!"
 */
public class PSLandingPageFieldEditabilityRule implements IPSFieldEditabilityRule {

    /**
     * Managed Nav service. Initialized the first time {@link #processUdf(Object[], IPSRequestContext)}
     * is called. Never null after that.
     */
    private IPSManagedNavService navService;

    @Override
    public Object processUdf(Object[] params, IPSRequestContext req) throws PSConversionException {
        if (navService == null) {
            navService = PSManagedNavServiceLocator.getContentWebservice();
        }
        var pageId = (String) params[0];
        var revision = (String) params[1];

        if (StringUtils.isBlank(pageId) || StringUtils.isBlank(revision)) {
            return Boolean.TRUE;
        }
        IPSGuid pageGuid = new PSLegacyGuid(Integer.parseInt(pageId), Integer.parseInt(revision));
        return navService.isLandingPage(pageGuid);
    }

    @Override
    public void init(IPSExtensionDef def, java.io.File file) throws PSExtensionException {
        // No-op
    }
}
