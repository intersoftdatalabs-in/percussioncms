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

package com.percussion.extension;

import static org.junit.jupiter.api.Assertions.*;

import com.google.common.io.Files;
import com.percussion.error.PSNonUniqueException;
import com.percussion.error.PSNotFoundException;
import com.percussion.utils.collections.PSIteratorUtils;
import java.io.File;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import org.junit.jupiter.api.Test;

/** Unit tests for the JavaScript extension handler and supporting classes. */
public class PSJavaScriptTest {

  @Test
  public void testAll() throws PSExtensionException, PSNonUniqueException, PSNotFoundException {

    File baseDir = Files.createTempDir();
    File expected = new File(baseDir, RESULT_FILE);
    if (expected.exists()) {
      expected.delete();
    }
    PSExtensionManager mgr = new PSExtensionManager();
    Properties props = new Properties();
    props.setProperty(IPSExtensionHandler.INIT_PARAM_CONFIG_FILENAME, RESULT_FILE);
    mgr.init(baseDir, props);
    System.out.println("Mgr inited successfully");

    String handler = "JavaScript";
    PSExtensionRef ref = PSExtensionRef.handlerRef(handler);
    if (!mgr.exists(ref)) {
      Properties initParams = new Properties();
      initParams.setProperty("className", PSJavaScriptExtensionHandler.class.getName());
      initParams.setProperty(
          IPSExtensionHandler.INIT_PARAM_CONFIG_FILENAME,
          IPSExtensionHandler.DEFAULT_CONFIG_FILENAME);
      initParams.setProperty(IPSExtensionDef.INIT_PARAM_REENTRANT, "yes");

      IPSExtensionDef def =
          new PSExtensionDef(
              ref, List.of(IPSExtensionHandler.class.getName()).iterator(), null, initParams, null);
      Iterator res = PSIteratorUtils.emptyIterator();
      mgr.installExtension(def, res);
      System.out.println(handler + " installed successfully");
    }
    mgr.startExtensionHandler(handler);
    System.out.println(handler + " started successfully");
    mgr.stopExtensionHandler(handler);
    System.out.println(handler + " stopped successfully");
    mgr.shutdown();
    System.out.println("mgr shutdown successfully");
    assertTrue(expected.exists());
    expected.delete();
  }

  /** collect all tests into a TestSuite and return it */

  /** Base directory for unit test resources */
  private static final String RESOURCE_BASE = "UnitTestResources";

  /** File to which extension def is written. */
  private static final String RESULT_FILE = "Extensions.xml";
}
