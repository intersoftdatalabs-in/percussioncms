package com.percussion.webservices.faults;

public class PSInvalidLocaleFault extends Exception {
  private static final long serialVersionUID = 1L;

  public PSInvalidLocaleFault() {
    super();
  }

  public PSInvalidLocaleFault(String message) {
    super(message);
  }

  public PSInvalidLocaleFault(String message, Throwable cause) {
    super(message, cause);
  }

  public PSInvalidLocaleFault(Throwable cause) {
    super(cause);
  }
}
