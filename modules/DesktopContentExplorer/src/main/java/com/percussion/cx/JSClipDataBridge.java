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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import javafx.scene.input.DataFormat;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Base model to represent a DataTransfer object used as the clipboardData propecty of a
 * ClipboardEvent. This allows us to Create a fake EventObject to pass to the paste event of tinymce
 * and populate from JavaFx Clipboard. The fields are public and are represented as an array to map
 * to DataTransfer.items and DataTransfer.types when the Object is mapped to a JSObject.
 * https://developer.mozilla.org/en-US/docs/Web/API/ClipboardEvent/clipboardData
 *
 * @author stephenbolton
 */
public class JSClipDataBridge {
  static Logger log = LogManager.getLogger(JSClipDataBridge.class);

  /**
   * The MIME types currently held on the clipboard, mirrored from
   * {@link javafx.scene.input.Clipboard#getContentTypes()}. Maps to
   * {@code DataTransfer.types} when this object is exposed to JavaScript.
   */
  public String[] types = new String[0];

  /**
   * The individual clipboard entries, one per supported MIME type. Maps to
   * {@code DataTransfer.items} when this object is exposed to JavaScript.
   */
  public JSClipDataItem[] items = new JSClipDataItem[0];
  private static volatile boolean isInit = false;

  /**
   * Constructs a new bridge and seeds the public {@code types} and {@code items} fields from
   * the current system {@link javafx.scene.input.Clipboard} (HTML and plain text content).
   */
  public JSClipDataBridge() {
    javafx.scene.input.Clipboard clipboardFx = javafx.scene.input.Clipboard.getSystemClipboard();

    log.debug("Current Types in clipboard = " + clipboardFx.getContentTypes());

    try {

      if (clipboardFx.hasHtml()) {
        this.setData("text/html", clipboardFx.getHtml());
      }

      if (clipboardFx.hasString()) {
        this.setData("text/plain", clipboardFx.getString());
      }
    } catch (Exception e) {
      log.error("Failed getting clipboard data", e);
    }
    isInit = true;
  }

  /**
   * Returns the MIME types currently held by this bridge.
   *
   * @return the supported clipboard types; never {@code null}, but may be empty.
   */
  public String[] getTypes() {
    return types;
  }

  /**
   * Returns the data previously stored for the supplied MIME type, if present.
   *
   * @param type the MIME type to look up, e.g. {@code text/html} or {@code text/plain}.
   * @return the value associated with {@code type}, or {@code null} if no entry exists.
   */
  public String getData(String type) {
    return Arrays.stream(items)
        .filter(i -> i.getType().equals(type))
        .findFirst()
        .map(JSClipDataItem::getAsString)
        .orElse(null);
  }

  /**
   * Stores a value for the supplied MIME type, replacing any existing entry of the same type,
   * and propagates the change to the system clipboard (after initialization).
   *
   * @param type the MIME type to store, currently only {@code text/html} and {@code text/plain}
   *     are supported; any other type is ignored.
   * @param value the string value to associate with {@code type}.
   */
  public void setData(String type, String value) {

    if (!type.equals("text/html") && !type.equals("text/plain")) {
      log.debug("Unsupported clipboard type " + type);
      return;
    }

    JSClipDataItem newItem = new JSClipDataItem(type, value);
    ArrayList<JSClipDataItem> list =
        Arrays.stream(items)
            .filter(i -> !i.getType().equals(type))
            .collect(Collectors.toCollection(ArrayList<JSClipDataItem>::new));
    list.add(newItem);
    items = list.toArray(new JSClipDataItem[list.size()]);
    ArrayList<String> typesList =
        Arrays.stream(types)
            .filter(t -> !t.equals(type))
            .collect(Collectors.toCollection(ArrayList<String>::new));
    typesList.add(type);
    types = typesList.toArray(new String[typesList.size()]);
    if (isInit) setClipboardData(this);
  }

  /**
   * Clears all clipboard data held by this bridge and empties the system clipboard.
   */
  public void clearData() {
    items = new JSClipDataItem[0];
    types = new String[0];
    javafx.scene.input.Clipboard clipboardFx = javafx.scene.input.Clipboard.getSystemClipboard();
    clipboardFx.clear();
  }

  private void setClipboardData(JSClipDataBridge dataBridge) {

    javafx.scene.input.Clipboard clipboardFx = javafx.scene.input.Clipboard.getSystemClipboard();

    Map<DataFormat, Object> content = new HashMap<>();
    for (JSClipDataItem item : dataBridge.items) {
      DataFormat df = DataFormat.lookupMimeType(item.getType());
      if (df == null) {
        log.error("Skipping invalid mime type in clipboard data " + item.getType());
      }
      if (df.equals(DataFormat.HTML) || df.equals(DataFormat.PLAIN_TEXT)) {
        content.put(df, item.getAsString());
      } else {
        log.debug("Cannot handle mime type in clipboard data " + item.getType());
      }
    }
    boolean success = clipboardFx.setContent(content);
    if (!success) log.error("Could not add content to clipboard");
  }

  @Override
  public String toString() {
    return "ClipDataBridge{" + "items=" + Arrays.toString(items) + '}';
  }
}
