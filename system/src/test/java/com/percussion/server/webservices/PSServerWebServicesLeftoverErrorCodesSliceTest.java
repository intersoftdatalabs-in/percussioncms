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
package com.percussion.server.webservices;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.intsof.percussioncms.auditlog.AuditContext;
import com.intsof.percussioncms.auditlog.AuditLogId;
import com.intsof.percussioncms.auditlog.DefaultAuditLogService;
import com.intsof.percussioncms.auditlog.LegacyErrorCodeRegistry;
import com.intsof.percussioncms.auditlog.SystemErrorCode;
import com.intsof.percussioncms.auditlog.codes.CmsErrorCodes;
import com.intsof.percussioncms.auditlog.codes.DataErrorCodes;
import com.intsof.percussioncms.auditlog.codes.HttpErrorCodes;
import com.intsof.percussioncms.auditlog.codes.PathItemErrorCodes;
import com.intsof.percussioncms.auditlog.codes.SecurityErrorCodes;
import com.intsof.percussioncms.auditlog.codes.ServerErrorCodes;
import com.intsof.percussioncms.auditlog.codes.ServerWebServicesErrorCodes;
import com.intsof.percussioncms.auditlog.spi.ConcurrentMemoryAuditLogRepository;
import com.percussion.cms.IPSCmsErrors;
import com.percussion.cms.PSCmsException;
import com.percussion.data.IPSDataErrors;
import com.percussion.data.PSConversionException;
import com.percussion.error.IPSErrorCode;
import com.percussion.error.PSException;
import com.percussion.security.IPSSecurityErrors;
import com.percussion.server.IPSHttpErrors;
import com.percussion.server.IPSServerErrors;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Issue #4263 (parent #2616 leftover): system {@code com.percussion.server.webservices} production
 * handlers throw typed {@code *ErrorCodes} — not bare {@code IPS*Errors} ints. Dual-write is
 * skipped where leftover operational codes are non-auditable; leftover auth / folder-permission
 * codes remain dual-write eligible.
 */
@Tag("UnitTest")
class PSServerWebServicesLeftoverErrorCodesSliceTest {

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
        IPSWebServicesErrors.WEB_SERVICE_MISSING_PARAMETER,
        ServerWebServicesErrorCodes.WEB_SERVICE_MISSING_PARAMETER.numericCode());
    assertEquals(
        IPSWebServicesErrors.WEB_SERVICE_MISSING_ELEMENT,
        ServerWebServicesErrorCodes.WEB_SERVICE_MISSING_ELEMENT.numericCode());
    assertEquals(
        IPSWebServicesErrors.WEB_SERVICE_DISPATCH_ERROR,
        ServerWebServicesErrorCodes.WEB_SERVICE_DISPATCH_ERROR.numericCode());
    assertEquals(
        IPSWebServicesErrors.WEB_SERVICE_ACTION_NOT_FOUND,
        ServerWebServicesErrorCodes.WEB_SERVICE_ACTION_NOT_FOUND.numericCode());
    assertEquals(
        IPSWebServicesErrors.WEB_SERVICE_INTERNAL_REQUEST_FAILED,
        ServerWebServicesErrorCodes.WEB_SERVICE_INTERNAL_REQUEST_FAILED.numericCode());
    assertEquals(
        IPSWebServicesErrors.WEB_SERVICE_INTERNAL_REQUEST_NOT_FOUND,
        ServerWebServicesErrorCodes.WEB_SERVICE_INTERNAL_REQUEST_NOT_FOUND.numericCode());
    assertEquals(
        IPSWebServicesErrors.WEB_SERVICE_CONTENT_ITEM_NOT_FOUND,
        ServerWebServicesErrorCodes.WEB_SERVICE_CONTENT_ITEM_NOT_FOUND.numericCode());
    assertEquals(
        IPSWebServicesErrors.WEB_SERVICE_CONTENT_TYPE_NOT_FOUND,
        ServerWebServicesErrorCodes.WEB_SERVICE_CONTENT_TYPE_NOT_FOUND.numericCode());
    assertEquals(
        IPSWebServicesErrors.WEB_SERVICE_ITEM_CHILD_NOT_FOUND,
        ServerWebServicesErrorCodes.WEB_SERVICE_ITEM_CHILD_NOT_FOUND.numericCode());
    assertEquals(
        IPSWebServicesErrors.WEB_SERVICE_MISSING_ID,
        ServerWebServicesErrorCodes.WEB_SERVICE_MISSING_ID.numericCode());
    assertEquals(
        IPSWebServicesErrors.INVALID_MIXED_CHILD_IDS,
        ServerWebServicesErrorCodes.INVALID_MIXED_CHILD_IDS.numericCode());
    assertEquals(
        IPSWebServicesErrors.WEB_SERVICE_INVALID_FOLDER,
        ServerWebServicesErrorCodes.WEB_SERVICE_INVALID_FOLDER.numericCode());
    assertEquals(
        IPSWebServicesErrors.WEB_SERVICE_INVALID_SEARCH_CONTENTTYPE,
        ServerWebServicesErrorCodes.WEB_SERVICE_INVALID_SEARCH_CONTENTTYPE.numericCode());
    assertEquals(
        IPSWebServicesErrors.WEB_SERVICE_PROMOTE_FAILED_CHECKOUT,
        ServerWebServicesErrorCodes.WEB_SERVICE_PROMOTE_FAILED_CHECKOUT.numericCode());
    assertEquals(
        IPSWebServicesErrors.WEB_SERVICE_PROMOTE_FAILED_CHECKIN,
        ServerWebServicesErrorCodes.WEB_SERVICE_PROMOTE_FAILED_CHECKIN.numericCode());
    assertEquals(
        IPSWebServicesErrors.WEB_SERVICE_SEARCH_RESOURCE_NOT_FOUND,
        ServerWebServicesErrorCodes.WEB_SERVICE_SEARCH_RESOURCE_NOT_FOUND.numericCode());
    assertEquals(
        IPSWebServicesErrors.WEB_SERVICE_LOGIN_FAILURE,
        ServerWebServicesErrorCodes.WEB_SERVICE_LOGIN_FAILURE.numericCode());

    assertEquals(IPSCmsErrors.FAIL_OPEN_FOLDER, PathItemErrorCodes.FAIL_OPEN_FOLDER.numericCode());
    assertEquals(IPSCmsErrors.INVALID_FOLDER_ID, PathItemErrorCodes.INVALID_FOLDER_ID.numericCode());
    assertEquals(
        IPSCmsErrors.FAIL_GET_PARENT_FOLDER, PathItemErrorCodes.FAIL_GET_PARENT_FOLDER.numericCode());
    assertEquals(
        IPSCmsErrors.FOLDER_PERMISSION_DENIED,
        PathItemErrorCodes.FOLDER_PERMISSION_DENIED.numericCode());
    assertEquals(
        IPSCmsErrors.FOLDER_CREATE_ERROR, PathItemErrorCodes.FOLDER_CREATE_ERROR.numericCode());
    assertEquals(
        IPSCmsErrors.DUPLICATE_ITEM_NAME, PathItemErrorCodes.DUPLICATE_ITEM_NAME.numericCode());
    assertEquals(IPSCmsErrors.VALIDATION_ERROR, CmsErrorCodes.VALIDATION_ERROR.numericCode());
    assertEquals(
        IPSCmsErrors.FAIL_GET_COMPONENT_SUMMARIES,
        CmsErrorCodes.FAIL_GET_COMPONENT_SUMMARIES.numericCode());
    assertEquals(
        IPSCmsErrors.CROSSSITE_LINK_PROCESS_MULTI_ERROR,
        CmsErrorCodes.CROSSSITE_LINK_PROCESS_MULTI_ERROR.numericCode());

    assertEquals(
        IPSDataErrors.INTERNAL_REQUEST_AUTHORIZATION_EXCEPTION,
        DataErrorCodes.INTERNAL_REQUEST_AUTHORIZATION_EXCEPTION.numericCode());
    assertEquals(
        IPSDataErrors.INTERNAL_REQUEST_AUTHENTICATION_FAILED_EXCEPTION,
        DataErrorCodes.INTERNAL_REQUEST_AUTHENTICATION_FAILED_EXCEPTION.numericCode());
    assertEquals(
        IPSDataErrors.XML_CONV_EXCEPTION, DataErrorCodes.XML_CONV_EXCEPTION.numericCode());
    assertEquals(
        IPSSecurityErrors.SESS_NOT_AUTHORIZED,
        SecurityErrorCodes.SESS_NOT_AUTHORIZED.numericCode());
    assertEquals(
        IPSHttpErrors.HTTP_UNAUTHORIZED, HttpErrorCodes.HTTP_UNAUTHORIZED.numericCode());
    assertEquals(
        IPSServerErrors.UNEXPECTED_EXCEPTION_CONSOLE,
        ServerErrorCodes.UNEXPECTED_EXCEPTION_CONSOLE.numericCode());
    assertEquals(
        IPSServerErrors.XML_PARSER_SAX_ERROR, ServerErrorCodes.XML_PARSER_SAX_ERROR.numericCode());
  }

  @Test
  void leftoverNonAuditableCodesSkipDualWrite() {
    DefaultAuditLogService svc = DefaultAuditLogService.createDefault();
    List<SystemErrorCode> leftovers =
        List.of(
            ServerWebServicesErrorCodes.WEB_SERVICE_MISSING_PARAMETER,
            ServerWebServicesErrorCodes.WEB_SERVICE_DISPATCH_ERROR,
            ServerWebServicesErrorCodes.WEB_SERVICE_INTERNAL_REQUEST_FAILED,
            ServerWebServicesErrorCodes.WEB_SERVICE_CONTENT_ITEM_NOT_FOUND,
            ServerWebServicesErrorCodes.WEB_SERVICE_CONTENT_TYPE_NOT_FOUND,
            ServerWebServicesErrorCodes.WEB_SERVICE_SEARCH_RESOURCE_NOT_FOUND,
            PathItemErrorCodes.INVALID_FOLDER_ID,
            PathItemErrorCodes.DUPLICATE_ITEM_NAME,
            CmsErrorCodes.VALIDATION_ERROR,
            CmsErrorCodes.FAIL_GET_COMPONENT_SUMMARIES,
            DataErrorCodes.INTERNAL_REQUEST_AUTHORIZATION_EXCEPTION,
            DataErrorCodes.XML_CONV_EXCEPTION,
            HttpErrorCodes.HTTP_UNAUTHORIZED,
            ServerErrorCodes.UNEXPECTED_EXCEPTION_CONSOLE,
            ServerErrorCodes.XML_PARSER_SAX_ERROR);

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
  void leftoverAuditableCodesStillDualWrite() {
    DefaultAuditLogService svc = DefaultAuditLogService.createDefault();
    for (SystemErrorCode code :
        List.of(
            ServerWebServicesErrorCodes.WEB_SERVICE_LOGIN_FAILURE,
            ServerWebServicesErrorCodes.WEB_SERVICE_CHECKOUT_USER_FAILURE,
            ServerWebServicesErrorCodes.WEB_SERVICE_INVALID_CLIENT_ACESS,
            PathItemErrorCodes.FOLDER_PERMISSION_DENIED,
            PathItemErrorCodes.FOLDER_CREATE_ERROR,
            PathItemErrorCodes.FAIL_OPEN_FOLDER,
            SecurityErrorCodes.SESS_NOT_AUTHORIZED)) {
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
        new PSException(
            ServerWebServicesErrorCodes.WEB_SERVICE_MISSING_PARAMETER, new Object[] {"sys_action"}),
        ServerWebServicesErrorCodes.WEB_SERVICE_MISSING_PARAMETER);
    leftoverNonAuditable(
        new PSException(
            ServerWebServicesErrorCodes.WEB_SERVICE_INTERNAL_REQUEST_FAILED, "no assembly found"),
        ServerWebServicesErrorCodes.WEB_SERVICE_INTERNAL_REQUEST_FAILED);
    leftoverNonAuditable(
        new PSException(
            ServerWebServicesErrorCodes.WEB_SERVICE_CONTENT_TYPE_NOT_FOUND, "301"),
        ServerWebServicesErrorCodes.WEB_SERVICE_CONTENT_TYPE_NOT_FOUND);
    leftoverNonAuditable(
        new PSCmsException(PathItemErrorCodes.INVALID_FOLDER_ID, "42"),
        PathItemErrorCodes.INVALID_FOLDER_ID);
    leftoverNonAuditable(
        new PSCmsException(
            CmsErrorCodes.VALIDATION_ERROR, new Object[] {"app/res", "bad field"}),
        CmsErrorCodes.VALIDATION_ERROR);
    leftoverNonAuditable(
        new PSConversionException(
            ServerErrorCodes.UNEXPECTED_EXCEPTION_CONSOLE, new Object[] {"io"}),
        ServerErrorCodes.UNEXPECTED_EXCEPTION_CONSOLE);
    leftoverNonAuditable(
        new PSConversionException(
            DataErrorCodes.XML_CONV_EXCEPTION, new Object[] {"sess", "xsl"}),
        DataErrorCodes.XML_CONV_EXCEPTION);

    leftoverAuditable(
        new PSCmsException(PathItemErrorCodes.FOLDER_PERMISSION_DENIED),
        PathItemErrorCodes.FOLDER_PERMISSION_DENIED);
    leftoverAuditable(
        new PSCmsException(PathItemErrorCodes.FAIL_OPEN_FOLDER, new Object[] {"/Sites"}),
        PathItemErrorCodes.FAIL_OPEN_FOLDER);
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
