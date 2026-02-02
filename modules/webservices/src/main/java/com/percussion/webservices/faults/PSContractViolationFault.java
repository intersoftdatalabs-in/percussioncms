package com.percussion.webservices.faults;

public class PSContractViolationFault extends Exception {
  private static final long serialVersionUID = 1L;

  private int code;
  private String detail;

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

  // Compatibility constructor used by legacy call sites (code, message, detail)
  public PSContractViolationFault(int code, String message, String detail) {
    super(message + (detail != null ? (" - " + detail) : ""));
    this.code = code;
    this.detail = detail;
  }

  public int getCode() {
    return code;
  }

  public String getDetail() {
    return detail;
  }
}
