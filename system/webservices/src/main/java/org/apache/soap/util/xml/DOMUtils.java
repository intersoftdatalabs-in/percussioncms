package org.apache.soap.util.xml;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * Minimal DOM utility helper used by legacy code.
 */
public final class DOMUtils {
    private DOMUtils() {}

    public static Element getFirstChildElement(Element parent) {
        if (parent == null) return null;
        Node n = parent.getFirstChild();
        while (n != null) {
            if (n instanceof Element) return (Element) n;
            n = n.getNextSibling();
        }
        return null;
    }
}
