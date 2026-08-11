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
package com.percussion.pagemanagement.data;

import com.fasterxml.jackson.annotation.JsonRootName;
import com.percussion.share.data.PSAbstractDataObject;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Structure for doc type information for templates. Contains a selected doc type and a list of
 * {@link PSMetadataDocTypeOptions}. Default doc type is HTML5 for new templates.
 *
 * @author leonardohildt
 */
@XmlRootElement(name = "DocType")
@JsonRootName("DocType")
public class PSMetadataDocType extends PSAbstractDataObject {
  private static final long serialVersionUID = 1L;

  private String selected = "";
  private ArrayList<PSMetadataDocTypeOptions> options = new ArrayList<>();

  /** Default constructor, sets default doc type to HTML5. */
  public PSMetadataDocType() {
    super();
  }

  /**
   * Sets the selected doc type.
   *
   * @param selected the selected doc type
   */
  public void setSelected(String selected) {
    this.selected = selected;
  }

  /**
   * Gets the selected doc type.
   *
   * @return the selected doc type
   */
  public String getSelected() {
    return selected;
  }

  /**
   * Gets the doc type options, never {@code null}.
   *
   * @return the doc type options
   */
  public List<PSMetadataDocTypeOptions> getOptions() {
    return options;
  }

  /**
   * Sets the doc type options, never {@code null}.
   *
   * @param options the doc type options
   */
  @SuppressWarnings("unchecked")
  public void setOptions(List<PSMetadataDocTypeOptions> options) {
    if (options == null) {
      this.options = null;
    } else if (options instanceof ArrayList) {
      this.options = (ArrayList<PSMetadataDocTypeOptions>) options;
    } else {
      this.options = new ArrayList<>(options);
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof PSMetadataDocType)) return false;
    var that = (PSMetadataDocType) o;
    return Objects.equals(getSelected(), that.getSelected())
        && Objects.equals(getOptions(), that.getOptions());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getSelected(), getOptions());
  }
}
