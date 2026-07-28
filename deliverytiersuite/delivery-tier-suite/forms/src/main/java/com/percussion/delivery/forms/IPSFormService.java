/*
 * Copyright 1999-2025 Percussion Software, Inc.
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

package com.percussion.delivery.forms;

import com.percussion.delivery.exceptions.PSEmailException;
import com.percussion.delivery.forms.data.IPSFormData;
import com.percussion.delivery.forms.impl.PSRecaptchaService;
import com.percussion.delivery.utils.PSEmailServiceNotInitializedException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Provides the high-level form submission lifecycle used by the delivery-tier forms service:
 * persisting submissions, exposing counts and lookup queries, and dispatching notification
 * emails. Implementations are typically backed by a JPA {@link IPSFormDao} and an email
 * helper.
 */
public interface IPSFormService {
  /**
   * Persists a single form submission. Implementations must validate the supplied form and
   * delegate the actual storage to the configured DAO.
   *
   * @param formdata the submission to persist, never <code>null</code>.
   */
  public void save(IPSFormData formdata);

  /**
   * Removes a form submission from the underlying store.
   *
   * @param form the submission to delete, never <code>null</code>.
   */
  public void delete(IPSFormData form);

  /**
   * Returns the recaptcha service used to validate end-user submissions for spam. May return
   * <code>null</code> when recaptcha integration is not configured.
   *
   * @return the recaptcha service in use, may be <code>null</code>.
   */
  public PSRecaptchaService getRecaptchaService();

  /**
   * Replaces the recaptcha service used to validate end-user submissions for spam.
   *
   * @param recaptchaService the new recaptcha service, may be <code>null</code>.
   */
  public void setRecaptchaService(PSRecaptchaService recaptchaService);

  /**
   * Creates a new form data object for the underlying data implementation.
   *
   * @param formname cannot be <code>null</code> or empty.
   * @param formdata cannot be <code>null</code>.
   * @return the new instance, never <code>null</code>.
   */
  public IPSFormData createFormData(String formname, Map<String, String[]> formdata);

  /**
   * Permanently deletes forms that have been marked as <code>exported</code>. If the supplied
   * form name is <code>null</code> or empty every exported form is removed; otherwise only
   * exported forms whose name matches the supplied value are deleted. The form name
   * comparison is case-insensitive.
   *
   * @param formName the form name to filter by, may be <code>null</code> or empty.
   */
  public void deleteExportedForms(String formName);

  /**
   * Loads every form stored under the supplied name, ordered by created date ascending. The
   * form name comparison is case-insensitive.
   *
   * @param name the form name to look up, never <code>null</code>.
   * @return the matching forms, never <code>null</code>, may be empty. Ordered by created date
   *     ascending. The form name comparison is case-insensitive.
   */
  public List<IPSFormData> findFormsByName(String name);

  /**
   * Loads every form currently stored in the system, ordered by name first ascending and then
   * by created date ascending.
   *
   * @return every stored form, never <code>null</code>, may be empty.
   */
  public List<IPSFormData> findAllForms();

  /**
   * Discovers the distinct form names in the underlying store. Names that differ only by
   * letter case are not considered distinct. The returned list is ordered ascending by name.
   *
   * @return the distinct form names, never <code>null</code>, may be empty. Ordered by created
   *     date ascending.
   */
  public List<String> findDistinctFormNames();

  /**
   * Sets a flag on each supplied form that is used by other methods in this interface. The purpose
   * of this is to allow the user to return form data, then clear the forms at a later time.
   *
   * @param forms Never <code>null</code>, may be empty. It is OK if a supplied form has already
   *     been marked.
   */
  public void markAsExported(Collection<IPSFormData> forms);

  /**
   * Counts the forms that have been marked as exported, optionally filtered by name. The form
   * name comparison is case-insensitive.
   *
   * @param name the form name, may be <code>null</code> or empty in which case all forms will be
   *     counted. The form name comparison is case-insensitive.
   * @return A count of forms that have been {@link #markAsExported(Collection)}.
   */
  public long getExportedFormCount(String name);

  /**
   * Counts all forms currently stored in the system, optionally filtered by name. The form
   * name comparison is case-insensitive.
   *
   * @param name the form name, may be <code>null</code> or empty in which case all forms will be
   *     counted. The form name comparison is case-insensitive.
   * @return A count of all forms currently in the system.
   */
  public long getTotalFormCount(String name);

  /**
   * Emails the supplied form data using the supplied info.
   *
   * @param toList A comma-delimited list of recipient email addresses, not <code>null</code> or empty.
   * @param subject The subject line to use, not <code>null</code> or empty.
   * @param formData The form data to include in the body of the email, not <code>null</code>.
   *
   * @throws PSEmailServiceNotInitializedException if the email service is not properly configured
   * @throws PSEmailException If there are any errors sending the email
   */
  public void emailFormData(String toList, String subject, IPSFormData formData)
      throws PSEmailServiceNotInitializedException, PSEmailException;

  /**
   * Check if form name is a valid Form
   *
   * @param formName the name of the form
   * @return true if valid
   */
  public boolean isValidFormName(String formName);
}
