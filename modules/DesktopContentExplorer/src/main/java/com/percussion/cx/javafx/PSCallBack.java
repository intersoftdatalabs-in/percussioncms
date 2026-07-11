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

package com.percussion.cx.javafx;

import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.web.PopupFeatures;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebEvent;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import javafx.util.Callback;

/**
 * Callback handler for JavaFX WebView popup windows. Implements the PopupFeatures callback
 * interface to create and manage popup browser windows with custom dimensions and behavior.
 *
 * @since 8.0.0
 */
public final class PSCallBack implements Callback<PopupFeatures, WebEngine> {
  double width;
  double height;
  Stage popupStage = new Stage();

  private WebView popupWebView;

  private WebEngine engine;

  /**
   * Creates a new popup callback handler.
   *
    * @param popupWebView the WebView to use for the popup window
    * @param width the width of the popup window
    * @param heigth the height of the popup window
   */
  public PSCallBack(WebView popupWebView, double width, double heigth) {

    this.popupWebView = popupWebView;
    this.engine = popupWebView.getEngine();
    this.height = heigth;
    this.width = width;
  }

  /**
   * Creates a new popup WebEngine for the given popup features.
   *
   * @param popupFeatures the popup configuration features
   * @return the WebEngine for the popup window
   */
  @Override
  public WebEngine call(PopupFeatures popupFeatures) {

    Scene popupScene = null;
    if (popupWebView.getScene() != null) {
      try {
        Thread.sleep(200);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }
    if (popupWebView.getScene() == null) {
      popupScene = new Scene(getPopupWebView());
      getPopupWebView().prefWidthProperty().bind(popupScene.widthProperty());
      getPopupWebView().prefHeightProperty().bind(popupScene.heightProperty());
    }
    popupStage.setScene(popupWebView.getScene());
    popupStage.setResizable(popupFeatures.isResizable());
    popupStage.setWidth(width);
    popupStage.setHeight(height);
    popupStage.show();
    return getPopupWebView().getEngine();
  }

  /** Sets up an event handler to close the popup stage when it becomes invisible. */
  public void setCloseEvent() {
    engine.setOnVisibilityChanged(
        new EventHandler<WebEvent<Boolean>>() {
          @Override
          public void handle(final WebEvent<Boolean> event) {

            // if event Data is set to false, means not visible.
            if (!event.getData()) {
              popupStage.close();
            }
          }
        });
  }

  /**
   * Sets the WebView to use for the popup window.
   *
   * @param popupWebView the WebView to set
   */
  public void setPopupWebView(WebView popupWebView) {
    this.popupWebView = popupWebView;
  }

  /**
   * Gets the WebEngine associated with this callback.
   *
   * @return the WebEngine instance
   */
  public WebEngine getEngine() {
    return engine;
  }

  /**
   * Sets the WebEngine to use.
   *
   * @param engine the WebEngine to set
   */
  public void setEngine(WebEngine engine) {
    this.engine = engine;
  }

  /**
   * Gets the popup WebView.
   *
   * @return the popup WebView
   */
  public WebView getPopupWebView() {
    return popupWebView;
  }
}
