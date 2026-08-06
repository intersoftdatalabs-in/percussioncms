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

package com.percussion.services.assembly.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Package deploy of perc.nav slots failed with ContentType source ID 0 during association id
 * transforms. Ensure package-shaped slot XML restores non-zero content type / template ids.
 */
class PSTemplateSlotXmlRestoreTest {

  /** Minimal copy of perc.nav.image.slotDef from perc.nav.ppkg. */
  private static final String PERC_NAV_IMAGE_SLOT =
      """
      <?xml version="1.0" encoding="utf-8"?>  <template-slot id="1">
          <guid>0-5-513</guid>
          <description>navigation image for rollovers</description>
          <finder-arguments/>
          <finder-name>Java/global/percussion/slotcontentfinder/sys_RelationshipContentFinder</finder-name>
          <label>Nav Image</label>
          <name>perc.nav.image</name>
          <relationship-name>ActiveAssembly</relationship-name>
          <slot-type-associations>
            <slot-type-association id="2">
              <slotid>513</slotid>
              <templateid>550</templateid>
              <contenttypeid>313</contenttypeid>
            </slot-type-association>
          </slot-type-associations>
          <slottype>0</slottype>
          <system-slot>false</system-slot>
          <version>2</version>
        </template-slot>
      """;

  @Test
  void fromXmlRestoresSlotTypeAssociationIds() throws Exception {
    PSTemplateSlot slot = new PSTemplateSlot();
    slot.fromXML(PERC_NAV_IMAGE_SLOT);

    PSTemplateTypeSlotAssociation[] assocs = slot.getSlotTypeAssociations();
    assertTrue(assocs != null && assocs.length >= 1, "expected at least one slot-type-association");

    boolean found = false;
    for (PSTemplateTypeSlotAssociation a : assocs) {
      if (a.getContentTypeId() == 313L && a.getTemplateId() == 550L) {
        found = true;
        break;
      }
    }
    assertTrue(
        found, "expected association contentTypeId=313 templateId=550; got: " + describe(assocs));
  }

  @Test
  void fromXmlDoesNotProduceZeroContentTypeAssociations() throws Exception {
    PSTemplateSlot slot = new PSTemplateSlot();
    slot.fromXML(PERC_NAV_IMAGE_SLOT);
    for (PSTemplateTypeSlotAssociation a : slot.getSlotTypeAssociations()) {
      assertTrue(
          a.getContentTypeId() != 0L,
          "contentTypeId must not be 0 (deploy maps CT id and fails with source ID 0)");
      assertTrue(a.getTemplateId() != 0L, "templateId must not be 0");
    }
  }

  /**
   * toXML / modern payloads already use hyphenated association field names. fromXML must restore
   * those without depending on the package unhyphenated rewrite.
   */
  @Test
  void fromXmlRestoresHyphenatedAssociationIds() throws Exception {
    String hyphenated =
        """
        <?xml version="1.0" encoding="utf-8"?>
        <template-slot id="1">
          <guid>0-5-513</guid>
          <description>navigation image for rollovers</description>
          <finder-arguments/>
          <finder-name>Java/global/percussion/slotcontentfinder/sys_RelationshipContentFinder</finder-name>
          <label>Nav Image</label>
          <name>perc.nav.image</name>
          <relationship-name>ActiveAssembly</relationship-name>
          <slot-type-associations>
            <slot-type-association id="2">
              <slot-id>513</slot-id>
              <template-id>550</template-id>
              <content-type-id>313</content-type-id>
            </slot-type-association>
          </slot-type-associations>
          <slottype>0</slottype>
          <system-slot>false</system-slot>
          <version>2</version>
        </template-slot>
        """;
    PSTemplateSlot slot = new PSTemplateSlot();
    slot.fromXML(hyphenated);
    PSTemplateTypeSlotAssociation[] assocs = slot.getSlotTypeAssociations();
    assertTrue(assocs != null && assocs.length >= 1, "expected at least one association");
    boolean found = false;
    for (PSTemplateTypeSlotAssociation a : assocs) {
      if (a.getContentTypeId() == 313L && a.getTemplateId() == 550L) {
        found = true;
        break;
      }
    }
    assertTrue(
        found, "expected association contentTypeId=313 templateId=550; got: " + describe(assocs));
  }

  @Test
  void normalizePackageAssociationElementNamesRewritesTags() {
    String in =
        "<slot-type-association><slotid>1</slotid><templateid>2</templateid><contenttypeid>3</contenttypeid></slot-type-association>";
    String out = PSTemplateSlot.normalizePackageAssociationElementNames(in);
    assertTrue(out.contains("<content-type-id>3</content-type-id>"), out);
    assertTrue(out.contains("<template-id>2</template-id>"), out);
    assertTrue(out.contains("<slot-id>1</slot-id>"), out);
  }

  @Test
  void normalizePackageAssociationElementNamesDoesNotTouchAttributeValues() {
    String in =
        "<x name=\"contenttypeid\" templateid=\"keep\"><contenttypeid>9</contenttypeid></x>";
    String out = PSTemplateSlot.normalizePackageAssociationElementNames(in);
    assertTrue(out.contains("name=\"contenttypeid\""), out);
    assertTrue(out.contains("templateid=\"keep\""), out);
    assertTrue(out.contains("<content-type-id>9</content-type-id>"), out);
  }

  @Test
  void normalizePackageAssociationElementNamesLeavesHyphenatedTagsAlone() {
    String in =
        "<slot-type-association><slot-id>1</slot-id><template-id>2</template-id><content-type-id>3</content-type-id></slot-type-association>";
    String out = PSTemplateSlot.normalizePackageAssociationElementNames(in);
    assertEquals(in, out);
  }

  private static String describe(PSTemplateTypeSlotAssociation[] assocs) {
    StringBuilder sb = new StringBuilder();
    for (PSTemplateTypeSlotAssociation a : assocs) {
      sb.append("[ct=")
          .append(a.getContentTypeId())
          .append(",tmp=")
          .append(a.getTemplateId())
          .append(",slot=")
          .append(a.getSlotId())
          .append(']');
    }
    return sb.toString();
  }
}
