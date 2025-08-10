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
package com.percussion.widgetbuilder.utils;

import static org.junit.jupiter.api.Assertions.*;

import com.percussion.server.PSServer;
import com.percussion.widgetbuilder.utils.xform.PSContentTypeFileTransformerTest;
import java.io.File;
import java.util.ArrayList;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Integration test for PSWidgetPackageBuilder. */
@Tag("IntegrationTest")
public class PSWidgetPackagerBuilderTest {

  @Test
  public void testGeneratePackage() throws Exception {
    var srcFile =
        new File(PSServer.getRxDir(), "sys_resources/widgetbuilder/percWidgetTemplate.zip");
    assertTrue(srcFile.exists());
    var tmpDir = new File(FileUtils.getTempDirectory(), this.getClass().getName());
    var tgtDir = new File(tmpDir, "packages");
    tgtDir.mkdirs();

    File result = null;
    try {
      var builder = new PSWidgetPackageBuilder(srcFile, tmpDir);
      var spec =
          new PSWidgetPackageSpec(
              "test", "www.test.com", "Custom Widget 2", "a 2nd test widget", "1.0.0", "3.1.0");
      spec.setResponsive(true);
      spec.setFields(PSContentTypeFileTransformerTest.setupPackageSpec().getFields());
      spec.setWidgetHtml("<div>$field</div>");
      var files = new ArrayList<String>();
      files.add("/web_resources/preMyWidget/foo/bar.css");
      files.add("/web_resources/preMyWidget/foo/bar2.css");
      files.add("http://foo.com/bar.css");
      spec.setCssFiles(files);

      files = new ArrayList<>();
      files.add("/web_resources/preMyWidget/foo/bar.js");
      files.add("/web_resources/preMyWidget/foo/bar2.js");
      files.add("http://foo.com/bar.js");
      spec.setJsFiles(files);

      result = builder.generatePackage(tgtDir, spec);
      assertTrue(result.exists());
      assertEquals(tgtDir, result.getParentFile());
      assertEquals(spec.getPackageName() + ".ppkg", result.getName());
    } finally {
      FileUtils.deleteQuietly(result);
    }
  }
}
