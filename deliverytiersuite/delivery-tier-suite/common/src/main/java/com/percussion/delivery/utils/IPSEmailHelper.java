/*
 * Copyright 1999-2023 Percussion Software, Inc.
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
package com.percussion.delivery.utils;

import com.percussion.delivery.email.data.IPSEmailRequest;
import com.percussion.delivery.exceptions.PSEmailException;
import org.apache.commons.mail.EmailException;

/** A common helper class to send emails. */
public interface IPSEmailHelper {
  /**
   * Sends an email with the details provided in {@link IPSEmailRequest}.
   *
   * @param emailRequest The request object that has the details of the email, must not be <code>
   *     null</code>.
   * @return The message id of the email sent.
   * @throws PSEmailException When there is an error while sending the email.
   * @throws PSEmailServiceNotInitializedException When there is an error while initializing the
   *     email client.
   */
  public String sendMail(IPSEmailRequest emailRequest)
      throws PSEmailServiceNotInitializedException, PSEmailException;

  /** Property key for the SMTP host name. */
  public static final String EMAIL_PROPS_HOSTNAME = "email.hostName";

  /** Property key for the SMTP port. */
  public static final String EMAIL_PROPS_PORT = "email.portNumber";

  /** Property key for the SMTP user name. */
  public static final String EMAIL_PROPS_SMTP_USERNAME = "email.userName";

  /** Property key for the SMTP user password. */
  public static final String EMAIL_PROPS_SMTP_PASSWORD = "email.password";

  /** Property key for the From address. */
  public static final String EMAIL_PROPS_FROM_ADDRESS = "email.fromAddress";

  /** Property key for the bounce address. */
  public static final String EMAIL_PROPS_BOUNCE_ADDRESS = "email.bounceAddress";

  /** Property key for the {@code TLS} flag. */
  public static final String EMAIL_PROPS_TLS = "email.TLS";

  /** Property key for the From display name. */
  public static final String EMAIL_PROPS_FROMNAME = "email.fromName";

  /** Property key for the SSL/TLS port. */
  public static final String EMAIL_PROPS_SSLPORT = "email.sslPort";
}
