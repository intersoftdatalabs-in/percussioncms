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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.design.objectstore.PSContentEditor;
import com.percussion.design.objectstore.PSWorkflowInfo;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.guidmgr.data.PSGuid;
import com.percussion.services.workflow.data.PSWorkflow;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link PSContentTypeWorkflowInstallUtils} (content-type package install workflow
 * helpers).
 *
 * <p>Regressions: inverted remove-workflow condition; Spring JDK proxy cast on default workflow;
 * default workflow missing from inclusion list.
 */
public class PSContentTypeWorkflowInstallUtilsTest {

  @Test
  public void testShouldRemoveWorkflowAssociation_whenMissingOnTarget() {
    assertTrue(
        PSContentTypeWorkflowInstallUtils.shouldRemoveWorkflowAssociation(null),
        "Missing target workflow must be dropped (pre-modernization semantics)");
  }

  @Test
  public void testShouldRemoveWorkflowAssociation_whenPresentOnTarget() {
    PSWorkflow present = new PSWorkflow();
    assertFalse(
        PSContentTypeWorkflowInstallUtils.shouldRemoveWorkflowAssociation(present),
        "Existing target workflow must be kept — modernization inverted this to remove-on-present");
  }

  @Test
  public void testUuidFromGuid_nullSafe() {
    assertEquals(-1, PSContentTypeWorkflowInstallUtils.uuidFromGuid(null));
    assertEquals(
        4, PSContentTypeWorkflowInstallUtils.uuidFromGuid(new PSGuid(PSTypeEnum.WORKFLOW, 4)));
  }

  @Test
  public void testEnsureDefaultWorkflowInInclusionList_addsMissingDefault() {
    PSContentEditor ce = newContentEditorWithWorkflow(4, Arrays.asList(5, 6, 7));
    PSContentTypeWorkflowInstallUtils.ensureDefaultWorkflowInInclusionList(ce);
    assertTrue(
        ce.getWorkflowInfo().getWorkflowIds().contains(Integer.valueOf(4)),
        "Default workflow id 4 must be added to inclusion list");
    assertEquals(4, ce.getWorkflowId());
  }

  @Test
  public void testEnsureDefaultWorkflowInInclusionList_noopWhenAlreadyPresent() {
    PSContentEditor ce = newContentEditorWithWorkflow(6, Arrays.asList(4, 5, 6, 7));
    int sizeBefore = ce.getWorkflowInfo().getWorkflowIds().size();
    PSContentTypeWorkflowInstallUtils.ensureDefaultWorkflowInInclusionList(ce);
    assertEquals(sizeBefore, ce.getWorkflowInfo().getWorkflowIds().size());
    assertTrue(ce.getWorkflowInfo().getWorkflowIds().contains(Integer.valueOf(6)));
  }

  @Test
  public void testEnsureDefaultWorkflowInInclusionList_noopWhenNullInfo() {
    PSContentEditor ce = new PSContentEditor("ceTest", 100L, 4);
    PSContentTypeWorkflowInstallUtils.ensureDefaultWorkflowInInclusionList(ce);
    assertEquals(4, ce.getWorkflowId());
  }

  @Test
  public void testEnsureDefaultWorkflowInInclusionList_rejectsNullEditor() {
    assertThrows(
        IllegalArgumentException.class,
        () -> PSContentTypeWorkflowInstallUtils.ensureDefaultWorkflowInInclusionList(null));
  }

  private static PSContentEditor newContentEditorWithWorkflow(int defaultId, List<Integer> ids) {
    PSContentEditor ce = new PSContentEditor("ceTest", 100L, defaultId);
    ce.setWorkflowInfo(
        new PSWorkflowInfo(PSWorkflowInfo.TYPE_INCLUSIONARY, new ArrayList<>(ids)));
    return ce;
  }
}
