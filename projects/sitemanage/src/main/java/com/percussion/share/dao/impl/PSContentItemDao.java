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
package com.percussion.share.dao.impl;

import static com.percussion.share.dao.impl.PSLegacyExceptionUtils.convertException;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.Validate.isTrue;
import static org.apache.commons.lang3.Validate.notEmpty;
import static org.apache.commons.lang3.Validate.notNull;

import com.percussion.auditlog.PSActionOutcome;
import com.percussion.auditlog.PSAuditLogService;
import com.percussion.auditlog.PSContentEvent;
import com.percussion.cms.IPSConstants;
import com.percussion.cms.objectstore.IPSFieldValue;
import com.percussion.cms.objectstore.PSCoreItem;
import com.percussion.cms.objectstore.server.PSPurgableFileValue;
import com.percussion.design.objectstore.PSContentTypeHelper;
import com.percussion.design.objectstore.PSRelationshipConfig;
import com.percussion.services.legacy.IPSCmsObjectMgr;
import com.percussion.servlets.PSSecurityFilter;
import com.percussion.share.dao.IPSContentItemDao;
import com.percussion.share.dao.IPSFolderHelper;
import com.percussion.share.dao.IPSRelationshipCataloger;
import com.percussion.share.dao.PSJcrNodeMap;
import com.percussion.share.data.IPSItemSummary;
import com.percussion.share.data.PSItemSummaryUtils;
import com.percussion.share.service.IPSDataItemSummaryService;
import com.percussion.share.service.IPSIdMapper;
import com.percussion.share.service.exception.PSDataServiceException;
import com.percussion.system.utils.PSSiteManageBean;
import com.percussion.util.PSPurgableTempFile;
import com.percussion.utils.exceptions.PSORMException;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.webservices.PSWebserviceUtils;
import com.percussion.webservices.content.IPSContentDesignWs;
import com.percussion.webservices.content.IPSContentWs;
import com.percussion.webservices.system.IPSSystemWs;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import javax.jcr.Node;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.Errors;

/**
 * Manages R/W of the content item through a {@link PSJcrNodeMap}. Write operations are done using
 * {@link IPSContentWs}. Read operations are done using JCR repo interface.
 */
@PSSiteManageBean("contentItemDao")
@Transactional(noRollbackFor = Exception.class)
public class PSContentItemDao implements IPSContentItemDao {

  private IPSContentWs contentWs;
  private IPSContentDesignWs contentDesignWs;
  private IPSCmsObjectMgr cmsObjectMgr;
  private IPSDataItemSummaryService itemSummaryService;
  private IPSIdMapper idMapper;
  private IPSFolderHelper folderHelper;
  private IPSRelationshipCataloger relationshipHelper;
  private IPSSystemWs systemWs;
  private PSAuditLogService psAuditLogService = PSAuditLogService.getInstance();
  private PSContentEvent psContentEvent;

  @Autowired
  public PSContentItemDao(
      IPSContentDesignWs contentDesignWs,
      IPSContentWs contentWs,
      IPSIdMapper idMapper,
      @Qualifier("itemSummaryService") IPSDataItemSummaryService itemSummaryService,
      @Lazy IPSFolderHelper folderHelper,
      IPSCmsObjectMgr cmsObjectMgr,
      @Qualifier("relationshipCataloger") IPSRelationshipCataloger relationshipHelper,
      IPSSystemWs systemWs) {
    this.contentDesignWs = contentDesignWs;
    this.contentWs = contentWs;
    this.idMapper = idMapper;
    this.itemSummaryService = itemSummaryService;
    this.folderHelper = folderHelper;
    this.cmsObjectMgr = cmsObjectMgr;
    this.relationshipHelper = relationshipHelper;
    this.systemWs = systemWs;
  }

  @Override
  public Collection<Integer> findAllItemIdsByType(String name) throws PSDataServiceException {
    var nodes = PSContentTypeHelper.loadNodeDefs(name);
    if (nodes.isEmpty()) {
      return new ArrayList<>();
    }
    var ctypeId = nodes.get(0).getGUID();
    try {
      return cmsObjectMgr.findContentIdsByType(ctypeId.getUUID()).collect(Collectors.toList());
    } catch (PSORMException e) {
      throw new PSDataServiceException("failed to find item IDs by content type name: " + name, e);
    }
  }

  @Override
  public PSContentItem findItemByPath(String name, String folderPath)
      throws PSDataServiceException {
    notEmpty(name, "name");
    notEmpty(folderPath, "folderPath");
    try {
      var summary = folderHelper.findItem(folderHelper.concatPath(folderPath, name));
      return find(summary.getId());
    } catch (Exception e) {
      throw new PSDataServiceException("find item by path failed", convertException(e));
    }
  }

  @Override
  public IPSItemSummary addItemToPath(IPSItemSummary item, String folderPath)
      throws PSDataServiceException {
    try {
      folderHelper.addItem(folderPath, item.getId());
      return itemSummaryService.find(item.getId());
    } catch (Exception e) {
      throw new PSDataServiceException(
          "Trying to add item to the folder failed", convertException(e));
    }
  }

  @Override
  public void removeItemFromPath(IPSItemSummary item, String folderPath)
      throws PSDataServiceException {
    notNull(item, "item");
    notNull(folderPath, "folderPath");
    notEmpty(folderPath, "folderPath");
    try {
      folderHelper.removeItem(folderPath, item.getId(), false);
    } catch (Exception e) {
      throw new PSDataServiceException(
          "Trying to remove item from the folder failed", convertException(e));
    }
  }

  @Override
  public PSContentItem findItemByPath(String fullPath) throws PSDataServiceException {
    notNull(fullPath, "fullPath");
    try {
      var summary = folderHelper.findItem(fullPath);
      if (summary == null) {
        return null;
      }
      return find(summary.getId());
    } catch (Exception e) {
      throw new PSDataServiceException(convertException(e));
    }
  }

  @Override
  public void validateDelete(String id, Errors errors) {
    var guid = idMapper.getGuid(id);
    var compSumry = cmsObjectMgr.loadComponentSummary(guid.getUUID());
    var userName = "";
    if (compSumry != null) {
      var locator = compSumry.getEditLocator();
      if (locator.getRevision() != -1) {
        // Only current user can delete items s/he has checked out
        if (!PSWebserviceUtils.isItemCheckedOutToUser(compSumry)) {
          // Find who has it and report back in the exception
          userName = compSumry.getCheckoutUserName();
          errors.reject(
              "object.cannotDeleteInUse",
              new Object[0],
              "User: " + userName + " is editing the item. Failed to delete item.");
        }
      }
    }
  }

  @Override
  public void revisionControlOn(String id) throws LoadException {
    notEmpty(id);
    var guid = idMapper.getGuid(id);
    try {
      revisionControlOn(guid);
    } catch (PSORMException e) {
      throw new LoadException("Failed to turn revision control on for id: " + id, e);
    }
  }

  @Transactional
  public void revisionControlOn(IPSGuid guid) throws PSORMException {
    // If it's revisionable, update the component summary if needed.
    var locator = idMapper.getLocator(guid);
    var contentId = locator.getId();
    var sum = cmsObjectMgr.loadComponentSummary(contentId);
    if (!sum.isRevisionLock()) {
      log.debug("Turning revision lock on for item: {}", contentId);
      sum.setRevisionLock(true);
      cmsObjectMgr.saveComponentSummaries(singletonList(sum));
    }
  }

  // not part of the public interface - remove incorrect @Override
  public void delete(String id) throws com.percussion.share.dao.IPSGenericDao.DeleteException {
    notNull(id, "id");
    var guid = idMapper.getGuid(id);
    var uid = guid.toString();
    String path = "";
    String substring = "";
    try {
      try {
        path = folderHelper.findPaths(uid, PSRelationshipConfig.TYPE_RECYCLED_CONTENT).get(0);
      } catch (Exception e) {
        // Just catching exception in case path is not working
      }
      contentWs.deleteItems(asList(guid));
      substring = uid.substring(uid.lastIndexOf("-") + 1, id.length());
      psContentEvent =
          new PSContentEvent(
              id,
              substring,
              path,
              PSContentEvent.ContentEventActions.delete,
              PSSecurityFilter.getCurrentRequest().getServletRequest(),
              PSActionOutcome.SUCCESS);
      psAuditLogService.logContentEvent(psContentEvent);
    } catch (Exception e) {
      psContentEvent =
          new PSContentEvent(
              id,
              substring,
              path,
              PSContentEvent.ContentEventActions.delete,
              PSSecurityFilter.getCurrentRequest().getServletRequest(),
              PSActionOutcome.FAILURE);
      psAuditLogService.logContentEvent(psContentEvent);
      throw new DeleteException("Failed to delete content item: " + id, convertException(e));
    }
  }

  @Override
  public void remove(String id) throws PSDataServiceException {
    try {
      delete(id);
    } catch (Exception e) {
      throw new PSDataServiceException("Failed to remove content item: " + id, e);
    }
  }

  @Override
  public void remove(PSContentItem object) throws PSDataServiceException {
    if (object != null) {
      remove(object.getId());
    }
  }

  @Override
  public PSContentItem find(String id) throws com.percussion.share.dao.IPSGenericDao.LoadException {
    try {
      return find(id, false);
    } catch (PSDataServiceException e) {
      // wrap with message and cause
      throw new LoadException(e.getMessage(), e);
    }
  }

  @Override
  public PSContentItem find(String id, boolean isSummary) throws PSDataServiceException {
    notNull(id, "id");
    var itemSummary = itemSummaryService.find(id);
    if (itemSummary == null) return null;
    var guid = idMapper.getGuid(id);
    List<Node> nodes;
    try {
      nodes = contentDesignWs.findNodesByIds(asList(guid), isSummary);
    } catch (Exception e) {
      throw new LoadException("Error loading nodes for id " + id, convertException(e));
    }
    if (nodes.isEmpty()) {
      return null;
    }
    var node = nodes.get(0);
    var nodeMap = new PSJcrNodeMap(node, true);
    var item = new PSContentItem();
    PSItemSummaryUtils.copyProperties(itemSummary, item);
    item.setFields(nodeMap);
    if (log.isTraceEnabled()) {
      log.trace("Found item for id: {} item: {}", id, item);
    }
    return item;
  }

  @Override
  public List<PSContentItem> findAll() throws com.percussion.share.dao.IPSGenericDao.LoadException {
    throw new UnsupportedOperationException("findAll is not yet supported");
  }

  @Override
  public PSContentItem save(PSContentItem contentItem)
      throws com.percussion.share.dao.IPSGenericDao.SaveException, DeleteException {
    log.debug("Saving object: {}", contentItem);

    boolean isNew = false;
    String id = null;
    try {
      notNull(contentItem, "contentItem");
      PSCoreItem coreItem;
      if (contentItem.getId() == null) {
        isTrue(isNotBlank(contentItem.getType()), "Content type missing from: ", contentItem);
        coreItem = contentWs.createItems(contentItem.getType(), 1).get(0);
        isNew = true;
      } else {
        var guid = idMapper.getGuid(contentItem.getId());
        var realGuid = contentDesignWs.getItemGuid(guid);
        var items = contentWs.loadItems(asList(realGuid), true, false, false, true);
        notEmpty(items);
        coreItem = items.get(0);
      }

      for (Entry<String, Object> nvp : contentItem.getFields().entrySet()) {
        var f = coreItem.getFieldByName(nvp.getKey());
        var value = nvp.getValue();
        if (f != null) {
          // CMS-7974: For file type asset. The value if null was giving attachment not found
          // validation error.
          if (value == null) {
            if (f.getItemFieldMeta() != null && f.getItemFieldMeta().isBinary()) {
              value = f.getValue();
            } else {
              f.clearValues();
            }
          } else {
            IPSFieldValue fv;
            f.clearValues();
            if (value instanceof PSPurgableTempFile) {
              fv = new PSPurgableFileValue((PSPurgableTempFile) value);
              f.addValue(fv);
            } else if (value instanceof List) {

              var values = (List<String>) value;
              for (var val : values) {
                fv = f.createFieldValue(val);
                f.addValue(fv);
              }
            } else if (value instanceof Long) {
              fv = f.createFieldValue(Long.toString((Long) value));
              f.addValue(fv);
            } else if (value instanceof Integer) {
              fv = f.createFieldValue(Integer.toString((Integer) value));
              f.addValue(fv);
            } else {
              if (value != null) {
                // Coerce non-String (e.g. Optional) and skip blank / unparseable date noise
                // such as "Optional.empty" that would abort the entire save.
                String text = value instanceof String ? (String) value : String.valueOf(value);
                if (text.isBlank()
                    || "Optional.empty".equals(text)
                    || text.startsWith("Optional[")) {
                  f.clearValues();
                } else {
                  try {
                    fv = f.createFieldValue(text);
                    f.addValue(fv);
                  } catch (IllegalArgumentException badValue) {
                    f.clearValues();
                  }
                }
              } else {
                f.clearValues();
              }
            }
          }
        }
      }

      // get the folder id to enable asset renaming if required
      IPSGuid folderId = null;
      var paths = contentItem.getFolderPaths();
      if (paths != null && !paths.isEmpty()) {
        folderId = contentWs.getIdByPath(paths.get(0));
      }

      var guid = contentWs.saveItems(singletonList(coreItem), false, false, folderId).get(0);
      id = idMapper.getString(guid);

      // Turn on revisioning if needed.
      if (contentItem.isRevisionable()) {
        revisionControlOn(guid);
      }

      // add the item to the necessary folders
      if (paths != null) {
        for (var p : paths) {
          folderHelper.addItem(p, id);
        }
      }
      return find(id);
    } catch (Exception e) {
      if (e instanceof LoadException) {
        if (isNew && id != null) {
          // find may have failed due to insufficient memory, delete the newly created asset
          delete(id);
        }
      }
      throw new SaveException("Error saving object: " + contentItem, convertException(e));
    }
  }

  @Override
  public List<String> findOwners(String id, String name, String contentType, String slot) {
    return relationshipHelper.findOwners(id, name, contentType, slot);
  }

  /** The log instance to use for this class, never null. */
  private static final Logger log = LogManager.getLogger(IPSConstants.CONTENTREPOSITORY_LOG);
}
