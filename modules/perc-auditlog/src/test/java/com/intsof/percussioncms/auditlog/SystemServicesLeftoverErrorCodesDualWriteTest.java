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
package com.intsof.percussioncms.auditlog;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.intsof.percussioncms.auditlog.codes.AssemblyErrorCodes;
import com.intsof.percussioncms.auditlog.codes.CatalogErrorCodes;
import com.intsof.percussioncms.auditlog.codes.CmsErrorCodes;
import com.intsof.percussioncms.auditlog.codes.ContentErrorCodes;
import com.intsof.percussioncms.auditlog.codes.ExtensionErrorCodes;
import com.intsof.percussioncms.auditlog.codes.FilterServiceErrorCodes;
import com.intsof.percussioncms.auditlog.codes.LockErrorCodes;
import com.intsof.percussioncms.auditlog.codes.PublisherErrorCodes;
import com.intsof.percussioncms.auditlog.codes.ServiceSecurityErrorCodes;
import com.intsof.percussioncms.auditlog.codes.SiteManagerErrorCodes;
import com.intsof.percussioncms.auditlog.codes.SystemServiceErrorCodes;
import com.intsof.percussioncms.auditlog.codes.UiErrorCodes;
import com.intsof.percussioncms.auditlog.codes.XmlErrorCodes;
import org.junit.jupiter.api.Test;

/**
 * Dual-write skip coverage for leftover {@code system/services} catalog codes (#3847). Package-local
 * ints collide with {@link com.intsof.percussioncms.auditlog.codes.WorkflowErrorCodes} in the flat
 * registry, so skip is asserted on the enum (not {@code find(int)}).
 */
class SystemServicesLeftoverErrorCodesDualWriteTest {

  @Test
  void leftoverOperationalCodesSkipDualWrite() {
    assertFalse(AssemblyErrorCodes.MISSING_FINDER.isAuditable());
    assertFalse(AssemblyErrorCodes.TEMPLATE_MISSING.isAuditable());
    assertFalse(AssemblyErrorCodes.HASHED_BINARY_NOT_FOUND.isAuditable());
    assertFalse(FilterServiceErrorCodes.FILTER_MISSING.isAuditable());
    assertFalse(FilterServiceErrorCodes.DATABASE.isAuditable());
    assertFalse(CatalogErrorCodes.UNKNOWN_TYPE.isAuditable());
    assertFalse(CatalogErrorCodes.TOXML.isAuditable());
    assertFalse(PublisherErrorCodes.ROW_RETRIEVAL.isAuditable());
    assertFalse(PublisherErrorCodes.FILTER_FAILED.isAuditable());
    assertFalse(SiteManagerErrorCodes.SITE_ID_NOT_EXIST.isAuditable());
    assertFalse(LockErrorCodes.LOCK_NOT_FOUND.isAuditable());
    assertFalse(LockErrorCodes.OBJECT_ALREADY_LOCKED.isAuditable());
    assertFalse(UiErrorCodes.MISSING_HIERARCHY_NODE.isAuditable());
    assertFalse(ContentErrorCodes.MISSING_KEYWORD.isAuditable());
    assertFalse(SystemServiceErrorCodes.MISSING_SHARED_PROPERTY.isAuditable());
    assertFalse(SystemServiceErrorCodes.ERROR_DETERMINING_FOLDER_READ.isAuditable());
    assertFalse(CmsErrorCodes.INVALID_REL_CONFIG_NAME.isAuditable());
    assertFalse(XmlErrorCodes.XML_ELEMENT_MISSING.isAuditable());
    assertFalse(ExtensionErrorCodes.EXT_PROCESSOR_EXCEPTION.isAuditable());
    assertFalse(ServiceSecurityErrorCodes.ACL_NOT_FOUND.isAuditable());
  }

  @Test
  void leftoverSecurityRelevantCodesRemainAuditable() {
    assertTrue(LockErrorCodes.PERMISSION_DENIED.isAuditable());
    assertTrue(LockErrorCodes.LOCK_EXTENSION_INVALID_SESSION.isAuditable());
    assertTrue(UiErrorCodes.ACCESS_DENIED.isAuditable());
    assertTrue(PublisherErrorCodes.JOB_FAILED.isAuditable());
    assertTrue(PublisherErrorCodes.ITEM_PUBLISH_FAILED.isAuditable());
    assertTrue(ServiceSecurityErrorCodes.ACCESS_DENIED.isAuditable());
    assertTrue(ServiceSecurityErrorCodes.AUTHENTICATION_FAILED.isAuditable());
  }
}
