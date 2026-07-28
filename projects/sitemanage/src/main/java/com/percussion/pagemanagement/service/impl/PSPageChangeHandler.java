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
package com.percussion.pagemanagement.service.impl;

import static com.percussion.share.spring.PSSpringWebApplicationContextUtils.getWebApplicationContext;

import com.percussion.assetmanagement.service.IPSWidgetAssetRelationshipService;
import com.percussion.assetmanagement.service.impl.PSWidgetAssetRelationshipService;
import com.percussion.itemmanagement.service.impl.PSItemWorkflowService;
import com.percussion.pagemanagement.data.PSPageChangeEvent;
import com.percussion.pagemanagement.data.PSPageChangeEvent.PSPageChangeEventType;
import com.percussion.pagemanagement.service.IPSPageChangeListener;
import com.percussion.rest.Guid;
import com.percussion.security.error.PSExceptionUtils;
import com.percussion.services.legacy.IPSCmsObjectMgrInternal;
import com.percussion.services.legacy.PSCmsObjectMgrLocator;
import com.percussion.services.notification.PSNotificationEvent;
import com.percussion.services.notification.PSNotificationEvent.EventType;
import com.percussion.services.notification.PSNotificationServiceLocator;
import com.percussion.share.dao.IPSContentItemDao;
import com.percussion.share.dao.impl.PSContentItem;
import com.percussion.share.service.exception.PSDataServiceException;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * This class implements the {@link IPSPageChangeListener} interface and gets notified when a page
 * is changed. It currently handles the title updates and page summary updates.
 *
 * @author BJoginipally
 */
public class PSPageChangeHandler implements IPSPageChangeListener {
  // Initialized on the first call to the #pageChanged() method.
  private IPSContentItemDao contentItemDao;
  // Initialized on the first call to the #pageChanged() method.
  private PSWidgetAssetRelationshipService widgetAssetRelationshipService;

  private final IPSCmsObjectMgrInternal cmsObjectMgr =
      (IPSCmsObjectMgrInternal) PSCmsObjectMgrLocator.getObjectManager();

  private static final String MORE_LINK_TEXT = "<span class=\"perc-blog-more-link\"></span>";

  // Constants for page and title widget fields
  private static final String TITLE_WIDGET_SYNC_FIELD_NAME = "sync_link_text";
  private static final String TITLE_WIDGET_SYNC = "1";
  private static final String PAGE_LINK_TEXT_FIELD_NAME = "resource_link_title";
  private static final String TITLE_WIDGET_TYPE = "percTitleAsset";
  private static final String TITLE_WIDGET_TITLE_FIELD_NAME = "text";

  // Constants for blog post asset fields
  private static final String BLOG_POST_ASSET_TYPE = "percBlogPostAsset";
  private static final String BLOG_POST_WIDGET_TITLE = "displaytitle";

  // Constants for page and summary fields
  private static final String PAGE_SUMMARY_FIELD_NAME = "page_summary";
  private static final String PAGE_SUMMARY_GEN_FIELD_NAME = "auto_generate_summary";
  private static final String AUTO_GENERATE_SUMMARY = "1";
  private static final String PAGE_AUTHOR_FIELD_NAME = "page_authorname";

  /** A map of content type name and a more link capable field name. */
  private static final Map<String, String> moreLinkSupportTypes = new HashMap<>();

  static {
    moreLinkSupportTypes.put("percRichTextAsset", "text");
    moreLinkSupportTypes.put(BLOG_POST_ASSET_TYPE, "postbody");
  }

  private static final Map<String, String> authorSupportedTypes = new HashMap<>();

  static {
    authorSupportedTypes.put("percBlogPostAsset", "authorname");
  }

  /** Logger for this class */
  public static final Logger log = LogManager.getLogger(PSPageChangeHandler.class);

  public PSPageChangeHandler() {
    // Default constructor
  }

  @Override
  public void pageChanged(PSPageChangeEvent pageChangeEvent) {
    if (contentItemDao == null) {
      contentItemDao = (IPSContentItemDao) getWebApplicationContext().getBean("contentItemDao");
    }
    if (widgetAssetRelationshipService == null) {
      widgetAssetRelationshipService =
          (PSWidgetAssetRelationshipService)
              getWebApplicationContext().getBean("widgetAssetRelationshipService");
    }

    var pageId = pageChangeEvent.getPageId();
    var itemId = pageChangeEvent.getItemId();
    var type = pageChangeEvent.getType();

    if ((type == PSPageChangeEventType.ITEM_ADDED
            || type == PSPageChangeEventType.ITEM_SAVED
            || type == PSPageChangeEventType.ITEM_REMOVED)
        && StringUtils.isBlank(itemId)) {
      throw new IllegalArgumentException("itemId must not be blank for item events");
    }

    PSContentItem page;
    try {
      page = contentItemDao.find(pageId);
      if (page == null) {
        throw new Exception("Unable to find Page with id " + pageId);
      }
    } catch (Exception e) {
      log.error(
          "Error while finding the Page with the pageId {} in pageChanged Event Handler.",
          pageId,
          e);
      return;
    }

    PSContentItem asset = null;
    if (!type.equals(PSPageChangeEventType.ITEM_REMOVED) && itemId != null) {
      try {
        asset = contentItemDao.find(itemId);
        if (asset == null) {
          throw new Exception("Unable to find Asset with id " + itemId);
        }
      } catch (Exception e) {
        log.error(
            "Error while finding the Asset with the itemId {} in pageChanged Event Handler.  Error:"
                + " {}",
            itemId,
            e.getMessage());
        log.debug(PSExceptionUtils.getDebugMessageForLog(e));
      }
    }

    if ((type == PSPageChangeEventType.ITEM_ADDED || type == PSPageChangeEventType.ITEM_SAVED)
        && asset != null) {
      updateLinkText(page, asset);
    }

    if (type == PSPageChangeEventType.PAGE_META_DATA_SAVED) {
      updateBlogPostWidgetTitle(page);
    }

    if (asset != null) {
      updateAuthor(page, asset);
    }
    updateSummary(page);

    // send generic content-changed notification since specific page/template events were removed
    var notifyEvent = new PSNotificationEvent(EventType.CONTENT_CHANGED, page.getId());
    var srv = PSNotificationServiceLocator.getNotificationService();
    srv.notifyEvent(notifyEvent);
  }

  /**
   * Gets all the widgets in the page and find the blog post widget. If the page has one, it updates
   * its title. If it doesn't, just return.
   *
   * @param page The page where the metadata has been changed.
   */
  private void updateBlogPostWidgetTitle(PSContentItem page) {
    try {
      var assets = widgetAssetRelationshipService.getLocalAssets(page.getId());
      var workFlowService =
          (PSItemWorkflowService) getWebApplicationContext().getBean("workflowRestService");

      if (assets != null) {
        for (var assetId : assets) {
          try {
            var asset = contentItemDao.find(assetId);
            if (BLOG_POST_ASSET_TYPE.equals(asset.getType())) {
              var pageFields = page.getFields();
              var pageTitle = (String) pageFields.get(PAGE_LINK_TEXT_FIELD_NAME);

              var assetFields = asset.getFields();
              if (assetFields.containsKey(BLOG_POST_WIDGET_TITLE)) {
                assetFields.put(BLOG_POST_WIDGET_TITLE, pageTitle);

                if (!workFlowService.isCheckedOutToCurrentUser(asset.getId())) {
                  workFlowService.checkOut(asset.getId());
                }

                contentItemDao.save(asset);
              }
              break;
            }
          } catch (PSDataServiceException e) {
            log.warn("Error updating Linked Title. Error:{}", PSExceptionUtils.getMessageForLog(e));
            log.debug(PSExceptionUtils.getDebugMessageForLog(e));
          }
        }
      }
    } catch (IPSWidgetAssetRelationshipService.PSWidgetAssetRelationshipServiceException e) {
      log.warn("Error updating Linked Title. Error:{}", PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
    }
  }

  /**
   * Check if the asset is a blog post widget, and then updates the page title. If it is not, and
   * this asset is a title widget, check for the sync_link_text field on the asset and if it exists
   * and equal to "1" then updates resource_link_title field value on the page and deletes the asset
   * other wise just return
   */
  private void updateLinkText(PSContentItem page, PSContentItem asset) {
    try {
      var assetType = asset.getType();

      if (assetType.equalsIgnoreCase(BLOG_POST_ASSET_TYPE)) {
        var assetFields = asset.getFields();
        var assetTitle = (String) assetFields.get(BLOG_POST_WIDGET_TITLE);

        var pageFields = page.getFields();
        if (pageFields.containsKey(PAGE_LINK_TEXT_FIELD_NAME)) {
          pageFields.put(PAGE_LINK_TEXT_FIELD_NAME, assetTitle);
          contentItemDao.save(page);
        }
      } else if (assetType.equalsIgnoreCase(TITLE_WIDGET_TYPE)) {
        var assetFields = asset.getFields();
        var syncValue = (String) assetFields.get(TITLE_WIDGET_SYNC_FIELD_NAME);
        if (!TITLE_WIDGET_SYNC.equals(syncValue)) {
          return;
        }

        var assetTitle = (String) assetFields.get(TITLE_WIDGET_TITLE_FIELD_NAME);
        var pageFields = page.getFields();
        if (pageFields.containsKey(PAGE_LINK_TEXT_FIELD_NAME)) {
          pageFields.put(PAGE_LINK_TEXT_FIELD_NAME, assetTitle);
          contentItemDao.save(page);
          contentItemDao.remove(asset.getId());
        }
        updateBlogPostWidgetTitle(page);
      }
    } catch (PSDataServiceException e) {
      log.error(PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
    }
  }

  /**
   * Updates the summary of the page, if the page's auto generate summary field is checked then
   * updates the page summary by getting the page summary from the first rich text asset that has
   * more link in it.
   */
  private void updateSummary(PSContentItem page) {
    try {
      var pageFields = page.getFields();
      var autoGen = (String) pageFields.get(PAGE_SUMMARY_GEN_FIELD_NAME);
      if (!AUTO_GENERATE_SUMMARY.equals(autoGen)) {
        return;
      }
      var newSummary = generatePageSummary(page.getId());
      if (pageFields.containsKey(PAGE_SUMMARY_FIELD_NAME)) {
        var intg = (new Guid(page.getId())).getUuid();
        // getFirstPublishDate returns Optional — never put Optional.toString()
        // ("Optional.empty") into date fields; PSDateValue rejects that format.
        var postDate = cmsObjectMgr.getFirstPublishDate(intg);
        if (page.getFields() != null
            && page.getFields().get("sys_contentpostdate") == null
            && postDate != null
            && postDate.isPresent()) {
          page.getFields().put("sys_contentpostdate", postDate.get().toString());
        }
        pageFields.put(PAGE_SUMMARY_FIELD_NAME, newSummary);
        contentItemDao.save(page);
      }
    } catch (PSDataServiceException e) {
      log.warn("Error update Page summary for Page: {} Error: {}", page.getId(), e.getMessage());
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
    }
  }

  /** Updates the author of the page, if the supplied asset type supports the author. */
  private void updateAuthor(PSContentItem page, PSContentItem asset) {
    try {
      var assetType = asset.getType();
      if (authorSupportedTypes.containsKey(assetType)) {
        var assetFields = asset.getFields();
        var authorFieldName = authorSupportedTypes.get(assetType);
        var author = (String) assetFields.get(authorFieldName);
        var pageFields = page.getFields();
        if (pageFields.containsKey(PAGE_AUTHOR_FIELD_NAME)) {
          pageFields.put(PAGE_AUTHOR_FIELD_NAME, author);
          contentItemDao.save(page);
        }
      }
    } catch (PSDataServiceException e) {
      log.warn("Error update Author for Page: {} Error: {}", page.getId(), e.getMessage());
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
    }
  }

  /**
   * Helper method that generates the page summary. Gets local assets and shared assets of the page
   * and from the first rich text field grabs the text from the beginning to the first more link
   * represented by <span class="perc-blog-more-link"></span>. Stops processing after finding first
   * rich text asset that has more link.
   */
  private String generatePageSummary(String pageId) {
    var summary = "";

    try {
      var assetIds = widgetAssetRelationshipService.getLocalAssets(pageId);
      assetIds.addAll(widgetAssetRelationshipService.getSharedAssets(pageId));
      for (var assetId : assetIds) {
        try {
          var asset = contentItemDao.find(assetId, false);
          if (asset != null && moreLinkSupportTypes.containsKey(asset.getType())) {
            var assetFields = asset.getFields();
            var text = (String) assetFields.get(moreLinkSupportTypes.get(asset.getType()));
            var moreIndex = text.indexOf(MORE_LINK_TEXT);
            if (moreIndex != -1) {
              summary = text.substring(0, moreIndex + MORE_LINK_TEXT.length());
              break;
            }
          }
        } catch (PSDataServiceException e) {
          log.warn(PSExceptionUtils.getMessageForLog(e));
          log.debug(PSExceptionUtils.getDebugMessageForLog(e));
        }
      }
    } catch (IPSWidgetAssetRelationshipService.PSWidgetAssetRelationshipServiceException e) {
      log.warn("Error generating Page summary for Page: {} Error: {}", pageId, e.getMessage());
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
    }
    return summary;
  }
}
