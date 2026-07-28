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

import static org.apache.commons.lang3.Validate.notNull;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.Table;
import jakarta.persistence.TableGenerator;
import jakarta.persistence.Version;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

/**
 * This object represents a form with its fields and data. It does not contain any information about
 * rendering. A form is immutable once constructed.
 *
 * @author PaulHoward
 */
@Entity
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region = "PSFormData1")
@Table(name = "PERC_FORMS")
public class PSFormData implements IPSFormData {
  /** The form's db id */
  @SuppressWarnings("unused")
  @TableGenerator(
      name = "formId",
      table = "PERC_ID_GEN",
      pkColumnName = "GEN_KEY",
      valueColumnName = "GEN_VALUE",
      pkColumnValue = "formId",
      allocationSize = 1)
  @Id
  @GeneratedValue(strategy = GenerationType.TABLE, generator = "formId")
  @Column(name = "ID")
  private long id;

  /** The hibernate object version */
  @SuppressWarnings("unused")
  @Version
  private Integer version;

  @Basic private String name;

  @Basic private Date created;

  @SuppressWarnings("unused")
  @Column(name = "EXPORTED")
  private char isExported = 'n';

  @ElementCollection(fetch = FetchType.EAGER)
  @JoinTable(
      name = "PERC_FORM_FIELDS",
      joinColumns = @JoinColumn(name = "PARENT_FORM_ID", referencedColumnName = "ID"))
  @MapKeyColumn(name = "FIELD_NAME")
  @Column(name = "FIELD_VALUE", length = 2048)
  private Map<String, String> properties = new HashMap<>();

  /**
   * Creates a new form record with the supplied form name and field map.
   *
   * @param name The name of the form. Never <code>null</code> or empty. Max length is 50 chars.
   * @param props The fields of the form with their values. The key is the name of the field,
   *     case-sensitive. If the value is a <code>String[]</code>, then all the entries in the array
   *     are merged into a single string, using the {@link #FIELD_VALUES_SEPARATOR} as a separator.
   *     If the entry contains separators, the caller is responsible for escaping them.
   */
  public PSFormData(String name, Map<String, String[]> props) {
    notNull(name);
    notNull(props);

    this.name = name;
    created = new Date();
    for (String key : props.keySet()) {
      String value;
      String[] val = props.get(key);
      if (val == null) value = StringUtils.EMPTY;
      else if (val.length == 1) value = (String) escapeForJoin(val[0]);
      else value = convertArrayToString(val);
      this.properties.put(key, value);
    }
  }

  /**
   * @param val Assumed not <code>null</code>.
   * @return Never <code>null</code>.
   */
  private String convertArrayToString(String[] entries) {
    StringBuilder result = new StringBuilder();
    for (String s : entries) {
      if (result.length() > 0) result.append(FIELD_VALUES_SEPARATOR);
      if (s != null) result.append(escapeForJoin(s));
    }
    return result.toString();
  }

  /**
   * Escapes any {@link #FIELD_VALUES_SEPARATOR} by inserting the {@link
   * #FIELD_VALUES_SEPARATOR_ESCAPE} char before it. Any instances of the escape char are escaped in
   * the same way with the same escape char.
   *
   * @param s Assumed not <code>null</code>;
   * @return Never <code>null</code>.
   */
  private Object escapeForJoin(String s) {
    if (s.indexOf(FIELD_VALUES_SEPARATOR_ESCAPE) >= 0)
      s =
          s.replace(
              FIELD_VALUES_SEPARATOR_ESCAPE,
              FIELD_VALUES_SEPARATOR_ESCAPE + FIELD_VALUES_SEPARATOR_ESCAPE);
    if (s.indexOf(FIELD_VALUES_SEPARATOR) >= 0)
      s = s.replace(FIELD_VALUES_SEPARATOR, FIELD_VALUES_SEPARATOR_ESCAPE + FIELD_VALUES_SEPARATOR);
    return s;
  }

  /**
   * Returns the configured name of the form.
   *
   * @return the form name, never <code>null</code>.
   */
  public String getName() {
    return name;
  }

  /**
   * Returns the timestamp at which the form was submitted.
   *
   * @return the creation date, never <code>null</code>.
   */
  public Date getCreateDate() {
    return created;
  }

  /**
   * Reports whether the form has been exported. The value is either {@code 'y'} when the form
   * has been exported, or {@code 'n'} otherwise.
   *
   * @return the export flag character.
   */
  public char isExported() {
    return isExported;
  }

  /**
   * Returns the creation timestamp of the form record (identical to
   * {@link #getCreateDate()}).
   *
   * @return the creation date, never <code>null</code>.
   */
  public Date getCreated() {
    return created;
  }

  /**
   * Returns the names of every field captured by the submission.
   *
   * @return an unmodifiable view of the field name set, never <code>null</code>, may be empty.
   */
  public Set<String> getFieldNames() {
    Set<String> result = Collections.unmodifiableSet(properties.keySet());
    if (result == null) result = Collections.emptySet();
    return result;
  }

  /**
   * Returns the captured field names and their associated values.
   *
   * @return an unmodifiable view of the field map, never <code>null</code>, may be empty.
   */
  public Map<String, String> getFields() {
    return Collections.unmodifiableMap(properties);
  }

  /**
   * Default constructor required by the JPA provider. Application code should use
   * {@link #PSFormData(String, Map)} instead.
   */
  protected PSFormData() {}

  /**
   * Returns the string form of the persistent identifier assigned to this form.
   *
   * @return the form id, never <code>null</code>.
   */
  public String getId() {
    return String.valueOf(id);
  }

  /**
   * Sets the persistent identifier for this form. A <code>null</code> value resets the
   * identifier to {@code 0}.
   *
   * @param id the identifier, may be <code>null</code>.
   */
  public void setId(String id) {
    if (id == null) {
      this.id = 0;
    } else {
      this.id = Long.valueOf(id);
    }
  }
}
