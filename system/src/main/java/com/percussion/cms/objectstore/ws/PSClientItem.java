/*
 * Minimal stub to satisfy compile until webservice-generated classes are restored.
 */
package com.percussion.cms.objectstore.ws;

import com.percussion.cms.objectstore.PSItemDefinition;
import com.percussion.cms.objectstore.PSItemField;
import java.util.List;

public class PSClientItem {

  public List<String> getAllFieldNames() {
    return java.util.Collections.emptyList();
  }

  public PSItemDefinition getItemDefinition() {
    return null;
  }

  public PSItemField getFieldByName(String name) {
    return null;
  }
}
