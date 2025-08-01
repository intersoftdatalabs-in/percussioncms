/*
 * Copyright 1999-2023 Percussion Software, Inc.
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

package com.percussion.apibridge;

/**
 * Defines default workflow state names for Percussion CMS.
 * These are used throughout the system for workflow transitions.
 */
public final class DefaultWorkflowStates {

    /** The "Live" workflow state. */
    public static final String LIVE = "Live";
    /** The "Quick Edit" workflow state. */
    public static final String QUICK_EDIT = "Quick Edit";
    /** The "Draft" workflow state. */
    public static final String DRAFT = "Draft";
    /** The "Archive" workflow state. */
    public static final String ARCHIVE = "Archive";
    /** The "Review" workflow state. */
    public static final String REVIEW = "Review";
    /** The "Pending" workflow state. */
    public static final String PENDING = "Pending";

    private DefaultWorkflowStates() {
        // Utility class; prevent instantiation.
    }
}
