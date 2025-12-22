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

// REFACTORED: CP-JAVA11
package com.percussion.searchmanagement.service.impl;

import static org.apache.commons.lang3.Validate.notNull;

import com.percussion.design.objectstore.PSLocator;
import com.percussion.itemmanagement.service.IPSWorkflowHelper;
import com.percussion.pagemanagement.dao.IPSPageDao;
import com.percussion.pagemanagement.dao.IPSPageDaoHelper;
import com.percussion.searchmanagement.service.IPSPageIndexService;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.error.PSNotFoundException;
import com.percussion.services.guidmgr.PSGuidUtils;
import com.percussion.share.service.IPSIdMapper;
import com.percussion.share.service.exception.PSValidationException;
import com.percussion.system.utils.PSSiteManageBean;
import java.util.HashSet;
import java.util.Set;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

/** Provides search indexing support for pages and templates. */
@PSSiteManageBean("pageIndexService")
public class PSPageIndexService implements IPSPageIndexService {
  private static final Logger log = LogManager.getLogger(PSPageIndexService.class);

  private final IPSIdMapper idMapper;
  private final IPSPageDao pageDao;
  private final IPSWorkflowHelper workflowHelper;
  private final IPSPageDaoHelper pageDaoHelper;
  private final PSIndexHelper indexHelper;

  @Autowired
  public PSPageIndexService(
      IPSIdMapper idMapper,
      IPSPageDao pageDao,
      IPSWorkflowHelper workflowHelper,
      IPSPageDaoHelper pageDaoHelper,
      PSIndexHelper indexHelper) {
    this.indexHelper = indexHelper;
    this.idMapper = idMapper;
    this.pageDao = pageDao;
    this.workflowHelper = workflowHelper;
    this.pageDaoHelper = pageDaoHelper;
  }

  @Override
  public void index(Set<Integer> ids) throws PSValidationException {
    notNull(ids);

    var locators = new HashSet<PSLocator>();

    for (var id : ids) {
      var guid = PSGuidUtils.makeGuid(id, PSTypeEnum.LEGACY_CONTENT);
      var guidStr = idMapper.getString(guid);
      try {
        if (workflowHelper.isPage(guidStr)) {
          locators.add(idMapper.getLocator(guidStr));
        }
      } catch (PSNotFoundException e) {
        log.error("Error indexing page with id: {}", id, e);
      }

      if (workflowHelper.isTemplate(guidStr)) {
        var pageIds = pageDaoHelper.findPageIdsByTemplate(guidStr);
        for (var pageId : pageIds) {
          locators.add(new PSLocator(pageId));
        }
      }
    }

    indexHelper.addItemsForIndex(locators);
  }
}
