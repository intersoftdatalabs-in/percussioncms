/*
 * Minimal stub to satisfy compile until webservice-generated classes are restored.
 */
package com.percussion.cms.objectstore.ws;

import com.percussion.cms.objectstore.PSItemDefinition;
import com.percussion.cms.objectstore.PSItemField;

public class PSClientItem {

  private PSItemDefinition def;

  public PSClientItem() {}

  public PSClientItem(PSItemDefinition def) {
    this.def = def;
  }

  public java.util.Iterator<String> getAllFieldNames() {
    return java.util.Collections.emptyIterator();
  }

  public PSItemDefinition getItemDefinition() {
    return null;
  }

  public PSItemField getFieldByName(String name) {
    return null;
  }

  public java.util.Iterator<PSItemField> getAllFields() {
    return java.util.Collections.emptyIterator();
  }

  public void loadXmlData(org.w3c.dom.Element el) {
    // no-op stub
  }

  public int getContentId() {
    return -1;
  }

  public int getRevision() {
    return -1;
  }

  public int getContentTypeId() {
    return -1;
  }
}
