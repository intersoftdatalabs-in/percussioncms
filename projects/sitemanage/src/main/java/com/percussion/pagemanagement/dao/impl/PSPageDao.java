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
package com.percussion.pagemanagement.dao.impl;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.isNumeric;
import static org.apache.commons.lang3.Validate.isTrue;
import static org.apache.commons.lang3.Validate.notNull;

import com.percussion.cms.objectstore.PSInvalidContentTypeException;
import com.percussion.cms.objectstore.server.PSItemDefManager;
import com.percussion.pagemanagement.dao.IPSPageDao;
import com.percussion.pagemanagement.dao.IPSWidgetItemIdGenerator;
import com.percussion.pagemanagement.data.PSPage;
import com.percussion.pagemanagement.data.PSPageSummary;
import com.percussion.pagemanagement.data.PSRegionBranches;
import com.percussion.pagemanagement.service.IPSPageService;
import com.percussion.pagemanagement.service.IPSPageService.PSPageException;
import com.percussion.security.error.PSExceptionUtils;
import com.percussion.services.contentmgr.IPSContentMgr;
import com.percussion.services.contentmgr.PSContentMgrLocator;
import com.percussion.services.notification.PSNotificationEvent;
import com.percussion.services.notification.PSNotificationEvent.EventType;
import com.percussion.services.notification.PSNotificationServiceLocator;
import com.percussion.share.dao.IPSContentItemDao;
import com.percussion.share.dao.PSJcrNodeFinder;
import com.percussion.share.dao.PSSerializerUtils;
import com.percussion.share.dao.impl.PSContentItem;
import com.percussion.share.service.IPSIdMapper;
import com.percussion.share.service.exception.PSDataServiceException;
import com.percussion.share.service.exception.PSPropertiesValidationException;
import com.percussion.share.service.exception.PSSpringValidationException;
import com.percussion.system.utils.IPSHtmlParameters;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * CRUDS page objects. Business logic such as validation is not done here.
 *
 * @author adamgent
 */
@Component("pageDao")
@Lazy
public class PSPageDao extends PSAbstractContentItemDao<PSPage> implements IPSPageDao {
  private static final Logger log = LogManager.getLogger(PSPageDao.class);

  private final IPSWidgetItemIdGenerator widgetItemIdGenerator;
  private final IPSIdMapper idMapper;
  private final PSJcrNodeFinder jcrNodeFinder;

  /** The cached page id, which is lazily loaded. */
  private Long pageContentTypeId;

  @Autowired
  public PSPageDao(
      IPSContentItemDao contentItemDao,
      IPSIdMapper idMapper,
      IPSWidgetItemIdGenerator widgetItemIdGenerator,
      IPSContentMgr contentMgr) {
    super(contentItemDao, idMapper);
    this.idMapper = idMapper;
    this.widgetItemIdGenerator = widgetItemIdGenerator;
    this.jcrNodeFinder =
        new PSJcrNodeFinder(contentMgr, IPSPageService.PAGE_CONTENT_TYPE, "sys_title");
  }

  @Override
  protected PSPage createObject() {
    return new PSPage();
  }

  @Override
  protected String getType() {
    return IPSPageService.PAGE_CONTENT_TYPE;
  }

  @Override
  protected PSPage getObjectFromContentItem(PSContentItem contentItem) {
    var page = super.getObjectFromContentItem(contentItem);
    page.setWorkflowId(
        Integer.parseInt((String) contentItem.getFields().get(IPSHtmlParameters.SYS_WORKFLOWID)));
    return page;
  }

  private void handleThumbnail(String id) {
    var notifyEvent = new PSNotificationEvent(EventType.PAGE_LOAD, id);
    var srv = PSNotificationServiceLocator.getNotificationService();
    srv.notifyEvent(notifyEvent);
  }

  @Override
  public PSPage findPageByPath(String fullFolderPath) throws PSPageException {
    try {
      var contentItem = getContentItemDao().findItemByPath(fullFolderPath);
      if (contentItem == null) {
        return null;
      }
      // Do not return content items that aren't Page's!
      if (contentItem.isPage()) {
        var page = getObjectFromContentItem(contentItem);
        handleThumbnail(page.getId());
        return page;
      }
      return null;
    } catch (Exception e) {
      throw new PSPageException(e.getMessage(), e);
    }
  }

  @Override
  public List<Integer> getPageIdsByFieldNameAndValue(String fieldName, String fieldValue)
      throws PSPageException {
    notNull(fieldName, "fieldName should not be null");
    notNull(fieldValue, "fieldValue should not be null");
    var pageTypeId = getPageContentTypeId();
    return PSContentMgrLocator.getContentMgr()
        .findItemsByLocalFieldValue(pageTypeId, fieldName, fieldValue);
  }

  @Override
  public PSPage findPage(String name, String folderPath) throws PSDataServiceException {
    var contentItem = getContentItemDao().findItemByPath(name, folderPath);
    if (contentItem == null || !contentItem.isPage()) {
      return null;
    }
    var page = find(contentItem.getId());
    handleThumbnail(page.getId());
    return page;
  }

  @Override
  public void delete(String id) throws PSDataServiceException {
    isTrue(isNotBlank(id), "id may not be blank");
    delete(id, false);
  }

  @Override
  public void delete(String id, boolean force) throws PSDataServiceException {
    isTrue(isNotBlank(id), "id may not be blank");
    var pve = new PSPropertiesValidationException(null, "delete");
    if (!force) {
      getContentItemDao().validateDelete(id, pve);
      try {
        pve.throwIfInvalid();
      } catch (PSSpringValidationException e) {
        throw new DeleteException(e.getMessage(), e);
      }
    }
    super.delete(id);
    var notifyEvent = new PSNotificationEvent(EventType.PAGE_DELETE, id);
    var srv = PSNotificationServiceLocator.getNotificationService();
    srv.notifyEvent(notifyEvent);
  }

  @Override
  public List<PSPage> findPagesBySiteAndTemplate(String path, String templateId)
      throws PSDataServiceException {
    isTrue(isNotBlank(templateId), "templateId may not be blank");
    var pages = new ArrayList<PSPage>();
    var whereFields = new HashMap<String, String>();
    whereFields.put("templateid", templateId);
    var nodes = jcrNodeFinder.find(path, whereFields);
    for (var node : nodes) {
      pages.add(find(idMapper.getString(node.getGuid())));
    }
    return pages;
  }

  @Override
  public List<PSPageSummary> findPagesBySiteAndWf(String path, int workflowId, int stateId)
      throws PSDataServiceException {
    isTrue(isNotBlank(path), "path may not be blank");
    var sums = new ArrayList<PSPageSummary>();
    var whereFields = new HashMap<String, String>();
    whereFields.put("sys_workflowid", String.valueOf(workflowId));
    if (stateId != -1) {
      whereFields.put("sys_contentstateid", String.valueOf(stateId));
    }
    var nodes = jcrNodeFinder.find(path, whereFields);
    for (var node : nodes) {
      sums.add(find(idMapper.getString(node.getGuid())));
      handleThumbnail(idMapper.getString(node.getGuid()));
    }
    return sums;
  }

  @Override
  protected void convertToObject(PSContentItem contentItem, PSPage page) {
    var f = contentItem.getFields();
    var name = (String) f.get("sys_title");
    var title = (String) f.get("page_title");
    var regionOverrides = (String) f.get("region_overrides");
    var linkTitle = (String) f.get("resource_link_title");
    var templateId = (String) f.get("templateid");
    var description = (String) f.get("page_description");
    var noindex = (String) f.get("page_noindex");
    var summary = (String) f.get("page_summary");
    var author = (String) f.get("page_authorname");
    List<String> tags = stringListField(f.get("page_tags"));
    var templateContentMigrationVersion = (String) f.get("template_content_migration_version");
    var migrationEmptyWidgetFlag = (String) f.get("migrationemptywidgets");

    // TODO: Adam serializer will probably break here.
    PSRegionBranches pageRegionBranches;
    if (isNotBlank(regionOverrides)) {
      pageRegionBranches = PSSerializerUtils.unmarshal(regionOverrides, PSRegionBranches.class);
    } else {
      pageRegionBranches = new PSRegionBranches();
    }

    page.setName(name);
    page.setTitle(title);
    page.setRegionBranches(pageRegionBranches);
    page.setTemplateId(templateId);
    page.setLinkTitle(linkTitle);
    page.setDescription(description);
    page.setNoindex(noindex);
    var folderPath = getFolderPath(contentItem);
    page.setFolderPath(folderPath);
    page.setSummary(summary);
    page.setAuthor(author);
    page.setTags(tags);
    PSHtmlMetadataUtils.fromMap(page, f);

    if (isNumeric(templateContentMigrationVersion)) {
      page.setTemplateContentMigrationVersion(templateContentMigrationVersion);
    }
    page.setMigrationEmptyWidgetFlag("yes".equalsIgnoreCase(migrationEmptyWidgetFlag));
  }

  protected String trimAt(final String stringForTrim, int length) {
    if (stringForTrim == null) {
      return null;
    }
    var out = stringForTrim.trim();
    if (length != -1) {
      out = StringUtils.substring(out, 0, length);
    }
    return out;
  }

  @Override
  protected void convertToItem(PSPage page, PSContentItem contentItem) {
    var f = contentItem.getFields();
    f.put("sys_title", trimAt(page.getName(), 255));
    f.put("page_title", trimAt(page.getTitle(), 255));
    f.put("resource_link_title", page.getLinkTitle());
    f.put("templateid", page.getTemplateId());
    f.put("page_noindex", page.getNoindex());
    f.put("page_description", page.getDescription());
    f.put("page_summary", page.getSummary());
    f.put("page_authorname", page.getAuthor());
    f.put("page_tags", page.getTags());
    f.put("template_content_migration_version", page.getTemplateContentMigrationVersion());
    f.put("migrationemptywidgets", page.isMigrationEmptyWidgetFlag() ? "yes" : "no");
    if (page.getWorkflowId() != null) {
      f.put(IPSHtmlParameters.SYS_WORKFLOWID, Integer.toString(page.getWorkflowId()));
    }
    if (page.getRegionBranches() != null) {
      widgetItemIdGenerator.generateIds(page.getRegionBranches());
      var pb = PSSerializerUtils.marshal(page.getRegionBranches());
      f.put("region_overrides", pb);
    }
    PSHtmlMetadataUtils.toMap(page, f);
  }

  @Override
  public List<PSPageSummary> findAllSummaries() {
    throw new UnsupportedOperationException("findAllSummaries is not yet supported");
  }

  @Override
  public PSPageSummary findSummary(@SuppressWarnings("unused") String id) {
    throw new UnsupportedOperationException("findSummary is not yet supported");
  }

  @Override
  public long getPageContentTypeId() throws PSPageException {
    if (pageContentTypeId == null) {
      try {
        var defMgr = PSItemDefManager.getInstance();
        pageContentTypeId = defMgr.contentTypeNameToId(IPSPageService.PAGE_CONTENT_TYPE);
      } catch (PSInvalidContentTypeException e) {
        log.error(PSExceptionUtils.getMessageForLog(e));
        log.debug(PSExceptionUtils.getDebugMessageForLog(e));
        throw new PSPageException(e.getMessage());
      }
    }
    return pageContentTypeId;
  }

  @Override
  public List<PSPage> findAllPagesBySite(String sitePath) throws PSDataServiceException {
    isTrue(isNotBlank(sitePath), "sitePath may not be blank");
    var pages = new ArrayList<PSPage>();
    var whereFields = new HashMap<String, String>();
    var nodes = jcrNodeFinder.find(sitePath, whereFields);
    for (var node : nodes) {
      pages.add(find(idMapper.getString(node.getGuid())));
    }
    return pages;
  }

  /** Convert a field map value that may be a list of strings (or null) without unchecked cast. */
  private static List<String> stringListField(Object value) {
    if (value == null) {
      return null;
    }
    if (value instanceof List<?> list) {
      var out = new ArrayList<String>(list.size());
      for (Object o : list) {
        out.add(o == null ? null : String.valueOf(o));
      }
      return out;
    }
    return List.of(String.valueOf(value));
  }
}
