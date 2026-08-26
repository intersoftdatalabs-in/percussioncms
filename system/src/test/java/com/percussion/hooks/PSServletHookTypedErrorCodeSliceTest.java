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
package com.percussion.hooks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.intsof.percussioncms.auditlog.codes.ServletErrorCodes;
import com.percussion.error.IPSErrorCode;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Issue #3848 (parent #2616): leftover servlet-hook production call sites use typed {@link
 * ServletErrorCodes} (not bare {@code IPSServletErrors} ints). Catalog codes are non-auditable.
 */
@Tag("UnitTest")
public class PSServletHookTypedErrorCodeSliceTest {

  @Test
  public void invalidPortFormatMessageUsesTypedCatalogCode() {
    String msg =
        PSConnectionFactory.formatMessage(
            ServletErrorCodes.INVALID_PORT_NUMBER, new Object[] {"not-a-port"});
    assertTrue(msg.contains("not-a-port"));
    assertEquals(10152, ServletErrorCodes.INVALID_PORT_NUMBER.numericCode());
    assertFalse(ServletErrorCodes.INVALID_PORT_NUMBER.isAuditable());
  }

  @Test
  public void connectionFailureFormatMessageUsesTypedCatalogCode() {
    String msg = PSConnectionFactory.formatMessage(ServletErrorCodes.CONNECTION_FAILURE, null);
    assertFalse(msg.isBlank());
    assertEquals(10155, ServletErrorCodes.CONNECTION_FAILURE.numericCode());
    assertFalse(ServletErrorCodes.CONNECTION_FAILURE.isAuditable());
  }

  @Test
  public void typedServletExceptionRetainsNonAuditableCode() {
    Object[] args = {"9999x"};
    PSServletException ex =
        new PSServletException(ServletErrorCodes.INVALID_PORT_NUMBER, args);
    assertEquals(ServletErrorCodes.INVALID_PORT_NUMBER.numericCode(), ex.getErrorCode());
    assertSame(ServletErrorCodes.INVALID_PORT_NUMBER, ex.getTypedErrorCode());
    assertFalse(ex.isAuditable());
  }

  @Test
  public void typedServletExceptionNoArgCtorRetainsCode() {
    PSServletException ex = new PSServletException(ServletErrorCodes.SERVLET_DESTROYED);
    assertEquals(ServletErrorCodes.SERVLET_DESTROYED.numericCode(), ex.getErrorCode());
    assertSame(ServletErrorCodes.SERVLET_DESTROYED, ex.getTypedErrorCode());
    assertFalse(ex.isAuditable());
  }

  @Test
  public void typedConstructorsRejectNullCode() {
    assertThrows(
        IllegalArgumentException.class, () -> new PSServletException((IPSErrorCode) null));
    assertThrows(
        IllegalArgumentException.class,
        () -> PSConnectionFactory.formatMessage((IPSErrorCode) null, null));
  }
}
