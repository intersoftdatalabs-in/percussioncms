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

import com.intsof.percussioncms.auditlog.codes.DataErrorCodes;
import com.intsof.percussioncms.auditlog.codes.ExtensionErrorCodes;
import com.intsof.percussioncms.auditlog.codes.ServerErrorCodes;
import com.intsof.percussioncms.auditlog.codes.XmlErrorCodes;
import com.intsof.percussioncms.auditlog.sink.CapturingAuditLogSink;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Dual-write skip coverage for leftover extensions-main catalog codes (#3756). All of these
 * leftovers are non-auditable operational / validation noise.
 */
class ExtensionsMainResidualErrorCodesDualWriteTest {

  @BeforeEach
  void rebootstrap() {
    LegacyErrorCodeRegistry.clearForTests();
    LegacyErrorCodeRegistry.bootstrap();
  }

  @Test
  void leftoverExtensionCodesSkipDualWrite() {
    List<ExtensionErrorCodes> leftovers =
        List.of(
            ExtensionErrorCodes.EXT_PROCESSOR_EXCEPTION,
            ExtensionErrorCodes.EXT_PARAM_VALUE_MISMATCH,
            ExtensionErrorCodes.EXT_PARAM_VALUE_INVALID,
            ExtensionErrorCodes.EXT_MISSING_REQUIRED_PARAMETER_ERROR,
            ExtensionErrorCodes.EXT_MISSING_HTML_PARAMETER_ERROR,
            ExtensionErrorCodes.MISSING_HTML_PARAMETER,
            ExtensionErrorCodes.MISSING_REQUIRED_PARAM_NO,
            ExtensionErrorCodes.CATALOG_EXT_RESOURCE_ERROR,
            ExtensionErrorCodes.AUTHTYPE_REGISTRATION_MISSING,
            ExtensionErrorCodes.AUTHTYPE_RESOURCE_MISSING,
            ExtensionErrorCodes.VALIDATE_SLOTNAME_NOT_UNIQUE,
            ExtensionErrorCodes.TRANSLATION_ALREADY_EXISTS,
            ExtensionErrorCodes.VARIANT_HAS_RELATIONSHIPS_ERROR,
            ExtensionErrorCodes.BAD_PUBLISH_CONTENT_INITIALIZATION_DATA,
            ExtensionErrorCodes.BAD_PUBLISH_CONTENT_FILE_DATA,
            ExtensionErrorCodes.SCHEME_CANT_BE_FOUND,
            ExtensionErrorCodes.UNEXPECTED_EXT_TYPE_EXCEPTION);

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
  void leftoverXmlDataServerCodesSkipDualWrite() {
    CapturingAuditLogSink sink = new CapturingAuditLogSink("cap");
    DefaultAuditLogService svc = DefaultAuditLogService.builder().addSink(sink).build();

    assertFalse(XmlErrorCodes.XML_PROCESSING_ERROR.isAuditable());
    assertFalse(XmlErrorCodes.RAW_XML_DUMP.isAuditable());
    assertFalse(DataErrorCodes.INTERNAL_REQUEST_CALL_EXCEPTION.isAuditable());
    assertFalse(ServerErrorCodes.UNEXPECTED_EXCEPTION_LOG.isAuditable());

    for (int numeric :
        new int[] {
          XmlErrorCodes.XML_PROCESSING_ERROR.numericCode(),
          XmlErrorCodes.RAW_XML_DUMP.numericCode(),
          DataErrorCodes.INTERNAL_REQUEST_CALL_EXCEPTION.numericCode(),
          ServerErrorCodes.UNEXPECTED_EXCEPTION_LOG.numericCode()
        }) {
      AuditLogId id =
          LegacyErrorCodeRegistry.logIfAuditable(
              svc, numeric, AuditContext.builder().actor("jdoe").build());
      assertEquals(LegacyErrorCodeRegistry.SKIPPED, id);
    }
    assertTrue(sink.records().isEmpty());
  }
}
