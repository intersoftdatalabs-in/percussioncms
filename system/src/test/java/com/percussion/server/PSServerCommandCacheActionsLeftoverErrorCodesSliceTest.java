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
import com.intsof.percussioncms.auditlog.codes.CloneErrorCodes;
import com.intsof.percussioncms.auditlog.codes.CmsErrorCodes;
import com.intsof.percussioncms.auditlog.codes.DataErrorCodes;
import com.intsof.percussioncms.auditlog.codes.HttpErrorCodes;
import com.intsof.percussioncms.auditlog.codes.ServerErrorCodes;
import com.intsof.percussioncms.auditlog.spi.ConcurrentMemoryAuditLogRepository;
import com.percussion.cms.IPSCmsErrors;
import com.percussion.data.IPSDataErrors;
import com.percussion.design.objectstore.PSLockedException;
import com.percussion.design.objectstore.PSSystemValidationException;
import com.percussion.error.IPSErrorCode;
import com.percussion.error.PSException;
import com.percussion.error.PSIllegalArgumentException;
import com.percussion.extension.PSExtensionProcessingException;
import com.percussion.extension.PSParameterMismatchException;
import com.percussion.server.actions.PSActionSetException;
import com.percussion.server.clone.IPSCloneErrors;
import com.percussion.server.command.PSConsoleCommandException;
import com.percussion.server.command.PSConsoleCommandParser;
import com.percussion.server.compare.PSCompareException;
import com.percussion.server.config.PSServerConfigException;
import com.percussion.util.PSCacheException;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Issue #4153 (parent #2616): leftover {@code system/server} command/cache/actions/clone/compare
 * /config production sites throw typed {@code *ErrorCodes} (not bare {@code IPS*Errors} ints).
 * Dual-write is skipped where {@code isAuditable() == false}.
 */
@Tag("UnitTest")
class PSServerCommandCacheActionsLeftoverErrorCodesSliceTest {

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
  void leftoverCatalogsMatchLegacyInts() {
    assertEquals(
        IPSServerErrors.RCONSOLE_CMD_EMPTY, ServerErrorCodes.RCONSOLE_CMD_EMPTY.numericCode());
    assertEquals(
        IPSServerErrors.RCONSOLE_INVALID_CMD, ServerErrorCodes.RCONSOLE_INVALID_CMD.numericCode());
    assertEquals(
        IPSServerErrors.RCONSOLE_INVALID_SUBCMD,
        ServerErrorCodes.RCONSOLE_INVALID_SUBCMD.numericCode());
    assertEquals(
        IPSServerErrors.RCONSOLE_SUBCMD_REQD, ServerErrorCodes.RCONSOLE_SUBCMD_REQD.numericCode());
    assertEquals(
        IPSServerErrors.RCONSOLE_CACHE_FLUSHED,
        ServerErrorCodes.RCONSOLE_CACHE_FLUSHED.numericCode());
    assertEquals(
        IPSServerErrors.CACHE_START_FAILED, ServerErrorCodes.CACHE_START_FAILED.numericCode());
    assertEquals(
        IPSServerErrors.CACHE_UNEXPECTED_EXCEPTION,
        ServerErrorCodes.CACHE_UNEXPECTED_EXCEPTION.numericCode());
    assertEquals(
        IPSServerErrors.MISSING_CACHE_KEY, ServerErrorCodes.MISSING_CACHE_KEY.numericCode());
    assertEquals(
        IPSServerErrors.ACTION_SET_DUPLICATE_NAME,
        ServerErrorCodes.ACTION_SET_DUPLICATE_NAME.numericCode());
    assertEquals(
        IPSServerErrors.UNKNOWN_CONFIGURATION,
        ServerErrorCodes.UNKNOWN_CONFIGURATION.numericCode());
    assertEquals(
        IPSServerErrors.COMPARE_CONTENTID_REQUIRED,
        ServerErrorCodes.COMPARE_CONTENTID_REQUIRED.numericCode());
    assertEquals(
        IPSCloneErrors.INVALID_CLONESOURCEID, CloneErrorCodes.INVALID_CLONESOURCEID.numericCode());
    assertEquals(IPSCloneErrors.NOT_AUTHORIZED, CloneErrorCodes.NOT_AUTHORIZED.numericCode());
    assertEquals(
        IPSCmsErrors.REQUIRED_RESOURCE_MISSING,
        CmsErrorCodes.REQUIRED_RESOURCE_MISSING.numericCode());
    assertEquals(
        IPSDataErrors.INTERNAL_REQUEST_AUTHORIZATION_EXCEPTION,
        DataErrorCodes.INTERNAL_REQUEST_AUTHORIZATION_EXCEPTION.numericCode());
    assertEquals(
        IPSServerErrors.NO_AUTHORIZATION, ServerErrorCodes.NO_AUTHORIZATION.numericCode());
    assertEquals(
        IPSHttpErrors.HTTP_INTERNAL_SERVER_ERROR,
        HttpErrorCodes.HTTP_INTERNAL_SERVER_ERROR.numericCode());
  }

  @Test
  void leftoverNonAuditableCodesSkipDualWrite() {
    DefaultAuditLogService svc = DefaultAuditLogService.createDefault();
    List<SystemErrorCode> leftovers =
        List.of(
            ServerErrorCodes.RCONSOLE_CMD_EMPTY,
            ServerErrorCodes.RCONSOLE_INVALID_CMD,
            ServerErrorCodes.CACHE_START_FAILED,
            ServerErrorCodes.CACHE_UNEXPECTED_EXCEPTION,
            ServerErrorCodes.MISSING_CACHE_KEY,
            ServerErrorCodes.ACTION_SET_DUPLICATE_NAME,
            ServerErrorCodes.UNKNOWN_CONFIGURATION,
            ServerErrorCodes.COMPARE_CONTENTID_REQUIRED,
            CloneErrorCodes.INVALID_CLONESOURCEID,
            CmsErrorCodes.REQUIRED_RESOURCE_MISSING);

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
  void leftoverAuditableAuthzCodesStillDualWrite() {
    DefaultAuditLogService svc = DefaultAuditLogService.createDefault();
    for (SystemErrorCode code :
        List.of(ServerErrorCodes.NO_AUTHORIZATION, CloneErrorCodes.NOT_AUTHORIZED)) {
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
        new PSIllegalArgumentException(ServerErrorCodes.RCONSOLE_CMD_EMPTY),
        ServerErrorCodes.RCONSOLE_CMD_EMPTY);
    leftoverNonAuditable(
        new PSConsoleCommandException(ServerErrorCodes.RCONSOLE_CACHE_ALREADY_STOPPED),
        ServerErrorCodes.RCONSOLE_CACHE_ALREADY_STOPPED);
    leftoverNonAuditable(
        new PSCacheException(ServerErrorCodes.CACHE_START_FAILED),
        ServerErrorCodes.CACHE_START_FAILED);
    leftoverNonAuditable(
        new PSCacheException(
            ServerErrorCodes.CACHE_UNEXPECTED_EXCEPTION, new RuntimeException("boom"), "disk"),
        ServerErrorCodes.CACHE_UNEXPECTED_EXCEPTION);
    leftoverNonAuditable(
        new PSSystemValidationException(
            ServerErrorCodes.MISSING_CACHE_KEY, new Object[] {"contentid"}),
        ServerErrorCodes.MISSING_CACHE_KEY);
    leftoverNonAuditable(
        new PSActionSetException(ServerErrorCodes.ACTION_SET_DUPLICATE_NAME, "setA"),
        ServerErrorCodes.ACTION_SET_DUPLICATE_NAME);
    leftoverNonAuditable(
        new PSServerConfigException(ServerErrorCodes.UNKNOWN_CONFIGURATION, "cfg"),
        ServerErrorCodes.UNKNOWN_CONFIGURATION);
    leftoverNonAuditable(
        new PSLockedException(ServerErrorCodes.CONFIG_LOCKED_SAME, new String[] {"cfg"}),
        ServerErrorCodes.CONFIG_LOCKED_SAME);
    leftoverNonAuditable(
        new PSCompareException("en-us", ServerErrorCodes.COMPARE_CONTENTID_REQUIRED, "1"),
        ServerErrorCodes.COMPARE_CONTENTID_REQUIRED);
    leftoverNonAuditable(
        new PSParameterMismatchException(
            CloneErrorCodes.INVALID_CLONESOURCEID, new Object[] {"x", "nfe"}),
        CloneErrorCodes.INVALID_CLONESOURCEID);
    leftoverNonAuditable(
        new PSExtensionProcessingException(CloneErrorCodes.REQUIRED_RESOURCE_MISSING, "app/res"),
        CloneErrorCodes.REQUIRED_RESOURCE_MISSING);

    leftoverAuditable(
        new PSExtensionProcessingException(
            CloneErrorCodes.NOT_AUTHORIZED, new Object[] {"denied"}),
        CloneErrorCodes.NOT_AUTHORIZED);
  }

  @Test
  void consoleParserNullCommandThrowsTypedIllegalArgument() {
    PSIllegalArgumentException ex =
        assertThrows(PSIllegalArgumentException.class, () -> PSConsoleCommandParser.parse(null));
    leftoverNonAuditable(ex, ServerErrorCodes.RCONSOLE_CMD_EMPTY);
  }

  @Test
  void consoleParserUnknownBaseThrowsTypedInvalidCmd() {
    PSIllegalArgumentException ex =
        assertThrows(PSIllegalArgumentException.class, () -> PSConsoleCommandParser.parse("bogus"));
    leftoverNonAuditable(ex, ServerErrorCodes.RCONSOLE_INVALID_CMD);
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

  private static void leftoverAuditable(PSException ex, IPSErrorCode expected) {
    assertSame(expected, ex.getTypedErrorCode());
    assertEquals(expected.numericCode(), ex.getErrorCode());
    assertTrue(ex.isAuditable());
  }
}
