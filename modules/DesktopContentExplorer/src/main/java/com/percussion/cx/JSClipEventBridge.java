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

/**
 * JavaScript-accessible bridge that models a clipboard {@code ClipboardEvent}. It exposes the
 * event's {@code clipboardData} payload along with the {@code preventDefault} / propagation
 * semantics expected by the TinyMCE paste handler running inside the embedded browser.
 */
public class JSClipEventBridge {

  private boolean defaultPrevented = false;
  private boolean isImmediatePropagationStopped = false;

  // public for JS to see
  /**
   * The clipboard payload associated with this event, initialized lazily from the system clipboard
   * by the no-arg constructor. Exposed to JavaScript.
   */
  public JSClipDataBridge clipboardData;

  /**
   * Constructs a new event bridge and initializes {@link #clipboardData} from the system clipboard.
   */
  public JSClipEventBridge() {
    this.clipboardData = new JSClipDataBridge();
  }

  /**
   * Returns whether {@link #preventDefault()} has been invoked on this event.
   *
   * @return {@code true} if the default action has been prevented; {@code false} otherwise.
   */
  public boolean isDefaultPrevented() {
    return this.defaultPrevented;
  }

  /**
   * Marks the default action for this event as prevented, mirroring the DOM {@code
   * Event.preventDefault()} contract.
   */
  public void preventDefault() {
    defaultPrevented = true;
  }

  /**
   * Returns whether immediate propagation of this event has been stopped.
   *
   * @return {@code true} if immediate propagation was stopped; {@code false} otherwise.
   */
  public boolean isImmediatePropagationStopped() {
    return isImmediatePropagationStopped;
  }
}
