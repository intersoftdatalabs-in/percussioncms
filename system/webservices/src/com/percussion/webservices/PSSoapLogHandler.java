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

package com.percussion.webservices;

import jakarta.xml.ws.handler.MessageContext;
import jakarta.xml.ws.handler.soap.SOAPHandler;
import jakarta.xml.ws.handler.soap.SOAPMessageContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import jakarta.xml.soap.SOAPException;
import jakarta.xml.soap.SOAPMessage;
import java.io.ByteArrayOutputStream;
import java.util.Collections;
import java.util.Set;

/**
 * Compatibility SOAP logging handler that works with JAX-WS/CXF. This class
 * replaces legacy Axis handlers and logs request/response SOAP messages when available.
 */
public class PSSoapLogHandler implements SOAPHandler<SOAPMessageContext> {

    private static final Logger log = LogManager.getLogger(PSSoapLogHandler.class);

    @Override
    public boolean handleMessage(SOAPMessageContext context) {
        Boolean outbound = (Boolean) context.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY);
        try {
            SOAPMessage msg = context.getMessage();
            if (msg != null) {
                ByteArrayOutputStream os = new ByteArrayOutputStream();
                msg.writeTo(os);
                String text = os.toString();
                if (Boolean.TRUE.equals(outbound)) {
                    log.debug("SOAP Response: {}", text);
                } else {
                    log.debug("SOAP Request: {}", text);
                }
            }
        } catch (SOAPException | java.io.IOException e) {
            log.debug("Failed to log SOAP message", e);
        }
        return true; // continue processing
    }

    @Override
    public boolean handleFault(SOAPMessageContext context) {
        return handleMessage(context);
    }

    @Override
    public void close(MessageContext context) {
        // no-op
    }

    @Override
    public Set getHeaders() {
        return Collections.emptySet();
    }
}
