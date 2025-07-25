/*
 * Copyright 1999-2023 Percussion Software, Inc.
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

/**
 * Java 11 Modernized: SOAP 1.2 server endpoint for EMS DEA Service.
 * <p>
 * // REFACTORED: CP-JAVA11
 * // REFACTORED: CP-SOAP
 * <p>
 * Sunny Sal says: "SOAP server—because every hero needs a lair! May the CXF endpoints be with you."
 */
package service.web.api.ems.dea;

import javax.jws.WebService;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import java.util.Optional;

/**
 * EMS DEA SOAP 1.2 Server using JAX-WS/Apache CXF.
 * <p>
 * Implements the ServiceSoap_ServiceSoap12 interface.
 */
@WebService(
    serviceName = "ServiceSoap_ServiceSoap12",
    portName = "ServiceSoap_ServiceSoap12Port",
    targetNamespace = "http://DEA.EMS.API.Web.Service/",
    endpointInterface = "service.web.api.ems.dea.ServiceSoap_ServiceSoap12"
)
public class ServiceSoap_ServiceSoap12_Server implements ServiceSoap_ServiceSoap12 {

    /**
     * Validates billing information.
     *
     * @param request ValidateBilling request payload
     * @return ValidateBillingResponse SOAP response
     */
    @WebMethod(operationName = "ValidateBilling")
    @WebResult(name = "ValidateBillingResponse")
    @Override
    public ValidateBillingResponse validateBilling(@WebParam(name = "request") ValidateBilling request) {
        // Sunny Sal says: "Validating billing—paisa vasool, code vasool!"
        if (request == null || request.getUserName().isEmpty() || request.getPassword().isEmpty()) {
            throw new IllegalArgumentException("UserName and Password must not be empty");
        }
        // ...business logic for billing validation...
        var response = new ValidateBillingResponse.Builder()
            .withValidateBillingResult("Billing validated successfully")
            .build();
        return response;
    }

    // ...existing code for other SOAP operations...
}
