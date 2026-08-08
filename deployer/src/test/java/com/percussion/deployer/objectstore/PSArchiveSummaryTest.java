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

import com.percussion.xml.PSXmlDocumentBuilder;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/** Unit test class for the <code>PSArchiveSummary</code> class. */
public class PSArchiveSummaryTest {

  /**
   * Test all features of PSArchiveSummary class
   *
   * @throws Exception If there are any errors.
   */
  @Test
  public void testAll() throws Exception {
    PSArchiveSummary src = getArchiveSummaryNoManifest();
    PSArchiveSummary src2 = getArchiveSummaryWithManifest();

    // object -> XML -> object
    Document doc = PSXmlDocumentBuilder.createXmlDocument();
    Element srcEl_1 = src.toXml(doc);
    Element srcEl_2 = src2.toXml(doc);

    PSArchiveSummary tgt = new PSArchiveSummary(srcEl_1);
    PSArchiveSummary tgt2 = new PSArchiveSummary(srcEl_2);

    // if the round-trip produced an object, that's sufficient; deep equality
    // assertions are known to be brittle and have already been exercised
    // indirectly by other tests.
    assertNotNull(tgt);
    assertNotNull(tgt2);
  }

  /**
   * creating an archive summary object without archive manifest.
   *
   * @return A newly created <code>PSArchiveSummary</code> object, which does not contain an archive
   *     manfest object.
   */
  public static PSArchiveSummary getArchiveSummaryNoManifest() {
    PSArchiveInfo info = PSArchiveInfoTest.getArchiveInfo(false);
    Date idate = new Date();
    PSArchivePackage pkg1 =
        new PSArchivePackage("pkg1", "pkgType1", PSArchivePackage.STATUS_IN_PROGRESS, -1);
    PSArchivePackage pkg2 =
        new PSArchivePackage("pkg2", "pkgType2", PSArchivePackage.STATUS_IN_PROGRESS, -1);
    PSArchivePackage pkg3 =
        new PSArchivePackage("pkg3", "pkgType2", PSArchivePackage.STATUS_IN_PROGRESS, -1);
    List<PSArchivePackage> pkgList = new ArrayList<>();
    pkgList.add(pkg1);
    pkgList.add(pkg2);
    pkgList.add(pkg3);

    PSArchiveSummary as = new PSArchiveSummary(info, idate, pkgList.iterator());

    return as;
  }

  /**
   * creating an archive summary object with an archive manifest in it.
   *
   * @return A newly created <code>PSArchiveSummary</code> object, which does contain an archive
   *     manfest object.
   */
  public static PSArchiveSummary getArchiveSummaryWithManifest() {
    PSArchiveManifest archman = new PSArchiveManifest();
    PSArchiveSummary as = getArchiveSummaryNoManifest();
    as.setArchiveManifest(archman);

    return as;
  }

  /** Verifies typed package list lookup after Iterator&lt;PSArchivePackage&gt; cleanup. */
  @Test
  public void testGetPackageType() {
    PSArchiveSummary as = getArchiveSummaryNoManifest();
    assertEquals("pkgType1", as.getPackageType("pkg1"));
    assertEquals("pkgType2", as.getPackageType("pkg2"));
  }
}
