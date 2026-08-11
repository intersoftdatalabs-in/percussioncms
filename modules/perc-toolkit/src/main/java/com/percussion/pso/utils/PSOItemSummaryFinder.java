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
/*
 * com.percussion.pso.util PSOItemSummaryFinder.java
 *
 * @author DavidBenua
 *
 */
package com.percussion.pso.utils;

import com.percussion.cms.objectstore.PSComponentSummary;
import com.percussion.design.objectstore.PSLocator;
import com.percussion.error.PSException;
import com.percussion.services.legacy.IPSCmsContentSummaries;
import com.percussion.services.legacy.PSCmsContentSummariesLocator;
import com.percussion.utils.guid.IPSGuid;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * PSOItemSummaryFinder class.
 */
public class PSOItemSummaryFinder {
  /** Logger for this class */
  private static final Logger log = LogManager.getLogger(PSOItemSummaryFinder.class);

  private static IPSCmsContentSummaries sumsvc = null;

  /** Static methods only, never constructed. */
  private PSOItemSummaryFinder() {}

  private static void initServices() {
    if (sumsvc == null) {
      sumsvc = PSCmsContentSummariesLocator.getObjectManager();
    }
  }

  /**
   * Returns the current or edit locator.
   *
   * @param guid the guid
   * @return the result
   * @throws PSException if an error occurs
   */
  public static PSLocator getCurrentOrEditLocator(IPSGuid guid) throws PSException {
    int id = guid.getUUID();
    return getCurrentOrEditLocator(id);
  }

  /**
   * Returns the current or edit locator.
   *
   * @param contentId the content id
   * @return the result
   * @throws PSException if an error occurs
   */
  public static PSLocator getCurrentOrEditLocator(String contentId) throws PSException {
    if (StringUtils.isBlank(contentId) || !StringUtils.isNumeric(contentId)) {
      String emsg = "Content id must be numeric " + contentId;
      log.error(emsg);
      throw new IllegalArgumentException(emsg);
    }
    int id = Integer.parseInt(contentId);
    return getCurrentOrEditLocator(id);
  }

  /**
   * Returns the current or edit locator.
   *
   * @param id the id
   * @return the result
   * @throws PSException if an error occurs
   */
  public static PSLocator getCurrentOrEditLocator(int id) throws PSException {
    PSComponentSummary cs = getSummary(id);
    PSLocator loc = cs.getCurrentLocator();
    if (cs.getEditLocator() != null && cs.getEditLocator().getRevision() > 0) {
      loc = cs.getEditLocator();
      log.debug("Using edit locator" + loc);
    }
    return loc;
  }

  /** checkout none. */
  public static final int CHECKOUT_NONE = 1;
  /** checkout by me. */
  public static final int CHECKOUT_BY_ME = 2;
  /** checkout by other. */
  public static final int CHECKOUT_BY_OTHER = 3;

  /**
   * Returns the checkout status.
   *
   * @param contentId the content id
   * @param userName the user name
   * @return the result
   * @throws PSException if an error occurs
   */
  public static int getCheckoutStatus(String contentId, String userName) throws PSException {
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
   * @return the component summary. Never <code>null</code>
   * @throws PSException if an error occurs
   */
  public static PSComponentSummary getSummary(String contentId) throws PSException {
    if (StringUtils.isBlank(contentId) || !StringUtils.isNumeric(contentId)) {
      String emsg = "Content id must be numeric " + contentId;
      log.error(emsg);
      throw new IllegalArgumentException(emsg);
    }
    int id = Integer.parseInt(contentId);
    return getSummary(id);
  }

  /**
   * Returns the summary.
   *
   * @param guid the guid
   * @return the result
   * @throws PSException if an error occurs
   */
  public static PSComponentSummary getSummary(IPSGuid guid) throws PSException {
    int id = guid.getUUID();
    return getSummary(id);
  }

  /**
   * Returns the summary.
   *
   * @param id the id
   * @return the result
   * @throws PSException if an error occurs
   */
  public static PSComponentSummary getSummary(int id) throws PSException {
    initServices();

    PSComponentSummary sum = sumsvc.loadComponentSummary(id);
    if (sum == null) {
      String emsg = "Content item not found " + id;
      log.error(emsg);
      throw new PSException(emsg);
    }
    return sum;
  }

  /**
   * Sets the summary service. Should be used only in unit tests.
   *
   * @param sumservice The sumsvc to set.
   */
  public static void setSumsvc(IPSCmsContentSummaries sumservice) {
    sumsvc = sumservice;
  }
}
