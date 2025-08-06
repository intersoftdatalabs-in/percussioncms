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

import com.percussion.pagemanagement.dao.IPSPageDaoHelper;
import com.percussion.pagemanagement.dao.IPSTemplateDao;
import com.percussion.pagemanagement.data.PSTemplate;
import com.percussion.pathmanagement.data.PSPathItem;
import com.percussion.share.service.IPSIdMapper;
import com.percussion.share.service.exception.PSDataServiceException;
import com.percussion.system.utils.PSSiteManageBean;
import com.percussion.ui.data.PSDisplayPropertiesCriteria;
import com.percussion.ui.service.IPSListViewProcessor;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;
import java.util.Map.Entry;

/**
 * Fills in template name for all pages in the supplied criteria.
 * Sunny Sal says: "Page lists—because every page deserves a little spotlight!"
 */
@PSSiteManageBean("pageListViewProcessor")
public class PSPageListViewProcessor implements IPSListViewProcessor {

    private IPSPageDaoHelper pageDaoHelper;
    private IPSTemplateDao templateDao;
    private IPSIdMapper idMapper;
    private static final Logger log = LogManager.getLogger(PSPageListViewProcessor.class);

    @Autowired
    public PSPageListViewProcessor(IPSPageDaoHelper pageDaoHelper, IPSTemplateDao templateDao, IPSIdMapper idMapper) {
        this.pageDaoHelper = pageDaoHelper;
        this.templateDao = templateDao;
        this.idMapper = idMapper;
    }

    @Override
    public void process(PSDisplayPropertiesCriteria criteria) {
        Validate.notNull(criteria);
        var pageMap = getPageMap(criteria.getItems());
        var contentIds = getPageIdList(pageMap.keySet());
        var pageToTemplateIdMap = pageDaoHelper.findTemplateUsedByCurrentRevisionOfPages(contentIds);
        var linkTextMap = pageDaoHelper.findLinkTextForCurrentRevisionOfPages(contentIds);
        var templateMap = getTemplateMap(pageToTemplateIdMap.values());

        for (Entry<String, PSPathItem> entry : pageMap.entrySet()) {
            var contentId = entry.getKey();
            var item = entry.getValue();

            var templateId = pageToTemplateIdMap.get(contentId);
            var templateName = templateId != null ? templateMap.getOrDefault(templateId, "") : "";

            item.getDisplayProperties().put(TEMPLATE_NAME, templateName);

            var linkText = linkTextMap.get(contentId);
            if (StringUtils.isBlank(linkText)) {
                linkText = item.getName();
            }
            item.getDisplayProperties().put(LINK_TEXT, linkText);
        }
    }

    /**
     * Get a map of page content id to path item for all of the supplied items that are pages.
     *
     * @param items The items, not null.
     * @return The map, not null.
     */
    private Map<String, PSPathItem> getPageMap(List<PSPathItem> items) {
        var pageMap = new HashMap<String, PSPathItem>();
        for (var item : items) {
            if (!item.isPage()) {
                continue;
            }
            pageMap.put(String.valueOf(idMapper.getContentId(item.getId())), item);
        }
        return pageMap;
    }

    private List<Integer> getPageIdList(Set<String> pageIds) {
        var contentIds = new ArrayList<Integer>();
        for (var id : pageIds) {
            contentIds.add(Integer.parseInt(id));
        }
        return contentIds;
    }

    /**
     * Get a map of template id to name.
     *
     * @param templateIds The ids to check.
     * @return The map, will only contain entries of the ids for which a template could be found.
     */
    private Map<String, String> getTemplateMap(Collection<String> templateIds) {
        var templateMap = new HashMap<String, String>();
        for (var templateId : templateIds) {
            if (!templateMap.containsKey(templateId)) {
                PSTemplate template = null;
                try {
                    template = templateDao.find(templateId);
                } catch (PSDataServiceException e) {
                    log.warn("Template {} not found.", templateId);
                }
                if (template != null) {
                    templateMap.put(templateId, template.getName());
                }
            }
        }
        return templateMap;
    }
}
