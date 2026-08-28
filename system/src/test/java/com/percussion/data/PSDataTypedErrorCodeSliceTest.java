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
package com.percussion.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.intsof.percussioncms.auditlog.codes.BackEndErrorCodes;
import com.intsof.percussioncms.auditlog.codes.DataErrorCodes;
import com.intsof.percussioncms.auditlog.codes.ExtensionErrorCodes;
import com.intsof.percussioncms.auditlog.codes.HttpErrorCodes;
import com.intsof.percussioncms.auditlog.codes.SecurityErrorCodes;
import com.intsof.percussioncms.auditlog.codes.ServerErrorCodes;
import com.intsof.percussioncms.auditlog.codes.UtilErrorCodes;
import com.percussion.data.macro.PSSqlEscapedUserNameExtractor;
import com.percussion.design.objectstore.PSSystemValidationException;
import com.percussion.error.IPSErrorCode;
import com.percussion.error.PSEvaluationException;
import com.percussion.error.PSException;
import com.percussion.error.PSIllegalArgumentException;
import com.percussion.error.PSNotFoundException;
import com.percussion.error.PSSqlException;
import com.percussion.extension.IPSExtensionErrors;
import com.percussion.extension.PSExtensionException;
import com.percussion.security.IPSSecurityErrors;
import com.percussion.server.IPSHttpErrors;
import com.percussion.server.IPSServerErrors;
import com.percussion.server.PSInvalidRequestTypeException;
import com.percussion.util.IPSUtilErrors;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Issue #3939 (parent #2616 leftover): {@code com.percussion.data} (+ macro/vfs) production sites
 * throw typed {@code *ErrorCodes} — not bare {@code IPS*Errors} ints. Dual-write skip is {@code
 * isAuditable()==false} on leftover operational catalog codes.
 */
@Tag("UnitTest")
class PSDataTypedErrorCodeSliceTest {

  @Test
  void leftoverCatalogsMatchLegacyIntsAndSkipDualWriteWhereNonAuditable() {
    assertEquals(
        IPSDataErrors.BE_COL_EXTR_INVALID_COL, DataErrorCodes.BE_COL_EXTR_INVALID_COL.numericCode());
    assertEquals(
        IPSDataErrors.WRONG_OPERATOR_USAGE, DataErrorCodes.WRONG_OPERATOR_USAGE.numericCode());
    assertEquals(
        IPSDataErrors.INTERNAL_REQUEST_CALL_EXCEPTION,
        DataErrorCodes.INTERNAL_REQUEST_CALL_EXCEPTION.numericCode());
    assertEquals(
        IPSDataErrors.INDEX_JOINER_RESULT_SET_REQD,
        DataErrorCodes.INDEX_JOINER_RESULT_SET_REQD.numericCode());
    assertEquals(
        IPSDataErrors.MACRO_EXTRACTOR_INVALID_PARAMETER,
        DataErrorCodes.MACRO_EXTRACTOR_INVALID_PARAMETER.numericCode());
    assertEquals(
        IPSDataErrors.NO_DATA_FOR_CONVERSION, DataErrorCodes.NO_DATA_FOR_CONVERSION.numericCode());
    assertEquals(
        IPSDataErrors.CACHER_FULL, DataErrorCodes.CACHER_FULL.numericCode());
    assertEquals(
        IPSBackEndErrors.EXEC_DATA_NO_CONNECTIONS,
        BackEndErrorCodes.EXEC_DATA_NO_CONNECTIONS.numericCode());
    assertEquals(
        IPSBackEndErrors.SQL_BUILDER_UDF_NOT_SUPPORTED_IN_MOD,
        BackEndErrorCodes.SQL_BUILDER_UDF_NOT_SUPPORTED_IN_MOD.numericCode());
    assertEquals(
        IPSBackEndErrors.EXEC_PLAN_NO_UPDATE_PIPES,
        BackEndErrorCodes.EXEC_PLAN_NO_UPDATE_PIPES.numericCode());
    assertEquals(
        IPSBackEndErrors.LOAD_META_DATA_EXCEPTION,
        BackEndErrorCodes.LOAD_META_DATA_EXCEPTION.numericCode());
    assertEquals(IPSServerErrors.RAW_DUMP, ServerErrorCodes.RAW_DUMP.numericCode());
    assertEquals(
        IPSServerErrors.APP_NO_QUERY_PIPES_IN_DATASET,
        ServerErrorCodes.APP_NO_QUERY_PIPES_IN_DATASET.numericCode());
    assertEquals(
        IPSServerErrors.MISSING_INTERNAL_REQUEST_RESOURCE,
        ServerErrorCodes.MISSING_INTERNAL_REQUEST_RESOURCE.numericCode());
    assertEquals(
        IPSExtensionErrors.INVALID_EXT_TYPE_EXCEPTION,
        ExtensionErrorCodes.INVALID_EXT_TYPE_EXCEPTION.numericCode());
    assertEquals(IPSHttpErrors.HTTP_NOT_FOUND, HttpErrorCodes.HTTP_NOT_FOUND.numericCode());
    assertEquals(IPSHttpErrors.HTTP_NO_CONTENT, HttpErrorCodes.HTTP_NO_CONTENT.numericCode());
    assertEquals(
        IPSUtilErrors.BASE64_DECODING_EXCEPTION,
        UtilErrorCodes.BASE64_DECODING_EXCEPTION.numericCode());
    assertEquals(
        IPSSecurityErrors.SESS_NOT_AUTHORIZED, SecurityErrorCodes.SESS_NOT_AUTHORIZED.numericCode());

    assertFalse(DataErrorCodes.WRONG_OPERATOR_USAGE.isAuditable());
    assertFalse(DataErrorCodes.INTERNAL_REQUEST_CALL_EXCEPTION.isAuditable());
    assertFalse(BackEndErrorCodes.EXEC_DATA_NO_CONNECTIONS.isAuditable());
    assertFalse(ServerErrorCodes.RAW_DUMP.isAuditable());
    assertFalse(ExtensionErrorCodes.INVALID_EXT_TYPE_EXCEPTION.isAuditable());
    assertFalse(HttpErrorCodes.HTTP_NOT_FOUND.isAuditable());
    assertFalse(UtilErrorCodes.BASE64_DECODING_EXCEPTION.isAuditable());
    assertTrue(SecurityErrorCodes.SESS_NOT_AUTHORIZED.isAuditable());
  }

  @Test
  void leftoverProductionExceptionTypesRetainTypedCodes() {
    leftoverNonAuditable(
        new PSDataExtractionException(DataErrorCodes.BE_COL_EXTR_INVALID_COL, "col_a"),
        DataErrorCodes.BE_COL_EXTR_INVALID_COL);
    leftoverRuntimeNonAuditable(
        new PSEvaluationException(DataErrorCodes.WRONG_OPERATOR_USAGE, new Object[] {"1", "=", "x"}),
        DataErrorCodes.WRONG_OPERATOR_USAGE);
    leftoverNonAuditable(
        new PSIllegalArgumentException(BackEndErrorCodes.EXEC_DATA_NO_CONNECTIONS),
        BackEndErrorCodes.EXEC_DATA_NO_CONNECTIONS);
    leftoverNonAuditable(
        new PSConversionException(DataErrorCodes.NO_DATA_FOR_CONVERSION),
        DataErrorCodes.NO_DATA_FOR_CONVERSION);
    leftoverNonAuditable(
        new PSUnsupportedConversionException(DataErrorCodes.HTML_CONV_EXT_NOT_SUPPORTED, ".bin"),
        DataErrorCodes.HTML_CONV_EXT_NOT_SUPPORTED);
    leftoverNonAuditable(
        new PSInternalRequestCallException(
            DataErrorCodes.INTERNAL_REQUEST_CALL_EXCEPTION, "sys_query", new IllegalStateException("nested")),
        DataErrorCodes.INTERNAL_REQUEST_CALL_EXCEPTION);
    leftoverNonAuditable(
        new PSInvalidRequestTypeException(
            ServerErrorCodes.APP_NO_QUERY_PIPES_IN_DATASET, new Object[] {"ds", "app"}),
        ServerErrorCodes.APP_NO_QUERY_PIPES_IN_DATASET);
    leftoverNonAuditable(
        new PSSystemValidationException(
            ServerErrorCodes.APPLICATION_INIT_EXCEPTION, new Object[] {"app", "stack"}),
        ServerErrorCodes.APPLICATION_INIT_EXCEPTION);
    leftoverNonAuditable(
        new PSExtensionException(
            ExtensionErrorCodes.INVALID_EXT_TYPE_EXCEPTION, new Object[] {"preProc", "wrong"}),
        ExtensionErrorCodes.INVALID_EXT_TYPE_EXCEPTION);
    leftoverNonAuditable(
        new PSNotFoundException(
            ServerErrorCodes.MISSING_INTERNAL_REQUEST_RESOURCE, new Object[] {"res", "none"}),
        ServerErrorCodes.MISSING_INTERNAL_REQUEST_RESOURCE);
    leftoverNonAuditable(
        new PSDataExtractionException(UtilErrorCodes.BASE64_DECODING_EXCEPTION, "blob"),
        UtilErrorCodes.BASE64_DECODING_EXCEPTION);
    leftoverNonAuditable(
        new PSDataExtractionException(ServerErrorCodes.RAW_DUMP, "session"),
        ServerErrorCodes.RAW_DUMP);

    PSSqlException sql =
        new PSSqlException(DataErrorCodes.INDEX_JOINER_RESULT_SET_REQD, "25000");
    assertEquals(DataErrorCodes.INDEX_JOINER_RESULT_SET_REQD.numericCode(), sql.getErrorCode());
    assertSame(DataErrorCodes.INDEX_JOINER_RESULT_SET_REQD, sql.getTypedErrorCode());
    assertFalse(sql.isAuditable());
  }

  @Test
  void sqlEscapedUserNameExtractorRethrowsTypedRawDump() {
    PSExecutionData data = mock(PSExecutionData.class);
    when(data.getRequest()).thenThrow(new RuntimeException("no session"));

    PSDataExtractionException ex =
        assertThrows(
            PSDataExtractionException.class, () -> new PSSqlEscapedUserNameExtractor().extract(data));
    leftoverNonAuditable(ex, ServerErrorCodes.RAW_DUMP);
  }

  @Test
  void typedConstructorsRejectNullCode() {
    assertThrows(IllegalArgumentException.class, () -> new PSDataExtractionException((IPSErrorCode) null));
    assertThrows(IllegalArgumentException.class, () -> new PSEvaluationException((IPSErrorCode) null));
    assertThrows(
        IllegalArgumentException.class, () -> new PSIllegalArgumentException((IPSErrorCode) null));
    assertThrows(IllegalArgumentException.class, () -> new PSConversionException((IPSErrorCode) null));
    assertThrows(
        IllegalArgumentException.class,
        () -> new PSInvalidRequestTypeException((IPSErrorCode) null));
    assertThrows(
        IllegalArgumentException.class, () -> new PSSqlException((IPSErrorCode) null, "25000"));
  }

  private static void leftoverNonAuditable(PSException ex, IPSErrorCode expected) {
    leftoverNonAuditable(expected, ex.getErrorCode(), ex.getTypedErrorCode(), ex.isAuditable());
  }

  private static void leftoverRuntimeNonAuditable(
      PSEvaluationException ex, IPSErrorCode expected) {
    leftoverNonAuditable(expected, ex.getErrorCode(), ex.getTypedErrorCode(), ex.isAuditable());
  }

  private static void leftoverNonAuditable(
      IPSErrorCode expected, int actualCode, IPSErrorCode typed, boolean auditable) {
    assertEquals(expected.numericCode(), actualCode, expected.toString());
    assertSame(expected, typed, expected.toString());
    assertFalse(auditable, expected.toString());
    assertFalse(expected.isAuditable(), expected.toString());
  }
}
