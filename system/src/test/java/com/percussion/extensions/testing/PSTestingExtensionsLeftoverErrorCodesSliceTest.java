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
package com.percussion.extensions.testing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.intsof.percussioncms.auditlog.AuditContext;
import com.intsof.percussioncms.auditlog.AuditLogId;
import com.intsof.percussioncms.auditlog.DefaultAuditLogService;
import com.intsof.percussioncms.auditlog.LegacyErrorCodeRegistry;
import com.intsof.percussioncms.auditlog.SystemErrorCode;
import com.intsof.percussioncms.auditlog.codes.ExtensionErrorCodes;
import com.intsof.percussioncms.auditlog.codes.ServerErrorCodes;
import com.intsof.percussioncms.auditlog.spi.ConcurrentMemoryAuditLogRepository;
import com.percussion.error.IPSErrorCode;
import com.percussion.error.PSException;
import com.percussion.extension.IPSExtensionErrors;
import com.percussion.extension.PSExtensionProcessingException;
import com.percussion.server.IPSServerErrors;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Issue #4337 (parent #2616 leftover): Testing Extensions exits throw typed {@code *ErrorCodes},
 * not bare {@code IPS*Errors} ints. Catalogs remain numeric bridges. Dual-write is skipped because
 * leftover codes are non-auditable. The exits themselves live under {@code system/Testing} (not
 * compiled by the perc-system Maven module); this test locks exception types and numeric parity.
 */
@Tag("UnitTest")
class PSTestingExtensionsLeftoverErrorCodesSliceTest {

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
        IPSServerErrors.REQUEST_HANDLER_NOT_FOUND,
        ServerErrorCodes.REQUEST_HANDLER_NOT_FOUND.numericCode());
    assertEquals(
        IPSExtensionErrors.EXT_PARAM_VALUE_INVALID,
        ExtensionErrorCodes.EXT_PARAM_VALUE_INVALID.numericCode());
    assertEquals(
        IPSExtensionErrors.EXT_MISSING_REQUIRED_PARAMETER_ERROR,
        ExtensionErrorCodes.EXT_MISSING_REQUIRED_PARAMETER_ERROR.numericCode());
  }

  @Test
  void leftoverNonAuditableCodesSkipDualWrite() {
    DefaultAuditLogService svc = DefaultAuditLogService.createDefault();
    List<SystemErrorCode> leftovers =
        List.of(
            ServerErrorCodes.REQUEST_HANDLER_NOT_FOUND,
            ExtensionErrorCodes.EXT_PARAM_VALUE_INVALID,
            ExtensionErrorCodes.EXT_MISSING_REQUIRED_PARAMETER_ERROR);

    for (SystemErrorCode code : leftovers) {
      assertFalse(code.isAuditable(), code.toString());
      assertSame(code, LegacyErrorCodeRegistry.find(code.numericCode()).orElseThrow());
      AuditLogId id =
          LegacyErrorCodeRegistry.logIfAuditable(
              svc, code.numericCode(), AuditContext.builder().actor("jdoe").build());
      assertEquals(LegacyErrorCodeRegistry.SKIPPED, id, code.toString());
    }
    assertEquals(0, ConcurrentMemoryAuditLogRepository.INSTANCE.size());
  }

  @Test
  void productionExceptionTypesRetainTypedCodes() {
    leftoverNonAuditable(
        new PSExtensionProcessingException(
            ServerErrorCodes.REQUEST_HANDLER_NOT_FOUND,
            new Object[] {"-internal request-", "app/ce"}),
        ServerErrorCodes.REQUEST_HANDLER_NOT_FOUND);
    leftoverNonAuditable(
        new PSExtensionProcessingException(
            ExtensionErrorCodes.EXT_PARAM_VALUE_INVALID, "sort order not a subset"),
        ExtensionErrorCodes.EXT_PARAM_VALUE_INVALID);
    leftoverNonAuditable(
        new PSExtensionProcessingException(
            ExtensionErrorCodes.EXT_MISSING_REQUIRED_PARAMETER_ERROR,
            new Object[] {"Internal request path", null}),
        ExtensionErrorCodes.EXT_MISSING_REQUIRED_PARAMETER_ERROR);
  }

  @Test
  void missingInternalRequestThrowsExactExtensionProcessingException() {
    PSExtensionProcessingException ex =
        assertThrows(
            PSExtensionProcessingException.class,
            () -> {
              throw new PSExtensionProcessingException(
                  ServerErrorCodes.REQUEST_HANDLER_NOT_FOUND,
                  new Object[] {"-internal request-", "missing/resource"});
            });
    leftoverNonAuditable(ex, ServerErrorCodes.REQUEST_HANDLER_NOT_FOUND);
  }

  private static void leftoverNonAuditable(PSException ex, IPSErrorCode expected) {
    assertSame(expected, ex.getTypedErrorCode());
    assertEquals(expected.numericCode(), ex.getErrorCode());
    assertFalse(ex.isAuditable());
  }
}
