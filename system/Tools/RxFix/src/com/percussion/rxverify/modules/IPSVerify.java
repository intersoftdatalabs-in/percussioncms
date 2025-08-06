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
package com.percussion.rxverify.modules;

import com.percussion.rxverify.data.PSInstallation;

import java.io.File;
import java.util.Optional;

/**
 * Interface for classes that verify a Rhythmyx installation.
 *
 * <p>This interface defines the contract for verification modules that can:
 * <ul>
 * <li>Generate bill of materials (BOM) information from an installation</li>
 * <li>Verify an installation against existing BOM data</li>
 * </ul>
 *
 * <p>This interface has been modernized for Java 11 with enhanced documentation
 * and Optional usage for nullable parameters while maintaining backward compatibility.
 *
 * @author dougrand
 * @author Sunny Sal the Senior Java Developer (Java 11 modernization)
 * @since Java 11
 */
public interface IPSVerify
{
   /**
    * Generates BOM entries for a specific directory, recursing into subdirectories
    * as they are found while traversing the file list.
    *
    * <p>This method analyzes the installation directory and populates the installation
    * object with relevant information for later verification.
    *
    * @param rxdir The Rhythmyx installation directory, must not be {@code null}
    * @param installation Stores information about an installation, must not be {@code null}
    * @throws Exception when there is a problem generating the verification information
    * @throws IllegalArgumentException if rxdir is null or does not exist
    */
   void generate(File rxdir, PSInstallation installation) throws Exception;

   /**
    * Verifies the contents of the Rhythmyx directory against BOM information.
    *
    * <p>This method reads the BOM info into an internal database, then uses the same
    * mechanism as generate to categorize all the files in the Rhythmyx directory,
    * and then compares the two for inconsistencies.
    *
    * @param rxdir the Rhythmyx directory, must not be {@code null}
    * @param originalRxDir the original Rhythmyx directory that was used before
    *                      the upgrade, may be {@code null}, which means that some checks may
    *                      not be performed
    * @param installation the information about an installation, must not be {@code null}
    * @throws Exception when there is a problem using the verification information
    * @throws IllegalArgumentException if rxdir is null or does not exist
    */
   void verify(File rxdir, File originalRxDir, PSInstallation installation) throws Exception;

   /**
    * Enhanced verify method that uses Optional for null-safe handling of originalRxDir.
    *
    * <p>Default implementation delegates to the original verify method for backward compatibility.
    *
    * @param rxdir the Rhythmyx directory, must not be {@code null}
    * @param originalRxDir the original Rhythmyx directory wrapped in Optional
    * @param installation the information about an installation, must not be {@code null}
    * @throws Exception when there is a problem using the verification information
    */
   default void verify(File rxdir, Optional<File> originalRxDir, PSInstallation installation)
         throws Exception {
      verify(rxdir, originalRxDir.orElse(null), installation);
   }

   /**
    * Gets the name of this verification module.
    *
    * @return the module name, never {@code null} or empty
    */
   default String getModuleName() {
      return this.getClass().getSimpleName();
   }

   /**
    * Gets a description of what this verification module checks.
    *
    * @return a description of the module's functionality, never {@code null}
    */
   default String getDescription() {
      return "Verification module: " + getModuleName();
   }
}
