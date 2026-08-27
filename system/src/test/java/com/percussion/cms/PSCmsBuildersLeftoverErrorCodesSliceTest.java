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
package com.percussion.cms;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.intsof.percussioncms.auditlog.codes.CmsErrorCodes;
import com.intsof.percussioncms.auditlog.codes.ServerErrorCodes;
import com.percussion.cms.handlers.PSContentEditorHandler;
import com.percussion.data.IPSInternalResultHandler;
import com.percussion.data.PSConversionException;
import com.percussion.data.PSDataExtractionException;
import com.percussion.data.PSExecutionData;
import com.percussion.design.objectstore.PSContentEditor;
import com.percussion.design.objectstore.PSSystemValidationException;
import com.percussion.design.objectstore.PSTextLiteral;
import com.percussion.error.PSNotFoundException;
import com.percussion.server.IPSServerErrors;
import com.percussion.server.PSRequest;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Issue #3882 (parent #2616): leftover {@code system/src/main} cms document builders throw typed
 * {@code *ErrorCodes} (not bare {@code IPS*Errors} ints). Dual-write is skipped where {@code
 * isAuditable() == false}.
 */
@Tag("UnitTest")
class PSCmsBuildersLeftoverErrorCodesSliceTest {

  @Test
  void leftoverServerAndCmsPeersMatchLegacyIntsAndSkipDualWrite() {
    assertEquals(
        IPSServerErrors.CE_MISSING_FIELDSET, ServerErrorCodes.CE_MISSING_FIELDSET.numericCode());
    assertEquals(
        IPSServerErrors.CE_MISSING_FIELD, ServerErrorCodes.CE_MISSING_FIELD.numericCode());
    assertEquals(
        IPSServerErrors.CE_MISSING_TABLE, ServerErrorCodes.CE_MISSING_TABLE.numericCode());
    assertEquals(
        IPSServerErrors.CE_MISSING_MAPPINGS, ServerErrorCodes.CE_MISSING_MAPPINGS.numericCode());
    assertEquals(
        IPSServerErrors.CE_NEEDED_APP_NOT_RUNNING,
        ServerErrorCodes.CE_NEEDED_APP_NOT_RUNNING.numericCode());
    assertEquals(
        IPSServerErrors.UNKNOWN_PROCESSING_ERROR,
        ServerErrorCodes.UNKNOWN_PROCESSING_ERROR.numericCode());
    assertEquals(
        IPSServerErrors.CE_INVALID_CHOICES_LOOKUP_EXTENSION,
        ServerErrorCodes.CE_INVALID_CHOICES_LOOKUP_EXTENSION.numericCode());
    assertEquals(
        IPSServerErrors.CE_CHOICES_LOOKUP_EXTENSION_NOT_FOUND,
        ServerErrorCodes.CE_CHOICES_LOOKUP_EXTENSION_NOT_FOUND.numericCode());
    assertEquals(
        IPSServerErrors.CE_INVALID_CHOICES_LOOKUP_URL,
        ServerErrorCodes.CE_INVALID_CHOICES_LOOKUP_URL.numericCode());
    assertEquals(
        IPSServerErrors.CE_MISSING_CHOICESET, ServerErrorCodes.CE_MISSING_CHOICESET.numericCode());
    assertEquals(
        IPSServerErrors.CE_CHOICESET_NOT_SUPPORTED,
        ServerErrorCodes.CE_CHOICESET_NOT_SUPPORTED.numericCode());
    assertEquals(
        IPSServerErrors.CE_MISSING_CONTROL_NAME,
        ServerErrorCodes.CE_MISSING_CONTROL_NAME.numericCode());
    assertEquals(
        IPSServerErrors.CE_MISSING_CONTROL, ServerErrorCodes.CE_MISSING_CONTROL.numericCode());
    assertEquals(
        IPSServerErrors.CE_MISSING_REQUESTOR, ServerErrorCodes.CE_MISSING_REQUESTOR.numericCode());
    assertEquals(
        IPSServerErrors.CE_MISSING_FORMACTION, ServerErrorCodes.CE_MISSING_FORMACTION.numericCode());
    assertEquals(
        IPSServerErrors.CE_VIEW_SET_MISSING, ServerErrorCodes.CE_VIEW_SET_MISSING.numericCode());
    assertEquals(
        IPSServerErrors.CE_BAD_NUMBER_FORMAT, ServerErrorCodes.CE_BAD_NUMBER_FORMAT.numericCode());
    assertEquals(IPSServerErrors.CE_SQL_ERRORS, ServerErrorCodes.CE_SQL_ERRORS.numericCode());
    assertEquals(
        IPSServerErrors.CE_AMBIGUOUS_PAGEID, ServerErrorCodes.CE_AMBIGUOUS_PAGEID.numericCode());
    assertEquals(IPSServerErrors.CE_NO_PARENT, ServerErrorCodes.CE_NO_PARENT.numericCode());
    assertEquals(
        IPSServerErrors.CE_MISSING_PAGEMAP_ENTRY,
        ServerErrorCodes.CE_MISSING_PAGEMAP_ENTRY.numericCode());
    assertEquals(
        IPSServerErrors.CE_MISSING_RESULTSET, ServerErrorCodes.CE_MISSING_RESULTSET.numericCode());
    assertEquals(
        IPSServerErrors.CE_NO_DATA_IN_RESULT_SET,
        ServerErrorCodes.CE_NO_DATA_IN_RESULT_SET.numericCode());
    assertEquals(
        IPSServerErrors.CE_MISSING_TABLE_NAME, ServerErrorCodes.CE_MISSING_TABLE_NAME.numericCode());
    assertEquals(
        IPSServerErrors.CE_INVALID_PARAM, ServerErrorCodes.CE_INVALID_PARAM.numericCode());
    assertEquals(
        IPSServerErrors.CE_UNSUPPORTED_MAPPING_TYPE,
        ServerErrorCodes.CE_UNSUPPORTED_MAPPING_TYPE.numericCode());
    assertEquals(
        IPSServerErrors.CE_BACKEND_COL_REQUIRED,
        ServerErrorCodes.CE_BACKEND_COL_REQUIRED.numericCode());
    assertEquals(
        IPSServerErrors.CE_MODIFY_VALIDATION_EXCEPTION,
        ServerErrorCodes.CE_MODIFY_VALIDATION_EXCEPTION.numericCode());
    assertEquals(
        IPSServerErrors.CE_MODIFY_VALIDATION_FAIL,
        ServerErrorCodes.CE_MODIFY_VALIDATION_FAIL.numericCode());
    assertEquals(
        IPSServerErrors.CE_MODIFY_VALIDATION_FAIL_NOT_CHECKOUT,
        ServerErrorCodes.CE_MODIFY_VALIDATION_FAIL_NOT_CHECKOUT.numericCode());
    assertEquals(
        IPSServerErrors.CE_MODIFY_VALIDATION_FAIL_OLD_EDITREVISION,
        ServerErrorCodes.CE_MODIFY_VALIDATION_FAIL_OLD_EDITREVISION.numericCode());
    assertEquals(
        IPSServerErrors.CE_MULTIPLE_TABLES_NOT_SUPPORTED,
        ServerErrorCodes.CE_MULTIPLE_TABLES_NOT_SUPPORTED.numericCode());
    assertEquals(IPSServerErrors.RAW_DUMP, ServerErrorCodes.RAW_DUMP.numericCode());
    assertEquals(
        IPSCmsErrors.LOAD_AA_RELATIONSHIP_FAILED,
        CmsErrorCodes.LOAD_AA_RELATIONSHIP_FAILED.numericCode());

    leftoverNonAuditable(ServerErrorCodes.CE_MISSING_FIELDSET);
    leftoverNonAuditable(ServerErrorCodes.CE_NEEDED_APP_NOT_RUNNING);
    leftoverNonAuditable(ServerErrorCodes.CE_SQL_ERRORS);
    leftoverNonAuditable(ServerErrorCodes.CE_MODIFY_VALIDATION_FAIL);
    leftoverNonAuditable(ServerErrorCodes.RAW_DUMP);
    leftoverNonAuditable(CmsErrorCodes.LOAD_AA_RELATIONSHIP_FAILED);
  }

  @Test
  void productionExceptionTypesRetainTypedCodesAndSkipDualWrite() {
    leftoverNonAuditable(
        new PSSystemValidationException(ServerErrorCodes.CE_MISSING_FIELDSET, "fs"),
        ServerErrorCodes.CE_MISSING_FIELDSET);
    leftoverNonAuditable(
        new PSSystemValidationException(
            ServerErrorCodes.CE_MISSING_FIELD, new String[] {"f", "label"}),
        ServerErrorCodes.CE_MISSING_FIELD);
    leftoverNonAuditable(
        new PSSystemValidationException(ServerErrorCodes.CE_VIEW_SET_MISSING),
        ServerErrorCodes.CE_VIEW_SET_MISSING);
    leftoverNonAuditable(
        new PSDataExtractionException(ServerErrorCodes.CE_NEEDED_APP_NOT_RUNNING, "sys_Lookup"),
        ServerErrorCodes.CE_NEEDED_APP_NOT_RUNNING);
    leftoverNonAuditable(
        new PSDataExtractionException(
            "en-us", ServerErrorCodes.CE_INVALID_CHOICES_LOOKUP_URL, new Object[] {"h", "q", "a"}),
        ServerErrorCodes.CE_INVALID_CHOICES_LOOKUP_URL);
    leftoverNonAuditable(
        new PSNotFoundException(ServerErrorCodes.CE_NO_PARENT, "7"),
        ServerErrorCodes.CE_NO_PARENT);
    leftoverNonAuditable(
        new PSConversionException(ServerErrorCodes.CE_MISSING_RESULTSET, "child"),
        ServerErrorCodes.CE_MISSING_RESULTSET);
    leftoverNonAuditable(
        new PSCmsException(
            CmsErrorCodes.LOAD_AA_RELATIONSHIP_FAILED, new Object[] {"42"}),
        CmsErrorCodes.LOAD_AA_RELATIONSHIP_FAILED);
  }

  @Test
  void editorDocumentContextMissingPageMapEntryThrowsTypedNotFound() {
    PSEditorDocumentContext ctx = newContext();
    Map<Integer, PSPageInfo> pages = new HashMap<>();
    pages.put(1, new PSPageInfo(PSPageInfo.TYPE_ROW_EDITOR, 10, null, null));
    ctx.setPageInfoMap(pages);

    PSNotFoundException child =
        assertThrows(PSNotFoundException.class, () -> ctx.getChildId(99));
    leftoverNonAuditable(child, ServerErrorCodes.CE_MISSING_PAGEMAP_ENTRY);

    PSNotFoundException page =
        assertThrows(PSNotFoundException.class, () -> ctx.getPageId(77));
    leftoverNonAuditable(page, ServerErrorCodes.CE_MISSING_PAGEMAP_ENTRY);
  }

  @Test
  void editorDocumentContextNoParentThrowsTypedNotFound() {
    PSEditorDocumentContext ctx = newContext();
    Map<Integer, PSPageInfo> pages = new HashMap<>();
    pages.put(1, new PSPageInfo(PSPageInfo.TYPE_ROW_EDITOR, 10, null, List.of(2)));
    ctx.setPageInfoMap(pages);

    PSNotFoundException ex = assertThrows(PSNotFoundException.class, () -> ctx.getParentPageId(9));
    leftoverNonAuditable(ex, ServerErrorCodes.CE_NO_PARENT);
  }

  @Test
  void editorDocumentContextAmbiguousParentThrowsTypedNotFound() {
    PSEditorDocumentContext ctx = newContext();
    Map<Integer, PSPageInfo> pages = new HashMap<>();
    pages.put(10, new PSPageInfo(PSPageInfo.TYPE_SUMMARY_EDITOR, 1, null, List.of(50)));
    pages.put(20, new PSPageInfo(PSPageInfo.TYPE_SUMMARY_EDITOR, 2, null, List.of(50)));
    ctx.setPageInfoMap(pages);

    PSNotFoundException ex =
        assertThrows(PSNotFoundException.class, () -> ctx.getParentPageId(50));
    leftoverNonAuditable(ex, ServerErrorCodes.CE_AMBIGUOUS_PAGEID);
  }

  @Test
  void validateModifyStepHandlerFailureThrowsTypedValidationException() throws Exception {
    Map<String, Object> validations = new HashMap<>();
    validations.put("EDITREVISION", new PSTextLiteral("1"));
    PSValidateModifyStep step =
        new PSValidateModifyStep("sys_validate", "DBActionType", "UPDATE", validations);
    IPSInternalResultHandler handler = mock(IPSInternalResultHandler.class);
    when(handler.makeInternalRequest(any())).thenThrow(new RuntimeException("handler down"));
    step.setHandler(handler);

    PSRequest request = new PSRequest(null, null, null, null);
    PSExecutionData data = new PSExecutionData(null, null, request);
    PSSystemValidationException ex =
        assertThrows(PSSystemValidationException.class, () -> step.execute(data));
    leftoverNonAuditable(ex, ServerErrorCodes.CE_MODIFY_VALIDATION_EXCEPTION);
  }

  @Test
  void validateModifyStepEmptyResultThrowsTypedValidationFail() throws Exception {
    Map<String, Object> validations = new HashMap<>();
    validations.put("EDITREVISION", new PSTextLiteral("1"));
    PSValidateModifyStep step =
        new PSValidateModifyStep("sys_validate", "DBActionType", "UPDATE", validations);
    IPSInternalResultHandler handler = mock(IPSInternalResultHandler.class);
    PSExecutionData intData = mock(PSExecutionData.class);
    ResultSet rs = mock(ResultSet.class);
    when(rs.next()).thenReturn(false);
    when(handler.makeInternalRequest(any())).thenReturn(intData);
    when(handler.getResultSet(any())).thenReturn(rs);
    step.setHandler(handler);

    PSRequest request = new PSRequest(null, null, null, null);
    PSExecutionData data = new PSExecutionData(null, null, request);
    PSSystemValidationException ex =
        assertThrows(PSSystemValidationException.class, () -> step.execute(data));
    leftoverNonAuditable(ex, ServerErrorCodes.CE_MODIFY_VALIDATION_FAIL);
  }

  private static PSEditorDocumentContext newContext() {
    return new PSEditorDocumentContext(
        mock(PSContentEditorHandler.class), null, mock(PSContentEditor.class));
  }

  private static void leftoverNonAuditable(
      com.intsof.percussioncms.auditlog.SystemErrorCode code) {
    assertFalse(code.isAuditable(), code.toString());
  }

  private static void leftoverNonAuditable(
      com.percussion.error.PSException ex,
      com.intsof.percussioncms.auditlog.SystemErrorCode expected) {
    assertSame(expected, ex.getTypedErrorCode());
    assertEquals(expected.numericCode(), ex.getErrorCode());
    assertFalse(ex.isAuditable());
  }
}
