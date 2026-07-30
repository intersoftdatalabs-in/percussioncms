/*
 * Copyright 1999-2026 Percussion Software, Inc.
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

package com.percussion.rest.sharedfields;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

/**
 * Read-only detail for one shared field group.
 *
 * <p>Does not support create/update/delete (later CD-15 write slice).
 */
@XmlRootElement(name = "SharedFieldGroupDetail")
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Shared field group detail with field catalog")
public class SharedFieldGroupDetail {

  private String name;
  private String filename;
  private List<SharedFieldSummary> fields = new ArrayList<>();
  private List<String> designGaps = new ArrayList<>();

  public SharedFieldGroupDetail() {}

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getFilename() {
    return filename;
  }

  public void setFilename(String filename) {
    this.filename = filename;
  }

  public List<SharedFieldSummary> getFields() {
    return fields;
  }

  public void setFields(List<SharedFieldSummary> fields) {
    this.fields = fields != null ? fields : new ArrayList<>();
  }

  public List<String> getDesignGaps() {
    return designGaps;
  }

  public void setDesignGaps(List<String> designGaps) {
    this.designGaps = designGaps != null ? designGaps : new ArrayList<>();
  }
}
