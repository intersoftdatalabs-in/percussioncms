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
 * Java 11 Modernized: SOAP 1.2 client for EMS DEA Service.
 * <p>
 * // REFACTORED: CP-JAVA11
 * // REFACTORED: CP-SOAP-CLIENT
 * <p>
 * Sunny Sal says: "SOAP client—because every hero needs a sidekick! May the CXF stubs be with you."
 */
package service.web.api.ems.dea;

import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import java.net.URL;
import java.util.Optional;

/**
 * EMS DEA SOAP 1.2 Client using CXF-generated stubs.
 * <p>
 * Example usage:
 * <pre>
 * var client = ServiceSoap_ServiceSoap12_Client.create("http://localhost:8080/emsdea?wsdl");
 * var response = client.validateBilling(request);
 * response.ifPresent(r -> System.out.println(r.getValidateBillingResult()));
 * </pre>
 */
public class ServiceSoap_ServiceSoap12_Client {

    private final ServiceSoap_ServiceSoap12 soapPort;

    private ServiceSoap_ServiceSoap12_Client(ServiceSoap_ServiceSoap12 soapPort) {
        this.soapPort = soapPort;
    }

    /**
     * Static factory method to create a client instance.
     *
     * @param wsdlUrl WSDL endpoint URL
     * @return ServiceSoap_ServiceSoap12_Client instance
     */
    public static ServiceSoap_ServiceSoap12_Client create(String wsdlUrl) {
        try {
            var url = new URL(wsdlUrl);
            var qname = new QName("http://DEA.EMS.API.Web.Service/", "ServiceSoap_ServiceSoap12");
            var service = Service.create(url, qname);
            var port = service.getPort(ServiceSoap_ServiceSoap12.class);
            return new ServiceSoap_ServiceSoap12_Client(port);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to create SOAP client: " + ex.getMessage(), ex);
        }
    }

    /**
     * Calls the ValidateBilling SOAP operation.
     *
     * @param request ValidateBilling request payload
     * @return Optional of ValidateBillingResponse
     */
    public Optional<ValidateBillingResponse> validateBilling(ValidateBilling request) {
        try {
            var response = soapPort.validateBilling(request);
            return Optional.ofNullable(response);
        } catch (Exception ex) {
            // Sunny Sal says: "Network error! Picture abhi baaki hai mere dost."
            return Optional.empty();
        }
    }

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
     * Java 11 Modernized: SOAP 1.2 client for EMS DEA Service.
     * <p>
     * // REFACTORED: CP-JAVA11
     * // REFACTORED: CP-SOAP-CLIENT
     * <p>
     * Sunny Sal says: "SOAP client—because every hero needs a sidekick! May the CXF stubs be with you."
     */
    package service.web.api.ems.dea;

    import javax.xml.namespace.QName;
    import javax.xml.ws.Service;
    import java.net.URL;
    import java.util.Optional;

    /**
     * EMS DEA SOAP 1.2 Client using CXF-generated stubs.
     * <p>
     * Example usage:
     * <pre>
     * var client = ServiceSoap_ServiceSoap12_Client.create("http://localhost:8080/emsdea?wsdl");
     * var response = client.validateBilling(request);
     * response.ifPresent(r -> System.out.println(r.getValidateBillingResult()));
     * </pre>
     */
    public class ServiceSoap_ServiceSoap12_Client {

        private final ServiceSoap_ServiceSoap12 soapPort;

        private ServiceSoap_ServiceSoap12_Client(ServiceSoap_ServiceSoap12 soapPort) {
            this.soapPort = soapPort;
        }

        /**
         * Static factory method to create a client instance.
         *
         * @param wsdlUrl WSDL endpoint URL
         * @return ServiceSoap_ServiceSoap12_Client instance
         */
        public static ServiceSoap_ServiceSoap12_Client create(String wsdlUrl) {
            try {
                var url = new URL(wsdlUrl);
                var qname = new QName("http://DEA.EMS.API.Web.Service/", "ServiceSoap_ServiceSoap12");
                var service = Service.create(url, qname);
                var port = service.getPort(ServiceSoap_ServiceSoap12.class);
                return new ServiceSoap_ServiceSoap12_Client(port);
            } catch (Exception ex) {
                throw new IllegalStateException("Failed to create SOAP client: " + ex.getMessage(), ex);
            }
        }

        /**
         * Calls the ValidateBilling SOAP operation.
         *
         * @param request ValidateBilling request payload
         * @return Optional of ValidateBillingResponse
         */
        public Optional<ValidateBillingResponse> validateBilling(ValidateBilling request) {
            try {
                var response = soapPort.validateBilling(request);
                return Optional.ofNullable(response);
            } catch (Exception ex) {
                // Sunny Sal says: "Network error! Picture abhi baaki hai mere dost."
                return Optional.empty();
            }
        }

        // ...existing code for other SOAP operations...
    }
