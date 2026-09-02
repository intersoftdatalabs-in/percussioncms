/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * you may obtain a copy of the License at
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
package com.percussion.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.intsof.percussioncms.auditlog.AuditContext;
import com.intsof.percussioncms.auditlog.AuditLogId;
import com.intsof.percussioncms.auditlog.DefaultAuditLogService;
import com.intsof.percussioncms.auditlog.LegacyErrorCodeRegistry;
import com.intsof.percussioncms.auditlog.SystemErrorCode;
import com.intsof.percussioncms.auditlog.codes.ServerErrorCodes;
import com.intsof.percussioncms.auditlog.spi.ConcurrentMemoryAuditLogRepository;
import com.percussion.conn.PSServerException;
import com.percussion.error.IPSErrorCode;
import com.percussion.error.PSException;
import com.percussion.error.PSIllegalArgumentException;
import com.percussion.error.PSNotFoundException;
import com.percussion.server.content.PSFormContentParser;
import com.percussion.server.content.PSJsonContentParser;
import com.percussion.server.content.PSXmlContentParser;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Issue #4150 (parent #2616 leftover): {@code PSServer}, application/request handlers, content
 * parsers, console/remote console, and {@code PSServerLogHandler} throw/log typed {@link
 * ServerErrorCodes} — not bare {@code IPSServerErrors} ints. Dual-write is skipped where leftover
 * operational codes are non-auditable; leftover authz codes remain dual-write eligible.
 */
@Tag("UnitTest")
class PSServerLeftoverErrorCodesSliceTest {

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
  void leftoverCatalogsMatchLegacyIntsAndSkipDualWriteWhereNonAuditable() {
    assertEquals(IPSServerErrors.RAW_DUMP, ServerErrorCodes.RAW_DUMP.numericCode());
    assertEquals(
        IPSServerErrors.INVALID_CONTENT_TYPE, ServerErrorCodes.INVALID_CONTENT_TYPE.numericCode());
    assertEquals(
        IPSServerErrors.PARSER_UNSUPPORTED_CONTENT_TYPE,
        ServerErrorCodes.PARSER_UNSUPPORTED_CONTENT_TYPE.numericCode());
    assertEquals(
        IPSServerErrors.XML_PARSER_SAX_ERROR, ServerErrorCodes.XML_PARSER_SAX_ERROR.numericCode());
    assertEquals(
        IPSServerErrors.JSON_PARSER_ERROR, ServerErrorCodes.JSON_PARSER_ERROR.numericCode());
    assertEquals(
        IPSServerErrors.INVALID_REQUEST_LINE, ServerErrorCodes.INVALID_REQUEST_LINE.numericCode());
    assertEquals(
        IPSServerErrors.VALIDATION_RULES_NOT_MET,
        ServerErrorCodes.VALIDATION_RULES_NOT_MET.numericCode());
    assertEquals(
        IPSServerErrors.APP_DATASET_NOT_FOUND, ServerErrorCodes.APP_DATASET_NOT_FOUND.numericCode());
    assertEquals(
        IPSServerErrors.NULL_APPLICATION_ERROR,
        ServerErrorCodes.NULL_APPLICATION_ERROR.numericCode());
    assertEquals(
        IPSServerErrors.EXCEPTION_NOT_CAUGHT, ServerErrorCodes.EXCEPTION_NOT_CAUGHT.numericCode());
    assertEquals(
        IPSServerErrors.UNEXPECTED_EXCEPTION_CONSOLE,
        ServerErrorCodes.UNEXPECTED_EXCEPTION_CONSOLE.numericCode());
    assertEquals(
        IPSServerErrors.UNEXPECTED_EXCEPTION_LOG,
        ServerErrorCodes.UNEXPECTED_EXCEPTION_LOG.numericCode());
    assertEquals(
        IPSServerErrors.RCONSOLE_CONN_OBJ_NULL,
        ServerErrorCodes.RCONSOLE_CONN_OBJ_NULL.numericCode());
    assertEquals(
        IPSServerErrors.RCONSOLE_CMD_EMPTY, ServerErrorCodes.RCONSOLE_CMD_EMPTY.numericCode());
    assertEquals(
        IPSServerErrors.REQ_DOC_MISSING, ServerErrorCodes.REQ_DOC_MISSING.numericCode());
    assertEquals(
        IPSServerErrors.RESPONSE_SEND_ERROR, ServerErrorCodes.RESPONSE_SEND_ERROR.numericCode());
    assertEquals(
        IPSServerErrors.REQUEST_HANDLER_NOT_FOUND,
        ServerErrorCodes.REQUEST_HANDLER_NOT_FOUND.numericCode());
    assertEquals(
        IPSServerErrors.SERVER_LOCK_NOT_ACQUIRED,
        ServerErrorCodes.SERVER_LOCK_NOT_ACQUIRED.numericCode());
    assertEquals(
        IPSServerErrors.NO_AUTHORIZATION, ServerErrorCodes.NO_AUTHORIZATION.numericCode());
    assertEquals(
        IPSServerErrors.COMMUNITIES_AUTHENTICATION_FAILED_INVALID_COMMUNITY,
        ServerErrorCodes.COMMUNITIES_AUTHENTICATION_FAILED_INVALID_COMMUNITY.numericCode());
    assertEquals(
        IPSServerErrors.COMMUNITIES_AUTHENTICATION_FAILED_ERROR,
        ServerErrorCodes.COMMUNITIES_AUTHENTICATION_FAILED_ERROR.numericCode());

    leftoverNonAuditable(ServerErrorCodes.RAW_DUMP);
    leftoverNonAuditable(ServerErrorCodes.INVALID_CONTENT_TYPE);
    leftoverNonAuditable(ServerErrorCodes.PARSER_UNSUPPORTED_CONTENT_TYPE);
    leftoverNonAuditable(ServerErrorCodes.XML_PARSER_SAX_ERROR);
    leftoverNonAuditable(ServerErrorCodes.JSON_PARSER_ERROR);
    leftoverNonAuditable(ServerErrorCodes.INVALID_REQUEST_LINE);
    leftoverNonAuditable(ServerErrorCodes.VALIDATION_RULES_NOT_MET);
    leftoverNonAuditable(ServerErrorCodes.APP_DATASET_NOT_FOUND);
    leftoverNonAuditable(ServerErrorCodes.NULL_APPLICATION_ERROR);
    leftoverNonAuditable(ServerErrorCodes.EXCEPTION_NOT_CAUGHT);
    leftoverNonAuditable(ServerErrorCodes.UNEXPECTED_EXCEPTION_CONSOLE);
    leftoverNonAuditable(ServerErrorCodes.UNEXPECTED_EXCEPTION_LOG);
    leftoverNonAuditable(ServerErrorCodes.RCONSOLE_CONN_OBJ_NULL);
    leftoverNonAuditable(ServerErrorCodes.RCONSOLE_CMD_EMPTY);
    leftoverNonAuditable(ServerErrorCodes.REQ_DOC_MISSING);
    leftoverNonAuditable(ServerErrorCodes.RESPONSE_SEND_ERROR);
    leftoverNonAuditable(ServerErrorCodes.REQUEST_HANDLER_NOT_FOUND);
    leftoverNonAuditable(ServerErrorCodes.SERVER_LOCK_NOT_ACQUIRED);
    leftoverAuditable(ServerErrorCodes.NO_AUTHORIZATION);
    leftoverAuditable(ServerErrorCodes.COMMUNITIES_AUTHENTICATION_FAILED_INVALID_COMMUNITY);
    leftoverAuditable(ServerErrorCodes.COMMUNITIES_AUTHENTICATION_FAILED_ERROR);
  }

  @Test
  void leftoverNonAuditableCodesSkipDualWrite() {
    DefaultAuditLogService svc = DefaultAuditLogService.createDefault();
    List<ServerErrorCodes> leftovers =
        List.of(
            ServerErrorCodes.RAW_DUMP,
            ServerErrorCodes.PARSER_UNSUPPORTED_CONTENT_TYPE,
            ServerErrorCodes.INVALID_CONTENT_TYPE,
            ServerErrorCodes.EXCEPTION_NOT_CAUGHT,
            ServerErrorCodes.UNEXPECTED_EXCEPTION_LOG,
            ServerErrorCodes.RCONSOLE_CMD_EMPTY,
            ServerErrorCodes.REQUEST_HANDLER_NOT_FOUND,
            ServerErrorCodes.SERVER_INIT_START);

    for (ServerErrorCodes code : leftovers) {
      assertFalse(code.isAuditable(), code.name());
      assertSame(code, LegacyErrorCodeRegistry.find(code.numericCode()).orElseThrow());
      AuditLogId id =
          LegacyErrorCodeRegistry.logIfAuditable(
              svc, code.numericCode(), AuditContext.builder().actor("jdoe").build());
      assertEquals(LegacyErrorCodeRegistry.SKIPPED, id, code.name());
    }
    assertEquals(0, ConcurrentMemoryAuditLogRepository.INSTANCE.size());
  }

  @Test
  void leftoverAuditableAuthzCodesStillDualWrite() {
    DefaultAuditLogService svc = DefaultAuditLogService.createDefault();
    for (ServerErrorCodes code :
        List.of(
            ServerErrorCodes.NO_AUTHORIZATION,
            ServerErrorCodes.COMMUNITIES_AUTHENTICATION_FAILED_INVALID_COMMUNITY,
            ServerErrorCodes.COMMUNITIES_AUTHENTICATION_FAILED_ERROR)) {
      assertTrue(code.isAuditable(), code.name());
      ConcurrentMemoryAuditLogRepository.INSTANCE.clear();
      AuditLogId id =
          LegacyErrorCodeRegistry.logIfAuditable(
              svc, code.numericCode(), AuditContext.builder().actor("jdoe").build(), "sess");
      assertFalse(LegacyErrorCodeRegistry.SKIPPED.equals(id), code.name());
      assertEquals(1, ConcurrentMemoryAuditLogRepository.INSTANCE.size(), code.name());
      assertEquals(code, ConcurrentMemoryAuditLogRepository.INSTANCE.findAll().get(0).code());
    }
  }

  @Test
  void productionExceptionTypesRetainTypedCodes() {
    leftoverNonAuditable(
        new PSRequestParsingException(
            ServerErrorCodes.INVALID_CONTENT_TYPE, new Object[] {"text/unknown"}),
        ServerErrorCodes.INVALID_CONTENT_TYPE);
    leftoverNonAuditable(new PSInvalidRequestException(), ServerErrorCodes.INVALID_REQUEST_LINE);
    leftoverNonAuditable(
        new PSServerLockException(ServerErrorCodes.SERVER_LOCK_NOT_ACQUIRED, new String[] {"a", "b"}),
        ServerErrorCodes.SERVER_LOCK_NOT_ACQUIRED);
    leftoverNonAuditable(
        new PSNotFoundException(ServerErrorCodes.APP_DATASET_NOT_FOUND, new Object[] {"ds", "app"}),
        ServerErrorCodes.APP_DATASET_NOT_FOUND);
    leftoverNonAuditable(
        new PSIllegalArgumentException(ServerErrorCodes.RCONSOLE_CONN_OBJ_NULL),
        ServerErrorCodes.RCONSOLE_CONN_OBJ_NULL);

    leftoverAuditable(
        new PSServerException(
            ServerErrorCodes.COMMUNITIES_AUTHENTICATION_FAILED_INVALID_COMMUNITY,
            new Object[] {"alice", "CommunityX"}),
        ServerErrorCodes.COMMUNITIES_AUTHENTICATION_FAILED_INVALID_COMMUNITY);
    leftoverAuditable(
        new PSServerException(
            ServerErrorCodes.NO_AUTHORIZATION, new Object[] {"alice"}),
        ServerErrorCodes.NO_AUTHORIZATION);
  }

  @Test
  void remoteConsoleNullConnectionThrowsTypedIllegalArgument() {
    PSIllegalArgumentException ex =
        assertThrows(PSIllegalArgumentException.class, () -> new PSRemoteConsole(null));
    leftoverNonAuditable(ex, ServerErrorCodes.RCONSOLE_CONN_OBJ_NULL);
  }

  @Test
  void xmlParserUnsupportedContentTypeThrowsTyped() throws Exception {
    PSXmlContentParser parser = new PSXmlContentParser();
    PSRequest request = new PSRequest(null, null, null, null);
    PSRequestParsingException ex =
        assertThrows(
            PSRequestParsingException.class,
            () -> parser.parse(request, "text/plain", "UTF-8", null, 4));
    leftoverNonAuditable(ex, ServerErrorCodes.PARSER_UNSUPPORTED_CONTENT_TYPE);
  }

  @Test
  void jsonParserUnsupportedContentTypeThrowsTyped() throws Exception {
    PSJsonContentParser parser = new PSJsonContentParser();
    PSRequest request = new PSRequest(null, null, null, null);
    PSRequestParsingException ex =
        assertThrows(
            PSRequestParsingException.class,
            () -> parser.parse(request, "text/plain", "UTF-8", null, 4));
    leftoverNonAuditable(ex, ServerErrorCodes.PARSER_UNSUPPORTED_CONTENT_TYPE);
  }

  @Test
  void formParserUnsupportedContentTypeThrowsTyped() throws Exception {
    PSFormContentParser parser = new PSFormContentParser();
    PSRequest request = new PSRequest(null, null, null, null);
    PSRequestParsingException ex =
        assertThrows(
            PSRequestParsingException.class,
            () -> parser.parse(request, "text/plain", "UTF-8", null, 4));
    leftoverNonAuditable(ex, ServerErrorCodes.PARSER_UNSUPPORTED_CONTENT_TYPE);
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
