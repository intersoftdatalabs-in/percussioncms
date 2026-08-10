/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Behavioral coverage for typed {@link PSArchiveDetail} DBMS info map and XML round-trip (issue
 * #2764 Xlint batch 4).
 */
public class PSArchiveDetailTypedTest {

  private static PSDeployableElement newPkg(String id, String display) {
    return new PSDeployableElement(
        PSDependency.TYPE_SHARED, id, "Package", "Package", display, true, false, false);
  }

  private static PSExportDescriptor newExportDesc(PSDeployableElement... pkgs) {
    PSExportDescriptor desc = new PSExportDescriptor("archive-detail-test");
    desc.setPackages(java.util.Arrays.asList(pkgs).iterator());
    return desc;
  }

  @Test
  public void testSetAndGetExternalDbmsList() {
    PSDeployableElement pkg = newPkg("pkg1", "Package One");
    PSArchiveDetail detail = new PSArchiveDetail(newExportDesc(pkg));

    List<PSDatasourceMap> infoList = new ArrayList<>();
    infoList.add(new PSDatasourceMap("RhythmyxData", "targetDs"));
    detail.setDbmsInfoList(pkg, infoList);

    Iterator<PSDatasourceMap> it = detail.getExternalDbmsList(pkg);
    assertTrue(it.hasNext());
    PSDatasourceMap map = it.next();
    assertEquals("RhythmyxData", map.getSrc());
    assertFalse(it.hasNext());
  }

  @Test
  public void testSetDbmsInfoListUnknownPackageThrows() {
    PSDeployableElement pkg = newPkg("pkg1", "Package One");
    PSArchiveDetail detail = new PSArchiveDetail(newExportDesc(pkg));
    PSDeployableElement other = newPkg("other", "Other");

    assertThrows(
        IllegalArgumentException.class,
        () -> detail.setDbmsInfoList(other, new ArrayList<>()));
  }

  @Test
  public void testXmlRoundTripPreservesDbmsInfo() throws Exception {
    PSDeployableElement pkg = newPkg("pkg1", "Package One");
    PSArchiveDetail original = new PSArchiveDetail(newExportDesc(pkg));
    List<PSDatasourceMap> infoList = new ArrayList<>();
    infoList.add(new PSDatasourceMap("sourceDs", "targetDs"));
    original.setDbmsInfoList(pkg, infoList);

    Document doc = com.percussion.xml.PSXmlDocumentBuilder.createXmlDocument();
    Element el = original.toXml(doc);
    PSArchiveDetail restored = new PSArchiveDetail(el);

    assertNotNull(restored.getExportDescriptor());
    Iterator<PSDeployableElement> pkgs = restored.getPackages();
    assertTrue(pkgs.hasNext());
    PSDeployableElement restoredPkg = pkgs.next();
    Iterator<PSDatasourceMap> it = restored.getExternalDbmsList(restoredPkg);
    assertTrue(it.hasNext());
    assertEquals("sourceDs", it.next().getSrc());
    assertFalse(it.hasNext());
  }

  @Test
  public void testCopyFromPreservesDbmsInfo() {
    PSDeployableElement pkg = newPkg("pkg1", "Package One");
    PSExportDescriptor desc = newExportDesc(pkg);
    PSArchiveDetail a = new PSArchiveDetail(desc);
    List<PSDatasourceMap> infoList = new ArrayList<>();
    infoList.add(new PSDatasourceMap("ds1", "ds2"));
    a.setDbmsInfoList(pkg, infoList);

    PSArchiveDetail b = new PSArchiveDetail(newExportDesc(newPkg("pkg1", "Package One")));
    b.copyFrom(a);

    Iterator<PSDatasourceMap> it = b.getExternalDbmsList(pkg);
    assertTrue(it.hasNext());
    assertEquals("ds1", it.next().getSrc());
  }

  @Test
  public void testEmptyDetailsEqual() {
    PSDeployableElement pkg = newPkg("pkg1", "Package One");
    PSExportDescriptor desc = newExportDesc(pkg);
    assertEquals(new PSArchiveDetail(desc), new PSArchiveDetail(desc));
  }
}
