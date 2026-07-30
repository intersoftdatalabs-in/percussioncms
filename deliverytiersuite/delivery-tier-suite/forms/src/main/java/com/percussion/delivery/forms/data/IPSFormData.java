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

package com.percussion.delivery.forms.data;

import java.util.Date;
import java.util.Map;
import java.util.Set;

/**
 * Represents a single submitted form along with its field names and values. Implementations are
 * normally backed by a JPA entity and follow the contract documented by each accessor.
 *
 * @author leonardohildt
 */
public interface IPSFormData {

  /** Separator used to join multiple values for the same form field. */
  public static final String FIELD_VALUES_SEPARATOR = "|";

  /** Escape character used to allow the separator to appear inside field values. */
  public static final String FIELD_VALUES_SEPARATOR_ESCAPE = "\\";

  /**
   * Returns the configured name of the form. Form names are case-sensitive.
   *
   * @return the form name, never <code>null</code>.
   */
  public String getName();

  /**
   * Returns the timestamp at which the form was submitted.
   *
   * @return the creation date, never <code>null</code>.
   */
  public Date getCreateDate();

  /**
   * Reports whether this form has been exported by an administrator. The value is either {@code
   * 'y'} when the form has been exported or {@code 'n'} otherwise.
   *
   * @return the export flag character, either {@code 'y'} or {@code 'n'}.
   */
  public char isExported();

  /**
   * Returns the timestamp at which the form record was created. Identical to {@link
   * #getCreateDate()}.
   *
   * @return the creation date, never <code>null</code>.
   */
  public Date getCreated();

  /**
   * Retrieve all the field names in this form.
   *
   * @return An unmodifiable collection. Never <code>null</code>, may be empty.
   */
  public Set<String> getFieldNames();

  /**
   * Retrieve the fields and their values.
   *
   * @return An unmodifiable map. Never <code>null</code>, may be empty.
   */
  public Map<String, String> getFields();

  /**
   * Returns the string form of the persistent identifier for this form.
   *
   * @return the form id, never <code>null</code>.
   */
  public String getId();

  /**
   * Sets the persistent identifier for this form. A <code>null</code> value resets the identifier
   * to {@code 0}, allowing the entity to be treated as newly created.
   *
   * @param id the identifier, may be <code>null</code>.
   */
  public void setId(String id);
}
