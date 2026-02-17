package org.apache.soap.messaging;

import org.apache.soap.Envelope;
import org.apache.soap.rpc.SOAPContext;
import org.apache.soap.transport.http.SOAPHTTPConnection;

import java.io.BufferedReader;
import java.net.URL;

/** Minimal shim for org.apache.soap.messaging.Message used by tests. */
public class Message
{
   private SOAPHTTPConnection transport = new SOAPHTTPConnection();

   public Message() {}

   public void send(URL target, String actionURI, Envelope env) throws Exception
   {
      // delegate to the HTTP transport stub; headers and context are ignored
      transport.send(target, actionURI, null, env, null, new SOAPContext());
   }

   public SOAPHTTPConnection getSOAPTransport()
   {
      return transport;
   }

   public SOAPContext getResponseSOAPContext()
   {
      return transport.getResponseSOAPContext();
   }
}
