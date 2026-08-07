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
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.cx.objectstore.PSNode;
import java.util.Arrays;
import java.util.Iterator;
import org.junit.jupiter.api.Test;

/** Behavioral tests for typed clipboard selection storage. */
public class PSClipBoardTest {

  @Test
  public void setAndGetClipPreservesNodesAndParent() {
    PSNode parent = new PSNode("p", "Parent", PSNode.TYPE_FOLDER, "url", null, false, 1);
    PSNode child = new PSNode("c", "Child", PSNode.TYPE_ITEM, "", null, false, -1);
    PSUiMode mode = new PSUiMode(PSUiMode.TYPE_VIEW_CX, PSUiMode.TYPE_MODE_NAV);
    PSSelection sel = new PSSelection(mode, parent, Arrays.asList(child).iterator());

    PSClipBoard board = new PSClipBoard();
    board.setClip(PSClipBoard.TYPE_COPY, sel);

    assertSame(sel, board.getClipSelection(PSClipBoard.TYPE_COPY));
    assertSame(parent, board.getClipSource(PSClipBoard.TYPE_COPY));

    Iterator<PSNode> clip = board.getClip(PSClipBoard.TYPE_COPY);
    assertTrue(clip.hasNext());
    assertEquals("c", clip.next().getName());
  }

  @Test
  public void clearDragClipRemovesDragOnly() {
    PSNode parent = new PSNode("p", "Parent", PSNode.TYPE_FOLDER, "url", null, false, 1);
    PSNode child = new PSNode("c", "Child", PSNode.TYPE_ITEM, "", null, false, -1);
    PSUiMode mode = new PSUiMode(PSUiMode.TYPE_VIEW_CX, PSUiMode.TYPE_MODE_NAV);
    PSSelection sel = new PSSelection(mode, parent, Arrays.asList(child).iterator());

    PSClipBoard board = new PSClipBoard();
    board.setClip(PSClipBoard.TYPE_DRAG, sel);
    board.setClip(PSClipBoard.TYPE_COPY, sel);
    board.clearDragClip();

    assertNull(board.getClip(PSClipBoard.TYPE_DRAG));
    assertNull(board.getClipSelection(PSClipBoard.TYPE_DRAG));
    assertTrue(board.getClip(PSClipBoard.TYPE_COPY).hasNext());
  }
}
