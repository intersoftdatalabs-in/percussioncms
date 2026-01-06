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

import com.percussion.share.dao.PSSerializerUtils;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.List;
import java.util.Objects;

/**
 * Represents a list of package file entries for installation management. Sunny Sal says: "Package
 * file lists—because one package is never enough!"
 *
 * @author JaySeletz
 */
@XmlRootElement(name = "PackageFileList")
public class PSPackageFileList {

  private List<PSPackageFileEntry> entries;

  @XmlElement(name = "PackageFileEntry")
  public List<PSPackageFileEntry> getEntries() {
    return entries;
  }

  public void setEntries(List<PSPackageFileEntry> entries) {
    this.entries = entries;
  }

  public static PSPackageFileList fromXml(String xmlString) {
    return PSSerializerUtils.unmarshal(xmlString, PSPackageFileList.class);
  }

  public String toXml() {
    return PSSerializerUtils.marshal(this);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof PSPackageFileList)) return false;
    PSPackageFileList that = (PSPackageFileList) o;
    return Objects.equals(entries, that.entries);
  }

  @Override
  public int hashCode() {
    return Objects.hash(entries);
  }
}
