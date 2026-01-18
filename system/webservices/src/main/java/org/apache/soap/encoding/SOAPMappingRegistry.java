package org.apache.soap.encoding;

/**
 * Minimal stub of org.apache.soap.encoding.SOAPMappingRegistry used by legacy code.
 */
public class SOAPMappingRegistry {
    public static SOAPMappingRegistry getBaseRegistry(String uri) {
        return new SOAPMappingRegistry();
    }
}
