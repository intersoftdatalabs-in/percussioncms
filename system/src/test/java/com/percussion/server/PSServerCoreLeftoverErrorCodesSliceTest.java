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
package com.percussion.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.intsof.percussioncms.auditlog.AuditContext;
import com.intsof.percussioncms.auditlog.AuditLogId;
import com.intsof.percussioncms.auditlog.DefaultAuditLogService;
import com.intsof.percussioncms.auditlog.LegacyErrorCodeRegistry;
import com.intsof.percussioncms.auditlog.SystemErrorCode;
import com.intsof.percussioncms.auditlog.codes.DataErrorCodes;
import com.intsof.percussioncms.auditlog.codes.HttpErrorCodes;
import com.intsof.percussioncms.auditlog.codes.SearchErrorCodes;
import com.intsof.percussioncms.auditlog.codes.SecurityErrorCodes;
import com.intsof.percussioncms.auditlog.codes.ServerErrorCodes;
import com.intsof.percussioncms.auditlog.spi.ConcurrentMemoryAuditLogRepository;
import com.percussion.conn.PSServerException;
import com.percussion.data.IPSDataErrors;
import com.percussion.data.PSInternalRequestCallException;
import com.percussion.error.IPSErrorCode;
import com.percussion.error.PSException;
import com.percussion.error.PSRuntimeException;
import com.percussion.search.IPSSearchErrors;
import com.percussion.security.IPSSecurityErrors;
import com.percussion.security.PSAuthenticationFailedException;
import com.percussion.security.PSAuthorizationException;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Issue #4262 (parent #2616 leftover): system server-core production sites ({@code PSServer},
 * {@code PSRequest}/{@code PSResponse}, {@code PSApplicationHandler}, job handler, login servlet)
 * throw/log typed {@code *ErrorCodes} — not bare {@code IPS*Errors} ints. Dual-write is skipped
 * where leftover operational codes are non-auditable; leftover auth codes remain dual-write
 * eligible.
 */
@Tag("UnitTest")
class PSServerCoreLeftoverErrorCodesSliceTest {

  @BeforeEach
  void setUp() {
    ConcurrentMemoryAuditLogRepository.INSTANCE.clear();
    DefaultAuditLogService.Holder.resetToDefault();
    LegacyErrorCodeRegistry.bootstrap();
  }

  @AfterEach
  void tearDown() {
    ConcurrentMemoryAuditLogRepository.INSTANCE.clear();
    DefaultAuditLogService.Holder.resetToDefault();
  }

  @Test
  void leftoverCatalogsMatchLegacyInts() {
    assertEquals(
        IPSDataErrors.EXECDATA_PRIVATE_OBJ_KEY_NULL,
        DataErrorCodes.EXECDATA_PRIVATE_OBJ_KEY_NULL.numericCode());
    assertEquals(
        IPSDataErrors.INTERNAL_REQUEST_CALL_EXCEPTION,
        DataErrorCodes.INTERNAL_REQUEST_CALL_EXCEPTION.numericCode());
    assertEquals(
        IPSDataErrors.INTERNAL_REQUEST_AUTHORIZATION_EXCEPTION,
        DataErrorCodes.INTERNAL_REQUEST_AUTHORIZATION_EXCEPTION.numericCode());
    assertEquals(
        IPSDataErrors.INTERNAL_REQUEST_AUTHENTICATION_FAILED_EXCEPTION,
        DataErrorCodes.INTERNAL_REQUEST_AUTHENTICATION_FAILED_EXCEPTION.numericCode());
    assertEquals(IPSHttpErrors.HTTP_OK, HttpErrorCodes.HTTP_OK.numericCode());
    assertEquals(IPSHttpErrors.HTTP_NOT_FOUND, HttpErrorCodes.HTTP_NOT_FOUND.numericCode());
    assertEquals(
        IPSHttpErrors.HTTP_NOT_MODIFIED, HttpErrorCodes.HTTP_NOT_MODIFIED.numericCode());
    assertEquals(
        IPSHttpErrors.HTTP_MOVED_TEMPORARILY,
        HttpErrorCodes.HTTP_MOVED_TEMPORARILY.numericCode());
    assertEquals(
        IPSHttpErrors.HTTP_INTERNAL_SERVER_ERROR,
        HttpErrorCodes.HTTP_INTERNAL_SERVER_ERROR.numericCode());
    assertEquals(
        IPSHttpErrors.HTTP_SERVICE_UNAVAILABLE,
        HttpErrorCodes.HTTP_SERVICE_UNAVAILABLE.numericCode());
    assertEquals(IPSServerErrors.ARGUMENT_ERROR, ServerErrorCodes.ARGUMENT_ERROR.numericCode());
    assertEquals(
        IPSServerErrors.REDIRECT_URL_TOO_LONG,
        ServerErrorCodes.REDIRECT_URL_TOO_LONG.numericCode());
    assertEquals(
        IPSServerErrors.LOADABLE_HANDLER_CONFIGURATION_FILE_IS_NULL,
        ServerErrorCodes.LOADABLE_HANDLER_CONFIGURATION_FILE_IS_NULL.numericCode());
    assertEquals(
        IPSServerErrors.LOADABLE_HANDLER_CONFIGURATION_FILE_IS_INVALID,
        ServerErrorCodes.LOADABLE_HANDLER_CONFIGURATION_FILE_IS_INVALID.numericCode());
    assertEquals(
        IPSServerErrors.LOADABLE_HANDLER_UNEXPECTED_EXCEPTION,
        ServerErrorCodes.LOADABLE_HANDLER_UNEXPECTED_EXCEPTION.numericCode());
    assertEquals(
        IPSSecurityErrors.SECURITY_NOT_INITIALIZED,
        SecurityErrorCodes.SECURITY_NOT_INITIALIZED.numericCode());
    assertEquals(
        IPSSecurityErrors.USER_NOT_AUTHORIZED,
        SecurityErrorCodes.USER_NOT_AUTHORIZED.numericCode());
    assertEquals(
        IPSSecurityErrors.GENERIC_AUTHENTICATION_FAILED,
        SecurityErrorCodes.GENERIC_AUTHENTICATION_FAILED.numericCode());
    assertEquals(
        IPSSearchErrors.SEARCH_ENGINE_FAILED_INIT,
        SearchErrorCodes.SEARCH_ENGINE_FAILED_INIT.numericCode());
  }

  @Test
  void leftoverNonAuditableCodesSkipDualWrite() {
    DefaultAuditLogService svc = DefaultAuditLogService.createDefault();
    List<SystemErrorCode> leftovers =
        List.of(
            DataErrorCodes.EXECDATA_PRIVATE_OBJ_KEY_NULL,
            DataErrorCodes.INTERNAL_REQUEST_CALL_EXCEPTION,
            DataErrorCodes.INTERNAL_REQUEST_AUTHORIZATION_EXCEPTION,
            DataErrorCodes.INTERNAL_REQUEST_AUTHENTICATION_FAILED_EXCEPTION,
            HttpErrorCodes.HTTP_OK,
            HttpErrorCodes.HTTP_NOT_FOUND,
            HttpErrorCodes.HTTP_NOT_MODIFIED,
            HttpErrorCodes.HTTP_MOVED_TEMPORARILY,
            HttpErrorCodes.HTTP_INTERNAL_SERVER_ERROR,
            HttpErrorCodes.HTTP_SERVICE_UNAVAILABLE,
            ServerErrorCodes.ARGUMENT_ERROR,
            ServerErrorCodes.REDIRECT_URL_TOO_LONG,
            ServerErrorCodes.LOADABLE_HANDLER_CONFIGURATION_FILE_IS_NULL,
            ServerErrorCodes.LOADABLE_HANDLER_CONFIGURATION_FILE_IS_INVALID,
            ServerErrorCodes.LOADABLE_HANDLER_UNEXPECTED_EXCEPTION,
            SecurityErrorCodes.SECURITY_NOT_INITIALIZED,
            SearchErrorCodes.SEARCH_ENGINE_FAILED_INIT);

    for (SystemErrorCode code : leftovers) {
      leftoverNonAuditable(code);
      assertSame(code, LegacyErrorCodeRegistry.find(code.numericCode()).orElseThrow());
      AuditLogId id =
          LegacyErrorCodeRegistry.logIfAuditable(
              svc, code.numericCode(), AuditContext.builder().actor("jdoe").build());
      assertEquals(LegacyErrorCodeRegistry.SKIPPED, id, code.toString());
    }
    assertEquals(0, ConcurrentMemoryAuditLogRepository.INSTANCE.size());
  }

  @Test
  void leftoverAuditableAuthCodesStillDualWrite() {
    DefaultAuditLogService svc = DefaultAuditLogService.createDefault();
    for (SystemErrorCode code :
        List.of(
            SecurityErrorCodes.USER_NOT_AUTHORIZED,
            SecurityErrorCodes.GENERIC_AUTHENTICATION_FAILED)) {
      leftoverAuditable(code);
      ConcurrentMemoryAuditLogRepository.INSTANCE.clear();
      AuditLogId id =
          LegacyErrorCodeRegistry.logIfAuditable(
              svc, code.numericCode(), AuditContext.builder().actor("jdoe").build(), "sess");
      assertFalse(LegacyErrorCodeRegistry.SKIPPED.equals(id), code.toString());
      assertEquals(1, ConcurrentMemoryAuditLogRepository.INSTANCE.size(), code.toString());
      assertEquals(code, ConcurrentMemoryAuditLogRepository.INSTANCE.findAll().get(0).code());
    }
  }

  @Test
  void productionExceptionTypesRetainTypedCodes() {
    leftoverNonAuditable(
        new PSInternalRequestCallException(
            DataErrorCodes.INTERNAL_REQUEST_CALL_EXCEPTION, "boom"),
        DataErrorCodes.INTERNAL_REQUEST_CALL_EXCEPTION);
    leftoverNonAuditable(
        new PSInternalRequestCallException(
            DataErrorCodes.INTERNAL_REQUEST_AUTHORIZATION_EXCEPTION, "denied"),
        DataErrorCodes.INTERNAL_REQUEST_AUTHORIZATION_EXCEPTION);
    leftoverNonAuditable(
        new PSInternalRequestCallException(
            DataErrorCodes.INTERNAL_REQUEST_AUTHENTICATION_FAILED_EXCEPTION, "auth"),
        DataErrorCodes.INTERNAL_REQUEST_AUTHENTICATION_FAILED_EXCEPTION);
    leftoverNonAuditable(
        new PSServerException(
            ServerErrorCodes.LOADABLE_HANDLER_CONFIGURATION_FILE_IS_NULL, "jobs"),
        ServerErrorCodes.LOADABLE_HANDLER_CONFIGURATION_FILE_IS_NULL);
    leftoverNonAuditable(
        new PSServerException(
            ServerErrorCodes.LOADABLE_HANDLER_CONFIGURATION_FILE_IS_INVALID,
            new Object[] {"jobs", "bad xml"}),
        ServerErrorCodes.LOADABLE_HANDLER_CONFIGURATION_FILE_IS_INVALID);
    leftoverNonAuditable(
        new PSServerException(
            ServerErrorCodes.LOADABLE_HANDLER_UNEXPECTED_EXCEPTION,
            new Object[] {"jobs", "io"}),
        ServerErrorCodes.LOADABLE_HANDLER_UNEXPECTED_EXCEPTION);
    leftoverNonAuditable(
        new PSAuthorizationException(SecurityErrorCodes.SECURITY_NOT_INITIALIZED, null),
        SecurityErrorCodes.SECURITY_NOT_INITIALIZED);
    leftoverNonAuditable(
        new PSRuntimeException(DataErrorCodes.EXECDATA_PRIVATE_OBJ_KEY_NULL),
        DataErrorCodes.EXECDATA_PRIVATE_OBJ_KEY_NULL);
    leftoverNonAuditable(
        new PSRuntimeException(SearchErrorCodes.SEARCH_ENGINE_FAILED_INIT),
        SearchErrorCodes.SEARCH_ENGINE_FAILED_INIT);

    leftoverAuditable(
        new PSAuthenticationFailedException(
            SecurityErrorCodes.GENERIC_AUTHENTICATION_FAILED, null),
        SecurityErrorCodes.GENERIC_AUTHENTICATION_FAILED);
  }

  @Test
  void requestPrivateObjectNullKeyThrowsTypedRuntimeException() {
    PSRequest request = new PSRequest(null, null, null, null);
    PSRuntimeException ex =
        assertThrows(PSRuntimeException.class, () -> request.getPrivateObject(null));
    leftoverNonAuditable(ex, DataErrorCodes.EXECDATA_PRIVATE_OBJ_KEY_NULL);

    PSRuntimeException setEx =
        assertThrows(PSRuntimeException.class, () -> request.setPrivateObject(null, "x"));
    leftoverNonAuditable(setEx, DataErrorCodes.EXECDATA_PRIVATE_OBJ_KEY_NULL);
  }

  private static void leftoverNonAuditable(SystemErrorCode code) {
    assertFalse(code.isAuditable(), code.toString());
  }

  private static void leftoverAuditable(SystemErrorCode code) {
    assertTrue(code.isAuditable(), code.toString());
  }

  private static void leftoverNonAuditable(PSException ex, IPSErrorCode expected) {
    assertSame(expected, ex.getTypedErrorCode());
    assertEquals(expected.numericCode(), ex.getErrorCode());
    assertFalse(ex.isAuditable());
  }

  private static void leftoverNonAuditable(PSRuntimeException ex, IPSErrorCode expected) {
    assertSame(expected, ex.getTypedErrorCode());
    assertEquals(expected.numericCode(), ex.getErrorCode());
    assertFalse(ex.isAuditable());
  }

  private static void leftoverAuditable(PSException ex, IPSErrorCode expected) {
    assertSame(expected, ex.getTypedErrorCode());
    assertEquals(expected.numericCode(), ex.getErrorCode());
    assertTrue(ex.isAuditable());
  }
}
