/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * you may obtain a copy of the License at
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
package com.percussion.error;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.intsof.percussioncms.auditlog.DefaultAuditLogService;
import com.intsof.percussioncms.auditlog.SystemErrorCode;
import com.intsof.percussioncms.auditlog.codes.BackEndErrorCodes;
import com.intsof.percussioncms.auditlog.codes.CatalogErrorCodes;
import com.intsof.percussioncms.auditlog.codes.DataErrorCodes;
import com.intsof.percussioncms.auditlog.codes.HttpErrorCodes;
import com.intsof.percussioncms.auditlog.codes.ServerErrorCodes;
import com.intsof.percussioncms.auditlog.codes.XmlErrorCodes;
import com.intsof.percussioncms.auditlog.spi.ConcurrentMemoryAuditLogRepository;
import com.percussion.data.IPSBackEndErrors;
import com.percussion.data.IPSDataErrors;
import com.percussion.design.catalog.IPSCatalogErrors;
import com.percussion.server.IPSHttpErrors;
import com.percussion.server.IPSServerErrors;
import com.percussion.xml.IPSXmlErrors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Issue #3971 (parent #2616): leftover {@code com.percussion.error} production sites use typed
 * {@code *ErrorCodes} (not bare {@code IPS*Errors} ints). Dual-write is skipped where the catalog
 * is non-auditable; leftover auditable authorization codes remain dual-write eligible.
 */
@Tag("UnitTest")
class PSErrorLeftoverErrorCodesSliceTest {

  @BeforeEach
  void setUp() {
    ConcurrentMemoryAuditLogRepository.INSTANCE.clear();
    DefaultAuditLogService.Holder.resetToDefault();
  }

  @AfterEach
  void tearDown() {
    ConcurrentMemoryAuditLogRepository.INSTANCE.clear();
    DefaultAuditLogService.Holder.resetToDefault();
  }

  @Test
  void leftoverCatalogsMatchLegacyIntsAndSkipDualWriteWhereNonAuditable() {
    assertEquals(
        IPSServerErrors.NATIVE_ERROR, ServerErrorCodes.NATIVE_ERROR.numericCode());
    assertEquals(IPSServerErrors.RAW_DUMP, ServerErrorCodes.RAW_DUMP.numericCode());
    assertEquals(
        IPSServerErrors.DATA_CONV_ERROR, ServerErrorCodes.DATA_CONV_ERROR.numericCode());
    assertEquals(
        IPSServerErrors.UNKNOWN_PROCESSING_ERROR,
        ServerErrorCodes.UNKNOWN_PROCESSING_ERROR.numericCode());
    assertEquals(
        IPSServerErrors.RESPONSE_SEND_ERROR,
        ServerErrorCodes.RESPONSE_SEND_ERROR.numericCode());
    assertEquals(
        IPSServerErrors.WRAPPED_LOG_ERROR, ServerErrorCodes.WRAPPED_LOG_ERROR.numericCode());
    assertEquals(
        IPSServerErrors.AUTHORIZATION_ERROR,
        ServerErrorCodes.AUTHORIZATION_ERROR.numericCode());
    assertEquals(
        IPSServerErrors.POOR_RESPONSE_TIME, ServerErrorCodes.POOR_RESPONSE_TIME.numericCode());
    assertEquals(
        IPSServerErrors.REQUEST_QUEUE_FULL, ServerErrorCodes.REQUEST_QUEUE_FULL.numericCode());
    assertEquals(
        IPSServerErrors.VALIDATION_ERROR, ServerErrorCodes.VALIDATION_ERROR.numericCode());
    assertEquals(
        IPSServerErrors.REQUEST_PREPROC_ERROR,
        ServerErrorCodes.REQUEST_PREPROC_ERROR.numericCode());
    assertEquals(
        IPSServerErrors.FATAL_SERVER_ERROR_MSG,
        ServerErrorCodes.FATAL_SERVER_ERROR_MSG.numericCode());
    assertEquals(
        IPSServerErrors.INTERNAL_SERVER_ERROR_MSG,
        ServerErrorCodes.INTERNAL_SERVER_ERROR_MSG.numericCode());
    assertEquals(
        IPSServerErrors.SERVER_UNAVAILABLE_ERROR_MSG,
        ServerErrorCodes.SERVER_UNAVAILABLE_ERROR_MSG.numericCode());
    assertEquals(
        IPSServerErrors.REQUEST_WAIT_TOO_LONG,
        ServerErrorCodes.REQUEST_WAIT_TOO_LONG.numericCode());
    assertEquals(
        IPSServerErrors.RCONSOLE_EXEC_EXCEPTION,
        ServerErrorCodes.RCONSOLE_EXEC_EXCEPTION.numericCode());
    assertEquals(
        IPSServerErrors.RCONSOLE_COMMAND_EXCEPTION,
        ServerErrorCodes.RCONSOLE_COMMAND_EXCEPTION.numericCode());
    assertEquals(
        IPSServerErrors.RCONSOLE_COMMAND_ERROR_MSG,
        ServerErrorCodes.RCONSOLE_COMMAND_ERROR_MSG.numericCode());
    assertEquals(
        IPSServerErrors.HOOK_REQUEST_ERROR_MSG,
        ServerErrorCodes.HOOK_REQUEST_ERROR_MSG.numericCode());
    assertEquals(
        IPSBackEndErrors.AUTHORIZATION_ERROR,
        BackEndErrorCodes.AUTHORIZATION_ERROR.numericCode());
    assertEquals(
        IPSBackEndErrors.REQUEST_QUEUE_FULL,
        BackEndErrorCodes.REQUEST_QUEUE_FULL.numericCode());
    assertEquals(
        IPSBackEndErrors.SERVER_DOWN_ERROR, BackEndErrorCodes.SERVER_DOWN_ERROR.numericCode());
    assertEquals(
        IPSDataErrors.QUERY_PROCESSING_ERROR,
        DataErrorCodes.QUERY_PROCESSING_ERROR.numericCode());
    assertEquals(
        IPSDataErrors.UPDATE_PROCESSING_ERROR,
        DataErrorCodes.UPDATE_PROCESSING_ERROR.numericCode());
    assertEquals(
        IPSDataErrors.HTML_GENERATION_ERROR,
        DataErrorCodes.HTML_GENERATION_ERROR.numericCode());
    assertEquals(
        IPSCatalogErrors.CATALOG_ERROR, CatalogErrorCodes.CATALOG_ERROR.numericCode());
    assertEquals(
        IPSXmlErrors.XML_PROCESSING_ERROR, XmlErrorCodes.XML_PROCESSING_ERROR.numericCode());
    assertEquals(
        IPSHttpErrors.HTTP_UNAUTHORIZED, HttpErrorCodes.HTTP_UNAUTHORIZED.numericCode());
    assertEquals(IPSHttpErrors.HTTP_NOT_FOUND, HttpErrorCodes.HTTP_NOT_FOUND.numericCode());

    leftoverNonAuditable(ServerErrorCodes.NATIVE_ERROR);
    leftoverNonAuditable(ServerErrorCodes.RAW_DUMP);
    leftoverNonAuditable(ServerErrorCodes.DATA_CONV_ERROR);
    leftoverNonAuditable(ServerErrorCodes.UNKNOWN_PROCESSING_ERROR);
    leftoverNonAuditable(ServerErrorCodes.RESPONSE_SEND_ERROR);
    leftoverNonAuditable(ServerErrorCodes.WRAPPED_LOG_ERROR);
    leftoverNonAuditable(ServerErrorCodes.POOR_RESPONSE_TIME);
    leftoverNonAuditable(ServerErrorCodes.REQUEST_QUEUE_FULL);
    leftoverNonAuditable(ServerErrorCodes.VALIDATION_ERROR);
    leftoverNonAuditable(ServerErrorCodes.REQUEST_PREPROC_ERROR);
    leftoverNonAuditable(ServerErrorCodes.FATAL_SERVER_ERROR_MSG);
    leftoverNonAuditable(ServerErrorCodes.INTERNAL_SERVER_ERROR_MSG);
    leftoverNonAuditable(ServerErrorCodes.SERVER_UNAVAILABLE_ERROR_MSG);
    leftoverNonAuditable(ServerErrorCodes.REQUEST_WAIT_TOO_LONG);
    leftoverNonAuditable(ServerErrorCodes.RCONSOLE_EXEC_EXCEPTION);
    leftoverNonAuditable(ServerErrorCodes.RCONSOLE_COMMAND_EXCEPTION);
    leftoverNonAuditable(ServerErrorCodes.RCONSOLE_COMMAND_ERROR_MSG);
    leftoverNonAuditable(ServerErrorCodes.HOOK_REQUEST_ERROR_MSG);
    leftoverNonAuditable(BackEndErrorCodes.REQUEST_QUEUE_FULL);
    leftoverNonAuditable(BackEndErrorCodes.SERVER_DOWN_ERROR);
    leftoverNonAuditable(DataErrorCodes.QUERY_PROCESSING_ERROR);
    leftoverNonAuditable(DataErrorCodes.UPDATE_PROCESSING_ERROR);
    leftoverNonAuditable(DataErrorCodes.HTML_GENERATION_ERROR);
    leftoverNonAuditable(CatalogErrorCodes.CATALOG_ERROR);
    leftoverNonAuditable(XmlErrorCodes.XML_PROCESSING_ERROR);
    leftoverNonAuditable(HttpErrorCodes.HTTP_UNAUTHORIZED);
    leftoverNonAuditable(HttpErrorCodes.HTTP_NOT_FOUND);

    leftoverAuditable(ServerErrorCodes.AUTHORIZATION_ERROR);
    leftoverAuditable(BackEndErrorCodes.AUTHORIZATION_ERROR);
  }

  @Test
  void leftoverProductionExceptionTypeRetainsTypedCodeAndSkipsDualWrite() {
    PSErrorException wrapped = new PSErrorException(null);
    assertSame(ServerErrorCodes.WRAPPED_LOG_ERROR, wrapped.getTypedErrorCode());
    assertEquals(ServerErrorCodes.WRAPPED_LOG_ERROR.numericCode(), wrapped.getErrorCode());
    assertFalse(wrapped.isAuditable());

    RuntimeException cause = new RuntimeException("wrap");
    PSErrorException withCause = new PSErrorException(null, cause);
    assertSame(ServerErrorCodes.WRAPPED_LOG_ERROR, withCause.getTypedErrorCode());
    assertSame(cause, withCause.getCause());
    assertFalse(withCause.isAuditable());

    PSErrorHandler.logLegacyExceptionIfAuditable(wrapped);
    assertEquals(0, ConcurrentMemoryAuditLogRepository.INSTANCE.size());
  }

  @Test
  void leftoverAuditableAuthorizationIntsStillDualWrite() {
    PSException serverAuth =
        new PSException(ServerErrorCodes.AUTHORIZATION_ERROR, new Object[] {"sess-1", "rx"});
    assertSame(ServerErrorCodes.AUTHORIZATION_ERROR, serverAuth.getTypedErrorCode());
    assertTrue(serverAuth.isAuditable());

    PSErrorHandler.logLegacyExceptionIfAuditable(serverAuth);
    assertEquals(1, ConcurrentMemoryAuditLogRepository.INSTANCE.size());
    assertEquals(
        ServerErrorCodes.AUTHORIZATION_ERROR,
        ConcurrentMemoryAuditLogRepository.INSTANCE.findAll().get(0).code());

    ConcurrentMemoryAuditLogRepository.INSTANCE.clear();

    PSException backEndAuth =
        new PSException(
            BackEndErrorCodes.AUTHORIZATION_ERROR,
            new Object[] {"host", "uid", "jdbc", "server"});
    assertSame(BackEndErrorCodes.AUTHORIZATION_ERROR, backEndAuth.getTypedErrorCode());
    assertTrue(backEndAuth.isAuditable());

    PSErrorHandler.logLegacyExceptionIfAuditable(backEndAuth);
    assertEquals(1, ConcurrentMemoryAuditLogRepository.INSTANCE.size());
    assertEquals(
        BackEndErrorCodes.AUTHORIZATION_ERROR,
        ConcurrentMemoryAuditLogRepository.INSTANCE.findAll().get(0).code());
  }

  @Test
  void leftoverLogErrorTypesConstructWithTypedNumericCodes() {
    PSApplicationAuthorizationError appAuth =
        new PSApplicationAuthorizationError(
            11,
            "127.0.0.1",
            "zzz",
            ServerErrorCodes.AUTHORIZATION_ERROR.numericCode(),
            "denied");
    assertEquals("127.0.0.1", appAuth.getHost());
    assertEquals(11, appAuth.getApplicationId());

    PSResponseSendError send =
        new PSResponseSendError(
            7, "sess-1", ServerErrorCodes.RESPONSE_SEND_ERROR.numericCode(), "io");
    assertEquals(7, send.getApplicationId());

    PSRemoteConsoleError console = new PSRemoteConsoleError("trace", new RuntimeException("boom"));
    assertEquals(0, console.getApplicationId());
  }

  private static void leftoverNonAuditable(SystemErrorCode code) {
    assertFalse(code.isAuditable(), code.toString());
  }

  private static void leftoverAuditable(SystemErrorCode code) {
    assertTrue(code.isAuditable(), code.toString());
  }
}
