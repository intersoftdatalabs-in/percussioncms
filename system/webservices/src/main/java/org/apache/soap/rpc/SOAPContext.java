package org.apache.soap.rpc;

import java.util.HashMap;
import java.util.Map;
import org.w3c.dom.Element;

/** Minimal shim for org.apache.soap.rpc.SOAPContext used by PSWebServices. */
public class SOAPContext {
    private final Map<String, Object> props = new HashMap<>();
    private String rootPart;
    private String rootContentType;
    private Element[] bodyParts = new Element[0];

    public Object getProperty(String name) {
        return props.get(name);
    }

    public void setProperty(String name, Object value) {
        props.put(name, value);
    }

    public void setRootPart(String part, String contentType) {
        this.rootPart = part;
        this.rootContentType = contentType;
    }

    public String getRootPart() {
        return rootPart;
    }

    public int getCount() {
        return bodyParts.length;
    }

    public void setBodyParts(Element[] parts) {
        this.bodyParts = parts == null ? new Element[0] : parts;
    }

    public Element getBodyPart(int idx) {
        if (idx < 0 || idx >= bodyParts.length) return null;
        return bodyParts[idx];
    }
}
