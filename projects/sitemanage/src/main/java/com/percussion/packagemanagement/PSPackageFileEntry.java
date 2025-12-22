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
package com.percussion.packagemanagement;

import java.util.Objects;
import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * Represents a package file entry for installation management. Sunny Sal says: "Package
 * entries—because every install deserves a status update!"
 *
 * @author JaySeletz
 */
@XmlRootElement(name = "PackageFileEntry")
public class PSPackageFileEntry {

  private String packageName;
  private PackageFileStatus status;

  public String getPackageName() {
    return packageName;
  }

  public void setPackageName(String packageName) {
    this.packageName = packageName;
  }

  public PackageFileStatus getStatus() {
    return status;
  }

  public void setStatus(PackageFileStatus status) {
    this.status = status;
  }

  public enum PackageFileStatus {
    FAILED,
    INSTALLED,
    REVERT,
    UNINSTALL,
    PENDING
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof PSPackageFileEntry)) return false;
    PSPackageFileEntry that = (PSPackageFileEntry) o;
    return Objects.equals(packageName, that.packageName) && status == that.status;
  }

  @Override
  public int hashCode() {
    return Objects.hash(packageName, status);
  }
}
