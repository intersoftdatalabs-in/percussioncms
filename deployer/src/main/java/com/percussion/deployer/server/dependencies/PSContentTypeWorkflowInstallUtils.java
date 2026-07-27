/*
 * Copyright 1999-2026 Percussion Software, Inc.
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

package com.percussion.deployer.server.dependencies;

import com.percussion.design.objectstore.PSContentEditor;
import com.percussion.design.objectstore.PSWorkflowInfo;
import com.percussion.services.workflow.data.PSWorkflow;
import com.percussion.utils.guid.IPSGuid;
import java.util.List;

/**
 * Pure helpers used when installing Content Type dependencies (package install).
 *
 * <p>Kept free of Spring/service static initializers so unit tests can exercise the logic without a
 * running CMS context. Used by {@link PSContentTypeDependencyHandler}.
 */
final class PSContentTypeWorkflowInstallUtils {

  private PSContentTypeWorkflowInstallUtils() {}

  /**
   * Extracts a UUID from a workflow GUID for default-workflow assignment.
   *
   * @param guid may be <code>null</code>
   * @return the UUID, or -1 if <code>guid</code> is null
   */
  static int uuidFromGuid(IPSGuid guid) {
    return guid == null ? -1 : guid.getUUID();
  }

  /**
   * Ensures an inclusionary workflow info list contains the content editor's default workflow id.
   * Server validation rejects content types whose default is outside the inclusion list.
   *
   * @param ce content editor, may not be <code>null</code>
   */
  static void ensureDefaultWorkflowInInclusionList(PSContentEditor ce) {
    if (ce == null) {
      throw new IllegalArgumentException("ce may not be null");
    }
    PSWorkflowInfo wfInfo = ce.getWorkflowInfo();
    if (wfInfo == null) {
      return;
    }
    if (!PSWorkflowInfo.TYPE_INCLUSIONARY.equals(wfInfo.getType())) {
      return;
    }
    Integer defaultId = Integer.valueOf(ce.getWorkflowId());
    List wfIds = wfInfo.getWorkflowIds();
    if (!wfIds.isEmpty() && !wfIds.contains(defaultId)) {
      wfIds.add(defaultId);
    }
  }

  /**
   * Whether a non-package-child workflow association should be dropped from workflow info.
   *
   * <p>Pre-modernization semantics: drop when the workflow does <em>not</em> resolve to an existing
   * target workflow. A Java 11 modernization inverted this to {@code wf != null}, which removed
   * valid target workflows and left only missing ones — causing install failures such as "default
   * workflow id X must be in the workflow info's inclusion list".
   *
   * @param resolvedOnTarget workflow loaded on the target (via id map), may be <code>null</code>
   * @return <code>true</code> if the association should be removed
   */
  static boolean shouldRemoveWorkflowAssociation(PSWorkflow resolvedOnTarget) {
    return resolvedOnTarget == null;
  }
}
