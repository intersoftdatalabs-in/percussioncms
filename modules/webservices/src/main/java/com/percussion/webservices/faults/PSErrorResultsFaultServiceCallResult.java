package com.percussion.webservices.faults;

import com.percussion.webservices.assembly.data.PSAssemblyTemplate;
import com.percussion.webservices.content.PSAaRelationship;
import com.percussion.webservices.system.PSAclImpl;
import com.percussion.webservices.ui.data.PSAction;

public class PSErrorResultsFaultServiceCallResult {
  private long id;
  private PSAaRelationship psAaRelationship;
  private PSAclImpl psAclImpl;
  private PSAction psAction;
  private PSAssemblyTemplate psAssemblyTemplate;

  public long getId() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
  }

  public PSAaRelationship getPSAaRelationship() {
    return psAaRelationship;
  }

  public void setPSAaRelationship(PSAaRelationship v) {
    this.psAaRelationship = v;
  }

  public PSAclImpl getPSAclImpl() {
    return psAclImpl;
  }

  public void setPSAclImpl(PSAclImpl v) {
    this.psAclImpl = v;
  }

  public PSAction getPSAction() {
    return psAction;
  }

  public void setPSAction(PSAction v) {
    this.psAction = v;
  }

  public PSAssemblyTemplate getPSAssemblyTemplate() {
    return psAssemblyTemplate;
  }

  public void setPSAssemblyTemplate(PSAssemblyTemplate v) {
    this.psAssemblyTemplate = v;
  }
}
