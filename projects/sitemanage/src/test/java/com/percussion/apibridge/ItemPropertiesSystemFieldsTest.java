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
package com.percussion.apibridge;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.percussion.cms.objectstore.PSComponentSummary;
import com.percussion.cms.objectstore.PSCoreItem;
import com.percussion.cms.objectstore.PSItemField;
import com.percussion.cms.objectstore.PSTextValue;
import org.junit.jupiter.api.Test;

class ItemPropertiesSystemFieldsTest {

  @Test
  void blankWorkflowIdIsCopiedFromTheSummary() {
    PSCoreItem core = mock(PSCoreItem.class);
    PSItemField workflow = blankField();
    when(core.getFieldByName("sys_workflowid")).thenReturn(workflow);
    PSComponentSummary summary = summaryWithWorkflow(4);

    ItemPropertiesSystemFields.preserveBlank(core, summary);

    verify(core).setTextField("sys_workflowid", "4");
  }

  @Test
  void storedWorkflowIdIsLeftAlone() {
    PSCoreItem core = mock(PSCoreItem.class);
    PSItemField workflow = mock(PSItemField.class);
    when(workflow.getValue()).thenReturn(new PSTextValue("9"));
    when(core.getFieldByName("sys_workflowid")).thenReturn(workflow);
    PSComponentSummary summary = summaryWithWorkflow(4);

    ItemPropertiesSystemFields.preserveBlank(core, summary);

    verify(core, never()).setTextField(org.mockito.ArgumentMatchers.eq("sys_workflowid"), any());
  }

  @Test
  void absentWorkflowFieldIsNotWritten() {
    PSCoreItem core = mock(PSCoreItem.class);
    when(core.getFieldByName("sys_workflowid")).thenReturn(null);
    PSComponentSummary summary = summaryWithWorkflow(4);

    assertFalse(ItemPropertiesSystemFields.needsBackfill(core));
    ItemPropertiesSystemFields.preserveBlank(core, summary);

    verify(core, never()).setTextField(any(), any());
  }

  @Test
  void blankStartDateIsNotCopied() {
    PSCoreItem core = mock(PSCoreItem.class);
    PSItemField start = blankField();
    when(core.getFieldByName("sys_contentstartdate")).thenReturn(start);
    when(core.getFieldByName("sys_workflowid")).thenReturn(null);
    when(core.getFieldByName("sys_communityid")).thenReturn(null);

    assertFalse(ItemPropertiesSystemFields.needsBackfill(core));
  }

  @Test
  void zeroWorkflowIdIsNotWrittenOverABlankField() {
    PSCoreItem core = mock(PSCoreItem.class);
    PSItemField workflow = blankField();
    when(core.getFieldByName("sys_workflowid")).thenReturn(workflow);
    PSComponentSummary summary = summaryWithWorkflow(0);

    ItemPropertiesSystemFields.preserveBlank(core, summary);

    verify(core, never()).setTextField(org.mockito.ArgumentMatchers.eq("sys_workflowid"), any());
  }

  @Test
  void checkoutEditRevisionReplacesThePreviewRevision() {
    PSCoreItem core = mock(PSCoreItem.class);
    when(core.getRevision()).thenReturn(10);
    PSComponentSummary summary = mock(PSComponentSummary.class);
    when(summary.getEditRevision()).thenReturn(11);

    ItemPropertiesSystemFields.useCheckoutEditRevision(core, summary);

    verify(core).setRevision(11);
    verify(core).setEditRevision(11);
  }

  @Test
  void matchingCheckoutRevisionIsLeftAlone() {
    PSCoreItem core = mock(PSCoreItem.class);
    when(core.getRevision()).thenReturn(1);
    PSComponentSummary summary = mock(PSComponentSummary.class);
    when(summary.getEditRevision()).thenReturn(1);

    ItemPropertiesSystemFields.useCheckoutEditRevision(core, summary);

    verify(core, never()).setRevision(anyInt());
    verify(core, never()).setEditRevision(anyInt());
  }

  @Test
  void uncheckedItemKeepsTheLoadedRevision() {
    PSCoreItem core = mock(PSCoreItem.class);
    PSComponentSummary summary = mock(PSComponentSummary.class);
    when(summary.getEditRevision()).thenReturn(-1);

    ItemPropertiesSystemFields.useCheckoutEditRevision(core, summary);

    verify(core, never()).setRevision(anyInt());
  }

  private static PSItemField blankField() {
    PSItemField field = mock(PSItemField.class);
    when(field.getValue()).thenReturn(null);
    return field;
  }

  private static PSComponentSummary summaryWithWorkflow(int workflowId) {
    PSComponentSummary summary = mock(PSComponentSummary.class);
    when(summary.getWorkflowAppId()).thenReturn(workflowId);
    when(summary.getCommunityId()).thenReturn(0);
    when(summary.getVersion()).thenReturn(null);
    return summary;
  }
}
