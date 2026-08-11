// REFACTORED: CP-JAVA11
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
package com.percussion.widgetbuilder.data;

import com.fasterxml.jackson.annotation.JsonRootName;
import com.percussion.share.dao.PSSerializerUtils;
import com.percussion.share.data.PSAbstractDataObject;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Container object for the list of widget fields. */
@XmlRootElement(name = "WidgetBuilderFieldsListData")
@JsonRootName("WidgetBuilderFieldsListData")
public class PSWidgetBuilderFieldsListData extends PSAbstractDataObject {

  private static final long serialVersionUID = 1L;

  private ArrayList<PSWidgetBuilderFieldData> fields = new ArrayList<>();

  public PSWidgetBuilderFieldsListData() {
    // Default constructor
  }

  public static PSWidgetBuilderFieldsListData fromXml(String fieldXml) {
    return PSSerializerUtils.unmarshal(fieldXml, PSWidgetBuilderFieldsListData.class);
  }

  public String toXml() {
    return PSSerializerUtils.marshal(this);
  }

  /**
   * Get the list of fields.
   *
   * @return The list of fields, never {@code null}, may be empty.
   */
  public List<PSWidgetBuilderFieldData> getFields() {
    return fields;
  }

  /**
   * Replace the list of fields.
   *
   * @param fields The fields, not {@code null}, may be empty.
   */
  public void setFields(List<PSWidgetBuilderFieldData> fields) {
    Objects.requireNonNull(fields, "fields must not be null");
    if (fields == null) {
      this.fields = null;
    } else if (fields instanceof ArrayList) {
      this.fields = (ArrayList) fields;
    } else {
      this.fields = new ArrayList<>(fields);
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof PSWidgetBuilderFieldsListData)) return false;
    var that = (PSWidgetBuilderFieldsListData) o;
    return Objects.equals(getFields(), that.getFields());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getFields());
  }

  @Override
  public String toString() {
    return "PSWidgetBuilderFieldsListData{" + "fields=" + fields + '}';
  }
}
