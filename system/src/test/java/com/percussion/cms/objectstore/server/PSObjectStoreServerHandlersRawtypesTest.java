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
package com.percussion.cms.objectstore.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.cms.IPSConstants;
import com.percussion.content.IPSMimeContentTypes;
import com.percussion.design.objectstore.PSLocator;
import com.percussion.extension.PSParameterMismatchException;
import com.percussion.system.utils.IPSHtmlParameters;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Behavioral unit tests for typed residual server-handler helpers (issue #2624 / parent #2022 slice
 * 2h). Full CMS stack paths remain integration-tested; these cover pure parameterization contracts.
 */
@Tag("UnitTest")
public class PSObjectStoreServerHandlersRawtypesTest {

  @Test
  void fieldRetrieverPrepareParamsIncludesBinaryCommandAndIds() {
    PSFieldRetriever retriever = new PSFieldRetriever(42L);
    PSLocator item = new PSLocator(10, 2);

    Map<String, Object> params = retriever.prepareParams(item, "body", -1);

    assertEquals(IPSMimeContentTypes.MIME_ENC_BINARY, params.get(IPSHtmlParameters.SYS_COMMAND));
    assertEquals(Integer.valueOf(10), params.get(IPSHtmlParameters.SYS_CONTENTID));
    assertEquals(Integer.valueOf(2), params.get(IPSHtmlParameters.SYS_REVISION));
    assertEquals("body", params.get(IPSConstants.SUBMITNAME_PARAM_NAME));
    assertFalse(params.containsKey("sys_childrowid"));
  }

  @Test
  void fieldRetrieverPrepareParamsAddsChildRowIdWhenNonNegative() {
    PSFieldRetriever retriever = new PSFieldRetriever(1L);
    PSLocator item = new PSLocator(5, 1);

    Map<String, Object> params = retriever.prepareParams(item, "image", 7);

    assertEquals("7", params.get("sys_childrowid"));
  }

  @Test
  void fieldRetrieverRejectsInvalidLocator() {
    PSFieldRetriever retriever = new PSFieldRetriever(1L);
    assertFalse(retriever.isLocatorValid(new PSLocator(0, 1)));
    assertFalse(retriever.isLocatorValid(new PSLocator(1, 0)));
    assertTrue(retriever.isLocatorValid(new PSLocator(1, 1)));
  }

  @Test
  void authTypesParseSkipsEmptyValuesAndShortKeys() {
    Properties props = new Properties();
    props.setProperty("authtype.1", "sys_aa/RelatedContent");
    props.setProperty("authtype.2", "");
    props.setProperty("short", "ignored-because-name-not-longer-than-prefix");
    props.setProperty("authtype.99", "app/resource");

    Map<String, String> map = PSAuthTypePropertiesParser.parse(props);

    assertEquals(2, map.size());
    assertEquals("sys_aa/RelatedContent", map.get("1"));
    assertEquals("app/resource", map.get("99"));
    assertFalse(map.containsKey("2"));
  }

  @Test
  void authTypesParseRejectsNullProperties() {
    assertThrows(IllegalArgumentException.class, () -> PSAuthTypePropertiesParser.parse(null));
  }

  @Test
  void catalogHandlerCollectFieldNamesFromListAndScalar() {
    Set<String> fromList =
        PSCatalogServerObjectHandler.collectFieldNames(Arrays.asList("a", null, "b", "a"));
    assertEquals(Set.of("a", "b"), fromList);

    Set<String> fromScalar = PSCatalogServerObjectHandler.collectFieldNames("solo");
    assertEquals(Set.of("solo"), fromScalar);

    assertTrue(PSCatalogServerObjectHandler.collectFieldNames(null).isEmpty());
  }

  @Test
  void loadChildDataExitSnapshotElementsIsIndependentOfLiveNodeList() throws Exception {
    Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
    Element root = doc.createElement("root");
    doc.appendChild(root);
    Element c1 = doc.createElement("child");
    Element c2 = doc.createElement("child");
    root.appendChild(c1);
    root.appendChild(c2);

    NodeList live = root.getElementsByTagName("child");
    List<Element> snapshot = PSLoadChildDataExit.snapshotElements(live);
    assertEquals(2, snapshot.size());

    root.removeChild(c1);
    assertEquals(1, live.getLength());
    assertEquals(2, snapshot.size());
    assertTrue(snapshot.contains(c1));
    assertTrue(snapshot.contains(c2));

    assertTrue(PSLoadChildDataExit.snapshotElements(null).isEmpty());
  }

  @Test
  void loadChildDataExitRejectsMissingParams() throws Exception {
    PSLoadChildDataExit exit = new PSLoadChildDataExit();
    Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
    doc.appendChild(doc.createElement("root"));

    assertThrows(
        PSParameterMismatchException.class, () -> exit.processResultDocument(null, null, doc));
    assertThrows(
        PSParameterMismatchException.class,
        () -> exit.processResultDocument(new Object[] {}, null, doc));
    assertThrows(
        PSParameterMismatchException.class,
        () -> exit.processResultDocument(new Object[] {"base"}, null, doc));
    assertThrows(
        PSParameterMismatchException.class,
        () -> exit.processResultDocument(new Object[] {"base", "child"}, null, doc));
  }

  @Test
  void loadChildDataExitReturnsDocWhenNoRoot() throws Exception {
    PSLoadChildDataExit exit = new PSLoadChildDataExit();
    Document empty = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
    Document result =
        exit.processResultDocument(new Object[] {"base", "child", "query"}, null, empty);
    assertEquals(empty, result);
  }
}
