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
package com.percussion.error;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.util.PSBase64Decoder;
import com.percussion.util.PSBase64Encoder;
import com.percussion.util.PSHttpConnection;
import com.percussion.util.UtilErrorCode;
import com.percussion.utils.container.PSJBossConnectors;
import com.percussion.utils.container.PSMissingApplicationPolicyException;
import com.percussion.utils.container.PSSecureCredentials;
import com.percussion.utils.container.jboss.IPSJBossErrors;
import com.percussion.utils.container.jboss.JBossErrorCode;
import com.percussion.utils.container.jboss.PSJBossJndiDatasource;
import com.percussion.utils.spring.IPSBeanConfig;
import com.percussion.utils.spring.PSSpringBeanUtils;
import com.percussion.utils.spring.PSSpringConfiguration;
import com.percussion.utils.xml.IPSXmlErrors;
import com.percussion.utils.xml.PSInvalidXmlException;
import com.percussion.utils.xml.PSXmlUtils;
import com.percussion.utils.xml.XmlErrorCode;
import com.percussion.xml.PSXmlDocumentBuilder;
import com.percussion.xml.PSXmlTreeWalker;
import com.sun.net.httpserver.HttpServer;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Issue #3859 (parent #2616): leftover {@code modules/utils} production sites throw typed {@code
 * *ErrorCode} peers (utils-local {@link IPSErrorCode} matching perc-auditlog catalogs) — not bare
 * {@code IPS*Errors} ints. Dual-write is skipped ({@code isAuditable() == false}).
 */
@Tag("UnitTest")
class PSUtilsLeftoverErrorCodesSliceTest {

  @TempDir Path tempDir;

  @Test
  void utilErrorCodePeersMatchLegacyIntsAndSkipDualWrite() {
    assertEquals(
        com.percussion.util.IPSUtilErrors.BASE64_ENCODING_EXCEPTION,
        UtilErrorCode.BASE64_ENCODING_EXCEPTION.numericCode());
    assertEquals(
        com.percussion.util.IPSUtilErrors.RECEIVE_DATA_ERROR,
        UtilErrorCode.RECEIVE_DATA_ERROR.numericCode());
    assertEquals(
        com.percussion.util.IPSUtilErrors.POST_DATA_ERROR,
        UtilErrorCode.POST_DATA_ERROR.numericCode());
    for (UtilErrorCode code : UtilErrorCode.values()) {
      assertFalse(code.isAuditable(), code.name());
    }
  }

  @Test
  void xmlAndJbossErrorCodePeersMatchLegacyIntsAndSkipDualWrite() {
    assertEquals(IPSXmlErrors.XML_ELEMENT_MISSING, XmlErrorCode.XML_ELEMENT_MISSING.numericCode());
    assertEquals(
        IPSXmlErrors.XML_ELEMENT_INVALID_VALUE,
        XmlErrorCode.XML_ELEMENT_INVALID_VALUE.numericCode());
    assertEquals(
        IPSXmlErrors.XML_TWO_ROOT_ELEMENTS, XmlErrorCode.XML_TWO_ROOT_ELEMENTS.numericCode());
    assertEquals(
        IPSXmlErrors.XML_ELEMENT_INVALID_ATTR, XmlErrorCode.XML_ELEMENT_INVALID_ATTR.numericCode());
    assertEquals(
        IPSXmlErrors.XML_ELEMENT_ATTR_INVALID_VAL,
        XmlErrorCode.XML_ELEMENT_ATTR_INVALID_VAL.numericCode());
    assertEquals(IPSXmlErrors.XML_RESTORE_ERROR, XmlErrorCode.XML_RESTORE_ERROR.numericCode());
    assertEquals(
        IPSJBossErrors.APP_POLICY_ELEMENT_MISSING,
        JBossErrorCode.APP_POLICY_ELEMENT_MISSING.numericCode());
    for (XmlErrorCode code : XmlErrorCode.values()) {
      assertFalse(code.isAuditable(), code.name());
    }
    assertFalse(JBossErrorCode.APP_POLICY_ELEMENT_MISSING.isAuditable());
  }

  @Test
  void base64EncoderAndDecoderThrowTypedNonAuditableRuntimeException() {
    PSRuntimeException encodeEx =
        assertThrows(
            PSRuntimeException.class, () -> PSBase64Encoder.encode("payload", "not-a-charset"));
    assertSame(UtilErrorCode.BASE64_ENCODING_EXCEPTION, encodeEx.getTypedErrorCode());
    assertEquals(
        UtilErrorCode.BASE64_ENCODING_EXCEPTION.numericCode(), encodeEx.getErrorCode());
    assertFalse(encodeEx.isAuditable());

    PSRuntimeException decodeEx =
        assertThrows(
            PSRuntimeException.class, () -> PSBase64Decoder.decode("YQ==", "not-a-charset"));
    assertSame(UtilErrorCode.BASE64_ENCODING_EXCEPTION, decodeEx.getTypedErrorCode());
    assertFalse(decodeEx.isAuditable());
  }

  @Test
  void runtimeExceptionTypedCtorRejectsNullCode() {
    assertThrows(
        IllegalArgumentException.class, () -> new PSRuntimeException((IPSErrorCode) null));
  }

  @Test
  void httpGetJsonHttpErrorUsesTypedPostDataError() throws Exception {
    HttpServer server =
        HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
    server.createContext(
        "/json",
        exchange -> {
          byte[] body = "{}".getBytes(StandardCharsets.UTF_8);
          exchange.sendResponseHeaders(400, body.length);
          exchange.getResponseBody().write(body);
          exchange.close();
        });
    server.start();
    try {
      int port = server.getAddress().getPort();
      URL url =
          URI.create("http://127.0.0.1:" + port + "/json?pssessionid=sid").toURL();
      PSHttpConnection conn = new PSHttpConnection(url, "sid");
      PSException ex = assertThrows(PSException.class, () -> conn.getJSON(url));
      assertSame(UtilErrorCode.POST_DATA_ERROR, ex.getTypedErrorCode());
      assertEquals(UtilErrorCode.POST_DATA_ERROR.numericCode(), ex.getErrorCode());
      assertFalse(ex.isAuditable());
    } finally {
      server.stop(0);
    }
  }

  @Test
  void xmlUtilsMissingAndInvalidUseTypedCodes() throws Exception {
    PSInvalidXmlException missing =
        assertThrows(
            PSInvalidXmlException.class, () -> PSXmlUtils.getElementData(null, "title", true));
    assertSame(XmlErrorCode.XML_ELEMENT_MISSING, missing.getTypedErrorCode());
    assertFalse(missing.isAuditable());

    Document doc = PSXmlDocumentBuilder.createXmlDocument();
    Element empty = PSXmlDocumentBuilder.createRoot(doc, "title");
    PSInvalidXmlException invalidValue =
        assertThrows(
            PSInvalidXmlException.class, () -> PSXmlUtils.getElementData(empty, "title", true));
    assertSame(XmlErrorCode.XML_ELEMENT_INVALID_VALUE, invalidValue.getTypedErrorCode());

    Document attrDoc = PSXmlDocumentBuilder.createXmlDocument();
    Element root = PSXmlDocumentBuilder.createRoot(attrDoc, "Root");
    PSInvalidXmlException invalidAttr =
        assertThrows(
            PSInvalidXmlException.class, () -> PSXmlUtils.checkAttribute(root, "req", true));
    assertSame(XmlErrorCode.XML_ELEMENT_INVALID_ATTR, invalidAttr.getTypedErrorCode());
    assertFalse(invalidAttr.isAuditable());
  }

  @Test
  void missingApplicationPolicyUsesTypedJbossCode() {
    PSMissingApplicationPolicyException ex =
        new PSMissingApplicationPolicyException("rx.policy", "login-config.xml");
    assertSame(JBossErrorCode.APP_POLICY_ELEMENT_MISSING, ex.getTypedErrorCode());
    assertEquals(
        JBossErrorCode.APP_POLICY_ELEMENT_MISSING.numericCode(), ex.getErrorCode());
    assertFalse(ex.isAuditable());
  }

  @Test
  void secureCredentialsXmlErrorsUseTypedCodes() throws Exception {
    Document doc = PSXmlDocumentBuilder.createXmlDocument();
    Element unnamed = doc.createElement(PSSecureCredentials.APP_POLICY_NODE_NAME);
    PSInvalidXmlException invalidAttr =
        assertThrows(PSInvalidXmlException.class, () -> new PSSecureCredentials(unnamed));
    assertSame(XmlErrorCode.XML_ELEMENT_INVALID_ATTR, invalidAttr.getTypedErrorCode());

    Element named = doc.createElement(PSSecureCredentials.APP_POLICY_NODE_NAME);
    named.setAttribute("name", "rx.datasource.jdbc_rx");
    PSInvalidXmlException missingAuth =
        assertThrows(PSInvalidXmlException.class, () -> new PSSecureCredentials(named));
    assertSame(XmlErrorCode.XML_ELEMENT_MISSING, missingAuth.getTypedErrorCode());
    assertFalse(missingAuth.isAuditable());
  }

  @Test
  void jbossDatasourceXmlErrorsUseTypedCodes() throws Exception {
    Document doc = PSXmlDocumentBuilder.createXmlDocument();
    Element empty = doc.createElement(PSJBossJndiDatasource.DATASOURCE_NODE_NAME);
    PSInvalidXmlException missingName =
        assertThrows(PSInvalidXmlException.class, () -> new PSJBossJndiDatasource(empty));
    assertSame(XmlErrorCode.XML_ELEMENT_MISSING, missingName.getTypedErrorCode());

    Element source = doc.createElement(PSJBossJndiDatasource.DATASOURCE_NODE_NAME);
    appendChild(doc, source, "jndi-name", "jdbc/x");
    appendChild(doc, source, "connection-url", "jdbc:jtds:sqlserver://host:1433");
    appendChild(doc, source, "driver-class", "net.sourceforge.jtds.jdbc.Driver");
    appendChild(doc, source, "min-pool-size", "not-an-int");
    PSInvalidXmlException invalidInt =
        assertThrows(PSInvalidXmlException.class, () -> new PSJBossJndiDatasource(source));
    assertSame(XmlErrorCode.XML_ELEMENT_INVALID_VALUE, invalidInt.getTypedErrorCode());
    assertFalse(invalidInt.isAuditable());
  }

  @Test
  void jbossDatasourceMissingMetadataUsesTypedCode() throws Exception {
    Document doc = PSXmlDocumentBuilder.createXmlDocument();
    Element source = doc.createElement(PSJBossJndiDatasource.DATASOURCE_NODE_NAME);
    appendChild(doc, source, "jndi-name", "jdbc/x");
    appendChild(doc, source, "connection-url", "jdbc:jtds:sqlserver://host:1433");
    appendChild(doc, source, "driver-class", "net.sourceforge.jtds.jdbc.Driver");
    appendChild(doc, source, "min-pool-size", "1");
    appendChild(doc, source, "max-pool-size", "10");
    appendChild(doc, source, "idle-timeout-minutes", "15");
    PSInvalidXmlException missingMeta =
        assertThrows(PSInvalidXmlException.class, () -> new PSJBossJndiDatasource(source));
    assertSame(XmlErrorCode.XML_ELEMENT_MISSING, missingMeta.getTypedErrorCode());
  }

  @Test
  void xmlConnectorsMissingServerRootUsesTypedCode() throws Exception {
    Path deployer =
        tempDir.resolve(
            Path.of("AppServer", "server", "rx", "deploy", "jboss-web.deployer"));
    Files.createDirectories(deployer);
    Files.writeString(deployer.resolve("server.xml"), "<NotServer/>");

    PSJBossConnectors connectors = new PSJBossConnectors(tempDir.toFile());
    RuntimeException wrap = assertThrows(RuntimeException.class, connectors::load);
    assertTrue(wrap.getCause() instanceof PSInvalidXmlException);
    PSInvalidXmlException ex = (PSInvalidXmlException) wrap.getCause();
    assertSame(XmlErrorCode.XML_ELEMENT_MISSING, ex.getTypedErrorCode());
    assertFalse(ex.isAuditable());
  }

  @Test
  void springConfigurationWrongRootUsesTypedCode() throws Exception {
    Path config = tempDir.resolve("not-beans.xml");
    Files.writeString(config, "<notbeans/>");
    PSInvalidXmlException ex =
        assertThrows(
            PSInvalidXmlException.class, () -> new PSSpringConfiguration(config.toFile()));
    assertSame(XmlErrorCode.XML_ELEMENT_MISSING, ex.getTypedErrorCode());
    assertFalse(ex.isAuditable());
  }

  @Test
  void springBeanUtilsRestoreAndMissingPropertyUseTypedCodes() throws Exception {
    Document doc = PSXmlDocumentBuilder.createXmlDocument();
    Element bean = PSXmlDocumentBuilder.createRoot(doc, IPSBeanConfig.BEAN_NODE_NAME);
    bean.setAttribute("id", "x");
    bean.setAttribute("class", "java.lang.String");

    PSInvalidXmlException restore =
        assertThrows(
            PSInvalidXmlException.class,
            () -> PSSpringBeanUtils.createBean("java.lang.String", bean));
    assertSame(XmlErrorCode.XML_RESTORE_ERROR, restore.getTypedErrorCode());
    assertFalse(restore.isAuditable());

    PSInvalidXmlException missingProp =
        assertThrows(
            PSInvalidXmlException.class,
            () -> PSSpringBeanUtils.getNextPropertyElement(bean, null, "missing"));
    assertSame(XmlErrorCode.XML_ELEMENT_MISSING, missingProp.getTypedErrorCode());
  }

  @Test
  void springBeanUtilsValidateRootUsesTypedInvalidAttr() throws Exception {
    Document doc = PSXmlDocumentBuilder.createXmlDocument();
    Element bean = PSXmlDocumentBuilder.createRoot(doc, IPSBeanConfig.BEAN_NODE_NAME);
    bean.setAttribute("id", "actual");
    bean.setAttribute("class", "com.example.Actual");
    PSInvalidXmlException ex =
        assertThrows(
            PSInvalidXmlException.class,
            () ->
                PSSpringBeanUtils.validateBeanRootElement(
                    "expected", "com.example.Expected", bean));
    assertSame(XmlErrorCode.XML_ELEMENT_INVALID_ATTR, ex.getTypedErrorCode());
  }

  @Test
  void xmlTreeWalkerTwoRootsConstructsTypedInvalidXmlThenRuntimeException() {
    List<String> twoTrees = Arrays.asList("alpha/child", "beta/child");
    RuntimeException ex =
        assertThrows(
            RuntimeException.class, () -> PSXmlTreeWalker.getLowestLevelElement(twoTrees));
    PSInvalidXmlException typed =
        new PSInvalidXmlException(XmlErrorCode.XML_TWO_ROOT_ELEMENTS, new Object[] {"alpha", "beta"});
    assertSame(XmlErrorCode.XML_TWO_ROOT_ELEMENTS, typed.getTypedErrorCode());
    assertFalse(typed.isAuditable());
    assertEquals(typed.getLocalizedMessage(), ex.getMessage());
  }

  private static void appendChild(Document doc, Element parent, String name, String value) {
    Element child = doc.createElement(name);
    child.appendChild(doc.createTextNode(value));
    parent.appendChild(child);
  }
}
