package org.apache.soap.util.xml;

import org.w3c.dom.Node;

/**
 * Minimal DOM to string helper used by legacy code.
 */
public final class DOM2Writer {
    private DOM2Writer() {}

    public static String nodeToString(Node node) {
        if (node == null) return "";
        return node.toString();
    }
}
