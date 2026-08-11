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
 * com.percussion.pso.utils. PSOItemSummaryFinderWrapper.java
 *
 * @author DavidBenua
 *
 */
package com.percussion.pso.utils;

import com.percussion.cms.objectstore.PSComponentSummary;
import com.percussion.design.objectstore.PSLocator;
import com.percussion.error.PSException;
import com.percussion.utils.guid.IPSGuid;

/**
 * Wrapper class for the static methods of PSOItemSummaryFinder. Used to extract an interface for
 * testability.
 *
 * @author DavidBenua
 */
public class PSOItemSummaryFinderWrapper implements IPSOItemSummaryFinder {
  /**
   * Default constructor.
   * Creates a new PSOItemSummaryFinderWrapper.
   *
   */
  public PSOItemSummaryFinderWrapper() {}

  /**
   * Returns the current or edit locator.
   *
   * @see
   *     com.percussion.pso.utils.IPSOItemSummaryFinder#getCurrentOrEditLocator(com.percussion.utils.guid.IPSGuid)
   * @param guid the guid
   * @return the result
   * @throws PSException if an error occurs
   */
  public PSLocator getCurrentOrEditLocator(IPSGuid guid) throws PSException {
    return PSOItemSummaryFinder.getCurrentOrEditLocator(guid);
  }

  /**
   * See referenced member.
   * @see com.percussion.pso.utils.IPSOItemSummaryFinder#getCurrentOrEditLocator(java.lang.String)
   * @param contentId the content id
   * @return the result
   * @throws PSException if an error occurs
   */
  public PSLocator getCurrentOrEditLocator(String contentId) throws PSException {
    return PSOItemSummaryFinder.getCurrentOrEditLocator(contentId);
  }

  /**
   * See referenced member.
   * @see com.percussion.pso.utils.IPSOItemSummaryFinder#getCurrentOrEditLocator(int)
   * @param id the id
   * @return the result
   * @throws PSException if an error occurs
   */
  public PSLocator getCurrentOrEditLocator(int id) throws PSException {
    return PSOItemSummaryFinder.getCurrentOrEditLocator(id);
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
   * @see com.percussion.pso.utils.IPSOItemSummaryFinder#getCheckoutStatus(java.lang.String,
   *     java.lang.String)
   * @param contentId the content id
   * @param userName the user name
   * @return the result
   * @throws PSException if an error occurs
   */
  public int getCheckoutStatus(String contentId, String userName) throws PSException {
    return PSOItemSummaryFinder.getCheckoutStatus(contentId, userName);
  }

  /**
   * See referenced member.
   * @see com.percussion.pso.utils.IPSOItemSummaryFinder#getSummary(java.lang.String)
   * @param contentId the content id
   * @return the result
   * @throws PSException if an error occurs
   */
  public PSComponentSummary getSummary(String contentId) throws PSException {
    return PSOItemSummaryFinder.getSummary(contentId);
  }

  /**
   * Returns the summary.
   *
   * @see
   *     com.percussion.pso.utils.IPSOItemSummaryFinder#getSummary(com.percussion.utils.guid.IPSGuid)
   * @param guid the guid
   * @return the result
   * @throws PSException if an error occurs
   */
  public PSComponentSummary getSummary(IPSGuid guid) throws PSException {
    return PSOItemSummaryFinder.getSummary(guid);
  }

  /**
   * See referenced member.
   * @see com.percussion.pso.utils.IPSOItemSummaryFinder#getSummary(int)
   * @param id the id
   * @return the result
   * @throws PSException if an error occurs
   */
  public PSComponentSummary getSummary(int id) throws PSException {
    return PSOItemSummaryFinder.getSummary(id);
  }
}
