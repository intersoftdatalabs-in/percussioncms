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

// REFACTORED: CP-JAVA11

package com.percussion.category.extension;

import static com.percussion.share.spring.PSSpringWebApplicationContextUtils.getWebApplicationContext;

import com.percussion.category.data.PSCategory;
import com.percussion.category.data.PSCategoryNode;
import com.percussion.category.service.IPSCategoryService;
import com.percussion.security.error.PSExceptionUtils;
import com.percussion.share.service.exception.PSDataServiceException;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

public class PSCategoryControlUtils {

  private static volatile IPSCategoryService categoryService = null;
  public static final Logger log = LogManager.getLogger(PSCategoryControlUtils.class);

  public static PSCategory getCategories(
      String siteName, String rootPath, boolean includeDeleted, boolean includeNotSelectable)
      throws PSDataServiceException {
    if (categoryService == null) {
      categoryService = (IPSCategoryService) getWebApplicationContext().getBean("categoryService");
    }
    return categoryService.getCategoryTreeForSite(
        siteName, rootPath, includeDeleted, includeNotSelectable);
  }

  public static PSCategoryNode findCategoryNode(
      String siteName, String rootPath, boolean includeDeleted, boolean includeNotSelectable) {
    if (categoryService == null) {
      categoryService = (IPSCategoryService) getWebApplicationContext().getBean("categoryService");
    }
    return categoryService.findCategoryNode(
        siteName, rootPath, includeDeleted, includeNotSelectable);
  }

  /**
   * Converts the category object to an XML string.
   *
   * @param category the category object
   * @return String containing category XML
   */
  public static String getCategoryXmlInString(PSCategory category) {
    try (var writer = new StringWriter()) {
      var jaxbContext = JAXBContext.newInstance(PSCategory.class);
      var jaxbMarshaller = jaxbContext.createMarshaller();
      jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
      jaxbMarshaller.marshal(category, writer);
      return writer.toString();
    } catch (JAXBException e) {
      log.error(
          "JAXB Exception occurred while marshalling category object to XML string -"
              + " PSCategoryControlUtils.getCategoryXmlInString() Error: {}",
          PSExceptionUtils.getMessageForLog(e));
    } catch (Exception e) {
      log.error(
          "Exception occurred while marshalling category object to XML string -"
              + " PSCategoryControlUtils.getCategoryXmlInString() Error: {}",
          PSExceptionUtils.getMessageForLog(e));
    }
    return "";
  }

  /**
   * Finds the parent node (set in the property of the control) in the categories.
   *
   * @param nodes List of nodes to look in
   * @param parentCategory parent category value set in the control property
   * @return The parent category node, or null if not found
   */
  private static PSCategoryNode findParentNode(List<PSCategoryNode> nodes, String parentCategory) {
    for (var node : nodes) {
      if (node.getTitle().equals(parentCategory)) {
        return node;
      } else if (node.getChildNodes() != null && !node.getChildNodes().isEmpty()) {
        var parentNode = findParentNode(node.getChildNodes(), parentCategory);
        if (parentNode != null) {
          return parentNode;
        }
      }
    }
    return null;
  }

  /**
   * Filters the nodes of the parent category based on the selectable property.
   *
   * @param parentNode the parent node
   * @return category node after filtering
   */
  private static PSCategoryNode filterNode(PSCategoryNode parentNode) {
    if (parentNode.getChildNodes() != null && !parentNode.getChildNodes().isEmpty()) {
      var childNodeList = new ArrayList<PSCategoryNode>();
      for (var node : parentNode.getChildNodes()) {
        if (node.isSelectable()) {
          childNodeList.add(node);
        }
      }
      parentNode.setChildNodes(childNodeList);
    }
    return parentNode;
  }

  /**
   * Converts the category XML file to a format that other controls (checkboxtree and pageautolist)
   * can understand and display.
   *
   * @param doc the input XML Document
   * @return the category XML Document in the old format
   */
  public static Document convertToOldFormatXml(Document doc) {
    var document = DocumentHelper.createDocument();
    var root = document.addElement("Tree");
    for (var attr : doc.getRootElement().attributes()) {
      if ("title".equalsIgnoreCase(attr.getName())) {
        root.addAttribute("label", attr.getStringValue());
      }
    }
    for (Iterator<Element> it = doc.getRootElement().elementIterator(); it.hasNext(); ) {
      var e = it.next();
      if ("Children".equalsIgnoreCase(e.getName())) {
        var newElement = root.addElement("Node");
        for (var attr : e.attributes()) {
          if ("id".equalsIgnoreCase(attr.getName()))
            newElement.addAttribute("id", attr.getStringValue());
          if ("title".equalsIgnoreCase(attr.getName()))
            newElement.addAttribute("label", attr.getStringValue());
          if ("selectable".equalsIgnoreCase(attr.getName())) {
            newElement.addAttribute(
                "selectable", attr.getStringValue().equalsIgnoreCase("true") ? "yes" : "no");
          }
        }
        if (e.elements() != null && !e.elements().isEmpty()) {
          for (Iterator<Element> eleIt = e.elementIterator(); eleIt.hasNext(); ) {
            createChildElement(eleIt.next(), newElement);
          }
        }
      }
    }
    return document;
  }

  /**
   * Supporting method to convert the category XML in the format that is understandable by
   * checkboxtree and pageautolist.
   *
   * @param source the source XML element
   * @param targetParent the target parent XML element
   */
  private static void createChildElement(Element source, Element targetParent) {
    var newChild = targetParent.addElement("Node");
    for (var attr : source.attributes()) {
      if ("id".equalsIgnoreCase(attr.getName())) newChild.addAttribute("id", attr.getStringValue());
      if ("title".equalsIgnoreCase(attr.getName()))
        newChild.addAttribute("label", attr.getStringValue());
      if ("selectable".equalsIgnoreCase(attr.getName())) {
        newChild.addAttribute(
            "selectable", attr.getStringValue().equalsIgnoreCase("true") ? "yes" : "no");
      }
    }
    if (source.elements() != null && !source.elements().isEmpty()) {
      for (Iterator<Element> eleIt = source.elementIterator(); eleIt.hasNext(); ) {
        createChildElement(eleIt.next(), newChild);
      }
    }
  }
}
