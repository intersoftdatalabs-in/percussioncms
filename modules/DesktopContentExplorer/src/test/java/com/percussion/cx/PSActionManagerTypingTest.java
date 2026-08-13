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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.cx.objectstore.PSMenuAction;
import com.percussion.cx.objectstore.PSParameters;
import com.percussion.utils.collections.PSIteratorUtils;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Behavioral tests for typed PSActionManager adapters after remaining rawtypes cleanup (#3286 /
 * parent #2045). Does not construct the Swing applet (requires live server resources).
 */
public class PSActionManagerTypingTest {

  @Test
  public void asParamKeysIteratesAllStringKeys() {
    PSParameters params = new PSParameters();
    params.setParameter("sys_revision", "$sys_revision");
    params.setParameter("static", "x");

    Set<String> keys = new HashSet<>();
    PSActionManager.asParamKeys(params).forEachRemaining(keys::add);

    assertEquals(Set.of("sys_revision", "static"), keys);
  }

  @Test
  public void asParamKeysNullIsEmpty() {
    assertFalse(PSActionManager.asParamKeys(null).hasNext());
  }

  @Test
  public void setMenuChildrenAndAsMenuActionsRoundTrip() {
    PSMenuAction parent =
        new PSMenuAction(
            "parent",
            "Parent",
            PSMenuAction.TYPE_MENU,
            "",
            PSMenuAction.HANDLER_CLIENT,
            0);
    PSMenuAction child = new PSMenuAction("child", "Child");

    PSActionManager.setMenuChildren(parent, PSIteratorUtils.iterator(child));

    Iterator<PSMenuAction> it = PSActionManager.asMenuActions(parent);
    assertTrue(it.hasNext());
    assertEquals("child", it.next().getName());
    assertFalse(it.hasNext());
  }

  @Test
  public void asMenuActionsNullIsEmpty() {
    assertFalse(PSActionManager.asMenuActions(null).hasNext());
  }

  @Test
  public void setMenuChildrenReplacesExistingChildren() {
    PSMenuAction parent =
        new PSMenuAction(
            "parent",
            "Parent",
            PSMenuAction.TYPE_MENU,
            "",
            PSMenuAction.HANDLER_CLIENT,
            0);
    PSActionManager.setMenuChildren(parent, PSIteratorUtils.iterator(new PSMenuAction("a", "A")));
    PSActionManager.setMenuChildren(parent, PSIteratorUtils.iterator(new PSMenuAction("b", "B")));

    List<String> names = new ArrayList<>();
    PSActionManager.asMenuActions(parent).forEachRemaining(a -> names.add(a.getName()));
    assertEquals(List.of("b"), names);
  }
}
