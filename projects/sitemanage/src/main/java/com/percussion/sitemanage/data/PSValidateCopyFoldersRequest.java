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
package com.percussion.sitemanage.data;

import com.percussion.pathmanagement.data.PSItemByWfStateRequest;
import java.util.Optional;
import jakarta.xml.bind.annotation.XmlRootElement;
import net.sf.oval.constraint.NotEmpty;
import net.sf.oval.constraint.NotNull;

/**
 * Request object used for validating a source and destination folder for copy. Both folder paths
 * are required. The destination folder may not be empty.
 */
@XmlRootElement(name = "ValidateCopyFoldersRequest")
public class PSValidateCopyFoldersRequest extends PSItemByWfStateRequest {

  @NotNull private String srcFolder;

  @NotNull @NotEmpty private String destFolder;

  /**
   * Gets the source folder path.
   *
   * @return source folder path.
   */
  public String getSrcFolder() {
    return srcFolder;
  }

  /**
   * Sets the source folder path.
   *
   * @param srcFolder source folder path.
   */
  public void setSrcFolder(String srcFolder) {
    this.srcFolder = srcFolder;
  }

  /**
   * Gets the destination folder path.
   *
   * @return destination folder path.
   */
  public String getDestFolder() {
    return destFolder;
  }

  /**
   * Sets the destination folder path.
   *
   * @param destFolder destination folder path.
   */
  public void setDestFolder(String destFolder) {
    this.destFolder = destFolder;
  }

  /**
   * Gets the source folder as Optional.
   *
   * @return Optional source folder.
   */
  public Optional<String> getSrcFolderOptional() {
    return Optional.ofNullable(srcFolder);
  }

  /**
   * Gets the destination folder as Optional.
   *
   * @return Optional destination folder.
   */
  public Optional<String> getDestFolderOptional() {
    return Optional.ofNullable(destFolder);
  }
}
