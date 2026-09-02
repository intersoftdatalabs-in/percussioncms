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
package com.percussion.fastforward.managednav;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.intsof.percussioncms.auditlog.AuditContext;
import com.intsof.percussioncms.auditlog.DefaultAuditLogService;
import com.intsof.percussioncms.auditlog.LegacyErrorCodeRegistry;
import com.intsof.percussioncms.auditlog.codes.NavigationErrorCodes;
import com.intsof.percussioncms.auditlog.spi.ConcurrentMemoryAuditLogRepository;
import com.percussion.error.IPSErrorCode;
import com.percussion.services.audit.PSSystemAuditLogger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Issue #4144 (parent #2616): leftover managednav production sites throw typed {@code
 * NavigationErrorCodes} (not bare {@code IPSNavigationErrors} ints). Dual-write is skipped because
 * the catalog is non-auditable. {@link IPSNavigationErrors} remains the numeric bridge.
 */
@Tag("UnitTest")
class PSManagedNavLeftoverErrorCodesSliceTest {

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
  void leftoverCatalogsMatchLegacyIntsAndSkipDualWrite() {
    assertEquals(
        IPSNavigationErrors.NAVIGATION_SERVICE_FOLDER_ID_NOT_FOUND_FOR_PATH,
        NavigationErrorCodes.NAVIGATION_SERVICE_FOLDER_ID_NOT_FOUND_FOR_PATH.numericCode());
    assertEquals(
        IPSNavigationErrors.NAVIGATION_SERVICE_CANT_FIND_RELATED_FOLDER_FOR_NAVON,
        NavigationErrorCodes.NAVIGATION_SERVICE_CANT_FIND_RELATED_FOLDER_FOR_NAVON.numericCode());
    assertEquals(
        IPSNavigationErrors.NAVIGATION_SERVICE_FAILED_TO_MOVE_SOURCE_NAVON_TO_TARGET,
        NavigationErrorCodes.NAVIGATION_SERVICE_FAILED_TO_MOVE_SOURCE_NAVON_TO_TARGET
            .numericCode());
    assertEquals(
        IPSNavigationErrors.NAVIGATION_SERVICE_FAILED_TO_MOVE_SECTION_BECAUSE_TARGET_ALREADY_HAS_ITEM,
        NavigationErrorCodes.NAVIGATION_SERVICE_FAILED_TO_MOVE_SECTION_BECAUSE_TARGET_ALREADY_HAS_ITEM
            .numericCode());
    assertEquals(
        IPSNavigationErrors.NAVIGATION_SERVICE_NAVTREE_CANNOT_BE_ADDED_TO_FOLDER_WITH_NAVON,
        NavigationErrorCodes.NAVIGATION_SERVICE_NAVTREE_CANNOT_BE_ADDED_TO_FOLDER_WITH_NAVON
            .numericCode());
    assertEquals(
        IPSNavigationErrors.NAVIGATION_SERVICE_NAVTREE_CANNOT_BE_ADDED_TO_FOLDER_WITH_NAVTREE,
        NavigationErrorCodes.NAVIGATION_SERVICE_NAVTREE_CANNOT_BE_ADDED_TO_FOLDER_WITH_NAVTREE
            .numericCode());
    assertEquals(
        IPSNavigationErrors.NAVIGATION_SERVICE_ERROR_ADDING_NAVTREE_TO_FOLDER,
        NavigationErrorCodes.NAVIGATION_SERVICE_ERROR_ADDING_NAVTREE_TO_FOLDER.numericCode());
    assertEquals(
        IPSNavigationErrors.NAVIGATION_SERVICE_CANNOT_FIND_ANY_NAVONS,
        NavigationErrorCodes.NAVIGATION_SERVICE_CANNOT_FIND_ANY_NAVONS.numericCode());
    assertEquals(
        IPSNavigationErrors.NAVIGATION_SERVICE_CANNOT_FIND_NAVTREE_FOR_SITE,
        NavigationErrorCodes.NAVIGATION_SERVICE_CANNOT_FIND_NAVTREE_FOR_SITE.numericCode());

    for (NavigationErrorCodes code : NavigationErrorCodes.values()) {
      leftoverNonAuditable(code);
    }
  }

  @Test
  void leftoverProductionExceptionTypeRetainsTypedCodeAndSkipsDualWrite() {
    RuntimeException cause = new RuntimeException("wrap");
    PSNavException folderMissing =
        new PSNavException(
            NavigationErrorCodes.NAVIGATION_SERVICE_FOLDER_ID_NOT_FOUND_FOR_PATH, "//Sites/x");
    assertSame(
        NavigationErrorCodes.NAVIGATION_SERVICE_FOLDER_ID_NOT_FOUND_FOR_PATH,
        folderMissing.getTypedErrorCode());
    assertEquals(
        NavigationErrorCodes.NAVIGATION_SERVICE_FOLDER_ID_NOT_FOUND_FOR_PATH.numericCode(),
        folderMissing.getErrorCode());
    assertFalse(folderMissing.isAuditable());

    PSNavException moveFailed =
        new PSNavException(
            NavigationErrorCodes.NAVIGATION_SERVICE_FAILED_TO_MOVE_SOURCE_NAVON_TO_TARGET,
            new Object[] {"src", "tgt"},
            cause);
    assertSame(
        NavigationErrorCodes.NAVIGATION_SERVICE_FAILED_TO_MOVE_SOURCE_NAVON_TO_TARGET,
        moveFailed.getTypedErrorCode());
    assertSame(cause, moveFailed.getCause());
    assertFalse(moveFailed.isAuditable());

    PSNavException addFailed =
        new PSNavException(
            NavigationErrorCodes.NAVIGATION_SERVICE_ERROR_ADDING_NAVTREE_TO_FOLDER, cause);
    assertSame(
        NavigationErrorCodes.NAVIGATION_SERVICE_ERROR_ADDING_NAVTREE_TO_FOLDER,
        addFailed.getTypedErrorCode());
    assertFalse(addFailed.isAuditable());
    assertNull(addFailed.getCause());

    var skipped =
        PSSystemAuditLogger.logLegacyIfAuditable(
            NavigationErrorCodes.NAVIGATION_SERVICE_FOLDER_ID_NOT_FOUND_FOR_PATH.numericCode(),
            AuditContext.builder().actor("jdoe").build(),
            "//Sites/x");
    assertEquals(LegacyErrorCodeRegistry.SKIPPED, skipped);
    assertEquals(0, ConcurrentMemoryAuditLogRepository.INSTANCE.size());
  }

  @Test
  void typedNavExceptionCtorRejectsNullCode() {
    assertThrows(IllegalArgumentException.class, () -> new PSNavException((IPSErrorCode) null));
    assertThrows(
        IllegalArgumentException.class, () -> new PSNavException((IPSErrorCode) null, "arg"));
    assertThrows(
        IllegalArgumentException.class,
        () -> new PSNavException((IPSErrorCode) null, new Object[] {"a"}));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new PSNavException(
                (IPSErrorCode) null, new Object[] {"a"}, new RuntimeException("c")));
  }

  @Test
  void legacyIntCtorRemainsNumericBridgeWithoutTypedCode() {
    PSNavException legacy =
        new PSNavException(IPSNavigationErrors.NAVIGATION_SERVICE_CANNOT_FIND_ANY_NAVONS);
    assertNull(legacy.getTypedErrorCode());
    assertEquals(
        NavigationErrorCodes.NAVIGATION_SERVICE_CANNOT_FIND_ANY_NAVONS.numericCode(),
        legacy.getErrorCode());
    assertFalse(legacy.isAuditable());
  }

  private static void leftoverNonAuditable(NavigationErrorCodes code) {
    assertFalse(code.isAuditable(), code.toString());
    PSNavException ex = new PSNavException(code);
    assertSame(code, ex.getTypedErrorCode());
    assertEquals(code.numericCode(), ex.getErrorCode());
    assertFalse(ex.isAuditable(), code.toString());
  }
}
