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
package com.percussion.relationship.effect;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.intsof.percussioncms.auditlog.codes.CmsErrorCodes;
import com.intsof.percussioncms.auditlog.codes.ExtensionErrorCodes;
import com.intsof.percussioncms.auditlog.codes.ServerErrorCodes;
import com.percussion.cms.IPSCmsErrors;
import com.percussion.error.IPSErrorCode;
import com.percussion.error.PSException;
import com.percussion.error.PSNotFoundException;
import com.percussion.extension.IPSExtensionErrors;
import com.percussion.extension.PSExtensionProcessingException;
import com.percussion.log.PSLogServerWarning;
import com.percussion.relationship.IPSExecutionContext;
import com.percussion.relationship.PSTestResult;
import com.percussion.server.IPSRequestContext;
import com.percussion.server.IPSServerErrors;
import com.percussion.system.utils.IPSHtmlParameters;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Issue #4156 (parent #2616 leftover): relationship-effect production sites throw typed {@code
 * *ErrorCodes} via IPSErrorCode-aware constructors — not bare {@code IPS*Errors} ints. Dual-write
 * skip is {@code isAuditable()==false} on leftover operational catalog codes.
 */
@Tag("UnitTest")
class PSRelationshipEffectLeftoverErrorCodesSliceTest {

  @Test
  void leftoverCatalogsMatchLegacyIntsAndSkipDualWrite() {
    assertEquals(
        IPSExtensionErrors.EXT_MISSING_HTML_PARAMETER_ERROR,
        ExtensionErrorCodes.EXT_MISSING_HTML_PARAMETER_ERROR.numericCode());
    assertEquals(
        IPSExtensionErrors.ILLEGAL_EXECUTION_CONTEXT,
        ExtensionErrorCodes.ILLEGAL_EXECUTION_CONTEXT.numericCode());
    assertEquals(
        IPSExtensionErrors.NONPROMOTABLE_RELATIONSHIP,
        ExtensionErrorCodes.NONPROMOTABLE_RELATIONSHIP.numericCode());
    assertEquals(
        IPSExtensionErrors.WORKFLOWID_IN_REQUEST_ISNULL,
        ExtensionErrorCodes.WORKFLOWID_IN_REQUEST_ISNULL.numericCode());
    assertEquals(
        IPSExtensionErrors.INVALID_WORKFLOW_ACTION,
        ExtensionErrorCodes.INVALID_WORKFLOW_ACTION.numericCode());
    assertEquals(
        IPSExtensionErrors.ITEM_NOT_IN_PUBLIC_STATE,
        ExtensionErrorCodes.ITEM_NOT_IN_PUBLIC_STATE.numericCode());
    assertEquals(
        IPSExtensionErrors.EFFECT_SELF_TRIGGERED,
        ExtensionErrorCodes.EFFECT_SELF_TRIGGERED.numericCode());
    assertEquals(
        IPSExtensionErrors.INVALID_OPTION_FOR_FORCETRANSITION,
        ExtensionErrorCodes.INVALID_OPTION_FOR_FORCETRANSITION.numericCode());
    assertEquals(
        IPSExtensionErrors.INVALID_TRANSITION_FOR_EFFECT,
        ExtensionErrorCodes.INVALID_TRANSITION_FOR_EFFECT.numericCode());
    assertEquals(
        IPSExtensionErrors.MISSING_INTERNAL_REQUEST_RESOURCE,
        ExtensionErrorCodes.MISSING_INTERNAL_REQUEST_RESOURCE.numericCode());
    assertEquals(
        IPSExtensionErrors.DEPENDENT_ITEM_NOT_IN_DESIRED_STATE,
        ExtensionErrorCodes.DEPENDENT_ITEM_NOT_IN_DESIRED_STATE.numericCode());
    assertEquals(
        IPSExtensionErrors.DEPENDENT_ITEM_CANNOT_GOTO_DESIRED_STATE,
        ExtensionErrorCodes.DEPENDENT_ITEM_CANNOT_GOTO_DESIRED_STATE.numericCode());
    assertEquals(
        IPSExtensionErrors.EFFECT_VALIDATE_MESSAGE,
        ExtensionErrorCodes.EFFECT_VALIDATE_MESSAGE.numericCode());
    assertEquals(
        IPSExtensionErrors.PROMOTE_TRANSITION_FAILED,
        ExtensionErrorCodes.PROMOTE_TRANSITION_FAILED.numericCode());
    assertEquals(
        IPSExtensionErrors.MANDATORY_TRANSITION_VALIDATION_FAILURE,
        ExtensionErrorCodes.MANDATORY_TRANSITION_VALIDATION_FAILURE.numericCode());
    assertEquals(
        IPSServerErrors.MISSING_INTERNAL_REQUEST_RESOURCE,
        ServerErrorCodes.MISSING_INTERNAL_REQUEST_RESOURCE.numericCode());
    assertEquals(IPSServerErrors.RAW_DUMP, ServerErrorCodes.RAW_DUMP.numericCode());
    assertEquals(
        IPSCmsErrors.UNDEFINED_DEFAULT_TRANSITION,
        CmsErrorCodes.UNDEFINED_DEFAULT_TRANSITION.numericCode());
    assertEquals(IPSCmsErrors.UNEXPECTED_ERROR, CmsErrorCodes.UNEXPECTED_ERROR.numericCode());

    leftoverNonAuditable(ExtensionErrorCodes.ILLEGAL_EXECUTION_CONTEXT);
    leftoverNonAuditable(ExtensionErrorCodes.MISSING_INTERNAL_REQUEST_RESOURCE);
    leftoverNonAuditable(ExtensionErrorCodes.EFFECT_VALIDATE_MESSAGE);
    leftoverNonAuditable(ExtensionErrorCodes.INVALID_TRANSITION_FOR_EFFECT);
    leftoverNonAuditable(ServerErrorCodes.MISSING_INTERNAL_REQUEST_RESOURCE);
    leftoverNonAuditable(ServerErrorCodes.RAW_DUMP);
    leftoverNonAuditable(CmsErrorCodes.UNDEFINED_DEFAULT_TRANSITION);
    leftoverNonAuditable(CmsErrorCodes.UNEXPECTED_ERROR);
  }

  @Test
  void typedConstructorsRetainCodesAndSkipDualWrite() {
    leftoverNonAuditable(
        new PSNotFoundException(
            "en-us",
            ExtensionErrorCodes.MISSING_INTERNAL_REQUEST_RESOURCE,
            new Object[] {"sys_effect", "res"}),
        ExtensionErrorCodes.MISSING_INTERNAL_REQUEST_RESOURCE);
    leftoverNonAuditable(
        new PSNotFoundException(
            ServerErrorCodes.MISSING_INTERNAL_REQUEST_RESOURCE,
            new Object[] {"res", "No request handler found."}),
        ServerErrorCodes.MISSING_INTERNAL_REQUEST_RESOURCE);
    leftoverNonAuditable(
        new PSExtensionProcessingException(
            "en-us",
            ExtensionErrorCodes.INVALID_OPTION_FOR_FORCETRANSITION,
            new Object[] {"sys_pub", "maybe"}),
        ExtensionErrorCodes.INVALID_OPTION_FOR_FORCETRANSITION);
    leftoverNonAuditable(
        new PSExtensionProcessingException(
            ExtensionErrorCodes.EXT_MISSING_HTML_PARAMETER_ERROR,
            new Object[] {"", "null or empty"}),
        ExtensionErrorCodes.EXT_MISSING_HTML_PARAMETER_ERROR);
    leftoverNonAuditable(
        new PSExtensionProcessingException(
            CmsErrorCodes.UNDEFINED_DEFAULT_TRANSITION, new Object[] {"1", "2"}),
        CmsErrorCodes.UNDEFINED_DEFAULT_TRANSITION);
    leftoverNonAuditable(
        new PSExtensionProcessingException(CmsErrorCodes.UNEXPECTED_ERROR, "boom"),
        CmsErrorCodes.UNEXPECTED_ERROR);

    new PSLogServerWarning(
        ExtensionErrorCodes.PROMOTE_TRANSITION_FAILED,
        new Object[] {"1:1", "archive", "fail"},
        false,
        null);
    leftoverNonAuditable(ExtensionErrorCodes.PROMOTE_TRANSITION_FAILED);
  }

  @Test
  void typedResultSettersRetainCodesAndSkipDualWrite() {
    PSTestResult error = new PSTestResult();
    error.setError(
        "en-us",
        ExtensionErrorCodes.EFFECT_VALIDATE_MESSAGE,
        new Object[] {"sys_validate", "must be item"});
    leftoverNonAuditable(error.getException(), ExtensionErrorCodes.EFFECT_VALIDATE_MESSAGE);

    PSTestResult warning = new PSTestResult();
    warning.setWarning(
        "en-us",
        ExtensionErrorCodes.ILLEGAL_EXECUTION_CONTEXT,
        new Object[] {"sys_promote", "post workflow"});
    leftoverNonAuditable(warning.getException(), ExtensionErrorCodes.ILLEGAL_EXECUTION_CONTEXT);
    assertTrue(warning.hasWarning());

    warning.setWarning("en-us", ServerErrorCodes.RAW_DUMP, new String[] {"Skip: already processed"});
    leftoverNonAuditable(warning.getException(), ServerErrorCodes.RAW_DUMP);
  }

  @Test
  void typedResultSettersRejectNullCode() {
    PSTestResult result = new PSTestResult();
    assertThrows(
        IllegalArgumentException.class,
        () -> result.setError("en-us", (IPSErrorCode) null, new Object[] {"x"}));
    assertThrows(
        IllegalArgumentException.class,
        () -> result.setWarning("en-us", (IPSErrorCode) null, new Object[] {"x"}));
  }

  @Test
  void effectUtilsMissingInternalRequestThrowsTyped() {
    IPSRequestContext request = mock(IPSRequestContext.class);
    when(request.getInternalRequest(anyString(), anyMap(), anyBoolean())).thenReturn(null);
    when(request.getUserLocale()).thenReturn("en-us");

    leftoverNonAuditable(
        assertThrows(
            PSNotFoundException.class,
            () -> PSEffectUtils.getWorkflowState(request, 7, "sys_effect")),
        ExtensionErrorCodes.MISSING_INTERNAL_REQUEST_RESOURCE);
    leftoverNonAuditable(
        assertThrows(
            PSNotFoundException.class,
            () -> PSEffectUtils.getWorkflowStates(request, java.util.List.of(7), "sys_effect")),
        ExtensionErrorCodes.MISSING_INTERNAL_REQUEST_RESOURCE);
  }

  @Test
  void validateSetsTypedErrorOnPreConstruction() {
    IPSRequestContext request = mock(IPSRequestContext.class);
    when(request.getUserLocale()).thenReturn("en-us");
    IPSExecutionContext context = mock(IPSExecutionContext.class);
    when(context.isPreConstruction()).thenReturn(true);

    PSTestResult result = new PSTestResult();
    new PSValidate().test(new Object[] {"must be item"}, request, context, result);
    leftoverNonAuditable(result.getException(), ExtensionErrorCodes.EFFECT_VALIDATE_MESSAGE);
    assertFalse(result.isSuccess());
  }

  @Test
  void isCloneExistsWrongContextSetsTypedWarning() throws Exception {
    IPSRequestContext request = mock(IPSRequestContext.class);
    when(request.getUserLocale()).thenReturn("en-us");
    IPSExecutionContext context = mock(IPSExecutionContext.class);
    when(context.isPreClone()).thenReturn(false);

    PSTestResult result = new PSTestResult();
    new PSIsCloneExists().test(new Object[0], request, context, result);
    leftoverNonAuditable(result.getException(), ExtensionErrorCodes.ILLEGAL_EXECUTION_CONTEXT);
    assertTrue(result.hasWarning());
  }

  @Test
  void isCloneExistsMissingContentIdThrowsTyped() {
    IPSRequestContext request = mock(IPSRequestContext.class);
    when(request.getUserLocale()).thenReturn("en-us");
    when(request.getParameter(IPSHtmlParameters.SYS_CONTENTID, "")).thenReturn("");
    IPSExecutionContext context = mock(IPSExecutionContext.class);
    when(context.isPreClone()).thenReturn(true);

    leftoverNonAuditable(
        assertThrows(
            PSExtensionProcessingException.class,
            () -> new PSIsCloneExists().test(new Object[0], request, context, new PSTestResult())),
        ExtensionErrorCodes.EXT_MISSING_HTML_PARAMETER_ERROR);
  }

  @Test
  void promoteWrongContextSetsTypedWarning() {
    IPSRequestContext request = mock(IPSRequestContext.class);
    when(request.getUserLocale()).thenReturn("en-us");
    IPSExecutionContext context = mock(IPSExecutionContext.class);
    when(context.isPostWorkflow()).thenReturn(false);

    PSTestResult result = new PSTestResult();
    new PSPromote().test(new Object[0], request, context, result);
    leftoverNonAuditable(result.getException(), ExtensionErrorCodes.ILLEGAL_EXECUTION_CONTEXT);
    assertTrue(result.hasWarning());
  }

  @Test
  void publishUnpublishWrongContextAndTriggerStateSetTypedWarnings() throws Exception {
    IPSRequestContext request = mock(IPSRequestContext.class);
    when(request.getUserLocale()).thenReturn("en-us");
    IPSExecutionContext context = mock(IPSExecutionContext.class);
    when(context.isPreWorkflow()).thenReturn(false);

    PSTestResult wrongCtx = new PSTestResult();
    new PSPublishMandatory().test(new Object[0], request, context, wrongCtx);
    leftoverNonAuditable(wrongCtx.getException(), ExtensionErrorCodes.ILLEGAL_EXECUTION_CONTEXT);

    PSTestResult publishTrigger = new PSTestResult();
    assertFalse(
        new PSPublishMandatory()
            .isTransitioningIntoTriggerState(request, false, false, false, publishTrigger));
    leftoverNonAuditable(
        publishTrigger.getException(), ExtensionErrorCodes.INVALID_TRANSITION_FOR_EFFECT);

    PSTestResult unpublishTrigger = new PSTestResult();
    assertFalse(
        new PSUnpublishMandatory()
            .isTransitioningIntoTriggerState(request, false, false, false, unpublishTrigger));
    leftoverNonAuditable(
        unpublishTrigger.getException(), ExtensionErrorCodes.INVALID_TRANSITION_FOR_EFFECT);
  }

  @Test
  void publishMandatoryInvalidForceOptionThrowsTyped() {
    IPSRequestContext request = mock(IPSRequestContext.class);
    when(request.getUserLocale()).thenReturn("en-us");
    IPSExecutionContext context = mock(IPSExecutionContext.class);
    when(context.isPreWorkflow()).thenReturn(true);
    when(context.getProcessedRelationships()).thenReturn(null);
    when(context.getCurrentRelationship())
        .thenReturn(mock(com.percussion.design.objectstore.PSRelationship.class));

    leftoverNonAuditable(
        assertThrows(
            PSExtensionProcessingException.class,
            () ->
                new PSPublishMandatory()
                    .test(new Object[] {"maybe"}, request, context, new PSTestResult())),
        ExtensionErrorCodes.INVALID_OPTION_FOR_FORCETRANSITION);
  }

  private static void leftoverNonAuditable(IPSErrorCode expected) {
    assertFalse(expected.isAuditable(), expected.toString());
  }

  private static void leftoverNonAuditable(PSException ex, IPSErrorCode expected) {
    assertSame(expected, ex.getTypedErrorCode(), expected.toString());
    assertEquals(expected.numericCode(), ex.getErrorCode(), expected.toString());
    assertFalse(ex.isAuditable(), expected.toString());
    assertFalse(expected.isAuditable(), expected.toString());
  }
}
