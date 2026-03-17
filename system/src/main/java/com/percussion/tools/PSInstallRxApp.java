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

package com.percussion.tools;

import com.percussion.security.validation.PathValidation;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

/**
 * This is a simple utility class that installs a Rhythmyx application from a specified JAR file.
 */
public class PSInstallRxApp {

  /**
   * Constructor that takes the JAR file containing all application files. Paths must be relative to
   * the Rhythmyx root directory.
   *
   * @param jarFilePath full path to the JAR file
   * @throws FileNotFoundException if the JAR file is not found
   * @throws IOException if the file could not be accessed
   */
  public PSInstallRxApp(String jarFilePath) throws FileNotFoundException, IOException {
    File file = new File(jarFilePath);
    m_JF = new JarFile(file);
    file = null;
  }

  /**
   * Installs the application from the JAR file. Copies the application file
   * 'ObjectStore/&lt;appName.xml&gt;' and all files in the app folder to the target directory.
   *
   * @param targetRoot Rhythmyx root directory (e.g., c:/Rhythmyx)
   * @param appName application name (e.g., WFEditor)
   */
  public void install(String targetRoot, String appName) {
    String appFilePath = "ObjectStore/" + appName + ".xml";

    ZipEntry entry = null;
    InputStream is = null;
    String name = appFilePath;

    entry = m_JF.getEntry(appFilePath);
    if (entry == null) {
      System.out.println("File <" + appFilePath + "> does not exist in the archive");
      return;
    }

    try {
      is = m_JF.getInputStream(entry);
      copyInputStreamToFile(is, targetRoot, name);
      is.close();
    } catch (IOException e) {
      System.out.println("Error: " + e.getMessage());
      return;
    }

    String appFilter = appName + "/";

    Enumeration<?> e = m_JF.entries();
    while (e.hasMoreElements()) {
      entry = (ZipEntry) e.nextElement();
      name = entry.getName();
      // skip all files with name starting with 'com/'
      if (name.indexOf(appFilter) == -1) {
        continue;
      }
      try {
        // CWE-22: Validate zip entry path to prevent ZipSlip attacks
        File targetDir = new File(targetRoot);
        File safeFile = PathValidation.constructSafePath(targetDir, entry.getName());
        System.out.println("Extracting file: " + name + "...");
        is = m_JF.getInputStream(entry);
        copyInputStreamToFileWithoutValidation(is, safeFile);
        is.close();
      } catch (SecurityException se) {
        // ZipSlip attack detected - skip malicious entry
        System.out.println("Security: Rejected malicious zip entry: " + name);
      } catch (IOException ioe) {
        System.out.println("Error: " + ioe.getMessage());
      }
    }
  }

  /**
   * This method copies the InputStream to the specified directory and file name.
   *
   * @param is - the InputStream to be written to the specified file.
   * @param tgtRoot - the target root diectory
   * @param fileName - the fileName to which the stream is to be written
   * @throws - IOExceptione when the target file cannot be created or written.
   */
  private void copyInputStreamToFile(InputStream is, String tgtRoot, String fileName)
      throws IOException {
    File file = new File(tgtRoot, fileName);
    File parent = file.getParentFile();
    if (null != parent && !parent.exists()) parent.mkdirs();

    copyData(is, file);
  }

  /** Copy pre-validated input stream to already-validated target file (no path validation). */
  private void copyInputStreamToFileWithoutValidation(InputStream is, File targetFile)
      throws IOException {
    File parent = targetFile.getParentFile();
    if (null != parent && !parent.exists()) parent.mkdirs();

    copyData(is, targetFile);
  }

  private void copyData(InputStream is, File targetFile) throws IOException {
    FileOutputStream fos = new FileOutputStream(targetFile);
    byte[] buffer = new byte[1024];
    int nRead = -1;
    while (true) {
      nRead = is.read(buffer);
      if (nRead < 0) break; // end of input stream
      fos.write(buffer, 0, nRead);
    }
    fos.flush();
    fos.close();
  }

  /** Instanec of the jar file that has all Rx application files. */
  protected JarFile m_JF = null;

  /**
   * The main method that takes three command line parameters. The first one is JAR file full path,
   * second one is the Rhythmyx root directory name and the last one is the name of application to
   * be installed.
   */
  public static void main(String[] args) {
    if (args.length < 3) {
      System.out.println("Usage:");
      System.out.println("PSInstallRxApp <JarFilePath> <RhythmyxRootDir> " + "<AppName>");
      System.out.println("Press ENTER to continue...");
      try {
        System.in.read();
      } catch (Exception ee) {
      }
      ;
      System.exit(1);
      ;
    }

    try {
      PSInstallRxApp installRxApp = new PSInstallRxApp(args[0]);
      installRxApp.install(args[1], args[2]);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
