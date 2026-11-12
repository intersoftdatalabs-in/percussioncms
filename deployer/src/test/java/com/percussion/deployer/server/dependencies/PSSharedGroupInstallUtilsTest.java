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

package com.percussion.deployer.server.dependencies;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.percussion.deployer.objectstore.PSDependencyFile;
import com.percussion.deployer.objectstore.PSTransactionSummary;
import com.percussion.deployer.server.dependencies.PSSharedGroupInstallUtils.SharedGroupDefEntry;
import com.percussion.deployer.server.dependencies.PSSharedGroupInstallUtils.SharedGroupInstallTarget;
import com.percussion.design.objectstore.PSContentEditorSharedDef;
import com.percussion.design.objectstore.PSSharedFieldGroup;
import com.percussion.util.PSCollection;
import java.io.File;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link PSSharedGroupInstallUtils} (issue #6133 residual of #6058).
 *
 * <p>The def-file match vs. create branches of {@code installDependencyFiles} were extracted here
 * so the merge behavior is locked without a live CMS.
 */
public class PSSharedGroupInstallUtilsTest {

  @Test
  public void findSharedGroupXmlFileReturnsFirstMatch() {
    PSDependencyFile other =
        new PSDependencyFile(PSDependencyFile.TYPE_SUPPORT_FILE, new File("other.dat"));
    PSDependencyFile first =
        new PSDependencyFile(PSDependencyFile.TYPE_SHARED_GROUP_XML, new File("a.xml"));
    PSDependencyFile second =
        new PSDependencyFile(PSDependencyFile.TYPE_SHARED_GROUP_XML, new File("b.xml"));

    assertSame(
        first,
        PSSharedGroupInstallUtils.findSharedGroupXmlFile(
            List.of(other, first, second).iterator()));
  }

  @Test
  public void findSharedGroupXmlFileReturnsNullWhenAbsent() {
    PSDependencyFile other =
        new PSDependencyFile(PSDependencyFile.TYPE_SUPPORT_FILE, new File("other.dat"));

    assertNull(
        PSSharedGroupInstallUtils.findSharedGroupXmlFile(List.of(other).iterator()));
    assertNull(
        PSSharedGroupInstallUtils.findSharedGroupXmlFile(
            List.<PSDependencyFile>of().iterator()));
  }

  @Test
  public void replaceGroupInCollectionSwapsSameNamedGroup() {
    PSSharedFieldGroup oldB = group("B");
    PSCollection<PSSharedFieldGroup> col = collectionOf(group("A"), oldB);
    PSSharedFieldGroup newB = group("B");

    PSSharedGroupInstallUtils.replaceGroupInCollection(col, newB);

    assertEquals(2, col.size());
    assertEquals("A", col.get(0).getName());
    assertSame(newB, col.get(1));
  }

  @Test
  public void replaceGroupInCollectionAppendsUnknownGroup() {
    PSCollection<PSSharedFieldGroup> col = collectionOf(group("A"));

    PSSharedFieldGroup added = group("B");
    PSSharedGroupInstallUtils.replaceGroupInCollection(col, added);

    assertEquals(2, col.size());
    assertSame(added, col.get(1));
  }

  @Test
  public void replaceGroupInCollectionRejectsNulls() {
    assertThrows(
        IllegalArgumentException.class,
        () -> PSSharedGroupInstallUtils.replaceGroupInCollection(null, group("A")));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            PSSharedGroupInstallUtils.replaceGroupInCollection(
                collectionOf(group("A")), null));
  }

  @Test
  public void resolveInstallTargetReplacesInOwnerDef() {
    PSContentEditorSharedDef ownerDef = defOf(group("A"), group("B"));
    SharedGroupDefEntry owner = new SharedGroupDefEntry(new File("owner.xml"), ownerDef);
    SharedGroupDefEntry match = new SharedGroupDefEntry(new File("orig.xml"), defOf(group("C")));
    PSSharedFieldGroup incoming = group("B");

    SharedGroupInstallTarget target =
        PSSharedGroupInstallUtils.resolveInstallTarget(
            match, owner, new File("orig.xml"), incoming);

    assertSame(ownerDef, target.def());
    assertEquals(new File("owner.xml"), target.file());
    assertEquals(PSTransactionSummary.ACTION_MODIFIED, target.action());
    assertEquals(2, target.fieldGroups().size());
    assertEquals("A", target.fieldGroups().get(0).getName());
    assertSame(incoming, target.fieldGroups().get(1));
  }

  @Test
  public void resolveInstallTargetAppendsToMatchDef() {
    PSContentEditorSharedDef matchDef = defOf(group("A"));
    SharedGroupDefEntry match = new SharedGroupDefEntry(new File("orig.xml"), matchDef);
    PSSharedFieldGroup incoming = group("B");

    SharedGroupInstallTarget target =
        PSSharedGroupInstallUtils.resolveInstallTarget(
            match, null, new File("orig.xml"), incoming);

    assertSame(matchDef, target.def());
    assertEquals(new File("orig.xml"), target.file());
    assertEquals(PSTransactionSummary.ACTION_MODIFIED, target.action());
    assertEquals(2, target.fieldGroups().size());
    assertSame(incoming, target.fieldGroups().get(1));
  }

  @Test
  public void resolveInstallTargetCreatesNewDef() {
    File origFile = new File("orig.xml");
    PSSharedFieldGroup incoming = group("B");

    SharedGroupInstallTarget target =
        PSSharedGroupInstallUtils.resolveInstallTarget(null, null, origFile, incoming);

    assertEquals(PSTransactionSummary.ACTION_CREATED, target.action());
    assertEquals(origFile, target.file());
    assertEquals(1, target.fieldGroups().size());
    assertSame(incoming, target.fieldGroups().get(0));
    assertSame(incoming, target.def().getSharedGroup("B"));
  }

  @Test
  public void resolveInstallTargetRejectsNullGroup() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            PSSharedGroupInstallUtils.resolveInstallTarget(
                null, null, new File("o.xml"), null));
  }

  @Test
  public void entriesRejectNulls() {
    assertThrows(
        IllegalArgumentException.class, () -> new SharedGroupDefEntry(null, defOf(group("A"))));
    assertThrows(
        IllegalArgumentException.class,
        () -> new SharedGroupDefEntry(new File("a.xml"), null));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new SharedGroupInstallTarget(
                null, new File("a.xml"), collectionOf(group("A")),
                PSTransactionSummary.ACTION_MODIFIED));
  }

  private static PSSharedFieldGroup group(String name) {
    return new PSSharedFieldGroup(name, name + ".xml");
  }

  private static PSCollection<PSSharedFieldGroup> collectionOf(PSSharedFieldGroup... groups) {
    PSCollection<PSSharedFieldGroup> col = new PSCollection<>(PSSharedFieldGroup.class);
    for (PSSharedFieldGroup group : groups) {
      col.add(group);
    }
    return col;
  }

  private static PSContentEditorSharedDef defOf(PSSharedFieldGroup... groups) {
    return new PSContentEditorSharedDef(collectionOf(groups));
  }
}
