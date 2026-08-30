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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.percussion.cms.objectstore.PSItemDefinition;
import com.percussion.cms.objectstore.server.PSItemDefManager;
import com.percussion.design.objectstore.PSContentEditor;
import com.percussion.rest.contenttypes.ContentTypeDetail;
import com.percussion.services.catalog.IPSCatalogSummary;
import com.percussion.utils.request.PSRequestInfoBase;
import com.percussion.webservices.content.IPSContentDesignWs;
import com.percussion.webservices.system.IPSSystemDesignWs;
import jakarta.ws.rs.WebApplicationException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.w3c.dom.Element;

/**
 * CD-14 POST import of Workbench-equivalent {@code ItemDefData} design XML via {@code
 * IPSContentDesignWs}. Admin only; 409 on name collision; does not steal locks.
 */
@Tag("UnitTest")
class ContentTypeAdaptorImportTest {

  private IPSContentDesignWs designWs;
  private IPSSystemDesignWs systemDesign;
  private PSItemDefManager itemDefManager;
  private ContentTypeAdaptor adaptor;

  @BeforeEach
  void setUp() {
    PSRequestInfoBase.initRequestInfo(new HashMap<>());
    PSRequestInfoBase.setRequestInfo(PSRequestInfoBase.KEY_JSESSIONID, "test-session");
    PSRequestInfoBase.setRequestInfo(PSRequestInfoBase.KEY_USER, "Admin");
    designWs = mock(IPSContentDesignWs.class);
    systemDesign = mock(IPSSystemDesignWs.class);
    itemDefManager = mock(PSItemDefManager.class);
    adaptor = new ContentTypeAdaptor(designWs, itemDefManager, systemDesign, () -> true);
    when(designWs.findContentTypes("*")).thenReturn(Collections.emptyList());
  }

  @AfterEach
  void tearDown() {
    PSRequestInfoBase.resetRequestInfo();
  }

  @Test
  void import_createsViaDesignWsAndRoundTripsNameThenGetable() throws Exception {
    String xml = sampleImportXml("importedOne", "Imported One");
    PSItemDefinition dest = stubCreatedDefinition("importedOne", 9001);
    when(dest.getLabel()).thenReturn("Imported One");
    when(designWs.createContentTypes(eq(List.of("importedOne")), eq("test-session"), eq("Admin")))
        .thenReturn(List.of(dest));

    ContentTypeDetail out = adaptor.importContentType(null, xml);

    assertEquals("importedOne", out.getName());
    assertEquals("Imported One", out.getLabel());
    verify(designWs)
        .createContentTypes(eq(List.of("importedOne")), eq("test-session"), eq("Admin"));
    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<PSItemDefinition>> saved = ArgumentCaptor.forClass(List.class);
    verify(designWs).saveContentTypes(saved.capture(), eq(true), eq("test-session"), eq("Admin"));
    assertEquals(1, saved.getValue().size());
    assertEquals(dest, saved.getValue().get(0));
    verify(dest).setTypeId(9001);
    verify(dest).setId(9001);
    verify(dest).setName("importedOne");
    verify(dest.getContentEditor()).setContentType(9001L);
    verify(designWs, never()).loadContentTypes(anyList(), anyBoolean(), anyBoolean(), any(), any());

    ContentTypeDetail reloaded = adaptor.getContentType(null, "importedOne");
    assertEquals("importedOne", reloaded.getName());
  }

  @Test
  void import_nonAdmin_is403() throws Exception {
    adaptor = new ContentTypeAdaptor(designWs, itemDefManager, systemDesign, () -> false);
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> adaptor.importContentType(null, sampleImportXml("importedOne", "Imported One")));
    assertEquals(403, ex.getResponse().getStatus());
    verify(designWs, never()).createContentTypes(anyList(), any(), any());
    verify(designWs, never()).saveContentTypes(anyList(), anyBoolean(), any(), any());
  }

  @Test
  void import_invalidXml_is400() {
    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class, () -> adaptor.importContentType(null, "<not-a-type"));
    assertTrue(ex.getMessage().contains("invalid content-type design XML"));
    verify(designWs, never()).createContentTypes(anyList(), any(), any());
  }

  @Test
  void import_blankXml_is400() {
    IllegalArgumentException ex =
        assertThrows(IllegalArgumentException.class, () -> adaptor.importContentType(null, "  "));
    assertEquals("content-type design XML is required", ex.getMessage());
  }

  @Test
  void import_jsonBody_is400() {
    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () -> adaptor.importContentType(null, "{\"name\":\"x\"}"));
    assertEquals("expected content-type design XML", ex.getMessage());
  }

  @Test
  void parseDesignXml_roundTripsNameFromWorkbenchXml() {
    String xml = sampleImportXml("importedOne", "Imported One");
    PSItemDefinition parsed = ContentTypeAdaptor.parseDesignXml(xml);
    assertEquals("importedOne", parsed.getName());
    assertEquals("Imported One", parsed.getLabel());
  }

  @Test
  void import_duplicateName_is409BeforeCreate() throws Exception {
    IPSCatalogSummary existing = mock(IPSCatalogSummary.class);
    when(existing.getName()).thenReturn("importedOne");
    when(designWs.findContentTypes("*")).thenReturn(List.of(existing));

    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> adaptor.importContentType(null, sampleImportXml("importedOne", "Imported One")));
    assertEquals(409, ex.getResponse().getStatus());
    assertTrue(ex.getMessage().contains("already exists"));
    verify(designWs, never()).createContentTypes(anyList(), any(), any());
    verify(designWs, never()).saveContentTypes(anyList(), anyBoolean(), any(), any());
  }

  @Test
  void import_persistTimeDuplicate_is409() throws Exception {
    when(designWs.createContentTypes(eq(List.of("importedOne")), eq("test-session"), eq("Admin")))
        .thenThrow(
            new IllegalArgumentException(
                "The name 'importedOne' for type 'NODEDEF' already exists."));
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> adaptor.importContentType(null, sampleImportXml("importedOne", "Imported One")));
    assertEquals(409, ex.getResponse().getStatus());
    verify(designWs, never()).saveContentTypes(anyList(), anyBoolean(), any(), any());
  }

  @Test
  void rewriteImportIdentity_setsCreatedTypeId() {
    Element root =
        ContentTypeAdaptor.parseDesignXml(sampleImportXml("importedOne", "Imported One"))
            .toXml(com.percussion.xml.PSXmlDocumentBuilder.createXmlDocument());
    ContentTypeAdaptor.rewriteImportIdentity(
        root, 9001, "../psx_ceimportedOne/importedOne.html", "importedOne");
    Element summary = (Element) root.getElementsByTagName("PSXItemDefSummary").item(0);
    Element editor = (Element) root.getElementsByTagName("PSXContentEditor").item(0);
    assertEquals("9001", summary.getAttribute("typeId"));
    assertEquals("9001", summary.getAttribute("id"));
    assertEquals("importedOne", summary.getAttribute("name"));
    assertEquals("9001", editor.getAttribute("contentType"));
  }

  private PSItemDefinition stubCreatedDefinition(String name, int typeId) throws Exception {
    PSItemDefinition def = mock(PSItemDefinition.class);
    PSContentEditor editor = mock(PSContentEditor.class);
    when(def.getName()).thenReturn(name);
    when(def.getLabel()).thenReturn(name);
    when(def.getDescription()).thenReturn("");
    when(def.isEnabled()).thenReturn(true);
    when(def.isHidden()).thenReturn(false);
    when(def.getAppName()).thenReturn("psx_ce" + name);
    when(def.getEditorUrl()).thenReturn("../psx_ce" + name + "/" + name + ".html");
    when(def.getTypeId()).thenReturn(typeId);
    when(def.getFieldSet()).thenReturn(null);
    when(def.getContentEditor()).thenReturn(editor);
    doNothing().when(def).fromXml(any(), any(), any());
    doAnswer(
            inv -> {
              when(def.getName()).thenReturn(inv.getArgument(0));
              return null;
            })
        .when(def)
        .setName(any());
    doAnswer(
            inv -> {
              when(def.getLabel()).thenReturn(inv.getArgument(0));
              return null;
            })
        .when(def)
        .setLabel(any());
    doAnswer(
            inv -> {
              when(def.getTypeId()).thenReturn(inv.getArgument(0));
              return null;
            })
        .when(def)
        .setTypeId(anyInt());
    when(itemDefManager.getItemDef(eq(name), eq(PSItemDefManager.COMMUNITY_ANY))).thenReturn(def);
    return def;
  }

  static String sampleImportXml(String name, String label) {
    return """
        <ItemDefData appName="psx_ce%s" isHidden="false" objectType="1">
          <PSXItemDefSummary editorUrl="../psx_ce%s/%s.html" id="557" label="%s" name="%s" typeId="557" />
          <PSXContentEditor contentType="557" enableRelatedContent="yes" iconSource="0" iconValue="" objectType="1" producesResource="no" workflowId="6">
            <PSXDataSet id="768">
              <name>%s</name>
              <description>imported</description>
              <transactionType>none</transactionType>
              <PSXContentEditorPipe id="0">
                <name>cePipe</name>
                <description>import</description>
                <InputDataExits></InputDataExits>
                <PSXContainerLocator>
                  <PSXTableSet>
                    <PSXTableLocator>
                      <PSXBackEndCredential id="0">
                        <alias>Cred1</alias>
                        <comment />
                        <datasource />
                      </PSXBackEndCredential>
                    </PSXTableLocator>
                    <PSXTableRef alias="CT_%s" name="CT_%s" />
                  </PSXTableSet>
                </PSXContainerLocator>
                <PSXContentEditorMapper>
                  <SystemFieldExcludes>
                    <FieldRef>sys_contentexpirydate</FieldRef>
                  </SystemFieldExcludes>
                  <PSXFieldSet name="CT_%s" repeatability="zeroOrMore" supportsSequencing="yes" type="parent" userSearchable="yes">
                  </PSXFieldSet>
                  <PSXUIDefinition>
                    <PSXDisplayMapper fieldSetRef="CT_%s" id="0">
                      <PSXDisplayMapping>
                        <FieldRef>sys_title</FieldRef>
                        <PSXUISet>
                          <Label>
                            <PSXDisplayText>Name:</PSXDisplayText>
                          </Label>
                          <PSXControlRef id="0" name="sys_EditBox">
                            <PSXParam name="maxlength">
                              <DataLocator>
                                <PSXTextLiteral id="0">
                                  <text>255</text>
                                </PSXTextLiteral>
                              </DataLocator>
                            </PSXParam>
                          </PSXControlRef>
                        </PSXUISet>
                      </PSXDisplayMapping>
                    </PSXDisplayMapper>
                  </PSXUIDefinition>
                </PSXContentEditorMapper>
                <userProperties></userProperties>
              </PSXContentEditorPipe>
              <PSXRequestor directDataStream="no" id="0">
                <requestPage>%s</requestPage>
                <SelectionParams />
                <ValidationRules />
                <characterEncoding>UTF-8</characterEncoding>
                <MimeProperties>
                  <html>
                    <PSXTextLiteral id="0">
                      <text>text/html</text>
                    </PSXTextLiteral>
                  </html>
                </MimeProperties>
              </PSXRequestor>
            </PSXDataSet>
            <PSXValidationRules maxErrorsToStop="10" />
            <PSXInputTranslations />
            <PSXOutputTranslations />
            <PSXWorkflowInfo type="inclusionary" values="4,5,6,7" />
          </PSXContentEditor>
        </ItemDefData>
        """
        .formatted(name, name, name, label, name, name, name.toUpperCase(), name.toUpperCase(),
            name.toUpperCase(), name.toUpperCase(), name);
  }
}
