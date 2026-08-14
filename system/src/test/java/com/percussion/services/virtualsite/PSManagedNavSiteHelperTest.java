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
package com.percussion.services.virtualsite;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.percussion.services.sitemgr.data.PSSite;
import com.percussion.services.sitemgr.data.PSSiteProperty;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class PSManagedNavSiteHelperTest {

  @Test
  void createFlagNullOrTrueMeansIncludeNav() {
    assertTrue(PSManagedNavSiteHelper.wantsManagedNavigation((Boolean) null));
    assertTrue(PSManagedNavSiteHelper.wantsManagedNavigation(Boolean.TRUE));
    assertFalse(PSManagedNavSiteHelper.wantsManagedNavigation(Boolean.FALSE));
  }

  @Test
  void traditionalDefaultsToManagedNav() {
    PSSite site = mock(PSSite.class);
    when(site.getProperties()).thenReturn(Set.of());
    assertTrue(PSManagedNavSiteHelper.wantsManagedNavigation(site));
    assertTrue(PSManagedNavSiteHelper.flagForNonVirtual(site));
  }

  @Test
  void traditionalFalsePropertySkipsNav() {
    PSSite site = mock(PSSite.class);
    PSSiteProperty prop = new PSSiteProperty();
    prop.setName(PSManagedNavSiteHelper.PROP_MANAGED);
    prop.setValue("false");
    when(site.getProperties()).thenReturn(Set.of(prop));
    assertFalse(PSManagedNavSiteHelper.wantsManagedNavigation(site));
    assertFalse(PSManagedNavSiteHelper.flagForNonVirtual(site));
  }

  @Test
  void virtualOmitsFlagAndNeverWantsCmsNav() {
    PSSite site = mock(PSSite.class);
    PSSiteProperty kind = new PSSiteProperty();
    kind.setName(PSVirtualSiteHelper.PROP_SOURCE_KIND);
    kind.setValue("git-filesystem");
    PSSiteProperty root = new PSSiteProperty();
    root.setName(PSVirtualSiteHelper.PROP_ROOT_PATH);
    root.setValue("C:/docs");
    Set<PSSiteProperty> props = new HashSet<>();
    props.add(kind);
    props.add(root);
    when(site.getProperties()).thenReturn(props);

    assertTrue(PSVirtualSiteHelper.isVirtual(site));
    assertFalse(PSManagedNavSiteHelper.wantsManagedNavigation(site));
    assertNull(PSManagedNavSiteHelper.flagForNonVirtual(site));
  }

  @Test
  void parseFlagAcceptsCommonFalseTokens() {
    assertFalse(PSManagedNavSiteHelper.parseFlag("false", true));
    assertFalse(PSManagedNavSiteHelper.parseFlag("0", true));
    assertFalse(PSManagedNavSiteHelper.parseFlag("no", true));
    assertTrue(PSManagedNavSiteHelper.parseFlag("yes", false));
    assertTrue(PSManagedNavSiteHelper.parseFlag("bogus", true));
  }
}
