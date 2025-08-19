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

import com.percussion.share.data.PSAbstractDataObject;
import java.util.Objects;
import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * Structure for doc type options for templates. Contains an option name and its value.
 *
 * @author leonardohildt
 */
@XmlRootElement(name = "Options")
public class PSMetadataDocTypeOptions extends PSAbstractDataObject {
  private static final long serialVersionUID = 1L;

  private String option;
  private String value;

  /** Default constructor. */
  public PSMetadataDocTypeOptions() {
    super();
  }

  /**
   * Constructs with option and value.
   *
   * @param option the option name
   * @param value the value
   */
  public PSMetadataDocTypeOptions(String option, String value) {
    this.option = option;
    this.value = value;
  }

  /**
   * Gets the option name.
   *
   * @return the option name
   */
  public String getOption() {
    return option;
  }

  /**
   * Sets the option name.
   *
   * @param option the option name
   */
  public void setOption(String option) {
    this.option = option;
  }

  /**
   * Gets the value.
   *
   * @return the value
   */
  public String getValue() {
    return value;
  }

  /**
   * Sets the value.
   *
   * @param value the value
   */
  public void setValue(String value) {
    this.value = value;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof PSMetadataDocTypeOptions)) return false;
    var that = (PSMetadataDocTypeOptions) o;
    return Objects.equals(getOption(), that.getOption())
        && Objects.equals(getValue(), that.getValue());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getOption(), getValue());
  }
}
