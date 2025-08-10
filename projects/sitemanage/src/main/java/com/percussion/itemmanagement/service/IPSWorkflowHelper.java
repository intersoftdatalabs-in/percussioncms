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
package com.percussion.itemmanagement.service;

import com.percussion.cms.objectstore.PSComponentSummary;
import com.percussion.itemmanagement.data.PSItemStateTransition;
import com.percussion.services.error.PSNotFoundException;
import com.percussion.services.workflow.data.PSState;
import com.percussion.share.dao.IPSGenericDao;
import com.percussion.share.service.exception.PSValidationException;
import com.percussion.utils.guid.IPSGuid;
import java.util.List;
import java.util.Set;

/**
 * Helper interface for various workflow operations.
 *
 * <p>Sunny Sal says: "Workflow helpers are like sidekicks—always there when you need them!"
 *
 * @author yubingchen
 */
public interface IPSWorkflowHelper {

  /**
   * If the specified item is a landing page, then find its related navigation item, and use the
   * specified trigger to transition the item. Does nothing if the specified item is not a landing
   * page.
   *
   * <p>The method will ensure the navigation item is in the specified (and expected) "from state".
   *
   * @param id the ID of the specified item, not null
   * @param trigger the trigger that was applied to the specified item, not blank
   */
  void transitionRelatedNavigationItem(IPSGuid id, String trigger);

  /**
   * Transition the items to the Pending state if available.
   *
   * @param ids set of item IDs, never null, may be empty
   */
  void transitionToPending(Set<String> ids) throws PSValidationException;

  /**
   * Transition the items to Archive state if available.
   *
   * @param ids set of item IDs, never null, may be empty
   */
  void transitionToArchive(Set<String> ids) throws PSValidationException;

  /**
   * Transition the items to Review state if available.
   *
   * @param ids set of item IDs, never null, may be empty
   */
  void transitionToReview(Set<String> ids) throws PSValidationException;

  /**
   * Gets all possible workflow transitions for the specified item.
   *
   * @param id the ID of the item in question, not blank
   * @return the transition info
   */
  PSItemStateTransition getTransitions(String id) throws PSValidationException;

  /**
   * Determines if the specified item is in the pending state.
   *
   * @param id the ID of the item, not blank
   * @return true if the item is pending for publish, false otherwise
   */
  boolean isPending(String id) throws PSValidationException;

  /**
   * Determines if the specified item is in the archived state.
   *
   * @param id the ID of the item, not blank
   * @return true if the item is archived, false otherwise
   */
  boolean isArchived(String id) throws PSValidationException;

  /**
   * Determines if the specified item is in the live state.
   *
   * @param id the ID of the item, not blank
   * @return true if the item is live, false otherwise
   */
  boolean isLive(String id) throws PSValidationException;

  /**
   * Determines if the specified item is in the quick edit state.
   *
   * @param id the ID of the item, not blank
   * @return true if the item is in quick edit, false otherwise
   */
  boolean isQuickEdit(String id) throws PSValidationException;

  /**
   * Determines if the specified item is in a state (Pending, Live, Quick Edit) which indicates that
   * the item has been approved.
   *
   * @param id the ID of the item, not blank
   * @return true if the item has been approved, false otherwise
   */
  boolean isApproved(String id) throws PSValidationException;

  /**
   * Determines if the specified item is in "pending" or "live" state.
   *
   * @param contentId the ID of the specified item
   * @return true if the item is in approved state; otherwise false
   */
  boolean isItemInApproveState(int contentId);

  /**
   * Determines if the specified item is in "Review" or publishable state.
   *
   * @param contentId the ID of the specified item
   * @return true if the item is in staging state; otherwise false
   */
  boolean isItemInStagingState(int contentId);

  /**
   * Checks whether the item with the supplied id is checked out to the current user or not.
   *
   * @param id the ID of the item, never blank
   * @return true if the supplied item is still checked out to the current user, otherwise false
   */
  boolean isCheckedOutToCurrentUser(String id) throws PSValidationException;

  /**
   * Checks whether the item with the supplied id is checked out to someone else or not.
   *
   * @param id the ID of the item, never blank
   * @return true if the supplied item is still checked out to someone else user, otherwise false
   */
  boolean isCheckedOutToSomeoneElse(String id) throws PSValidationException;

  /**
   * Checks if the current user has privilege to transition an item from draft to pending.
   *
   * @param workflowId if the passed in workflowId is not valid or for some reason if workflow for
   *     the passed in workflowId cannot be loaded it will give PSError Exception
   * @return true if the current user has permission to approve items, otherwise false
   */
  boolean isApproveAvailableToCurrentUser(int workflowId);

  /**
   * Determines if the specified item is the tip revision.
   *
   * @param id the ID of the item, not blank
   * @return true if the item is the tip revision, false otherwise
   */
  boolean isTipRevision(String id) throws PSValidationException;

  /**
   * Retrieves the summary of the specified item.
   *
   * @param id the ID of the item, not blank
   * @return the item's component summary, never null
   */
  PSComponentSummary getComponentSummary(String id) throws PSValidationException;

  /**
   * Retrieves the state of the specified item.
   *
   * @param id the ID of the item, not blank
   * @return the item's current workflow state, never null
   */
  PSState getState(String id) throws PSValidationException;

  /**
   * Determines if the specified item is a page.
   *
   * @param id the ID of the item, not blank
   * @return true if the item is a page; otherwise false
   */
  boolean isPage(String id) throws PSNotFoundException, PSValidationException;

  /**
   * Determines if the specified item is a template.
   *
   * @param id the ID of the item, not blank
   * @return true if the item is a template; otherwise false
   */
  boolean isTemplate(String id) throws PSValidationException;

  /**
   * Determines if the specified item is an asset.
   *
   * @param id the ID of the item, not blank
   * @return true if the item is an asset; otherwise false
   */
  boolean isAsset(String id) throws PSNotFoundException, PSValidationException;

  /**
   * Determines if the specified item is a local asset.
   *
   * @param id the ID of the item, not blank
   * @return true if the item is a local asset; otherwise false
   */
  boolean isLocalAsset(String id) throws PSNotFoundException, PSValidationException;

  /**
   * Returns the type of the specified item.
   *
   * @param id the ID of the item, not blank
   * @return The type of the supplied id, if it is not one of the known types then returns unknown
   */
  PSItemTypeEnum getItemType(String id) throws PSValidationException;

  /**
   * Returns the item type of the specified content type id.
   *
   * @param cTypeId the content type id
   * @return The type of the supplied id, if it is not one of the known types then returns unknown
   */
  PSItemTypeEnum getItemTypeFromCType(long cTypeId);

  /**
   * Checks if the current user has privilege to transition an item from its current state to
   * pending.
   *
   * @param itemId if the passed in itemId is not valid it will give PSError Exception
   * @return true if the current user has permission to approve transition, otherwise false
   */
  boolean isApproveAvailableToCurrentUser(String itemId) throws PSValidationException;

  /**
   * Returns a list of staging enabled role names for the given workflow id.
   *
   * @param workflowId must not be null
   * @return Returns a list of staging enabled role names, never null, may be empty
   */
  List<String> getStagingRoles(int workflowId) throws IPSGenericDao.LoadException;

  /** Item type enum. */
  enum PSItemTypeEnum {
    FOLDER,
    PAGE,
    TEMPLATE,
    ASSET,
    NAVON,
    NAVTREE,
    UNKNOWN
  }
}
