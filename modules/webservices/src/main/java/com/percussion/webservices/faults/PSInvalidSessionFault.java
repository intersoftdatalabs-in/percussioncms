package com.percussion.webservices.faults;

public class PSInvalidSessionFault extends Exception {
  private static final long serialVersionUID = 1L;

  public PSInvalidSessionFault() {
    super();
  }

  public PSInvalidSessionFault(String message) {
    super(message);
  }

  public PSInvalidSessionFault(String message, Throwable cause) {
    super(message, cause);
  }

  public PSInvalidSessionFault(Throwable cause) {
    super(cause);
  }
}
