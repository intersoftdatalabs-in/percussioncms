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
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.net.MalformedURLException;
import java.net.URL;
import org.junit.jupiter.api.Test;

/**
 * Behavioral equivalence for the #2045 {@code java.net.URL} constructor migration (issue #5711):
 * the {@link PSContentExplorerUtils#toURL} helpers must preserve the observable behavior of the
 * deprecated-for-removal {@code URL} constructors they replace — same resolved values for valid
 * input and the same checked {@link MalformedURLException} for malformed input. Headless-safe: no
 * network access is performed.
 */
public class PSContentExplorerUrlTest {

  @Test
  public void stringFormParsesValidUrl() throws Exception {
    URL url =
        PSContentExplorerUtils.toURL("http://localhost:9992/Rhythmyx/sys_resources/AppletJars/");
    assertEquals("http", url.getProtocol());
    assertEquals("localhost", url.getHost());
    assertEquals(9992, url.getPort());
    assertEquals("/Rhythmyx/sys_resources/AppletJars/", url.getPath());
  }

  @Test
  public void stringFormRejectsMalformedInputWithCheckedException() {
    assertThrows(
        MalformedURLException.class, () -> PSContentExplorerUtils.toURL("ht!tp://bad url"));
  }

  @Test
  public void contextFormResolvesRelativeSpec() throws Exception {
    URL base = PSContentExplorerUtils.toURL("http://localhost:9992/Rhythmyx/dce/");
    URL resolved = PSContentExplorerUtils.toURL(base, "../sys_cxSupport/getWfStateIds.xml");
    assertEquals("http://localhost:9992/Rhythmyx/sys_cxSupport/getWfStateIds.xml", resolved.toString());
  }

  @Test
  public void contextFormKeepsAbsoluteSpec() throws Exception {
    URL base = PSContentExplorerUtils.toURL("http://localhost:9992/Rhythmyx/dce/");
    URL resolved = PSContentExplorerUtils.toURL(base, "http://other:8080/Rhythmyx/logout");
    assertEquals("http://other:8080/Rhythmyx/logout", resolved.toString());
  }

  @Test
  public void contextFormRejectsMalformedInputWithCheckedException() throws Exception {
    URL base = PSContentExplorerUtils.toURL("http://localhost:9992/Rhythmyx/dce/");
    assertThrows(MalformedURLException.class, () -> PSContentExplorerUtils.toURL(base, "ht!tp://^^"));
  }

  @Test
  public void componentsFormComposesCodeBase() throws Exception {
    URL url = PSContentExplorerUtils.toURL("http", "localhost", 9992, "/Rhythmyx/dce/");
    assertEquals("http://localhost:9992/Rhythmyx/dce/", url.toString());
  }

  @Test
  public void componentsFormHonorsDefaultPort() throws Exception {
    URL url = PSContentExplorerUtils.toURL("http", "cms.example.com", -1, "/Rhythmyx/dce/");
    assertEquals("http", url.getProtocol());
    assertEquals("cms.example.com", url.getHost());
    assertEquals(-1, url.getPort());
    assertEquals("/Rhythmyx/dce/", url.getPath());
  }

  @Test
  public void componentsFormRejectsMalformedInputWithCheckedException() {
    assertThrows(
        MalformedURLException.class,
        () -> PSContentExplorerUtils.toURL("ht!tp", "localhost", 9992, "/Rhythmyx/dce/"));
  }
}
