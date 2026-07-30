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
package com.percussion.delivery.email.data;

/**
 * Defines the contract for an email request payload used by the delivery tier email services.
 *
 * <p>Implementations carry the sender subject, body and recipient lists required to dispatch an
 * email message.
 */
public interface IPSEmailRequest {
  /**
   * Sets the comma-separated list of primary recipient (To) email addresses.
   *
   * @param toList the list of To addresses; may be <code>null</code> or empty.
   */
  public void setToList(String toList);

  /**
   * Sets the comma-separated list of carbon copy (CC) recipient email addresses.
   *
   * @param ccList the list of CC addresses; may be <code>null</code> or empty.
   */
  public void setCCList(String ccList);

  /**
   * Sets the comma-separated list of blind carbon copy (BCC) recipient email addresses.
   *
   * @param bccList the list of BCC addresses; may be <code>null</code> or empty.
   */
  public void setBCCList(String bccList);

  /**
   * Sets the body content of the email.
   *
   * @param bodycontent the body text; may be <code>null</code> or empty.
   */
  public void setBody(String bodycontent);

  /**
   * Sets the subject line of the email.
   *
   * @param subject the subject text; may be <code>null</code> or empty.
   */
  public void setSubject(String subject);

  /**
   * Returns the comma-separated list of primary recipient (To) email addresses.
   *
   * @return the To address list, never <code>null</code>; may be empty.
   */
  public String getToList();

  /**
   * Returns the comma-separated list of carbon copy (CC) recipient email addresses.
   *
   * @return the CC address list, never <code>null</code>; may be empty.
   */
  public String getCCList();

  /**
   * Returns the comma-separated list of blind carbon copy (BCC) recipient email addresses.
   *
   * @return the BCC address list, never <code>null</code>; may be empty.
   */
  public String getBCCList();

  /**
   * Returns the body content of the email.
   *
   * @return the body text, never <code>null</code>; may be empty.
   */
  public String getBody();

  /**
   * Returns the subject line of the email.
   *
   * @return the subject text, never <code>null</code>; may be empty.
   */
  public String getSubject();
}
