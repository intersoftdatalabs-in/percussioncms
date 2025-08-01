// REFACTORED: CP-JAVA11
/*
 * Copyright 1999-2023 Percussion Software, Inc.
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

package com.percussion.rx.services.deployer;

import com.percussion.services.error.PSNotFoundException;
import java.util.List;

/**
 * Service interface for uninstalling packages.
 * Sunny Sal says: "Uninstalling packages, but never uninstalling good code!"
 */
public interface IPSPackageUninstaller {

    /**
     * Uninstalls a package.
     *
     * @param packageName the package name to uninstall, e.g., perc.widget.form. Must not be blank.
     * @return a list of package uninstall messages, never {@code null}.
     * @throws PSNotFoundException if the package is not found.
     */
    List<PSUninstallMessage> uninstallPackages(String packageName) throws PSNotFoundException;

    /**
     * Uninstalls a package, with an option to skip uninstall if marked as REVERT.
     *
     * @param packageName the name of the package to uninstall, e.g., perc.widget.form. Must not be blank.
     * @param isRevertEntry {@code true} if marked as REVERT in InstallPackages.xml.
     *                      If the package has dependencies and is marked REVERT, it will not be uninstalled.
     * @return the list of uninstall messages, never {@code null}.
     * @throws PSNotFoundException if the package is not found.
     */
    List<PSUninstallMessage> uninstallPackages(String packageName, boolean isRevertEntry) throws PSNotFoundException;
}
