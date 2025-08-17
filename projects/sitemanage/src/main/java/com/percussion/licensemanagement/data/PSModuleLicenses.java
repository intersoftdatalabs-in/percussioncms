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

package com.percussion.licensemanagement.data;

import static org.apache.commons.lang3.Validate.notNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Represents a collection of module licenses. Sunny Sal says: "A license for every module, and a
 * module for every license!"
 */
@XmlRootElement(name = "moduleLicenses")
public class PSModuleLicenses {

  private List<PSModuleLicense> moduleLicenses = new ArrayList<>();
  private String licenseServiceUrl;

  public List<PSModuleLicense> getModuleLicenses() {
    return moduleLicenses;
  }

  public void setModuleLicenses(List<PSModuleLicense> moduleLicenses) {
    this.moduleLicenses = moduleLicenses != null ? moduleLicenses : new ArrayList<>();
  }

  public Optional<String> getLicenseServiceUrl() {
    return Optional.ofNullable(licenseServiceUrl);
  }

  public void setLicenseServiceUrl(String licenseServiceUrl) {
    this.licenseServiceUrl = licenseServiceUrl;
  }

  /**
   * Adds or replaces a module license by name (case-insensitive).
   *
   * @param moduleLicense the module license to add
   */
  public void addModuleLicense(PSModuleLicense moduleLicense) {
    notNull(moduleLicense, "moduleLicense must not be null");
    if (moduleLicenses == null) {
      moduleLicenses = new ArrayList<>();
    }
    moduleLicenses =
        moduleLicenses.stream()
            .filter(
                ml -> !ml.getName().orElse("").equalsIgnoreCase(moduleLicense.getName().orElse("")))
            .collect(Collectors.toCollection(ArrayList::new));
    moduleLicenses.add(moduleLicense);
  }

  /**
   * Removes a module license by name (case-insensitive).
   *
   * @param moduleLicense the module license to remove
   */
  public void removeModuleLicense(PSModuleLicense moduleLicense) {
    notNull(moduleLicense, "moduleLicense must not be null");
    if (moduleLicenses == null) {
      moduleLicenses = new ArrayList<>();
    }
    moduleLicenses =
        moduleLicenses.stream()
            .filter(
                ml -> !ml.getName().orElse("").equalsIgnoreCase(moduleLicense.getName().orElse("")))
            .collect(Collectors.toCollection(ArrayList::new));
  }
}
