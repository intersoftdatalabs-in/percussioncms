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

/**
 * A common helper class to send emails.
 * // REFACTORED: CP-JAVA11
 */
public interface IPSEmailHelper {
    /**
     * Sends an email with the details provided in {@link com.percussion.delivery.email.data.IPSEmailRequest}
     *
     * @param emailRequest The request object that has the details of the email, must not be null.
     * @return The message id of the email sent.
     * @throws com.percussion.delivery.utils.PSEmailServiceNotInitializedException When there is an error initializing the email client.
     * @throws com.percussion.delivery.exceptions.PSEmailException on email send failure.
     */
    String sendMail(com.percussion.delivery.email.data.IPSEmailRequest emailRequest)
            throws PSEmailServiceNotInitializedException, com.percussion.delivery.exceptions.PSEmailException;

    String EMAIL_PROPS_HOSTNAME = "email.hostName";
    String EMAIL_PROPS_PORT = "email.portNumber";
    String EMAIL_PROPS_SMTP_USERNAME = "email.userName";
    String EMAIL_PROPS_SMTP_PASSWORD = "email.password";
    String EMAIL_PROPS_FROM_ADDRESS = "email.fromAddress";
    String EMAIL_PROPS_BOUNCE_ADDRESS = "email.bounceAddress";
    String EMAIL_PROPS_TLS = "email.TLS";
    String EMAIL_PROPS_FROMNAME = "email.fromName";
    String EMAIL_PROPS_SSLPORT = "email.sslPort";
}
