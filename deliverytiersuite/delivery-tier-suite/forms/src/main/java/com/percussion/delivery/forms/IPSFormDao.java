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

import com.percussion.delivery.forms.data.IPSFormData;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Defines the persistence operations available for form submissions. Implementations are
 * responsible for storing, retrieving, and managing the lifecycle of {@link IPSFormData} records
 * independent of the underlying storage technology.
 */
public interface IPSFormDao {

  /**
   * Persists the supplied form submission. Implementations may attach the form to the current
   * persistence context and flush on transaction commit.
   *
   * @param form the form to save, never <code>null</code>.
   */
  public void save(IPSFormData form);

  /**
   * Creates a new form data object for the underlying data implementation.
   *
   * @param formname cannot be <code>null</code> or empty.
   * @param formdata cannot be <code>null</code>.
   * @return the new instance, never <code>null</code>.
   */
  public IPSFormData createFormData(String formname, Map<String, String[]> formdata);

  /**
   * Deletes the supplied form submission from the underlying store.
   *
   * @param form the form to delete, never <code>null</code>.
   */
  public void delete(IPSFormData form);

  /**
   * Counts the forms that have been marked as exported, optionally filtered by name. The form name
   * comparison is case-insensitive.
   *
   * @param name the form name to filter by, may be <code>null</code> or empty in which case forms
   *     across all names are counted.
   * @return the number of exported forms matching the supplied name, never <code>null</code>.
   */
  public long getExportedFormCount(String name);

  /**
   * Counts all forms currently stored in the system, optionally filtered by name. The form name
   * comparison is case-insensitive.
   *
   * @param name the form name to filter by, may be <code>null</code> or empty in which case all
   *     forms are counted.
   * @return the total number of forms matching the supplied name, never <code>null</code>.
   */
  public long getTotalFormCount(String name);

  /**
   * Marks each supplied form as exported. This is used to record that the form data has been
   * exported by an administrator so it can be cleaned up later.
   *
   * @param forms never <code>null</code>, may be empty. It is OK if a supplied form has already
   *     been marked.
   */
  public void markAsExported(Collection<IPSFormData> forms);

  /**
   * Removes all forms that have been previously marked as exported. If a form name is supplied only
   * exported forms matching that name are removed; otherwise all exported forms are deleted. The
   * form name comparison is case-insensitive.
   *
   * @param formName the form name to filter by, may be <code>null</code> or empty in which case
   *     every exported form is deleted.
   */
  public void deleteExportedForms(String formName);

  /**
   * Loads all forms stored under the supplied name, ordered by creation date ascending. The form
   * name comparison is case-insensitive.
   *
   * @param name the form name to look up, never <code>null</code>.
   * @return the matching forms, never <code>null</code>, may be empty.
   */
  public List<IPSFormData> findFormsByName(String name);

  /**
   * Loads every form currently stored in the system, ordered by name ascending and then by creation
   * date ascending.
   *
   * @return every stored form, never <code>null</code>, may be empty.
   */
  public List<IPSFormData> findAllForms();

  /**
   * Discovers the distinct form names in the underlying store. Form names that differ only by
   * letter case are not considered distinct. The returned list is ordered ascending by name.
   *
   * @return the distinct form names, never <code>null</code>, may be empty.
   */
  public List<String> findDistinctFormNames();
}
