/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
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

package com.percussion.itemmanagement.service;

import com.percussion.itemmanagement.data.PSItemUserInfo;
import com.percussion.itemmanagement.service.IPSItemWorkflowService.PSItemWorkflowServiceException;
import org.apache.commons.lang3.StringUtils;

/**
 * Maps a component summary's checkout user onto {@link PSItemUserInfo} without checking the item
 * out (#4910).
 */
public final class CheckoutOwnerInfo {

  private CheckoutOwnerInfo() {}

  /**
   * @param id item id, never blank
   * @param itemName summary name; falls back to {@code id} when blank
   * @param checkoutUser holder, or null/blank when not checked out
   * @param currentUser session user, never blank
   */
  public static PSItemUserInfo fromSummary(
      String id, String itemName, String checkoutUser, String currentUser)
      throws PSItemWorkflowServiceException {
    if (StringUtils.isBlank(id)) {
      throw new PSItemWorkflowServiceException("item id is required");
    }
    if (StringUtils.isBlank(currentUser)) {
      throw new PSItemWorkflowServiceException("No session user");
    }
    String name = StringUtils.isBlank(itemName) ? id.trim() : itemName.trim();
    String lock = checkoutUser == null ? "" : checkoutUser;
    return new PSItemUserInfo(name, lock, currentUser.trim(), "Reader");
  }
}
