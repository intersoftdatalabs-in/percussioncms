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

import com.intsof.percussioncms.auditlog.codes.RemoteErrorCodes;
import com.intsof.percussioncms.auditlog.codes.ServletErrorCodes;
import com.intsof.percussioncms.auditlog.codes.WebdavErrorCodes;
import com.intsof.percussioncms.auditlog.sink.CapturingAuditLogSink;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Dual-write skip coverage for leftover servlet-hook / WebDAV catalog codes (#3848). Every leftover
 * in this slice is non-auditable operational / protocol noise.
 */
class ServletWebdavResidualErrorCodesDualWriteTest {

  @BeforeEach
  void rebootstrap() {
    LegacyErrorCodeRegistry.clearForTests();
    LegacyErrorCodeRegistry.bootstrap();
  }

  @Test
  void leftoverServletCodesSkipDualWrite() {
    CapturingAuditLogSink sink = new CapturingAuditLogSink("cap");
    DefaultAuditLogService svc = DefaultAuditLogService.builder().addSink(sink).build();

    for (ServletErrorCodes code : ServletErrorCodes.values()) {
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
  void leftoverWebdavCodesSkipDualWrite() {
    CapturingAuditLogSink sink = new CapturingAuditLogSink("cap");
    DefaultAuditLogService svc = DefaultAuditLogService.builder().addSink(sink).build();

    WebdavErrorCodes[] leftovers = {
      WebdavErrorCodes.XML_ATTRIBUTE_MUST_BE_SPECIFIED,
      WebdavErrorCodes.XML_INVALID_FORMAT,
      WebdavErrorCodes.XML_ELEMENT_CANNOT_BE_EMPTY,
      WebdavErrorCodes.XML_FAILED_CREATE_DOC_FROM_CONTENT,
      WebdavErrorCodes.UNSUPPORTED_METHOD,
      WebdavErrorCodes.MIMETYPES_REQUIRED,
      WebdavErrorCodes.CANNOT_HAVE_DUPLICATE_PROPERTIES,
      WebdavErrorCodes.MISSING_REQUIRED_PROPERTY,
      WebdavErrorCodes.CAN_ONLY_HAVE_ONE_DEFAULT_CONTENTTYPE,
      WebdavErrorCodes.IO_EXCEPTION_OCCURED,
      WebdavErrorCodes.SAX_EXCEPTION_OCCURED,
      WebdavErrorCodes.FILE_DOES_NOT_EXIST,
      WebdavErrorCodes.DUPLICATE_CONTENTTYPE_NAMES,
      WebdavErrorCodes.RESOURCE_NOT_FIND,
      WebdavErrorCodes.HEADER_MISSING,
      WebdavErrorCodes.FORBIDDEN_SRC_TARGET_SAME,
      WebdavErrorCodes.METHOD_FAIL_CANNOT_OVERWRITE,
      WebdavErrorCodes.ITEMFIELD_NOT_EXIST,
      WebdavErrorCodes.LOCKSCOPE_NOT_ALLOWED,
      WebdavErrorCodes.LOCKTYPE_NOT_ALLOWED,
      WebdavErrorCodes.FIELDNAME_CANNOT_BE_EMPTY_OR_MISSING,
      WebdavErrorCodes.CONTENTTYPE_NOT_CONFIGURED,
      WebdavErrorCodes.UNKNOWN_BODY_IN_MKCOL_REQ,
      WebdavErrorCodes.MALFORMED_URL_FROM_HEADER,
      WebdavErrorCodes.NO_PUBLIC_AUTO_TRANSITION,
      WebdavErrorCodes.NO_QE_AUTO_TRANSITION
    };

    for (WebdavErrorCodes code : leftovers) {
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
  void leftoverRemoteUnexpectedErrorSkipsDualWrite() {
    CapturingAuditLogSink sink = new CapturingAuditLogSink("cap");
    DefaultAuditLogService svc = DefaultAuditLogService.builder().addSink(sink).build();

    assertFalse(RemoteErrorCodes.REMOTE_UNEXPECTED_ERROR.isAuditable());
    AuditLogId id =
        LegacyErrorCodeRegistry.logIfAuditable(
            svc,
            RemoteErrorCodes.REMOTE_UNEXPECTED_ERROR.numericCode(),
            AuditContext.builder().actor("jdoe").build());
    assertEquals(LegacyErrorCodeRegistry.SKIPPED, id);
    assertTrue(sink.records().isEmpty());
  }
}
