package com.percussion.webservices.faults;

public class PSErrorResultsFault extends Exception {
  private static final long serialVersionUID = 1L;

  public PSErrorResultsFault() {
    super();
  }

  public PSErrorResultsFault(String message) {
    super(message);
  }

  public PSErrorResultsFault(String message, Throwable cause) {
    super(message, cause);
  }

  public PSErrorResultsFault(Throwable cause) {
    super(cause);
  }
}
