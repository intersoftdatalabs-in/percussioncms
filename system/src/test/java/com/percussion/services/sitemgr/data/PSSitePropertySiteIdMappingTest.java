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

package com.percussion.services.sitemgr.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.guidmgr.data.PSGuid;
import com.percussion.services.virtualsite.PSVirtualSiteHelper;
import com.percussion.utils.guid.IPSGuid;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import java.lang.reflect.Field;
import org.junit.jupiter.api.Test;

/**
 * {@code RXASSEMBLERPROPERTIES.SITEID} must be written on INSERT for Create Site and Developer
 * Virtual Site save (#3511 / #3521). Child owns the FK; parent properties are {@code mappedBy}.
 */
class PSSitePropertySiteIdMappingTest {

  @Test
  void siteJoinColumnIsInsertable() throws Exception {
    Field site = PSSiteProperty.class.getDeclaredField("site");
    JoinColumn join = site.getAnnotation(JoinColumn.class);
    assertNotNull(join, "SITEID JoinColumn");
    assertTrue(join.insertable(), "SITEID must be written on INSERT");
    assertTrue(join.updatable(), "SITEID must be writable on UPDATE");
    assertFalse(join.nullable(), "SITEID is NOT NULL");
    assertTrue("SITEID".equalsIgnoreCase(join.name()));
  }

  @Test
  void parentPropertiesAreMappedBySite() throws Exception {
    Field properties = PSSite.class.getDeclaredField("properties");
    OneToMany otm = properties.getAnnotation(OneToMany.class);
    assertNotNull(otm, "PSSite.properties OneToMany");
    assertEquals("site", otm.mappedBy(), "child PSSiteProperty.site must own SITEID");
    assertNull(
        properties.getAnnotation(JoinColumn.class),
        "parent must not also map SITEID (Hibernate 6 omits FK on INSERT)");
  }

  @Test
  void putPropertyOnExistingSitePopulatesSiteId() {
    PSSite site = new PSSite();
    site.setGUID(new PSGuid(PSTypeEnum.SITE, 100L));
    IPSGuid ctx = new PSGuid(PSTypeEnum.CONTEXT, 1L);

    PSVirtualSiteHelper.putProperty(
        site, ctx, PSVirtualSiteHelper.PROP_SOURCE_KIND, "git-filesystem");
    PSVirtualSiteHelper.putProperty(
        site, ctx, PSVirtualSiteHelper.PROP_ROOT_PATH, "C:/docs/product-docs");

    assertFalse(site.getProperties().isEmpty());
    for (PSSiteProperty property : site.getProperties()) {
      assertSame(site, property.getSite(), property.getName());
      assertEquals(100L, ((PSSite) property.getSite()).getSiteId());
    }
  }

  @Test
  void addPropertyLinksSiteWhenMissing() {
    PSSite site = new PSSite();
    site.setSiteId(42L);
    PSSiteProperty property = new PSSiteProperty();
    property.setPropertyId(1L);
    property.setName(PSVirtualSiteHelper.PROP_SOURCE_KIND);
    property.setValue("git-filesystem");
    property.setContextId(new PSGuid(PSTypeEnum.CONTEXT, 1L));

    site.addProperty(property);

    assertSame(site, property.getSite());
    assertEquals(42L, ((PSSite) property.getSite()).getSiteId());
  }
}
