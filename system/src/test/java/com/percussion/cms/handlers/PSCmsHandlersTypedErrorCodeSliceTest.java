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
package com.percussion.cms.handlers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.intsof.percussioncms.auditlog.codes.CmsErrorCodes;
import com.intsof.percussioncms.auditlog.codes.DataErrorCodes;
import com.intsof.percussioncms.auditlog.codes.HttpErrorCodes;
import com.intsof.percussioncms.auditlog.codes.PathItemErrorCodes;
import com.intsof.percussioncms.auditlog.codes.ServerErrorCodes;
import com.percussion.cms.IPSCmsErrors;
import com.percussion.cms.PSCmsException;
import com.percussion.conn.PSServerException;
import com.percussion.data.IPSDataErrors;
import com.percussion.data.PSConversionException;
import com.percussion.data.PSDataExtractionException;
import com.percussion.data.PSInternalRequestCallException;
import com.percussion.data.PSUnsupportedConversionException;
import com.percussion.design.objectstore.PSSystemValidationException;
import com.percussion.error.IPSErrorCode;
import com.percussion.error.PSNotFoundException;
import com.percussion.security.PSAuthorizationException;
import com.percussion.server.IPSHttpErrors;
import com.percussion.server.IPSServerErrors;
import com.percussion.server.PSRequestValidationException;
import com.percussion.server.config.PSServerConfigException;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Issue #3883 (parent #2616 leftover): {@code com.percussion.cms.handlers} production sites throw
 * typed {@code *ErrorCodes} via IPSErrorCode-aware constructors — not bare {@code IPS*Errors}
 * ints. Dual-write skip is {@code isAuditable()==false} on leftover operational catalog codes.
 */
@Tag("UnitTest")
public class PSCmsHandlersTypedErrorCodeSliceTest {

  @Test
  public void leftoverOperationalCodesSkipDualWrite() {
    leftoverNonAuditable(
        new PSCmsException(CmsErrorCodes.UNKNOWN_AA_COMMAND, "insert"),
        CmsErrorCodes.UNKNOWN_AA_COMMAND);
    leftoverNonAuditable(
        new PSCmsException(CmsErrorCodes.MISSING_AA_PARAMETER, "sys_command, inputdoc"),
        CmsErrorCodes.MISSING_AA_PARAMETER);
    leftoverNonAuditable(
        new PSCmsException(CmsErrorCodes.UNEXPECTED_ERROR, "boom"), CmsErrorCodes.UNEXPECTED_ERROR);
    leftoverNonAuditable(
        new PSCmsException(CmsErrorCodes.NO_ORIGINATING_RELATIONSHIP),
        CmsErrorCodes.NO_ORIGINATING_RELATIONSHIP);
    leftoverNonAuditable(
        new PSCmsException(CmsErrorCodes.ID_GENERATOR_FAILED, "sql text"),
        CmsErrorCodes.ID_GENERATOR_FAILED);
    leftoverNonAuditable(
        new PSRequestValidationException(
            ServerErrorCodes.CE_MODIFY_INVALID_PARAM, new Object[] {"sys_contentid"}),
        ServerErrorCodes.CE_MODIFY_INVALID_PARAM);
    leftoverNonAuditable(
        new PSSystemValidationException(ServerErrorCodes.CE_SYSTEM_DEF_INVALID),
        ServerErrorCodes.CE_SYSTEM_DEF_INVALID);
    leftoverNonAuditable(
        new PSDataExtractionException(ServerErrorCodes.CE_NO_REDIRECT_URL),
        ServerErrorCodes.CE_NO_REDIRECT_URL);
    leftoverNonAuditable(
        new PSConversionException(ServerErrorCodes.FIELD_TRANSFORM_ERROR, new Object[] {"body"}),
        ServerErrorCodes.FIELD_TRANSFORM_ERROR);
    leftoverNonAuditable(
        new PSUnsupportedConversionException(DataErrorCodes.HTML_CONV_EXT_NOT_SUPPORTED, ".bin"),
        DataErrorCodes.HTML_CONV_EXT_NOT_SUPPORTED);
    leftoverNonAuditable(
        new PSInternalRequestCallException(
            DataErrorCodes.INTERNAL_REQUEST_CALL_EXCEPTION, "sys_cePreview"),
        DataErrorCodes.INTERNAL_REQUEST_CALL_EXCEPTION);
    leftoverNonAuditable(
        new PSInternalRequestCallException(
            DataErrorCodes.INTERNAL_REQUEST_CALL_EXCEPTION,
            "sys_ceModify",
            new IllegalStateException("nested")),
        DataErrorCodes.INTERNAL_REQUEST_CALL_EXCEPTION);
    leftoverNonAuditable(
        new PSNotFoundException(ServerErrorCodes.CE_INVALID_PAGEID, "7"),
        ServerErrorCodes.CE_INVALID_PAGEID);
    leftoverNonAuditable(
        new PSServerException(ServerErrorCodes.RAW_DUMP, "stack"), ServerErrorCodes.RAW_DUMP);
    leftoverNonAuditable(
        new PSServerConfigException(
            ServerErrorCodes.UNKNOWN_CLONEHANDLER_CONFIGURATION, "standard"),
        ServerErrorCodes.UNKNOWN_CLONEHANDLER_CONFIGURATION);
    leftoverNonAuditable(
        new PSCmsException(
            ServerErrorCodes.RAW_DUMP, "formatted sql"),
        ServerErrorCodes.RAW_DUMP);
  }

  @Test
  public void folderAndCommunityDenialsRetainTypedAuditableCodes() {
    PSAuthorizationException denied =
        new PSAuthorizationException(
            PathItemErrorCodes.FOLDER_PERMISSION_DENIED, new String[] {});
    assertEquals(
        PathItemErrorCodes.FOLDER_PERMISSION_DENIED.numericCode(), denied.getErrorCode());
    assertSame(PathItemErrorCodes.FOLDER_PERMISSION_DENIED, denied.getTypedErrorCode());
    assertTrue(denied.isAuditable());
    assertEquals(IPSCmsErrors.FOLDER_PERMISSION_DENIED, denied.getErrorCode());

    Object[] args = {"item-1", "1", "page", "community-2"};
    PSAuthorizationException hidden =
        new PSAuthorizationException(
            PathItemErrorCodes.CONTENT_TYPE_NOT_VISIBLE_BY_COMMUNITY, args);
    assertSame(
        PathItemErrorCodes.CONTENT_TYPE_NOT_VISIBLE_BY_COMMUNITY, hidden.getTypedErrorCode());
    assertTrue(hidden.isAuditable());
    assertEquals(IPSCmsErrors.CONTENT_TYPE_NOT_VISIBLE_BY_COMMUNITY, hidden.getErrorCode());
  }

  @Test
  public void leftoverNumericCodesMatchLegacyIpsErrors() {
    assertEquals(IPSCmsErrors.UNKNOWN_AA_COMMAND, CmsErrorCodes.UNKNOWN_AA_COMMAND.numericCode());
    assertEquals(
        IPSCmsErrors.MISSING_AA_PARAMETER, CmsErrorCodes.MISSING_AA_PARAMETER.numericCode());
    assertEquals(IPSCmsErrors.UNEXPECTED_ERROR, CmsErrorCodes.UNEXPECTED_ERROR.numericCode());
    assertEquals(
        IPSCmsErrors.NO_ORIGINATING_RELATIONSHIP,
        CmsErrorCodes.NO_ORIGINATING_RELATIONSHIP.numericCode());
    assertEquals(
        IPSCmsErrors.ID_GENERATOR_FAILED, CmsErrorCodes.ID_GENERATOR_FAILED.numericCode());
    assertEquals(
        IPSCmsErrors.SQL_EXCEPTION_WRAPPER, ServerErrorCodes.RAW_DUMP.numericCode());
    assertEquals(
        IPSCmsErrors.FOLDER_PERMISSION_DENIED,
        PathItemErrorCodes.FOLDER_PERMISSION_DENIED.numericCode());
    assertEquals(
        IPSCmsErrors.CONTENT_TYPE_NOT_VISIBLE_BY_COMMUNITY,
        PathItemErrorCodes.CONTENT_TYPE_NOT_VISIBLE_BY_COMMUNITY.numericCode());
    assertEquals(IPSServerErrors.RAW_DUMP, ServerErrorCodes.RAW_DUMP.numericCode());
    assertEquals(
        IPSServerErrors.CE_MODIFY_INVALID_PARAM,
        ServerErrorCodes.CE_MODIFY_INVALID_PARAM.numericCode());
    assertEquals(
        IPSServerErrors.CE_NO_REDIRECT_URL, ServerErrorCodes.CE_NO_REDIRECT_URL.numericCode());
    assertEquals(
        IPSServerErrors.FIELD_TRANSFORM_ERROR,
        ServerErrorCodes.FIELD_TRANSFORM_ERROR.numericCode());
    assertEquals(
        IPSServerErrors.UNKNOWN_CLONEHANDLER_CONFIGURATION,
        ServerErrorCodes.UNKNOWN_CLONEHANDLER_CONFIGURATION.numericCode());
    assertEquals(
        IPSDataErrors.INTERNAL_REQUEST_CALL_EXCEPTION,
        DataErrorCodes.INTERNAL_REQUEST_CALL_EXCEPTION.numericCode());
    assertEquals(
        IPSDataErrors.HTML_CONV_EXT_NOT_SUPPORTED,
        DataErrorCodes.HTML_CONV_EXT_NOT_SUPPORTED.numericCode());
    assertEquals(IPSHttpErrors.HTTP_NO_CONTENT, HttpErrorCodes.HTTP_NO_CONTENT.numericCode());
    assertEquals(IPSHttpErrors.HTTP_NOT_FOUND, HttpErrorCodes.HTTP_NOT_FOUND.numericCode());
    assertEquals(
        IPSHttpErrors.HTTP_INTERNAL_SERVER_ERROR,
        HttpErrorCodes.HTTP_INTERNAL_SERVER_ERROR.numericCode());
    assertFalse(HttpErrorCodes.HTTP_NOT_FOUND.isAuditable());
    assertFalse(ServerErrorCodes.RAW_DUMP.isAuditable());
  }

  @Test
  public void typedConstructorsRejectNullCode() {
    assertThrows(IllegalArgumentException.class, () -> new PSCmsException((IPSErrorCode) null));
    assertThrows(
        IllegalArgumentException.class,
        () -> new PSRequestValidationException((IPSErrorCode) null));
    assertThrows(
        IllegalArgumentException.class,
        () -> new PSSystemValidationException((IPSErrorCode) null));
    assertThrows(
        IllegalArgumentException.class, () -> new PSDataExtractionException((IPSErrorCode) null));
    assertThrows(
        IllegalArgumentException.class,
        () -> new PSUnsupportedConversionException((IPSErrorCode) null));
    assertThrows(
        IllegalArgumentException.class,
        () -> new PSInternalRequestCallException((IPSErrorCode) null));
    assertThrows(
        IllegalArgumentException.class, () -> new PSServerConfigException((IPSErrorCode) null));
    assertThrows(
        IllegalArgumentException.class, () -> new PSServerException((IPSErrorCode) null));
  }

  private static void leftoverNonAuditable(com.percussion.error.PSException ex, IPSErrorCode expected) {
    assertEquals(expected.numericCode(), ex.getErrorCode(), expected.toString());
    assertSame(expected, ex.getTypedErrorCode(), expected.toString());
    assertFalse(ex.isAuditable(), expected.toString());
    assertFalse(expected.isAuditable(), expected.toString());
  }
}
