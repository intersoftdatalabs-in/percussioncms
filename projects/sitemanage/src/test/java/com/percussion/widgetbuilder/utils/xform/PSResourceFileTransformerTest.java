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
package com.percussion.widgetbuilder.utils.xform;

import com.percussion.pagemanagement.data.PSResourceDefinitionGroup;
import com.percussion.share.dao.PSSerializerUtils;
import com.percussion.widgetbuilder.utils.PSWidgetPackageSpec;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for PSResourceFileTransformer.
 */
public class PSResourceFileTransformerTest {

    @Test
    public void testTransformFile() throws Exception {
        var packageSpec = new PSWidgetPackageSpec("pre", "url", "MyWidget", "", "1.0.0", "3.2.1");
        var files = new ArrayList<String>();
        files.add("/web_resources/preMyWidget/foo/bar.css");
        files.add("/web_resources/preMyWidget/foo/bar2.css");
        files.add("http://foo.com/bar.css");
        packageSpec.setCssFiles(files);

        files = new ArrayList<>();
        files.add("/web_resources/preMyWidget/foo/bar.js");
        files.add("/web_resources/preMyWidget/foo/bar2.js");
        files.add("http://foo.com/bar.js");
        packageSpec.setJsFiles(files);

        var xform = new PSResourceFileTransformer();
        try (Reader reader = new InputStreamReader(this.getClass().getResourceAsStream("transformResources.xml"))) {
            var file = new File("sys__UserDependency--rxconfig_Resources_preMyWidget/preMyWidget.xml");
            var expected = IOUtils.toString(this.getClass().getResourceAsStream("expectedResources.xml"));
            expected = PSSerializerUtils.marshal(PSSerializerUtils.unmarshal(expected, PSResourceDefinitionGroup.class));
            var result = IOUtils.toString(xform.transformFile(file, reader, packageSpec));
            assertEquals(expected, result);
        }
    }

    @Test
    public void testHandleFile() {
        var xform = new PSResourceFileTransformer();
        assertFalse(xform.handleFile(new File("testWidget.xml")));
        assertFalse(xform.handleFile(new File("sys__UserDependency--rxconfig_Widgets_mywidget/testWidget.xml")));
        assertTrue(xform.handleFile(new File("sys__UserDependency--rxconfig_Resources_mywidget/testWidget.xml")));
    }

    @Test
    public void testTransformPath() {
        var test = new File("/a/b/c");
        var xform = new PSResourceFileTransformer();
        assertEquals(test, xform.transformPath(test, null));
    }
}
