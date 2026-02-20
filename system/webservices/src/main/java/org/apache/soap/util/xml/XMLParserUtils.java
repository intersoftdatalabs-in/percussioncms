package org.apache.soap.util.xml;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

/** Minimal shim for org.apache.soap.util.xml.XMLParserUtils used by tests. */
public final class XMLParserUtils
{
   private XMLParserUtils() {}

   public static DocumentBuilder getXMLDocBuilder() throws Exception
   {
      DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
      dbf.setNamespaceAware(true);
      return dbf.newDocumentBuilder();
   }
}
