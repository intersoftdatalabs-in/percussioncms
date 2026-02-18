package org.apache.soap.rpc;

import org.apache.soap.Envelope;
import org.apache.soap.encoding.SOAPMappingRegistry;

/** Minimal stub for org.apache.soap.rpc.Response used by legacy test helpers. */
public class Response {

    private org.apache.soap.Fault fault;

    /**
     * Minimal extractor that mirrors the historical API used by tests.
     * This implementation does not perform real SOAP parsing; it returns an
     * empty Response so unit tests that only inspect the presence/absence of a
     * Fault can compile and run.
     */
    public static Response extractFromEnvelope(Envelope env,
                                               SOAPMappingRegistry registry,
                                               SOAPContext ctx)
    {
        return new Response();
    }

    /** Return the fault (may be null in test stubs). */
    public org.apache.soap.Fault getFault()
    {
        return fault;
    }
}

