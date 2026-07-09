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

package com.percussion.category.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.percussion.category.dao.IPSCategoryDao;
import com.percussion.category.data.PSCategory;
import com.percussion.category.data.PSCategoryLockInfo;
import com.percussion.category.data.PSCategoryNode;
import com.percussion.category.marshaller.PSCategoryMarshaller;
import com.percussion.category.marshaller.PSCategoryUnMarshaller;
import com.percussion.category.service.IPSCategoryService;
import com.percussion.delivery.service.IPSDeliveryInfoService;
import com.percussion.itemmanagement.service.IPSItemService;
import com.percussion.itemmanagement.service.IPSItemWorkflowService;
import com.percussion.pubserver.IPSPubServerService;
import com.percussion.services.contentchange.IPSContentChangeService;
import com.percussion.services.contentchange.data.PSContentChangeEvent;
import com.percussion.services.contentchange.data.PSContentChangeType;
import com.percussion.services.error.PSNotFoundException;
import com.percussion.services.guidmgr.IPSGuidManager;
import com.percussion.services.guidmgr.data.PSLegacyGuid;
import com.percussion.share.service.IPSIdMapper;
import com.percussion.share.service.exception.PSBeanValidationException;
import com.percussion.share.service.exception.PSDataServiceException;
import com.percussion.share.service.exception.PSParameterValidationUtils;
import com.percussion.share.service.exception.PSValidationException;
import com.percussion.share.validation.PSAbstractBeanValidator;
import com.percussion.sitemanage.data.PSSiteSummary;
import com.percussion.sitemanage.service.IPSSiteDataService;
import com.percussion.sitemanage.service.IPSSitePublishService;
import com.percussion.sitemanage.service.IPSSitePublishService.PubType;
import com.percussion.user.service.IPSUserService;
import com.percussion.utils.guid.IPSGuid;
import java.nio.channels.OverlappingFileLockException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestParam;

@Path("/category")
@Component("categoryService")
@Lazy
public class PSCategoryService implements IPSCategoryService {

  private static final Logger log = LogManager.getLogger(PSCategoryService.class);

  private IPSUserService userService;
  private IPSDeliveryInfoService deliveryService;
  private IPSSiteDataService siteDataService;

  @Autowired private IPSCategoryDao categoryDao;

  @Autowired private IPSGuidManager guidMgr;

  @Autowired private IPSContentChangeService contentChangeService;

  @Autowired private IPSSitePublishService sitePublishService;

  @Autowired private IPSIdMapper idMapper;

  public PSCategoryService() {
    // empty for jax-rs
  }

  @Autowired
  public PSCategoryService(
      IPSUserService userService,
      IPSDeliveryInfoService deliveryService,
      IPSSiteDataService siteDataService) {
    this.userService = userService;
    this.deliveryService = deliveryService;
    this.siteDataService = siteDataService;
  }

  @GET
  @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN, MediaType.APPLICATION_XML})
  @Consumes({MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN, MediaType.APPLICATION_XML})
  @Path("/all")
  public String getCategoryList() throws PSDataServiceException {
    return getCategoryList(null).toJSON();
  }

  @GET
  @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN, MediaType.APPLICATION_XML})
  @Consumes({MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN, MediaType.APPLICATION_XML})
  @Path("/all/{sitename}")
  public String getCategoryListWithStrng(@PathParam("sitename") String sitename)
      throws PSDataServiceException {
    if (sitename != null && sitename.equals("undefined")) sitename = null;
    PSCategory category = getCategoryTreeForSite(sitename, null, false, true);
    return category.toJSON();
  }

  public PSCategory getCategoryList(String sitename) throws PSDataServiceException {
    return getCategoryTreeForSite(sitename, null, false, true);
  }

  public PSCategory getCategoryTreeForSite(
      String sitename, String rootPath, boolean includeDeleted, boolean includeNotSelectable)
      throws PSDataServiceException {
    if (StringUtils.isBlank(sitename)) sitename = null;
    if (StringUtils.isBlank(rootPath)) rootPath = null;

    PSCategoryUnMarshaller unMarshaller = new PSCategoryUnMarshaller();
    PSCategory category = unMarshaller.unMarshal();

    PSCategoryNode node =
        findCategoryNode(category, sitename, rootPath, includeDeleted, includeNotSelectable);

    // extract nodes from dummy root or other found node.
    if (node == null) {
      category.setTopLevelNodes(new ArrayList<>());
      return category;
    }

    // Create new guids for manually created category items.
    boolean saveCategories = createIds(node);

    List<PSCategoryNode> filteredNodes = node.getChildNodes();
    if (filteredNodes == null) {
      filteredNodes = new ArrayList<>();
    }
    if (node.getChildNodes() != null && !node.getChildNodes().isEmpty()) {
      category.setTopLevelNodes(node.getChildNodes());
    }

    if (saveCategories) updateCategories(category, sitename);

    return category;
  }

  private boolean createIds(PSCategoryNode node) {
    boolean createdIds = false;
    if (StringUtils.isEmpty(node.getId())) {
      String newId = PSCategoryServiceUtil.createGuid();
      log.info("Created new id for category " + node.getTitle() + " " + newId);
      node.setId(newId);
      createdIds = true;
    }

    for (PSCategoryNode sub : node.getChildNodes()) createdIds |= createIds(sub);
    return createdIds;
  }

  @Override
  public PSCategoryNode findCategoryNode(
      String siteName, String rootPath, boolean includeDeleted, boolean includeNotSelectable) {

    PSCategoryUnMarshaller unMarshaller = new PSCategoryUnMarshaller();
    PSCategory category = unMarshaller.unMarshal();

    log.debug("Finding categoryNode with rootPath {} and site {}", rootPath, siteName);
    return findCategoryNode(category, siteName, rootPath, includeDeleted, includeNotSelectable);
  }

  private PSCategoryNode findCategoryNode(
      PSCategory category,
      String sitename,
      String rootPath,
      boolean includeDeleted,
      boolean includeNotSelectable) {
    LinkedList<String> findPath = new LinkedList<>();
    boolean relativePath = false;
    if (rootPath != null) {
      relativePath = (!rootPath.startsWith("/"));
      findPath = new LinkedList<>(Arrays.asList(StringUtils.split(rootPath, "/")));
    }

    if (!findPath.isEmpty()) {
      String checkElement = findPath.peek();
      while (!findPath.isEmpty()
          && (checkElement.equals("Categories") || StringUtils.isEmpty(checkElement))) {
        findPath.removeFirst();
        checkElement = findPath.peek();
      }
    }

    log.debug("Cleaned up parent seach path = {}", findPath);

    PSCategoryNode dummyRoot = new PSCategoryNode();
    dummyRoot.setId(PSCategoryServiceUtil.DUMMYROOT);
    dummyRoot.setChildNodes(category.getTopLevelNodes());
    dummyRoot.setDeleted(false);
    dummyRoot.setSelectable(true);
    dummyRoot.setTitle(PSCategoryServiceUtil.DUMMYROOT);

    return PSCategoryServiceUtil.filterForSite(
        dummyRoot,
        sitename,
        findPath,
        getActiveSiteNames(),
        relativePath,
        includeDeleted,
        includeNotSelectable);
  }

  private List<String> getActiveSiteNames() {
    List<PSSiteSummary> siteSummaries = siteDataService.findAll();
    List<String> currentSites = new ArrayList<>();
    for (PSSiteSummary site : siteSummaries) {
      currentSites.add(site.getName());
    }
    return currentSites;
  }

  @POST
  @Path("/update/{sitename}")
  @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN, MediaType.APPLICATION_XML})
  @Consumes({MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN, MediaType.APPLICATION_XML})
  public String updateCategoriesWithString(
      @RequestParam("categorystring") String categoryString, @PathParam("sitename") String sitename)
      throws PSDataServiceException {
    if (StringUtils.isBlank(categoryString)) {
      throw new IllegalArgumentException("categorystring parameter must not be null or empty");
    }
    PSCategory categoryWithJson = null;
    try {
      ObjectMapper mapper = new ObjectMapper();
      categoryWithJson = mapper.readValue(categoryString, PSCategory.class);
    } catch (JsonProcessingException ex) {
      log.error("Error while parsing json to {} Error: {}", categoryString, ex.getMessage());
      log.debug(ex.getMessage(), ex);
    }

    return updateCategories(categoryWithJson, sitename).toJSON();
  }

  @POST
  @Path("/update")
  @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN, MediaType.APPLICATION_XML})
  @Consumes({MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN, MediaType.APPLICATION_XML})
  public String updateCategoriesWithString(@RequestParam("categorystring") String categoryString)
      throws PSDataServiceException {
    if (StringUtils.isBlank(categoryString)) {
      throw new IllegalArgumentException("categorystring parameter must not be null or empty");
    }
    PSCategory categoryWithJson = null;
    try {
      ObjectMapper mapper = new ObjectMapper();
      categoryWithJson = mapper.readValue(categoryString, PSCategory.class);
    } catch (JsonProcessingException ex) {
      log.error("Error while parsing json to {} Error: {}", categoryString, ex.getMessage());
      log.debug(ex.getMessage(), ex);
    }

    return updateCategories(categoryWithJson, null).toJSON();
  }

  public PSCategory updateCategories(PSCategory category) throws PSDataServiceException {
    return updateCategories(category, null);
  }

  public PSCategory updateCategories(PSCategory category, String sitename)
      throws PSValidationException {

    PSCategory updatedCategory = null;
    doValidation(category);

    PSParameterValidationUtils.rejectIfNull("update", "category", category);

    PSCategoryUnMarshaller unMarshaller = new PSCategoryUnMarshaller();
    PSCategory oldCategory = unMarshaller.unMarshal();

    if (oldCategory != null) {

      PSCategoryServiceUtil.preserveDeletedNodes(
          category.getTopLevelNodes(),
          oldCategory.getTopLevelNodes(),
          sitename,
          getActiveSiteNames());
    }

    PSCategoryMarshaller marshaller = new PSCategoryMarshaller();

    marshaller.setCategory(category);

    try {
      marshaller.marshal();
    } catch (OverlappingFileLockException e) {
      log.error(
          "Category XML is locked by another user ! - PSCategoryService.updateCategories()", e);
    }

    updatedCategory = unMarshaller.unMarshal();

    if (updatedCategory == null) {
      log.error(
          "The updated categories are null ! - PSCategoryService.updateCategories()",
          new PSDataServiceException("Updated Categories are null"));
    } else if (updatedCategory.getTopLevelNodes() != null
        && !updatedCategory.getTopLevelNodes().isEmpty()) {
      Set<String> nodesToRemove = new HashSet<>();
      nodesToRemove =
          PSCategoryServiceUtil.removeDeletedNodes(
              updatedCategory.getTopLevelNodes(), nodesToRemove);
      if (!nodesToRemove.isEmpty()) {
        List<Integer> pageIds = categoryDao.getPageIdsFromCategoryIds(nodesToRemove);
        List<IPSGuid> guids = getGuidsFromPageIds(pageIds);
        categoryDao.delete(nodesToRemove, guids);
      }
    }
    return updatedCategory;
  }

  @GET
  @Path("/lockinfo")
  @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN, MediaType.APPLICATION_XML})
  @Consumes({MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN, MediaType.APPLICATION_XML})
  public String getLockInfo() {

    JSONObject jsonObject = null;

    if (PSCategoryLockInfo.isFileLocked()) jsonObject = PSCategoryLockInfo.getLockInfo();

    if (jsonObject != null) {
      try {
        // make sure the object is valid
        jsonObject.get("userName");
        return jsonObject.toString();
      } catch (JSONException e) {
        log.error(
            "JSON Exception occurred while reading from the json object - PSCategoryService.getLockInfo()",
            new PSDataServiceException("Could not read lock information file"));
      }
    } else {
      try {
        jsonObject = new JSONObject();
        jsonObject.put("userName", "");
        jsonObject.put("sessionId", "");
        jsonObject.put("sitename", "");
      } catch (JSONException e) {
        log.error(
            "JSON Exception occurred while creating empty json object - PSCategoryService.getLockInfo()",
            new WebApplicationException(
                "No lock on category tab. Could not create an empty json to return from api."));
      }
    }
    return jsonObject.toString();
  }

  @POST
  @Path("/locktab/{date}")
  @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN, MediaType.APPLICATION_XML})
  @Consumes({MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN, MediaType.APPLICATION_XML})
  public void lockCategoryTab(@PathParam("date") String date) {

    JSONObject jsonObject = null;

    if (PSCategoryLockInfo.isFileLocked()) jsonObject = PSCategoryLockInfo.getLockInfo();
    try {
      if (jsonObject != null) {

        if (!(jsonObject.get("userName")).equals(userService.getCurrentUser().getName())) {
          PSCategoryLockInfo.removeLockInfo();
          PSCategoryLockInfo.writeLockInfoToFile(userService, date);
        } else if (!(jsonObject.get("creationDate")).equals(date)) {
          PSCategoryLockInfo.removeLockInfo();
          PSCategoryLockInfo.writeLockInfoToFile(userService, date);
        }

      } else {
        PSCategoryLockInfo.writeLockInfoToFile(userService, date);
      }
    } catch (JSONException | PSDataServiceException e) {
      log.error(
          "JSON Exception occurred while reading from the json object - PSCategoryService.overrideCatTabLock()",
          new WebApplicationException("Could not read lock information file"));
    }
  }

  @POST
  @Path("/removelocktab")
  public void removeCategoryTabLock() {
    PSCategoryLockInfo.removeLockInfo();
  }

  @POST
  @Path("/updateindts/{sitename}/{deliveryserver}")
  @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN, MediaType.APPLICATION_XML})
  @Consumes({MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN, MediaType.APPLICATION_XML})
  public void updateCategoryInDTS(
      @PathParam("sitename") String sitename, @PathParam("deliveryserver") String deliveryserver)
      throws PSValidationException {

    try {
      PSCategory categoryTree = getCategoryList(sitename);
      String category = PSCategoryServiceUtil.prepareCategoryJson(categoryTree);

      if (deliveryserver.equalsIgnoreCase("Both")) {
        PSCategoryServiceUtil.publishToDTS(category, sitename, "Production", deliveryService);
        queuePagesForRepublish(categoryTree, sitename, "Production");
        PSCategoryServiceUtil.publishToDTS(category, sitename, "Staging", deliveryService);
        queuePagesForRepublish(categoryTree, sitename, "Staging");
      } else {
        PSCategoryServiceUtil.publishToDTS(category, sitename, deliveryserver, deliveryService);
        queuePagesForRepublish(categoryTree, sitename, deliveryserver);
      }
    } catch (PSValidationException e) {
      // Propagate as-is so it is handled by the registered validationExceptionMapper, which
      // returns a proper 400 with the structured validation error instead of a generic 500.
      log.warn(
          "Category publish to {} for site {} did not send any changes: {}",
          deliveryserver,
          sitename,
          e.getMessage());
      throw e;
    } catch (PSDataServiceException e) {
      log.error(
          "Error publishing categories to {} for site {}. Error: {}",
          deliveryserver,
          sitename,
          e.getMessage());
      log.debug(e.getMessage(), e);
      throw new WebApplicationException(e.getMessage());
    }
  }

  /**
   * After category rename/move/delete changes have been published to a delivery server, immediately
   * publishes any already-published pages that use one of the affected categories - the same
   * "Publish Now"/"Publish to Staging Now" on-demand action a user would get by publishing an
   * individual page. This is necessary because publishing category changes only updates the
   * delivery tier's category index used by the live Category Browser widget - it does not
   * regenerate any previously published page's HTML. Each page's own category label/breadcrumb text
   * is resolved once and baked into its markup at that page's own last publish time (see
   * PSPageUtils#getCategoryLabel and sys_assembly.vm), so without this, a renamed/moved/deleted
   * category would silently remain stale on already-published pages.
   *
   * <p>If the immediate on-demand publish fails for a given page (e.g. no matching on-demand
   * edition configured, or the page isn't currently in a workflow state from which it can be
   * auto-approved), that page is instead queued for the site's next scheduled incremental publish
   * run, so the update is not silently lost.
   *
   * <p>This is a best-effort step: any failure here is logged and swallowed rather than propagated,
   * since the category publish action itself already completed successfully by the time this runs.
   *
   * @param categoryTree the just-published category tree for the site, not <code>null</code>.
   * @param sitename the site the categories were published for.
   * @param deliveryserver "Production" or "Staging" - determines whether affected pages are
   *     published now to the live site or the staging server.
   */
  private void queuePagesForRepublish(
      PSCategory categoryTree, String sitename, String deliveryserver) {
    try {
      Set<String> affectedCategoryIds = PSCategoryServiceUtil.getAffectedCategoryIds(categoryTree);
      if (affectedCategoryIds.isEmpty()) {
        return;
      }

      List<Integer> pageIds = categoryDao.getPageIdsFromCategoryIds(affectedCategoryIds);
      if (pageIds == null || pageIds.isEmpty()) {
        return;
      }

      PubType pubType =
          "Staging".equalsIgnoreCase(deliveryserver) ? PubType.STAGE_NOW : PubType.PUBLISH_NOW;

      int publishedNow = 0;
      int queuedForLater = 0;
      for (Integer pageId : pageIds) {
        String itemId = idMapper.getString(new PSLegacyGuid(pageId, -1));
        try {
          sitePublishService.publish(null, pubType, itemId, false, null);
          publishedNow++;
        } catch (PSDataServiceException
            | IPSPubServerService.PSPubServerServiceException
            | IPSItemWorkflowService.PSItemWorkflowServiceException
            | IPSItemService.PSItemServiceException
            | PSNotFoundException
            | RuntimeException e) {
          // Immediate on-demand publish failed for this page (e.g. no PUBLISH_NOW/STAGE_NOW
          // edition configured for the site, or the page isn't in a workflow state that can be
          // auto-approved). Fall back to queueing it for the next scheduled incremental publish
          // rather than silently dropping the update.
          log.warn(
              "Could not immediately publish page {} after category publish to {} for site {}."
                  + " It will be queued for the next incremental publish instead. Error: {}",
              pageId,
              deliveryserver,
              sitename,
              e.getMessage());
          log.debug(e.getMessage(), e);
          if (queuePageForIncrementalPublish(pageId, sitename, deliveryserver)) {
            queuedForLater++;
          }
        }
      }
      log.info(
          "Category publish to {} for site {}: published {} page(s) immediately, queued {}"
              + " page(s) for the next incremental publish.",
          deliveryserver,
          sitename,
          publishedNow,
          queuedForLater);
    } catch (RuntimeException e) {
      // Never let republish failures block the category publish action itself - the DTS index
      // update already succeeded; log and move on. Affected pages can still be republished
      // manually if this best-effort step doesn't succeed.
      log.error(
          "Error republishing pages after category publish to {} for site {}. Error: {}",
          deliveryserver,
          sitename,
          e.getMessage());
      log.debug(e.getMessage(), e);
    }
  }

  /**
   * Fallback used when an immediate on-demand publish attempt fails for a page - marks the page
   * dirty so the site's next scheduled incremental publish run still picks it up, instead of the
   * category change being silently lost.
   *
   * @return <code>true</code> if the page was successfully queued.
   */
  private boolean queuePageForIncrementalPublish(
      int pageId, String sitename, String deliveryserver) {
    try {
      PSSiteSummary site = siteDataService.findByName(sitename);
      if (site == null || site.getSiteId() == null) {
        log.warn(
            "Could not resolve site id for site {}. Unable to queue page {} for incremental"
                + " publish either.",
            sitename,
            pageId);
        return false;
      }

      PSContentChangeType changeType =
          "Staging".equalsIgnoreCase(deliveryserver)
              ? PSContentChangeType.PENDING_STAGED
              : PSContentChangeType.PENDING_LIVE;

      PSContentChangeEvent changeEvent = new PSContentChangeEvent();
      changeEvent.setContentId(pageId);
      changeEvent.setSiteId(site.getSiteId());
      changeEvent.setChangeType(changeType);
      contentChangeService.contentChanged(changeEvent);
      return true;
    } catch (PSDataServiceException | RuntimeException e) {
      log.error("Failed to queue page {} for incremental publish: {}", pageId, e.getMessage());
      return false;
    }
  }

  /**
   * Validates the specified category. It validates the role object according to its annotation and
   * invokes {@link PSCategoryValidator validate(Object)} for additional validation.
   *
   * @param category the category in question, not <code>null</code>.
   * @throws PSBeanValidationException if failed to validate the specified role.
   */
  protected void doValidation(PSCategory category) throws PSValidationException {
    PSCategoryValidator validator = new PSCategoryValidator();

    validator.validate(category).throwIfInvalid();
  }

  /**
   * This is used to validate a {@link PSCategory} object before updating an existing category or
   * create a new one.
   */
  protected class PSCategoryValidator extends PSAbstractBeanValidator<PSCategory> {

    PSCategoryValidator() {}

    @Override
    protected void doValidation(PSCategory category, PSBeanValidationException e) {
      if (category != null && category.toJSON().contains("title\":\"\",")) {
        String msg = "Category Name Cannot be blank.";
        log.debug(msg);
        e.rejectValue("title", "category.invalidName", msg);
      }
    }
  }

  /**
   * @param pageIds
   * @return
   */
  private List<IPSGuid> getGuidsFromPageIds(List<Integer> pageIds) {
    List<IPSGuid> guids = new ArrayList<>();
    for (Integer id : pageIds) {
      try {
        IPSGuid guid = new PSLegacyGuid(id, -1);
        guids.add(guid);
      } catch (Exception e) {
        log.error("Error creating guid from id: {}", id);
      }
    }
    return guids;
  }
}
