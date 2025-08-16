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

import com.percussion.cms.objectstore.PSComponentSummary;
import com.percussion.cx.objectstore.PSMenuAction;
import com.percussion.cx.objectstore.PSParameters;
import com.percussion.cx.objectstore.PSProperties;
import com.percussion.i18n.PSI18nUtils;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.guidmgr.IPSGuidManager;
import com.percussion.services.guidmgr.PSGuidManagerLocator;
import com.percussion.services.legacy.IPSCmsObjectMgr;
import com.percussion.services.legacy.PSCmsObjectMgrLocator;
import com.percussion.services.workflow.data.PSAssignmentTypeEnum;
import com.percussion.services.workflow.data.PSContentApproval;
import com.percussion.services.workflow.data.PSState;
import com.percussion.services.workflow.data.PSTransition;
import com.percussion.services.workflow.data.PSTransitionRole;
import com.percussion.services.workflow.data.PSWorkflow;
import com.percussion.services.workflow.data.PSTransition.PSWorkflowCommentEnum;
import com.percussion.system.utils.IPSHtmlParameters;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.workflow.PSWorkFlowUtils;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * This class provides help in calculating available workflow actions for one or more items
 * with Java 11 modernization. It calculates the requested actions for each item specified
 * during construction and then returns the actions common for all items.
 *
 * <h2>Java 11 Features</h2>
 * <ul>
 * <li>Stream API for efficient action filtering and processing</li>
 * <li>Optional-based safe access for null handling</li>
 * <li>Enhanced validation with Objects.requireNonNull</li>
 * <li>Immutable collections for action metadata</li>
 * <li>Factory methods for action creation</li>
 * </ul>
 *
 * <p>For optimal performance, this class should be used within a service call that
 * has initiated a transaction.
 *
 * @author dougrand
 */
public final class PSWorkflowActionsHelper {

    /**
     * Logger for this class.
     */
    private static final Logger ms_logger = LogManager.getLogger(PSWorkflowActionsHelper.class);

    /**
     * The name of the parameter used to specify the workflow action trigger.
     */
    private static final String ms_actionTriggerName = IPSHtmlParameters.SYS_WF_ACTION;

    /**
     * Standard action URLs and labels.
     */
    private static final String CHECKIN_URL = "../sys_cxItemAssembly/checkIn.html";
    private static final String CHECKOUT_URL = "../sys_cxItemAssembly/checkOut.html";
    private static final String TRANS_URL = "../sys_cxItemAssembly/itemTransition.html";
    private static final String ADHOC_TRANS_URL = "../sys_cxItemAssembly/adhocTransition.html";

    private static final String CHECKIN_ACTION_LABEL = "Check In";
    private static final String FORCE_CHECKIN_ACTION_LABEL = "Force Check In";
    private static final String CHECKOUT_ACTION_LABEL = "Check Out";
    private static final String TRANSITION_NAME = "TransitionName";
    private static final String SYS_TRANSITIONID = IPSHtmlParameters.SYS_TRANSITIONID;

    /**
     * Standard properties for actions.
     */
    private static final PSProperties ms_stdProps = createStandardProperties();
    private static final PSProperties ms_adhocProps = createAdhocProperties();
    private static final PSParameters ms_stdParams = createStandardParameters();

    /**
     * Immutable list of item information objects.
     */
    private final List<PSItemInfo> m_itemInfoList;

    /**
     * The user name for action calculations.
     */
    private final String m_userName;

    /**
     * The user's roles for action calculations.
     */
    private final List<String> m_userRoles;

    /**
     * The locale for action label localization.
     */
    private final String m_locale;

    /**
     * Lazily loaded user approvals cache.
     */
    private List<PSContentApproval> m_approvals;

    /**
     * Construct the actions helper with the information required to calculate possible actions.
     *
     * @param contentids A list of content ids, not {@code null} or empty,
     *                   for which actions will be calculated.
     * @param assignmentTypes The assignment types for each of the supplied content ids,
     *                        not {@code null} or empty, must contain the same number of elements
     *                        as the content id list.
     * @param userName The name of the user for whom the actions will be calculated,
     *                 not {@code null} or empty.
     * @param userRoles The names of the roles the user is a member of,
     *                  not {@code null}, may be empty.
     * @param locale The locale to use for localizing action labels,
     *               may be {@code null} or empty to use the default locale.
     * @throws IllegalArgumentException if any required parameter is invalid
     */
    public PSWorkflowActionsHelper(List<IPSGuid> contentids,
                                  List<PSAssignmentTypeEnum> assignmentTypes,
                                  String userName,
                                  List<String> userRoles,
                                  String locale) {

        Objects.requireNonNull(contentids, "contentids cannot be null");
        Objects.requireNonNull(assignmentTypes, "assignmentTypes cannot be null");
        Objects.requireNonNull(userRoles, "userRoles cannot be null");

        if (contentids.isEmpty()) {
            throw new IllegalArgumentException("contentids cannot be empty");
        }

        if (contentids.size() != assignmentTypes.size()) {
            throw new IllegalArgumentException(
                "The number of contentids must match the number of assignment types");
        }

        if (StringUtils.isBlank(userName)) {
            throw new IllegalArgumentException("userName cannot be null or empty");
        }

        this.m_userName = userName.trim();
        this.m_userRoles = List.copyOf(userRoles);
        this.m_locale = StringUtils.isBlank(locale) ? null : locale.trim();

        // Build immutable item info list using streams
        var mgr = PSCmsObjectMgrLocator.getObjectManager();
        this.m_itemInfoList = contentids.stream()
            .map(contentId -> {
                var index = contentids.indexOf(contentId);
                var type = assignmentTypes.get(index);
                var summary = mgr.loadComponentSummary(contentId.getUUID());
                return summary != null
                    ? Optional.of(createItemInfo(contentId, type, summary))
                    : Optional.<PSItemInfo>empty();
            })
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.toUnmodifiableList());
    }

    /**
     * Factory method to create PSItemInfo instances.
     */
    private PSItemInfo createItemInfo(IPSGuid contentId, PSAssignmentTypeEnum assignmentType,
                                    PSComponentSummary summary) {
        return new PSItemInfo(
            contentId,
            assignmentType,
            summary.getWorkflowAppId(),
            summary.getContentStateId(),
            summary.getCheckoutUserName()
        );
    }

    /**
     * Get all actions including the CIAO and transition actions.
     *
     * @return The list of actions, never {@code null}, may be empty.
     * @throws PSWorkflowException If there is an error determining the item status.
     */
    public List<PSMenuAction> getAllWorkflowActions() throws PSWorkflowException {
        var ciaoActions = getCIAOActions();
        var transitionActions = getTransitionActions();

        return ciaoActions.stream()
            .collect(() -> new ArrayList<>(ciaoActions),
                    (list, action) -> {}, // Already added via constructor
                    (list1, list2) -> list1.addAll(list2))
            .stream()
            .collect(() -> {
                var result = new ArrayList<>(ciaoActions);
                result.addAll(transitionActions);
                return result;
            }, (list, action) -> {}, ArrayList::addAll);
    }

    /**
     * Get the checkin and checkout actions possible for the user and items.
     * If multiple items were specified, then the intersection of the possible actions is returned.
     *
     * @return The list of actions, never {@code null}, may be empty.
     * @throws PSWorkflowException If there is an error determining the item status.
     */
    public List<PSMenuAction> getCIAOActions() throws PSWorkflowException {
        return m_itemInfoList.stream()
            .map(this::createCIAOActionForItem)
            .reduce(this::intersectActions)
            .map(action -> action != null ? List.of(action) : List.<PSMenuAction>of())
            .orElse(List.of());
    }

    /**
     * Create CIAO action for a single item.
     */
    private PSMenuAction createCIAOActionForItem(PSItemInfo info) {
        if (m_userName.equals(info.getCheckedOutUserName())) {
            return createCIAOAction(PSMenuAction.CHECKIN_ACTION_NAME, CHECKIN_URL,
                PSWorkFlowUtils.TRIGGER_CHECK_IN, CHECKIN_ACTION_LABEL);
        } else if (info.isCheckedOut() && info.hasAdminAccess()) {
            return createCIAOAction(PSMenuAction.FORCE_CHECKIN_ACTION_NAME, CHECKIN_URL,
                PSWorkFlowUtils.TRIGGER_FORCE_CHECK_IN, FORCE_CHECKIN_ACTION_LABEL);
        } else if (!info.isCheckedOut() && info.hasAssigneeAccess()) {
            return createCIAOAction(PSMenuAction.CHECKOUT_ACTION_NAME, CHECKOUT_URL,
                PSWorkFlowUtils.TRIGGER_CHECK_OUT, CHECKOUT_ACTION_LABEL);
        }
        return null;
    }

    /**
     * Intersect two actions for multi-item scenarios.
     */
    private PSMenuAction intersectActions(PSMenuAction action1, PSMenuAction action2) {
        return Objects.equals(action1, action2) ? action1 : null;
    }

    /**
     * Gets the list of possible transition actions common to each item.
     *
     * @return The list of actions, never {@code null}, may be empty.
     */
    public List<PSMenuAction> getTransitionActions() {
        var svc = PSWorkflowServiceLocator.getWorkflowService();

        return m_itemInfoList.stream()
            .map(info -> getTransitionActionsForItem(svc, info))
            .reduce(this::intersectActionLists)
            .orElse(List.of());
    }

    /**
     * Intersect two action lists, removing adhoc actions from subsequent lists.
     */
    private List<PSMenuAction> intersectActionLists(List<PSMenuAction> list1, List<PSMenuAction> list2) {
        var filteredList2 = removeAdhocActions(list2);
        return list1.stream()
            .filter(filteredList2::contains)
            .collect(Collectors.toList());
    }

    /**
     * Remove adhoc actions from the action list using streams.
     */
    private List<PSMenuAction> removeAdhocActions(List<PSMenuAction> actions) {
        return actions.stream()
            .filter(action -> !PSMenuAction.VAL_BOOLEAN_TRUE.equals(
                action.getParameters().getParameter(PSMenuAction.SHOW_ADHOC)))
            .collect(Collectors.toList());
    }

    /**
     * Get transition actions for a single item.
     */
    private List<PSMenuAction> getTransitionActionsForItem(IPSWorkflowService svc, PSItemInfo info) {
        // Early returns for access checks
        if (!info.hasAssigneeAccess() ||
            (!info.hasAdminAccess() && info.isCheckedOut() &&
             !Objects.equals(info.getCheckedOutUserName(), m_userName))) {
            return List.of();
        }

        return svc.findWorkflowState(info.getStateId(), info.getWorkflowId())
            .filter(state -> !hasUserActed(svc, info))
            .map(state -> state.getTransitions().stream()
                .filter(trans -> info.hasAdminAccess() || canActInRole(svc, info, trans))
                .map(trans -> createTransitionAction(svc, trans, info))
                .collect(Collectors.toList()))
            .orElseGet(() -> {
                logStateNotFound(info);
                return List.of();
            });
    }

    /**
     * Log state not found error with proper context.
     */
    private void logStateNotFound(PSItemInfo info) {
        var message = "Failed to calculate workflow actions for item with contentid {0}: " +
            "No state found for workflowid {1} and stateid {2}";
        var args = new Object[]{
            info.getContentId().getUUID(),
            info.getWorkflowId().getUUID(),
            info.getStateId().getUUID()
        };
        ms_logger.error(MessageFormat.format(message, args));
    }

    /**
     * Create a transition action with enhanced error handling.
     */
    private PSMenuAction createTransitionAction(IPSWorkflowService svc, PSTransition trans, PSItemInfo info) {
        var guidMgr = PSGuidManagerLocator.getGuidMgr();

        return svc.findWorkflowState(
                guidMgr.makeGuid(trans.getToState(), PSTypeEnum.WORKFLOW_STATE),
                info.getWorkflowId())
            .map(toState -> {
                var label = getTransitionLabel(trans, info);
                var isAdhoc = toState.isAdhocEnabled();
                var action = createAction(
                    trans.getName(),
                    isAdhoc ? ADHOC_TRANS_URL : TRANS_URL,
                    trans.getTrigger(),
                    label,
                    String.valueOf(trans.getGUID().longValue()),
                    isAdhoc
                );

                enhanceTransitionAction(action, trans, isAdhoc);
                return action;
            })
            .orElseThrow(() -> new IllegalStateException(
                "Cannot find to-state for transition: " + trans.getGUID()));
    }

    /**
     * Get localized transition label.
     */
    private String getTransitionLabel(PSTransition trans, PSItemInfo info) {
        var key = PSI18nUtils.PSX_WORKFLOW_TRANSITION +
            PSI18nUtils.LOOKUP_KEY_SEPARATOR +
            info.getWorkflowId().longValue() +
            PSI18nUtils.LOOKUP_KEY_SEPARATOR +
            trans.getGUID().longValue() +
            PSI18nUtils.LOOKUP_KEY_SEPARATOR_LAST +
            trans.getLabel();

        return PSI18nUtils.getString(key, m_locale);
    }

    /**
     * Enhance transition action with additional parameters.
     */
    private void enhanceTransitionAction(PSMenuAction action, PSTransition trans, boolean isAdhoc) {
        var params = action.getParameters();
        params.setParameter(ms_actionTriggerName, trans.getTrigger());

        if (isAdhoc) {
            action.setAdhocParam(true);
        }

        getCommentRequirement(trans.getRequiresComment())
            .ifPresent(action::setCommentRequired);
    }

    /**
     * Get comment requirement value for transition.
     */
    private Optional<String> getCommentRequirement(PSWorkflowCommentEnum commentEnum) {
        if (commentEnum.equals(PSWorkflowCommentEnum.REQUIRED)) {
            return Optional.of(PSMenuAction.VAL_BOOLEAN_TRUE);
        } else if (commentEnum.equals(PSWorkflowCommentEnum.DO_NOT_SHOW)) {
            return Optional.of(PSMenuAction.VAL_HIDE);
        }
        return Optional.empty();
    }

    /**
     * Check if user has already acted on the item.
     */
    private boolean hasUserActed(IPSWorkflowService svc, PSItemInfo info) {
        if (m_approvals == null) {
            m_approvals = svc.findApprovalsByUser(m_userName);
        }

        return m_approvals.stream()
            .anyMatch(approval ->
                approval.getWorkflowId() == info.getWorkflowId().longValue() &&
                approval.getStateId() == info.getStateId().longValue() &&
                approval.getContentId() == info.getContentId().getUUID());
    }

    /**
     * Check if user can act in required role for transition.
     */
    private boolean canActInRole(IPSWorkflowService svc, PSItemInfo info, PSTransition trans) {
        if (trans.isAllowAllRoles()) {
            return true;
        }

        return svc.findWorkflow(info.getWorkflowId())
            .map(workflow -> {
                var userRoleIds = workflow.getRoleIds(m_userRoles);
                var transRoleIds = getTransitionRoleIds(trans);
                var intersection = new HashSet<>(userRoleIds);
                intersection.retainAll(transRoleIds);

                if (intersection.isEmpty()) {
                    return false;
                }

                // Check if roles are still available (not used by other approvals)
                var usedRoleIds = getUsedRoleIds(svc, info);
                intersection.removeAll(usedRoleIds);

                return !intersection.isEmpty();
            })
            .orElse(false);
    }

    /**
     * Get role IDs for transition using streams.
     */
    private Set<Integer> getTransitionRoleIds(PSTransition trans) {
        return trans.getTransitionRoles().stream()
            .mapToInt(transRole -> (int) transRole.getRoleId())
            .boxed()
            .collect(Collectors.toSet());
    }

    /**
     * Get already used role IDs for the item.
     */
    private Set<Integer> getUsedRoleIds(IPSWorkflowService svc, PSItemInfo info) {
        return svc.findApprovalsByItem(info.getContentId()).stream()
            .filter(approval ->
                approval.getWorkflowId() == info.getWorkflowId().getUUID() &&
                approval.getStateId() == info.getStateId().getUUID())
            .map(PSContentApproval::getRoleId)
            .collect(Collectors.toSet());
    }

    // Factory methods for creating actions and properties

    /**
     * Create a checkin or checkout action with localization.
     */
    private PSMenuAction createCIAOAction(String name, String url, String triggerName, String label) {
        var localizedLabel = PSI18nUtils.getString(
            PSI18nUtils.PSX_CE_ACTION + PSI18nUtils.LOOKUP_KEY_SEPARATOR_LAST + label,
            m_locale);

        var trigger = PSWorkFlowUtils.properties.getProperty(triggerName);
        return createAction(name, url, trigger, localizedLabel, "", false);
    }

    /**
     * Create a workflow action with all required parameters.
     */
    private PSMenuAction createAction(String name, String url, String triggerName,
                                    String label, String transId, boolean isAdhoc) {
        var props = new PSProperties(isAdhoc ? ms_adhocProps : ms_stdProps);
        var params = new PSParameters(ms_stdParams);

        var trigger = PSWorkFlowUtils.properties.getProperty(triggerName);
        if (StringUtils.isBlank(trigger)) {
            trigger = triggerName;
        }

        params.setParameter(TRANSITION_NAME, label);
        params.setParameter(ms_actionTriggerName, triggerName);
        params.setParameter(SYS_TRANSITIONID, transId);

        var action = new PSMenuAction(name, label, PSMenuAction.TYPE_MENUITEM,
            url, PSMenuAction.HANDLER_SERVER, 0);
        action.setParameters(params);
        action.setProperties(props);

        return action;
    }

    /**
     * Get the parameter name used to specify the workflow action trigger.
     */
    public static String getTriggerParamName() {
        return ms_actionTriggerName;
    }

    // Static factory methods for properties

    private static PSProperties createStandardProperties() {
        var props = new PSProperties();
        // Add standard properties as needed
        return props;
    }

    private static PSProperties createAdhocProperties() {
        var props = new PSProperties();
        props.setProperty(PSMenuAction.SHOW_ADHOC, PSMenuAction.VAL_BOOLEAN_TRUE);
        return props;
    }

    private static PSParameters createStandardParameters() {
        var params = new PSParameters();
        // Add standard parameters as needed
        return params;
    }

    /**
     * Immutable item information class with enhanced validation.
     */
    private static final class PSItemInfo {
        private final IPSGuid contentId;
        private final PSAssignmentTypeEnum assignmentType;
        private final IPSGuid workflowId;
        private final IPSGuid stateId;
        private final String checkedOutUserName;

        PSItemInfo(IPSGuid contentId, PSAssignmentTypeEnum assignmentType,
                  IPSGuid workflowId, IPSGuid stateId, String checkedOutUserName) {
            this.contentId = Objects.requireNonNull(contentId, "contentId cannot be null");
            this.assignmentType = Objects.requireNonNull(assignmentType, "assignmentType cannot be null");
            this.workflowId = Objects.requireNonNull(workflowId, "workflowId cannot be null");
            this.stateId = Objects.requireNonNull(stateId, "stateId cannot be null");
            this.checkedOutUserName = checkedOutUserName;
        }

        IPSGuid getContentId() { return contentId; }
        IPSGuid getWorkflowId() { return workflowId; }
        IPSGuid getStateId() { return stateId; }
        String getCheckedOutUserName() { return checkedOutUserName; }

        boolean isCheckedOut() {
            return !StringUtils.isBlank(checkedOutUserName);
        }

        boolean hasAssigneeAccess() {
            return assignmentType == PSAssignmentTypeEnum.ASSIGNEE ||
                   assignmentType == PSAssignmentTypeEnum.ADMIN;
        }

        boolean hasAdminAccess() {
            return assignmentType == PSAssignmentTypeEnum.ADMIN;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof PSItemInfo)) return false;
            var other = (PSItemInfo) obj;
            return Objects.equals(contentId, other.contentId) &&
                   assignmentType == other.assignmentType &&
                   Objects.equals(workflowId, other.workflowId) &&
                   Objects.equals(stateId, other.stateId) &&
                   Objects.equals(checkedOutUserName, other.checkedOutUserName);
        }

        @Override
        public int hashCode() {
            return Objects.hash(contentId, assignmentType, workflowId, stateId, checkedOutUserName);
        }

        @Override
        public String toString() {
            return "PSItemInfo{" +
                "contentId=" + contentId +
                ", assignmentType=" + assignmentType +
                ", workflowId=" + workflowId +
                ", stateId=" + stateId +
                ", checkedOut=" + isCheckedOut() +
                '}';
        }
    }
}
