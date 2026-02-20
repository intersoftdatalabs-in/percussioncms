package com.percussion.webservices.faults;

public class PSNotAuthenticatedFault extends Exception {
  private static final long serialVersionUID = 1L;

  public PSNotAuthenticatedFault() {
    super();
  }

  public PSNotAuthenticatedFault(String message) {
    super(message);
  }

  public PSNotAuthenticatedFault(String message, Throwable cause) {
    super(message, cause);
  }

  public PSNotAuthenticatedFault(Throwable cause) {
    super(cause);
  }
}
