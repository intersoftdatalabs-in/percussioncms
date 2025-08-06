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
package com.percussion.workflow.service.impl;

import com.percussion.system.utils.PSSiteManageBean;
import com.percussion.workflow.service.IPSSteppedWorkflowMetadata;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

/**
 * Provides metadata for stepped workflows, including system states and excluded workflows.
 */
@PSSiteManageBean("steppedWorkflowMetadata")
@Lazy
public class PSSteppedWorkflowMetadata implements IPSSteppedWorkflowMetadata {

    // Workflow names to exclude from the list to show
    static final Set<String> excludedWorkflows = Set.of(
        "LocalContent", "Standard Workflow", "Simple Workflow"
    );

    // Steps names to exclude from the list to show
    static final Set<String> excludedStates = Set.of(
        "Pending", "Live", "Quick Edit"
    );

    // Constants for workflow states
    static final String DRAFT_STATE = "Draft";
    static final String REVIEW_STATE = "Review";
    static final String QUICK_EDIT_STATE = "Quick Edit";
    static final String PENDING_STATE = "Pending";
    static final String LIVE_STATE = "Live";
    static final String ARCHIVE_STATE = "Archive";

    // Constants for transitions
    static final String TRANSITION_NAME_EDIT = "Edit";
    static final String TRANSITION_NAME_LIVE = "Live";
    static final String TRANSITION_NAME_REMOVE = "Remove";
    static final String TRANSITION_NAME_ARCHIVE = "Archive";
    static final String TRANSITION_NAME_RESUBMIT = "Resubmit";
    static final String TRANSITION_NAME_APPROVE = "Approve";
    static final String TRANSITION_NAME_REJECT = "Reject";
    static final String TRANSITION_NAME_SUBMIT = "Submit";
    static final String TRANSITION_NAME_PUBLISH = "Publish";

    // List of default step transitions
    static final List<String> defaultTransitions = List.of(
        TRANSITION_NAME_SUBMIT, TRANSITION_NAME_REJECT, TRANSITION_NAME_APPROVE,
        TRANSITION_NAME_PUBLISH, TRANSITION_NAME_ARCHIVE
    );

    // Ordered list of transitions names
    static final List<String> orderedTransitions = List.of(
        TRANSITION_NAME_SUBMIT, TRANSITION_NAME_RESUBMIT, TRANSITION_NAME_REJECT,
        TRANSITION_NAME_APPROVE, TRANSITION_NAME_PUBLISH, TRANSITION_NAME_ARCHIVE
    );

    // Steps names that are locked down to the system
    static final List<String> systemStatesList = List.of(
        DRAFT_STATE, QUICK_EDIT_STATE, REVIEW_STATE, PENDING_STATE, LIVE_STATE, ARCHIVE_STATE
    );

    @Override
    public List<String> getSystemStatesList() {
        return systemStatesList;
    }
}
