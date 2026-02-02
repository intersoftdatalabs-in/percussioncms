package org.apache.soap;

/** Minimal compatibility stub for org.apache.soap.SOAPException */
public class SOAPException extends Exception {
    public SOAPException() { super(); }
    public SOAPException(String msg) { super(msg); }
    public SOAPException(String msg, Throwable t) { super(msg, t); }

    // Compatibility constructors used by older SOAP code paths that pass
    // a fault code (as String) followed by a human-readable message.
    public SOAPException(String faultCode, String msg) {
        super(faultCode + ": " + msg);
    }

    public SOAPException(String faultCode, String msg, Throwable t) {
        super(faultCode + ": " + msg, t);
    }
}
