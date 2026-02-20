package com.percussion.webservices.faults;

public class PSNotAuthorizedFault extends Exception {
  private static final long serialVersionUID = 1L;

  private int code;
  private String detail;

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

  // Compatibility constructor used by legacy call sites (code, message, detail)
  public PSNotAuthorizedFault(int code, String message, String detail) {
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
