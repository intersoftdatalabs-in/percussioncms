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

package com.percussion.data;

import com.percussion.design.objectstore.IPSReplacementValue;
import com.percussion.design.objectstore.PSBackEndColumn;
import com.percussion.design.objectstore.PSCgiVariable;
import com.percussion.design.objectstore.PSContentItemData;
import com.percussion.design.objectstore.PSContentItemStatus;
import com.percussion.design.objectstore.PSCookie;
import com.percussion.design.objectstore.PSDateLiteral;
import com.percussion.design.objectstore.PSDisplayFieldRef;
import com.percussion.design.objectstore.PSDisplayTextLiteral;
import com.percussion.design.objectstore.PSExtensionCall;
import com.percussion.design.objectstore.PSFunctionCall;
import com.percussion.design.objectstore.PSHtmlParameter;
import com.percussion.design.objectstore.PSLiteralSet;
import com.percussion.design.objectstore.PSMacro;
import com.percussion.design.objectstore.PSNumericLiteral;
import com.percussion.design.objectstore.PSOriginatingRelationshipProperty;
import com.percussion.design.objectstore.PSRelationshipProperty;
import com.percussion.design.objectstore.PSSingleHtmlParameter;
import com.percussion.design.objectstore.PSTextLiteral;
import com.percussion.design.objectstore.PSUrlRequest;
import com.percussion.design.objectstore.PSUserContext;
import com.percussion.design.objectstore.PSXmlField;
import com.percussion.util.PSMapClassToObject;

import java.lang.reflect.Constructor;

/**
 * The PSDataExtractorFactory class provides a convenient mechanism for
 * building data extractors. Since extractors are used all over, this
 * is a simpler way to avoid programming errors.
 *
 * @author     Tas Giakouminakis
 * @version    1.0
 * @since      1.0
 */
public abstract class PSDataExtractorFactory
{
   /**
    * Create a data extractor for the specified replacement value.
    *
    * @param      replValue      the replacement value to use
    *
    * @return                    the newly created extractor
    */
   public static IPSDataExtractor createReplacementValueExtractor(
      IPSReplacementValue replValue)
   {
      // hard to do this without a replacement value!
      if (replValue == null) {
         return null;
      }

      IPSDataExtractor ret = null;
      String errString = null;
      try{
         Constructor constructor = getExtractorConstructor(replValue);

         if (constructor != null){
            try {
               Object[] args = { replValue };
               ret = (IPSDataExtractor)constructor.newInstance(args);
            } catch (InstantiationException | IllegalArgumentException | IllegalAccessException | ExceptionInInitializerError inst){
               errString = inst.toString();
            } catch (java.lang.reflect.InvocationTargetException tar){
               errString = tar.getTargetException().toString();
            }
         } else{
            errString = "cannot create a data extractor for the specified replacement value";
         }
      }
      catch (NoSuchMethodException e) {
         errString = e.toString();
      }

      if (ret == null) {
         String arg0 = replValue.getValueType();
         if (errString != null)
            arg0 += "; " + errString;

         throw new IllegalArgumentException("unknown parameter type " +
               arg0 + " " + replValue.getClass().getName());
      }

      return ret;
   }


   /**
    * Get the extractor constructor for the specified class.
    */
   private static Constructor getExtractorConstructor(IPSReplacementValue replValue)
      throws NoSuchMethodException
   {
      Class extractorClass = (Class)ms_extractors.getMapping(replValue.getClass());
      if (extractorClass != null)
      {
         return extractorClass.getConstructor(
            new Class[] { replValue.getClass() });
      }

      return null;
   }

   /** a map from value class to extractor class */
   private static PSMapClassToObject ms_extractors = new PSMapClassToObject();

   static
   {
      // create the map of extracted value class -> extractor class
      ms_extractors.addReplaceMapping( PSBackEndColumn.class,
            PSBackEndColumnExtractor.class );
      ms_extractors.addReplaceMapping( PSCgiVariable.class,
            PSCgiVariableExtractor.class );
      ms_extractors.addReplaceMapping( PSCookie.class,
            PSCookieExtractor.class );
      ms_extractors.addReplaceMapping( PSDisplayFieldRef.class,
            PSDisplayFieldRefExtractor.class );
      ms_extractors.addReplaceMapping( PSHtmlParameter.class,
            PSHtmlParameterExtractor.class );
      ms_extractors.addReplaceMapping( PSExtensionCall.class,
            PSUdfCallExtractor.class );
      ms_extractors.addReplaceMapping( PSFunctionCall.class,
            PSFunctionCallExtractor.class );
      ms_extractors.addReplaceMapping( PSUserContext.class,
            PSUserContextExtractor.class );
      ms_extractors.addReplaceMapping( PSXmlField.class,
            PSXmlFieldExtractor.class );
      ms_extractors.addReplaceMapping( PSUrlRequest.class,
            PSUrlRequestExtractor.class );
      ms_extractors.addReplaceMapping( PSSingleHtmlParameter.class,
            PSSingleHtmlParameterExtractor.class );
      ms_extractors.addReplaceMapping(PSContentItemStatus.class,
         PSContentItemStatusExtractor.class );
      ms_extractors.addReplaceMapping(PSContentItemData.class,
         PSContentItemDataExtractor.class );
      ms_extractors.addReplaceMapping(PSRelationshipProperty.class,
            PSRelationshipPropertyExtractor.class );
      ms_extractors.addReplaceMapping(PSOriginatingRelationshipProperty.class,
            PSOriginatingRelationshipPropertyExtractor.class );
      ms_extractors.addReplaceMapping(PSMacro.class, PSMacroExtractor.class );

      // literals
      // each literal needs its own extractor class because getConstructor()
      // appears to match class names exactly, rather than using inheritance
      ms_extractors.addReplaceMapping( PSDateLiteral.class,
            PSDateLiteralExtractor.class );
      ms_extractors.addReplaceMapping( PSLiteralSet.class,
            PSLiteralSetExtractor.class );
      ms_extractors.addReplaceMapping( PSNumericLiteral.class,
            PSNumericLiteralExtractor.class );
      ms_extractors.addReplaceMapping( PSTextLiteral.class,
            PSTextLiteralExtractor.class );
      ms_extractors.addReplaceMapping( PSDisplayTextLiteral.class,
            PSDisplayTextLiteralExtractor.class );
   }
}
