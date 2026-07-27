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

import com.percussion.itemmanagement.data.PSApprovableItems;
import com.percussion.itemmanagement.data.PSBulkApprovalJobStatus;
import com.percussion.itemmanagement.data.PSItemStateTransition;
import com.percussion.itemmanagement.data.PSItemTransitionResults;
import com.percussion.itemmanagement.data.PSItemUserInfo;
import com.percussion.services.error.PSNotFoundException;
import com.percussion.share.dao.IPSGenericDao;
import com.percussion.share.data.PSNoContent;
import com.percussion.share.service.exception.PSDataServiceException;
import com.percussion.share.service.exception.PSValidationException;
import java.util.Set;

/**
 * Handles all workflow operations, such as check-in, check-out, and transitions for content items.
 *
 * <p>Sunny Sal says: "Workflow so smooth, even your code will want a vacation!"
 *
 * @author peterfrontiero
 */
public interface IPSItemWorkflowService {

  /**
   * Performs a check-in of the item identified by the specified id. All local content items
   * associated with the item will also be checked in. A forced check-in will be performed if the
   * item is currently checked out by a different user and the current user has admin privileges. If
   * the item does not exist, the method silently returns successfully.
   *
   * @param id never blank
   * @return a no-content response as a workaround for an issue in jQuery/ajax
   * @throws PSItemWorkflowServiceException if an error occurs
   */
  PSNoContent checkIn(String id) throws PSItemWorkflowServiceException;

  /**
   * Checks in the content. If ignoreRevisionCheck is true, the underlying server code doesn't check
   * whether the content's revision is checked out to the current user or not. Pass true for this
   * method only in case of force checkin.
   *
   * @param id item id assumed to be a valid guid
   * @param ignoreRevisionCheck flag to ignore revisions while checking in or not
   * @return PSNoContent
   */
  PSNoContent checkIn(String id, boolean ignoreRevisionCheck)
      throws PSItemWorkflowServiceException, PSDataServiceException;

  /**
   * Performs a check-out of the item identified by the specified id to the current user. The item
   * will be transitioned to the quick edit state prior to check-out if necessary.
   *
   * @param id never blank
   * @return the user info for the item, never null
   * @throws PSItemWorkflowServiceException if the request context for the current user thread is
   *     invalid
   */
  PSItemUserInfo checkOut(String id) throws PSItemWorkflowServiceException;

  /**
   * Performs a forced check-out of the item identified by the specified id to the current user.
   * This includes a check-in of the item immediately followed by a check-out.
   *
   * @param id never blank
   * @return the user info for the item, never null
   * @throws PSItemWorkflowServiceException if the request context for the current user thread is
   *     invalid or any other error occurs
   */
  PSItemUserInfo forceCheckOut(String id) throws PSItemWorkflowServiceException;

  /**
   * Gets all possible workflow transitions for the specified item.
   *
   * @param id the ID of the item in question, not blank
   * @return the transition info
   */
  PSItemStateTransition getTransitions(String id);

  /** Calls {@link #transitionWithComments(String, String, String)} with null for comment. */
  PSItemTransitionResults transition(String id, String trigger);

  /**
   * Transition a specified item according to the specified trigger name. If the trigger is {@link
   * #TRANSITION_TRIGGER_APPROVE} calls {@link #performApproveTransition(String, boolean, String)}
   * method to handle it.
   *
   * @param id the ID of the item, not blank
   * @param trigger the trigger name of the transition, not blank. A trigger of 'Publish' will also
   *     result in the transition of all shared content items associated with the item if 'Publish'
   *     is an available transition trigger.
   * @param comment transition comment may be null or empty
   * @return the transition results for the item. This includes shared assets which failed to
   *     transition.
   */
  PSItemTransitionResults transitionWithComments(String id, String trigger, String comment);

  /**
   * Performs approve transition of the supplied item. The item will only be transitioned if all
   * shared assets which can be transitioned are also successfully transitioned. If it is index
   * page, associated navon is transitioned. If the supplied preventIfStartDate flag is true, then
   * doesn't transition if the user doesn't have access to the publish transition from the current
   * state of the item and the start date is set on the item.
   *
   * @param id the ID of the item, not blank
   * @param preventIfStartDate flag to indicate whether to prevent approve transition if the start
   *     date is set
   * @param comment transition comment may be null or empty
   * @return the transition results for the item. This includes shared assets which failed to
   *     transition.
   */
  PSItemTransitionResults performApproveTransition(
      String id, boolean preventIfStartDate, String comment)
      throws PSItemWorkflowServiceException, PSDataServiceException, PSNotFoundException;

  /**
   * Determines if the current user is authorized to modify (check-out, delete, etc.) the specified
   * item in its current state.
   *
   * @param id the ID of the item, not blank
   * @return true if the item can be modified by the current user, false otherwise
   */
  boolean isModifiableByUser(String id)
      throws PSValidationException, PSItemWorkflowServiceException;

  /**
   * Gets all approved pages which use the specified asset. An approved page is a page which is the
   * tip revision and is in an approved state (Pending, Live, Quick Edit) or which is not the tip
   * revision and is in the Quick Edit state.
   *
   * @param id the ID of the asset, never blank
   * @return set of page ids. Never null, may be empty
   */
  Set<String> getApprovedPages(String id) throws PSValidationException, PSNotFoundException;

  /**
   * Gets all approved pages on the site specified by the given folder path which use the specified
   * asset. See {@link #getApprovedPages(String)} for a description of an approved page.
   *
   * @param id the ID of the asset, never blank
   * @param folderPath the asset's folder path, never blank. Must represent a valid site folder path
   * @return set of page ids. Never null, may be empty
   */
  Set<String> getApprovedPages(String id, String folderPath)
      throws PSValidationException, PSNotFoundException;

  /**
   * Checks whether the item with the supplied id is checked out to the current user or not.
   *
   * @param id the ID of the item, never blank
   * @return true if the supplied item is still checked out to the current user, otherwise false
   */
  boolean isCheckedOutToCurrentUser(String id);

  /**
   * Checks whether the item with the supplied id is checked out to someone else or not.
   *
   * @param id the ID of the item, never blank
   * @return true if the supplied item is still checked out to someone else user, otherwise false
   */
  boolean isCheckedOutToSomeoneElse(String id) throws PSValidationException;

  /**
   * Checks whether the currently logged in user has privileges to do approve on the items that are
   * in draft status.
   *
   * @param path The path to use to determine which workflow to check, if not supplied then default
   *     workflow is used
   * @return true if user can have approve, otherwise false
   */
  boolean isApproveAvailableToCurrentUser(String path);

  /**
   * Determines the id of the workflow in the specified request.
   *
   * @param workflowName never null
   * @return the workflow id for the specified request
   * @throws PSItemWorkflowServiceException if the workflow could not be found
   */
  int getWorkflowId(String workflowName)
      throws PSItemWorkflowServiceException, PSValidationException;

  /**
   * Determines the id of the workflow state for the given workflow and state.
   *
   * @param workflowName never null
   * @param stateName never null
   * @return the workflow state id for the specified request or -1 if a matching state could not be
   *     found
   * @throws PSItemWorkflowServiceException if the workflow could not be found
   */
  int getStateId(String workflowName, String stateName) throws PSItemWorkflowServiceException;

  /**
   * Determines if the given trigger is available for the current user for the given item in its
   * current state, which means, can the current user use this trigger to transition the item?
   *
   * @param id of the item, never blank
   * @param trigger the transition trigger, never blank
   * @return true if the item can be transitioned using the trigger, false otherwise
   */
  boolean isTriggerAvailable(String id, String trigger) throws PSValidationException;

  /**
   * Checks whether staging option is available or not for the supplied user for the supplied item.
   * If the item is in one of the staging publishable states and if the user is in a role that has
   * staging permission then returns true otherwise returns false.
   *
   * @param id of the item, never blank
   * @return true if available otherwise false
   */
  boolean isStagingOptionAvailable(String id)
      throws PSValidationException, IPSGenericDao.LoadException;

  /**
   * Checks whether remove from staging option is available or not for the supplied user for the
   * supplied item. If the item is in archive state and if the user is in a role that has staging
   * permission then returns true otherwise returns false.
   *
   * @param id of the item, never blank
   * @return true if available otherwise false
   */
  boolean isRemoveFromStagingOptionAvailable(String id)
      throws PSValidationException, IPSGenericDao.LoadException;

  /**
   * Returns the id of the local content workflow recognized by the name "LocalContent", throws
   * RunTimeException if the workflow is not found.
   *
   * @return The id of the local content workflow
   */
  int getLocalContentWorkflowId() throws PSItemWorkflowServiceException;

  /**
   * Checks if a modification to the given item is allowed. The modifications are allowed if:
   *
   * <ul>
   *   <li>the item is checked out to the current user
   *   <li>the item is not checked out to current user
   * </ul>
   *
   * @param id String with the id of the item. Must not be null
   * @return true if the modifications are allowed, false otherwise
   */
  boolean isModifyAllowed(String id) throws PSValidationException, PSItemWorkflowServiceException;

  /**
   * Bulk approve operation.
   *
   * @param items items to approve
   * @return job id or status string
   */
  String bulkApprove(PSApprovableItems items);

  /**
   * Gets full approval job status.
   *
   * @param jobId job id
   * @return job status
   */
  PSBulkApprovalJobStatus getApprovalStatusFull(String jobId);

  /**
   * Gets processed approval job status.
   *
   * @param jobId job id
   * @return job status
   */
  PSBulkApprovalJobStatus getApprovalStatusProcessed(String jobId);

  /** Thrown when an error is encountered in the item workflow service. */
  class PSItemWorkflowServiceException extends Exception {
    private static final long serialVersionUID = 1L;

    public PSItemWorkflowServiceException() {
      super();
    }

    public PSItemWorkflowServiceException(String message) {
      super(message);
    }

    public PSItemWorkflowServiceException(String message, Throwable cause) {
      super(message, cause);
    }

    public PSItemWorkflowServiceException(Throwable cause) {
      super(cause);
    }
  }

  // Workflow transition trigger constants
  String TRANSITION_TRIGGER_APPROVE = "Approve";
  String TRANSITION_TRIGGER_ARCHIVE = "Archive";
  String TRANSITION_TRIGGER_RESUBMIT = "Resubmit";
  String TRANSITION_TRIGGER_TAKEDOWN = "Take Down";
  String TRANSITION_TRIGGER_LIVE = "forcetolive";
  String TRANSITION_TRIGGER_SUBMIT = "Submit";
  String TRANSITION_TRIGGER_REJECT = "Reject";
  String TRANSITION_TRIGGER_EDIT = "Quick Edit";
  String TRANSITION_TRIGGER_PUBLISH = "Publish";
  String TRANSITION_TRIGGER_REMOVE = "Remove";

  // Workflow state constants
  String CURRENT_STATE_PENDING = "Pending";
  String CURRENT_STATE_LIVE = "Live";

  /**
   * Determines if the quick edit trigger is available for the current user for the given item in
   * its current state (pending or live), which means, can the current user use this trigger to
   * transition the item?
   *
   * @param id of the item, never blank
   * @param trigger the transition trigger, never blank
   * @param currentState "Pending" or "Live", never blank
   * @return true if the item can be transitioned using the trigger, false otherwise
   */
  boolean isQuickEditTriggerAvailableForPendingOrLivePage(
      String id, String trigger, String currentState) throws PSValidationException;
}
