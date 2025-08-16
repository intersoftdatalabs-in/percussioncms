/*
 * Copyright 1999-2025 Percussion Software, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.percussion.content;

import java.util.ArrayList;
import java.util.HashMap;
import org.w3c.dom.DOMException;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

// REFACTORED: CP-JAVA11
public class HTMLNamedNodeMap extends HashMap<String, Node> implements NamedNodeMap {
  private final ArrayList<String> m_keys;

  public HTMLNamedNodeMap() {
    super();
    m_keys = new ArrayList<>();
  }

  @Override
  public Node getNamedItem(String name) {
    return get(name);
  }

  @Override
  public Node setNamedItem(Node arg) throws DOMException {
    m_keys.add(arg.getNodeName());
    return put(arg.getNodeName(), arg);
  }

  @Override
  public Node removeNamedItem(String name) {
    m_keys.remove(name);
    Node ret = remove(name);
    if (ret == null) {
      throw new HTMLException(DOMException.NOT_FOUND_ERR, "Node not found");
    }
    return ret;
  }

  @Override
  public Node item(int index) {
    String key = m_keys.get(index);
    return get(key);
  }

  public int getLength() {
    return size();
  }

  /** Method introduced later in DOM level 2. Not implemented. */
  public Node getNamedItemNS(String namespaceURI, String localName) {
    // TODO: implement
    throw new RuntimeException("Method getNamedItemNS not supported");
  }

  /** Method introduced later in DOM level 2. Not implemented. */
  public Node setNamedItemNS(Node arg) throws DOMException {
    // TODO: implement
    throw new RuntimeException("Method setNamedItemNS not supported");
  }

  /** Method introduced later in DOM level 2. Not implemented. */
  public Node removeNamedItemNS(String namespaceURI, String localName) throws DOMException {
    // TODO: implement
    throw new RuntimeException("Method removeNamedItemNS not supported");
  }

  // m_keys is already defined above as ArrayList<String>
}
