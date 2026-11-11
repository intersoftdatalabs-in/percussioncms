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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.deployer.objectstore.PSArchive;
import com.percussion.deployer.objectstore.PSArchiveInfo;
import com.percussion.deployer.objectstore.PSArchiveManifest;
import com.percussion.system.utils.PSArchiveFiles;
import com.percussion.util.IOTools;
import com.percussion.xml.PSXmlDocumentBuilder;
import java.io.File;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

/** Unit test for the PSPackageLockManager. */
@org.junit.jupiter.api.Disabled("requires packaged resources that are not available on CI")
public class PSPackageLockManagerTest {
  /**
   * Tests lock/unlock of a package directory.
   *
   * @throws Exception If there is an error.
   */
  @Test
  public void testLockUnlockDirectory() throws Exception {
    File stagingDir = stageResourceFiles();
    try {
      PSPackageLockManager pkgLockMgr = new PSPackageLockManager();

      // lock packages
      pkgLockMgr.update(stagingDir, true);
      assertAllLocked(stagingDir);

      // unlock packages
      pkgLockMgr.update(stagingDir, false);
      assertAllUnlocked(stagingDir);

      // re-lock packages
      pkgLockMgr.update(stagingDir, true);
      assertAllLocked(stagingDir);
    } finally {
      IOTools.deleteFile(stagingDir);
    }
  }

  /**
   * Tests lock/unlock of a single package file.
   *
   * @throws Exception If there is an error.
   */
  @Test
  public void testLockUnlockSinglePackage() throws Exception {
    File stagingDir = stageResourceFiles();
    try {
      PSPackageLockManager pkgLockMgr = new PSPackageLockManager();

      File f = stagingDir.listFiles()[0];

      // lock
      pkgLockMgr.update(f, true);
      assertTrue(isLocked(f));

      // unlock
      pkgLockMgr.update(f, false);
      assertFalse(isLocked(f));

      // re-lock
      pkgLockMgr.update(f, true);
      assertTrue(isLocked(f));
    } finally {
      IOTools.deleteFile(stagingDir);
    }
  }

  /**
   * Tests that locking a package preserves its entries and manifest.
   *
   * @throws Exception If there is an error.
   */
  @Test
  public void testLockedPackageContentsPreserved() throws Exception {
    File stagingDir = stageResourceFiles();
    try {
      PSPackageLockManager pkgLockMgr = new PSPackageLockManager();

      File f = stagingDir.listFiles()[0];

      Map<String, Long> entryMap1 = zipEntryCrcs(f);

      // load the original manifest
      PSArchiveManifest manifest1 = loadManifest(f);

      // lock
      pkgLockMgr.update(f, true);
      assertTrue(isLocked(f));

      // load the manifest after locking
      PSArchiveManifest manifest2 = loadManifest(f);

      Map<String, Long> entryMap2 = zipEntryCrcs(f);

      assertPackageContentsPreserved(entryMap1, entryMap2);

      // make sure archive manifests are equal
      assertEquals(manifest1, manifest2);
    } finally {
      IOTools.deleteFile(stagingDir);
    }
  }

  /**
   * Stages the test resource packages into a new temp directory.
   *
   * @return The staging directory, never <code>null</code>.
   * @throws Exception If there is an error.
   */
  private File stageResourceFiles() throws Exception {
    File stagingDir = File.createTempFile("tmp", null);
    stagingDir.deleteOnExit();
    stagingDir.delete();
    stagingDir.mkdir();

    copyResourceFilesInto(stagingDir);

    return stagingDir;
  }

  /**
   * Copies the test resource package files into the specified directory, skipping sub-directories.
   *
   * @param stagingDir The target directory, assumed not <code>null</code>.
   * @throws Exception If there is an error.
   */
  private void copyResourceFilesInto(File stagingDir) throws Exception {
    File resourceDir = new File(RESOURCE_DIR);
    File[] resourceFiles = resourceDir.listFiles();
    for (File file : resourceFiles) {
      if (file.isDirectory()) {
        continue;
      }

      IOTools.copyToDir(file, stagingDir);
    }
  }

  /**
   * Asserts that every file in the specified directory is locked.
   *
   * @param stagingDir The directory to check, assumed not <code>null</code>.
   * @throws Exception If there is an error.
   */
  private void assertAllLocked(File stagingDir) throws Exception {
    File[] files = stagingDir.listFiles();
    for (File file : files) {
      assertTrue(isLocked(file));
    }
  }

  /**
   * Asserts that every file in the specified directory is unlocked.
   *
   * @param stagingDir The directory to check, assumed not <code>null</code>.
   * @throws Exception If there is an error.
   */
  private void assertAllUnlocked(File stagingDir) throws Exception {
    File[] files = stagingDir.listFiles();
    for (File file : files) {
      assertFalse(isLocked(file));
    }
  }

  /**
   * Loads the archive manifest of the specified package file.
   *
   * @param packageFile The package file, assumed not <code>null</code>.
   * @return The manifest, never <code>null</code>.
   * @throws Exception If there is an error.
   */
  private PSArchiveManifest loadManifest(File packageFile) throws Exception {
    PSArchive archive = new PSArchive(packageFile);
    try {
      return archive.getArchiveManifest();
    } finally {
      archive.close();
    }
  }

  /**
   * Maps each zip entry name of the specified package file to its CRC.
   *
   * @param packageFile The package file, assumed not <code>null</code>.
   * @return The entry name to CRC map, never <code>null</code>.
   * @throws Exception If there is an error.
   */
  private Map<String, Long> zipEntryCrcs(File packageFile) throws Exception {
    Map<String, Long> entryMap = new HashMap<String, Long>();
    try (ZipFile zipFile = new ZipFile(packageFile)) {
      Enumeration<? extends ZipEntry> entries = zipFile.entries();
      while (entries.hasMoreElements()) {
        ZipEntry entry = entries.nextElement();
        entryMap.put(entry.getName(), entry.getCrc());
      }
    }
    return entryMap;
  }

  /**
   * Asserts that locking preserved every package entry except the archive info and manifest entries,
   * whose contents may legitimately differ.
   *
   * @param before The entry CRCs before locking, assumed not <code>null</code>.
   * @param after The entry CRCs after locking, assumed not <code>null</code>.
   */
  private void assertPackageContentsPreserved(Map<String, Long> before, Map<String, Long> after) {
    assertEquals(before.size(), after.size());

    for (String key : after.keySet()) {
      if (key.equals(PSArchive.ARCHIVE_INFO_PATH) || key.equals(PSArchive.ARCHIVE_MANIFEST_PATH)) {
        // archive info may be different, archive manifest contents may
        // have different order
        continue;
      }

      assertEquals(before.get(key), after.get(key));
    }
  }

  /**
   * Determines if the specified package file is locked.
   *
   * @param packageFile The package file, assumed not <code>null</code>.
   * @return <code>true</code> if the package is locked, <code>false</code> if it is unlocked.
   * @throws Exception If there is an error.
   */
  private boolean isLocked(File packageFile) throws Exception {
    ZipFile zip = null;
    InputStream in = null;

    try {
      zip = new ZipFile(packageFile);
      in = PSArchiveFiles.getFile(zip, PSArchive.ARCHIVE_INFO_PATH);
      Document doc = PSXmlDocumentBuilder.createXmlDocument(in, false);
      PSArchiveInfo info = new PSArchiveInfo(doc.getDocumentElement());
      return !info.isEditable();
    } finally {
      if (in != null) {
        in.close();
      }

      if (zip != null) {
        zip.close();
      }
    }
  }

  /** Defines the path to the files used by this unit test, relative from the E2 root. */
  private static final String RESOURCE_DIR = "Packages/Percussion";
}
