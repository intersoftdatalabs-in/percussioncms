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

package com.percussion.category.service.impl;

import static com.percussion.share.service.exception.PSParameterValidationUtils.validateParameters;

import com.percussion.category.data.PSCategory;
import com.percussion.category.data.PSCategoryNode;
import com.percussion.category.marshaller.PSCategoryMarshaller;
import com.percussion.category.marshaller.PSCategoryUnMarshaller;
import com.percussion.delivery.client.IPSDeliveryClient.HttpMethodType;
import com.percussion.delivery.client.IPSDeliveryClient.PSDeliveryActionOptions;
import com.percussion.delivery.client.IPSDeliveryClient.PSDeliveryClientException;
import com.percussion.delivery.client.PSDeliveryClient;
import com.percussion.delivery.data.PSDeliveryInfo;
import com.percussion.delivery.service.IPSDeliveryInfoService;
import com.percussion.share.service.exception.PSValidationException;
import com.percussion.share.validation.PSValidationErrorsBuilder;
import java.util.*;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

public class PSCategoryServiceUtil {
  public static final String DUMMYROOT = "dummyroot";
  private static final Logger log = LogManager.getLogger(PSCategoryServiceUtil.class);
  private static final String CATEGORIES_UPDATE =
      "perc-metadata-services/metadata/categories/update/";

  public static void preserveDeletedNodes(
      List<PSCategoryNode> newCategories,
      List<PSCategoryNode> oldCategories,
      String site,
      List<String> parentSites) {
    var titleMap = new HashMap<String, PSCategoryNode>();
    var idMap = new HashMap<String, PSCategoryNode>();
    for (var category : oldCategories) {
      titleMap.put(category.getTitle(), category);
      idMap.put(category.getId(), category);
    }
    var newCategoryIds = new HashSet<String>();
    checkAndMapIds(newCategories, titleMap, idMap, newCategoryIds);

    var processedIds = new HashSet<String>();
    var processedTitles = new HashSet<String>();
    var oldCatIt = oldCategories.iterator();
    var newCatIt = newCategories.iterator();
    var fullCategories = new ArrayList<PSCategoryNode>();
    PSCategoryNode nextOld = getNext(oldCatIt);
    PSCategoryNode nextNew = getNext(newCatIt);
    var oldIdMap = new HashMap<String, PSCategoryNode>();

    while (nextOld != null || nextNew != null) {
      if (nextOld != null && (nextOld.getId() == null || processedIds.contains(nextOld.getId()))) {
        log.debug("Skipping processed nextOld {}", nextOld.getTitle());
        nextOld = getNext(oldCatIt);
        continue;
      }
      if (nextNew != null && processedIds.contains(nextNew.getId())) {
        log.debug("Skipping processed nextNew {}", nextNew.getTitle());
        nextNew = getNext(newCatIt);
        continue;
      }
      PSCategoryNode processedItem = null;
      if (nextOld != null
          && (nextOld.getId() == null || !newCategoryIds.contains(nextOld.getId()))) {
        var siteList = getAllowedSitesAsList(parentSites, nextOld);
        if (!nextOld.isDeleted() && (site == null || site.equals("undefined"))
            || siteList.contains(site)) {
          log.debug("Removing node that has been deleted or removed {}", nextOld.getId());
          nextOld.setDeleted(true);
        } else {
          log.debug("Merging back categories from other sites and deleted {}", nextOld.getId());
        }
        processedItem = nextOld;
        fullCategories.add(nextOld);
        nextOld = getNext(oldCatIt);
      } else if (nextNew != null) {
        log.debug("adding sent category: {}", nextNew.getId());
        fullCategories.add(nextNew);
        processedItem = nextNew;
        if (nextOld != null && nextOld.getId().equalsIgnoreCase(nextNew.getId()))
          nextOld = getNext(oldCatIt);
        nextNew = getNext(newCatIt);
      }
      if (processedItem != null) {
        if (processedIds.contains(processedItem.getId()))
          throw new IllegalArgumentException(
              "Trying to add a duplicate id " + processedItem.getId());
        processedIds.add(processedItem.getId());
        if (!processedItem.isDeleted()) {
          if (processedTitles.contains(processedItem.getTitle()))
            throw new IllegalArgumentException(
                "Trying to add a duplicate title " + processedItem.getTitle());
          processedTitles.add(processedItem.getTitle());
        }
      }
    }
    for (var category : newCategories) {
      var oldCategory = oldIdMap.get(category.getId());
      if (oldCategory != null) {
        var siteList = getAllowedSitesAsList(parentSites, oldCategory);
        preserveDeletedNodes(category.getChildNodes(), oldCategory.getChildNodes(), site, siteList);
      }
    }
    newCategories.clear();
    newCategories.addAll(fullCategories);
  }

  private static void checkAndMapIds(
      List<PSCategoryNode> newCategories,
      Map<String, PSCategoryNode> titleMap,
      Map<String, PSCategoryNode> idMap,
      Set<String> newCategoryIds) {
    var newCatIt1 = newCategories.iterator();
    while (newCatIt1.hasNext()) {
      var newCategory = newCatIt1.next();
      var id = newCategory.getId();
      var title = newCategory.getTitle();
      if (StringUtils.isEmpty(id)) throw new RuntimeException("Category node does not have an id");
      var existing = titleMap.get(title);
      if (!idMap.containsKey(id) && existing != null && existing.getId() != null) {
        id = existing.getId();
        newCategory.setCreatedBy(existing.getCreatedBy());
        newCategory.setCreationDate(existing.getCreationDate());
        newCategory.setId(existing.getId());
        newCategory.setChildNodes(existing.getChildNodes());
        newCategory.setId(id);
      }
      if (newCategoryIds.contains(id)) {
        log.warn("Duplicate id {} passed in category update skipping", id);
        newCatIt1.remove();
      } else if ("Add Top Level Categories".equals(title)) {
        newCategoryIds.remove(id);
        newCatIt1.remove();
      } else newCategoryIds.add(id);
    }
  }

  private static PSCategoryNode getNext(Iterator<PSCategoryNode> iterator) {
    return iterator.hasNext() ? iterator.next() : null;
  }

  public static String createGuid() {
    return UUID.randomUUID().toString();
  }

  public static Set<String> removeDeletedNodes(
      List<PSCategoryNode> childNodes, Set<String> nodesToRemove) {
    log.debug("Total nodes for removal : {}", childNodes.size());
    var removedNodes = nodesToRemove;
    var tempList = new ArrayList<>(childNodes);
    for (var node : tempList) {
      if (node.isDeleted()) {
        nodesToRemove.add(node.getId());
        childNodes.remove(node);
      } else if (node.getChildNodes() != null && !node.getChildNodes().isEmpty()) {
        removeDeletedNodes(node.getChildNodes(), nodesToRemove);
      }
    }
    return nodesToRemove;
  }

  public static String prepareCategoryJson(PSCategory category) {
    var categoryJson = PSCategoryMarshaller.marshalToJson(category);
    log.debug("Prepared Category Json is : {}", categoryJson);
    return categoryJson;
  }

  public static void publishToDTS(
      String category,
      String sitename,
      String deliveryServer,
      IPSDeliveryInfoService deliveryService)
      throws PSValidationException {
    var server =
        deliveryService.findByService(PSDeliveryInfo.SERVICE_INDEXER, deliveryServer.toUpperCase());
    if (server == null)
      throw new PSDeliveryClientException(
          "The "
              + deliveryServer
              + " Server is not configured. Cannot perform the category publish. Please select the"
              + " correct option.");
    log.debug("Server to publish the categories is {}", server.getServerType());
    var deliveryClient = new PSDeliveryClient();
    var categories = getCategoriesForPublish(category);
    if (categories != null && !"[]".equals(categories))
      deliveryClient
          .getJsonObject(
              new PSDeliveryActionOptions(
                  server,
                  CATEGORIES_UPDATE + sitename + "/" + deliveryServer,
                  HttpMethodType.POST,
                  true),
              categories)
          .toString();
    else {
      PSValidationErrorsBuilder builder = validateParameters("publishToDTS");
      builder
          .reject(
              "no.categories.to.publish",
              "There are no recently edited categories to publish.  A category should be edited"
                  + " before publishing.")
          .throwIfInvalid();
    }
  }

  /**
   * Builds the JSON array string of category path renames to push to DTS.
   *
   * <p>Package-visible for unit tests. Returns {@code "[]"} when nothing was renamed, or {@code
   * null} when the category payload cannot be read.
   *
   * @param categoryString marshalled category JSON; may be blank
   * @return JSON array string of {@code previousCategoryName}/{@code title} pairs, or null
   */
  static String getCategoriesForPublish(String categoryString) {
    String forPublish = null;
    var category = PSCategoryUnMarshaller.unMarshalFromString(categoryString);
    if (category == null) {
      return null;
    }
    log.debug("Getting categories for publish.");
    var topCategories = category.getTopLevelNodes();
    if (topCategories != null && !topCategories.isEmpty()) {
      String treeTitle = StringUtils.defaultIfBlank(category.getTitle(), "Categories");
      forPublish =
          findModifiedCategories(topCategories, "/" + treeTitle, null, false).toString();
    }
    return forPublish;
  }

  /**
   * Walks the category tree and collects rename pairs for DTS {@code perc:category} updates.
   *
   * @param categories siblings at the current level; never null
   * @param oldPrefix path prefix using previous names for ancestors
   * @param newPrefix path prefix using new titles for renamed ancestors; may be null
   * @param hasParentChanged true when an ancestor was renamed
   * @return JSON array of rename objects (may be empty)
   */
  static JSONArray findModifiedCategories(
      List<PSCategoryNode> categories,
      String oldPrefix,
      String newPrefix,
      boolean hasParentChanged) {
    var jsonArray = new JSONArray();
    log.debug("Finding modified categories.");
    try {
      for (var parent : categories) {
        // Must reset per sibling; previously a rename on one node incorrectly
        // treated every following sibling as renamed (and broke child paths).
        boolean thisParentChanged = false;
        var obj = new JSONObject();
        if (StringUtils.isNotBlank(parent.getPreviousCategoryName())
            && StringUtils.isNotBlank(parent.getTitle())) {
          thisParentChanged = true;
          if (hasParentChanged) {
            obj.put("previousCategoryName", oldPrefix + "/" + parent.getPreviousCategoryName());
            obj.put("title", newPrefix + "/" + parent.getTitle());
          } else {
            obj.put("previousCategoryName", oldPrefix + "/" + parent.getPreviousCategoryName());
            obj.put("title", oldPrefix + "/" + parent.getTitle());
          }
          jsonArray.put(obj);
          if (StringUtils.isBlank(newPrefix)) newPrefix = oldPrefix;
        } else {
          if (hasParentChanged) {
            obj.put("previousCategoryName", oldPrefix + "/" + parent.getTitle());
            obj.put("title", newPrefix + "/" + parent.getTitle());
            jsonArray.put(obj);
          }
        }
        if (parent.getChildNodes() != null && !parent.getChildNodes().isEmpty()) {
          JSONArray temp;
          if (thisParentChanged)
            temp =
                findModifiedCategories(
                    parent.getChildNodes(),
                    oldPrefix + "/" + parent.getPreviousCategoryName(),
                    newPrefix + "/" + parent.getTitle(),
                    true);
          else {
            if (hasParentChanged)
              temp =
                  findModifiedCategories(
                      parent.getChildNodes(),
                      oldPrefix + "/" + parent.getTitle(),
                      newPrefix + "/" + parent.getTitle(),
                      true);
            else
              temp =
                  findModifiedCategories(
                      parent.getChildNodes(),
                      oldPrefix + "/" + parent.getTitle(),
                      oldPrefix + "/" + parent.getTitle(),
                      false);
          }
          for (int i = 0; i < temp.length(); i++) {
            jsonArray.put(temp.get(i));
          }
        }
      }
    } catch (JSONException e) {
      log.error(
          "Error occurred while creating json object for category to be published. -"
              + " PSCategoryServiceUtil.getCategoriesForPublish()",
          e);
    }
    return jsonArray;
  }

  private static List<String> getAllowedSitesAsList(List<String> parentSites, PSCategoryNode node) {
    var nodeAllowedSites = new ArrayList<String>();
    if (node.getAllowedSites() != null) {
      nodeAllowedSites.addAll(
          Arrays.stream(StringUtils.split(node.getAllowedSites(), ","))
              .map(String::trim)
              .collect(Collectors.toList()));
    }
    if (nodeAllowedSites.isEmpty()) {
      nodeAllowedSites = parentSites != null ? new ArrayList<>(parentSites) : new ArrayList<>();
    } else if (parentSites != null) {
      nodeAllowedSites.retainAll(parentSites);
    }
    return nodeAllowedSites;
  }

  public static PSCategoryNode filterForSite(
      PSCategoryNode currentNode,
      String sitename,
      LinkedList<String> findPath,
      List<String> allowedSites,
      boolean relativePath,
      boolean includeDeleted,
      boolean includeNotSelectable) {
    if (!includeDeleted && currentNode.isDeleted()) return null;
    var nodeAllowedSites = getAllowedSitesAsList(allowedSites, currentNode);
    if (StringUtils.isNotEmpty(sitename) && !nodeAllowedSites.contains(sitename)) return null;
    String pathElement = null;
    PSCategoryNode foundNode = null;
    if (findPath != null && !findPath.isEmpty()) {
      if (!DUMMYROOT.equals(currentNode.getId())) {
        pathElement = findPath.peekFirst();
        boolean matches = testTitleOrId(pathElement, currentNode);
        if (matches) {
          relativePath = false;
          findPath.removeFirst();
        } else if (!relativePath) {
          return null;
        }
      }
      if (!findPath.isEmpty()) {
        for (var child : currentNode.getChildNodes()) {
          var testNode =
              filterForSite(
                  child,
                  sitename,
                  findPath,
                  nodeAllowedSites,
                  relativePath,
                  includeDeleted,
                  includeNotSelectable);
          if (testNode != null) {
            foundNode = testNode;
            break;
          }
        }
        return foundNode;
      }
    }
    if (!DUMMYROOT.equals(currentNode.getId())
        && pathElement == null
        && !includeNotSelectable
        && !currentNode.isSelectable()) return null;
    var filteredChildList = new ArrayList<PSCategoryNode>();
    for (var child : currentNode.getChildNodes()) {
      var testNode =
          filterForSite(
              child,
              sitename,
              null,
              nodeAllowedSites,
              relativePath,
              includeDeleted,
              includeNotSelectable);
      if (testNode != null) {
        filteredChildList.add(testNode);
      }
    }
    currentNode.setChildNodes(filteredChildList);
    return currentNode;
  }

  private static boolean testTitleOrId(String checkString, PSCategoryNode node) {
    return StringUtils.equals(node.getTitle(), checkString)
        || StringUtils.equals(node.getId(), checkString);
  }
}
