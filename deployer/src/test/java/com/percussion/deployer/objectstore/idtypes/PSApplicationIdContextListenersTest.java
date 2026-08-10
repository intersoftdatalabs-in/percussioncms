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

package com.percussion.deployer.objectstore.idtypes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.design.objectstore.PSUnknownNodeTypeException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/** Coverage for typed listener list on {@link PSApplicationIdContext} (issue #2697). */
public class PSApplicationIdContextListenersTest {

  /** Minimal concrete context that records listener notifications. */
  private static final class ListeningCtx extends PSApplicationIdContext {
    private final AtomicInteger updates = new AtomicInteger();
    private final AtomicReference<PSApplicationIdContext> last = new AtomicReference<>();

    @Override
    public String getDisplayText() {
      return "listening";
    }

    @Override
    public Element toXml(Document doc) {
      throw new UnsupportedOperationException("not needed");
    }

    @Override
    public void fromXml(Element sourceNode) throws PSUnknownNodeTypeException {
      throw new UnsupportedOperationException("not needed");
    }

    @Override
    protected void ctxValueUpdated(PSApplicationIdContext ctx) {
      updates.incrementAndGet();
      last.set(ctx);
    }

    int getUpdates() {
      return updates.get();
    }

    PSApplicationIdContext getLast() {
      return last.get();
    }
  }

  /** Concrete context that keeps base {@link #ctxValueUpdated} behavior. */
  private static final class BareCtx extends PSApplicationIdContext {
    @Override
    public String getDisplayText() {
      return "bare";
    }

    @Override
    public Element toXml(Document doc) {
      throw new UnsupportedOperationException("not needed");
    }

    @Override
    public void fromXml(Element sourceNode) throws PSUnknownNodeTypeException {
      throw new UnsupportedOperationException("not needed");
    }

    void invokeBaseCtxValueUpdated(PSApplicationIdContext ctx) {
      super.ctxValueUpdated(ctx);
    }
  }

  @Test
  public void testNotifyTypedListeners() {
    ListeningCtx source = new ListeningCtx();
    ListeningCtx listener = new ListeningCtx();
    source.addCtxChangeListener(listener);
    // self is not added
    source.addCtxChangeListener(source);

    source.notifyCtxChangeListeners(source);
    assertEquals(1, listener.getUpdates());
    assertSame(source, listener.getLast());

    source.removeCtxChangeListener(listener);
    source.notifyCtxChangeListeners(source);
    assertEquals(1, listener.getUpdates());
  }

  @Test
  public void testBaseCtxValueUpdatedRejectsNullAndThrows() {
    BareCtx bare = new BareCtx();
    assertThrows(IllegalArgumentException.class, () -> bare.invokeBaseCtxValueUpdated(null));
    UnsupportedOperationException ex =
        assertThrows(
            UnsupportedOperationException.class, () -> bare.invokeBaseCtxValueUpdated(bare));
    assertTrue(ex.getMessage().contains("not implemented"));
  }
}
