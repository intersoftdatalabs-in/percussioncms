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
package test.percussion.pso.installer;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Pins the repaired submit handler in the installer-payload REST Import
 * Service test helper ({@code webapp/WEB-INF/pages/importtest_post.jsp},
 * issue #7372, residual of #7369 under parent audit #5627).
 *
 * <p>The de-jqueryed handler was inert: it read the server-side JSP implicit
 * {@code request} object from client-side script (browser
 * {@code ReferenceError} on every submit), dereferenced a commented-out
 * {@code #post_url} textarea ({@code null.value}), and rendered into a
 * {@code #message_ajax} element that did not exist. The repair reads the live
 * {@code #body} textarea, POSTs to the form's own action, and renders into an
 * added {@code #message_ajax} result div. These tests trip if any of the
 * three dead references comes back.
 */
public class ImportTestPostJspHandlerTest {

  private static String readHelperJsp() throws IOException {
    List<Path> candidates =
        List.of(
            Path.of(
                System.getProperty("user.dir"),
                "src",
                "main",
                "resources",
                "InstallDir",
                "webapp",
                "WEB-INF",
                "pages",
                "importtest_post.jsp"),
            Path.of(
                System.getProperty("user.dir"),
                "modules",
                "perc-toolkit",
                "src",
                "main",
                "resources",
                "InstallDir",
                "webapp",
                "WEB-INF",
                "pages",
                "importtest_post.jsp"));
    for (Path candidate : candidates) {
      if (Files.isRegularFile(candidate)) {
        return Files.readString(candidate, StandardCharsets.UTF_8);
      }
    }
    fail("importtest_post.jsp not found under user.dir=" + System.getProperty("user.dir"));
    throw new IllegalStateException("unreachable");
  }

  /** Extracts the client-side script block (after the last server-side %>) for targeted checks. */
  private static String clientScript(String jsp) {
    int script = jsp.indexOf("<script");
    assertTrue(script >= 0, "helper JSP must contain a client <script> block");
    return jsp.substring(script);
  }

  @Test
  public void clientScriptMustNotReferenceServerImplicitRequest() throws IOException {
    String script = clientScript(readHelperJsp());
    assertFalse(
        script.contains("request.getParameter"),
        "client-side handler must not call the server-side JSP implicit `request` object "
            + "(throws ReferenceError in the browser — issue #7372)");
  }

  @Test
  public void handlerReadsLiveBodyTextarea() throws IOException {
    String script = clientScript(readHelperJsp());
    assertTrue(
        script.contains("getElementById(\"body\")"),
        "handler must read the payload from the live #body textarea (issue #7372)");
  }

  @Test
  public void handlerMustNotReferenceMissingPostUrlElement() throws IOException {
    String jsp = readHelperJsp();
    String script = clientScript(jsp);
    assertFalse(
        script.contains("getElementById(\"post_url\")"),
        "handler must not dereference #post_url while that textarea stays commented "
            + "out of the markup (null.value — issue #7372)");
  }

  @Test
  public void messageAjaxResultElementExists() throws IOException {
    String jsp = readHelperJsp();
    assertTrue(
        jsp.contains("id=\"message_ajax\""),
        "markup must provide the #message_ajax element the fetch callback renders into "
            + "(issue #7372)");
  }

  @Test
  public void fetchPostsToFormAction() throws IOException {
    String script = clientScript(readHelperJsp());
    assertTrue(
        script.contains("getAttribute(\"action\")"),
        "handler must POST to the form's own action now that the #post_url "
            + "override field is commented out (issue #7372)");
  }
}
