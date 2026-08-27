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
package com.percussion.cms.objectstore.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.intsof.percussioncms.auditlog.codes.CmsErrorCodes;
import com.intsof.percussioncms.auditlog.codes.DataErrorCodes;
import com.intsof.percussioncms.auditlog.codes.HttpErrorCodes;
import com.intsof.percussioncms.auditlog.codes.PathItemErrorCodes;
import com.intsof.percussioncms.auditlog.codes.SecurityErrorCodes;
import com.intsof.percussioncms.auditlog.codes.ServerErrorCodes;
import com.intsof.percussioncms.auditlog.codes.ServerWebServicesErrorCodes;
import com.percussion.cms.IPSCmsErrors;
import com.percussion.cms.PSCmsException;
import com.percussion.data.IPSDataErrors;
import com.percussion.design.objectstore.PSDisplayError;
import com.percussion.design.objectstore.PSFieldValidationException;
import com.percussion.error.IPSErrorCode;
import com.percussion.error.PSException;
import com.percussion.error.PSNotFoundException;
import com.percussion.extension.PSExtensionProcessingException;
import com.percussion.extension.PSParameterMismatchException;
import com.percussion.security.IPSSecurityErrors;
import com.percussion.server.IPSHttpErrors;
import com.percussion.server.IPSRequestContext;
import com.percussion.server.IPSServerErrors;
import com.percussion.server.webservices.IPSWebServicesErrors;
import com.percussion.xml.PSXmlDocumentBuilder;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Issue #3900 (parent #2616): leftover {@code cms.objectstore.server} production sites throw typed
 * {@code *ErrorCodes} (not bare {@code IPS*Errors} ints). Dual-write is skipped where the catalog
 * is non-auditable.
 */
@Tag("UnitTest")
class PSCmsObjectstoreServerLeftoverErrorCodesSliceTest {

  @Test
  void leftoverCatalogsMatchLegacyIntsAndSkipDualWriteWhereNonAuditable() {
    assertEquals(
        IPSCmsErrors.CORRUPT_DATABASE_ENTRY, CmsErrorCodes.CORRUPT_DATABASE_ENTRY.numericCode());
    assertEquals(
        IPSCmsErrors.INVALID_CONTENT_TYPE_ID, CmsErrorCodes.INVALID_CONTENT_TYPE_ID.numericCode());
    assertEquals(
        IPSCmsErrors.CONTENT_TYPE_CANNOT_BE_OPENED,
        CmsErrorCodes.CONTENT_TYPE_CANNOT_BE_OPENED.numericCode());
    assertEquals(
        IPSCmsErrors.REQUIRED_DOCUMENT_MISSING_ERROR,
        CmsErrorCodes.REQUIRED_DOCUMENT_MISSING_ERROR.numericCode());
    assertEquals(
        IPSCmsErrors.MALFORMED_XML_DOCUMENT_UKNOWN_NODE_TYPE,
        CmsErrorCodes.MALFORMED_XML_DOCUMENT_UKNOWN_NODE_TYPE.numericCode());
    assertEquals(
        IPSCmsErrors.CMS_INTERNAL_REQUEST_ERROR,
        CmsErrorCodes.CMS_INTERNAL_REQUEST_ERROR.numericCode());
    assertEquals(
        IPSCmsErrors.REQUIRED_RESOURCE_MISSING,
        CmsErrorCodes.REQUIRED_RESOURCE_MISSING.numericCode());
    assertEquals(IPSCmsErrors.XML_PARSING_ERROR, CmsErrorCodes.XML_PARSING_ERROR.numericCode());
    assertEquals(
        IPSCmsErrors.MISSING_HTML_PARAMETER, CmsErrorCodes.MISSING_HTML_PARAMETER.numericCode());
    assertEquals(
        IPSCmsErrors.CONTENTTYPE_DEFINITION_NOT_FOUND,
        CmsErrorCodes.CONTENTTYPE_DEFINITION_NOT_FOUND.numericCode());
    assertEquals(
        IPSCmsErrors.UNKNOWN_RELATED_TYPE, CmsErrorCodes.UNKNOWN_RELATED_TYPE.numericCode());
    assertEquals(
        IPSCmsErrors.INVALID_RELATED_TYPE, CmsErrorCodes.INVALID_RELATED_TYPE.numericCode());
    assertEquals(
        IPSCmsErrors.UNEXPECTED_KEY_TYPE, CmsErrorCodes.UNEXPECTED_KEY_TYPE.numericCode());
    assertEquals(
        IPSCmsErrors.PERSISTED_KEY_EXPECTED, CmsErrorCodes.PERSISTED_KEY_EXPECTED.numericCode());
    assertEquals(
        IPSCmsErrors.INVALID_INSERT_RELATIONSHIP_TYPE,
        CmsErrorCodes.INVALID_INSERT_RELATIONSHIP_TYPE.numericCode());
    assertEquals(IPSCmsErrors.UNEXPECTED_ERROR, CmsErrorCodes.UNEXPECTED_ERROR.numericCode());
    assertEquals(
        IPSCmsErrors.UNEXPECTED_CATALOG_ERROR, CmsErrorCodes.UNEXPECTED_CATALOG_ERROR.numericCode());
    assertEquals(
        IPSCmsErrors.INVALID_RELATIONSHIP_PROP_VALUE,
        CmsErrorCodes.INVALID_RELATIONSHIP_PROP_VALUE.numericCode());
    assertEquals(IPSCmsErrors.VALIDATION_ERROR, CmsErrorCodes.VALIDATION_ERROR.numericCode());
    assertEquals(IPSCmsErrors.NON_EXITING_OWNER, CmsErrorCodes.NON_EXITING_OWNER.numericCode());
    assertEquals(
        IPSCmsErrors.NON_EXITING_DEPENDENT, CmsErrorCodes.NON_EXITING_DEPENDENT.numericCode());
    assertEquals(IPSCmsErrors.INVALID_CONTENT_TYPE, CmsErrorCodes.INVALID_CONTENT_TYPE.numericCode());
    assertEquals(
        IPSCmsErrors.FOLDER_ERROR_MSG, PathItemErrorCodes.FOLDER_ERROR_MSG.numericCode());
    assertEquals(
        IPSCmsErrors.SQL_EXCEPTION_WRAPPER, ServerErrorCodes.RAW_DUMP.numericCode());
    assertEquals(
        IPSServerErrors.MISSING_INTERNAL_REQUEST_RESOURCE,
        ServerErrorCodes.MISSING_INTERNAL_REQUEST_RESOURCE.numericCode());
    assertEquals(
        IPSServerErrors.UNEXPECTED_EXCEPTION_CONSOLE,
        ServerErrorCodes.UNEXPECTED_EXCEPTION_CONSOLE.numericCode());
    assertEquals(IPSServerErrors.CE_SQL_ERRORS, ServerErrorCodes.CE_SQL_ERRORS.numericCode());
    assertEquals(
        IPSDataErrors.INTERNAL_REQUEST_AUTHORIZATION_EXCEPTION,
        DataErrorCodes.INTERNAL_REQUEST_AUTHORIZATION_EXCEPTION.numericCode());
    assertEquals(
        IPSDataErrors.INTERNAL_REQUEST_AUTHENTICATION_FAILED_EXCEPTION,
        DataErrorCodes.INTERNAL_REQUEST_AUTHENTICATION_FAILED_EXCEPTION.numericCode());
    assertEquals(
        IPSSecurityErrors.SESS_NOT_AUTHORIZED, SecurityErrorCodes.SESS_NOT_AUTHORIZED.numericCode());
    assertEquals(IPSHttpErrors.HTTP_UNAUTHORIZED, HttpErrorCodes.HTTP_UNAUTHORIZED.numericCode());
    assertEquals(
        IPSWebServicesErrors.WEB_SERVICE_CONTENT_TYPE_NOT_FOUND,
        ServerWebServicesErrorCodes.WEB_SERVICE_CONTENT_TYPE_NOT_FOUND.numericCode());
    assertEquals(
        IPSWebServicesErrors.WEB_SERVICE_INTERNAL_REQUEST_FAILED,
        ServerWebServicesErrorCodes.WEB_SERVICE_INTERNAL_REQUEST_FAILED.numericCode());

    assertFalse(CmsErrorCodes.CORRUPT_DATABASE_ENTRY.isAuditable());
    assertFalse(CmsErrorCodes.MISSING_HTML_PARAMETER.isAuditable());
    assertFalse(CmsErrorCodes.VALIDATION_ERROR.isAuditable());
    assertFalse(PathItemErrorCodes.FOLDER_ERROR_MSG.isAuditable());
    assertFalse(ServerErrorCodes.RAW_DUMP.isAuditable());
    assertFalse(ServerErrorCodes.MISSING_INTERNAL_REQUEST_RESOURCE.isAuditable());
    assertFalse(DataErrorCodes.INTERNAL_REQUEST_AUTHORIZATION_EXCEPTION.isAuditable());
    assertFalse(HttpErrorCodes.HTTP_UNAUTHORIZED.isAuditable());
    assertFalse(ServerWebServicesErrorCodes.WEB_SERVICE_CONTENT_TYPE_NOT_FOUND.isAuditable());
    assertTrue(SecurityErrorCodes.SESS_NOT_AUTHORIZED.isAuditable());
  }

  @Test
  void idGeneratorExitMissingLookupKeyThrowsTypedParameterMismatch() throws Exception {
    IPSRequestContext request = mock(IPSRequestContext.class);
    when(request.getParameter("sys_lookupkey", "")).thenReturn("  ");
    Document doc = PSXmlDocumentBuilder.createXmlDocument();

    PSParameterMismatchException ex =
        assertThrows(
            PSParameterMismatchException.class,
            () -> new PSIdGeneratorExit().processResultDocument(null, request, doc));
    assertSame(CmsErrorCodes.MISSING_HTML_PARAMETER, ex.getTypedErrorCode());
    assertEquals(CmsErrorCodes.MISSING_HTML_PARAMETER.numericCode(), ex.getErrorCode());
    assertFalse(ex.isAuditable());
  }

  @Test
  void corruptDatabaseExceptionRetainsTypedNonAuditableCode() {
    PSCorruptDatabaseException ex =
        new PSCorruptDatabaseException("CONTENTSTATUS", "42", "duplicate key");
    assertSame(CmsErrorCodes.CORRUPT_DATABASE_ENTRY, ex.getTypedErrorCode());
    assertEquals(CmsErrorCodes.CORRUPT_DATABASE_ENTRY.numericCode(), ex.getErrorCode());
    assertFalse(ex.isAuditable());
  }

  @Test
  void leftoverProductionExceptionTypesRetainTypedCodes() throws Exception {
    PSCmsException missingResource =
        new PSCmsException(CmsErrorCodes.REQUIRED_RESOURCE_MISSING, "sys_psxObjectSupport/folderacl");
    assertSame(CmsErrorCodes.REQUIRED_RESOURCE_MISSING, missingResource.getTypedErrorCode());
    assertFalse(missingResource.isAuditable());

    PSCmsException catalog =
        new PSCmsException(
            CmsErrorCodes.UNEXPECTED_CATALOG_ERROR, new Object[] {"sys_psxCms", "parse"});
    assertSame(CmsErrorCodes.UNEXPECTED_CATALOG_ERROR, catalog.getTypedErrorCode());

    PSCmsException propValue =
        new PSCmsException(
            CmsErrorCodes.INVALID_RELATIONSHIP_PROP_VALUE, new Object[] {"1", "2", "sys_sortrank", "x"});
    assertSame(CmsErrorCodes.INVALID_RELATIONSHIP_PROP_VALUE, propValue.getTypedErrorCode());

    PSCmsException folder =
        new PSCmsException(PathItemErrorCodes.FOLDER_ERROR_MSG, "Error updating folder relationships.");
    assertSame(PathItemErrorCodes.FOLDER_ERROR_MSG, folder.getTypedErrorCode());
    assertFalse(folder.isAuditable());

    PSCmsException owner = new PSCmsException(CmsErrorCodes.NON_EXITING_OWNER, new Object[] {"9"});
    assertSame(CmsErrorCodes.NON_EXITING_OWNER, owner.getTypedErrorCode());

    PSCmsException insert =
        new PSCmsException(CmsErrorCodes.INVALID_INSERT_RELATIONSHIP_TYPE, "ActiveAssembly");
    assertSame(CmsErrorCodes.INVALID_INSERT_RELATIONSHIP_TYPE, insert.getTypedErrorCode());

    PSCmsException persisted = new PSCmsException(CmsErrorCodes.PERSISTED_KEY_EXPECTED);
    assertSame(CmsErrorCodes.PERSISTED_KEY_EXPECTED, persisted.getTypedErrorCode());

    PSNotFoundException missingIr =
        new PSNotFoundException(
            ServerErrorCodes.MISSING_INTERNAL_REQUEST_RESOURCE,
            new Object[] {"sys_psxRelationshipSupport/slot", "No request handler found."});
    assertSame(ServerErrorCodes.MISSING_INTERNAL_REQUEST_RESOURCE, missingIr.getTypedErrorCode());
    assertFalse(missingIr.isAuditable());

    PSCmsException console =
        new PSCmsException(ServerErrorCodes.UNEXPECTED_EXCEPTION_CONSOLE, "assembly down");
    assertSame(ServerErrorCodes.UNEXPECTED_EXCEPTION_CONSOLE, console.getTypedErrorCode());

    PSCmsException sql = new PSCmsException(ServerErrorCodes.CE_SQL_ERRORS, "SQLSTATE");
    assertSame(ServerErrorCodes.CE_SQL_ERRORS, sql.getTypedErrorCode());

    PSExtensionProcessingException wrapper =
        new PSExtensionProcessingException(ServerErrorCodes.RAW_DUMP, "SQLException: boom");
    assertSame(ServerErrorCodes.RAW_DUMP, wrapper.getTypedErrorCode());
    assertEquals(IPSCmsErrors.SQL_EXCEPTION_WRAPPER, wrapper.getErrorCode());
    assertFalse(wrapper.isAuditable());

    PSException typeNotFound =
        new PSException(ServerWebServicesErrorCodes.WEB_SERVICE_CONTENT_TYPE_NOT_FOUND, "301");
    assertSame(
        ServerWebServicesErrorCodes.WEB_SERVICE_CONTENT_TYPE_NOT_FOUND, typeNotFound.getTypedErrorCode());
    assertFalse(typeNotFound.isAuditable());

    PSException irFailed =
        new PSException(
            ServerWebServicesErrorCodes.WEB_SERVICE_INTERNAL_REQUEST_FAILED,
            "sys_psxWebServices/login");
    assertSame(
        ServerWebServicesErrorCodes.WEB_SERVICE_INTERNAL_REQUEST_FAILED, irFailed.getTypedErrorCode());

    PSCmsException validation =
        new PSCmsException(CmsErrorCodes.VALIDATION_ERROR, new Object[] {"app/res", "bad field"});
    assertSame(CmsErrorCodes.VALIDATION_ERROR, validation.getTypedErrorCode());

    Document doc = PSXmlDocumentBuilder.createXmlDocument();
    Element root = PSXmlDocumentBuilder.createRoot(doc, "DisplayError");
    root.setAttribute("errorCount", "1");
    Element gm = doc.createElement("GenericMessage");
    gm.appendChild(doc.createTextNode("invalid"));
    root.appendChild(gm);
    PSFieldValidationException field =
        new PSFieldValidationException(
            CmsErrorCodes.VALIDATION_ERROR, new Object[] {"app/res", "invalid"}, new PSDisplayError(root), "");
    assertSame(CmsErrorCodes.VALIDATION_ERROR, field.getTypedErrorCode());
    assertEquals(CmsErrorCodes.VALIDATION_ERROR.numericCode(), field.getErrorCode());
    assertFalse(field.isAuditable());
  }

  @Test
  void typedProductionCtorsRejectNullCode() {
    assertThrows(IllegalArgumentException.class, () -> new PSCmsException((IPSErrorCode) null));
    assertThrows(
        IllegalArgumentException.class, () -> new PSCorruptDatabaseException(null, "1", "msg"));
    assertThrows(
        IllegalArgumentException.class,
        () -> new PSNotFoundException((IPSErrorCode) null, "resource"));
    assertThrows(
        IllegalArgumentException.class,
        () -> new PSExtensionProcessingException((IPSErrorCode) null, "sql"));
    assertThrows(
        IllegalArgumentException.class,
        () -> new PSParameterMismatchException((IPSErrorCode) null, new String[] {"sys_lookupkey", ""}));
  }
}
