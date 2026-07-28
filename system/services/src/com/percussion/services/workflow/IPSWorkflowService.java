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
package com.percussion.services.workflow;


import com.percussion.services.catalog.data.PSObjectSummary;
import com.percussion.services.notification.IPSNotificationService;
import com.percussion.services.workflow.data.PSAssignmentTypeEnum;
import com.percussion.services.workflow.data.PSNotification;
import com.percussion.services.workflow.data.PSState;
import com.percussion.services.workflow.data.PSTransition;
import com.percussion.services.workflow.data.PSWorkflow;
import com.percussion.utils.guid.IPSGuid;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * The workflow service provides comprehensive workflow management capabilities with Java 11 modernization.
 *
 * <h2>Workflows</h2>
 * A workflow consists of a collection of states, which are arranged by
 * pairwise relationships called transitions. An item can be in one workflow
 * state at a time. For any given state, there may be transitions available to
 * other states.
 * <p>
 * For an item in a given state, there are certain roles that are allowed
 * to manipulate the item. These roles are "assignee" roles. Other roles are
 * allowed access to the item only, these roles are "reader" roles. Additionally
 * there are "admin" roles and roles that are not included, in which case they
 * cannot act on the item. These possible <em>assignment types</em> will be
 * extended in the future.
 * <p>
 * One or more roles for a given state can allow <em>adhoc</em> assignment.
 * Adhoc assignments allow the specification of particular users or set
 * of users that have the assignee role. Normal adhoc requires that the user(s)
 * must also be members of the adhoc role. Anonymous adhoc allows any user to
 * be made an adhoc assignee, regardless of their normal membership in the role.
 * <p>
 * At this point one action plus one notification can be made on any given
 * transition. A notification sends electronic mail to:
 * <ul>
 * <li>Users in a given role</li>
 * <li>Extra users in a CC list</li>
 * </ul>
 * Notifications are sent asynchronously using the notification service.
 *
 * <h2>Publishable State</h2>
 * A given workflow state has an attribute that describes what the publishable
 * state is when an item is in the given state. This value typically
 * describes if an item should be published, archived or ignored when
 * publishing. Additionally, many implementations create a poor man's version
 * of staging by extending these values.
 *
 * <h2>Java 11 Features</h2>
 * This modernized interface provides:
 * <ul>
 * <li>Optional-based safe access methods for null safety</li>
 * <li>Stream API support for efficient data processing</li>
 * <li>Enhanced validation with Objects.requireNonNull</li>
 * <li>Default methods for backward compatibility</li>
 * </ul>
 *
 * @see PSAssignmentTypeEnum for information about assignment types
 * @see IPSNotificationService for information about asynchronous notification
 *
 * @author dougrand
 */
public interface IPSWorkflowService {

    /**
     * Find all workflows for the specified name.
     *
     * @param name the name of the workflow to find, may be {@code null}
     *             or empty in which case all workflows will be returned.
     *             SQL type (%) wildcards are supported
     * @return an immutable list of summaries for all found workflows for the supplied name,
     *         never {@code null}, may be empty
     */
    List<PSObjectSummary> findWorkflowSummariesByName(String name);

    /**
     * Find all workflow summaries as a stream for efficient processing.
     *
     * @param name the name pattern to match workflows
     * @return Stream of workflow summaries, never {@code null}
     */
    default Stream<PSObjectSummary> streamWorkflowSummaries(String name) {
        return findWorkflowSummariesByName(name).stream();
    }

    /**
     * Load a workflow using a cached copy if possible. This is a fast call, but
     * will return a shared instance that must not be modified. The returned
     * workflow object is a complete workflow tree that includes all the
     * other objects aggregated by the workflow. This object graph can be
     * traversed to discover workflow states and transitions without fear that
     * some portion of the tree has not been correctly loaded (causing a
     * Hibernate exception).
     *
     * @param id The GUID, not {@code null}
     * @return the workflow, or {@code null} if the instance is not found
     * @throws IllegalArgumentException if id is null
     */
    PSWorkflow loadWorkflow(IPSGuid id);

    /**
     * Load a workflow using a cached copy, returning an Optional for safe access.
     * This is the preferred method for workflow access as it provides null safety.
     *
     * @param id The GUID, not {@code null}
     * @return an Optional containing the workflow if found, empty otherwise
     * @throws IllegalArgumentException if id is null
     */
    default Optional<PSWorkflow> findWorkflow(IPSGuid id) {
        return Optional.ofNullable(loadWorkflow(id));
    }

    /**
     * Convenience method to obtain the default workflow GUID for the system.
     *
     * <p>Historically this method existed in various helpers; having it on the
     * service allows callers to avoid hardcoding defaults.
     *
     * <p>The default implementation returns {@code null} to maintain backward
     * compatibility.  Implementations should override as appropriate.
     *
     * @return the default workflow GUID or {@code null} if unknown
     */
    default IPSGuid getDefaultWorkflowId() {
        return null;
    }

    /**
     * Load a workflow from the database, bypassing cache.
     *
     * @param id The GUID, not {@code null}
     * @return the workflow, or {@code null} if the instance is not found
     * @throws IllegalArgumentException if id is null
     */
    PSWorkflow loadWorkflowDb(IPSGuid id);

    /**
     * Load a workflow from database, returning an Optional for safe access.
     *
     * @param id The GUID, not {@code null}
     * @return an Optional containing the workflow if found, empty otherwise
     * @throws IllegalArgumentException if id is null
     */
    default Optional<PSWorkflow> findWorkflowDb(IPSGuid id) {
        return Optional.ofNullable(loadWorkflowDb(id));
    }

    /**
     * Check whether a workflow state is marked publishable in the given workflow.
     *
     * @param stateId the state id, not {@code null}
     * @param workflowId the workflow id, not {@code null}
     * @return {@code true} if publishable, {@code false} otherwise
     * @throws PSWorkflowException if there is a problem checking the state
     */
    boolean isPublic(IPSGuid stateId, IPSGuid workflowId) throws PSWorkflowException;

    /**
     * Load all workflows for the specified name. Unlike
     * {@link #loadWorkflow(IPSGuid)}, this does not return a read-only copy
     * of the workflow cached in memory. However, the returned object has been
     * properly configured so that all aggregated data is available.
     *
     * @param name the name of the workflow to find, may be {@code null}
     *             or empty in which case all workflows will be returned.
     *             SQL type (%) wildcards are supported
     * @return an immutable list with all found workflows for the supplied name, never
     *         {@code null}, may be empty
     */
    List<PSWorkflow> findWorkflowsByName(String name);

    /**
     * Get all available workflow actions for the supplied content items and context.
     *<p>
     * Historically callers invoked this via {@code PSWorkflowServiceImpl} directly; the
     * interface was updated to expose the method so that Active Assembly and other UI
     * modules can compile against the interface instead of casting.
     *
     * @param contentids list of content item GUIDs, never {@code null}
     * @param assignmentTypes assignment type enums obtained from the system service,
     *                        may be empty
     * @param userName user performing the request, not {@code null}
     * @param userRoles roles assigned to the user, not {@code null}
     * @param locale user locale string, may be {@code null}
     * @return list of {@link com.percussion.cx.objectstore.PSMenuAction} objects,
     *         never {@code null}, may be empty
     * @throws PSWorkflowException on service error
     */
    List<com.percussion.cx.objectstore.PSMenuAction> getAllWorkflowActions(
        List<com.percussion.utils.guid.IPSGuid> contentids,
        List<PSAssignmentTypeEnum> assignmentTypes,
        String userName,
        List<String> userRoles,
        String locale) throws PSWorkflowException;

    /**
     * Get a stream of workflows for efficient processing.
     *
     * @param name the name pattern to match workflows
     * @return Stream of workflows, never {@code null}
     */
    default Stream<PSWorkflow> streamWorkflows(String name) {
        return findWorkflowsByName(name).stream();
    }

    /**
     * Get all workflows as a stream for efficient processing.
     *
     * @return Stream of all workflows, never {@code null}
     */
    default Stream<PSWorkflow> streamAllWorkflows() {
        return streamWorkflows(null);
    }

    /**
     * Get the name of the default workflow configured in the system.
     *
     * @return workflow name never {@code null} or empty
     */
    String getDefaultWorkflowName();

    /**
     * Save the designated workflow. Only used for testing at this point.
     * For internal use only.
     *
     * @param workflow the workflow, not {@code null}
     * @throws IllegalArgumentException if workflow is null
     */
    void saveWorkflow(PSWorkflow workflow);

    /**
     * Update the workflow database version for the supplied workflow GUID. Implementations should
     * provide the version bump logic (used by tests and admin scripts).
     *
     * @param id the workflow id, not {@code null}
     */
    void updateWorkflowVersion(IPSGuid id);

    /**
     * Find adhoc assignment information for the supplied content item.
     *
     * @param contentId the content GUID, not {@code null}
     * @return a list of adhoc users for the item, never {@code null}
     */
    java.util.List<com.percussion.services.workflow.data.PSContentAdhocUser> findAdhocInfoByItem(IPSGuid contentId);

    /**
     * Find approvals for a user.
     *
     * @param username the user name, not {@code null}
     * @return a list of approvals for the user, never {@code null}
     */
    java.util.List<com.percussion.services.workflow.data.PSContentApproval> findApprovalsByUser(String username);

    /**
     * Find approvals for a content item.
     *
     * @param contentId the content GUID, not {@code null}
     * @return a list of approvals for the item, never {@code null}
     */
    java.util.List<com.percussion.services.workflow.data.PSContentApproval> findApprovalsByItem(IPSGuid contentId);

    /**
     * Add a role to a workflow instance.
     *
     * @param id the role id (may be null to generate),
     * @param roleName the role name, not {@code null}
     * @param wf the workflow to modify, not {@code null}
     */
    void addRoleToWorkflow(IPSGuid id, String roleName, PSWorkflow wf);

    /**
     * Add a role to a workflow by GUID (backwards-compatible signature).
     * Implementations must provide a concrete implementation.
     *
     * @param wfId workflow id, may be {@code null} to affect all workflows
     * @param roleName role name, not {@code null}
     */
    void addWorkflowRole(IPSGuid wfId, String roleName);

    /**
     * Remove a role from a workflow by GUID (backwards-compatible signature).
     * Implementations must provide a concrete implementation.
     *
     * @param wfId workflow id, may be {@code null} to affect all workflows
     * @param roleName role name, not {@code null}
     * @return true if removed, false otherwise
     */
    boolean removeWorkflowRole(IPSGuid wfId, String roleName);

    /**
     * Save an adhoc user assignment for content.
     * @param adhoc the adhoc user, not {@code null}
     */
    void saveContentAdhocUser(com.percussion.services.workflow.data.PSContentAdhocUser adhoc);

    /**
     * Find adhoc users by username.
     * @param username the username, not {@code null}
     * @return list of adhoc entries, never {@code null}
     */
    java.util.List<com.percussion.services.workflow.data.PSContentAdhocUser> findAdhocInfoByUser(String username);

    /**
     * Delete adhoc user assignment.
     * @param adhoc the adhoc user to delete, not {@code null}
     */
    void deleteContentAdhocUser(com.percussion.services.workflow.data.PSContentAdhocUser adhoc);

    /**
     * Get workflow state information for a list of content GUIDs.
     * @param guids list of content GUIDs, not {@code null}
     * @return list of PSContentWorkflowState, never {@code null}
     */
    java.util.List<com.percussion.services.workflow.data.PSContentWorkflowState> getWorkflowStateForContent(java.util.List<IPSGuid> guids);

    /**
     * Delete content approvals for the supplied content id.
     * @param contentid content guid, not {@code null}
     */
    void deleteContentApprovals(IPSGuid contentid);

    /**
     * Save a content approval record.
     * @param approval approval to save, not {@code null}
     */
    void saveContentApproval(com.percussion.services.workflow.data.PSContentApproval approval);

    /**
     * Delete the designated workflow. If the workflow does not exist, then this
     * call has no effect.
     *
     * @param wfid the workflow GUID, not {@code null}
     * @return {@code true} if the workflow was deleted, {@code false} if it didn't exist
     * @throws Exception if there is an error deleting the workflow
     * @throws IllegalArgumentException if wfid is null
     */
    boolean deleteWorkflow(IPSGuid wfid) throws Exception;

    /**
     * Loads a specified workflow state. This is a fast call, but will return a
     * shared instance that must not be modified. The returned instance is not
     * the same instance (by address) you will find by traversing the workflow
     * identified in the call. However, it will have the same values.
     *
     * @param stateId the ID of the specified state, not {@code null}
     * @param workflowId the ID of the workflow which contains the specified
     *                   state, not {@code null}
     * @return the specified workflow state, may be {@code null} if the
     *         specified workflow state does not exist
     * @throws IllegalArgumentException if stateId or workflowId is null
     */
    PSState loadWorkflowState(IPSGuid stateId, IPSGuid workflowId);

    /**
     * Loads a specified workflow state, returning an Optional for safe access.
     * This is the preferred method for state access as it provides null safety.
     *
     * @param stateId the ID of the specified state, not {@code null}
     * @param workflowId the ID of the workflow which contains the specified
     *                   state, not {@code null}
     * @return an Optional containing the workflow state if found, empty otherwise
     * @throws IllegalArgumentException if stateId or workflowId is null
     */
    default Optional<PSState> findWorkflowState(IPSGuid stateId, IPSGuid workflowId) {
        return Optional.ofNullable(loadWorkflowState(stateId, workflowId));
    }

    /**
     * Loads a single workflow transition by its (workflowId, transitionId) key, joining the same
     * Hibernate session as the rest of the CMS so callers running under a Spring transaction do not
     * open a second pool connection.
     *
     * <p>Added for #1561 Phase 3 to replace the raw-JDBC
     * {@code PSTransitionsContext(transitionId, workflowId, Connection)} read path inside
     * {@code modules/extensions-workflow/.../PSExitUpdateHistory}.
     *
     * @param workflowAppId the workflow id, must be {@code > 0}.
     * @param transitionId the transition id, must be {@code > 0}.
     * @return the transition, or {@code null} when no row matches the supplied key.
     */
    PSTransition loadWorkflowTransition(long workflowAppId, long transitionId);

    /**
     * Loads the assigned state roles for a (workflow, state) pair, filtered by minimum assignment
     * type. Added for #1561 Phase 4b so {@code modules/extensions-workflow/.../PSStateRolesContext}
     * can load its data from the shared Hibernate session instead of opening a second pool connection.
     *
     * @param workflowAppId the workflow id, must be {@code > 0}.
     * @param stateId the state id, must be {@code > 0}.
     * @param minAssignmentType the minimum assignment type; passed through unchanged to
     *     {@code PSAssignedRole.assignmentType}.
     * @return the matching rows, never {@code null}, may be empty.
     */
    java.util.List<com.percussion.services.workflow.data.PSAssignedRole>
        findStateRoles(long workflowAppId, long stateId, int minAssignmentType);

   /**
    * Loads the {@code TRANSITIONNOTIFICATIONS} rows for a (workflowId, transitionId) pair.
    * Added for #1561 Phase 4c so {@code modules/extensions-workflow/.../PSTransitionNotificationsContext}
    * can load its data from the shared Hibernate session instead of opening a second pool connection.
    *
    * @param workflowAppId the workflow id, must be {@code > 0}.
    * @param transitionId the transition id, must be {@code > 0}.
    * @return the matching rows, never {@code null}, may be empty if no notifications are configured
    *     for the transition.
    */
   java.util.List<PSNotification> findTransitionNotifications(
       long workflowAppId, long transitionId);

   /**
    * Loads the workflow roles for the supplied workflow id, restricted to those whose ids appear
    * in the supplied role-id set. Used by Phase 4b to hydrate role names for the result of
    * {@link #findStateRoles}.
    *
    * @param workflowAppId the workflow id, must be {@code > 0}.
    * @param roleIds the role ids to load names for, must not be {@code null}.
    * @return the matching rows, never {@code null}, may be empty if none of the supplied ids
    *     exist in this workflow.
    */
   java.util.List<com.percussion.services.workflow.data.PSWorkflowRole>
       findWorkflowRoles(long workflowAppId, java.util.Set<Long> roleIds);

    /**
     * Loads a specified workflow state by name. This is a fast call, but will
     * return a shared instance that must not be modified.
     *
     * @param stateName the name of the specified state, not {@code null} or empty
     * @param workflowId the ID of the workflow which contains the specified
     *                   state, not {@code null}
     * @return the specified workflow state, may be {@code null} if the
     *         specified workflow state does not exist
     * @throws IllegalArgumentException if stateName is null/empty or workflowId is null
     */
    PSState loadWorkflowStateByName(String stateName, IPSGuid workflowId);

    /**
     * Loads a specified workflow state by name, returning an Optional for safe access.
     * This is the preferred method for state lookup by name as it provides null safety.
     *
     * @param stateName the name of the specified state, not {@code null} or empty
     * @param workflowId the ID of the workflow which contains the specified
     *                   state, not {@code null}
     * @return an Optional containing the workflow state if found, empty otherwise
     * @throws IllegalArgumentException if stateName is null/empty or workflowId is null
     */
    default Optional<PSState> findWorkflowStateByName(String stateName, IPSGuid workflowId) {
        return Optional.ofNullable(loadWorkflowStateByName(stateName, workflowId));
    }

    /**
     * Creates a state for the specified workflow. Caller is responsible to
     * set all properties (except the ID) for the created object.
     *
     * @param workflowId the workflow ID, not {@code null}
     * @return the created state with a new ID, never {@code null}
     * @throws IllegalArgumentException if workflowId is null
     */
    PSState createState(IPSGuid workflowId);

    /**
     * Creates a transition for a specified workflow and state.
     * Caller is responsible to set all properties (except the ID)
     * for the created object.
     *
     * @param wfId the workflow ID, not {@code null}
     * @param stateId the state ID, not {@code null}
     * @return the created transition with a new ID, never {@code null}
     * @throws IllegalArgumentException if wfId or stateId is null
     */
    PSTransition createTransition(IPSGuid wfId, IPSGuid stateId);

    /**
     * Creates a notification for a specified workflow and transition.
     * Caller is responsible to set all properties (except the ID)
     * for the created object.
     *
     * @param wfId the workflow ID, not {@code null}
     * @param transitionId the transition ID, not {@code null}
     * @return the created notification with a new ID, never {@code null}
     * @throws IllegalArgumentException if wfId or transitionId is null
     */
    PSNotification createNotification(IPSGuid wfId, IPSGuid transitionId);

    /**
     * Check if a workflow exists by ID.
     *
     * @param workflowId the workflow ID to check, not {@code null}
     * @return {@code true} if the workflow exists, {@code false} otherwise
     * @throws IllegalArgumentException if workflowId is null
     */
    default boolean workflowExists(IPSGuid workflowId) {
        return findWorkflow(workflowId).isPresent();
    }

    /**
     * Check if a state exists in the specified workflow.
     *
     * @param stateId the state ID to check, not {@code null}
     * @param workflowId the workflow ID, not {@code null}
     * @return {@code true} if the state exists in the workflow, {@code false} otherwise
     * @throws IllegalArgumentException if stateId or workflowId is null
     */
    default boolean stateExists(IPSGuid stateId, IPSGuid workflowId) {
        return findWorkflowState(stateId, workflowId).isPresent();
    }

    /**
     * Get all states for a workflow as a stream for efficient processing.
     *
     * @param workflowId the workflow ID, not {@code null}
     * @return Stream of states, never {@code null}
     * @throws IllegalArgumentException if workflowId is null
     */
    default Stream<PSState> streamWorkflowStates(IPSGuid workflowId) {
        return findWorkflow(workflowId)
            .map(PSWorkflow::getStates)
            .map(List::stream)
            .orElse(Stream.empty());
    }

    /**
     * Get all transitions for a workflow state as a stream for efficient processing.
     *
     * @param stateId the state ID, not {@code null}
     * @param workflowId the workflow ID, not {@code null}
     * @return Stream of transitions, never {@code null}
     * @throws IllegalArgumentException if stateId or workflowId is null
     */
    default Stream<PSTransition> streamStateTransitions(IPSGuid stateId, IPSGuid workflowId) {
        return findWorkflowState(stateId, workflowId)
            .map(PSState::getTransitions)
            .map(List::stream)
            .orElse(Stream.empty());
    }

    /**
     * Find workflows by name pattern with Optional result for single matches.
     *
     * @param name the exact name to match, not {@code null} or empty
     * @return an Optional containing the workflow if exactly one match is found, empty otherwise
     * @throws IllegalArgumentException if name is null or empty
     */
    default Optional<PSWorkflow> findWorkflowByExactName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("name cannot be null or empty");
        }
        var workflows = findWorkflowsByName(name);
        return workflows.size() == 1 ? Optional.of(workflows.get(0)) : Optional.empty();
    }

    /**
     * Count workflows matching the given name pattern.
     *
     * @param name the name pattern to match workflows
     * @return the count of matching workflows
     */
    default long countWorkflows(String name) {
        return streamWorkflows(name).count();
    }

    /**
     * Check if any workflows exist for the given name pattern.
     *
     * @param name the name pattern to match workflows
     * @return {@code true} if any workflows match, {@code false} otherwise
     */
    default boolean hasWorkflows(String name) {
        return streamWorkflows(name).findAny().isPresent();
    }

    /**
     * Copy all workflow assignments that reference {@code fromRole} to {@code toRole}.
     *
     * <p>This is primarily a convenience method used when renaming or duplicating roles.  The
     * implementation is responsible for efficiently updating any database records or caches
     * that refer to the original role.
     *
     * @param fromRole the name of the role to copy assignments from, not {@code null}
     * @param toRole the name of the role to copy assignments to, not {@code null}
     */
    void copyWorkflowToRole(String fromRole, String toRole);
}
