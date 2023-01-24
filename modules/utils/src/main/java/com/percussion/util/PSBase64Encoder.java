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

package com.percussion.util;

import com.percussion.error.PSRuntimeException;
import com.percussion.tools.PSCopyStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Base64;

/**
 * The PSBase64Encoder class is used to encode data from the base64 format. It
 * is a wrapper for the <a href="http://jakarta.apache.org/commons/codec/">
 * Apache Commons Base64 codec</a>.
 * <p>
 * Base64 Encoding is defined in <A
 * HREF="http://www.cis.ohio-state.edu/htbin/rfc/rfc1521.html">RFC 1521</A>.
 * 
 * @author Tas Giakouminakis
 * @version 1.0
 * @since 1.0
 */
@Deprecated // Use java.util.Base64 instead
public class PSBase64Encoder
{
   /**
    * This allows the encoder to be run stand-alone. The sole argument should be
    * the text to encode.
    * 
    * @param args an array of arguments passed in. All provided strings will be
    *           base64 encoded and then printed to the console.
    */
   public static void main(String[] args)
   {

      if (args.length == 0)
      {
         System.out.println("B64() = ");
      }
      else
      {
         for (int i = 0; i < args.length; i++)
         {
            if (args[i].length() == 0)
            {
               System.out.println("B64() = ");
            }
            else
            {
               System.out.print("B64(");
               System.out.print(args[i]);
               System.out.print(") = ");
               System.out.print(encode(args[i]));
            }
         }
      }
   }

   /**
    * Encodes the specified string using the base64 algorithm.
    * 
    * @param in the string containing the plain text representation of the data,
    *           never <code>null</code>
    * 
    * @return a string containing the base64 encoded data, never
    *         <code>null</code>
    * @throws PSRuntimeException if anything goes wrong while encoding the
    *            provided string.
    */
   public static String encode(String in)
         throws com.percussion.error.PSRuntimeException
   {
      if (in == null)
         throw new IllegalArgumentException("input string may not be null");

      return encode(in, /* Default to none (use system default) */null);
   }

   /**
    * Encodes the specified string using the base64 algorithm.
    * 
    * @param in the string containing the plain text representation of the data
    * @param encoding the encoding string to use for retrieving the string to be
    *           encoded. if <code>null</code>, the platform's default
    *           charater set is used.
    * 
    * @return a string containing the base64 encoded data, never
    *         <code>null</code>
    * @throws PSRuntimeException if the specified encoding is not supported
    */
   public static String encode(String in, String encoding)
         throws PSRuntimeException
   {
      if (in == null)
         throw new IllegalArgumentException("input string may not be null");

      byte[] source;
      if (encoding == null)
      {
         source = in.getBytes();
      }
      else
      {
         try
         {
            source = in.getBytes(encoding);
         }
         catch (UnsupportedEncodingException e)
         {
            throw new PSRuntimeException(
                  IPSUtilErrors.BASE64_ENCODING_EXCEPTION, new Object[]
                  {in, e.toString()});
         }
      }

      byte[] encodedData = encode(source);

      return new String(encodedData);
   }

   /**
    * Encodes binary data from the specified input stream using the base64
    * algorithm, writing the encoded data to the specified output stream.
    * 
    * @param in the stream containing the plain text representation of the data,
    *           never <code>null</code>.
    * @param out the stream to store the base64 encoded data, never
    *           <code>null</code>.
    * 
    * @return The number of bytes encoded.
    * @throws IOException if an I/O exception occurs
    */
   public static long encode(InputStream in, OutputStream out)
         throws IOException
   {
      if (in == null)
         throw new IllegalArgumentException("input stream may not be null");
      if (out == null)
         throw new IllegalArgumentException("output stream may not be null");

      // read stream into byte[]
      try(ByteArrayOutputStream bout = new ByteArrayOutputStream(4096)) {
         PSCopyStream.copyStream(in, bout);
         byte[] encoded = encode(bout.toByteArray());
         try (ByteArrayInputStream bin = new ByteArrayInputStream(encoded)) {
            PSCopyStream.copyStream(bin, out);
            return encoded.length;
         }
      }
   }

   /**
    * Encodes binary data using the base64 algorithm and chunks the encoded
    * output into 76 character blocks.
    * 
    * @param in byte data to be encoded, never <code>null</code>
    * @return Base64 characters chunked in 76 character blocks
    */
   public static byte[] encode(byte[] in)
   {
      if (in == null)
         throw new IllegalArgumentException("array may not be null");
      return Base64.getMimeEncoder().encode(in);
   }

}
