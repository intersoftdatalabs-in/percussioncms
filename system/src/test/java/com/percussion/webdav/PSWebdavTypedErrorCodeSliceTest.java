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
package com.percussion.webdav;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

import com.intsof.percussioncms.auditlog.codes.RemoteErrorCodes;
import com.intsof.percussioncms.auditlog.codes.WebdavErrorCodes;
import com.percussion.cms.objectstore.client.PSRemoteException;
import com.percussion.error.IPSErrorCode;
import com.percussion.webdav.error.PSWebdavException;
import com.percussion.webdav.method.PSMethodFactory;
import com.percussion.webdav.objectstore.PSWebdavConfigDef;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.nio.file.Path;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Issue #3848 (parent #2616): leftover WebDAV production throw sites use typed {@link
 * WebdavErrorCodes} via IPSErrorCode-aware {@link PSWebdavException} constructors — not bare {@code
 * IPSWebdavErrors} ints. Catalog codes are non-auditable.
 */
@Tag("UnitTest")
public class PSWebdavTypedErrorCodeSliceTest {

  @Test
  public void unsupportedMethodThrowsTypedNonAuditableException() {
    HttpServletRequest req = mock(HttpServletRequest.class);
    HttpServletResponse resp = mock(HttpServletResponse.class);
    PSWebdavServlet servlet = mock(PSWebdavServlet.class);

    PSWebdavException ex =
        assertThrows(
            PSWebdavException.class,
            () -> PSMethodFactory.createMethod("TRACE", req, resp, servlet));
    assertEquals(WebdavErrorCodes.UNSUPPORTED_METHOD.numericCode(), ex.getErrorCode());
    assertSame(WebdavErrorCodes.UNSUPPORTED_METHOD, ex.getTypedErrorCode());
    assertFalse(ex.isAuditable());
    assertEquals(PSWebdavStatus.SC_METHOD_NOT_ALLOWED, ex.getStatusCode());
  }

  @Test
  public void missingConfigFileThrowsTypedNonAuditableException() {
    Path missing =
        Path.of(System.getProperty("java.io.tmpdir"), "percussion-webdav-missing-config.xml");
    PSWebdavException ex =
        assertThrows(PSWebdavException.class, () -> new PSWebdavConfigDef(missing.toFile()));
    assertEquals(WebdavErrorCodes.FILE_DOES_NOT_EXIST.numericCode(), ex.getErrorCode());
    assertSame(WebdavErrorCodes.FILE_DOES_NOT_EXIST, ex.getTypedErrorCode());
    assertFalse(ex.isAuditable());
  }

  @Test
  public void headerMissingCtorRetainsTypedCodeAndStatus() {
    PSWebdavException ex =
        new PSWebdavException(
            WebdavErrorCodes.HEADER_MISSING, "Destination", PSWebdavStatus.SC_BAD_REQUEST);
    assertEquals(WebdavErrorCodes.HEADER_MISSING.numericCode(), ex.getErrorCode());
    assertSame(WebdavErrorCodes.HEADER_MISSING, ex.getTypedErrorCode());
    assertFalse(ex.isAuditable());
    assertEquals(PSWebdavStatus.SC_BAD_REQUEST, ex.getStatusCode());
  }

  @Test
  public void typedConstructorRejectsNullCode() {
    assertThrows(IllegalArgumentException.class, () -> new PSWebdavException((IPSErrorCode) null));
  }

  @Test
  public void remoteUnexpectedErrorRetainsTypedNonAuditableCode() {
    PSRemoteException ex =
        new PSRemoteException(RemoteErrorCodes.REMOTE_UNEXPECTED_ERROR, "timeout");
    assertEquals(RemoteErrorCodes.REMOTE_UNEXPECTED_ERROR.numericCode(), ex.getErrorCode());
    assertSame(RemoteErrorCodes.REMOTE_UNEXPECTED_ERROR, ex.getTypedErrorCode());
    assertFalse(ex.isAuditable());
  }
}
