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
package com.percussion.mail;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.intsof.percussioncms.auditlog.codes.MailErrorCodes;
import com.percussion.error.IPSErrorCode;
import com.percussion.error.PSIllegalArgumentException;
import java.util.Properties;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Issue #4017 (parent #2616): leftover {@code com.percussion.mail} production sites throw typed
 * {@link MailErrorCodes} (not bare {@code IPSMailErrors} ints). Dual-write is skipped because the
 * mail catalog is non-auditable.
 */
@Tag("UnitTest")
class PSMailLeftoverErrorCodesSliceTest {

  @Test
  void leftoverCatalogMatchesLegacyIntsAndSkipsDualWrite() {
    assertEquals(IPSMailErrors.MAIL_ADDRESS_EMPTY, MailErrorCodes.MAIL_ADDRESS_EMPTY.numericCode());
    assertEquals(
        IPSMailErrors.MAIL_ADDRESS_INVALID, MailErrorCodes.MAIL_ADDRESS_INVALID.numericCode());
    assertEquals(
        IPSMailErrors.MAIL_CUSTOM_TO_HEADER_INVALID,
        MailErrorCodes.MAIL_CUSTOM_TO_HEADER_INVALID.numericCode());
    assertEquals(
        IPSMailErrors.MAIL_CUSTOM_TO_HEADER_EMPTY,
        MailErrorCodes.MAIL_CUSTOM_TO_HEADER_EMPTY.numericCode());
    assertEquals(
        IPSMailErrors.MAIL_SEND_UNEXPECTED_EXCEPTION,
        MailErrorCodes.MAIL_SEND_UNEXPECTED_EXCEPTION.numericCode());
    assertEquals(
        IPSMailErrors.MAIL_SERVER_UP_EXCEPTION,
        MailErrorCodes.MAIL_SERVER_UP_EXCEPTION.numericCode());
    assertEquals(
        IPSMailErrors.MAIL_SERVER_CONNECTION_ERROR,
        MailErrorCodes.MAIL_SERVER_CONNECTION_ERROR.numericCode());
    assertEquals(IPSMailErrors.HOST_NOT_VALID, MailErrorCodes.HOST_NOT_VALID.numericCode());

    for (MailErrorCodes code : MailErrorCodes.values()) {
      assertFalse(code.isAuditable(), code.toString());
    }
  }

  @Test
  void mailMessageRejectsEmptyAndInvalidAddressesWithTypedIllegalArgument() throws Exception {
    PSMailMessage msg = new PSMailMessage();

    PSIllegalArgumentException empty =
        assertThrows(PSIllegalArgumentException.class, () -> msg.addSendTo(""));
    assertSame(MailErrorCodes.MAIL_ADDRESS_EMPTY, empty.getTypedErrorCode());
    assertEquals(MailErrorCodes.MAIL_ADDRESS_EMPTY.numericCode(), empty.getErrorCode());
    assertFalse(empty.isAuditable());

    PSIllegalArgumentException invalid =
        assertThrows(PSIllegalArgumentException.class, () -> msg.addCopyTo("not-an-address"));
    assertSame(MailErrorCodes.MAIL_ADDRESS_INVALID, invalid.getTypedErrorCode());
    assertEquals(MailErrorCodes.MAIL_ADDRESS_INVALID.numericCode(), invalid.getErrorCode());
    assertFalse(invalid.isAuditable());

    PSIllegalArgumentException fromEmpty =
        assertThrows(PSIllegalArgumentException.class, () -> msg.setFrom(null));
    assertSame(MailErrorCodes.MAIL_ADDRESS_EMPTY, fromEmpty.getTypedErrorCode());
    assertFalse(fromEmpty.isAuditable());
  }

  @Test
  void mailMessageRejectsEmptyAndInvalidCustomToHeaderWithTypedIllegalArgument() throws Exception {
    PSMailMessage msg = new PSMailMessage();

    PSIllegalArgumentException empty =
        assertThrows(PSIllegalArgumentException.class, () -> msg.setToHeaderString(""));
    assertSame(MailErrorCodes.MAIL_CUSTOM_TO_HEADER_EMPTY, empty.getTypedErrorCode());
    assertEquals(MailErrorCodes.MAIL_CUSTOM_TO_HEADER_EMPTY.numericCode(), empty.getErrorCode());
    assertFalse(empty.isAuditable());

    PSIllegalArgumentException invalid =
        assertThrows(PSIllegalArgumentException.class, () -> msg.setToHeaderString("All\nEmployees"));
    assertSame(MailErrorCodes.MAIL_CUSTOM_TO_HEADER_INVALID, invalid.getTypedErrorCode());
    assertEquals(
        MailErrorCodes.MAIL_CUSTOM_TO_HEADER_INVALID.numericCode(), invalid.getErrorCode());
    assertFalse(invalid.isAuditable());
  }

  @Test
  void mailProviderGetNamePartsThrowsTypedIllegalArgument() {
    PSIllegalArgumentException ex =
        assertThrows(PSIllegalArgumentException.class, () -> PSMailProvider.getNameParts("user@"));
    assertSame(MailErrorCodes.MAIL_ADDRESS_INVALID, ex.getTypedErrorCode());
    assertEquals(MailErrorCodes.MAIL_ADDRESS_INVALID.numericCode(), ex.getErrorCode());
    assertFalse(ex.isAuditable());

    PSMailSendException wrapped =
        new PSMailSendException(ex.getTypedErrorCode(), ex.getErrorArguments());
    assertSame(MailErrorCodes.MAIL_ADDRESS_INVALID, wrapped.getTypedErrorCode());
    assertFalse(wrapped.isAuditable());
  }

  @Test
  void smtpProviderMissingHostThrowsTypedIllegalArgument() {
    Properties empty = new Properties();
    PSIllegalArgumentException ctor =
        assertThrows(PSIllegalArgumentException.class, () -> new PSSmtpMailProvider(empty));
    assertSame(MailErrorCodes.HOST_NOT_VALID, ctor.getTypedErrorCode());
    assertEquals(MailErrorCodes.HOST_NOT_VALID.numericCode(), ctor.getErrorCode());
    assertFalse(ctor.isAuditable());

    PSSmtpMailProvider provider = new PSSmtpMailProvider();
    PSIllegalArgumentException setProps =
        assertThrows(PSIllegalArgumentException.class, () -> provider.setProperties(empty));
    assertSame(MailErrorCodes.HOST_NOT_VALID, setProps.getTypedErrorCode());
    assertFalse(setProps.isAuditable());
  }

  @Test
  void smtpSendWithoutHostThrowsTypedMailSendException() throws Exception {
    PSSmtpMailProvider provider = new PSSmtpMailProvider();
    PSMailMessage msg = new PSMailMessage();
    msg.addSendTo("user@example.com");

    PSMailSendException ex = assertThrows(PSMailSendException.class, () -> provider.send(msg));
    assertSame(MailErrorCodes.HOST_NOT_VALID, ex.getTypedErrorCode());
    assertEquals(MailErrorCodes.HOST_NOT_VALID.numericCode(), ex.getErrorCode());
    assertFalse(ex.isAuditable());
  }

  @Test
  void smtpSendWithoutRecipientsThrowsTypedMailSendException() throws Exception {
    Properties props = new Properties();
    props.setProperty(PSSmtpMailProvider.PROPERTY_HOST, "smtp.example.com");
    PSSmtpMailProvider provider = new PSSmtpMailProvider(props);

    PSMailSendException ex =
        assertThrows(PSMailSendException.class, () -> provider.send(new PSMailMessage()));
    assertSame(MailErrorCodes.MAIL_ADDRESS_EMPTY, ex.getTypedErrorCode());
    assertEquals(MailErrorCodes.MAIL_ADDRESS_EMPTY.numericCode(), ex.getErrorCode());
    assertFalse(ex.isAuditable());
  }

  @Test
  void smtpSendWithInvalidRecipientThrowsTypedMailSendException() throws Exception {
    Properties props = new Properties();
    props.setProperty(PSSmtpMailProvider.PROPERTY_HOST, "smtp.example.com");
    PSSmtpMailProvider provider = new PSSmtpMailProvider(props);
    PSMailMessage msg =
        new PSMailMessage() {
          @Override
          public String[] getRecipients() {
            return new String[] {"not-an-address"};
          }
        };

    PSMailSendException ex = assertThrows(PSMailSendException.class, () -> provider.send(msg));
    assertSame(MailErrorCodes.MAIL_ADDRESS_INVALID, ex.getTypedErrorCode());
    assertEquals(MailErrorCodes.MAIL_ADDRESS_INVALID.numericCode(), ex.getErrorCode());
    assertFalse(ex.isAuditable());
  }

  @Test
  void mailSendExceptionTypedCtorsRetainNonAuditableCodes() {
    PSMailSendException noArgs = new PSMailSendException(MailErrorCodes.MAIL_ADDRESS_EMPTY);
    assertSame(MailErrorCodes.MAIL_ADDRESS_EMPTY, noArgs.getTypedErrorCode());
    assertEquals(MailErrorCodes.MAIL_ADDRESS_EMPTY.numericCode(), noArgs.getErrorCode());
    assertFalse(noArgs.isAuditable());

    PSMailSendException single =
        new PSMailSendException(MailErrorCodes.MAIL_ADDRESS_INVALID, "bad@");
    assertSame(MailErrorCodes.MAIL_ADDRESS_INVALID, single.getTypedErrorCode());
    assertFalse(single.isAuditable());

    Object[] args = {"socket failed"};
    PSMailSendException array =
        new PSMailSendException(MailErrorCodes.MAIL_SERVER_CONNECTION_ERROR, args);
    assertSame(MailErrorCodes.MAIL_SERVER_CONNECTION_ERROR, array.getTypedErrorCode());
    assertFalse(array.isAuditable());

    PSMailSendException wrapped = new PSMailSendException(new RuntimeException("boom"));
    assertSame(MailErrorCodes.MAIL_SEND_UNEXPECTED_EXCEPTION, wrapped.getTypedErrorCode());
    assertEquals(
        MailErrorCodes.MAIL_SEND_UNEXPECTED_EXCEPTION.numericCode(), wrapped.getErrorCode());
    assertFalse(wrapped.isAuditable());

    PSMailSendException serverUp =
        new PSMailSendException(MailErrorCodes.MAIL_SERVER_UP_EXCEPTION, "421 busy");
    assertSame(MailErrorCodes.MAIL_SERVER_UP_EXCEPTION, serverUp.getTypedErrorCode());
    assertFalse(serverUp.isAuditable());
  }

  @Test
  void typedMailSendExceptionRejectsNullCode() {
    assertThrows(IllegalArgumentException.class, () -> new PSMailSendException((IPSErrorCode) null));
    assertThrows(
        IllegalArgumentException.class,
        () -> new PSMailSendException((IPSErrorCode) null, "arg"));
    assertThrows(
        IllegalArgumentException.class,
        () -> new PSMailSendException((IPSErrorCode) null, new Object[] {"arg"}));
  }
}
