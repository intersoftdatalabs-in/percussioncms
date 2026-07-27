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

package com.percussion.soln.linkback.utils;

import com.percussion.cms.objectstore.PSComponentSummary;
import com.percussion.design.objectstore.PSLocator;
import com.percussion.error.PSException;
import com.percussion.services.legacy.IPSCmsContentSummaries;
import com.percussion.utils.guid.IPSGuid;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Class finds content summaries for content item ids.
 *
 * <p>Taken from the PSO Toolkit.
 *
 * @author adamgent
 */
public class ItemSummaryFinder {

  /** Logger for this class */
  private static final Logger log = LogManager.getLogger(ItemSummaryFinder.class);

  private IPSCmsContentSummaries cmsContentSummaries = null;

  /** Creates a new item summary finder. */
  public ItemSummaryFinder() {
    super();
  }

  /**
   * Programmatic constructor
   *
   * @param cmsContentSummaries - CMS content summaries service.
   */
  public ItemSummaryFinder(IPSCmsContentSummaries cmsContentSummaries) {
    super();
    this.cmsContentSummaries = cmsContentSummaries;
  }

  /**
   * Gets the current or edit locator for the given guid.
   *
   * @param guid the guid
   * @return the locator
   * @throws PSException when the locator cannot be found
   */
  public PSLocator getCurrentOrEditLocator(IPSGuid guid) throws PSException {
    int id = guid.getUUID();
    return getCurrentOrEditLocator(id);
  }

  /**
   * Gets the current or edit locator for the given content id.
   *
   * @param contentId the content id
   * @return the locator
   * @throws PSException when the locator cannot be found
   */
  public PSLocator getCurrentOrEditLocator(String contentId) throws PSException {
    if (StringUtils.isBlank(contentId) || !StringUtils.isNumeric(contentId)) {
      String emsg = "Content id must be numeric " + contentId;
      log.error(emsg);
      throw new IllegalArgumentException(emsg);
    }
    int id = Integer.parseInt(contentId);
    return getCurrentOrEditLocator(id);
  }

  /**
   * Gets the current or edit locator for the given content id.
   *
   * @param id the content id
   * @return the locator
   * @throws PSException when the locator cannot be found
   */
  public PSLocator getCurrentOrEditLocator(int id) throws PSException {
    PSComponentSummary cs = getSummary(id);
    PSLocator loc = cs.getCurrentLocator();
    if (cs.getEditLocator() != null && cs.getEditLocator().getRevision() > 0) {
      loc = cs.getEditLocator();
      log.debug("Using edit locator" + loc);
    }
    return loc;
  }

  /** Constant for no checkout status. */
  public static final int CHECKOUT_NONE = 1;

  /** Constant for checkout by the current user. */
  public static final int CHECKOUT_BY_ME = 2;

  /** Constant for checkout by another user. */
  public static final int CHECKOUT_BY_OTHER = 3;

  /**
   * Gets the checkout status for the given content id and user name.
   *
   * @param contentId the content id
   * @param userName the user name
   * @return one of {@link #CHECKOUT_NONE}, {@link #CHECKOUT_BY_ME}, {@link #CHECKOUT_BY_OTHER}
   * @throws PSException when the content summary cannot be found
   */
  public int getCheckoutStatus(String contentId, String userName) throws PSException {
    if (StringUtils.isBlank(userName)) {
      String emsg = "User name must not be blank";
      log.error(emsg);
      throw new IllegalArgumentException(emsg);
    }
    PSComponentSummary sum = getSummary(contentId);
    String uname = sum.getCheckoutUserName();
    if (StringUtils.isBlank(uname)) {
      return CHECKOUT_NONE;
    }
    if (userName.equalsIgnoreCase(uname)) {
      return CHECKOUT_BY_ME;
    }
    return CHECKOUT_BY_OTHER;
  }

  /**
   * Gets the component summary for an item.
   *
   * @param contentId the content id
   * @return the component summary. Never <code>null</code>.
   * @throws PSException when the item does not exist
   */
  public PSComponentSummary getSummary(String contentId) throws PSException {
    if (StringUtils.isBlank(contentId) || !StringUtils.isNumeric(contentId)) {
      String emsg = "Content id must be numeric " + contentId;
      log.error(emsg);
      throw new IllegalArgumentException(emsg);
    }
    int id = Integer.parseInt(contentId);
    return getSummary(id);
  }

  /**
   * Gets the component summary for an item.
   *
   * @param guid the guid
   * @return the component summary. Never <code>null</code>.
   * @throws PSException when the item does not exist
   */
  public PSComponentSummary getSummary(IPSGuid guid) throws PSException {
    int id = guid.getUUID();
    return getSummary(id);
  }

  /**
   * Gets the component summary for an item.
   *
   * @param id the content id
   * @return the component summary. Never <code>null</code>.
   * @throws PSException when the item does not exist
   */
  public PSComponentSummary getSummary(int id) throws PSException {

    PSComponentSummary sum = getCmsContentSummaries().loadComponentSummary(id);
    if (sum == null) {
      String emsg = "Content item not found " + id;
      log.error(emsg);
      throw new PSException(emsg);
    }
    return sum;
  }

  /**
   * Gets the CMS content summaries service.
   *
   * @return the CMS content summaries service
   */
  public IPSCmsContentSummaries getCmsContentSummaries() {
    return cmsContentSummaries;
  }

  /**
   * Sets the CMS content summaries service.
   *
   * @param cmsContentSummaries the CMS content summaries service
   */
  public void setCmsContentSummaries(IPSCmsContentSummaries cmsContentSummaries) {
    this.cmsContentSummaries = cmsContentSummaries;
  }
}
