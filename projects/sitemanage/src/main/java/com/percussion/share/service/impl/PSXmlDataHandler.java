// REFACTORED: CP-JAVA11
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
package com.percussion.share.service.impl;

import static org.apache.commons.lang3.Validate.notNull;

import com.percussion.share.service.impl.jaxb.Data;
import com.percussion.share.service.impl.jaxb.Response;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import jakarta.xml.bind.JAXBContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * This handler uses JAXB to load data from a specified XML file.
 *
 * @author peterfrontiero
 */
public class PSXmlDataHandler {

  private static final Logger log = LogManager.getLogger(PSXmlDataHandler.class);

  /** The path to the XML data file. Initialized in constructor, never null after that. */
  private String file;

  /**
   * Gets the response data associated with the request which matches the specified properties.
   *
   * @param properties request properties, must not be null.
   * @return Response containing result data or null if a matching request could not be found or an
   *     error occurs.
   */
  public Response getData(Map<String, Object> properties) {
    notNull(properties, "properties must not be null");
    try (InputStream is = new FileInputStream(new File(file))) {
      var jc = JAXBContext.newInstance("com.percussion.share.service.impl.jaxb");
      var unmarshaller = jc.createUnmarshaller();
      var data = (Data) unmarshaller.unmarshal(is);
      for (var request : data.getRequest()) {
        var reqProps = new HashMap<String, Object>();
        var settings = request.getSettings();
        for (var prop : settings.getProperty()) {
          Object val;
          var pvalues = prop.getPvalues();
          if (pvalues != null) {
            val = pvalues.getPvalue();
          } else {
            val = prop.getValue();
          }
          reqProps.put(prop.getName(), val);
        }
        if (reqProps.equals(properties)) {
          return request.getResponse();
        }
      }
    } catch (Exception e) {
      log.error("Error occurred getting response data: ", e);
    }
    return null;
  }

  /**
   * @return the file
   */
  public String getFile() {
    return file;
  }

  /**
   * @param file the file to set
   */
  public void setFile(String file) {
    this.file = file;
  }
}
