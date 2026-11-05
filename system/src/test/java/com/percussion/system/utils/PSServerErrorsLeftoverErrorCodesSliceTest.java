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
package com.percussion.system.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.intsof.percussioncms.auditlog.SystemErrorCode;
import com.intsof.percussioncms.auditlog.codes.ServerErrorCodes;
import com.percussion.design.objectstore.PSLocator;
import com.percussion.error.IPSErrorCode;
import com.percussion.error.PSCatalogException;
import com.percussion.error.PSNotFoundException;
import com.percussion.extension.PSExtensionProcessingException;
import com.percussion.fastforward.utils.PSUtils;
import com.percussion.server.IPSRequestContext;
import com.percussion.server.IPSServerErrors;
import com.percussion.xml.PSDtdParser;
import com.percussion.xml.PSDtdTree;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Issue #4142 (parent #2616): leftover DTD / workflow / relationship / date / CMS production sites
 * throw typed {@link ServerErrorCodes} (not bare {@code IPSServerErrors} ints). Dual-write is
 * skipped because leftover codes in this slice are non-auditable.
 */
@Tag("UnitTest")
class PSServerErrorsLeftoverErrorCodesSliceTest {

  @Test
  void leftoverCatalogsMatchLegacyIntsAndSkipDualWriteWhereNonAuditable() {
    assertEquals(
        IPSServerErrors.MISSING_INTERNAL_REQUEST_RESOURCE,
        ServerErrorCodes.MISSING_INTERNAL_REQUEST_RESOURCE.numericCode());
    assertEquals(
        IPSServerErrors.XML_PARSER_SAX_ERROR,
        ServerErrorCodes.XML_PARSER_SAX_ERROR.numericCode());
    assertEquals(IPSServerErrors.ARGUMENT_ERROR, ServerErrorCodes.ARGUMENT_ERROR.numericCode());

    leftoverNonAuditable(ServerErrorCodes.MISSING_INTERNAL_REQUEST_RESOURCE);
    leftoverNonAuditable(ServerErrorCodes.XML_PARSER_SAX_ERROR);
    leftoverNonAuditable(ServerErrorCodes.ARGUMENT_ERROR);
  }

  @Test
  void leftoverProductionExceptionTypesRetainTypedCodes() {
    Object[] missingArgs = {"sys_psxRelationshipSupport/foo", "No request handler found."};
    leftoverNonAuditable(
        new PSNotFoundException(ServerErrorCodes.MISSING_INTERNAL_REQUEST_RESOURCE, missingArgs),
        ServerErrorCodes.MISSING_INTERNAL_REQUEST_RESOURCE);
    leftoverNonAuditable(
        new PSExtensionProcessingException(
            ServerErrorCodes.MISSING_INTERNAL_REQUEST_RESOURCE, missingArgs),
        ServerErrorCodes.MISSING_INTERNAL_REQUEST_RESOURCE);
    leftoverNonAuditable(
        new PSCatalogException(ServerErrorCodes.XML_PARSER_SAX_ERROR, "sax boom"),
        ServerErrorCodes.XML_PARSER_SAX_ERROR);

    RuntimeException cause = new RuntimeException("parse");
    leftoverNonAuditable(
        new PSCatalogException(ServerErrorCodes.XML_PARSER_SAX_ERROR, cause),
        ServerErrorCodes.XML_PARSER_SAX_ERROR);
  }

  @Test
  void invalidPsDateUsesArgumentErrorNumericCode() {
    IllegalArgumentException ex =
        assertThrows(IllegalArgumentException.class, () -> new PSDate(1999, 13, 1, 0, 0, 0));
    assertTrue(ex.getMessage().startsWith(ServerErrorCodes.ARGUMENT_ERROR.numericCode() + ":"));
    leftoverNonAuditable(ServerErrorCodes.ARGUMENT_ERROR);
  }

  @Test
  void missingInternalRequestOnRelationshipAndCmsThrowsTypedNotFound() throws Exception {
    IPSRequestContext request = mock(IPSRequestContext.class);
    when(request.getInternalRequest(anyString())).thenReturn(null);
    when(request.getInternalRequest(anyString(), nullable(Map.class), anyBoolean()))
        .thenReturn(null);

    leftoverNonAuditable(
        assertThrows(
            PSNotFoundException.class, () -> PSRelationshipUtils.doesTranslationExist(request)),
        ServerErrorCodes.MISSING_INTERNAL_REQUEST_RESOURCE);
    leftoverNonAuditable(
        assertThrows(
            PSNotFoundException.class, () -> PSCms.getContentType(request, new PSLocator(7, 1))),
        ServerErrorCodes.MISSING_INTERNAL_REQUEST_RESOURCE);
    leftoverNonAuditable(
        assertThrows(PSNotFoundException.class, () -> PSUtils.getbaseUrl("1", request)),
        ServerErrorCodes.MISSING_INTERNAL_REQUEST_RESOURCE);
  }

  @Test
  void invalidDtdUsesTypedSaxCatalogCode() {
    byte[] garbage = "not a dtd".getBytes(StandardCharsets.UTF_8);
    PSCatalogException treeEx =
        assertThrows(
            PSCatalogException.class,
            () -> new PSDtdTree(new ByteArrayInputStream(garbage), "Item"));
    leftoverNonAuditable(treeEx, ServerErrorCodes.XML_PARSER_SAX_ERROR);

    PSDtdParser parser = new PSDtdParser();
    PSCatalogException parserEx =
        assertThrows(
            PSCatalogException.class,
            () -> parser.parseDtd(new ByteArrayInputStream(garbage), "UTF-8"));
    leftoverNonAuditable(parserEx, ServerErrorCodes.XML_PARSER_SAX_ERROR);
  }

  @Test
  void typedCatalogCtorRejectsNullCode() {
    assertThrows(IllegalArgumentException.class, () -> new PSCatalogException((IPSErrorCode) null));
  }

  private static void leftoverNonAuditable(SystemErrorCode code) {
    assertFalse(code.isAuditable(), code.toString());
  }

  private static void leftoverNonAuditable(
      com.percussion.error.PSException ex, SystemErrorCode expected) {
    assertEquals(expected.numericCode(), ex.getErrorCode(), expected.toString());
    assertSame(expected, ex.getTypedErrorCode(), expected.toString());
    assertFalse(ex.isAuditable(), expected.toString());
    assertFalse(expected.isAuditable(), expected.toString());
  }
}
