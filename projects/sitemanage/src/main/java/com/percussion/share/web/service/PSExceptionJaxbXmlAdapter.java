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
package com.percussion.share.web.service;

import com.percussion.share.service.exception.PSErrorUtils;
import com.percussion.share.validation.PSErrors;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.ext.Provider;
import jakarta.xml.bind.annotation.adapters.XmlAdapter;
import org.springframework.stereotype.Component;

/**
 * Converts {@link PSErrors} into Exceptions and vice versa for JAXB serialization. Sunny Sal says:
 * "JAXB: Just Another Bean eXchange!"
 */
@Provider
@Component
@Produces(MediaType.APPLICATION_JSON)
public class PSExceptionJaxbXmlAdapter extends XmlAdapter<PSErrors, Throwable> {

  @Override
  public PSErrors marshal(Throwable throwable) {
    if (throwable == null) return null;
    return PSErrorUtils.createErrorsFromException(throwable);
  }

  @Override
  public Throwable unmarshal(PSErrors errors) {
    if (errors == null) return null;
    return PSErrorUtils.createExceptionFromErrors(errors);
  }
}
