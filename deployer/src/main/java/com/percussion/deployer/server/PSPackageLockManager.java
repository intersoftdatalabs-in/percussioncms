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

package com.percussion.deployer.server;

import com.percussion.deployer.client.IPSDeployConstants;
import com.percussion.deployer.objectstore.PSArchive;
import com.percussion.deployer.objectstore.PSArchiveInfo;
import com.percussion.design.objectstore.PSUnknownNodeTypeException;
import com.percussion.error.PSDeployException;
import com.percussion.security.error.PSExceptionUtils;
import com.percussion.system.utils.PSArchiveFiles;
import com.percussion.util.IOTools;
import com.percussion.xml.PSXmlDocumentBuilder;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipFile;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.xml.sax.SAXException;

/**
 * An application that manages the lock of one or more specified packages. The lock of a package
 * translates to an "editable" flag in the package's archive info. See {@link
 * PSArchiveInfo#isEditable()}.
 */
public class PSPackageLockManager {

  /** Default constructor for use by Spring. */
  public PSPackageLockManager() {}

  private static final Logger log = LogManager.getLogger(PSPackageLockManager.class);

  /**
   * Invokes this application, must be run from the Rhythmyx root directory.
   *
   * @param args Expects the following:
   *     <ul>
   *       <li>-lock/unlock - (arg is case-insensitive) will lock/unlock all package files specified
   *           by the proceeding argument, which may either be an absolute or relative path to a
   *           package file or directory of package files
   *       <li>-h[elp] - will display the commandline help (arg is case-insenstive)
   *     </ul>
   */
  public static void main(String[] args) {
    boolean lock = false;
    String filePath = null;

    if (args.length > 0) {
      String arg = args[0];
      if (arg.equalsIgnoreCase("-lock") || arg.equalsIgnoreCase("-unlock")) {
        lock = arg.equalsIgnoreCase("-lock") ? true : false;

        if (args.length > 1) {
          filePath = args[1];
        } else {
          showUsageAndExit();
        }
      } else if (arg.equalsIgnoreCase("-h") || arg.equalsIgnoreCase("-help")) {
        showUsageAndExit();
      } else {
        showUsageAndExit();
      }
    } else {
      showUsageAndExit();
    }

    try {
      PSPackageLockManager manager = new PSPackageLockManager();
      manager.update(new File(filePath), lock);
    } catch (Exception e) {
      System.err.println("Error encountered during conversion");
      log.error(PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
      log.debug(System.err);
    }
  }

  /**
   * Recursive method which updates the lock of the specified package or packages.
   *
   * @param packageFile The package file or directory of package files to be updated. May not be
   *     <code>null</code>.
   * @param lock <code>true</code> to lock the package(s), <code>false</code> to unlock the
   *     package(s).
   * @throws PSUnknownNodeTypeException If there is a problem with an XML file format.
   * @throws SAXException If the XML doc is malformed.
   * @throws IOException If there is an error reading from a file.
   * @throws PSDeployException If there is an archive error.
   */
  public void update(File packageFile, boolean lock)
      throws IOException, SAXException, PSDeployException, PSUnknownNodeTypeException {
    if (packageFile == null) {
      throw new IllegalArgumentException("packageFile may not be null");
    }

    if (packageFile.isDirectory()) {
      var files = packageFile.listFiles();
      for (var file : files) {
        update(file, lock);
      }
    } else {
      if (!packageFile.getName().endsWith(IPSDeployConstants.ARCHIVE_EXTENSION)) {
        return;
      }

      var action = lock ? "Locking" : "Unlocking";

      System.out.println(action + " package file: " + packageFile.getName());

      try (var zip = new ZipFile(packageFile)) {
        var oldArchive = new PSArchive(packageFile);
        var oldHandler = new PSArchiveHandler(oldArchive);
        var manifest = oldArchive.getArchiveManifest();
        var info = oldArchive.getArchiveInfo(true);

        try (var in = PSArchiveFiles.getFile(zip, PSArchive.ARCHIVE_INFO_PATH)) {
          var doc = PSXmlDocumentBuilder.createXmlDocument(in, false);
          info = new PSArchiveInfo(doc.getDocumentElement());
          info.setEditable(!lock);
        }

        var tmpArchive = File.createTempFile("tmp", null);
        var newArchive = new PSArchive(tmpArchive, info);
        newArchive.storeArchiveManifest(manifest);

        manifest
            .getFiles()
            .forEachRemaining(
                depFile -> {
                  try (var in = oldHandler.getFileData(depFile)) {
                    addFile(
                        in, depFile.getArchiveLocation().getPath().replace('\\', '/'), newArchive);
                  } catch (IOException | PSDeployException e) {
                    throw new RuntimeException(e);
                  }
                });

        var detail = info.getArchiveDetail();
        var descriptor = detail.getExportDescriptor();
        if (!descriptor.getConfigDefFile().isBlank()) {
          copyConfigFile("configurations/impl_config.xml", oldArchive, newArchive);
        }
        if (!descriptor.getLocalConfigFile().isBlank()) {
          copyConfigFile("configurations/local_config.xml", oldArchive, newArchive);
        }

        oldHandler.close();
        newArchive.close();
        IOTools.copyFileStreams(tmpArchive, packageFile);
      }
    }
  }

  /** Write the usage text to the log and exits the program. */
  private static void showUsageAndExit() {
    System.out.println(
        "PackageLockManager.bat [-lock | -unlock] [package | package dir] "
            + "-h[elp]\n"
            + "Example (single package): PackageLockManager.bat -lock "
            + "C:\\Rhythmyx\\myPackage.ppkg\n"
            + "Example (package dir): PackageLockManager.bat -unlock "
            + "C:\\Rhythmyx\\myPackages");
    System.exit(1);
  }

  /**
   * Adds a file to an archive from an input stream.
   *
   * @param in InputStream from which the file will be generated and copied, assumed not <code>null
   *     </code>. Will not be closed by this method.
   * @param entryPath The entry path under which the file will be added to the archive, assumed not
   *     <code>null</code>.
   * @param archive The archive, assumed not <code>null</code>.
   * @throws IOException If an error occurs processing streams.
   * @throws PSDeployException If an error occurs storing the file.
   */
  private void addFile(InputStream in, String entryPath, PSArchive archive)
      throws IOException, PSDeployException {

    var tmp = File.createTempFile("tmp", null);
    try (var out = new FileOutputStream(tmp)) {
      IOTools.copyStream(in, out);
      archive.storeFile(tmp, entryPath);
    } finally {
      tmp.delete();
    }
  }

  /**
   * Copies a configuration file from one archive to another.
   *
   * @param entryPath The configuration file archive entry path, assumed not <code>null</code>.
   * @param srcArchive The source archive, assumed not <code>null</code>.
   * @param tgtArchive The target archive, assumed not <code>null</code>.
   * @throws PSDeployException If an archive error occurs.
   * @throws IOException If a file error occurs.
   */
  private void copyConfigFile(String entryPath, PSArchive srcArchive, PSArchive tgtArchive)
      throws PSDeployException, IOException {
    try (var in = srcArchive.getFile(entryPath)) {
      addFile(in, entryPath, tgtArchive);
    }
  }
}
