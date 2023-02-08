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

package com.percussion.extensions.general;

import com.percussion.data.PSConversionException;
import com.percussion.extension.IPSUdfProcessor;
import com.percussion.extension.PSSimpleJavaUdfExtension;
import com.percussion.server.IPSRequestContext;
import com.percussion.util.PSUrlUtils;

import java.net.URL;
import java.util.HashMap;


/**
 * This class implements the UDF processor interface so it can be used as a
 * Rhythmyx function. See {@link #processUdf(Object[], IPSRequestContext)
 * processUdf} for a description.
 */
public class PSMakeAbsLink extends PSSimpleJavaUdfExtension
      implements IPSUdfProcessor
{
   /**
    * Creates a URL from the supplied parameters and returns it.
    * <p>A URI has the following pieces for purposes of this description
    * (see RFC 2396 for more details):
    * <p>
    * <p>&lt;scheme&gt;://&lt;host&gt;&lt;path-segments&gt;
    *    &lt;resource&gt;?&lt;query&gt;#&lt;fragment&gt;
    * <p>
    * <p>All parts except resource are optional.
    * <p>Five basic forms are allowed for the supplied URI:
    * <ul>
    * <li>Fully qualified
    * (e.g. http://server:9992/Rhythmyx/approot/res.html</li>
    * <li>Partially qualified (e.g. /Rhythmyx/approot/res.html)</li>
    * <li>Relative (e.g. ../myApp/res.html)</li>
    * <li>Resource name only (e.g. res.html)</li>
    * <li>An empty string.</li>
    * </ul>
    * <p>Any of these forms may contain a query and fragment part. Any relative
    * url is assumed to be relative to the orginiating request's app root.
    * <p>If the supplied URL is fully qualified and the protocol is not
    * 'http' or 'https', the supplied URL will be returned, unmodified.
    * Otherwise, any pieces supplied will be substituted.  If the supplied URL
    * is not fully qualified, the missing parts will be added. For a partially
    * qualified name, the http protocol, server and port will be added to the
    * supplied name. For an unqualified name, these items, plus the Rhythmyx
    * request root and the originating application request root will be added.
    * For a relative name, the protocol (http or https), server, port, and
    * Rhythmyx root will be added, assuming it is relative to the originating
    * request's app root.  For an empty string, all parts of the
    * originating request will be used, substituting the supplied parameters.If
    * the port is 80, no port number will be added to the generated url.
    * <p>
    * Note: The URL returned will use the 'http' protocol unless  the
    * supplied URL specifies 'https' or if the originating request is using
    * 'https'.
    * <table border="1">
    * <tr>
    * <th>Original Request Protocol</th><th>
    *    supplied URL protocol</th><th>Resulting protocol</th>
    *    <th>Resulting port</th>
    * </tr>
    * <tr>
    * <td>http</td><td>none</td><td>http</td><td>originating request port</td>
    * </tr>
    * <tr>
    * <td>https</td><td>none</td><td>https</td><td>originating request port</td>
    * </tr>
    * <tr>
    * <td>http</td><td>http</td><td>http</td><td>port from supplied URL
    *    </td>
    * </tr>
    * <tr>
    * <td>http</td><td>https</td><td>https</td><td>port from supplied URL</td>
    * </tr>
    * <tr>
    * <td>https</td><td>http</td><td>http</td><td>port from supplied URL</td>
    * </tr>
    * <tr>
    * <td>https</td><td>https</td><td>https</td><td>port from supplied URL</td>
    * </tr>
    * </table>
    *
    * <p>Multiple name/value pairs may be specified for the parameters.
    * For example, if the following were supplied as parameters:
    * <ul>
    *   <li>resource = query1.html</li>
    *   <li>param1 = city</li>
    *   <li>value1 = Boston</li>
    *   <li>param2 = state</li>
    *   <li>value2 = MA</li>
    * </ul>
    * then the following URL would be generated (assuming the request was
    * targeted directly at the Rhythmyx server):
    *    <p>http://rxserver:9992/Rhythmyx/MyApp/query1.html?city=Boston&state=MA
    *    </p>
    *
    *   <p>Note: The resource may contain parameters defined on it,
    *       in which case the supplied parameters will be appended after
    *       the last parameter defined therein.
    *
    *
    * @param params An array with elements as defined below. The array
    * is processed from beginning to end. As soon as the first <code>null</code>
    * parameter is encountered (<code>null</code> values allowed), processing
    * of the parameters will stop.
    *
    * <table border="1">
    *   <tr><th>Param #</th><th>Description</th><th>Required?</th><th>default
    *    value</th><tr>
    *   <tr>
    *     <td>1</td>
    *     <td> path to the resource.</td>
    *     <td>no</td>
    *     <td>""</td>
    *   </tr>
    *   <tr>
    *     <td>2 * N</td>
    *     <td>The name of the Nth parameter</td>
    *     <td>no</td>
    *     <td>none</td>
    *   </tr>
    *   <tr>
    *     <td>2 * N + 1</td>
    *     <td>The value of the Nth parameter</td>
    *     <td>no</td>
    *     <td>none</td>
    *   </tr>
    * </table>
    *
    * @param request The current request context. May not be <code>null</code>.
    *
    * @return The absolute URL created from the supplied foundation,
    *    user session information, and supplied parameters and values.
    *    If the resource is <code>null</code>, an empty string will
    *    be returned.  If the supplied base specifies a protocol other than
    *    HTTP or HTTPS, then it is returned unchanged.
    *
    * @throws PSConversionException If the url cannot be constructed.
    */
   public Object processUdf(Object[] params, IPSRequestContext request)
         throws PSConversionException
   {
      if ( null == params || params.length < 1 || null == params[0])
      {
         return "";
      }

      String sourceUrl = params[0].toString().trim();

      // build params map
      HashMap paramMap = new HashMap();
      int paramMaxIndex = params.length - 1;
      for ( int paramIndex = 1;
            paramIndex < paramMaxIndex && null != params[paramIndex]
            && params[paramIndex].toString().trim().length() > 0;
            paramIndex+=2 )
      {

         int valIndex = paramIndex+1;
         Object o = params[valIndex];
         if (o != null)
            o = o.toString();
         paramMap.put(params[paramIndex].toString(), o);
      }

      // create the url
      URL result = null;
      try
      {
         result = PSUrlUtils.createUrl(null, null, sourceUrl,
                                       paramMap.entrySet().iterator(),
                                       null, request);
      }
      catch (Throwable t)
      {
         throw new PSConversionException(0, t.toString());
      }

      return result;

   }
}
