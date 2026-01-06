/*
 * Copyright 1999-2025 Percussion Software, Inc.
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

// REFACTORED: CP-JAVA11

package com.percussion.cx;

import com.percussion.cx.javafx.PSDesktopExplorerWindow;
import com.percussion.cx.javafx.PSFileSaver;
import com.percussion.cx.javafx.PSWindowManager;
import netscape.javascript.JSObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.Transferable;
import java.util.concurrent.CountDownLatch;
import netscape.javascript.JSObject;
import org.apache.log4j.Logger;

public class PSJavaBridge implements ClipboardOwner {

   static Logger log = LogManager.getLogger(PSJavaBridge.class);

   final CountDownLatch initialized = new CountDownLatch(1);

   private final PSDesktopExplorerWindow frame;

   public PSJavaBridge(PSDesktopExplorerWindow frame)
   {
      this.frame = frame;
   }
