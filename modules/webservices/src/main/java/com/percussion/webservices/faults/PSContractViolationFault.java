package com.percussion.webservices.faults;

public class PSContractViolationFault extends Exception {
  private static final long serialVersionUID = 1L;

  public PSContractViolationFault() {
    super();
  }

  public PSContractViolationFault(String message) {
    super(message);
  }

  public PSContractViolationFault(String message, Throwable cause) {
    super(message, cause);
  }

  public PSContractViolationFault(Throwable cause) {
    super(cause);
  }
}
