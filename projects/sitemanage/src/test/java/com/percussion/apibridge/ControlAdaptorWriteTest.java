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

package com.percussion.apibridge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.design.objectstore.PSControlMeta;
import com.percussion.rest.cecontrols.ControlDef;
import com.percussion.xml.PSXmlDocumentBuilder;
import jakarta.ws.rs.WebApplicationException;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * UI-01 POST create / PUT update / DELETE persist user CE controls as XSL files plus {@code
 * writeImports}. Admin only; unique name; system controls 409.
 */
@Tag("UnitTest")
class ControlAdaptorWriteTest {

  @TempDir Path tempDir;

  private TestUserControlIo io;
  private ControlAdaptor adaptor;

  @BeforeEach
  void setUp() throws IOException {
    io = new TestUserControlIo(tempDir.resolve("controls"));
    Files.createDirectories(io.dir);
    adaptor = new ControlAdaptor(() -> true, io);
  }

  @Test
  void create_writesXslAndRefreshesImports() {
    ControlDef body = new ControlDef();
    body.setName("myUserControl");
    body.setDisplayName("My User Control");
    body.setDescription("created via REST");

    ControlDef out = adaptor.createControl(body);

    assertEquals("myUserControl", out.getName());
    assertEquals("user", out.getScope());
    Path written = io.dir.resolve("myUserControl.xsl");
    assertTrue(Files.isRegularFile(written));
    assertEquals(1, io.importsWritten);
    ControlDef fetched = adaptor.findControlByName("myUserControl");
    assertNotNull(fetched);
    assertEquals("myUserControl", fetched.getName());
    assertTrue(adaptor.listControls().stream().anyMatch(c -> "myUserControl".equals(c.getName())));
  }

  @Test
  void create_duplicateName_is409BeforeWrite() {
    io.system.add(controlMeta("sys_EditBox"));
    ControlDef body = new ControlDef();
    body.setName("sys_EditBox");
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> adaptor.createControl(body));
    assertEquals(409, ex.getResponse().getStatus());
    assertTrue(ex.getMessage().contains("already exists"));
    assertFalse(Files.exists(io.dir.resolve("sys_EditBox.xsl")));
    assertEquals(0, io.importsWritten);
  }

  @Test
  void create_duplicateUserName_is409() {
    ControlDef first = new ControlDef();
    first.setName("myUserControl");
    adaptor.createControl(first);
    io.importsWritten = 0;
    ControlDef dup = new ControlDef();
    dup.setName("myUserControl");
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> adaptor.createControl(dup));
    assertEquals(409, ex.getResponse().getStatus());
    assertEquals(0, io.importsWritten);
  }

  @Test
  void create_blankName_throws() {
    assertThrows(IllegalArgumentException.class, () -> adaptor.createControl(null));
    ControlDef blank = new ControlDef();
    blank.setName("  ");
    IllegalArgumentException ex =
        assertThrows(IllegalArgumentException.class, () -> adaptor.createControl(blank));
    assertTrue(ex.getMessage().contains("name is required"));
    assertEquals(0, io.importsWritten);
  }

  @Test
  void create_nameWithSpaces_throws() {
    ControlDef body = new ControlDef();
    body.setName("has space");
    IllegalArgumentException ex =
        assertThrows(IllegalArgumentException.class, () -> adaptor.createControl(body));
    assertEquals("name cannot contain whitespace", ex.getMessage());
  }

  @Test
  void create_wildcardName_throws() {
    ControlDef body = new ControlDef();
    body.setName("My*Control");
    IllegalArgumentException ex =
        assertThrows(IllegalArgumentException.class, () -> adaptor.createControl(body));
    assertEquals("name must not contain wildcards", ex.getMessage());
  }

  @Test
  void create_invalidDimension_throws() {
    ControlDef body = new ControlDef();
    body.setName("myUserControl");
    body.setDimension("wide");
    IllegalArgumentException ex =
        assertThrows(IllegalArgumentException.class, () -> adaptor.createControl(body));
    assertTrue(ex.getMessage().contains("dimension"));
    assertEquals(0, io.importsWritten);
  }

  @Test
  void create_invalidXslSource_throws() {
    ControlDef body = new ControlDef();
    body.setName("myUserControl");
    body.setXslSource("<not-xsl/>");
    IllegalArgumentException ex =
        assertThrows(IllegalArgumentException.class, () -> adaptor.createControl(body));
    assertTrue(ex.getMessage().toLowerCase().contains("xslsource"));
    assertEquals(0, io.importsWritten);
  }

  @Test
  void create_nonAdmin_is403() {
    adaptor = new ControlAdaptor(() -> false, io);
    ControlDef body = new ControlDef();
    body.setName("myUserControl");
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> adaptor.createControl(body));
    assertEquals(403, ex.getResponse().getStatus());
    assertEquals(ControlAdaptor.ADMIN_REQUIRED, ex.getMessage());
    assertFalse(Files.exists(io.dir.resolve("myUserControl.xsl")));
  }

  @Test
  void save_updatesUserFile() {
    ControlDef created = new ControlDef();
    created.setName("myUserControl");
    created.setDisplayName("Original");
    adaptor.createControl(created);
    io.importsWritten = 0;

    ControlDef body = new ControlDef();
    body.setDisplayName("Updated");
    body.setDescription("saved via REST");
    ControlDef out = adaptor.saveControl("myUserControl", body);

    assertEquals("myUserControl", out.getName());
    assertEquals(1, io.importsWritten);
    ControlDef fetched = adaptor.findControlByName("myUserControl");
    assertNotNull(fetched);
    assertEquals("Updated", fetched.getDisplayName());
  }

  @Test
  void save_systemControl_is409AndDoesNotWrite() {
    io.system.add(controlMeta("sys_EditBox"));
    ControlDef body = new ControlDef();
    body.setDisplayName("nope");
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> adaptor.saveControl("sys_EditBox", body));
    assertEquals(409, ex.getResponse().getStatus());
    assertEquals(ControlAdaptor.SYSTEM_CONTROL_READONLY, ex.getMessage());
    assertFalse(Files.exists(io.dir.resolve("sys_EditBox.xsl")));
    assertEquals(0, io.importsWritten);
  }

  @Test
  void save_missingUser_isNull() {
    ControlDef body = new ControlDef();
    body.setDisplayName("nope");
    assertNull(adaptor.saveControl("missing", body));
    assertEquals(0, io.importsWritten);
  }

  @Test
  void delete_userControl_removesFileAndRefreshesImports() {
    ControlDef created = new ControlDef();
    created.setName("myUserControl");
    adaptor.createControl(created);
    io.importsWritten = 0;

    assertTrue(adaptor.deleteControl("myUserControl"));
    assertFalse(Files.exists(io.dir.resolve("myUserControl.xsl")));
    assertEquals(1, io.importsWritten);
    assertNull(adaptor.findControlByName("myUserControl"));
  }

  @Test
  void delete_systemControl_is409AndDoesNotDelete() {
    io.system.add(controlMeta("sys_EditBox"));
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> adaptor.deleteControl("sys_EditBox"));
    assertEquals(409, ex.getResponse().getStatus());
    assertEquals(ControlAdaptor.SYSTEM_CONTROL_READONLY, ex.getMessage());
    assertEquals(0, io.importsWritten);
  }

  @Test
  void delete_missing_isFalse() {
    assertFalse(adaptor.deleteControl("missing"));
  }

  @Test
  void delete_nonAdmin_is403() {
    ControlDef created = new ControlDef();
    created.setName("myUserControl");
    adaptor.createControl(created);
    adaptor = new ControlAdaptor(() -> false, io);
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> adaptor.deleteControl("myUserControl"));
    assertEquals(403, ex.getResponse().getStatus());
    assertTrue(Files.exists(io.dir.resolve("myUserControl.xsl")));
  }

  @Test
  void generateDefaultXsl_isParseableWithMatchingName() throws Exception {
    String xsl =
        ControlAdaptor.generateDefaultXsl(
            "myUserControl", "Label", "desc", "single", "none");
    ControlAdaptor.validateXslSource("myUserControl", xsl);
    assertTrue(xsl.contains("name=\"myUserControl\""));
  }

  @Test
  void generateDefaultXsl_doesNotUseOsSeparators() {
    String xsl = ControlAdaptor.generateDefaultXsl("ctrl", "ctrl", "", "single", "none");
    assertFalse(xsl.contains("\\"));
    assertFalse(xsl.contains("rx_resources"));
  }

  static final class TestUserControlIo implements UserControlIo {
    final Path dir;
    final List<PSControlMeta> system = new ArrayList<>();
    int importsWritten;

    TestUserControlIo(Path dir) {
      this.dir = dir;
    }

    @Override
    public Path userControlsDirectory() {
      return dir;
    }

    @Override
    public void writeImports() {
      importsWritten++;
    }

    @Override
    public List<PSControlMeta> loadSystemControls() {
      return new ArrayList<>(system);
    }

    @Override
    public List<PSControlMeta> loadUserControls() {
      List<PSControlMeta> out = new ArrayList<>();
      if (!Files.isDirectory(dir)) {
        return out;
      }
      try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "*.xsl")) {
        for (Path file : stream) {
          try (InputStream in = Files.newInputStream(file)) {
            Document doc = PSXmlDocumentBuilder.createXmlDocument(in, false);
            NodeList nodes = doc.getElementsByTagName(PSControlMeta.XML_NODE_NAME);
            for (int i = 0; i < nodes.getLength(); i++) {
              out.add(new PSControlMeta((Element) nodes.item(i)));
            }
          }
        }
      } catch (Exception e) {
        throw new IllegalStateException("Failed to load user control files", e);
      }
      return out;
    }

    @Override
    public Path findUserControlFile(String name) {
      Path file = dir.resolve(name + ".xsl");
      return Files.isRegularFile(file) ? file : null;
    }
  }

  private static PSControlMeta controlMeta(String name) {
    try {
      String xml =
          "<psxctl:ControlMeta xmlns:psxctl=\"urn:percussion.com/control\" name=\""
              + name
              + "\" dimension=\"single\" choiceset=\"none\"/>";
      Document doc = PSXmlDocumentBuilder.createXmlDocument(new StringReader(xml), false);
      return new PSControlMeta(doc.getDocumentElement());
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }
}
