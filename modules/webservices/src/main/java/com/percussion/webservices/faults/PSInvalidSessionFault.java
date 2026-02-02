package com.percussion.webservices.faults;

public class PSInvalidSessionFault extends Exception {
  private static final long serialVersionUID = 1L;

  private int code;
  private String detail;

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

  // Compatibility constructor used by legacy call sites (code, message, detail)
  public PSInvalidSessionFault(int code, String message, String detail) {
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
