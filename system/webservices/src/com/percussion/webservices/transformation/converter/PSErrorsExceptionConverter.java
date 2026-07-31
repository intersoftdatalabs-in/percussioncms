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
package com.percussion.webservices.transformation.converter;

import com.percussion.services.guidmgr.data.PSDesignGuid;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.webservices.PSErrorException;
import com.percussion.webservices.PSErrorsException;
import com.percussion.webservices.PSLockErrorException;
import com.percussion.webservices.faults.PSError;
import com.percussion.webservices.faults.PSErrorsFaultBean;
import com.percussion.webservices.faults.PSErrorsFaultServiceCall;
import com.percussion.webservices.faults.PSErrorsFaultServiceCallError;
import com.percussion.webservices.faults.PSErrorsFaultServiceCallSuccess;
import com.percussion.webservices.faults.PSLockFaultBean;

import java.util.Map;

import org.apache.commons.beanutils.BeanUtilsBean;
import org.apache.commons.beanutils.ConversionException;
import org.apache.commons.beanutils.Converter;

/**
 * Converts objects between the classes 
 * <code>com.percussion.webservices.PSErrorsException</code> and 
 * <code>com.percussion.webservices.faults.PSErrorsFault</code>.
 */
public class PSErrorsExceptionConverter extends PSConverter
{
   /* (non-Javadoc)
    * @see PSConverter#PSConvert(BeanUtilsUtil)
    */
   public PSErrorsExceptionConverter(BeanUtilsBean beanUtils)
   {
      super(beanUtils);
   }

   /* (non-Javadoc)
    * @see PSConverter#convert(Class, Object)
    */
   @Override
   public Object convert(Class type, Object value)
   {
      if (value == null)
         return null;
      
      if (value instanceof PSErrorsFaultBean)
      {
         PSErrorsFaultBean source = (PSErrorsFaultBean) value;

         PSErrorsException target = new PSErrorsException();
         
         for (PSErrorsFaultBean.ServiceCall call : source.getServiceCall())
         {
            if (call.getSuccess() != null)
            {
               target.addResult(new PSDesignGuid(call.getSuccess().getId()));
            }
            else
            {
               PSErrorsFaultBean.ServiceCall.Error error = call.getError();
               Object errorValue = null;
               Class<?> errorType = null;
               if (error.getPSError() != null)
               {
                  errorValue = error.getPSError();
                  errorType = PSErrorException.class;
               }
               else if (error.getPSLockFault() != null)
               {
                  errorValue = error.getPSLockFault();
                  errorType = PSLockErrorException.class;
               }
               
               if (errorValue == null)
                  throw new ConversionException(
                     "No error value found for PSErrorsFaultBean converter.");
                  
               Converter converter = getConverter(errorType);
               target.addError(new PSDesignGuid(error.getId()), 
                  converter.convert(errorType, errorValue));
            }
         }
         
         return target;
      }
      else
      {
         PSErrorsException source = (PSErrorsException) value;

         PSErrorsFaultBean target = new PSErrorsFaultBean();
         target.setService("");

         for (IPSGuid id : source.getIds())
         {
            PSErrorsFaultBean.ServiceCall call = new PSErrorsFaultBean.ServiceCall();
            Object sourceError = source.getErrors().get(id);
            if (sourceError == null)
            {
               PSErrorsFaultBean.ServiceCall.Success success = 
                  new PSErrorsFaultBean.ServiceCall.Success();
               success.setId((new PSDesignGuid(id)).getValue());
               call.setSuccess(success);
            }
            else
            {
               Converter converter = getConverter(sourceError.getClass());

               PSErrorsFaultBean.ServiceCall.Error error = 
                  new PSErrorsFaultBean.ServiceCall.Error();
               error.setId((new PSDesignGuid(id)).getValue());
               if (sourceError instanceof PSLockErrorException)
               {
                  Object resultValue = converter.convert(PSLockFaultBean.class, 
                     sourceError);
                  error.setPSLockFault((PSLockFaultBean) resultValue);
               }
               else if (sourceError instanceof PSErrorException)
               {
                  Object resultValue = converter.convert(PSError.class, 
                     sourceError);
                  error.setPSError((PSError) resultValue);
               }
               else
                  throw new ConversionException(
                     "Unsupported PSErrorsException error type.");
               
               call.setError(error);
            }
            
            target.getServiceCall().add(call);
         }
         
         return target;
      }
   }
}

