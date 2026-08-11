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
package com.percussion.design.objectstore;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.util.PSCollection;
import com.percussion.xml.PSXmlDocumentBuilder;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Behavioral coverage for ctor/fromXml this-escape real-fix batch (#2404): private helpers /
 * final setters preserve Element restore and value construction.
 */
public class PSDesignObjectStoreThisEscapeTest {

  @Test
  public void choicesGlobalCtorAndElementRoundTrip() throws Exception {
    Document doc = PSXmlDocumentBuilder.createXmlDocument();
    PSChoices original = new PSChoices(42);
    Element elem = original.toXml(doc);

    PSChoices restored = new PSChoices(elem, null, null);
    assertEquals(original, restored);
    assertEquals(42, restored.getGlobal());
    assertEquals(PSChoices.TYPE_GLOBAL, restored.getType());
  }

  @Test
  public void choicesLocalCtorAndElementRoundTrip() throws Exception {
    Document doc = PSXmlDocumentBuilder.createXmlDocument();
    PSEntry entry = new PSEntry("v1", new PSDisplayText("label1"));
    PSCollection local = new PSCollection(PSEntry.class);
    local.add(entry);
    PSChoices original = new PSChoices(local);
    Element elem = original.toXml(doc);

    PSChoices restored = new PSChoices(elem, null, null);
    assertEquals(original, restored);
    assertEquals(PSChoices.TYPE_LOCAL, restored.getType());
  }

  @Test
  public void fieldValueCtorAndElementRoundTrip() throws Exception {
    Document doc = PSXmlDocumentBuilder.createXmlDocument();
    PSField original = new PSField("sys_title", null);
    original.setType(PSField.TYPE_SYSTEM);
    Element elem = original.toXml(doc);

    PSField restored = new PSField(elem, null, null);
    assertEquals(original.getSubmitName(), restored.getSubmitName());
    assertEquals(original.getType(), restored.getType());
  }

  @Test
  public void entryValueCtorAndElementRoundTrip() throws Exception {
    Document doc = PSXmlDocumentBuilder.createXmlDocument();
    PSEntry original = new PSEntry("val", new PSDisplayText("lbl"));
    Element elem = original.toXml(doc);

    PSEntry restored = new PSEntry(elem, null, null);
    assertEquals(original.getValue(), restored.getValue());
    assertEquals(original.getLabel().getText(), restored.getLabel().getText());
  }

  @Test
  public void urlRequestCtorAndElementRoundTrip() throws Exception {
    Document doc = PSXmlDocumentBuilder.createXmlDocument();
    PSCollection params = new PSCollection(PSParam.class);
    params.add(new PSParam("p1", new PSTextLiteral("one")));
    PSUrlRequest original = new PSUrlRequest("req", "http://example.intsof/test", params);
    Element elem = original.toXml(doc);

    PSUrlRequest restored = new PSUrlRequest(elem, null, null);
    assertEquals(original.getName(), restored.getName());
    assertEquals(original.getHref(), restored.getHref());
  }

  @Test
  public void aclEntryNameCtorAndElementRoundTrip() throws Exception {
    Document doc = PSXmlDocumentBuilder.createXmlDocument();
    PSAclEntry original = new PSAclEntry("admin1", PSAclEntry.ACE_TYPE_USER);
    Element elem = original.toXml(doc);

    PSAclEntry restored = new PSAclEntry(elem, null, null);
    assertEquals(original.getName(), restored.getName());
    assertTrue(restored.isUser());
  }

  @Test
  public void aclElementRoundTripPreservesEntries() throws Exception {
    Document doc = PSXmlDocumentBuilder.createXmlDocument();
    PSAcl original = new PSAcl();
    PSCollection entries = new PSCollection(PSAclEntry.class);
    PSAclEntry entry = new PSAclEntry("Editor", PSAclEntry.ACE_TYPE_ROLE);
    entries.add(entry);
    original.setEntries(entries);
    original.setAccessForMultiMembershipMaximum();
    Element elem = original.toXml(doc);

    PSAcl restored = new PSAcl(elem, null, null);
    assertNotNull(restored.getEntries());
    assertEquals(1, restored.getEntries().size());
    assertEquals("Editor", ((PSAclEntry) restored.getEntries().get(0)).getName());
    assertTrue(restored.isAccessForMultiMembershipMaximum());
  }

  @Test
  public void fieldSetNameCtor() {
    PSFieldSet set = new PSFieldSet("body");
    assertEquals("body", set.getName());
    assertEquals(PSFieldSet.TYPE_PARENT, set.getType());
  }

  @Test
  public void propertyNameCtorAndElementRoundTrip() throws Exception {
    Document doc = PSXmlDocumentBuilder.createXmlDocument();
    PSProperty original =
        new PSProperty("dimage", PSProperty.TYPE_STRING, "gif", false, "image type");
    Element elem = original.toXml(doc);

    PSProperty restored = new PSProperty(elem);
    assertEquals(original.getName(), restored.getName());
    assertEquals(original.getType(), restored.getType());
  }

  @Test
  public void relativeSubjectNameCtorAndElementRoundTrip() throws Exception {
    Document doc = PSXmlDocumentBuilder.createXmlDocument();
    PSRelativeSubject original =
        new PSRelativeSubject("alice", PSSubject.SUBJECT_TYPE_USER, null);
    Element elem = original.toXml(doc);

    PSRelativeSubject restored = new PSRelativeSubject(elem, null, null);
    assertEquals(original.getName(), restored.getName());
    assertEquals(original.getType(), restored.getType());
    assertTrue(restored.isUser());
  }

  @Test
  public void globalSubjectNameCtorAndElementRoundTrip() throws Exception {
    Document doc = PSXmlDocumentBuilder.createXmlDocument();
    PSGlobalSubject original =
        new PSGlobalSubject("editors", PSSubject.SUBJECT_TYPE_GROUP, null);
    Element elem = original.toXml(doc);

    PSGlobalSubject restored = new PSGlobalSubject(elem, null, null);
    assertEquals(original.getName(), restored.getName());
    assertEquals(original.getType(), restored.getType());
    assertTrue(restored.isGroup());
  }

  @Test
  public void attributeNameCtorAndElementRoundTrip() throws Exception {
    Document doc = PSXmlDocumentBuilder.createXmlDocument();
    PSAttribute original = new PSAttribute("department");
    Element elem = original.toXml(doc);

    PSAttribute restored = new PSAttribute(elem, null, null);
    assertEquals(original.getName(), restored.getName());
  }

  @Test
  public void attributeListElementRoundTrip() throws Exception {
    Document doc = PSXmlDocumentBuilder.createXmlDocument();
    PSAttributeList original = new PSAttributeList();
    original.setAttribute("dept", java.util.List.of("eng"));
    Element elem = original.toXml(doc);

    PSAttributeList restored = new PSAttributeList(elem);
    assertEquals("eng", restored.getAttribute("dept").getValues().get(0));
  }

  @Test
  public void notifierProviderCtorAndElementRoundTrip() throws Exception {
    Document doc = PSXmlDocumentBuilder.createXmlDocument();
    PSNotifier original = new PSNotifier(PSNotifier.MP_TYPE_SMTP, "mail.example.intsof");
    original.setFrom("cms@example.intsof");
    Element elem = original.toXml(doc);

    PSNotifier restored = new PSNotifier(elem, null, null);
    assertEquals(original.getServer(), restored.getServer());
    assertEquals(original.getFrom(), restored.getFrom());
    assertEquals(PSNotifier.MP_TYPE_SMTP, restored.getProviderType());
  }

  @Test
  public void loginWebPageCtorAndElementRoundTrip() throws Exception {
    Document doc = PSXmlDocumentBuilder.createXmlDocument();
    PSLoginWebPage original =
        new PSLoginWebPage(new java.net.URL("https://example.intsof/login"), true);
    Element elem = original.toXml(doc);

    PSLoginWebPage restored = new PSLoginWebPage(elem, null, null);
    assertEquals(original.getUrl().toExternalForm(), restored.getUrl().toExternalForm());
    assertTrue(restored.isSecure());
  }

  @Test
  public void errorWebPagesElementRoundTrip() throws Exception {
    Document doc = PSXmlDocumentBuilder.createXmlDocument();
    PSErrorWebPages original = new PSErrorWebPages(false);
    Element elem = original.toXml(doc);

    PSErrorWebPages restored = new PSErrorWebPages(elem, null, null);
    assertEquals(original.isHtmlReturned(), restored.isHtmlReturned());
  }

  @Test
  public void roleNameCtorAndElementRoundTrip() throws Exception {
    Document doc = PSXmlDocumentBuilder.createXmlDocument();
    PSRole original = new PSRole("Admin");
    Element elem = original.toXml(doc);

    PSRole restored = new PSRole(elem, null, null);
    assertEquals(original.getName(), restored.getName());
  }

  @Test
  public void securityProviderInstanceNameCtorAndElementRoundTrip() throws Exception {
    Document doc = PSXmlDocumentBuilder.createXmlDocument();
    // SP_TYPE_WEB_SERVER is a stable supported type used in unit tests
    PSSecurityProviderInstance original =
        new PSSecurityProviderInstance(
            "web1", com.percussion.security.PSSecurityProvider.SP_TYPE_WEB_SERVER);
    Element elem = original.toXml(doc);

    PSSecurityProviderInstance restored = new PSSecurityProviderInstance(elem, null, null);
    assertEquals(original.getName(), restored.getName());
    assertEquals(original.getType(), restored.getType());
  }

  @Test
  public void traceInfoDefaultAndElementRoundTrip() throws Exception {
    Document doc = PSXmlDocumentBuilder.createXmlDocument();
    PSTraceInfo original = new PSTraceInfo();
    Element elem = original.toXml(doc);

    PSTraceInfo restored = new PSTraceInfo(elem, null, null);
    assertEquals(original.getColumnWidth(), restored.getColumnWidth());
    assertEquals(original.isTraceEnabled(), restored.isTraceEnabled());
  }

  // --- #2465 app/editor config residual batch ---

  @Test
  public void applicationNameCtorSetsNameAndRequestRoot() {
    // Package-visible / protected name ctor uses applyName/applyRequestRoot (this-escape safe).
    PSApplication app = new PSApplication("DemoApp");
    assertEquals("DemoApp", app.getName());
    assertEquals("DemoApp", app.getRequestRoot());
  }

  @Test
  public void relationshipIdCtorAndCopyCtor() {
    PSLocator owner = new PSLocator(10, 1);
    PSLocator dependent = new PSLocator(20, 1);
    PSRelationshipConfig config =
        new PSRelationshipConfig("ActiveAssembly", PSRelationshipConfig.RS_TYPE_SYSTEM);
    PSRelationship original = new PSRelationship(42, owner, dependent, config);
    assertEquals(42, original.getId());
    assertEquals(owner, original.getOwner());
    assertEquals(dependent, original.getDependent());

    PSRelationship copy = new PSRelationship(99, original);
    assertEquals(99, copy.getId());
    assertEquals(owner, copy.getOwner());
    assertEquals(config.getName(), copy.getConfig().getName());
  }

  @Test
  public void relationshipConfigNameTypeCtorAndElementRoundTrip() throws Exception {
    Document doc = PSXmlDocumentBuilder.createXmlDocument();
    PSRelationshipConfig original =
        new PSRelationshipConfig(
            "ActiveAssembly",
            PSRelationshipConfig.RS_TYPE_SYSTEM,
            PSRelationshipConfig.CATEGORY_ACTIVE_ASSEMBLY);
    Element elem = original.toXml(doc);

    PSRelationshipConfig restored = new PSRelationshipConfig(elem, null, null);
    assertEquals(original.getName(), restored.getName());
    assertEquals(original.getType(), restored.getType());
    assertEquals(original.getCategory(), restored.getCategory());
  }

  @Test
  public void queryPipeNameCtorAndElementRoundTrip() throws Exception {
    Document doc = PSXmlDocumentBuilder.createXmlDocument();
    PSQueryPipe original = new PSQueryPipe("qpipe1");
    Element elem = original.toXml(doc);

    PSQueryPipe restored = new PSQueryPipe(elem, null, null);
    assertEquals(original.getName(), restored.getName());
  }

  @Test
  public void updatePipeNameCtorAndElementRoundTrip() throws Exception {
    Document doc = PSXmlDocumentBuilder.createXmlDocument();
    PSUpdatePipe original = new PSUpdatePipe("upipe1");
    Element elem = original.toXml(doc);

    PSUpdatePipe restored = new PSUpdatePipe(elem, null, null);
    assertEquals(original.getName(), restored.getName());
  }

  @Test
  public void dataMappingCtorAndElementRoundTrip() throws Exception {
    Document doc = PSXmlDocumentBuilder.createXmlDocument();
    // PSBackEndColumn implements IPSBackEndMapping for mapping construction.
    PSBackEndTable table = new PSBackEndTable("CT_TABLE");
    PSBackEndColumn col = new PSBackEndColumn(table, "TITLE");
    PSDataMapping original = new PSDataMapping(new PSXmlField("title"), col);
    Element elem = original.toXml(doc);

    PSDataMapping restored = new PSDataMapping(elem, null, null);
    assertEquals(original.getXmlField(), restored.getXmlField());
  }

  @Test
  public void workflowInfoTypeCtorAndElementRoundTrip() throws Exception {
    Document doc = PSXmlDocumentBuilder.createXmlDocument();
    PSWorkflowInfo original =
        new PSWorkflowInfo(PSWorkflowInfo.TYPE_INCLUSIONARY, java.util.List.of(1, 2, 3));
    Element elem = original.toXml(doc);

    PSWorkflowInfo restored = new PSWorkflowInfo(elem);
    assertEquals(original.getType(), restored.getType());
    java.util.List<Integer> restoredValues = new java.util.ArrayList<>();
    restored.getValues().forEachRemaining(restoredValues::add);
    assertEquals(java.util.List.of(1, 2, 3), restoredValues);
  }

  @Test
  public void sharedFieldGroupNameCtor() {
    PSSharedFieldGroup group = new PSSharedFieldGroup("sharedBody", "sharedBody.xml");
    assertEquals("sharedBody", group.getName());
    assertEquals("sharedBody.xml", group.getFilename());
  }

  @Test
  public void resultPageStyleSheetCtorAndElementRoundTrip() throws Exception {
    Document doc = PSXmlDocumentBuilder.createXmlDocument();
    PSResultPage original = new PSResultPage(new java.net.URL("https://example.intsof/out.xsl"));
    Element elem = original.toXml(doc);

    PSResultPage restored = new PSResultPage(elem, null, null);
    assertEquals(original.getStyleSheet().toExternalForm(), restored.getStyleSheet().toExternalForm());
  }

  /**
   * Review mitigation: {@code fromXmlBase} must call virtual {@code setName} so upgrade-plugin
   * subclasses (whitespace-allowed names) still work on the Element-ctor path.
   */
  @Test
  public void relationshipConfigFromXmlDispatchesVirtualSetName() throws Exception {
    Document doc = PSXmlDocumentBuilder.createXmlDocument();
    PSRelationshipConfig base =
        new PSRelationshipConfig(
            "ActiveAssembly",
            PSRelationshipConfig.RS_TYPE_SYSTEM,
            PSRelationshipConfig.CATEGORY_ACTIVE_ASSEMBLY);
    Element elem = base.toXml(doc);
    elem.setAttribute("name", "Name With Spaces");

    WhitespaceRelationshipConfig restored = new WhitespaceRelationshipConfig(elem);
    assertEquals("Name With Spaces", restored.getName());
  }

  /**
   * Review mitigation: {@link PSComponent#copyFrom} must call virtual {@code setId} so subclasses
   * that override {@code setId} (e.g. PSDependency id-mirroring) still run.
   */
  @Test
  public void componentCopyFromDispatchesVirtualSetId() {
    TrackingIdComponent source = new TrackingIdComponent();
    source.applyId(77);
    TrackingIdComponent target = new TrackingIdComponent();
    target.copyFrom(source);
    assertEquals(1, target.setIdCalls);
    assertEquals(77, target.getId());
    assertEquals(77, target.lastSetId);
  }

  /** Mirrors upgrade-plugin RelationshipConfig: setName allows whitespace. */
  private static final class WhitespaceRelationshipConfig extends PSRelationshipConfig {
    WhitespaceRelationshipConfig(Element src) throws PSUnknownNodeTypeException {
      super(src, null, null);
    }

    @Override
    public void setName(String name) {
      m_name = name;
    }
  }

  /** Tracks virtual setId dispatch for copyFrom regression coverage. */
  private static final class TrackingIdComponent extends PSComponent {
    private static final long serialVersionUID = 1L;
    int setIdCalls;
    int lastSetId = -1;

    @Override
    public void setId(int id) {
      setIdCalls++;
      lastSetId = id;
      super.setId(id);
    }

    @Override
    public Element toXml(Document doc) {
      return doc.createElement("TrackingIdComponent");
    }

    @Override
    public void fromXml(Element sourceNode, IPSDocument parentDoc, java.util.List parentComponents) {
      // no-op for unit test
    }

    @Override
    public void validate(IPSValidationContext cxt) {
      // no-op for unit test
    }
  }
}
