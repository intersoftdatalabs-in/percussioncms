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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.apache.commons.lang.StringUtils;
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

    // Preserve category order between sites when only nodes for one site are returned to server.
    // Check for duplicate names,  merge.

    // Map id to path
    HashMap<String, PSCategoryNode> titleMap = new HashMap<>();
    HashMap<String, PSCategoryNode> idMap = new HashMap<>();

    for (PSCategoryNode category : oldCategories) {
      titleMap.put(category.getTitle(), category);
      idMap.put(category.getId(), category);
    }

    HashSet<String> newCategoryIds = new HashSet<>();

    checkAndMapIds(newCategories, titleMap, idMap, newCategoryIds);

    HashSet<String> processedIds = new HashSet<>();
    HashSet<String> processedTitles = new HashSet<>();

    Iterator<PSCategoryNode> oldCatIt = oldCategories.iterator();
    Iterator<PSCategoryNode> newCatIt = newCategories.iterator();

    ArrayList<PSCategoryNode> fullCategories = new ArrayList<>();

    PSCategoryNode nextOld = getNext(oldCatIt);
    PSCategoryNode nextNew = getNext(newCatIt);
    HashMap<String, PSCategoryNode> oldIdMap = new HashMap<>();

    while (nextOld != null || nextNew != null) {
      // Skip if category has no id (we have already added these ids to the new Items), if id has
      // already been processed, or we
      // are skipping remaining old items that no longer exist
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
        List<String> siteList = getAllowedSitesAsList(parentSites, nextOld);

        // handle categories not sent back.  If old category was valid for site it is
        // deleted otherwise we sequentially add
        if (!nextOld.isDeleted() && (site == null || site.equals("undefined"))
            || siteList.contains(site)) {
          // skip and move to next
          log.debug("Removing node that has been deleted or removed {}", nextOld.getId());
          nextOld.setDeleted(true);

        } else {
          log.debug("Merging back categories from other sites and deleted {}", nextOld.getId());
        }
        // merge in hidden node from other site
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

      // Check for duplicates
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

    //  process child items that map
    for (PSCategoryNode category : newCategories) {
      PSCategoryNode oldCategory = oldIdMap.get(category.getId());
      if (oldCategory != null) {
        List<String> siteList = getAllowedSitesAsList(parentSites, oldCategory);
        preserveDeletedNodes(category.getChildNodes(), oldCategory.getChildNodes(), site, siteList);
      }
    }

    // replace with new set of categories
    newCategories.clear();
    newCategories.addAll(fullCategories);
  }

  private static void checkAndMapIds(
      List<PSCategoryNode> newCategories,
      HashMap<String, PSCategoryNode> titleMap,
      HashMap<String, PSCategoryNode> idMap,
      HashSet<String> newCategoryIds) {
    Iterator<PSCategoryNode> newCatIt1 = newCategories.iterator();

    while (newCatIt1.hasNext()) {
      PSCategoryNode newCategory = newCatIt1.next();
      String id = newCategory.getId();
      String title = newCategory.getTitle();

      // Empty category ids should have been added before this point in
      // PSCategoryService.getCategoryTreeForSite
      if (StringUtils.isEmpty(id)) throw new RuntimeException("Category node does not have an id");
      PSCategoryNode existing = titleMap.get(title);
      if (!idMap.containsKey(id) && existing != null && existing.getId() != null) {
        // if there is an existing deleted item we adopt it.
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
      } else if (title.equals("Add Top Level Categories")) {
        newCategoryIds.remove(id);
        newCatIt1.remove();
      } else newCategoryIds.add(id);
    }
  }

  private static PSCategoryNode getNext(Iterator<PSCategoryNode> iterator) {
    return iterator.hasNext() ? iterator.next() : null;
  }

  public static String createGuid() {
    UUID uuid = UUID.randomUUID();
    return uuid.toString();
  }

  public static Set<String> removeDeletedNodes(
      List<PSCategoryNode> childNodes, Set<String> nodesToRemove) {

    log.debug("Total nodes for removal : {}", childNodes.size());

    Set<String> removedNodes = nodesToRemove;
    List<PSCategoryNode> tempList = new ArrayList<>();
    tempList.addAll(childNodes);

    for (PSCategoryNode node : tempList) {

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
    String categoryJson = null;

    categoryJson = PSCategoryMarshaller.marshalToJson(category);

    log.debug("Prepared Category Json is : {}", categoryJson);

    return categoryJson;
  }

  public static void publishToDTS(
      String category,
      String sitename,
      String deliveryServer,
      IPSDeliveryInfoService deliveryService)
      throws PSValidationException {

    PSDeliveryInfo server =
        deliveryService.findByService(PSDeliveryInfo.SERVICE_INDEXER, deliveryServer.toUpperCase());

    if (server == null)
      throw new PSDeliveryClientException(
          "The "
              + deliveryServer
              + " Server is not configured. Cannot perform the category publish. Please select the correct option.");

    log.debug("Server to publish the categories is {}", server.getServerType());

    PSDeliveryClient deliveryClient = new PSDeliveryClient();

    // Get a json array out from the category object for all the categories that were changed
    // (renamed, moved, or deleted) but were not yet published.
    String categories = getCategoriesForPublish(category);

    if (categories != null && !categories.equals("[]")) {
      log.debug(
          "Publishing category changes to {} for site {}: {}",
          deliveryServer,
          sitename,
          categories);
      try {
        net.sf.json.JSONObject response =
            deliveryClient.getJsonObject(
                new PSDeliveryActionOptions(
                    server,
                    CATEGORIES_UPDATE + sitename + "/" + deliveryServer,
                    HttpMethodType.POST,
                    true),
                categories);

        // The DTS always answers with HTTP 200 for this endpoint (a bad entry doesn't abort
        // the rest of the batch), but it does report per-entry success/failure counts in the
        // response body. Inspect those counts so a partial failure on the delivery server is
        // not misreported here as a full success.
        int failedCount = 0;
        try {
          if (response != null && response.has("failed")) {
            failedCount = response.getInt("failed");
          }
        } catch (RuntimeException e) {
          log.debug(
              "Could not parse success/failure counts from DTS category publish response: {}",
              e.getMessage());
        }

        if (failedCount > 0) {
          log.error(
              "Published category changes to {} server for site {}, but {} of the entries"
                  + " failed to apply on the delivery server. Check the delivery server log"
                  + " for details.",
              deliveryServer,
              sitename,
              failedCount);
        } else {
          log.info(
              "Successfully published category changes to {} server for site {}",
              deliveryServer,
              sitename);
        }
      } catch (RuntimeException e) {
        log.error(
            "Failed to publish category changes to {} server for site {}. Error: {}",
            deliveryServer,
            sitename,
            e.getMessage());
        log.debug(e.getMessage(), e);
        throw e;
      }
    } else {
      log.info(
          "No category changes to publish to {} for site {}.  Note that categories with no"
              + " pages assigned to them yet will not appear on the delivery server until a page"
              + " using them is published.",
          deliveryServer,
          sitename);
      PSValidationErrorsBuilder builder = validateParameters("publishToDTS");
      builder
          .reject(
              "no.categories.to.publish",
              "There are no recently edited categories to publish.  A category should be edited before publishing.")
          .throwIfInvalid();
    }
  }

  /**
   * Collects the ids of every category node that was renamed, moved or deleted in the given
   * category tree, plus the ids of any of that node's descendants - a descendant's own resolved
   * label path also changes whenever an ancestor's title changes or an ancestor is removed, even
   * though the descendant's own title and id are unchanged. Used to determine which
   * already-published pages must be queued for republish so their baked-in category label reflects
   * the change, since publishing category changes to a delivery server only updates the delivery
   * tier's category index/browse widget - it does not regenerate any previously published page
   * HTML.
   *
   * @param category the just-published category tree, may be <code>null</code>.
   * @return a (possibly empty) set of affected category ids, never <code>null</code>.
   */
  public static Set<String> getAffectedCategoryIds(PSCategory category) {
    Set<String> affectedIds = new HashSet<>();
    if (category == null || category.getTopLevelNodes() == null) {
      return affectedIds;
    }
    collectAffectedCategoryIds(category.getTopLevelNodes(), affectedIds, false);
    return affectedIds;
  }

  private static void collectAffectedCategoryIds(
      List<PSCategoryNode> nodes, Set<String> affectedIds, boolean ancestorChanged) {
    if (nodes == null) {
      return;
    }

    for (PSCategoryNode node : nodes) {
      boolean renamedOrMoved =
          StringUtils.isNotBlank(node.getPreviousCategoryName())
              && StringUtils.isNotBlank(node.getTitle())
              && !node.getPreviousCategoryName().equals(node.getTitle());
      boolean changed = ancestorChanged || renamedOrMoved || node.isDeleted();

      if (changed && StringUtils.isNotBlank(node.getId())) {
        affectedIds.add(node.getId());
      }

      collectAffectedCategoryIds(node.getChildNodes(), affectedIds, changed);
    }
  }

  private static String getCategoriesForPublish(String categoryString) {

    String forPublish = null;
    PSCategory category = PSCategoryUnMarshaller.unMarshalFromString(categoryString);
    log.debug("Getting categories for publish.");

    // traverse the category object to find where the title is different than the
    // previousCategoryName and publish date is not same as last modified date.
    List<PSCategoryNode> topCategories = category.getTopLevelNodes();

    if (topCategories != null && !topCategories.isEmpty()) {

      JSONArray combined =
          findModifiedCategories(topCategories, "/" + category.getTitle(), null, false);

      // Renamed/moved categories are captured above (via previousCategoryName vs title).
      // Deleted categories are never captured by findModifiedCategories, since it never
      // inspects the "deleted" flag. Collect those separately so a category deletion is
      // actually propagated to the DTS instead of silently being dropped.
      JSONArray deleted = collectDeletedCategoryPaths(topCategories, "/" + category.getTitle());

      for (int i = 0; i < deleted.length(); i++) {
        try {
          combined.put(deleted.get(i));
        } catch (JSONException e) {
          log.error(
              "Error occurred while merging deleted categories into publish payload - PSCategoryServiceUtil.getCategoriesForPublish()",
              e);
        }
      }

      forPublish = combined.toString();
    }

    return forPublish;
  }

  /**
   * Recursively walks the category tree collecting the full path of every node currently marked as
   * {@code deleted}, regardless of whether it was renamed. Each entry is emitted as {@code
   * {"previousCategoryName": "<fullPath>", "deleted": true}} so the DTS can remove any metadata
   * already indexed under that category path, without requiring the pages that reference it to be
   * republished.
   *
   * @param categories the nodes to inspect, not <code>null</code>.
   * @param pathPrefix the resolved path of the parent of {@code categories}, not <code>null</code>
   *     .
   * @return a (possibly empty) {@link JSONArray} of delete instructions, never <code>null</code>.
   */
  private static JSONArray collectDeletedCategoryPaths(
      List<PSCategoryNode> categories, String pathPrefix) {

    JSONArray jsonArray = new JSONArray();

    if (categories == null || categories.isEmpty()) {
      return jsonArray;
    }

    for (PSCategoryNode node : categories) {
      String fullPath = pathPrefix + "/" + node.getTitle();

      if (node.isDeleted()) {
        try {
          JSONObject obj = new JSONObject();
          obj.put("previousCategoryName", fullPath);
          obj.put("deleted", true);
          jsonArray.put(obj);
        } catch (JSONException e) {
          log.error(
              "Error occurred while creating json object for deleted category to be published. - PSCategoryServiceUtil.collectDeletedCategoryPaths()",
              e);
        }
      }

      if (node.getChildNodes() != null && !node.getChildNodes().isEmpty()) {
        JSONArray childDeletes = collectDeletedCategoryPaths(node.getChildNodes(), fullPath);
        for (int i = 0; i < childDeletes.length(); i++) {
          try {
            jsonArray.put(childDeletes.get(i));
          } catch (JSONException e) {
            log.error(
                "Error occurred while merging child deleted categories - PSCategoryServiceUtil.collectDeletedCategoryPaths()",
                e);
          }
        }
      }
    }

    return jsonArray;
  }

  private static JSONArray findModifiedCategories(
      List<PSCategoryNode> categories,
      String oldPrefix,
      String newPrefix,
      boolean hasParentChanged) {

    JSONArray jsonArray = new JSONArray();
    boolean thisParentChanged = false;
    log.debug("Finding modified categories.");

    try {
      for (PSCategoryNode parent : categories) {
        JSONObject obj = new JSONObject();
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
          JSONArray temp = null;

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
          "Error occurred while creating json object for category to be published. - PSCategoryServiceUtil.getCategoriesForPublish()",
          e);
    }

    return jsonArray;
  }

  private static List<String> getAllowedSitesAsList(List<String> parentSites, PSCategoryNode node) {
    List<String> nodeAllowedSites = new ArrayList<>();
    if (node.getAllowedSites() != null) {
      for (String string : StringUtils.split(node.getAllowedSites(), ",")) {
        nodeAllowedSites.add(string.trim());
      }
    }
    if (nodeAllowedSites.isEmpty()) nodeAllowedSites = parentSites;
    else if (parentSites != null) {
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

    List<String> nodeAllowedSites = getAllowedSitesAsList(allowedSites, currentNode);
    if (StringUtils.isNotEmpty(sitename) && !nodeAllowedSites.contains(sitename)) return null;

    String pathElement = null;
    PSCategoryNode foundNode = null;
    if (findPath != null && !findPath.isEmpty()) {
      if (!StringUtils.equals(currentNode.getId(), DUMMYROOT)) {
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
        for (PSCategoryNode child : currentNode.getChildNodes()) {
          PSCategoryNode testNode =
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
      // else found node so continue to process
    }

    // don't filter if this is a root element
    if (!StringUtils.equals(currentNode.getId(), DUMMYROOT)
        && pathElement == null
        && !includeNotSelectable
        && !currentNode.isSelectable()) return null;

    List<PSCategoryNode> filteredChildList = new ArrayList<>();
    for (PSCategoryNode child : currentNode.getChildNodes()) {
      PSCategoryNode testNode =
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
