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
package com.percussion.conn;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.intsof.percussioncms.auditlog.SystemErrorCode;
import com.intsof.percussioncms.auditlog.codes.ConnectionErrorCodes;
import com.intsof.percussioncms.auditlog.codes.ServerErrorCodes;
import com.percussion.content.IPSMimeContentTypes;
import com.percussion.error.IPSErrorCode;
import com.percussion.error.PSException;
import com.percussion.server.IPSServerErrors;
import com.percussion.xml.PSXmlDocumentBuilder;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Issue #4016 (parent #2616): leftover {@code com.percussion.conn} production sites throw typed
 * {@code ConnectionErrorCodes} / {@code ServerErrorCodes.RAW_DUMP} (not bare {@code IPS*Errors}
 * ints). Dual-write is skipped where leftover connection codes are non-auditable; {@code
 * UNAUTHORIZED} remains dual-write eligible.
 */
@Tag("UnitTest")
class PSConnLeftoverErrorCodesSliceTest {

  @Test
  void leftoverCatalogsMatchLegacyIntsAndSkipDualWriteWhereNonAuditable() {
    assertEquals(
        IPSConnectionErrors.SERVER_NOT_RESPONDING,
        ConnectionErrorCodes.SERVER_NOT_RESPONDING.numericCode());
    assertEquals(
        IPSConnectionErrors.UNKNOWN_SERVER_EXCEPTION,
        ConnectionErrorCodes.UNKNOWN_SERVER_EXCEPTION.numericCode());
    assertEquals(
        IPSConnectionErrors.SERVER_GENERATED_EXCEPTION,
        ConnectionErrorCodes.SERVER_GENERATED_EXCEPTION.numericCode());
    assertEquals(
        IPSConnectionErrors.RESPONSE_INVALID_MIME_TYPE,
        ConnectionErrorCodes.RESPONSE_INVALID_MIME_TYPE.numericCode());
    assertEquals(
        IPSConnectionErrors.RESPONSE_PARSE_EXCEPTION,
        ConnectionErrorCodes.RESPONSE_PARSE_EXCEPTION.numericCode());
    assertEquals(
        IPSConnectionErrors.RESPONSE_PARSE_EXCEPTION_NOLINEINFO,
        ConnectionErrorCodes.RESPONSE_PARSE_EXCEPTION_NOLINEINFO.numericCode());
    assertEquals(IPSConnectionErrors.UNAUTHORIZED, ConnectionErrorCodes.UNAUTHORIZED.numericCode());
    assertEquals(IPSServerErrors.RAW_DUMP, ServerErrorCodes.RAW_DUMP.numericCode());

    leftoverNonAuditable(ConnectionErrorCodes.SERVER_NOT_RESPONDING);
    leftoverNonAuditable(ConnectionErrorCodes.UNKNOWN_SERVER_EXCEPTION);
    leftoverNonAuditable(ConnectionErrorCodes.SERVER_GENERATED_EXCEPTION);
    leftoverNonAuditable(ConnectionErrorCodes.RESPONSE_INVALID_MIME_TYPE);
    leftoverNonAuditable(ConnectionErrorCodes.RESPONSE_PARSE_EXCEPTION);
    leftoverNonAuditable(ConnectionErrorCodes.RESPONSE_PARSE_EXCEPTION_NOLINEINFO);
    leftoverNonAuditable(ServerErrorCodes.RAW_DUMP);
    leftoverAuditable(ConnectionErrorCodes.UNAUTHORIZED);
  }

  @Test
  void leftoverProductionExceptionTypesRetainTypedCodes() {
    leftoverNonAuditable(
        new PSServerException(ConnectionErrorCodes.SERVER_NOT_RESPONDING, "http://cms/Designer"),
        ConnectionErrorCodes.SERVER_NOT_RESPONDING);
    leftoverNonAuditable(
        new PSServerException(ServerErrorCodes.RAW_DUMP, "stack"), ServerErrorCodes.RAW_DUMP);
    leftoverNonAuditable(
        new PSServerException(ConnectionErrorCodes.UNKNOWN_SERVER_EXCEPTION, "io"),
        ConnectionErrorCodes.UNKNOWN_SERVER_EXCEPTION);
    leftoverNonAuditable(
        new PSServerException(
            ConnectionErrorCodes.RESPONSE_INVALID_MIME_TYPE,
            new Object[] {IPSMimeContentTypes.MIME_TYPE_TEXT_XML, "text/html"}),
        ConnectionErrorCodes.RESPONSE_INVALID_MIME_TYPE);
    leftoverNonAuditable(
        new PSServerException(
            ConnectionErrorCodes.RESPONSE_PARSE_EXCEPTION,
            new Object[] {"bad xml", "12", "3"}),
        ConnectionErrorCodes.RESPONSE_PARSE_EXCEPTION);
    leftoverNonAuditable(
        new PSServerException(
            ConnectionErrorCodes.RESPONSE_PARSE_EXCEPTION_NOLINEINFO, new Object[] {"bad xml"}),
        ConnectionErrorCodes.RESPONSE_PARSE_EXCEPTION_NOLINEINFO);

    RuntimeException cause = new RuntimeException("wrap");
    PSServerException wrapped = new PSServerException(cause);
    leftoverNonAuditable(wrapped, ConnectionErrorCodes.UNKNOWN_SERVER_EXCEPTION);
    assertTrue(String.valueOf(wrapped.getErrorArguments()[0]).contains("wrap"));

    PSServerException unauthorized =
        new PSServerException(ConnectionErrorCodes.UNAUTHORIZED, "401");
    assertSame(ConnectionErrorCodes.UNAUTHORIZED, unauthorized.getTypedErrorCode());
    assertEquals(IPSConnectionErrors.UNAUTHORIZED, unauthorized.getErrorCode());
    assertTrue(unauthorized.isAuditable());
  }

  @Test
  void createExceptionFromXmlUsesGeneratedExceptionDefault() {
    assertNull(PSDesignerConnection.createExceptionFromXml(null));

    Document wrongDoc = PSXmlDocumentBuilder.createXmlDocument();
    Element wrong = PSXmlDocumentBuilder.createRoot(wrongDoc, "NotError");
    assertNull(PSDesignerConnection.createExceptionFromXml(wrong));

    Document doc = PSXmlDocumentBuilder.createXmlDocument();
    Element root = PSXmlDocumentBuilder.createRoot(doc, "PSXError");
    PSXmlDocumentBuilder.addElement(doc, root, "exceptionClass", "java.lang.RuntimeException");
    PSXmlDocumentBuilder.addElement(doc, root, "message", "boom");
    PSException reconstructed = PSDesignerConnection.createExceptionFromXml(root);
    assertTrue(reconstructed instanceof PSServerException);
    assertEquals(
        ConnectionErrorCodes.SERVER_GENERATED_EXCEPTION.numericCode(),
        reconstructed.getErrorCode());
    assertEquals(IPSConnectionErrors.SERVER_GENERATED_EXCEPTION, reconstructed.getErrorCode());
  }

  @Test
  void typedProductionCtorsRejectNullCode() {
    assertThrows(IllegalArgumentException.class, () -> new PSServerException((IPSErrorCode) null));
    assertThrows(
        IllegalArgumentException.class, () -> new PSServerException((IPSErrorCode) null, "arg"));
    assertThrows(
        IllegalArgumentException.class,
        () -> new PSServerException((IPSErrorCode) null, new Object[] {"arg"}));
  }

  private static void leftoverNonAuditable(SystemErrorCode code) {
    assertFalse(code.isAuditable(), code.toString());
  }

  private static void leftoverAuditable(SystemErrorCode code) {
    assertTrue(code.isAuditable(), code.toString());
  }

  private static void leftoverNonAuditable(PSException ex, IPSErrorCode expected) {
    assertEquals(expected.numericCode(), ex.getErrorCode(), expected.toString());
    assertSame(expected, ex.getTypedErrorCode(), expected.toString());
    assertFalse(ex.isAuditable(), expected.toString());
    assertFalse(expected.isAuditable(), expected.toString());
  }
}
