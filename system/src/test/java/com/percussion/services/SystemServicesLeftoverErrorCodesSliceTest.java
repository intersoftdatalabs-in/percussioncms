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
package com.percussion.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

import com.intsof.percussioncms.auditlog.codes.AssemblyErrorCodes;
import com.intsof.percussioncms.auditlog.codes.CmsErrorCodes;
import com.intsof.percussioncms.auditlog.codes.ContentErrorCodes;
import com.intsof.percussioncms.auditlog.codes.FilterServiceErrorCodes;
import com.intsof.percussioncms.auditlog.codes.LockErrorCodes;
import com.intsof.percussioncms.auditlog.codes.PublisherErrorCodes;
import com.intsof.percussioncms.auditlog.codes.ServiceSecurityErrorCodes;
import com.intsof.percussioncms.auditlog.codes.UiErrorCodes;
import com.intsof.percussioncms.auditlog.codes.XmlErrorCodes;
import com.percussion.cms.PSCmsException;
import com.percussion.error.IPSErrorCode;
import com.percussion.services.assembly.IPSAssemblyService;
import com.percussion.services.assembly.PSAssemblyException;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.content.PSContentException;
import com.percussion.services.filter.IPSFilterService;
import com.percussion.services.filter.PSFilterException;
import com.percussion.services.guidmgr.data.PSGuid;
import com.percussion.services.locking.PSLockException;
import com.percussion.services.publisher.PSPublisherException;
import com.percussion.services.security.PSServiceSecurityException;
import com.percussion.services.ui.PSUiException;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.utils.xml.PSInvalidXmlException;
import java.util.Optional;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Issue #3847 (parent #2616): leftover {@code system/services} production sites throw typed {@code
 * *ErrorCodes}. Representative exact exception types and interface defaults.
 */
@Tag("UnitTest")
class SystemServicesLeftoverErrorCodesSliceTest {

  @Test
  void assemblyInterfaceDefaultsThrowTypedNonAuditableCodes() throws Exception {
    IPSAssemblyService svc = mock(IPSAssemblyService.class, CALLS_REAL_METHODS);

    PSAssemblyException missing =
        assertThrows(PSAssemblyException.class, () -> svc.loadFinder("sys_finder"));
    assertEquals(AssemblyErrorCodes.MISSING_FINDER.numericCode(), missing.getErrorCode());
    assertSame(AssemblyErrorCodes.MISSING_FINDER, missing.getTypedErrorCode());
    assertFalse(missing.isAuditable());

    PSAssemblyException landing =
        assertThrows(PSAssemblyException.class, () -> svc.getLandingPageLink(null, null, null));
    assertSame(AssemblyErrorCodes.LANDING_PAGE_URL_1, landing.getTypedErrorCode());
    assertFalse(landing.isAuditable());
  }

  @Test
  void assemblyTypedConstructorRejectsNullCode() {
    assertThrows(IllegalArgumentException.class, () -> new PSAssemblyException((IPSErrorCode) null));
  }

  @Test
  void filterSafeLookupUsesTypedMissingVsOtherCodes() throws Exception {
    IPSFilterService svc = mock(IPSFilterService.class, CALLS_REAL_METHODS);
    doThrow(new PSFilterException(FilterServiceErrorCodes.FILTER_MISSING, "gone"))
        .when(svc)
        .findFilterByName("gone");
    doThrow(new PSFilterException(FilterServiceErrorCodes.DATABASE, new RuntimeException("db")))
        .when(svc)
        .findFilterByName("db");

    Optional<?> missing = svc.findFilterByNameSafe("gone");
    assertTrue(missing.isEmpty());

    IllegalStateException other =
        assertThrows(IllegalStateException.class, () -> svc.findFilterByNameSafe("db"));
    assertTrue(other.getCause() instanceof PSFilterException);
    PSFilterException cause = (PSFilterException) other.getCause();
    assertSame(FilterServiceErrorCodes.DATABASE, cause.getTypedErrorCode());
    assertFalse(cause.isAuditable());
  }

  @Test
  void lockFactoryAndContentThrowsRetainTypedCodes() {
    PSLockException notFound = PSLockException.lockNotFound(42L);
    assertEquals(LockErrorCodes.LOCK_NOT_FOUND.numericCode(), notFound.getErrorCode());
    assertSame(LockErrorCodes.LOCK_NOT_FOUND, notFound.getTypedErrorCode());
    assertFalse(notFound.isAuditable());

    PSLockException expired = PSLockException.lockExpired(7L);
    assertSame(LockErrorCodes.LOCK_EXPIRED, expired.getTypedErrorCode());

    PSContentException missingKw =
        new PSContentException(ContentErrorCodes.MISSING_KEYWORD, 99L);
    assertSame(ContentErrorCodes.MISSING_KEYWORD, missingKw.getTypedErrorCode());
    assertFalse(missingKw.isAuditable());
  }

  @Test
  void uiAndSecurityFactoriesRetainTypedAuditability() {
    IPSGuid nodeId = new PSGuid(PSTypeEnum.NODEDEF, 11L);
    PSUiException missingNode = PSUiException.nodeNotFound(nodeId);
    assertEquals(UiErrorCodes.MISSING_HIERARCHY_NODE.numericCode(), missingNode.getErrorCode());
    assertSame(UiErrorCodes.MISSING_HIERARCHY_NODE, missingNode.getTypedErrorCode());
    assertFalse(missingNode.isAuditable());

    PSServiceSecurityException denied =
        PSServiceSecurityException.accessDenied(nodeId, "bob");
    assertSame(ServiceSecurityErrorCodes.ACCESS_DENIED, denied.getTypedErrorCode());
    assertTrue(denied.isAuditable());

    PSServiceSecurityException acl =
        new PSServiceSecurityException(ServiceSecurityErrorCodes.ACL_NOT_FOUND, "acl-1");
    assertSame(ServiceSecurityErrorCodes.ACL_NOT_FOUND, acl.getTypedErrorCode());
    assertFalse(acl.isAuditable());
  }

  @Test
  void publisherCmsAndXmlTypedConstruction() {
    RuntimeException cause = new RuntimeException("row");
    PSPublisherException row =
        new PSPublisherException(PublisherErrorCodes.ROW_RETRIEVAL, cause);
    assertSame(PublisherErrorCodes.ROW_RETRIEVAL, row.getTypedErrorCode());
    assertFalse(row.isAuditable());
    assertSame(cause, row.getCause());

    PSPublisherException job = new PSPublisherException(PublisherErrorCodes.JOB_FAILED, "job-9");
    assertSame(PublisherErrorCodes.JOB_FAILED, job.getTypedErrorCode());
    assertTrue(job.isAuditable());

    PSCmsException cms =
        new PSCmsException(CmsErrorCodes.INVALID_REL_CONFIG_NAME, "relName");
    assertSame(CmsErrorCodes.INVALID_REL_CONFIG_NAME, cms.getTypedErrorCode());
    assertFalse(cms.isAuditable());

    PSInvalidXmlException xml =
        new PSInvalidXmlException(XmlErrorCodes.XML_ELEMENT_MISSING, "PSXEditionContentList");
    assertSame(XmlErrorCodes.XML_ELEMENT_MISSING, xml.getTypedErrorCode());
    assertFalse(xml.isAuditable());
  }
}
