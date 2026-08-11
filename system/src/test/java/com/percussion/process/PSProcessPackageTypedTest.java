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
package com.percussion.process;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.xml.PSXmlDocumentBuilder;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Behavioral tests for typed {@code com.percussion.process} collection APIs after rawtypes cleanup
 * (#2299 / #2022 slice 5g).
 */
@Tag("UnitTest")
@DisplayName("process package generics")
class PSProcessPackageTypedTest {

  @Test
  @DisplayName("PSProcessManager loads typed process map from fixture")
  void processManagerLoadsTypedMap() throws Exception {
    try (InputStream in = getClass().getResourceAsStream("processes.xml")) {
      assertNotNull(in, "test fixture processes.xml must be on test classpath");
      PSProcessManager mgr = new PSProcessManager(in);
      IPSProcess create = mgr.getProcess("sindex_create");
      IPSProcess listing = mgr.getProcess("dirlisting");
      assertNotNull(create);
      assertEquals("sindex_create", create.getName());
      assertEquals("com.percussion.process.PSSimpleProcess", create.getType());
      assertNotNull(listing);
      assertNull(mgr.getProcess("missing-process"));
    }
  }

  @Test
  @DisplayName("PSProcessDef resolves command params with typed String context")
  void processDefResolvesTypedCommandParams() throws Exception {
    try (InputStream in = getClass().getResourceAsStream("processes.xml")) {
      assertNotNull(in);
      PSProcessManager mgr = new PSProcessManager(in);
      IPSProcess proc = mgr.getProcess("sindex_create");
      assertNotNull(proc);
      PSProcessDef def = proc.getProcessDef();
      assertNotNull(def, "fixture defines process for current OS");

      Map<String, String> ctx = new HashMap<>();
      ctx.put("WORKING_DIR", "testdir");
      ctx.put("RW_LIBRARY_NAME", "ce2");

      assertEquals("sindex", def.getExecutable(ctx));
      String[] params = def.getCommandParams(ctx);
      assertNotNull(params);
      assertTrue(params.length >= 4);
      // Path resolver expands {WORKING_DIR}; value includes platform absolute form of testdir
      boolean sawCfg = false;
      boolean sawLibrary = false;
      boolean sawNew = false;
      boolean sawCreate = false;
      for (String p : params) {
        if (p != null && p.contains("config") && p.contains("rware.cfg")) {
          sawCfg = true;
        }
        if ("-library".equals(p) || (p != null && p.contains("ce2"))) {
          sawLibrary = true;
        }
        if ("-new".equals(p)) {
          sawNew = true;
        }
        if ("-create".equals(p)) {
          sawCreate = true;
        }
      }
      assertTrue(sawCfg, "expected path-resolved cfg param");
      assertTrue(sawLibrary, "expected library param from typed ctx");
      assertTrue(sawNew);
      assertTrue(sawCreate);
    }
  }

  @Test
  @DisplayName("PSBasicResolver expands templates from typed Map")
  void basicResolverTypedContext() throws Exception {
    IPSVariableResolver resolver = new PSBasicResolver();
    Map<String, String> ctx = new HashMap<>();
    ctx.put("HOST", "localhost");
    ctx.put("PORT", "9992");
    String expanded = resolver.getValue("{HOST}:{PORT}", ctx);
    assertEquals("localhost:9992", expanded);
  }

  @Test
  @DisplayName("PSLiteralResolver ignores context and returns value")
  void literalResolverTypedContext() throws Exception {
    PSLiteralResolver resolver = new PSLiteralResolver();
    Map<String, String> ctx = new HashMap<>();
    ctx.put("IGNORED", "x");
    assertEquals("plain", resolver.resolve("plain", ctx));
    assertEquals("", resolver.resolve(null, ctx));
  }

  @Test
  @DisplayName("PSProcessRequest stores typed params and round-trips toXml")
  void processRequestTypedParamsRoundTrip() throws Exception {
    Map<String, String> env = new HashMap<>();
    env.put("HOST", "localhost");
    env.put("PORT", "9992");
    PSProcessRequest req = new PSProcessRequest("dirlisting", 5000, true, env);

    Map<String, String> params = req.getParams();
    assertEquals(2, params.size());
    assertEquals("localhost", params.get("HOST"));
    assertEquals("9992", params.get("PORT"));
    assertEquals("dirlisting", req.getName());
    assertEquals(5000, req.getWait());
    assertTrue(req.isTerminate());

    Document doc = PSXmlDocumentBuilder.createXmlDocument();
    Element el = req.toXml(doc);
    PSProcessRequest restored = new PSProcessRequest(el);
    assertEquals("dirlisting", restored.getName());
    assertEquals(5000, restored.getWait());
    assertTrue(restored.isTerminate());
    assertEquals("localhost", restored.getParams().get("HOST"));
    assertEquals("9992", restored.getParams().get("PORT"));
  }

  @Test
  @DisplayName("PSProcessRequest rejects empty name")
  void processRequestRejectsEmptyName() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new PSProcessRequest("  ", 0, false, Map.of()));
  }

  @Test
  @DisplayName("start rejects null typed context")
  void startRejectsNullContext() throws Exception {
    try (InputStream in = getClass().getResourceAsStream("processes.xml")) {
      assertNotNull(in);
      IPSProcess proc = new PSProcessManager(in).getProcess("dirlisting");
      assertNotNull(proc);
      assertThrows(IllegalArgumentException.class, () -> proc.start(null));
    }
  }

  @Test
  @DisplayName("getValue rejects null context")
  void getValueRejectsNullContext() {
    IPSVariableResolver resolver = new PSBasicResolver();
    assertThrows(IllegalArgumentException.class, () -> resolver.getValue("x", null));
  }

  @Test
  @DisplayName("PSProcessManager OS helpers remain stable")
  void processManagerOsHelpers() throws Exception {
    assertTrue(PSProcessManager.getOS() >= 0);
    String name = PSProcessManager.getOSType(PSProcessManager.getOS());
    assertNotNull(name);
    assertFalse(name.isEmpty());
    assertEquals(PSProcessManager.OS_WIN, PSProcessManager.getOSType("win"));
  }
}
