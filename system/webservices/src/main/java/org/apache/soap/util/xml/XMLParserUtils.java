package org.apache.soap.util.xml;

import com.percussion.security.xml.PSSecureXMLUtils;
import com.percussion.security.xml.PSXmlSecurityOptions;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

/**
 * Minimal shim for org.apache.soap.util.xml.XMLParserUtils used by tests.
 *
 * <p>Uses {@link PSSecureXMLUtils} to guard against XXE
 * (XML External Entity) attacks per OWASP guidelines.
 */
public final class XMLParserUtils
{
   private XMLParserUtils() {}

   public static DocumentBuilder getXMLDocBuilder() throws Exception
   {
      DocumentBuilderFactory dbf =
          PSSecureXMLUtils.getSecuredDocumentBuilderFactory(
              new PSXmlSecurityOptions(
                  false,  // enableExternalEntities
                  true,   // enableDtdDeclarations
                  false,  // enableExternalDtdReferences
                  true,   // enableSecureProcessing
                  false,  // enableExternalParameterEntities
                  false   // enableValidation
              ));
      dbf.setNamespaceAware(true);
      return dbf.newDocumentBuilder();
   }
}
