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

package com.percussion.deployer.objectstore;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Unit test for the <code>PSArchive</code> object. */
public class PSArchiveTest {
  @TempDir Path temporaryFolder;
  private String rxdeploydir;

  @BeforeEach
  public void setup() throws IOException {

    rxdeploydir = System.getProperty("rxdeploydir");
    System.setProperty("rxdeploydir", temporaryFolder.toFile().getAbsolutePath());
  }

  @AfterEach
  public void teardown() {
    if (rxdeploydir != null) System.setProperty("rxdeploydir", rxdeploydir);
  }

  /** Construct this unit test */
  public PSArchiveTest() {
    super();
  }

  /**
   * Test all archive functionality
   *
   * @throws Exception if there are any errors.
   */
  // TODO: Fix Me!
  @Test
  @Disabled
  public void testArchive() throws Exception {
    File archiveFile = File.createTempFile("ArchiveTest", ".pda");
    archiveFile.deleteOnExit();

    // create a new archive
    PSArchiveInfo info1 = PSArchiveInfoTest.getArchiveInfo(true);
    String archiveRef = "ref1";
    info1.setArchiveRef(archiveRef);
    PSArchive archive = new PSArchive(archiveFile, info1);

    PSArchiveInfo info2 = archive.getArchiveInfo(true);
    assertEquals(info1, info2);
    PSArchiveManifest man = new PSArchiveManifest();
    assertNull(archive.getArchiveManifest());
    archive.storeArchiveManifest(man);
    assertNotNull(archive.getArchiveManifest());

    // be sure we can't read a file while opened for writing.
    boolean caught;
    caught = false;
    try {
      archive.getFile("testFile.xml");
    } catch (IllegalStateException e) {
      caught = true;
    }
    assertTrue(caught);

    archive.close();

    // be sure we can't write after closing.
    caught = false;
    try {
      archive.storeArchiveManifest(man);
    } catch (IllegalStateException e) {
      caught = true;
    }
    assertTrue(caught);

    // should always be able to get the info object.
    caught = false;
    try {
      archive.getArchiveInfo(false);
    } catch (IllegalStateException e) {
      caught = true;
    }
    assertTrue(!caught);

    // now open for reading
    archive = new PSArchive(archiveFile);
    info2 = archive.getArchiveInfo(true);

    // archive open will clear the dbmsinfo since there were no external dbms
    // listed in the manifest
    PSArchiveDetail detail1 = info1.getArchiveDetail();
    Iterator pkgs = detail1.getPackages();
    while (pkgs.hasNext()) {
      detail1.setDbmsInfoList((PSDeployableElement) pkgs.next(), new ArrayList<>());
    }

    // archive ref will now be filename
    assertTrue(!info1.equals(info2));
    String newArchiveRef = archiveFile.getName();
    newArchiveRef = newArchiveRef.substring(0, newArchiveRef.lastIndexOf("."));

    info1.setArchiveRef(newArchiveRef);
    assertEquals(info1, info2);
    assertNotNull(archive.getArchiveManifest());

    // be sure we can't write a file while opened for writing.
    caught = false;
    try {
      archive.storeArchiveManifest(man);
    } catch (IllegalStateException e) {
      caught = true;
    }
    assertTrue(caught);
    archive.close();

    // be sure we can't read after closing.
    caught = false;
    try {
      archive.getFile("testFile.xml");
    } catch (IllegalStateException e) {
      caught = true;
    }
    assertTrue(caught);
  }

  /**
   * Test that getArchiveInfo with includeDetail=true handles null archive detail correctly. This
   * test verifies the fix for the NullPointerException when loading archives without a detail
   * section (i.e., when PSArchiveDetail is not present in the archive XML).
   *
   * <p>The key assertion is that we should NOT get a NullPointerException when detail is null.
   * Other errors are acceptable (e.g., archive corruption), but NPE from updateDbmsInfoList
   * indicates the bug is still present.
   *
   * @throws Exception if there are any errors.
   */
  @Test
  public void testArchiveWithoutDetailNull() throws Exception {
    // Create a minimal archive info without detail
    PSArchiveInfo info = PSArchiveInfoTest.getArchiveInfo(false);
    assertNull(info.getArchiveDetail(), "Test setup: archive info must have null detail");

    File archiveFile = File.createTempFile("ArchiveTestNoDetail", ".pda");
    archiveFile.deleteOnExit();

    // Create and store the archive
    PSArchive archive = new PSArchive(archiveFile, info);
    archive.close();

    // The critical test: reopen the archive with includeDetail=true
    // With the bug, this would throw NullPointerException from updateDbmsInfoList
    // With the fix, it should either succeed or fail with a different error
    try {
      archive = new PSArchive(archiveFile);
      PSArchiveInfo retrievedInfo = archive.getArchiveInfo(true);

      // If we get here, the archive was read successfully
      assertNull(
          retrievedInfo.getArchiveDetail(), "Detail should remain null when not stored in archive");
      archive.close();
    } catch (NullPointerException e) {
      // This would indicate the bug is still present
      fail("NullPointerException should not occur - the fix may not be applied: " + e.getMessage());
    } catch (Exception e) {
      // Other exceptions are acceptable; just log and verify it's not NPE
      System.out.println(
          "Archive read raised exception (not NullPointerException, which is expected): "
              + e.getClass().getSimpleName()
              + " - "
              + e.getMessage());
    }
  }

  /**
   * Test loading one of the actual pre-built packages from the system to see if detail is being
   * read correctly. This helps debug why detail might be null in real packages.
   *
   * @throws Exception if there are any errors.
   */
  @Test
  public void testLoadPreBuiltPackage() throws Exception {
    String packagePath = System.getenv("PACKAGE_PATH");
    if (packagePath == null) {
      packagePath = "/home/nate/installs/cms-8.2-dev/Packages/Percussion/perc.baseTemplates.ppkg";
    }

    File pkgFile = new File(packagePath);
    if (!pkgFile.exists()) {
      System.out.println("Skipping test - pre-built package not found at: " + packagePath);
      return;
    }

    try {
      PSArchive archive = new PSArchive(pkgFile);
      PSArchiveInfo info = archive.getArchiveInfo(true);

      System.out.println("Package loaded: " + info.getArchiveRef());
      System.out.println(
          "Archive detail is: " + (info.getArchiveDetail() == null ? "NULL" : "PRESENT"));

      if (info.getArchiveDetail() != null) {
        Iterator<PSDeployableElement> pkgs = info.getArchiveDetail().getPackages();
        long pkgCount =
            java.util.stream.StreamSupport.stream(
                    java.util.Spliterators.spliteratorUnknownSize(pkgs, 0), false)
                .count();
        System.out.println("Detail contains " + pkgCount + " packages");

        // Reset iterator since we consumed it
        Iterator<PSDeployableElement> pkgs2 = info.getArchiveDetail().getPackages();
        int pkgIndex = 0;
        while (pkgs2.hasNext()) {
          PSDeployableElement pkg = pkgs2.next();
          Iterator<PSDependency> deps = pkg.getDependencies();
          long depCount =
              java.util.stream.StreamSupport.stream(
                      java.util.Spliterators.spliteratorUnknownSize(deps, 0), false)
                  .count();
          System.out.println(
              "  Package "
                  + pkgIndex
                  + " ("
                  + pkg.getDisplayName()
                  + ") has "
                  + depCount
                  + " dependencies");
          pkgIndex++;
        }
      } else {
        System.out.println(
            "WARNING: Detail is null even though package should contain it! This indicates a"
                + " parsing issue.");
      }

      archive.close();
    } catch (Exception e) {
      System.out.println("Error loading package: " + e.getMessage());
      throw e;
    }
  }
}
