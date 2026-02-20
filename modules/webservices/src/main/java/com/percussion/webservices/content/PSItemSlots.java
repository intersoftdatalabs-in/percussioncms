package com.percussion.webservices.content;

import java.util.ArrayList;
import java.util.List;

public class PSItemSlots {
  private String name;
  private List<PSRelatedItem> psRelatedItem = new ArrayList<>();

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public List<PSRelatedItem> getPSRelatedItem() {
    return psRelatedItem;
  }

  public void setPSRelatedItem(List<PSRelatedItem> related) {
    this.psRelatedItem = related;
  }
}
