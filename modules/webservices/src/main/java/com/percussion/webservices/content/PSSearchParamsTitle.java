package com.percussion.webservices.content;

import com.percussion.webservices.common.ConnectorTypes;
import com.percussion.webservices.common.OperatorTypes;

public class PSSearchParamsTitle {
  private OperatorTypes operator;
  private ConnectorTypes connector;
  private String value;

  public OperatorTypes getOperator() {
    return operator;
  }

  public void setOperator(OperatorTypes operator) {
    this.operator = operator;
  }

  public ConnectorTypes getConnector() {
    return connector;
  }

  public void setConnector(ConnectorTypes connector) {
    this.connector = connector;
  }

  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    this.value = value;
  }
}
