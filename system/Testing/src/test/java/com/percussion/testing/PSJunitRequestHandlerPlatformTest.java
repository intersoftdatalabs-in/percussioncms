package com.percussion.testing;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.percussion.server.PSRequest;
import com.percussion.server.PSResponse;
import com.percussion.xml.PSXmlDocumentBuilder;

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
}
