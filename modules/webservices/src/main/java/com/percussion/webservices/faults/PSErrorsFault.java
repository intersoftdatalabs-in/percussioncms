package com.percussion.webservices.faults;

public class PSErrorsFault extends Exception {
  private static final long serialVersionUID = 1L;

  private String service;

  public PSErrorsFault() {
    super();
  }

  public PSErrorsFault(String message) {
    super(message);
  }

  public PSErrorsFault(String message, Throwable cause) {
    super(message, cause);
  }

  public PSErrorsFault(Throwable cause) {
    super(cause);
  }

  public void setService(String service) {
    this.service = service;
  }

  public String getService() {
    return service;
  }
}
