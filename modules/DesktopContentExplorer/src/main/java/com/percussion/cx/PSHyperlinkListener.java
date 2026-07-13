/*
 * Copyright 1999-2023 Percussion Software, Inc.
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

import java.awt.Desktop;
import java.net.URI;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Worker.State;
import javafx.scene.web.WebView;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.events.Event;
import org.w3c.dom.events.EventListener;
import org.w3c.dom.events.EventTarget;
import org.w3c.dom.html.HTMLAnchorElement;

/**
 * Listener that intercepts anchor clicks in the embedded header web view and opens the target URL
 * in the system browser instead of navigating within the embedded view.
 */
public class PSHyperlinkListener implements ChangeListener<State>, EventListener {
  private static Logger log = LogManager.getLogger(PSHyperlinkListener.class);

  private static final String CLICK_EVENT = "click";
  private static final String ANCHOR_TAG = "a";

  private final WebView webView;

  /**
   * Constructs the listener for the given web view.
   *
   * @param webView the web view whose anchor clicks will be intercepted, may not be
   *     <code>null</code>.
   */
  public PSHyperlinkListener(WebView webView) {
    this.webView = webView;
  }

  @Override
  public void changed(ObservableValue<? extends State> observable, State oldValue, State newValue) {
    if (State.SUCCEEDED.equals(newValue)) {
      Document document = webView.getEngine().getDocument();
      NodeList anchors = document.getElementsByTagName(ANCHOR_TAG);
      for (int i = 0; i < anchors.getLength(); i++) {
        Node node = anchors.item(i);
        EventTarget eventTarget = (EventTarget) node;
        eventTarget.addEventListener(CLICK_EVENT, this, false);
      }
    }
  }

  @Override
  public void handleEvent(Event event) {
    HTMLAnchorElement anchorElement = (HTMLAnchorElement) event.getCurrentTarget();
    openLinkInBrowser(anchorElement.getHref());

    event.preventDefault();
  }

  private void openLinkInSystemBrowser(String url) {
    log.debug(String.format("Opening link '{0}' in default system browser.", url));

    try {
      URI uri = new URI(url);
      Desktop.getDesktop().browse(uri);
    } catch (Throwable e) {
      log.error(String.format("Error on opening link '{0}' in system browser.", url), e);
    }
  }

  private void openLinkInBrowser(String href) {
    if (Desktop.isDesktopSupported()) {
      openLinkInSystemBrowser(href);
    } else {
      log.warn(
          String.format(
              "OS does not support desktop operations like browsing. Cannot open link '{0}'.",
              href));
    }
  }
}
