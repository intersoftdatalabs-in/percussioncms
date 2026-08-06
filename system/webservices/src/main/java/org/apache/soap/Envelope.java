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
 */
package org.apache.soap;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.Document;
import java.util.Vector;
import org.apache.soap.rpc.SOAPContext;
import org.apache.soap.util.xml.DOM2Writer;

/**
 * Minimal compatibility shim to build and parse SOAP Envelopes used by PSWebServiceAgent.
 * This is intentionally small and only implements the methods that are used by the project.
 */
public class Envelope
{
    private Header header;
    private Body body;

    public Envelope()
    {
    }

    public void setHeader(Header h)
    {
        this.header = h;
    }

    public Header getHeader()
    {
        return this.header;
    }

    public void setBody(Body b)
    {
        this.body = b;
    }

    public Body getBody()
    {
        return this.body;
    }

    /**
     * Unmarshall an Envelope from a DOM Element (SOAP Envelope).
     */
    public static Envelope unmarshall(Element envEl, SOAPContext ctx)
    {
        Envelope env = new Envelope();

        NodeList headerNodes = envEl.getElementsByTagNameNS("http://schemas.xmlsoap.org/soap/envelope/", "Header");
        if (headerNodes != null && headerNodes.getLength() > 0)
        {
            Element headerEl = (Element) headerNodes.item(0);
            Header h = new Header();
            Vector<Element> headerEntries = new Vector<>();
            NodeList children = headerEl.getChildNodes();
            for (int i = 0; i < children.getLength(); i++)
            {
                if (children.item(i) instanceof Element)
                    headerEntries.add((Element) children.item(i));
            }
            h.setHeaderEntries(headerEntries);
            env.setHeader(h);
        }

        NodeList bodyNodes = envEl.getElementsByTagNameNS("http://schemas.xmlsoap.org/soap/envelope/", "Body");
        if (bodyNodes != null && bodyNodes.getLength() > 0)
        {
            Element bodyEl = (Element) bodyNodes.item(0);
            Body b = new Body();
            Vector<Element> bodyEntries = new Vector<>();
            NodeList children = bodyEl.getChildNodes();
            for (int i = 0; i < children.getLength(); i++)
            {
                if (children.item(i) instanceof Element)
                    bodyEntries.add((Element) children.item(i));
            }
            b.setBodyEntries(bodyEntries);
            env.setBody(b);
            // populate the SOAPContext body parts for clients that use it
            if (ctx != null) {
                Element[] parts = new Element[bodyEntries.size()];
                for (int i = 0; i < bodyEntries.size(); i++) parts[i] = bodyEntries.get(i);
                ctx.setBodyParts(parts);
            }
        }

        return env;
    }

    /**
     * Marshall the envelope into a writer. This implementation writes a minimal
     * SOAP envelope containing the body entries.
     */
    public void marshall(java.io.StringWriter sw, Object smr, SOAPContext ctx) throws SOAPException
    {
        try {
            sw.write("<env:Envelope xmlns:env=\"http://schemas.xmlsoap.org/soap/envelope/\">\n");
            if (header != null && header.getHeaderEntries() != null && !header.getHeaderEntries().isEmpty()) {
                sw.write("  <env:Header>\n");
                for (org.w3c.dom.Element el : header.getHeaderEntries()) {
                    sw.write(DOM2Writer.nodeToString(el));
                }
                sw.write("  </env:Header>\n");
            }
            sw.write("  <env:Body>\n");
            if (body != null && body.getBodyEntries() != null) {
                for (org.w3c.dom.Element el : body.getBodyEntries()) {
                    sw.write(DOM2Writer.nodeToString(el));
                }
            }
            sw.write("  </env:Body>\n");
            sw.write("</env:Envelope>");
        } catch (Exception e) {
            throw new SOAPException(e.getMessage(), e);
        }
    }
}

