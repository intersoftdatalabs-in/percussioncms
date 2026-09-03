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
package com.percussion.xml;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.intsof.percussioncms.auditlog.AuditContext;
import com.intsof.percussioncms.auditlog.AuditLogService;
import com.intsof.percussioncms.auditlog.LegacyErrorCodeRegistry;
import com.intsof.percussioncms.auditlog.SystemErrorCode;
import com.intsof.percussioncms.auditlog.codes.XmlErrorCodes;
import com.percussion.error.PSCatalogException;
import java.io.ByteArrayInputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Issue #4197 (parent #2616): leftover {@code IPSXmlErrors} throws in {@link PSDtdTree} use typed
 * {@link XmlErrorCodes}. Dual-write is skipped because every XML catalog code is non-auditable.
 */
@Tag("UnitTest")
class PSDtdTreeTypedXmlErrorCodesSliceTest {

  @Test
  void leftoverDtdCatalogsMatchLegacyIntsAndSkipDualWrite() {
    assertEquals(IPSXmlErrors.DTD_IO_ERROR, XmlErrorCodes.DTD_IO_ERROR.numericCode());
    assertEquals(
        IPSXmlErrors.DTD_ROOTNOTFOUND_ERROR, XmlErrorCodes.DTD_ROOTNOTFOUND_ERROR.numericCode());
    assertEquals(
        IPSXmlErrors.DTD_MULTIPLE_OCCURRENCE_NOTSUPPORTED_ERROR,
        XmlErrorCodes.DTD_MULTIPLE_OCCURRENCE_NOTSUPPORTED_ERROR.numericCode());
    assertEquals(
        IPSXmlErrors.DTD_ELEMENT_NOTFOUND_ERROR,
        XmlErrorCodes.DTD_ELEMENT_NOTFOUND_ERROR.numericCode());

    AuditLogService svc = mock(AuditLogService.class);
    leftoverNonAuditableSkipDualWrite(svc, XmlErrorCodes.DTD_IO_ERROR);
    leftoverNonAuditableSkipDualWrite(svc, XmlErrorCodes.DTD_ROOTNOTFOUND_ERROR);
    leftoverNonAuditableSkipDualWrite(
        svc, XmlErrorCodes.DTD_MULTIPLE_OCCURRENCE_NOTSUPPORTED_ERROR);
    leftoverNonAuditableSkipDualWrite(svc, XmlErrorCodes.DTD_ELEMENT_NOTFOUND_ERROR);
    verifyNoInteractions(svc);
  }

  @Test
  void missingFileUrlThrowsTypedDtdIoError(@TempDir Path tmp) throws Exception {
    Path missing = tmp.resolve("does-not-exist.dtd");
    URL url = missing.toUri().toURL();
    leftoverNonAuditable(
        assertThrows(PSCatalogException.class, () -> new PSDtdTree(url)), XmlErrorCodes.DTD_IO_ERROR);
  }

  @Test
  void missingRootDeclarationThrowsTypedRootNotFound() {
    PSDtd dtd = mock(PSDtd.class);
    when(dtd.getName()).thenReturn("GhostRoot");
    when(dtd.getElementDeclaration("GhostRoot")).thenReturn(null);

    leftoverNonAuditable(
        assertThrows(PSCatalogException.class, () -> new PSDtdTree(dtd)),
        XmlErrorCodes.DTD_ROOTNOTFOUND_ERROR);
  }

  @Test
  void nestedOccurrenceThrowsTypedMultipleOccurrenceNotSupported() {
    String dtd = "<!ELEMENT Root (Child+)?>\n<!ELEMENT Child (#PCDATA)>\n";
    leftoverNonAuditable(
        assertThrows(
            PSCatalogException.class,
            () -> new PSDtdTree(bytes(dtd), "Root")),
        XmlErrorCodes.DTD_MULTIPLE_OCCURRENCE_NOTSUPPORTED_ERROR);
  }

  @Test
  void undeclaredChildThrowsTypedElementNotFound() {
    String dtd = "<!ELEMENT Root (MissingChild)>\n";
    leftoverNonAuditable(
        assertThrows(
            PSCatalogException.class,
            () -> new PSDtdTree(bytes(dtd), "Root")),
        XmlErrorCodes.DTD_ELEMENT_NOTFOUND_ERROR);
  }

  @Test
  void simpleDeclaredElementStillParses() throws Exception {
    PSDtdTree tree = new PSDtdTree(bytes("<!ELEMENT Item (#PCDATA)>\n"), "Item");
    assertNotNull(tree.getRoot());
  }

  private static ByteArrayInputStream bytes(String dtd) {
    return new ByteArrayInputStream(dtd.getBytes(StandardCharsets.UTF_8));
  }

  private static void leftoverNonAuditableSkipDualWrite(AuditLogService svc, SystemErrorCode code) {
    assertFalse(code.isAuditable(), code.toString());
    assertEquals(
        LegacyErrorCodeRegistry.SKIPPED,
        LegacyErrorCodeRegistry.logIfAuditable(svc, code.numericCode(), AuditContext.empty(), "dtd"),
        code.toString());
  }

  private static void leftoverNonAuditable(PSCatalogException ex, SystemErrorCode expected) {
    assertEquals(expected.numericCode(), ex.getErrorCode(), expected.toString());
    assertSame(expected, ex.getTypedErrorCode(), expected.toString());
    assertFalse(ex.isAuditable(), expected.toString());
    assertFalse(expected.isAuditable(), expected.toString());
  }
}
