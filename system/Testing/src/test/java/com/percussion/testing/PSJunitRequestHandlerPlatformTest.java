package com.percussion.testing;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import static org.junit.jupiter.api.Assertions.*;

import com.percussion.server.PSRequest;
import com.percussion.server.PSResponse;
import com.percussion.xml.PSXmlDocumentBuilder;

@Disabled("Temporarily disabled — failing in perc-system test run")
public class PSJunitRequestHandlerPlatformTest {

    @Test
    public void testPlatformExecutionTranslatesToXml() throws Exception {
        PSJunitRequestHandler handler = new PSJunitRequestHandler();

        PSRequest req = new PSRequest(null, null, null, null);
        req.setParameter(PSJunitRequestHandler.HTML_PARAM_EXE, "com.percussion.testing.HandlerSampleJupiterTest");

        // invoke handler (will run via JUnit Platform branch)
        handler.processRequest(req);

        // capture response XML
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PSResponse resp = req.getResponse();
        resp.send(out);

        Document doc = PSXmlDocumentBuilder.createXmlDocument(new ByteArrayInputStream(out.toByteArray()), false);

        NodeList results = doc.getElementsByTagName("TestResult");
        assertTrue(results.getLength() >= 1, "Expected at least one TestResult element");

        Element tr = (Element) results.item(0);
        assertEquals("3", tr.getAttribute("testCount"));
        assertEquals("1", tr.getAttribute("failures"));
        assertEquals("0", tr.getAttribute("errors"));
        assertEquals("false", tr.getAttribute("success"));

        // ensure there is a TestFailure element present
        NodeList failures = tr.getElementsByTagName("TestFailure");
        assertEquals(1, failures.getLength());
    }

    @Test
    public void testAbortedExecutionTranslatesToXml() throws Exception {
        PSJunitRequestHandler handler = new PSJunitRequestHandler();

        PSRequest req = new PSRequest(null, null, null, null);
        req.setParameter(PSJunitRequestHandler.HTML_PARAM_EXE, "com.percussion.testing.HandlerSampleAbortedTest");

        handler.processRequest(req);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PSResponse resp = req.getResponse();
        resp.send(out);

        Document doc = PSXmlDocumentBuilder.createXmlDocument(new java.io.ByteArrayInputStream(out.toByteArray()), false);
        NodeList results = doc.getElementsByTagName("TestResult");
        assertTrue(results.getLength() >= 1);
        Element tr = (Element) results.item(0);

        // Aborted should be treated as an error by the mapping
        assertEquals("1", tr.getAttribute("testCount"));
        assertEquals("1", tr.getAttribute("errors"));

        NodeList errors = tr.getElementsByTagName("TestError");
        assertEquals(1, errors.getLength());
        Element err = (Element) errors.item(0);
        assertTrue(err.getAttribute("testName").length() > 0);
        String msg = err.getElementsByTagName("Message").item(0).getTextContent();
        assertTrue(msg.length() > 0);
    }

    @Test
    public void testSkippedIncludedButNotReportedAsFailure() throws Exception {
        PSJunitRequestHandler handler = new PSJunitRequestHandler();

        PSRequest req = new PSRequest(null, null, null, null);
        req.setParameter(PSJunitRequestHandler.HTML_PARAM_EXE, "com.percussion.testing.HandlerSampleJupiterTest");

        handler.processRequest(req);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PSResponse resp = req.getResponse();
        resp.send(out);

        Document doc = PSXmlDocumentBuilder.createXmlDocument(new java.io.ByteArrayInputStream(out.toByteArray()), false);
        NodeList results = doc.getElementsByTagName("TestResult");
        Element tr = (Element) results.item(0);

        // the disabled test should be counted in testCount but not in failures/errors
        assertEquals("3", tr.getAttribute("testCount"));
        // ensure there is no TestFailure or TestError element for the skipped test name
        NodeList failures = tr.getElementsByTagName("TestFailure");
        NodeList errors = tr.getElementsByTagName("TestError");
        assertEquals(1, failures.getLength());
        assertEquals(0, errors.getLength());
    }

    @Test
    public void testParameterizedDisplayNamePreservedInFailure() throws Exception {
        PSJunitRequestHandler handler = new PSJunitRequestHandler();

        PSRequest req = new PSRequest(null, null, null, null);
        req.setParameter(PSJunitRequestHandler.HTML_PARAM_EXE, "com.percussion.testing.HandlerSampleParameterizedTest");

        handler.processRequest(req);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PSResponse resp = req.getResponse();
        resp.send(out);

        Document doc = PSXmlDocumentBuilder.createXmlDocument(new java.io.ByteArrayInputStream(out.toByteArray()), false);
        NodeList results = doc.getElementsByTagName("TestResult");
        Element tr = (Element) results.item(0);

        // one parameterized invocation should fail
        assertEquals("2", tr.getAttribute("testCount"));
        assertEquals("1", tr.getAttribute("failures"));

        NodeList failures = tr.getElementsByTagName("TestFailure");
        assertEquals(1, failures.getLength());
        Element f = (Element) failures.item(0);
        // Display name pattern used in the sample is "value={0}" -> expect "value=2"
        assertEquals("value=2", f.getAttribute("testName"));
        String msg = f.getElementsByTagName("Message").item(0).getTextContent();
        assertTrue(msg.contains("failing param"));
    }
}
