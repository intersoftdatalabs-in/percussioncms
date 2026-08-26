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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.intsof.percussioncms.auditlog.codes.CmsErrorCodes;
import com.intsof.percussioncms.auditlog.codes.ExtensionErrorCodes;
import com.intsof.percussioncms.auditlog.codes.ServerErrorCodes;
import com.intsof.percussioncms.auditlog.sink.CapturingAuditLogSink;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Dual-write skip coverage for leftover nav/sfp/workflow catalog codes (#3770). Leftovers listed
 * here are non-auditable operational / validation noise. Auditable authz leftovers (e.g. {@link
 * ExtensionErrorCodes#AUTHENTICATION_FAILED2}) keep dual-write and are not skipped.
 */
class NavSfpWorkflowResidualErrorCodesDualWriteTest {

  @BeforeEach
  void rebootstrap() {
    LegacyErrorCodeRegistry.clearForTests();
    LegacyErrorCodeRegistry.bootstrap();
  }

  @Test
  void leftoverExtensionCodesSkipDualWrite() {
    List<ExtensionErrorCodes> leftovers =
        List.of(
            ExtensionErrorCodes.EXT_MISSING_REQUIRED_PARAMETER_ERROR,
            ExtensionErrorCodes.EXT_MISSING_HTML_PARAMETER_ERROR,
            ExtensionErrorCodes.EXT_PARAM_VALUE_INVALID,
            ExtensionErrorCodes.EXT_PROCESSOR_EXCEPTION,
            ExtensionErrorCodes.EMPTY_USRNAME1,
            ExtensionErrorCodes.EMPTY_USRNAME2,
            ExtensionErrorCodes.INVALID_WORKFLOWID,
            ExtensionErrorCodes.ROLE_ERROR_STATEID_WORKFLOWID,
            ExtensionErrorCodes.WKFLOW_ACTIONLIST_EMPTY,
            ExtensionErrorCodes.WKFLOW_CONTEXT_NULL,
            ExtensionErrorCodes.INVALID_WKFLOW_EXT,
            ExtensionErrorCodes.EXEC_EXT_NOTFOUND,
            ExtensionErrorCodes.STATUS_DOC_EMPTY,
            ExtensionErrorCodes.CONTENTID_NODENAME_EMPTY,
            ExtensionErrorCodes.CONTENTID_NODE_MISSING_EMPTY,
            ExtensionErrorCodes.CONTENTID_NODE_MISSING,
            ExtensionErrorCodes.EXIT_PARAM_NULL,
            ExtensionErrorCodes.CONTENTID_NULL,
            ExtensionErrorCodes.PUBDOC_UPDATE_ERROR,
            ExtensionErrorCodes.HTML_PARAM_NULL1,
            ExtensionErrorCodes.ROLEINFO_OBJ_NULL,
            ExtensionErrorCodes.ROLELIST_EMPTY,
            ExtensionErrorCodes.INVALID_PARAM_NUM,
            ExtensionErrorCodes.TRANSITION_COMMENT_NOT_SPECIFIED,
            ExtensionErrorCodes.DOC_NOT_CHECKEDOUT,
            ExtensionErrorCodes.EDIT_REVISION_MISSING,
            ExtensionErrorCodes.CHECKOUT_REVISION_MISMATCH,
            ExtensionErrorCodes.CHECKOUT_REVISION_LIMIT,
            ExtensionErrorCodes.TRANSITION_ATTEMPT,
            ExtensionErrorCodes.MAIL_DOMAIN_NULL,
            ExtensionErrorCodes.MAIL_DOMAIN_EMPTY,
            ExtensionErrorCodes.SMTP_HOST_NULL,
            ExtensionErrorCodes.SMTP_HOST_EMPTY,
            ExtensionErrorCodes.USERNAME_NULL_EMPTY_TRIM,
            ExtensionErrorCodes.INVALID_ADHOC,
            ExtensionErrorCodes.STATEROLE_NULL_EMPTY_TRIM,
            ExtensionErrorCodes.NO_RECORDS,
            ExtensionErrorCodes.ADHOC_ASSIGNMENT_NOT_FOUND,
            ExtensionErrorCodes.INVALID_TRANSITION,
            ExtensionErrorCodes.MISSING_TRANSITION,
            ExtensionErrorCodes.WF_COMMENT_CANNOT_EXCEED_255);

    CapturingAuditLogSink sink = new CapturingAuditLogSink("cap");
    DefaultAuditLogService svc = DefaultAuditLogService.builder().addSink(sink).build();

    for (ExtensionErrorCodes code : leftovers) {
      assertFalse(code.isAuditable(), code.name());
      assertSame(code, LegacyErrorCodeRegistry.find(code.numericCode()).orElseThrow());
      AuditLogId id =
          LegacyErrorCodeRegistry.logIfAuditable(
              svc, code.numericCode(), AuditContext.builder().actor("jdoe").build());
      assertEquals(LegacyErrorCodeRegistry.SKIPPED, id, code.name());
    }
    assertTrue(sink.records().isEmpty());
  }

  @Test
  void leftoverCmsAndServerCodesSkipDualWrite() {
    CapturingAuditLogSink sink = new CapturingAuditLogSink("cap");
    DefaultAuditLogService svc = DefaultAuditLogService.builder().addSink(sink).build();

    assertFalse(CmsErrorCodes.INVALID_AUTHTYPE.isAuditable());
    assertFalse(CmsErrorCodes.CMS_INTERNAL_REQUEST_ERROR.isAuditable());
    assertFalse(CmsErrorCodes.REQUIRED_RESOURCE_MISSING.isAuditable());
    assertFalse(ServerErrorCodes.MISSING_INTERNAL_REQUEST_RESOURCE.isAuditable());
    assertFalse(ServerErrorCodes.EXCEPTION_NOT_CAUGHT.isAuditable());
    assertFalse(ServerErrorCodes.SQL_PROBLEM.isAuditable());

    for (int numeric :
        new int[] {
          CmsErrorCodes.INVALID_AUTHTYPE.numericCode(),
          CmsErrorCodes.CMS_INTERNAL_REQUEST_ERROR.numericCode(),
          CmsErrorCodes.REQUIRED_RESOURCE_MISSING.numericCode(),
          ServerErrorCodes.MISSING_INTERNAL_REQUEST_RESOURCE.numericCode(),
          ServerErrorCodes.EXCEPTION_NOT_CAUGHT.numericCode(),
          ServerErrorCodes.SQL_PROBLEM.numericCode()
        }) {
      AuditLogId id =
          LegacyErrorCodeRegistry.logIfAuditable(
              svc, numeric, AuditContext.builder().actor("jdoe").build());
      assertEquals(LegacyErrorCodeRegistry.SKIPPED, id);
    }
    assertTrue(sink.records().isEmpty());
  }
}
