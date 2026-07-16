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
import com.percussion.services.guidmgr.IPSGuidManager;
import com.percussion.services.guidmgr.data.PSLegacyGuid;
import com.percussion.share.service.exception.PSBeanValidationException;
import com.percussion.share.service.exception.PSDataServiceException;
import com.percussion.share.service.exception.PSParameterValidationUtils;
import com.percussion.share.service.exception.PSValidationException;
import com.percussion.share.validation.PSAbstractBeanValidator;
import com.percussion.sitemanage.data.PSSiteSummary;
import com.percussion.sitemanage.service.IPSSiteDataService;
import com.percussion.user.service.IPSUserService;
import com.percussion.utils.guid.IPSGuid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import java.nio.channels.OverlappingFileLockException;
import java.util.*;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
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
    if ("undefined".equals(sitename)) sitename = null;
    var category = getCategoryTreeForSite(sitename, null, false, true);
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

    var unMarshaller = new PSCategoryUnMarshaller();
    var category = unMarshaller.unMarshal();

    var node = findCategoryNode(category, sitename, rootPath, includeDeleted, includeNotSelectable);

    if (node == null) {
      category.setTopLevelNodes(new ArrayList<>());
      return category;
    }

    boolean saveCategories = createIds(node);

    if (node.getChildNodes() != null && !node.getChildNodes().isEmpty()) {
      category.setTopLevelNodes(node.getChildNodes());
    } else {
      category.setTopLevelNodes(new ArrayList<>());
    }

    if (saveCategories) updateCategories(category, sitename);

    return category;
  }

  private boolean createIds(PSCategoryNode node) {
    boolean createdIds = false;
    if (StringUtils.isEmpty(node.getId())) {
      var newId = PSCategoryServiceUtil.createGuid();
      log.info("Created new id for category {} {}", node.getTitle(), newId);
      node.setId(newId);
      createdIds = true;
    }
    for (var sub : node.getChildNodes()) createdIds |= createIds(sub);
    return createdIds;
  }

  @Override
  public PSCategoryNode findCategoryNode(
      String siteName, String rootPath, boolean includeDeleted, boolean includeNotSelectable) {
    var unMarshaller = new PSCategoryUnMarshaller();
    var category = unMarshaller.unMarshal();
    log.debug("Finding categoryNode with rootPath {} and site {}", rootPath, siteName);
    return findCategoryNode(category, siteName, rootPath, includeDeleted, includeNotSelectable);
  }

  private PSCategoryNode findCategoryNode(
      PSCategory category,
      String sitename,
      String rootPath,
      boolean includeDeleted,
      boolean includeNotSelectable) {
    var findPath = new LinkedList<String>();
    boolean relativePath = false;
    if (rootPath != null) {
      relativePath = (!rootPath.startsWith("/"));
      findPath =
          Arrays.stream(StringUtils.split(rootPath, "/"))
              .collect(Collectors.toCollection(LinkedList::new));
    }
    if (!findPath.isEmpty()) {
      var checkElement = findPath.peek();
      while (!findPath.isEmpty()
          && ("Categories".equals(checkElement) || StringUtils.isEmpty(checkElement))) {
        findPath.removeFirst();
        checkElement = findPath.peek();
      }
    }
    log.debug("Cleaned up parent search path = {}", findPath);

    var dummyRoot = new PSCategoryNode();
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
    return siteDataService.findAll().stream()
        .map(PSSiteSummary::getName)
        .collect(Collectors.toList());
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
      var mapper = new ObjectMapper();
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
      var mapper = new ObjectMapper();
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
    doValidation(category);
    PSParameterValidationUtils.rejectIfNull("update", "category", category);

    var unMarshaller = new PSCategoryUnMarshaller();
    var oldCategory = unMarshaller.unMarshal();

    if (oldCategory != null) {
      PSCategoryServiceUtil.preserveDeletedNodes(
          category.getTopLevelNodes(),
          oldCategory.getTopLevelNodes(),
          sitename,
          getActiveSiteNames());
    }

    var marshaller = new PSCategoryMarshaller();
    marshaller.setCategory(category);

    try {
      marshaller.marshal();
    } catch (OverlappingFileLockException e) {
      log.error(
          "Category XML is locked by another user ! - PSCategoryService.updateCategories()", e);
    }

    var updatedCategory = unMarshaller.unMarshal();

    if (updatedCategory == null) {
      log.error(
          "The updated categories are null ! - PSCategoryService.updateCategories()",
          new PSDataServiceException("Updated Categories are null"));
    } else if (updatedCategory.getTopLevelNodes() != null
        && !updatedCategory.getTopLevelNodes().isEmpty()) {
      var nodesToRemove =
          PSCategoryServiceUtil.removeDeletedNodes(
              updatedCategory.getTopLevelNodes(), new HashSet<>());
      if (!nodesToRemove.isEmpty()) {
        var pageIds = categoryDao.getPageIdsFromCategoryIds(nodesToRemove);
        var guids = getGuidsFromPageIds(pageIds);
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
        jsonObject.get("userName");
        return jsonObject.toString();
      } catch (JSONException e) {
        log.error(
            "JSON Exception occurred while reading from the json object -"
                + " PSCategoryService.getLockInfo()",
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
            "JSON Exception occurred while creating empty json object -"
                + " PSCategoryService.getLockInfo()",
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
        if (!jsonObject.get("userName").equals(userService.getCurrentUser().getName())) {
          PSCategoryLockInfo.removeLockInfo();
          PSCategoryLockInfo.writeLockInfoToFile(userService, date);
        } else if (!jsonObject.get("creationDate").equals(date)) {
          PSCategoryLockInfo.removeLockInfo();
          PSCategoryLockInfo.writeLockInfoToFile(userService, date);
        }
      } else {
        PSCategoryLockInfo.writeLockInfoToFile(userService, date);
      }
    } catch (JSONException | PSDataServiceException e) {
      log.error(
          "JSON Exception occurred while reading from the json object -"
              + " PSCategoryService.overrideCatTabLock()",
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
      @PathParam("sitename") String sitename, @PathParam("deliveryserver") String deliveryserver) {
    try {
      var category = PSCategoryServiceUtil.prepareCategoryJson(getCategoryList(sitename));
      if ("Both".equalsIgnoreCase(deliveryserver)) {
        PSCategoryServiceUtil.publishToDTS(category, sitename, "Production", deliveryService);
        PSCategoryServiceUtil.publishToDTS(category, sitename, "Staging", deliveryService);
      } else {
        PSCategoryServiceUtil.publishToDTS(category, sitename, deliveryserver, deliveryService);
      }
    } catch (PSDataServiceException e) {
      throw new WebApplicationException(e.getMessage());
    }
  }

  /**
   * Validates the specified category. It validates the role object according to its annotation and
   * invokes {@link PSCategoryValidator validate(Object)} for additional validation.
   *
   * @param category the category in question, not null.
   * @throws PSBeanValidationException if failed to validate the specified role.
   */
  protected void doValidation(PSCategory category) throws PSValidationException {
    var validator = new PSCategoryValidator();
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
        var msg = "Category Name Cannot be blank.";
        log.debug(msg);
        e.rejectValue("title", "category.invalidName", msg);
      }
    }
  }

  private List<IPSGuid> getGuidsFromPageIds(List<Integer> pageIds) {
    return pageIds.stream()
        .map(
            id -> {
              try {
                return new PSLegacyGuid(id, -1);
              } catch (Exception e) {
                log.error("Error creating guid from id: {}", id);
                return null;
              }
            })
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
  }
}
