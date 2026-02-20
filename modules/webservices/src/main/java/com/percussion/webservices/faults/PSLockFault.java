package com.percussion.webservices.faults;

public class PSLockFault extends Exception {
  private static final long serialVersionUID = 1L;

  public PSLockFault() {
    super();
  }

  public PSLockFault(String message) {
    super(message);
  }

  public PSLockFault(String message, Throwable cause) {
    super(message, cause);
  }

  public PSLockFault(Throwable cause) {
    super(cause);
  }
}
