package org.apache.soap;

/** Minimal compatibility stub for org.apache.soap.SOAPException */
public class SOAPException extends Exception {
    public SOAPException() { super(); }
    public SOAPException(String msg) { super(msg); }
    public SOAPException(String msg, Throwable t) { super(msg, t); }
}
