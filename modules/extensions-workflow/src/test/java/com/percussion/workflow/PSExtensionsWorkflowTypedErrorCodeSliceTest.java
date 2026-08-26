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
package com.percussion.workflow;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.intsof.percussioncms.auditlog.codes.ExtensionErrorCodes;
import com.percussion.workflow.mail.IPSMailMessageContext;
import com.percussion.workflow.mail.PSJavaxMailProgram;
import com.percussion.workflow.mail.PSMailException;
import com.percussion.workflow.mail.PSSecureMailProgram;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Leftover workflow production throw sites now construct typed {@link ExtensionErrorCodes} (issue
 * #3770).
 */
@Tag("UnitTest")
class PSExtensionsWorkflowTypedErrorCodeSliceTest {

  @Test
  void javaxMailEmptyDomainThrowsTypedMailException() throws Exception {
    IPSMailMessageContext ctx = mock(IPSMailMessageContext.class);
    when(ctx.getSmtpHost()).thenReturn("localhost");
    when(ctx.getMailDomain()).thenReturn("");

    PSMailException ex =
        assertThrows(PSMailException.class, () -> new PSJavaxMailProgram().sendMessage(ctx));
    assertSame(ExtensionErrorCodes.MAIL_DOMAIN_EMPTY, ex.getTypedErrorCode());
    assertFalse(ex.isAuditable());
  }

  @Test
  void secureMailEmptySmtpHostThrowsTypedMailException() {
    IPSMailMessageContext ctx = mock(IPSMailMessageContext.class);
    when(ctx.getSmtpHost()).thenReturn("");

    PSMailException ex =
        assertThrows(PSMailException.class, () -> new PSSecureMailProgram().sendMessage(ctx));
    assertSame(ExtensionErrorCodes.SMTP_HOST_EMPTY, ex.getTypedErrorCode());
    assertFalse(ex.isAuditable());
  }

  @Test
  void roleExceptionLanguageTypedCtorIsNonAuditable() {
    PSRoleException ex = new PSRoleException("en-us", ExtensionErrorCodes.ROLEINFO_OBJ_NULL);
    assertSame(ExtensionErrorCodes.ROLEINFO_OBJ_NULL, ex.getTypedErrorCode());
    assertFalse(ex.isAuditable());
  }

  @Test
  void invalidParameterLanguageTypedCtorIsNonAuditable() {
    PSInvalidParameterTypeException ex =
        new PSInvalidParameterTypeException("en-us", ExtensionErrorCodes.EMPTY_USRNAME1);
    assertSame(ExtensionErrorCodes.EMPTY_USRNAME1, ex.getTypedErrorCode());
    assertFalse(ex.isAuditable());
  }

  @Test
  void checkInExceptionLanguageTypedCtorIsAuditableWhenCatalogSaysSo() {
    PSCheckInCheckOutException ex =
        new PSCheckInCheckOutException("en-us", ExtensionErrorCodes.CHECKOUT_NOT_ALLOWED);
    assertSame(ExtensionErrorCodes.CHECKOUT_NOT_ALLOWED, ex.getTypedErrorCode());
    assertTrue(ex.isAuditable());
  }
}
