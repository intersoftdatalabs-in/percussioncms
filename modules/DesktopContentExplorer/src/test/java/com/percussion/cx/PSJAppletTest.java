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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Image;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.Iterator;
import org.junit.jupiter.api.Test;

/** Behavioral tests for the JDK-24+ {@link PSJApplet} host surface. */
public class PSJAppletTest {

  @Test
  public void constructWithoutStubIsSafe() {
    PSJApplet applet = new PSJApplet();
    assertNull(applet.getStub());
    assertNull(applet.getParameter("any"));
    assertNull(applet.getCodeBase());
    assertNull(applet.getDocumentBase());
    assertNull(applet.getAppletContext());
    assertFalse(applet.isActive());
    assertSame(applet, applet.getContentPane());
  }

  @Test
  public void setStubSuppliesParametersAndCodeBase() throws Exception {
    PSJApplet applet = new PSJApplet();
    RecordingStub stub = new RecordingStub();
    applet.setStub(stub);

    assertSame(stub, applet.getStub());
    assertEquals("swing", applet.getParameter("SWING"));
    assertEquals(URI.create("http://localhost/Rhythmyx/dce/").toURL(), applet.getCodeBase());
    assertEquals(URI.create("http://localhost/Rhythmyx/").toURL(), applet.getDocumentBase());
    assertSame(stub, applet.getAppletContext());
    assertTrue(applet.isActive());
  }

  @Test
  public void lifecycleDefaultsAreNoOps() {
    PSJApplet applet = new PSJApplet();
    applet.init();
    applet.start();
    applet.stop();
    applet.destroy();
  }

  @Test
  public void getImageNullUrlReturnsNull() {
    assertNull(new PSJApplet().getImage(null));
  }

  @Test
  public void contentExplorerAppletDoesNotExtendRemovedJdkApplet() {
    assertTrue(PSJApplet.class.isAssignableFrom(PSContentExplorerApplet.class));
    assertEquals(PSJApplet.class, PSContentExplorerApplet.class.getSuperclass());
    assertTrue(PSAppletStub.class.isAssignableFrom(PSContentExplorerFrame.class));
    assertTrue(PSAppletContext.class.isAssignableFrom(PSContentExplorerAppletStub.class));
  }

  @Test
  public void contentExplorerAppletConstructsWithoutHost() {
    PSContentExplorerApplet applet = new PSContentExplorerApplet(true);
    applet.toggleFlaggedFolder("7", true);
    assertTrue(applet.getFlaggedFolderSet().contains("7"));
  }

  private static final class RecordingStub implements PSAppletStub, PSAppletContext {
    @Override
    public boolean isActive() {
      return true;
    }

    @Override
    public URL getDocumentBase() {
      return url("http://localhost/Rhythmyx/");
    }

    @Override
    public URL getCodeBase() {
      return url("http://localhost/Rhythmyx/dce/");
    }

    @Override
    public String getParameter(String name) {
      return "SWING".equals(name) ? "swing" : null;
    }

    @Override
    public PSAppletContext getAppletContext() {
      return this;
    }

    @Override
    public void appletResize(int width, int height) {
      // unused
    }

    @Override
    public Image getImage(URL url) {
      return null;
    }

    @Override
    public void showDocument(URL url) {
      // unused
    }

    @Override
    public void showDocument(URL url, String target) {
      // unused
    }

    @Override
    public void showStatus(String status) {
      // unused
    }

    @Override
    public InputStream getStream(String key) {
      return null;
    }

    @Override
    public void setStream(String key, InputStream stream) throws IOException {
      // unused
    }

    @Override
    public Iterator<String> getStreamKeys() {
      return null;
    }

    private static URL url(String spec) {
      try {
        return URI.create(spec).toURL();
      } catch (java.net.MalformedURLException e) {
        throw new IllegalStateException(e);
      }
    }
  }
}
