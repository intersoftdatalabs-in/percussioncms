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
package com.percussion.pso.restservice.utils;

import com.percussion.pso.restservice.model.Item;
import com.percussion.pso.restservice.model.Items;
import com.percussion.security.error.PSExceptionUtils;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.StringWriter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.io.DocumentResult;

/** */
public class ItemServiceHelper {

  private static final Logger log = LogManager.getLogger(ItemServiceHelper.class);

  /**
   * Method getItemXml.
   *
   * @param item Item
   * @return String
   */
  public static String getItemXml(Item item) {
    StringWriter sw = new StringWriter();
    try {

      JAXBContext jc = JAXBContext.newInstance(Item.class);
      Marshaller m = jc.createMarshaller();
      m.setProperty("jaxb.fragment", Boolean.TRUE);
      m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
      m.marshal(item, sw);
    } catch (JAXBException e) {
      log.error(PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
    }
    sw.flush();
    return sw.toString();
  }

  /**
   * Method getItemFromXml.
   *
   * @param is InputStream
   * @return Item
   * @throws JAXBException
   */
  public static Item getItemFromXml(InputStream is) throws JAXBException {
    JAXBContext jc = JAXBContext.newInstance(Item.class);
    Unmarshaller um = jc.createUnmarshaller();
    Item item = (Item) um.unmarshal(is);
    return item;
  }

  /**
   * Method getItemsFromXml.
   *
   * @param is InputStream
   * @return Items
   * @throws JAXBException
   */
  public static Items getItemsFromXml(InputStream is) throws JAXBException {
    JAXBContext jc = JAXBContext.newInstance(Items.class);
    Unmarshaller um = jc.createUnmarshaller();
    Items items = (Items) um.unmarshal(is);
    return items;
  }

  /**
   * Method getItemFromXml.
   *
   * @param string String
   * @return Item
   * @throws JAXBException
   */
  public static Item getItemFromXml(String string) throws JAXBException {
    ByteArrayInputStream input = new ByteArrayInputStream(string.getBytes());
    return getItemFromXml(input);
  }

  /**
   * Method getItemsFromXml.
   *
   * @param string String
   * @return Items
   * @throws JAXBException
   */
  public static Items getItemsFromXml(String string) throws JAXBException {
    ByteArrayInputStream input = new ByteArrayInputStream(string.getBytes());
    return getItemsFromXml(input);
  }

  /**
   * Method getItemDOM.
   *
   * @param item Item
   * @return Document
   */
  public static Document getItemDOM(Item item) {
    DocumentResult dr = new DocumentResult();
    try {

      JAXBContext jc = JAXBContext.newInstance(Item.class);
      Marshaller m = jc.createMarshaller();
      m.setProperty("jaxb.fragment", Boolean.TRUE);
      m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
      m.marshal(item, dr);
    } catch (JAXBException e) {
      log.error(PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
    }

    return dr.getDocument();
  }
}
