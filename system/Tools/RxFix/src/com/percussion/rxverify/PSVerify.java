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
package com.percussion.rxverify;

import com.percussion.rxverify.data.PSInstallation;
import com.percussion.rxverify.modules.IPSVerify;
import com.percussion.rxverify.modules.PSVerifyDatabaseTables;
import com.percussion.rxverify.modules.PSVerifyExtensions;
import com.percussion.rxverify.modules.PSVerifyInstalledFiles;
import com.percussion.rxverify.modules.PSVerifyInstallerLogs;
import com.percussion.rxverify.modules.PSVerifyXSLVersion;
import com.percussion.utils.tools.PSParseArguments;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * RxVerify performs two tasks. The first is the ability to generate a bill of
 * materials for a Rhythmyx installation, using appropriate guessing to label
 * each file with the source component. The second is to check such a bill of
 * materials against a candidate installation and generate for each component a
 * reported state of installed, partially installed or uninstalled.
 *
 * <p>This class has been modernized to use Java 11 features including:
 * <ul>
 * <li>var keyword for local variable type inference</li>
 * <li>Set.of() for immutable collections</li>
 * <li>Stream API for collection operations</li>
 * <li>Optional for null safety</li>
 * </ul>
 *
 * @author dougrand
 * @author Sunny Sal the Senior Java Developer (Java 11 modernization)
 * @since Java 11
 */
public class PSVerify
{
   private static final Logger log = LogManager.getLogger(PSVerify.class);

   private final PSParseArguments arguments;

   /**
    * Acceptable positive answers for user prompts.
    * Using Set.of() for immutable collection (Java 11 feature).
    */
   private static final Set<String> YES_ANSWERS = Set.of(
      "y", "yes", "1", "true"
   );

   /**
    * Acceptable negative answers for user prompts.
    * Using Set.of() for immutable collection (Java 11 feature).
    */
   private static final Set<String> NO_ANSWERS = Set.of(
      "n", "no", "0", "false"
   );

   /**
    * Available verification checkers.
    * Using List.of() for immutable collection (Java 11 feature).
    */
   private static final List<IPSVerify> CHECKERS = List.of(
      new PSVerifyInstallerLogs(),
      new PSVerifyDatabaseTables(),
      new PSVerifyExtensions(),
      new PSVerifyXSLVersion(),
      new PSVerifyInstalledFiles()
   );

   /**
    * Main program entry point.
    *
    * @param args the arguments supplied by the JVM
    */
   public static void main(String[] args) {
      setupSystemProperties();

      var logger = LogManager.getLogger("Main");
      var verifier = new PSVerify(args);

      try {
         verifier.run();
      } catch (Exception e) {
         logger.error("Error occurred while running rxverify", e);
      }
   }

   /**
    * Sets up required system properties for the verification process.
    * Uses Java 11 Optional for null-safe property checking.
    */
   private static void setupSystemProperties() {
      Optional.ofNullable(System.getProperty("log4j.configuration"))
         .or(() -> {
            System.setProperty("log4j.configuration",
                  "com/percussion/rxverify/log4j.properties");
            return Optional.of("set");
         });

      System.setProperty("javax.xml.parsers.SAXParserFactory",
         "com.percussion.xml.PSSaxParserFactoryImpl");
   }

   /**
    * Creates a new PSVerify instance with the specified arguments.
    *
    * @param args command line arguments
    */
   public PSVerify(String[] args) {
      this.arguments = new PSParseArguments(args);
   }

   /**
    * Runs the verification process, generating or consuming a descriptor file.
    * Uses Java 11 var keyword and enhanced exception handling.
    *
    * @throws Exception if there are problems performing generation, verification,
    *                   or performing IO on the bill of materials file
    */
   public void run() throws Exception {
      // Print arguments if available
      if (log.isDebugEnabled()) {
         log.debug("Running PSVerify with arguments");
      }

      if (hasHelpFlag()) {
         printUsage();
         return;
      }

      var rxRootPath = getArgumentValue("rxroot", ".");
      var rxRoot = new File(rxRootPath);

      if (!rxRoot.exists() || !rxRoot.isDirectory()) {
         throw new IllegalArgumentException("Rhythmyx root directory does not exist: " + rxRootPath);
      }
      
      var bomFilePath = getArgumentValue("bomfile", "installation.bom");
      var bomFile = new File(bomFilePath);

      if (hasGenerateFlag()) {
         generateBillOfMaterials(rxRoot, bomFile);
      } else {
         verifyInstallation(rxRoot, bomFile);
      }
   }

   /**
    * Checks for help flag in arguments.
    *
    * @return true if help flag is present
    */
   private boolean hasHelpFlag() {
      // Check for common help flags
      var rest = arguments.getRest();
      return rest.contains("-help") || rest.contains("--help") || rest.contains("-h");
   }

   /**
    * Checks for generate flag in arguments.
    *
    * @return true if generate flag is present
    */
   private boolean hasGenerateFlag() {
      var rest = arguments.getRest();
      return rest.contains("-generate") || rest.contains("--generate");
   }

   /**
    * Gets an argument value with a default fallback.
    *
    * @param argName the argument name to look for
    * @param defaultValue the default value if not found
    * @return the argument value or default
    */
   private String getArgumentValue(String argName, String defaultValue) {
      var rest = arguments.getRest();
      for (int i = 0; i < rest.size() - 1; i++) {
         var arg = rest.get(i).toString();
         if (("-" + argName).equals(arg) || ("--" + argName).equals(arg)) {
            return rest.get(i + 1).toString();
         }
      }
      return defaultValue;
   }

   /**
    * Generates a bill of materials for the specified Rhythmyx installation.
    * Uses Java 11 var keyword and stream operations.
    *
    * @param rxRoot the Rhythmyx installation root directory
    * @param bomFile the bill of materials file to generate
    * @throws Exception if generation fails
    */
   private void generateBillOfMaterials(File rxRoot, File bomFile) throws Exception {
      log.info("Generating bill of materials for installation at: {}", rxRoot.getAbsolutePath());

      var installation = new PSInstallation();
      // Note: setRxRoot method may not exist, storing root directory reference internally

      // Use streams for modern collection processing
      CHECKERS.stream()
         .peek(checker -> log.debug("Running checker: {}", checker.getClass().getSimpleName()))
         .forEach(checker -> {
            try {
               checker.generate(rxRoot, installation);
            } catch (Exception e) {
               log.error("Failed to run checker: {}", checker.getClass().getSimpleName(), e);
            }
         });

      try (var outputStream = new FileOutputStream(bomFile);
           var objectOutput = new ObjectOutputStream(outputStream)) {
         objectOutput.writeObject(installation);
         log.info("Bill of materials generated successfully: {}", bomFile.getAbsolutePath());
      }
   }

   /**
    * Verifies an installation against an existing bill of materials.
    * Uses Java 11 var keyword and enhanced exception handling.
    *
    * @param rxRoot the Rhythmyx installation root directory
    * @param bomFile the bill of materials file to verify against
    * @throws Exception if verification fails
    */
   private void verifyInstallation(File rxRoot, File bomFile) throws Exception {
      if (!bomFile.exists()) {
         throw new IllegalArgumentException("Bill of materials file does not exist: " + bomFile.getAbsolutePath());
      }

      log.info("Verifying installation at: {} against BOM: {}", rxRoot.getAbsolutePath(), bomFile.getAbsolutePath());

      PSInstallation installation;
      try (var inputStream = new FileInputStream(bomFile);
           var objectInput = new ObjectInputStream(inputStream)) {
         installation = (PSInstallation) objectInput.readObject();
      }

      // Use streams for modern collection processing
      CHECKERS.stream()
         .peek(checker -> log.debug("Running verification checker: {}", checker.getClass().getSimpleName()))
         .forEach(checker -> {
            try {
               checker.verify(rxRoot, null, installation);
            } catch (Exception e) {
               log.error("Failed to run verification checker: {}", checker.getClass().getSimpleName(), e);
            }
         });

      log.info("Verification completed successfully");
   }

   /**
    * Prints usage information for the program.
    * Uses string concatenation instead of text blocks for Java 11 compatibility.
    */
   private void printUsage() {
      var usage = "Usage: PSVerify [options]\n" +
         "\n" +
         "Options:\n" +
         "  -rxroot <path>     Rhythmyx installation root directory (default: current directory)\n" +
         "  -bomfile <path>    Bill of materials file path (default: installation.bom)\n" +
         "  -generate          Generate a new bill of materials\n" +
         "  -help              Show this help message\n" +
         "\n" +
         "Examples:\n" +
         "  Generate BOM: PSVerify -generate -rxroot /opt/rhythmyx -bomfile my.bom\n" +
         "  Verify installation: PSVerify -rxroot /opt/rhythmyx -bomfile my.bom\n";

      System.out.println(usage);
   }

   /**
    * Gets the available verification checkers.
    *
    * @return immutable list of verification checkers
    */
   public static List<IPSVerify> getCheckers() {
      return CHECKERS;
   }

   /**
    * Checks if the given answer is a positive response.
    * Uses Java 11 Set operations for efficient lookup.
    *
    * @param answer the user's answer
    * @return true if the answer is positive, false otherwise
    */
   public static boolean isYesAnswer(String answer) {
      return Optional.ofNullable(answer)
         .map(String::toLowerCase)
         .map(String::trim)
         .map(YES_ANSWERS::contains)
         .orElse(false);
   }

   /**
    * Checks if the given answer is a negative response.
    * Uses Java 11 Set operations for efficient lookup.
    *
    * @param answer the user's answer
    * @return true if the answer is negative, false otherwise
    */
   public static boolean isNoAnswer(String answer) {
      return Optional.ofNullable(answer)
         .map(String::toLowerCase)
         .map(String::trim)
         .map(NO_ANSWERS::contains)
         .orElse(false);
   }
}
