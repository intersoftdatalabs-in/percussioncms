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

package com.percussion.share.validation;

import java.io.StringWriter;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import org.junit.jupiter.api.Test;

/** Unit test for {@link PSValidationErrors}. */
public class PSValidationErrorsTest {

  @Test
  void testSerialization() throws JAXBException {
    var errors = new PSValidationErrors();

    // Get a JAXB Context for the object we created above
    var context = JAXBContext.newInstance(errors.getClass());

    // To convert errors to XML, create a JAXB Marshaller
    var marshaller = context.createMarshaller();

    // Make the output pretty
    marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
    var sw = new StringWriter();

    // Marshall the object to XML
    marshaller.marshal(errors, sw);

    // Print it out for this example
    System.out.println(sw.toString());
  }
}
