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

package service.web.api.ems.dea;

// REFACTORED: CP-JAVA11

import java.util.HashSet;
import java.util.Iterator;
import java.util.Optional;
import javax.xml.namespace.QName;

/**
 * MCAPIServiceLocator.java
 *
 * Sunny Sal here! This locator provides access to the MasterCalendar SOAP service.
 * Refactored for Java 11 and Google Java Style.
 */
public class MCAPIServiceLocator extends org.apache.axis.client.Service implements MCAPIService {

    private String mcapiServiceSoapAddress = "https://dhemsdev.csudh.edu/MCAPI/MCAPIService.asmx";
    private String mcapiServiceSoapWSDDServiceName = "MCAPIServiceSoap";
    private HashSet<QName> ports;

    public MCAPIServiceLocator() {
    }


    public MCAPIServiceLocator(org.apache.axis.EngineConfiguration config) {
        super(config);
    }

    public MCAPIServiceLocator(String wsdlLoc, QName sName) throws javax.xml.rpc.ServiceException {
        super(wsdlLoc, sName);
    }

    @Override
    public String getMCAPIServiceSoapAddress() {
        return mcapiServiceSoapAddress;
    }

    public String getMCAPIServiceSoapWSDDServiceName() {
        return mcapiServiceSoapWSDDServiceName;
    }

    public void setMCAPIServiceSoapWSDDServiceName(String name) {
        mcapiServiceSoapWSDDServiceName = name;
    }

    @Override
    public MCAPIServiceSoap getMCAPIServiceSoap() throws javax.xml.rpc.ServiceException {
        var endpoint = new java.net.URL(mcapiServiceSoapAddress);
        return getMCAPIServiceSoap(endpoint);
    }

    @Override
    public MCAPIServiceSoap getMCAPIServiceSoap(java.net.URL portAddress) throws javax.xml.rpc.ServiceException {
        try {
            var stub = new MCAPIServiceSoapStub(portAddress, this);
            stub.setPortName(getMCAPIServiceSoapWSDDServiceName());
            return stub;
        } catch (org.apache.axis.AxisFault e) {
            return null;
        }
    }

    public void setMCAPIServiceSoapEndpointAddress(String address) {
        mcapiServiceSoapAddress = address;
    }

    @Override
    public java.rmi.Remote getPort(Class serviceEndpointInterface) throws javax.xml.rpc.ServiceException {
        try {
            if (MCAPIServiceSoap.class.isAssignableFrom(serviceEndpointInterface)) {
                var stub = new MCAPIServiceSoapStub(new java.net.URL(mcapiServiceSoapAddress), this);
                stub.setPortName(getMCAPIServiceSoapWSDDServiceName());
                return stub;
            }
        } catch (Throwable t) {
            throw new javax.xml.rpc.ServiceException(t);
        }
        throw new javax.xml.rpc.ServiceException(
            "There is no stub implementation for the interface:  "
                + (serviceEndpointInterface == null ? "null" : serviceEndpointInterface.getName()));
    }

    @Override
    public java.rmi.Remote getPort(QName portName, Class serviceEndpointInterface) throws javax.xml.rpc.ServiceException {
        if (portName == null) {
            return getPort(serviceEndpointInterface);
        }
        var inputPortName = portName.getLocalPart();
        if ("MCAPIServiceSoap".equals(inputPortName)) {
            return getMCAPIServiceSoap();
        } else {
            var stub = getPort(serviceEndpointInterface);
            ((org.apache.axis.client.Stub) stub).setPortName(portName);
            return stub;
        }
    }

    @Override
    public QName getServiceName() {
        return new QName("http://DEA.Web.Service.MasterCalendar.API/", "MCAPIService");
    }

    public Iterator<QName> getPorts() {
        if (ports == null) {
            ports = new HashSet<>();
            ports.add(new QName("http://DEA.Web.Service.MasterCalendar.API/", "MCAPIServiceSoap"));
        }
        return ports.iterator();
    }

    public void setEndpointAddress(String portName, String address) throws javax.xml.rpc.ServiceException {
        if ("MCAPIServiceSoap".equals(portName)) {
            setMCAPIServiceSoapEndpointAddress(address);
        } else {
            throw new javax.xml.rpc.ServiceException("Cannot set Endpoint Address for Unknown Port " + portName);
        }
    }

    public void setEndpointAddress(QName portName, String address) throws javax.xml.rpc.ServiceException {
        setEndpointAddress(portName.getLocalPart(), address);
    }
}
