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
package com.percussion.pso.jexl;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.percussion.extension.IPSJexlExpression;
import com.percussion.extension.IPSJexlMethod;
import com.percussion.extension.IPSJexlParam;
import com.percussion.extension.PSJexlUtilBase;
import com.percussion.utils.tools.PSCopyStream;
import org.apache.commons.codec.binary.Base64;

import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.ValueFormatException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

/**
 * 
 *
 * @author DavidBenua
 *
 */
public class PSOBase64Codec extends PSJexlUtilBase implements IPSJexlExpression
{
   /**
    * Logger for this class
    */
   private static final Logger log = LogManager.getLogger(PSOBase64Codec.class);

   /**
    * 
    */
   public PSOBase64Codec()
   {
      super();
   }

   /**
    * Base64 encode a binary property
    * @param jcrProperty the property to encode
    * @return a base 64 encoded String representing the property value.
    * @throws ValueFormatException
    * @throws RepositoryException
    * @throws IOException 
    * @throws UnsupportedEncodingException 
    */
   @IPSJexlMethod(description="Encode a binary property", 
         params={
        @IPSJexlParam(name="source", description="binary property to encode")})
   public String encode(Property jcrProperty) throws ValueFormatException, RepositoryException, UnsupportedEncodingException, IOException
   {
      if(jcrProperty == null)
      {
         log.debug("Property is null, no base64 encoding is possible" ); 
         return null; 
      }
      return encode(jcrProperty.getStream()); 
   }

   /**
    * Base64 encode a binary stream.  
    * @param stream the byte stream to be encoded.
    * @return a base 64 encoded String representing the binary value. 
    * @throws IOException 
    * @throws UnsupportedEncodingException 
    */
   @IPSJexlMethod(description="Encode a binary stream", 
         params={
        @IPSJexlParam(name="source", description="binary stream to encode")})
   public String encode(InputStream stream) throws UnsupportedEncodingException, IOException
   {
      return encode(streamToBytes(stream));
   }
   
   /**
    * Copy a binary stream into a byte array.
    * @param stream the binary stream to be copied
    * @return the byte array. Will be <code>null</code> if the 
    * stream is <code>null</code>. 
    * @throws IOException if a memory error occurs. 
    */
   @IPSJexlMethod(description="copy a binary stream to a byte array", 
         params={
        @IPSJexlParam(name="stream", description="binary stream to copy")})
   public byte[] streamToBytes(InputStream stream) throws IOException
   {
      ByteArrayOutputStream baos = new ByteArrayOutputStream(); 
      try
      {
         if(stream == null)
         {
            log.debug("No Stream found, cannot base64 encode ");
            return null;
         }
         PSCopyStream.copyStream(stream, baos);
      } catch (IOException ex)
      {
        // should never happen unless we run out of memory
        log.error("Unexpected exception Error: {}", ex.getMessage());
         log.debug(ex.getMessage(),ex);
        throw ex;
      } 
      return baos.toByteArray();
   }
   
   /**
    * Base 54 encode a byte array
    * @param bytes the byte array to be encoded. 
    * @return a base64 encoded String representing the binary value. 
    * @throws UnsupportedEncodingException if an error occurs. ASCII 
    * should always be supported, so this exception is impossible.  
    */
   @IPSJexlMethod(description="Encode a binary byte array", 
         params={
        @IPSJexlParam(name="source", description="binary byte array to encode")})
   public String encode(byte[] bytes) throws UnsupportedEncodingException
   {
      byte[] out = Base64.encodeBase64(bytes);
      try
      {
         return new String(out,"ASCII"); //base64 strings are ASCII only
      } catch (UnsupportedEncodingException ex)
      {
         //ASCII is always supported, this should never happen 
         log.error("Unsupported Encoding Error: {}", ex.getMessage());
         log.debug(ex.getMessage(),ex);
         throw ex; 
      }
      
   }
}
