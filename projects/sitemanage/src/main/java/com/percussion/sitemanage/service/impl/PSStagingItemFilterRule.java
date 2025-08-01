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
package com.percussion.sitemanage.service.impl;

import com.percussion.services.contentchange.IPSContentChangeService;
import com.percussion.services.contentchange.PSContentChangeServiceLocator;
import com.percussion.services.contentchange.data.PSContentChangeType;
import com.percussion.services.filter.IPSFilterItem;
import com.percussion.services.filter.IPSItemFilterRule;
import com.percussion.services.pubserver.data.PSPubServer;
import com.percussion.util.IPSHtmlParameters;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;

/**
 * This is a staging filter rule that extends public asset item filter rule. This rule while getting the
 * workflow items passes true for staging.
 */
public class PSStagingItemFilterRule extends PSPublicAssetItemFilterRule implements IPSItemFilterRule
{
    
    @Override
    public List<IPSFilterItem> filter(List<IPSFilterItem> items, Map<String, String> params)
    {
        var isPublish = !"unpublish".equals(params.get(IPSHtmlParameters.SYS_PUBLISH));
        var pubServer = findPubServer(params.get(IPSHtmlParameters.SYS_EDITIONID));
        var ignoreAssets = pubServer == null ? "false" : pubServer.getProperty(PUBLISH_IGNORE_UNMODIFIED_ASSETS_PROPERTY).getValue();
        var ignoreUnModAssets = StringUtils.equals(ignoreAssets, "true");
        var serverId = pubServer == null ? null : pubServer.getServerId();
        var worker = getWorker(params);
        var rvalue = new ArrayList<IPSFilterItem>();
        var contentChangeService = PSContentChangeServiceLocator.getContentChangeService();
        List<Integer> changedIds = null;
        Set<Integer> changedIdsSet = null;
        if (ignoreUnModAssets) {
            if (contentChangeService != null) {
                changedIds = contentChangeService.getChangedContent(pubServer.getSiteId(), PSContentChangeType.PENDING_STAGED);
                changedIdsSet = new HashSet<>(changedIds);
            }
        }
        for (var item : items) {
            var wfItem = worker.getWorkflowItem(item.getItemId(), true);
            var r = process(worker, item, wfItem, isPublish, ignoreUnModAssets, serverId, changedIdsSet);
            if (r != null)
                rvalue.add(r);
        }
        return rvalue;
    }
}
