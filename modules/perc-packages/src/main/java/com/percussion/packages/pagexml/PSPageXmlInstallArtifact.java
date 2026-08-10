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

package com.percussion.packages.pagexml;

import java.util.Objects;

/**
 * One install-path TemplateDef payload derived from a modern page component package.
 *
 * @param stem template id / file stem (e.g. {@code perc.base.plain})
 * @param guid assembly GUID string (e.g. {@code 0-4-591})
 * @param archiveFolder mapping value (e.g. {@code TemplateDef-591})
 * @param templateDefXml legacy {@code <assembly-template>} document text for deployer install
 */
public record PSPageXmlInstallArtifact(
    String stem, String guid, String archiveFolder, String templateDefXml) {

  public PSPageXmlInstallArtifact {
    Objects.requireNonNull(stem, "stem");
    Objects.requireNonNull(guid, "guid");
    Objects.requireNonNull(archiveFolder, "archiveFolder");
    Objects.requireNonNull(templateDefXml, "templateDefXml");
    if (stem.isBlank()) {
      throw new IllegalArgumentException("stem must not be blank");
    }
    if (guid.isBlank()) {
      throw new IllegalArgumentException("guid must not be blank");
    }
    if (archiveFolder.isBlank()) {
      throw new IllegalArgumentException("archiveFolder must not be blank");
    }
  }

  /** Root dual-ship file name: {@code &lt;stem&gt;.templateDef}. */
  public String rootFileName() {
    return stem + ".templateDef";
  }
}
