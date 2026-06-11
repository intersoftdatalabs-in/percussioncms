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
package com.percussion.pagemanagement.assembler;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class PSPageUtilsTest {

  @Test
  public void testProcessFrameworkContent() {
    PSPageUtils utils = new PSPageUtils();

    // Null and empty checks
    assertEquals("", utils.processFrameworkContent(null));
    assertEquals("", utils.processFrameworkContent(""));
    assertEquals("", utils.processFrameworkContent("   "));

    // Plain text / script tags / HTML should not be modified
    assertEquals(
        "<script src=\"https://code.jquery.com/jquery-3.7.1.min.js\"></script>",
        utils.processFrameworkContent(
            "<script src=\"https://code.jquery.com/jquery-3.7.1.min.js\"></script>"));
    assertEquals(
        "console.log(\"hello\");", utils.processFrameworkContent("console.log(\"hello\");"));

    // CDN / Web URLs ending in .js should be wrapped in script tags
    assertEquals(
        "<script src=\"https://code.jquery.com/jquery-3.7.1.min.js\"></script>",
        utils.processFrameworkContent("https://code.jquery.com/jquery-3.7.1.min.js"));
    assertEquals(
        "<script src=\"http://code.jquery.com/jquery-3.7.1.js?v=2\"></script>",
        utils.processFrameworkContent("http://code.jquery.com/jquery-3.7.1.js?v=2"));
    assertEquals(
        "<script src=\"//code.jquery.com/ui/1.13.3/jquery-ui.min.js\"></script>",
        utils.processFrameworkContent("//code.jquery.com/ui/1.13.3/jquery-ui.min.js"));

    // Relative URLs ending in .js
    assertEquals(
        "<script src=\"/js/custom.js\"></script>", utils.processFrameworkContent("/js/custom.js"));

    // CDN / Web URLs ending in .css should be wrapped in link tags
    assertEquals(
        "<link rel=\"stylesheet\" href=\"https://code.jquery.com/ui/1.13.3/themes/smoothness/jquery-ui.css\" />",
        utils.processFrameworkContent(
            "https://code.jquery.com/ui/1.13.3/themes/smoothness/jquery-ui.css"));
    assertEquals(
        "<link rel=\"stylesheet\" href=\"//example.com/styles.css?foo=bar#baz\" />",
        utils.processFrameworkContent("//example.com/styles.css?foo=bar#baz"));

    // URLs with other extensions or not looking like JS/CSS should not be modified
    assertEquals(
        "https://example.com/image.png",
        utils.processFrameworkContent("https://example.com/image.png"));
  }
}
