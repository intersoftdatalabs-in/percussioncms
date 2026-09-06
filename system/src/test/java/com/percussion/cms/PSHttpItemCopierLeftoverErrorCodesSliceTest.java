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
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.intsof.percussioncms.auditlog.AuditContext;
import com.intsof.percussioncms.auditlog.AuditLogId;
import com.intsof.percussioncms.auditlog.DefaultAuditLogService;
import com.intsof.percussioncms.auditlog.LegacyErrorCodeRegistry;
import com.intsof.percussioncms.auditlog.codes.HttpErrorCodes;
import com.intsof.percussioncms.auditlog.spi.ConcurrentMemoryAuditLogRepository;
import com.percussion.server.IPSHttpErrors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Issue #4338 (parent #2616 leftover): Testing/cms {@code HttpItemCopier} treats clone success as
 * HTTP 302 via typed {@link HttpErrorCodes#HTTP_MOVED_TEMPORARILY}, not a bare {@code
 * IPSHttpErrors} int. The tool lives under {@code system/Testing} (not compiled by perc-system);
 * this test locks numeric parity, dual-write skip, and the 302-only redirect compare.
 */
@Tag("UnitTest")
class PSHttpItemCopierLeftoverErrorCodesSliceTest {

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
  void leftoverCatalogMatchesLegacyInt() {
    assertEquals(
        IPSHttpErrors.HTTP_MOVED_TEMPORARILY,
        HttpErrorCodes.HTTP_MOVED_TEMPORARILY.numericCode());
    assertEquals(302, HttpErrorCodes.HTTP_MOVED_TEMPORARILY.numericCode());
  }

  @Test
  void leftoverNonAuditableCodeSkipsDualWrite() {
    HttpErrorCodes code = HttpErrorCodes.HTTP_MOVED_TEMPORARILY;
    assertFalse(code.isAuditable(), code.toString());
    assertSame(code, LegacyErrorCodeRegistry.find(code.numericCode()).orElseThrow());
    DefaultAuditLogService svc = DefaultAuditLogService.createDefault();
    AuditLogId id =
        LegacyErrorCodeRegistry.logIfAuditable(
            svc, code.numericCode(), AuditContext.builder().actor("jdoe").build());
    assertEquals(LegacyErrorCodeRegistry.SKIPPED, id, code.toString());
    assertEquals(0, ConcurrentMemoryAuditLogRepository.INSTANCE.size());
  }

  @Test
  void cloneSuccessIsHttp302MovedTemporarilyOnly() {
    assertTrue(isCloneRedirectSuccess(HttpErrorCodes.HTTP_MOVED_TEMPORARILY.numericCode()));
    assertTrue(isCloneRedirectSuccess(IPSHttpErrors.HTTP_MOVED_TEMPORARILY));
    assertTrue(isCloneRedirectSuccess(302));
    assertFalse(isCloneRedirectSuccess(HttpErrorCodes.HTTP_OK.numericCode()));
    assertFalse(isCloneRedirectSuccess(HttpErrorCodes.HTTP_MOVED_PERMANENTLY.numericCode()));
    assertFalse(isCloneRedirectSuccess(HttpErrorCodes.HTTP_SEE_OTHER.numericCode()));
    assertFalse(isCloneRedirectSuccess(307));
  }

  /**
   * Mirrors {@code HttpItemCopier.makeCopies} success check: only HTTP 302 counts as clone
   * redirect success. Do not treat other 3xx statuses as success.
   */
  static boolean isCloneRedirectSuccess(int resp) {
    return resp == HttpErrorCodes.HTTP_MOVED_TEMPORARILY.numericCode();
  }
}
