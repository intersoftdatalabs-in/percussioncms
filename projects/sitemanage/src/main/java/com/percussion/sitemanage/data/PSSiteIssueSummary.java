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
package com.percussion.sitemanage.data;

import com.fasterxml.jackson.annotation.JsonRootName;
import com.percussion.share.data.PSAbstractDataObject;
import java.util.Optional;
import jakarta.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "issues")
@JsonRootName("issues")
public class PSSiteIssueSummary extends PSAbstractDataObject {

  private static final long serialVersionUID = 1L;

  private String refUri;
  private String suggestion;
  private String type;
  private PSSiteIssueResource resource;

  public Optional<String> getRefUri() {
    return Optional.ofNullable(refUri);
  }

  public void setRefUri(String refUri) {
    this.refUri = refUri;
  }

  public Optional<String> getSuggestion() {
    return Optional.ofNullable(suggestion);
  }

  public void setSuggestion(String suggestion) {
    this.suggestion = suggestion;
  }

  public Optional<String> getType() {
    return Optional.ofNullable(type);
  }

  public void setType(String type) {
    this.type = type;
  }

  public Optional<PSSiteIssueResource> getResource() {
    return Optional.ofNullable(resource);
  }

  public void setResource(PSSiteIssueResource resource) {
    this.resource = resource;
  }
}
