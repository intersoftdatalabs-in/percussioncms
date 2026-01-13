package com.percussion.webservices.faults;

public class PSErrorResultsFaultServiceCall {
  private PSErrorResultsFaultServiceCallResult result;
  private PSErrorResultsFaultServiceCallError error;

  public PSErrorResultsFaultServiceCallResult getResult() {
    return result;
  }

  public void setResult(PSErrorResultsFaultServiceCallResult result) {
    this.result = result;
  }

  public PSErrorResultsFaultServiceCallError getError() {
    return error;
  }

  public void setError(PSErrorResultsFaultServiceCallError error) {
    this.error = error;
  }
}
