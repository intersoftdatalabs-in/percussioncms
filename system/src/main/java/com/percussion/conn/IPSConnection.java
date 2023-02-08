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

package com.percussion.conn;

import com.percussion.error.PSIllegalStateException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;


/**
 * The IPSConnection interface defines methods to support remote as well as
 * local communication. The transport mechanism is hidden.
 *
 * @author     Paul Howard
 * @version    1.0
 * @since      1.0
**/
public interface IPSConnection
{
   /**
    * Get the input stream for reading on this connection.
    *
    * @return     the input stream
    *
    * @exception   IOException               if an i/o error occurs
    *
    * @exception   PSIllegalStateException   if close has already been called
   **/
   public InputStream getInputStream()
         throws IOException, PSIllegalStateException;

   /**
    * Get the output stream for writing to this connection. This should only
    * be used by E2 internals as sequence errors may occur if the data is
    * not written in the correct order.
    *
    * @return      the output stream
    *
    * @throws IOException if an i/o error occurs
    *
    * @throws PSIllegalStateException if close has already been called
   **/
   public OutputStream getOutputStream()
         throws IOException, PSIllegalStateException;

   /**
    * Attempt to make a connection to the server specified in the connection
    * properties supplied in the constructor.
    *
    * @throws   IOException      if an i/o error occurs
    *
    * @throws PSIllegalStateException if the connection is already open
   **/
   public void open()
         throws IOException, PSIllegalStateException;

   /**
    * Shutdown the connection. If the connection is not open, this method is a
    * no-op.
    *
    * @throws   IOException      if an i/o error occurs
    *
    * @throws PSIllegalStateException if the connection is not currently open
   **/
   public void close()
         throws IOException, PSIllegalStateException;


   /**
    * Has a connection been established?
    *
    * @return  <code>true</code> if it has; <code>false</code> otherwise
    */
   public boolean isOpen();

   /**
    * Get the host we're connected to.
    *
    * @return      the host address
    *
    * @throws IOException               if an i/o error occurs
    *
    * @throws PSIllegalStateException   if close has already been called
    */
   public InetAddress getHost()
      throws IOException, PSIllegalStateException;

   /**
    * Get the local host we're connected from.
    *
    * @return      the host address
    *
    * @exception   IOException               if an i/o error occurs
    *
    * @exception  PSIllegalStateException   if close has already been called
    */
   public InetAddress getLocalHost()
      throws IOException, PSIllegalStateException;

   /**
    * Get the local port we're connected from.
    *
    * @return      the local port
    *
    * @exception   IOException               if an i/o error occurs
    *
    * @exception  PSIllegalStateException   if close has already been called
    */
   public int getLocalPort()
      throws IOException, PSIllegalStateException;

   /**
    * Determines if the connection is over a secure socket.
    *
    * @return <code>true</code> if the connection is secure, <code>false</code>
    * otherwise
    */
   public boolean isSecure();

   /**
    * Gets the name of the cipher used if the connection is over a secure
    * socket.
    *
    * @return The cipher used, or <code>null</code> if the connection is not
    * secure.
    *
    * @see #isSecure()
    */
   public String getSSLCipher();

}

