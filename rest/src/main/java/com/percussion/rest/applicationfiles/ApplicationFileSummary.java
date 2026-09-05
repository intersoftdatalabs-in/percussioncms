/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
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

package com.percussion.rest.applicationfiles;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonRootName;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

/** Application CMS/resource file catalog entry (SY-05). */
@XmlRootElement(name = "ApplicationFile")
@JsonRootName("ApplicationFile")
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "XML application CMS/resource file under an application root")
public class ApplicationFileSummary {

  /** Catalog application name (trusted object-store name). */
  private String applicationName;

  /**
   * Relative path under the application root using {@code /} separators (never an absolute
   * filesystem path).
   */
  private String path;

  /** Final path segment (file or folder name). */
  private String name;

  private Boolean directory;

  /** File text when loaded for detail / PUT body; omitted on list. */
  private String content;

  private String mimeType;
  private String characterEncoding;
  private Long contentLength;
  private List<String> designGaps = new ArrayList<>();

  public ApplicationFileSummary() {}

  public String getApplicationName() {
    return applicationName;
  }

  public void setApplicationName(String applicationName) {
    this.applicationName = applicationName;
  }

  public String getPath() {
    return path;
  }

  public void setPath(String path) {
    this.path = path;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Boolean getDirectory() {
    return directory;
  }

  public void setDirectory(Boolean directory) {
    this.directory = directory;
  }

  public String getContent() {
    return content;
  }

  public void setContent(String content) {
    this.content = content;
  }

  public String getMimeType() {
    return mimeType;
  }

  public void setMimeType(String mimeType) {
    this.mimeType = mimeType;
  }

  public String getCharacterEncoding() {
    return characterEncoding;
  }

  public void setCharacterEncoding(String characterEncoding) {
    this.characterEncoding = characterEncoding;
  }

  public Long getContentLength() {
    return contentLength;
  }

  public void setContentLength(Long contentLength) {
    this.contentLength = contentLength;
  }

  /**
   * Catalog-level capability notes. Present on detail; omitted on list rows when null
   * (REST-GAPS-02 payload dedup; class uses NON_NULL).
   */
  @Schema(
      description =
          "Honest design gaps for this surface. Present on detail GET/PUT; typically omitted on"
              + " list rows to avoid repeating the same catalog-level array")
  public List<String> getDesignGaps() {
    return designGaps;
  }

  public void setDesignGaps(List<String> designGaps) {
    this.designGaps = designGaps;
  }
}
