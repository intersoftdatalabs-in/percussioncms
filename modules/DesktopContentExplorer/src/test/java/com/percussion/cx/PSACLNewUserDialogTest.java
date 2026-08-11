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
package com.percussion.cx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.cms.objectstore.PSSecurityProviderInstanceSummary;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Behavioral tests for typed provider grouping helpers on {@link PSACLNewUserDialog} after rawtypes
 * cleanup (#2939). Does not open the Swing dialog (requires live applet resources).
 */
public class PSACLNewUserDialogTest {

  @Test
  public void groupProvidersByTypeNullIteratorReturnsEmpty() {
    List<PSACLNewUserDialog.ProviderType> groups =
        PSACLNewUserDialog.groupProvidersByType(null);
    assertTrue(groups.isEmpty());
  }

  @Test
  public void groupProvidersByTypeEmptyIteratorReturnsEmpty() {
    List<PSACLNewUserDialog.ProviderType> groups =
        PSACLNewUserDialog.groupProvidersByType(Collections.emptyIterator());
    assertTrue(groups.isEmpty());
  }

  @Test
  public void groupProvidersByTypeAddsInstancePerProvider() {
    PSSecurityProviderInstanceSummary a = summary(1, "Directory", "ldap-a");
    PSSecurityProviderInstanceSummary b = summary(2, "WebServer", "ws-b");

    List<PSSecurityProviderInstanceSummary> providers = new ArrayList<>();
    providers.add(a);
    providers.add(b);

    List<PSACLNewUserDialog.ProviderType> groups =
        PSACLNewUserDialog.groupProvidersByType(providers.iterator());

    // Historic ProviderType.equals uses Object.equals super, so distinct ProviderType objects
    // never match — each provider becomes its own group (preserves pre-generics dialog behavior).
    assertEquals(2, groups.size());
    assertEquals(1, groups.get(0).getInstances().size());
    assertSame(a, groups.get(0).getInstances().get(0));
    assertEquals("Directory", groups.get(0).toString());
    assertEquals(1, groups.get(1).getInstances().size());
    assertSame(b, groups.get(1).getInstances().get(0));
    assertEquals("WebServer", groups.get(1).toString());
  }

  @Test
  public void providerTypeGetInstanceFindsByName() {
    PSSecurityProviderInstanceSummary a = summary(1, "Directory", "ldap-a");
    PSSecurityProviderInstanceSummary b = summary(1, "Directory", "ldap-b");
    PSACLNewUserDialog.ProviderType type = new PSACLNewUserDialog.ProviderType(1, "Directory");
    type.addInstance(a);
    type.addInstance(b);
    type.addInstance(null); // ignored

    assertSame(a, type.getInstance("ldap-a"));
    assertSame(b, type.getInstance("ldap-b"));
    assertNull(type.getInstance("missing"));
  }

  @Test
  public void providerTypeGetInstanceRejectsNullName() {
    PSACLNewUserDialog.ProviderType type = new PSACLNewUserDialog.ProviderType(1, "Directory");
    assertThrows(IllegalArgumentException.class, () -> type.getInstance(null));
  }

  @Test
  public void providerTypeGetInstancesIsMutableListBackingField() {
    PSACLNewUserDialog.ProviderType type = new PSACLNewUserDialog.ProviderType(1, "Directory");
    assertTrue(type.getInstances().isEmpty());
    type.addInstance(summary(1, "Directory", "x"));
    assertEquals(1, type.getInstances().size());
    Iterator<PSSecurityProviderInstanceSummary> it = type.getInstances().iterator();
    assertTrue(it.hasNext());
  }

  private static PSSecurityProviderInstanceSummary summary(
      int typeId, String typeName, String instanceName) {
    try {
      Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
      Element root = doc.createElement(PSSecurityProviderInstanceSummary.XML_ELEMENT_ROOT);
      root.setAttribute(PSSecurityProviderInstanceSummary.XML_ATTRIB_TYPEID, String.valueOf(typeId));
      root.setAttribute(PSSecurityProviderInstanceSummary.XML_ATTRIB_TYPENAME, typeName);
      Element name = doc.createElement(PSSecurityProviderInstanceSummary.XML_ELEMENT_NAME);
      name.appendChild(doc.createTextNode(instanceName));
      root.appendChild(name);
      return new PSSecurityProviderInstanceSummary(root);
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }
}
