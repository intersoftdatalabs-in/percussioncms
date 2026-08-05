/*
 * Copyright 1999-2026 Percussion Software, Inc.
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
package com.percussion.services.workflow.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.guidmgr.data.PSGuid;
import com.percussion.services.workflow.data.PSAgingTransition.PSAgingTypeEnum;
import com.percussion.services.workflow.data.PSNotification.PSStateRoleRecipientTypeEnum;
import com.percussion.services.workflow.data.PSTransition.PSWorkflowCommentEnum;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 * Golden / round-trip tests for the workflow design-object tree under the Jackson-backed {@code
 * PSXmlSerializationHelper} (issue #1890, epic #505). Offline only — no live CMS.
 */
class PSWorkflowXmlSerializationTest {

  @Test
  void writeEmitsNestedWireNamesAndSuppressesVersion() throws Exception {
    PSWorkflow original = sampleMultiStateWorkflow();
    String xml = original.toXML();

    assertNotNull(xml);
    assertFalse(xml.trim().startsWith("<null"), xml);
    assertTrue(containsTag(xml, "workflow"), "root: " + xml);
    assertTrue(containsTag(xml, "state"), xml);
    assertTrue(containsTag(xml, "role"), xml);
    assertFalse(containsTag(xml, "workflow-role"), "must pin role not workflow-role: " + xml);
    assertTrue(containsTag(xml, "notification-def"), xml);
    assertFalse(containsTag(xml, "notificationdef"), xml);
    assertTrue(containsTag(xml, "assigned-role"), xml);
    assertTrue(containsTag(xml, "transition"), xml);
    assertTrue(containsTag(xml, "aging-transition"), xml);
    assertTrue(containsTag(xml, "transition-role"), xml);
    assertTrue(containsTag(xml, "notification"), xml);
    assertTrue(containsTag(xml, "ccrecipients"), "historical ccrecipients wrapper: " + xml);
    assertFalse(containsTag(xml, "cc-recipients"), xml);
    assertFalse(containsTag(xml, "version"), "version suppressed: " + xml);
    assertTrue(containsTag(xml, "guid"), xml);
    assertTrue(xml.contains("Sample Multi-State"), xml);
  }

  @Test
  void writeMatchesGoldenFixture() throws Exception {
    String xml = sampleMultiStateWorkflow().toXML();
    String golden = loadResource("com/percussion/services/workflow/data/ps-workflow-golden.xml");
    assertLogicalXmlParity(golden, xml);
  }

  @Test
  void roundTripRestoresStatesTransitionsNotificationsAndRoles() throws Exception {
    PSWorkflow original = sampleMultiStateWorkflow();
    String xml = original.toXML();

    PSWorkflow restored = new PSWorkflow();
    restored.fromXML(xml);

    assertWorkflowGraphEquals(original, restored);
  }

  @Test
  void leafRoundTrips() throws Exception {
    PSWorkflowRole role = new PSWorkflowRole();
    role.setGUID(new PSGuid(PSTypeEnum.WORKFLOW_ROLE, 24));
    role.setName("role_24");
    role.setDescription("desc_24");
    role.setWorkflowId(12);
    PSWorkflowRole role2 = new PSWorkflowRole();
    role2.fromXML(role.toXML());
    assertEquals(role.getName(), role2.getName());
    assertEquals(role.getDescription(), role2.getDescription());
    assertEquals(role.getGUID().toString(), role2.getGUID().toString());
    assertEquals(role.getWorkflowId(), role2.getWorkflowId());

    PSNotification notification = new PSNotification();
    notification.setGUID(new PSGuid(PSTypeEnum.WORKFLOW_TRANS_NOTIFICATION, 48));
    notification.setNotificationId(1);
    notification.setTransitionId(72);
    notification.setWorkflowId(12);
    notification.setRecipients(Arrays.asList("recipient_1", "recipient_2"));
    notification.setCCRecipients(Arrays.asList("cc_1"));
    notification.setStateRoleRecipientType(PSStateRoleRecipientTypeEnum.TO_STATE_RECIPIENTS);
    PSNotification notification2 = new PSNotification();
    notification2.fromXML(notification.toXML());
    assertEquals(notification.getRecipients(), notification2.getRecipients());
    assertEquals(notification.getCCRecipients(), notification2.getCCRecipients());
    assertEquals(
        notification.getStateRoleRecipientType(), notification2.getStateRoleRecipientType());
    assertEquals(notification.getGUID().toString(), notification2.getGUID().toString());

    PSTransition transition = createTransition(72, role, notification);
    PSTransition transition2 = new PSTransition();
    transition2.fromXML(transition.toXML());
    assertEquals(transition.getLabel(), transition2.getLabel());
    assertEquals(transition.getApprovals(), transition2.getApprovals());
    assertEquals(transition.getRequiresComment(), transition2.getRequiresComment());
    assertEquals(transition.isDefaultTransition(), transition2.isDefaultTransition());
    assertEquals(transition.isAllowAllRoles(), transition2.isAllowAllRoles());
    assertEquals(1, transition2.getNotifications().size());
    assertEquals(1, transition2.getTransitionRoles().size());
    assertEquals(24L, transition2.getTransitionRoles().get(0).getRoleId());

    PSAgingTransition aging = createAgingTransition(60);
    PSAgingTransition aging2 = new PSAgingTransition();
    aging2.fromXML(aging.toXML());
    assertEquals(aging.getLabel(), aging2.getLabel());
    assertEquals(aging.getInterval(), aging2.getInterval());
    assertEquals(aging.getSystemField(), aging2.getSystemField());
    assertEquals(aging.getAgingTypeEnum(), aging2.getAgingTypeEnum());

    PSAssignedRole assigned = new PSAssignedRole();
    assigned.setGUID(new PSGuid(PSTypeEnum.WORKFLOW_ROLE, 3));
    assigned.setStateId(1);
    assigned.setWorkflowId(12);
    assigned.setAssignmentType(PSAssignmentTypeEnum.ASSIGNEE);
    assigned.setAdhocType(PSAdhocTypeEnum.DISABLED);
    assigned.setDoNotify(true);
    assigned.setShowInInbox(false);
    PSAssignedRole assigned2 = new PSAssignedRole();
    assigned2.fromXML(assigned.toXML());
    assertEquals(assigned.getAssignmentType(), assigned2.getAssignmentType());
    assertEquals(assigned.getAdhocType(), assigned2.getAdhocType());
    assertEquals(assigned.isDoNotify(), assigned2.isDoNotify());
    assertEquals(assigned.isShowInInbox(), assigned2.isShowInInbox());
    assertEquals(assigned.getGUID().toString(), assigned2.getGUID().toString());

    PSNotificationDef def = new PSNotificationDef();
    def.setGUID(new PSGuid(PSTypeEnum.WORKFLOW_NOTIFICATION, 36));
    def.setSubject("subject_1");
    def.setBody("body_1");
    def.setDescription("desc");
    def.setWorkflowId(12);
    PSNotificationDef def2 = new PSNotificationDef();
    def2.fromXML(def.toXML());
    assertEquals(def.getSubject(), def2.getSubject());
    assertEquals(def.getBody(), def2.getBody());
    assertEquals(def.getGUID().toString(), def2.getGUID().toString());
  }

  @Test
  void designExportFixtureSmoke() throws Exception {
    // Offline smoke: historical multi-state design export under webservices test resources
    String packaged =
        loadResource("com/percussion/webservices/transformation/converter/testWorkflow1.xml");
    assertTrue(packaged.contains("<workflow"), packaged);
    assertTrue(packaged.contains("<state"), packaged);
    assertTrue(packaged.contains("<aging-transition"), packaged);

    PSWorkflow restored = new PSWorkflow();
    restored.fromXML(packaged);

    assertEquals("Simple Workflow", restored.getName());
    assertEquals("Admin", restored.getAdministratorRole());
    assertEquals(1L, restored.getInitialStateId());
    assertFalse(restored.getStates().isEmpty());
    assertFalse(restored.getRoles().isEmpty());
    assertFalse(restored.getNotificationDefs().isEmpty());

    boolean foundAging = false;
    boolean foundTransition = false;
    for (PSState state : restored.getStates()) {
      if (!state.getAgingTransitions().isEmpty()) {
        foundAging = true;
      }
      if (!state.getTransitions().isEmpty()) {
        foundTransition = true;
      }
    }
    assertTrue(foundAging, "fixture should restore aging transitions");
    assertTrue(foundTransition, "fixture should restore transitions");
  }

  private static PSWorkflow sampleMultiStateWorkflow() {
    PSWorkflowRole roleAdmin = createWorkflowRole(1, "Admin");
    PSWorkflowRole roleEditor = createWorkflowRole(2, "Editor");

    PSNotification notification = new PSNotification();
    notification.setGUID(new PSGuid(PSTypeEnum.WORKFLOW_TRANS_NOTIFICATION, 101));
    notification.setNotificationId(1);
    notification.setTransitionId(10);
    notification.setWorkflowId(12);
    notification.setRecipients(Arrays.asList("editor@example.com"));
    notification.setCCRecipients(Arrays.asList("admin@example.com"));
    notification.setStateRoleRecipientType(PSStateRoleRecipientTypeEnum.TO_STATE_RECIPIENTS);

    PSTransition submit = createTransition(10, roleAdmin, notification);
    submit.setLabel("Submit");
    submit.setTrigger("Submit");
    submit.setDescription("Submit for review");
    submit.setStateId(1);
    submit.setToState(2);
    submit.setWorkflowId(12);
    submit.setDefaultTransition(true);
    submit.setAllowAllRoles(false);

    PSAgingTransition aging = createAgingTransition(20);
    aging.setStateId(2);
    aging.setToState(3);
    aging.setWorkflowId(12);

    PSAssignedRole draftRole = new PSAssignedRole();
    draftRole.setGUID(new PSGuid(PSTypeEnum.WORKFLOW_ROLE, 1));
    draftRole.setStateId(1);
    draftRole.setWorkflowId(12);
    draftRole.setAssignmentType(PSAssignmentTypeEnum.ASSIGNEE);
    draftRole.setAdhocType(PSAdhocTypeEnum.DISABLED);
    draftRole.setDoNotify(false);
    draftRole.setShowInInbox(true);

    PSState draft = new PSState();
    draft.setGUID(new PSGuid(PSTypeEnum.WORKFLOW_STATE, 1));
    draft.setName("Draft");
    draft.setDescription("Where draft content is first created");
    draft.setSortOrder(10);
    draft.setWorkflowId(12);
    draft.setPublishable(false);
    draft.setAssignedRoles(List.of(draftRole));
    draft.setTransitions(List.of(submit));
    draft.setAgingTransitions(new ArrayList<>());

    PSAssignedRole reviewRole = new PSAssignedRole();
    reviewRole.setGUID(new PSGuid(PSTypeEnum.WORKFLOW_ROLE, 2));
    reviewRole.setStateId(2);
    reviewRole.setWorkflowId(12);
    reviewRole.setAssignmentType(PSAssignmentTypeEnum.ASSIGNEE);
    reviewRole.setAdhocType(PSAdhocTypeEnum.DISABLED);
    reviewRole.setDoNotify(true);
    reviewRole.setShowInInbox(true);

    PSState review = new PSState();
    review.setGUID(new PSGuid(PSTypeEnum.WORKFLOW_STATE, 2));
    review.setName("Review");
    review.setDescription("Content is reviewed here");
    review.setSortOrder(20);
    review.setWorkflowId(12);
    review.setPublishable(false);
    review.setAssignedRoles(List.of(reviewRole));
    review.setTransitions(new ArrayList<>());
    review.setAgingTransitions(List.of(aging));

    PSNotificationDef def = new PSNotificationDef();
    def.setGUID(new PSGuid(PSTypeEnum.WORKFLOW_NOTIFICATION, 1));
    def.setSubject("Content is awaiting your approval");
    def.setBody("Please review the content item.");
    def.setDescription("Notification template for users.");
    def.setWorkflowId(12);

    PSWorkflow wf = new PSWorkflow();
    wf.setGUID(new PSGuid(PSTypeEnum.WORKFLOW, 12));
    wf.setName("Sample Multi-State");
    wf.setDescription("Two-state sample for Jackson golden/round-trip");
    wf.setAdministratorRole("Admin");
    wf.setInitialStateId(1);
    wf.setRoles(List.of(roleAdmin, roleEditor));
    wf.setStates(List.of(draft, review));
    wf.setNotificationDefs(List.of(def));
    return wf;
  }

  private static PSWorkflowRole createWorkflowRole(int roleId, String name) {
    PSWorkflowRole role = new PSWorkflowRole();
    role.setGUID(new PSGuid(PSTypeEnum.WORKFLOW_ROLE, roleId));
    role.setName(name);
    role.setDescription("desc_" + name);
    role.setWorkflowId(12);
    return role;
  }

  private static PSAgingTransition createAgingTransition(int uuId) {
    PSAgingTransition agingTransition = new PSAgingTransition();
    agingTransition.setGUID(new PSGuid(PSTypeEnum.WORKFLOW_TRANSITION, uuId));
    agingTransition.setLabel("agingLabel_" + uuId);
    agingTransition.setDescription("agingDesc_" + uuId);
    agingTransition.setTrigger("Age" + uuId);
    agingTransition.setInterval(1);
    agingTransition.setSystemField("CONTENTSTARTDATE");
    agingTransition.setType(PSAgingTypeEnum.SYSTEM_FIELD);
    return agingTransition;
  }

  private static PSTransition createTransition(
      int uuId, PSWorkflowRole role, PSNotification notification) {
    PSTransition transition = new PSTransition();
    transition.setGUID(new PSGuid(PSTypeEnum.WORKFLOW_TRANSITION, uuId));
    transition.setLabel("tranLabel_" + uuId);
    transition.setTrigger("trigger_" + uuId);
    transition.setDescription("tranDesc_" + uuId);
    transition.setApprovals(1);
    transition.setRequiresComment(PSWorkflowCommentEnum.OPTIONAL);
    transition.setDefaultTransition(false);
    transition.setAllowAllRoles(false);
    transition.setNotifications(List.of(notification));
    PSTransitionRole transRole = new PSTransitionRole();
    transRole.setWorkflowId(role.getWorkflowId());
    transRole.setTransitionId(uuId);
    transRole.setRoleId(role.getGUID().longValue());
    transition.setTransitionRoles(List.of(transRole));
    return transition;
  }

  private static void assertWorkflowGraphEquals(PSWorkflow expected, PSWorkflow actual) {
    assertNotNull(actual);
    assertEquals(expected.getName(), actual.getName());
    assertEquals(expected.getDescription(), actual.getDescription());
    assertEquals(expected.getAdministratorRole(), actual.getAdministratorRole());
    assertEquals(expected.getInitialStateId(), actual.getInitialStateId());
    assertEquals(expected.getGUID().toString(), actual.getGUID().toString());

    assertEquals(expected.getRoles().size(), actual.getRoles().size());
    for (int i = 0; i < expected.getRoles().size(); i++) {
      PSWorkflowRole e = expected.getRoles().get(i);
      PSWorkflowRole a = actual.getRoles().get(i);
      assertEquals(e.getName(), a.getName());
      assertEquals(e.getGUID().toString(), a.getGUID().toString());
      assertEquals(e.getWorkflowId(), a.getWorkflowId());
    }

    assertEquals(expected.getNotificationDefs().size(), actual.getNotificationDefs().size());
    for (int i = 0; i < expected.getNotificationDefs().size(); i++) {
      PSNotificationDef e = expected.getNotificationDefs().get(i);
      PSNotificationDef a = actual.getNotificationDefs().get(i);
      assertEquals(e.getSubject(), a.getSubject());
      assertEquals(e.getBody(), a.getBody());
      assertEquals(e.getGUID().toString(), a.getGUID().toString());
    }

    assertEquals(expected.getStates().size(), actual.getStates().size());
    for (int i = 0; i < expected.getStates().size(); i++) {
      PSState e = expected.getStates().get(i);
      PSState a = actual.getStates().get(i);
      assertEquals(e.getName(), a.getName());
      assertEquals(e.getStateId(), a.getStateId());
      assertEquals(e.getWorkflowId(), a.getWorkflowId());
      assertEquals(e.getSortOrder(), a.getSortOrder());
      assertEquals(e.isPublishable(), a.isPublishable());
      assertEquals(e.getAssignedRoles().size(), a.getAssignedRoles().size());
      assertEquals(e.getTransitions().size(), a.getTransitions().size());
      assertEquals(e.getAgingTransitions().size(), a.getAgingTransitions().size());

      if (!e.getTransitions().isEmpty()) {
        PSTransition et = e.getTransitions().get(0);
        PSTransition at = a.getTransitions().get(0);
        assertEquals(et.getLabel(), at.getLabel());
        assertEquals(et.getTrigger(), at.getTrigger());
        assertEquals(et.getApprovals(), at.getApprovals());
        assertEquals(et.getNotifications().size(), at.getNotifications().size());
        assertEquals(et.getTransitionRoles().size(), at.getTransitionRoles().size());
        if (!et.getNotifications().isEmpty()) {
          assertEquals(
              et.getNotifications().get(0).getRecipients(),
              at.getNotifications().get(0).getRecipients());
          assertEquals(
              et.getNotifications().get(0).getCCRecipients(),
              at.getNotifications().get(0).getCCRecipients());
        }
      }
      if (!e.getAgingTransitions().isEmpty()) {
        PSAgingTransition ea = e.getAgingTransitions().get(0);
        PSAgingTransition aa = a.getAgingTransitions().get(0);
        assertEquals(ea.getLabel(), aa.getLabel());
        assertEquals(ea.getAgingTypeEnum(), aa.getAgingTypeEnum());
        assertEquals(ea.getSystemField(), aa.getSystemField());
      }
    }
  }

  private static boolean containsTag(String xml, String localName) {
    return xml.contains("<" + localName) || xml.contains("</" + localName + ">");
  }

  private static String loadResource(String classpath) throws Exception {
    try (InputStream in =
        PSWorkflowXmlSerializationTest.class.getClassLoader().getResourceAsStream(classpath)) {
      assertNotNull(in, "missing test resource: " + classpath);
      return new String(in.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  /**
   * Compare logical XML trees: ignore XML declaration, Betwixt graph-identity {@code id}
   * attributes, insignificant whitespace, and HTML comments.
   */
  private static void assertLogicalXmlParity(String expectedXml, String actualXml)
      throws Exception {
    Document expected = parseXml(stripXmlDeclaration(expectedXml));
    Document actual = parseXml(stripXmlDeclaration(actualXml));
    assertElementTreeEquals(expected.getDocumentElement(), actual.getDocumentElement(), "/");
  }

  private static String stripXmlDeclaration(String xml) {
    String s = Objects.requireNonNull(xml).trim();
    if (s.startsWith("<?xml")) {
      int end = s.indexOf("?>");
      if (end >= 0) {
        s = s.substring(end + 2).trim();
      }
    }
    while (s.startsWith("<!--")) {
      int end = s.indexOf("-->");
      if (end < 0) {
        break;
      }
      s = s.substring(end + 3).trim();
    }
    return s;
  }

  private static Document parseXml(String xml) throws Exception {
    var factory = DocumentBuilderFactory.newInstance();
    factory.setNamespaceAware(false);
    factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
    var builder = factory.newDocumentBuilder();
    return builder.parse(new InputSource(new java.io.StringReader(xml)));
  }

  private static void assertElementTreeEquals(Element expected, Element actual, String path) {
    assertEquals(expected.getTagName(), actual.getTagName(), "tag at " + path);
    List<Node> eChildren = significantChildren(expected);
    List<Node> aChildren = significantChildren(actual);
    assertEquals(
        eChildren.size(),
        aChildren.size(),
        "child count at "
            + path
            + " expected="
            + summarize(eChildren)
            + " actual="
            + summarize(aChildren));
    for (int i = 0; i < eChildren.size(); i++) {
      Node en = eChildren.get(i);
      Node an = aChildren.get(i);
      if (en.getNodeType() == Node.TEXT_NODE) {
        assertEquals(en.getTextContent().trim(), an.getTextContent().trim(), "text at " + path);
      } else {
        assertElementTreeEquals(
            (Element) en, (Element) an, path + "/" + ((Element) en).getTagName() + "[" + i + "]");
      }
    }
  }

  private static List<Node> significantChildren(Element el) {
    NodeList nl = el.getChildNodes();
    java.util.ArrayList<Node> out = new java.util.ArrayList<>();
    boolean hasElementChild = false;
    for (int i = 0; i < nl.getLength(); i++) {
      if (nl.item(i).getNodeType() == Node.ELEMENT_NODE) {
        hasElementChild = true;
        break;
      }
    }
    for (int i = 0; i < nl.getLength(); i++) {
      Node n = nl.item(i);
      if (n.getNodeType() == Node.ELEMENT_NODE) {
        out.add(n);
      } else if (n.getNodeType() == Node.TEXT_NODE && !hasElementChild) {
        String t = n.getTextContent();
        if (t != null && !t.trim().isEmpty()) {
          out.add(n);
        }
      }
    }
    return out;
  }

  private static String summarize(List<Node> nodes) {
    StringBuilder b = new StringBuilder("[");
    for (int i = 0; i < nodes.size(); i++) {
      if (i > 0) {
        b.append(',');
      }
      Node n = nodes.get(i);
      if (n.getNodeType() == Node.ELEMENT_NODE) {
        b.append(((Element) n).getTagName());
      } else {
        b.append("#text");
      }
    }
    return b.append(']').toString();
  }
}
