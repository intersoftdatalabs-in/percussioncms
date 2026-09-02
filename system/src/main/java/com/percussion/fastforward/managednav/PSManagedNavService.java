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
package com.percussion.fastforward.managednav;

import static com.percussion.fastforward.managednav.PSNavFolderUtils.SYS_WORKFLOWID;
import static com.percussion.fastforward.managednav.PSNavFolderUtils.addNavonSubmenu;
import static com.percussion.fastforward.managednav.PSNavFolderUtils.addNavonToChildFolder;
import static com.percussion.fastforward.managednav.PSNavFolderUtils.findChildNavonLocator;
import static com.percussion.fastforward.managednav.PSNavFolderUtils.getChildNavonSummary;
import static com.percussion.fastforward.managednav.PSNavFolderUtils.getParentFolder;
import static com.percussion.fastforward.managednav.PSNavFolderUtils.removeNavonParents;
import static java.util.Arrays.asList;
import static org.apache.commons.lang3.Validate.notEmpty;
import static org.apache.commons.lang3.Validate.notNull;

import com.intsof.percussioncms.auditlog.codes.NavigationErrorCodes;
import com.percussion.cms.IPSConstants;
import com.percussion.cms.PSCmsException;
import com.percussion.cms.objectstore.PSAaRelationship;
import com.percussion.cms.objectstore.PSNavNameAliases;
import com.percussion.cms.objectstore.PSComponentSummary;
import com.percussion.cms.objectstore.PSCoreItem;
import com.percussion.cms.objectstore.PSDateValue;
import com.percussion.cms.objectstore.PSInvalidContentTypeException;
import com.percussion.cms.objectstore.PSItemField;
import com.percussion.cms.objectstore.PSRelationshipFilter;
import com.percussion.cms.objectstore.server.PSItemDefManager;
import com.percussion.cms.objectstore.server.PSRelationshipProcessor;
import com.percussion.design.objectstore.PSLocator;
import com.percussion.design.objectstore.PSRelationship;
import com.percussion.design.objectstore.PSRelationshipConfig;
import com.percussion.security.error.PSExceptionUtils;
import com.percussion.server.IPSRequestContext;
import com.percussion.system.utils.IPSHtmlParameters;
import com.percussion.server.PSRequest;
import com.percussion.server.PSRequestContext;
import com.percussion.server.cache.IPSFolderRelationshipCache;
import com.percussion.server.cache.PSFolderRelationshipCache;
import com.percussion.server.webservices.PSServerFolderProcessor;
import com.percussion.services.assembly.IPSAssemblyService;
import com.percussion.services.assembly.IPSTemplateSlot;
import com.percussion.services.assembly.PSAssemblyException;
import com.percussion.services.assembly.impl.nav.PSNavConfig;
import com.percussion.services.content.data.PSItemStatus;
import com.percussion.services.guidmgr.IPSGuidManager;
import com.percussion.services.guidmgr.data.PSLegacyGuid;
import com.percussion.services.legacy.IPSCmsObjectMgr;
import com.percussion.services.legacy.IPSItemEntry;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.utils.request.PSRequestInfoBase;
import com.percussion.webservices.PSErrorException;
import com.percussion.webservices.PSErrorResultsException;
import com.percussion.webservices.PSWebserviceUtils;
import com.percussion.webservices.content.IPSContentDesignWs;
import com.percussion.webservices.content.IPSContentWs;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import javax.jcr.Node;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Implements {@link IPSManagedNavService}.
 *
 * @author YuBingChen
 */
@Component("sys_managedNavService")
public class PSManagedNavService implements IPSManagedNavService {
  /**
   * Constructs the service from the related services.
   *
   * @param contentWs the content service, not <code>null</code>.
   * @param contentDsWs the content design service, not <code>null</code>.
   * @param asmService the assembly service, not <code>null</code>.
   * @param guidMgr the guid manager, not <code>null</code>.
   */
  @Autowired
  public PSManagedNavService(
      IPSContentWs contentWs,
      IPSContentDesignWs contentDsWs,
      IPSAssemblyService asmService,
      IPSGuidManager guidMgr,
      IPSCmsObjectMgr cmsMgr) {
    this.contentWs = contentWs;
    this.contentDsWs = contentDsWs;
    this.asmService = asmService;
    this.guidMgr = guidMgr;
    this.cmsMgr = cmsMgr;
  }

  /*
   * //see base interface method for details
   */
  public List<Long> getNavTreeContentTypeIds() {
    List<Long> ret = new ArrayList<>();

    for (IPSGuid g : PSNavConfig.getInstance().getNavTreeTypes()) {
      ret.add(g.longValue());
    }
    return ret;
  }

  /*
   * //see base interface method for details
   */
  public List<String> getNavTreeContentTypeNames() {
    return getNavConfig().getNavTreeContentTypeNames();
  }

  /*
   * //see base interface method for details
   */
  public List<Long> getNavonContentTypeIds() {
    List<Long> ret = new ArrayList<>();

    for (IPSGuid g : PSNavConfig.getInstance().getNavonTypes()) {
      ret.add(g.longValue());
    }
    return ret;
  }

  /*
   * //see base interface method for details
   */
  public List<String> getNavonContentTypeNames() {
    return getNavConfig().getNavonContentTypeNames();
  }

  /*
   * //see base interface method for details
   */
  public IPSGuid addNavonToFolder(
      IPSGuid parentFolderId, IPSGuid childFolderId, String navonName, String navonTitle) {
    return addNavonToFolder(parentFolderId, childFolderId, navonName, navonTitle, -1);
  }

  public IPSGuid addNavonToFolder(
      IPSGuid parentFolderId,
      IPSGuid childFolderId,
      String navonName,
      String navonTitle,
      int workflowId) {
    notNull(parentFolderId);
    notNull(childFolderId);
    notNull(navonName);
    notEmpty(navonName);
    notNull(navonTitle);
    notEmpty(navonTitle);

    IPSRequestContext req = getRequestCtx();
    PSLocator parentLoc = ((PSLegacyGuid) parentFolderId).getLocator();
    PSLocator childLoc = ((PSLegacyGuid) childFolderId).getLocator();
    Long slotUuid = getMenuSlotId();
    Object curWfId = null;
    if (workflowId != -1) {
      curWfId = req.getPrivateObject(SYS_WORKFLOWID);
      req.setPrivateObject(SYS_WORKFLOWID, workflowId);
    }
    // Use DUMMY_TEMPLATEID as the template ID since the template in the
    // AA link is not used to render the navigation node (title).
    // Should not use -1L because it will be treated as NULL value when it is
    // retrieved by PSRelationshipService
    PSComponentSummary navon =
        addNavonToChildFolder(
            req, parentLoc, childLoc, navonName, navonTitle, slotUuid, DUMMY_TEMPLATEID);

    if (curWfId != null) req.setParameter(SYS_WORKFLOWID, curWfId);

    if (navon == null) return null;

    return new PSLegacyGuid(navon.getCurrentLocator());
  }

  /**
   * Gets the folder that is related to the specified navigation node.
   *
   * @param navonId the ID of the navigation node, assumed not <code>null</code>.
   * @return the summary of the related folder, never <code>null</code>.
   */
  private PSComponentSummary getNavonFolder(IPSGuid navonId) {
    PSComponentSummary navon = cmsMgr.loadComponentSummary(((PSLegacyGuid) navonId).getContentId());
    PSComponentSummary folder = getParentFolder(getRequestCtx(), navon);
    if (folder == null) {
      throw new PSNavException(
          NavigationErrorCodes.NAVIGATION_SERVICE_CANT_FIND_RELATED_FOLDER_FOR_NAVON, navonId);
    }

    return folder;
  }

  /*
   * //see base interface method for details
   */
  public void moveNavon(IPSGuid srcId, IPSGuid srcParentId, IPSGuid targetId, int index) {
    notNull(srcId);
    notNull(targetId);

    // Same-parent sibling reorder must not checkout the parent navon.
    // prepareForEdit + releaseFromEdit on sample rffNavTree NPEs (CONTENTSTATEID
    // 0) or applies SYS_SORTRANK on a checked-out revision that check-in
    // discards, so Move up/down appears to succeed but GET order is unchanged
    // (#3797). Also never fall through to moveNavonAndFolder for same parent
    // (folder-move onto self is a silent no-op).
    if (sameNavonContentId(srcParentId, targetId)) {
      rearrangeSameParentChild(srcId, targetId, index);
      return;
    }

    List<PSItemStatus> statuses = null;
    try {
      statuses = contentWs.prepareForEdit(Collections.singletonList(targetId));
      PSComponentSummary sum =
          cmsMgr.loadComponentSummary(((PSLegacyGuid) targetId).getContentId());
      targetId = new PSLegacyGuid(sum.getHeadLocator());
      PSAaRelationship rel = getChildNavonRelationship(srcId, targetId);
      if (rel != null) {
        List<IPSGuid> targetChildList = findChildNavonIds(targetId);
        boolean duplicateFound = false;

        for (IPSGuid id : targetChildList) {
          if (sameNavonContentId(srcId, id)) {
            duplicateFound = true;
            break;
          }
        }
        if (!duplicateFound) {
          contentWs.reArrangeContentRelations(Collections.singletonList(rel), index);
        }
      } else {
        moveNavonAndFolder(
            (PSLegacyGuid) srcId, (PSLegacyGuid) srcParentId, (PSLegacyGuid) targetId, index);
      }
    } catch (PSErrorResultsException e) {
      PSNavException ne =
          new PSNavException(
              NavigationErrorCodes.NAVIGATION_SERVICE_FAILED_TO_MOVE_SOURCE_NAVON_TO_TARGET,
              new Object[] {srcId, targetId},
              e);
      log.error(PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
      throw (ne);
    } finally {
      if (statuses != null) contentWs.releaseFromEdit(statuses, false);
    }
  }

  /**
   * True when both GUIDs identify the same navon, ignoring revision. {@code
   * PSLegacyGuid.toString()} includes revision so sibling move (same parent
   * id string from the tree vs head locator) used to look like a reparent
   * (#3797).
   */
  static boolean sameNavonContentId(IPSGuid left, IPSGuid right) {
    if (left == null || right == null) {
      return false;
    }
    if (left.equals(right)) {
      return true;
    }
    if (left instanceof PSLegacyGuid && right instanceof PSLegacyGuid) {
      return ((PSLegacyGuid) left).getContentId() == ((PSLegacyGuid) right).getContentId();
    }
    return left.toString().equalsIgnoreCase(right.toString());
  }

  /**
   * Reorder {@code srcId} under {@code parentId} without checkout. {@code
   * reArrangeContentRelations} requires the parent navon checked out and
   * sample rffNavTree prepare NPEs (#3797). Persist {@code sys_sortrank} on
   * the AA relationships directly so GET/reload sees the new sibling order.
   */
  void rearrangeSameParentChild(IPSGuid srcId, IPSGuid parentId, int index) {
    IPSGuid head = parentId;
    if (parentId instanceof PSLegacyGuid && cmsMgr != null) {
      try {
        PSComponentSummary sum =
            cmsMgr.loadComponentSummary(((PSLegacyGuid) parentId).getContentId());
        if (sum != null && sum.getHeadLocator() != null) {
          head = new PSLegacyGuid(sum.getHeadLocator());
        }
      } catch (RuntimeException e) {
        log.warn("Could not resolve head locator for navon reorder; id={}", parentId, e);
      }
    }
    List<PSAaRelationship> existing = loadMenuSlotRelationships(head);
    if ((existing == null || existing.isEmpty()) && !head.equals(parentId)) {
      existing = loadMenuSlotRelationships(parentId);
    }
    if (existing == null || existing.isEmpty()) {
      log.warn("No child navon relationship to reorder; src={} parent={}", srcId, parentId);
      return;
    }
    int dependentId = ((PSLegacyGuid) srcId).getContentId();
    PSAaRelationship moving = null;
    List<PSAaRelationship> others = new ArrayList<>();
    for (PSAaRelationship rel : existing) {
      if (rel.getDependent().getId() == dependentId) {
        moving = rel;
      } else {
        others.add(rel);
      }
    }
    if (moving == null) {
      log.warn("No child navon relationship to reorder; src={} parent={}", srcId, parentId);
      return;
    }
    int insertAt = index;
    if (insertAt < 0 || insertAt > others.size()) {
      insertAt = others.size();
    }
    others.add(insertAt, moving);
    for (int i = 0; i < others.size(); i++) {
      others.get(i).setProperty(IPSHtmlParameters.SYS_SORTRANK, String.valueOf(i));
    }
    PSWebserviceUtils.saveAaRelationships(others);
  }

  private List<PSAaRelationship> loadMenuSlotRelationships(IPSGuid ownerId) {
    try {
      return contentWs.loadSlotContentRelationships(ownerId, getMenuSlot().getGUID());
    } catch (PSErrorException e) {
      throw new PSNavException("Failed to load menu-slot relationships for navon " + ownerId, e);
    }
  }

  /**
   * Gets the relationship where the owner is the target node, dependent is the source node and the
   * slot property is the menu slot.
   *
   * @param srcId the ID of the source node, assumed not <code>null</code>.
   * @param targetId the ID of the target node, assumed not <code>null</code>.
   * @return the relationship if exist; otherwise return <code>null</code> if the source is not a
   *     child of the target node.
   */
  private PSAaRelationship getChildNavonRelationship(IPSGuid srcId, IPSGuid targetId) {
    List<PSAaRelationship> rels =
        contentWs.loadSlotContentRelationships(targetId, getMenuSlot().getGUID());
    int dependentId = ((PSLegacyGuid) srcId).getContentId();
    for (PSAaRelationship r : rels) {
      if (r.getDependent().getId() == dependentId) {
        return r;
      }
    }
    return null;
  }

  /**
   * Moves the source navigation node (and its related folder) to the target navigation node (and
   * its related folder).
   *
   * @param srcId the ID of the source navigation node, assumed not <code>null</code>.
   * @param srcParentId the parent ID of the source navigation node. It may be <code>null</code>.
   * @param targetId the ID of the target navigation node, assumed not <code>null</code>.
   * @param index the target location of the source node. It is <code>0</code> based, <code>-1
   *     </code> to append at the end of the target node.
   */
  private void moveNavonAndFolder(
      PSLegacyGuid srcId, PSLegacyGuid srcParentId, PSLegacyGuid targetId, int index) {
    try {
      PSComponentSummary srcFolder = getNavonFolder(srcId);
      PSComponentSummary targetFolder = getNavonFolder(targetId);

      validateMoveRequest(srcFolder, targetFolder);

      PSLocator targetNavon = targetId.getLocator();
      PSLocator srcNavon = srcId.getLocator();

      // move folder first
      IPSGuid srcFolderId = new PSLegacyGuid(srcFolder.getHeadLocator());
      IPSGuid tgtFolderId = new PSLegacyGuid(targetFolder.getHeadLocator());
      PSLocator srcParentLocator = getRelationshipParentFolder(srcFolderId.getUUID());
      IPSGuid srcParent = new PSLegacyGuid(srcParentLocator);
      contentWs.moveFolderChildren(srcParent, tgtFolderId, Collections.singletonList(srcFolderId));

      PSLocator parentLoc = srcParentId == null ? null : srcParentId.getLocator();

      // handle the navigation node
      removeNavonParents(getRequestCtx(), srcNavon, parentLoc);

      // convert index to 1 based number
      index = (index == -1) ? index : index + 1;
      addNavonSubmenu(
          getRequestCtx(), targetNavon, srcNavon, getMenuSlotId(), DUMMY_TEMPLATEID, index);
    } catch (Exception ex) {
      PSNavException ne =
          new PSNavException(
              NavigationErrorCodes.NAVIGATION_SERVICE_ERROR_ADDING_NAVTREE_TO_FOLDER, ex);
      if (ex instanceof PSNavException) {
        ne = (PSNavException) ex;
      } else if (ex instanceof PSErrorException) {
        ne = new PSNavException(ex);
      }
      log.error(PSExceptionUtils.getMessageForLog(ne));
      log.debug(ex);
      throw ne;
    }
  }

  /**
   * This API should return 1 live parent, if returns null, that is a problem with corrupted data
   * and needs to be looked at
   *
   * @param childId
   * @throws PSCmsException
   */
  private PSLocator getRelationshipParentFolder(int childId) throws PSCmsException {

    PSRelationshipFilter filter = new PSRelationshipFilter();

    filter.setDependentId(childId);

    filter.setCommunityFiltering(false);
    filter.setCategory(PSRelationshipFilter.FILTER_CATEGORY_FOLDER);

    List<PSRelationship> relationships =
        PSRelationshipProcessor.getInstance().getRelationshipList(filter);
    for (PSRelationship rel : relationships) {
      if (!rel.getConfig().getName().equals(PSRelationshipConfig.CATEGORY_RECYCLED)) {
        return rel.getOwner();
      }
    }

    return null;
  }

  /**
   * Make sure the target folder does not have a child with the same name as the source folder.
   *
   * @param srcFolder the source folder that contains the source navon, assumed not <code>null
   *     </code>.
   * @param targetFolder the target folder that contains the target navon, assumed not <code>null
   *     </code>.
   */
  private void validateMoveRequest(PSComponentSummary srcFolder, PSComponentSummary targetFolder) {
    PSLegacyGuid tgtId = new PSLegacyGuid(targetFolder.getHeadLocator());
    String[] paths = contentWs.findItemPaths(tgtId);
    String newPath = paths[0] + "/" + srcFolder.getName();
    IPSGuid id = contentWs.getIdByPath(newPath);
    if (id != null) {
      PSNavException e =
          new PSNavException(
              NavigationErrorCodes
                  .NAVIGATION_SERVICE_FAILED_TO_MOVE_SECTION_BECAUSE_TARGET_ALREADY_HAS_ITEM,
              new Object[] {targetFolder.getName(), srcFolder.getName()});
      log.warn(PSExceptionUtils.getMessageForLog(e));
      throw e;
    }
  }

  /*
   * //see base interface method for details
   */
  public IPSGuid addNavTreeToFolder(String path, String navTreeName, String navTreeTitle) {
    return addNavTreeToFolder(path, navTreeName, navTreeTitle, -1);
  }

  /*
   * //see base interface method for details
   */
  public IPSGuid addNavTreeToFolder(
      String path, String navTreeName, String navTreeTitle, int workflowId) {
    notEmpty(path, "path");
    notEmpty(navTreeName, "navTreeName");
    notEmpty(navTreeTitle, "navTreeTitle");

    PSComponentSummary navSummary = findNavSummary(path);
    if (navSummary != null) {
      if (getNavonContentTypeIds().contains(navSummary.getContentTypeId())) {
        throw new PSNavException(
            NavigationErrorCodes.NAVIGATION_SERVICE_NAVTREE_CANNOT_BE_ADDED_TO_FOLDER_WITH_NAVON);
      } else {
        throw new PSNavException(
            NavigationErrorCodes.NAVIGATION_SERVICE_NAVTREE_CANNOT_BE_ADDED_TO_FOLDER_WITH_NAVTREE);
      }
    }

    try {
      List<String> typeNames = getNavTreeContentTypeNames();
      if (typeNames == null || typeNames.isEmpty()) {
        throw new PSNavException(
            NavigationErrorCodes.NAVIGATION_SERVICE_ERROR_ADDING_NAVTREE_TO_FOLDER);
      }
      PSCoreItem coreItem = contentWs.createItems(typeNames.get(0), 1).get(0);
      coreItem.setTextField("sys_title", navTreeName);
      coreItem.setTextField("displaytitle", navTreeTitle);
      PSItemField startDate = coreItem.getFieldByName("sys_contentstartdate");
      if (startDate != null) {
        startDate.addValue(new PSDateValue(new Date()));
      }

      if (workflowId != -1) {
        coreItem.setTextField("sys_workflowid", String.valueOf(workflowId));
      }

      // Save without check-in. Default / sample-site workflows can NPE in
      // sys_wfPerformTransition during check-in (m_nextAgingTransition).
      // saveItems(..., checkin=true) then throws PSErrorResultsException even
      // after the item row exists, so Create Site maps to HTTP 500 (#3364).
      // Do not call checkinItems here either: a failed check-in marks the
      // surrounding Spring transaction rollback-only, and homepage/template
      // save then fails with UnexpectedRollbackException.
      // The item is a valid nav root once it is a folder child (#3352).
      List<IPSGuid> guids = contentWs.saveItems(Collections.singletonList(coreItem), false, false);
      if (guids == null || guids.isEmpty() || guids.get(0) == null) {
        throw new PSNavException(
            NavigationErrorCodes.NAVIGATION_SERVICE_ERROR_ADDING_NAVTREE_TO_FOLDER);
      }
      contentWs.addFolderChildren(path, guids);

      return guids.get(0);
    } catch (PSNavException ne) {
      throw ne;
    } catch (Exception ex) {
      PSNavException ne =
          new PSNavException(
              NavigationErrorCodes.NAVIGATION_SERVICE_ERROR_ADDING_NAVTREE_TO_FOLDER,
              new Object[] {path, navTreeName},
              ex);
      if (ex instanceof PSErrorException) {
        ne = new PSNavException(ex);
      }

      // Surface nested error map from multi-object save (otherwise only the exception class name)
      if (ex instanceof com.percussion.webservices.PSErrorResultsException ere) {
        log.error(
            "Error adding NavTree to folder path={} name={}: errors={}",
            path,
            navTreeName,
            ere.getErrors());
      }
      log.error(PSExceptionUtils.getMessageForLog(ex));
      log.debug(PSExceptionUtils.getDebugMessageForLog(ex));
      throw ne;
    }
  }

  /*
   * //see base interface method for details
   */
  public PSComponentSummary findNavSummary(IPSGuid folderId) {
    notNull(folderId);

    IPSRequestContext req = getRequestCtx();
    PSLocator folderLoc = ((PSLegacyGuid) folderId).getLocator();

    return getChildNavonSummary(req, folderLoc);
  }

  /*
   * //see base interface method for details
   */
  public PSComponentSummary findNavSummary(String folderPath) throws PSNavException {
    notNull(folderPath);
    notEmpty(folderPath);

    IPSRequestContext req = getRequestCtx();
    PSLocator folderLoc = getFolderIdFromPath(req, folderPath, FOLDER_RELATE_TYPE);

    return getChildNavonSummary(req, folderLoc);
  }

  /*
   * //see base interface method for details
   */
  public IPSGuid findNavigationIdFromFolder(String folderPath) {
    return findNavigationIdFromFolder(folderPath, FOLDER_RELATE_TYPE);
  }

  public IPSGuid findNavigationIdFromFolder(String folderPath, String relationshipTypeName) {
    notNull(folderPath);
    notEmpty(folderPath);

    IPSRequestContext req = getRequestCtx();
    PSLocator folderLoc = getFolderIdFromPath(req, folderPath, relationshipTypeName);
    IPSGuid id = guidMgr.makeGuid(folderLoc);
    return findNavigationIdFromFolder(id, relationshipTypeName);
  }

  /*
   * //see base interface method for details
   */
  @Override
  public IPSGuid findNavigationIdFromFolder(IPSGuid folderId) {
    return findNavigationIdFromFolder(folderId, PSRelationshipConfig.TYPE_FOLDER_CONTENT);
  }

  @Override
  public IPSGuid findNavigationIdFromFolder(IPSGuid folderId, String relationshipTypeName) {
    notNull(folderId);

    PSLocator folderLoc = guidMgr.makeLocator(folderId);
    IPSRequestContext req = getRequestCtx();
    PSLocator navonLoc = findChildNavonLocator(req, folderLoc, relationshipTypeName);
    return (navonLoc == null) ? null : guidMgr.makeGuid(navonLoc);
  }

  /*
   * //see base interface method for details
   */
  public String getNavTitle(IPSGuid navId) {
    notNull(navId);
    Map<String, String> map = getNavonProperties(navId, Collections.singletonList("displaytitle"));
    return map.get("displaytitle");
  }

  /*
   * (non-Javadoc)
   * @see com.percussion.fastforward.managednav.IPSManagedNavService#getNavonProperties(com.percussion.utils.guid.IPSGuid, java.util.List)
   */
  public Map<String, String> getNavonProperties(IPSGuid navId, List<String> propertyNames) {
    notNull(navId);
    Map<String, String> propertyMap = new HashMap<>();
    List<Node> navNodes = contentDsWs.findNodesByIds(Collections.singletonList(navId), true);
    if (navNodes.isEmpty()) {
      throw new PSNavException("Cannot find nav-node id = " + navId);
    }
    Node node = navNodes.get(0);
    try {
      for (String name : propertyNames) {
        if (node.hasProperty(name)) {
          propertyMap.put(name, node.getProperty("rx:" + name).getString());
        }
      }
    } catch (Exception e) {
      String errorMsg = "Cannot get properties from nav-node id = {} Error: {}";
      log.error(errorMsg, navId, PSExceptionUtils.getMessageForLog(e));
      throw new PSNavException(e);
    }
    return propertyMap;
  }

  /*
   * //see base interface method for details
   */
  public void setNavTitle(IPSGuid nodeId, String title) {
    notNull(nodeId);
    notNull(title);
    notEmpty(title);
    Map<String, String> map = new HashMap<>();
    map.put("displaytitle", title);
    setNavonProperties(nodeId, map);
  }

  /*
   * (non-Javadoc)
   * @see com.percussion.fastforward.managednav.IPSManagedNavService#setNavonProperties(com.percussion.utils.guid.IPSGuid, java.util.Map)
   */
  public void setNavonProperties(IPSGuid nodeId, Map<String, String> propertyMap) {
    notNull(nodeId);
    // JCR PSContentNode is a read-only wrapper (LockException). Persist via
    // loadItems/saveItems. prepareForEdit on sample rffNavon NPEs and, if it
    // joins the REST TX, Spring marks rollback-only (#3797). Isolate checkout.
    List<PSItemStatus> statuses = null;
    if (!isNavonAlreadyCheckedOut(nodeId)) {
      statuses = prepareForEditIsolated(nodeId);
    }
    try {
      applyNavonPropertyMap(nodeId, propertyMap);
    } catch (Exception e) {
      throw new PSNavException("Failed to set properties for navon (id=" + nodeId + ").", e);
    } finally {
      releaseFromEditIsolated(statuses);
    }
  }

  /**
   * Load the navon, set text fields, {@code saveItems} without check-in.
   */
  void applyNavonPropertyMap(IPSGuid nodeId, Map<String, String> propertyMap)
      throws PSErrorResultsException {
    List<PSCoreItem> items =
        contentWs.loadItems(Collections.singletonList(nodeId), false, false, false, false);
    if (items == null || items.isEmpty()) {
      throw new PSNavException("Cannot find nav-node id = " + nodeId);
    }
    PSCoreItem item = items.get(0);
    for (Entry<String, String> entry : propertyMap.entrySet()) {
      String name = entry.getKey();
      if (name == null || name.isBlank()) {
        continue;
      }
      item.setTextField(name, entry.getValue() == null ? "" : entry.getValue());
    }
    contentWs.saveItems(Collections.singletonList(item), false, false);
  }

  /**
   * {@code prepareForEdit} without joining the caller TX. Sample-workflow NPE
   * would otherwise mark the Architecture {@code POST /section/update} TX
   * rollback-only even when the exception is caught.
   *
   * @return checkout statuses, or {@code null} when sample-workflow prepare
   *     was skipped
   */
  List<PSItemStatus> prepareForEditIsolated(IPSGuid nodeId) {
    try {
      return runWithoutJoiningCallerTx(
          () -> {
            try {
              return contentWs.prepareForEdit(Collections.singletonList(nodeId));
            } catch (PSErrorResultsException e) {
              throw new PSNavException(
                  "Failed to prepare navon for edit (id=" + nodeId + ").", e);
            }
          });
    } catch (RuntimeException e) {
      if (!PSNavFolderUtils.isSampleWorkflowAttachFailure(e)) {
        throw e;
      }
      log.warn("Skipping prepareForEdit for navon properties (sample workflow); id={}", nodeId, e);
      return null;
    }
  }

  void releaseFromEditIsolated(List<PSItemStatus> statuses) {
    if (statuses == null) {
      return;
    }
    try {
      runWithoutJoiningCallerTx(
          () -> {
            contentWs.releaseFromEdit(statuses, false);
            return Boolean.TRUE;
          });
    } catch (RuntimeException e) {
      if (!PSNavFolderUtils.isSampleWorkflowAttachFailure(e)) {
        throw e;
      }
      log.warn("Skipping releaseFromEdit after navon property save", e);
    }
  }

  boolean isNavonAlreadyCheckedOut(IPSGuid nodeId) {
    if (cmsMgr == null || !(nodeId instanceof PSLegacyGuid)) {
      return false;
    }
    try {
      PSComponentSummary sum =
          cmsMgr.loadComponentSummary(((PSLegacyGuid) nodeId).getContentId());
      return sum != null && StringUtils.isNotBlank(sum.getCheckoutUserName());
    } catch (RuntimeException e) {
      log.debug("Could not read checkout user for navon {}", nodeId, e);
      return false;
    }
  }

  private <T> T runWithoutJoiningCallerTx(java.util.function.Supplier<T> work) {
    if (transactionManager == null) {
      return work.get();
    }
    TransactionTemplate tt = new TransactionTemplate(transactionManager);
    tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_NOT_SUPPORTED);
    return tt.execute(status -> work.get());
  }

  @Autowired(required = false)
  public void setTransactionManager(PlatformTransactionManager transactionManager) {
    this.transactionManager = transactionManager;
  }

  /*
   * //see base interface method for details
   */
  public List<IPSGuid> findChildNavonIds(IPSGuid nodeId) {
    notNull(nodeId);

    List<IPSGuid> results = new ArrayList<>();
    IPSFolderRelationshipCache cache = PSFolderRelationshipCache.getInstance();

    try {
      IPSGuid slotid = getMenuSlot().getGUID();
      List<PSAaRelationship> rels = contentWs.loadSlotContentRelationships(nodeId, slotid);
      for (PSAaRelationship r : rels) {
        PSLocator loc = r.getDependent();
        PSLegacyGuid depId = new PSLegacyGuid(loc);
        if (doesParentFolderExist(cache, loc)) results.add(depId);
      }
    } catch (PSErrorException e) {
      String errorMsg = "Failed to load slot content for node id = " + nodeId;
      throw new PSNavException(errorMsg, e);
    }

    return results;
  }

  private boolean doesParentFolderExist(IPSFolderRelationshipCache cache, PSLocator psLocator) {
    boolean doesParentFolderExist = false;
    if (cache != null) {
      List<PSRelationship> parentRels = cache.getParents(psLocator);
      for (PSRelationship rel : parentRels) {
        if (rel.getConfig().getName().equals(FOLDER_RELATE_TYPE)) {
          doesParentFolderExist = true;
          break;
        }
      }
    }
    return doesParentFolderExist;
  }

  /*
   * //see base interface method for details
   */
  public List<IPSGuid> findDescendantNavonIds(IPSGuid nodeId) {
    notNull(nodeId);

    List<IPSGuid> cIds = findChildNavonIds(nodeId);
    if (cIds.isEmpty()) return cIds;

    List<IPSGuid> results = new ArrayList<>(cIds);
    for (IPSGuid cid : cIds) {
      cid = contentDsWs.getItemGuid(cid);
      List<IPSGuid> ids = findDescendantNavonIds(cid);
      results.addAll(ids);
    }
    return results;
  }

  public List<IPSGuid> findAncestorNavonIds(IPSGuid nodeId) {
    if (log.isDebugEnabled()) log.debug("[findAncestorNavonIds] nodeId = {}", nodeId.toString());

    List<IPSGuid> ancestorIds = new ArrayList<>();
    PSLocator dependent = new PSLocator(((PSLegacyGuid) nodeId).getContentId());
    findAncestorNavonIds(dependent, ancestorIds);

    Collections.reverse(ancestorIds);
    return ancestorIds;
  }

  private void findAncestorNavonIds(PSLocator dependent, List<IPSGuid> ancestorIds) {
    if (log.isDebugEnabled())
      log.debug(
          "[findAncestorNavonIds] dependent = {}, ancestorIds = {}",
          dependent.getId(),
          ancestorIds);

    IPSGuid slotId = getMenuSlot().getGUID();

    List<PSAaRelationship> relationships =
        contentWs.loadDependentSlotContentRelationships(dependent, slotId);

    boolean checkSectionList = relationships.size() > 1;

    // iterate over the owners to find the "original owner", but not the owner
    // that links to the "section link" node
    for (PSAaRelationship r : relationships) {
      if (checkSectionList) {
        IPSGuid ownerId = new PSLegacyGuid(r.getOwner());
        IPSGuid childNavonId = new PSLegacyGuid(dependent);
        if (isSectionLink(childNavonId, ownerId)) {
          if (log.isDebugEnabled())
            log.debug(
                "Skip section link ownerId = {} , dependentId = {}",
                ownerId.getUUID(),
                childNavonId.getUUID());

          // skip the owner that "links" to the "section link node".
          continue;
        }
      }

      ancestorIds.add(new PSLegacyGuid(r.getOwner()));
      findAncestorNavonIds(r.getOwner(), ancestorIds);
      break;
    }
  }

  /**
   * Gets a folder ID from its path.
   *
   * @param req the request context, assumed not <code>null</code>.
   * @param folderPath the folder path, assumed not blank.
   * @return the locator of the specified folder, not <code>null</code>.
   */
  private PSLocator getFolderIdFromPath(
      IPSRequestContext req, String folderPath, String relationshipTypeName) throws PSNavException {
    try {
      PSServerFolderProcessor fp = PSServerFolderProcessor.getInstance();
      int id = fp.getIdByPath(folderPath, relationshipTypeName);
      if (id == -1) {
        PSNavException e =
            new PSNavException(
                NavigationErrorCodes.NAVIGATION_SERVICE_FOLDER_ID_NOT_FOUND_FOR_PATH, folderPath);
        log.error(PSExceptionUtils.getMessageForLog(e));
        log.debug(PSExceptionUtils.getDebugMessageForLog(e));
        throw (e);
      }

      return new PSLocator(id, 1);
    } catch (PSCmsException e) {
      PSNavException ne =
          new PSNavException(
              NavigationErrorCodes.NAVIGATION_SERVICE_FOLDER_ID_NOT_FOUND_FOR_PATH,
              new Object[] {folderPath},
              e);
      log.error(PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
      throw (ne);
    }
  }

  /*
   * //see base interface method for details
   */
  public void addLandingPageToNavnode(IPSGuid pageId, IPSGuid nodeId, String templateName) {
    notNull(pageId);
    notNull(nodeId);
    notNull(templateName);
    notEmpty(templateName);

    PSNavConfig config = getNavConfig();
    List<String> lpSlotNames = config.getNavLandingPageSlotNames();

    try {
      nodeId = contentDsWs.getItemGuid(nodeId);
      removeLinksToLandingPages(nodeId);
      // Fresh percNavon under sample rffNavTree is already checked out from save
      // (#3672 skipped check-in). prepareForEdit NPEs when CONTENTSTATEID is 0
      // or sys_contentstateid is missing on the item def (#3676 / #3364).
      prepareForEditIgnoringSampleWorkflow(nodeId);
      prepareForEditIgnoringSampleWorkflow(pageId);
      contentWs.addContentRelations(
          nodeId, Collections.singletonList(pageId), lpSlotNames.get(0), templateName, 0);
    } catch (Exception e) {
      String msg = "Failed to add landing page (id=" + pageId + ") to navon (id=" + nodeId + ").";
      log.error("{} Error: {}", msg, PSExceptionUtils.getMessageForLog(e));
      throw new PSNavException(msg, e);
    }
  }

  /**
   * Checkout for AA landing attach. Sample-site workflows NPE in {@code
   * PSContentWs.prepareForEdit}/{@code checkinItems}; the item is already checked
   * out after percNavon save, so skip and still add the relationship (#3676).
   */
  /**
   * @return {@code true} when checkout ran; {@code false} when sample-workflow
   *     prepare was skipped.
   */
  boolean prepareForEditIgnoringSampleWorkflow(IPSGuid id) {
    try {
      contentWs.prepareForEdit(id);
      return true;
    } catch (RuntimeException e) {
      if (!PSNavFolderUtils.isSampleWorkflowAttachFailure(e)) {
        throw e;
      }
      log.warn("Skipping prepareForEdit for landing attach (sample workflow); id={}", id, e);
      return false;
    }
  }

  /*
   * (non-Javadoc)
   * @see com.percussion.fastforward.managednav.IPSManagedNavService#addNavonToNavon(com.percussion.utils.guid.IPSGuid, com.percussion.utils.guid.IPSGuid)
   */
  public void addNavonToParentNavon(IPSGuid navonId, IPSGuid parentNavonId, int index) {
    notNull(navonId);
    notNull(parentNavonId);
    parentNavonId = contentDsWs.getItemGuid(parentNavonId);
    navonId = contentDsWs.getItemGuid(navonId);

    // convert index to 1 based number
    index = (index == -1) ? index : index + 1;

    addNavonSubmenu(
        getRequestCtx(),
        ((PSLegacyGuid) parentNavonId).getLocator(),
        ((PSLegacyGuid) navonId).getLocator(),
        getMenuSlotId(),
        DUMMY_TEMPLATEID,
        index);
  }

  /*
   * (non-Javadoc)
   * @see com.percussion.fastforward.managednav.IPSManagedNavService#deleteNavonRelationship(com.percussion.utils.guid.IPSGuid, com.percussion.utils.guid.IPSGuid)
   */
  public void deleteNavonRelationship(IPSGuid navonId, IPSGuid parentNavonId) {
    notNull(navonId);
    notNull(parentNavonId);
    parentNavonId = contentDsWs.getItemGuid(parentNavonId);
    navonId = contentDsWs.getItemGuid(navonId);
    PSRelationshipFilter rfilter = new PSRelationshipFilter();
    rfilter.setOwnerId(((PSLegacyGuid) parentNavonId).getContentId());
    rfilter.setDependentId(((PSLegacyGuid) navonId).getContentId());
    List<PSAaRelationship> rels = contentWs.loadContentRelations(rfilter, false);
    List<IPSGuid> relGuids = new ArrayList<>();
    for (PSAaRelationship rel : rels) {
      relGuids.add(rel.getGuid());
    }
    PSWebserviceUtils.deleteRelationships(relGuids, false);
  }

  /*
   * (non-Javadoc)
   * @see com.percussion.fastforward.managednav.IPSManagedNavService#replaceNavon(com.percussion.utils.guid.IPSGuid, com.percussion.utils.guid.IPSGuid, com.percussion.utils.guid.IPSGuid)
   */
  public void replaceNavon(IPSGuid oldNavonId, IPSGuid newNavonId, IPSGuid parentNavonId) {
    notNull(oldNavonId);
    notNull(newNavonId);
    notNull(parentNavonId);
    parentNavonId = contentDsWs.getItemGuid(parentNavonId);
    oldNavonId = contentDsWs.getItemGuid(oldNavonId);
    newNavonId = contentDsWs.getItemGuid(newNavonId);
    PSRelationshipFilter filter = new PSRelationshipFilter();
    filter.setOwnerId(((PSLegacyGuid) parentNavonId).getContentId());
    filter.limitToEditOrCurrentOwnerRevision(true);
    filter.setDependentId(((PSLegacyGuid) oldNavonId).getContentId());
    List<PSRelationship> rels = PSWebserviceUtils.loadRelationships(filter);
    if (!rels.isEmpty()) {
      PSRelationship rel = rels.get(0);
      rel.setDependent(((PSLegacyGuid) newNavonId).getLocator());
      PSWebserviceUtils.saveRelationship(rel);
    }
  }

  /**
   * Removes the links to its landing pages (if there are any).
   *
   * @param nodeId the ID of the navigation node, assumed not <code>null</code>.
   */
  private void removeLinksToLandingPages(IPSGuid nodeId) {
    List<PSAaRelationship> links = getLandingPageLinks(nodeId);
    if (links == null) return;

    List<IPSGuid> ids = new ArrayList<>();
    for (PSAaRelationship link : links) {
      ids.add(link.getGuid());
    }
    contentWs.deleteContentRelations(ids);
  }

  /**
   * Gets the AA relationships that link the specified navigation node (navon / navtree) to its
   * landing pages. Note, this may return more than one links, but there should not be more than one
   * such link in a "right" environment.
   *
   * @param nodeId the ID of navigation node, assumed not <code>null</code>.
   * @return the AA relationships, it may be <code>null</code> if there is no such link.
   */
  private List<PSAaRelationship> getLandingPageLinks(IPSGuid nodeId) {
    nodeId = contentDsWs.getItemGuid(nodeId);
    PSLocator navonLoc = guidMgr.makeLocator(nodeId);
    IPSTemplateSlot lpSlot = getLandingPageSlot();

    PSRelationshipFilter filter = new PSRelationshipFilter();
    filter.setName(PSRelationshipFilter.FILTER_NAME_ACTIVE_ASSEMBLY);
    filter.setOwner(navonLoc);
    filter.limitToOwnerRevision(true);
    filter.setProperty(PSRelationshipConfig.PDU_SLOTID, String.valueOf(lpSlot.getGUID().getUUID()));

    try {
      List<PSAaRelationship> rels = contentWs.loadContentRelations(filter, false);
      if (rels.isEmpty()) return null;

      return rels;
    } catch (Exception e) {
      String msg = "Failed to get landing page from navon (id=" + nodeId.toString() + ").";
      log.error(msg, e);
      throw new PSNavException(msg, e);
    }
  }

  /*
   * //see base interface method for details
   */
  public IPSGuid getLandingPageFromNavnode(IPSGuid nodeId) {
    notNull(nodeId);

    List<PSAaRelationship> links = getLandingPageLinks(nodeId);
    if (links == null) return null;

    return new PSLegacyGuid(links.get(0).getDependent());
  }

  /*
   * //see base interface method for details
   */
  public boolean isLandingPage(IPSGuid pageId) {
    return isLandingPage(pageId, PSRelationshipConfig.TYPE_FOLDER_CONTENT);
  }

  /*
   * //see base interface method for details
   */
  public boolean isLandingPage(IPSGuid pageId, String relationshipTypeName) {
    notNull(pageId);
    return findRelatedNavigationNodeId(pageId, relationshipTypeName) != null;
  }

  public IPSGuid findRelatedNavigationNodeId(IPSGuid id) {
    return findRelatedNavigationNodeId(id, PSRelationshipConfig.TYPE_FOLDER_CONTENT);
  }

  /*
   * //see base interface method for details
   */
  public IPSGuid findRelatedNavigationNodeId(IPSGuid id, String relationshipTypeName) {
    notNull(id);
    List<String> paths = asList(contentWs.findFolderPaths(id, relationshipTypeName));
    if (paths.isEmpty()) {
      return null;
    }

    IPSGuid navId = findNavigationIdFromFolder(paths.get(0), relationshipTypeName);
    if (navId == null) {
      return null;
    }

    IPSGuid pageId = getLandingPageFromNavnode(navId);
    if (pageId != null) {
      if (((PSLegacyGuid) pageId).getContentId() == ((PSLegacyGuid) id).getContentId()) {
        return navId;
      }

      return null;
    } else {
      log.debug("Cannot find landing page for navigation id: {}", navId);
      return null;
    }
  }

  /*
   * //see base interface method for details
   */
  public boolean isSectionLink(IPSGuid navonId, IPSGuid navonParentId) {
    notNull(navonId);
    notNull(navonParentId);

    boolean result = false;

    PSServerFolderProcessor fp = PSServerFolderProcessor.getInstance();

    List<PSLocator> navonFolders;
    List<PSLocator> parentFolders;

    try {
      navonFolders = fp.getAncestorLocators(new PSLocator(navonId.getUUID()));
      parentFolders = fp.getAncestorLocators(new PSLocator(navonParentId.getUUID()));
    } catch (PSCmsException e) {
      throw new PSNavException("Cannot find related folder for navigation node id = " + navonId, e);
    }

    // if a real section, then the navon's folder's parent folder will be the same as it's parent
    // navon's folder
    if (navonFolders.size() == parentFolders.size())
      result = true; // shortcut, since can't be a real navon in this case
    else {
      try {
        int navonGrandparentPos = navonFolders.size() - 2;
        int parentFolderPos = parentFolders.size() - 1;

        PSLocator navonGpFolderLoc = navonFolders.get(navonGrandparentPos);
        PSLocator parentFolderLoc = parentFolders.get(parentFolderPos);
        result = navonGpFolderLoc.getId() != parentFolderLoc.getId();
      } catch (Exception e) {
        log.error("Error processing navigation section link: Error: {}", e.getMessage());
      }
    }

    if (log.isDebugEnabled())
      log.debug(
          "[isSectionLink : {} ] navonId = {} , navonParentId = {}",
          result,
          navonId.getUUID(),
          navonParentId.getUUID());

    return result;
  }

  /* (non-Javadoc)
   * @see com.percussion.fastforward.managednav.IPSManagedNavService#isNavTree(com.percussion.utils.guid.IPSGuid)
   */
  public boolean isNavTree(IPSGuid guid) {
    notNull(guid);

    try {
      IPSItemEntry target = cmsMgr.findItemEntry(((PSLegacyGuid) guid).getContentId());
      String contentTypeName =
          PSItemDefManager.getInstance().contentTypeIdToName(target.getContentTypeId());
      if (PSNavNameAliases.isNavTreeTypeName(contentTypeName)) {
        return true;
      }
      for (String configured : getNavTreeContentTypeNames()) {
        if (StringUtils.equalsIgnoreCase(contentTypeName, configured)
            || PSNavNameAliases.sameNavRole(contentTypeName, configured)) {
          return true;
        }
      }
      return false;
    } catch (PSInvalidContentTypeException e) {
      return false;
    }
  }

  /**
   * Gets the configuration of the managed navigation.
   *
   * @return the configuration, never <code>null</code>.
   */
  private PSNavConfig getNavConfig() {
    if (navConfig != null) return navConfig;

    navConfig = PSNavConfig.getInstance();
    return navConfig;
  }

  /*
   * //see base interface method for details
   */
  public boolean isManagedNavUsed() {
    return PSNavConfig.isManagedNavUsed();
  }

  /**
   * Gets the request context, which contains either the current servlet request or the default
   * request if the current thread is not initiated from a servlet request.
   *
   * @return the request context, never <code>null</code>.
   */
  private IPSRequestContext getRequestCtx() {
    PSRequestContext req =
        (PSRequestContext) PSRequestInfoBase.getRequestInfo(PSRequestInfoBase.KEY_PSREQUESTCONTEXT);
    if (req == null) req = new PSRequestContext(PSRequest.getContextForRequest());

    return req;
  }

  /**
   * Gets the navigation menu slot defined in the navigation configuration.
   *
   * @return the navigation menu slot, never <code>null</code>.
   */
  private IPSTemplateSlot getMenuSlot() {
    if (menuSlot == null) {
      menuSlot = getSlot(getNavConfig().getNavSubMenuSlotNames().get(0));
    }

    return menuSlot;
  }

  /**
   * Gets the UU-ID of the menu slot.
   *
   * @return menu slot UUID.
   */
  public long getMenuSlotId() {
    return getMenuSlot().getGUID().getUUID();
  }

  /**
   * Gets the navigation landing page slot defined in the navigation configuration.
   *
   * @return the navigation landing page slot, never <code>null</code>.
   */
  private IPSTemplateSlot getLandingPageSlot() {
    if (landingPageSlot == null) {
      landingPageSlot = getSlot(getNavConfig().getNavLandingPageSlotNames().get(0));
    }

    return landingPageSlot;
  }

  /**
   * Gets the slot identified by the specified name.
   *
   * @param name the slot name, assumed not <code>null</code>.
   * @return the slot, never <code>null</code>.
   * @throws PSNavException if the slot could not be found.
   */
  private IPSTemplateSlot getSlot(String name) {
    try {
      return asmService.findSlotByName(name);
    } catch (PSAssemblyException e) {
      String errorMsg = "Failed to find slot: \"" + name + "\"";
      log.error(errorMsg, e);
      throw new PSNavException(errorMsg, e);
    }
  }

  /** The cached navigation configuration, initialized by {@link #getNavConfig()}. */
  private PSNavConfig navConfig = null;

  /** The content service, initialized by constructor. */
  private IPSContentWs contentWs;

  /** The content design service, initialized by constructor. */
  private IPSContentDesignWs contentDsWs;

  /** The assembly service, initialized by constructor. */
  private IPSAssemblyService asmService;

  /** The guid manager, initialized by constructor. */
  private IPSGuidManager guidMgr;

  /**
   * The service used to retrieve legacy contents such as component summaries, initialized by
   * constructor.
   */
  private IPSCmsObjectMgr cmsMgr;

  /**
   * Optional Spring TX manager. Used to suspend the caller TX around sample
   * workflow {@code prepareForEdit} so NPE cannot mark REST rollback-only.
   */
  private PlatformTransactionManager transactionManager;

  /**
   * The menu slot defined in the navigation configuration. Set in {@link #getMenuSlot()}. Never
   * <code>null</code> after that.
   */
  private IPSTemplateSlot menuSlot;

  /**
   * The landing page slot defined in the navigation configuration. Set in {@link
   * #getLandingPageSlot()}. Never <code>null</code> after that.
   */
  private IPSTemplateSlot landingPageSlot;

  /** Logger for this service. */
  private static final Logger log = LogManager.getLogger(IPSConstants.NAVIGATION_LOG);

  /**
   * The dummy template ID, used for create AA relationship between navigation nodes where the
   * template is not used for rendering the navigation node.
   */
  private static Long DUMMY_TEMPLATEID = -2L;

  private static final String FOLDER_RELATE_TYPE = PSRelationshipConfig.TYPE_FOLDER_CONTENT;

  private static final String RECYCLED_RELATE_TYPE = PSRelationshipConfig.TYPE_RECYCLED_CONTENT;
}
