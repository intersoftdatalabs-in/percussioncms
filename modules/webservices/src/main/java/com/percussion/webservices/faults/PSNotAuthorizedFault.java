package com.percussion.webservices.faults;

public class PSNotAuthorizedFault extends Exception {
  private static final long serialVersionUID = 1L;

  public PSNotAuthorizedFault() {
    super();
  }

  public PSNotAuthorizedFault(String message) {
    super(message);
  }

  public PSNotAuthorizedFault(String message, Throwable cause) {
    super(message, cause);
  }

  public PSNotAuthorizedFault(Throwable cause) {
    super(cause);
  }
}
