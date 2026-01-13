package com.percussion.webservices.faults;

public class PSErrorResultsFaultServiceCallError {
  private long id;
  private PSError pSError;
  private PSLockFaultBean psLockFault;

  public long getId() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
  }

  public PSError getPSError() {
    return pSError;
  }

  public void setPSError(PSError pSError) {
    this.pSError = pSError;
  }

  public PSLockFaultBean getPSLockFault() {
    return psLockFault;
  }

  public void setPSLockFault(PSLockFaultBean psLockFault) {
    this.psLockFault = psLockFault;
  }
}
