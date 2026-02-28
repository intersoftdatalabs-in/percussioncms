package org.apache.soap.transport.http;

import org.apache.soap.Envelope;
import org.apache.soap.Header;
import org.apache.soap.Body;
import org.apache.soap.rpc.SOAPContext;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Hashtable;
import java.util.Vector;
import org.w3c.dom.Element;
import com.percussion.security.xml.PSSecureXMLUtils;
import com.percussion.xml.PSXmlDocumentBuilder;
import org.w3c.dom.Document;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.Transformer;
import java.io.StringWriter;

/**
 * Lightweight HTTP transport shim providing the narrow API used by PSWebServiceAgent.
 * It performs a basic POST of a SOAP 1.1 envelope built from the supplied Envelope.
 */
public class SOAPHTTPConnection
{
    private BufferedReader lastResponseReader;

    public SOAPHTTPConnection()
    {
    }

    public void send(URL target, String soapAction, Hashtable headers, Envelope env, Object unused, SOAPContext ctx) throws org.apache.soap.SOAPException
    {
        try {
            HttpURLConnection conn = (HttpURLConnection) target.openConnection();
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "text/xml; charset=utf-8");
            if (soapAction != null)
                conn.setRequestProperty("SOAPAction", soapAction);
            if (headers != null) {
                for (Object kobj : headers.keySet()) {
                    String key = kobj.toString();
                    Object val = headers.get(kobj);
                    conn.setRequestProperty(key, val == null ? "" : val.toString());
                }
            }

            String payload = buildEnvelopePayload(env);

            try (OutputStreamWriter out = new OutputStreamWriter(conn.getOutputStream(), "UTF-8")) {
                out.write(payload);
                out.flush();
            }

            this.lastResponseReader = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
        } catch (Exception e) {
            throw new org.apache.soap.SOAPException(e.getMessage(), e);
        }
    }

    public BufferedReader receive() throws Exception
    {
        return this.lastResponseReader;
    }

    public SOAPContext getResponseSOAPContext()
    {
        return new SOAPContext();
    }

    private String buildEnvelopePayload(Envelope env) throws Exception
    {
        Document doc = PSXmlDocumentBuilder.createXmlDocument();
        Element envEl = doc.createElementNS("http://schemas.xmlsoap.org/soap/envelope/", "SOAP-ENV:Envelope");
        doc.appendChild(envEl);

        // Header
        Header h = env.getHeader();
        Element headerEl = doc.createElementNS("http://schemas.xmlsoap.org/soap/envelope/", "SOAP-ENV:Header");
        if (h != null && h.getHeaderEntries() != null) {
            for (Element e : h.getHeaderEntries()) {
                Element imported = (Element) doc.importNode(e, true);
                headerEl.appendChild(imported);
            }
        }
        envEl.appendChild(headerEl);

        // Body
        Body b = env.getBody();
        Element bodyEl = doc.createElementNS("http://schemas.xmlsoap.org/soap/envelope/", "SOAP-ENV:Body");
        if (b != null && b.getBodyEntries() != null) {
            for (Element e : b.getBodyEntries()) {
                Element imported = (Element) doc.importNode(e, true);
                bodyEl.appendChild(imported);
            }
        }
        envEl.appendChild(bodyEl);

        // Transform to string
        TransformerFactory tf = PSSecureXMLUtils.getSecuredTransformerFactory();
        Transformer transformer = tf.newTransformer();
        StringWriter writer = new StringWriter();
        transformer.transform(new DOMSource(doc), new StreamResult(writer));
        return writer.toString();
    }
}
