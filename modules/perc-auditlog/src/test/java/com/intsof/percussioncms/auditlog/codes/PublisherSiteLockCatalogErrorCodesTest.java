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
package com.intsof.percussioncms.auditlog.codes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.intsof.percussioncms.auditlog.AuditEventType;
import com.intsof.percussioncms.auditlog.AuditModule;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Unit coverage for #2882 residual catalogs: publisher, site manager, filter, lock, catalog,
 * deployment, navigation, UI.
 */
class PublisherSiteLockCatalogErrorCodesTest {

  @Test
  void publisherJobAndItemFailuresAreAuditable() {
    assertTrue(PublisherErrorCodes.JOB_FAILED.isAuditable());
    assertEquals(AuditEventType.CONTENT_PUBLISH, PublisherErrorCodes.JOB_FAILED.eventType());
    assertEquals(19, PublisherErrorCodes.JOB_FAILED.numericCode());
    assertTrue(PublisherErrorCodes.ITEM_PUBLISH_FAILED.isAuditable());
    assertEquals(20, PublisherErrorCodes.ITEM_PUBLISH_FAILED.numericCode());
    assertFalse(PublisherErrorCodes.LIST_MISSING.isAuditable());
    assertNull(PublisherErrorCodes.LIST_MISSING.eventType());
    assertEquals(AuditModule.PUB, PublisherErrorCodes.JOB_FAILED.module());
  }

  @Test
  void publisherConstantsUniqueAndPreserveLegacyInts() {
    assertUniqueNumeric(PublisherErrorCodes.values(), c -> c.numericCode());
    assertEquals(10, PublisherErrorCodes.LIST_MISSING.numericCode());
    assertEquals(24, PublisherErrorCodes.UNEXPECTED.numericCode());
    assertEquals(15, PublisherErrorCodes.values().length);
  }

  @Test
  void siteManagerAndFilterAreNonAuditablePubModule() {
    for (SiteManagerErrorCodes code : SiteManagerErrorCodes.values()) {
      assertEquals(AuditModule.PUB, code.module());
      assertFalse(code.isAuditable(), code.name());
      assertNull(code.eventType(), code.name());
    }
    assertEquals(1, SiteManagerErrorCodes.SITE_ID_NOT_EXIST.numericCode());
    assertEquals(9, SiteManagerErrorCodes.NO_SUCH_CONTEXT.numericCode());

    for (FilterServiceErrorCodes code : FilterServiceErrorCodes.values()) {
      assertEquals(AuditModule.PUB, code.module());
      assertFalse(code.isAuditable(), code.name());
    }
    assertEquals(1, FilterServiceErrorCodes.FILTER_MISSING.numericCode());
    assertEquals(8, FilterServiceErrorCodes.PROBABLE_CYCLE.numericCode());
  }

  @Test
  void lockPermissionAndInvalidSessionAreAuditable() {
    assertTrue(LockErrorCodes.PERMISSION_DENIED.isAuditable());
    assertEquals(AuditEventType.ACCESS_DENIED, LockErrorCodes.PERMISSION_DENIED.eventType());
    assertEquals(9, LockErrorCodes.PERMISSION_DENIED.numericCode());
    assertTrue(LockErrorCodes.LOCK_EXTENSION_INVALID_SESSION.isAuditable());
    assertEquals(
        AuditEventType.AUTH_FAILURE, LockErrorCodes.LOCK_EXTENSION_INVALID_SESSION.eventType());
    assertFalse(LockErrorCodes.OBJECT_ALREADY_LOCKED.isAuditable());
    assertEquals(AuditModule.SYS, LockErrorCodes.PERMISSION_DENIED.module());
  }

  @Test
  void uiAccessDeniedIsAuditableOthersNot() {
    assertTrue(UiErrorCodes.ACCESS_DENIED.isAuditable());
    assertEquals(AuditEventType.ACCESS_DENIED, UiErrorCodes.ACCESS_DENIED.eventType());
    assertEquals(8, UiErrorCodes.ACCESS_DENIED.numericCode());
    assertFalse(UiErrorCodes.MISSING_HIERARCHY_NODE.isAuditable());
    assertEquals(AuditModule.SYS, UiErrorCodes.ACCESS_DENIED.module());
  }

  @Test
  void catalogDesignRangePreservedServiceRangeNonAuditable() {
    assertEquals(1, CatalogErrorCodes.SUMMARY_ERROR.numericCode());
    assertEquals(4101, CatalogErrorCodes.REQD_PROP_NOT_SPECIFIED.numericCode());
    assertEquals(4311, CatalogErrorCodes.CATALOG_EXCEPTION.numericCode());
    for (CatalogErrorCodes code : CatalogErrorCodes.values()) {
      assertEquals(AuditModule.SYS, code.module());
      assertFalse(code.isAuditable(), code.name());
    }
    assertUniqueNumeric(CatalogErrorCodes.values(), c -> c.numericCode());
  }

  @Test
  void deploymentLockCodesAuditableVersionFlatRegisteredRangePreserved() {
    assertTrue(DeploymentErrorCodes.LOCK_ALREADY_HELD.isAuditable());
    assertEquals(46, DeploymentErrorCodes.LOCK_ALREADY_HELD.numericCode());
    assertTrue(DeploymentErrorCodes.LOCK_NOT_EXTENSIBLE_TAKEN.isAuditable());
    assertTrue(DeploymentErrorCodes.LOCK_NOT_EXTENSIBLE_TAKEN_RELEASED.isAuditable());
    assertFalse(DeploymentErrorCodes.NULL_INPUT_DOC.isAuditable());
    assertEquals(8, DeploymentErrorCodes.NULL_INPUT_DOC.numericCode());
    assertEquals(74, DeploymentErrorCodes.VERSION_LOWER_THAN_INSTALLED.numericCode());
    assertEquals(85, DeploymentErrorCodes.WRONG_FORMAT_FOR_PAIRID_DEP_ID.numericCode());
    assertEquals(AuditModule.SYS, DeploymentErrorCodes.LOCK_ALREADY_HELD.module());
    assertUniqueNumeric(DeploymentErrorCodes.values(), c -> c.numericCode());
  }

  @Test
  void navigationCodesUniqueContModuleNonAuditable() {
    for (NavigationErrorCodes code : NavigationErrorCodes.values()) {
      assertEquals(AuditModule.CONT, code.module());
      assertFalse(code.isAuditable(), code.name());
      assertTrue(code.numericCode() >= 18001 && code.numericCode() <= 18009, code.name());
    }
    assertEquals(
        18001, NavigationErrorCodes.NAVIGATION_SERVICE_FOLDER_ID_NOT_FOUND_FOR_PATH.numericCode());
    assertEquals(
        18009, NavigationErrorCodes.NAVIGATION_SERVICE_CANNOT_FIND_NAVTREE_FOR_SITE.numericCode());
    assertEquals(9, NavigationErrorCodes.values().length);
  }

  private static <T> void assertUniqueNumeric(T[] values, java.util.function.ToIntFunction<T> fn) {
    Set<Integer> seen = new HashSet<>();
    for (T v : values) {
      int n = fn.applyAsInt(v);
      assertTrue(n > 0, v.toString());
      assertTrue(seen.add(n), "duplicate numeric: " + n);
      assertNotNull(v);
    }
  }
}
