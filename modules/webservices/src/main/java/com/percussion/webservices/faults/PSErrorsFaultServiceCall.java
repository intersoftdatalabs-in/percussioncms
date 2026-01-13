package com.percussion.webservices.faults;

public class PSErrorsFaultServiceCall {
  private PSErrorsFaultServiceCallSuccess success;
  private PSErrorsFaultServiceCallError error;
  private long id;

  public PSErrorsFaultServiceCallSuccess getSuccess() {
    return success;
  }

  public void setSuccess(PSErrorsFaultServiceCallSuccess s) {
    this.success = s;
  }

  public PSErrorsFaultServiceCallError getError() {
    return error;
  }

  public void setError(PSErrorsFaultServiceCallError e) {
    this.error = e;
  }

  public long getId() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
  }
}
