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

package com.percussion.rest.contenttypes;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Workbench-equivalent content-type design XML (CD-14 export). The HTTP resource maps this to
 * {@code application/xml} with {@code Content-Disposition} from {@link #getName()}.
 */
@Schema(description = "Content type design-object XML export (CD-14)")
public class ContentTypeExport {

  private String name;
  private String xml;

  public ContentTypeExport() {}

  public ContentTypeExport(String name, String xml) {
    this.name = name;
    this.xml = xml;
  }

  @Schema(description = "Unique content type name used for the download filename")
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  @Schema(description = "Workbench-equivalent design XML of the loaded content type")
  public String getXml() {
    return xml;
  }

  public void setXml(String xml) {
    this.xml = xml;
  }
}
