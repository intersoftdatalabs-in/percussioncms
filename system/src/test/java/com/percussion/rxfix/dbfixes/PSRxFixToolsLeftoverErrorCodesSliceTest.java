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
package com.percussion.rxfix.dbfixes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;

import com.intsof.percussioncms.auditlog.AuditContext;
import com.intsof.percussioncms.auditlog.AuditLogId;
import com.intsof.percussioncms.auditlog.DefaultAuditLogService;
import com.intsof.percussioncms.auditlog.LegacyErrorCodeRegistry;
import com.intsof.percussioncms.auditlog.codes.NavigationErrorCodes;
import com.intsof.percussioncms.auditlog.codes.TableFactoryErrorCodes;
import com.intsof.percussioncms.auditlog.spi.ConcurrentMemoryAuditLogRepository;
import com.percussion.fastforward.managednav.IPSNavigationErrors;
import com.percussion.fastforward.managednav.PSNavException;
import com.percussion.tablefactory.IPSTableFactoryErrors;
import com.percussion.tablefactory.PSJdbcTableFactoryException;
import java.sql.SQLException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Issue #4339 (parent #2616 leftover): RxFix Tools call-sites throw typed {@code *ErrorCodes}, not
 * bare {@code IPS*Errors} ints. Catalogs remain numeric bridges. Dual-write is skipped because both
 * leftover codes are non-auditable. Exact exception types: {@link PSNavException} and {@link
 * PSJdbcTableFactoryException}.
 */
@Tag("UnitTest")
class PSRxFixToolsLeftoverErrorCodesSliceTest {

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
        IPSNavigationErrors.NAVIGATION_SERVICE_CANNOT_FIND_ANY_NAVONS,
        NavigationErrorCodes.NAVIGATION_SERVICE_CANNOT_FIND_ANY_NAVONS.numericCode());
    assertEquals(
        IPSTableFactoryErrors.SQL_CONNECTION_FAILED,
        TableFactoryErrorCodes.SQL_CONNECTION_FAILED.numericCode());
  }

  @Test
  void leftoverNonAuditableCodesSkipDualWrite() {
    NavigationErrorCodes nav = NavigationErrorCodes.NAVIGATION_SERVICE_CANNOT_FIND_ANY_NAVONS;
    assertFalse(nav.isAuditable());
    assertSame(nav, LegacyErrorCodeRegistry.find(nav.numericCode()).orElseThrow());
    DefaultAuditLogService svc = DefaultAuditLogService.createDefault();
    AuditLogId navId =
        LegacyErrorCodeRegistry.logIfAuditable(
            svc, nav.numericCode(), AuditContext.builder().actor("jdoe").build());
    assertEquals(LegacyErrorCodeRegistry.SKIPPED, navId);

    // TableFactory ints collide with ServerErrorCodes and are not flat-registered.
    // Dual-write skip is on the typed catalog / exception, not the int registry.
    assertFalse(TableFactoryErrorCodes.SQL_CONNECTION_FAILED.isAuditable());
    assertEquals(0, ConcurrentMemoryAuditLogRepository.INSTANCE.size());
  }

  @Test
  void fixNavigationMissingNavonsThrowsTypedNavException() {
    PSNavException ex = PSFixNavigation.cannotFindAnyNavons();
    assertSame(
        NavigationErrorCodes.NAVIGATION_SERVICE_CANNOT_FIND_ANY_NAVONS, ex.getTypedErrorCode());
    assertEquals(
        NavigationErrorCodes.NAVIGATION_SERVICE_CANNOT_FIND_ANY_NAVONS.numericCode(),
        ex.getErrorCode());
    assertFalse(ex.isAuditable());
    assertEquals(PSNavException.class, ex.getClass());
  }

  @Test
  void jdbcTableCheckConnectionFailedRetainsTypedException() {
    SQLException cause = new SQLException("refused");
    Object[] args = {PSJdbcTableFactoryException.formatSqlException(cause)};
    PSJdbcTableFactoryException ex =
        new PSJdbcTableFactoryException(
            TableFactoryErrorCodes.SQL_CONNECTION_FAILED, args, cause);
    assertSame(TableFactoryErrorCodes.SQL_CONNECTION_FAILED, ex.getTypedErrorCode());
    assertEquals(TableFactoryErrorCodes.SQL_CONNECTION_FAILED.numericCode(), ex.getErrorCode());
    assertFalse(TableFactoryErrorCodes.SQL_CONNECTION_FAILED.isAuditable());
    assertFalse(ex.isAuditable());
    assertEquals(PSJdbcTableFactoryException.class, ex.getClass());
  }
}
