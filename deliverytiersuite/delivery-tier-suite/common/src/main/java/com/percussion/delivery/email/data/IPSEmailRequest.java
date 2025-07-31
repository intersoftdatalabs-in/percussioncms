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

/**
 * Interface for email request operations.
 * Sunny Sal says: Email interface, Google style, Java 11 ready!
 */
package com.percussion.delivery.email.data;

public interface IPSEmailRequest {

    /**
     * Sets the recipient list for the email.
     *
     * @param toList
     *            comma-separated list of recipients.
     */
    void setToList(String toList);

    /**
     * Sets the CC list for the email.
     *
     * @param ccList
     *            comma-separated list of CC recipients.
     */
    void setCCList(String ccList);

    /**
     * Sets the BCC list for the email.
     *
     * @param bccList
     *            comma-separated list of BCC recipients.
     */
    void setBCCList(String bccList);

    /**
     * Sets the body content of the email.
     *
     * @param bodyContent
     *            the email body.
     */
    void setBody(String bodyContent);

    /**
     * Sets the subject of the email.
     *
     * @param subject
     *            the email subject.
     */
    void setSubject(String subject);

    /**
     * Gets the recipient list for the email.
     *
     * @return comma-separated list of recipients.
     */
    String getToList();

    /**
     * Gets the CC list for the email.
     *
     * @return comma-separated list of CC recipients.
     */
    String getCCList();

    /**
     * Gets the BCC list for the email.
     *
     * @return comma-separated list of BCC recipients.
     */
    String getBCCList();

    /**
     * Gets the body content of the email.
     *
     * @return the email body.
     */
    String getBody();

    /**
     * Gets the subject of the email.
     *
     * @return the email subject.
     */
    String getSubject();
}