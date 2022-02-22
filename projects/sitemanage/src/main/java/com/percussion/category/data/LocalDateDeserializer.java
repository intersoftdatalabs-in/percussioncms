/*
 *     Percussion CMS
 *     Copyright (C) 1999-2020 Percussion Software, Inc.
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Affero General Public License for more details.
 *
 *     Mailing Address:
 *
 *      Percussion Software, Inc.
 *      PO Box 767
 *      Burlington, MA 01803, USA
 *      +01-781-438-9900
 *      support@percussion.com
 *      https://www.percussion.com
 *
 *     You should have received a copy of the GNU Affero General Public License along with this program.  If not, see <https://www.gnu.org/licenses/>
 */

package com.percussion.category.data;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.percussion.share.service.exception.PSDataServiceException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.LocalDateTime;

public class LocalDateDeserializer extends JsonDeserializer<LocalDateTime>
{
    private static final Logger log = LogManager.getLogger(LocalDateDeserializer.class);
    public LocalDateTime deserialize(JsonParser arg0, DeserializationContext arg1){
        String dateInStringFormat= "";
        try{
            dateInStringFormat = arg0.getText();
            StringBuilder date = new StringBuilder();
            for(String doubledigit : dateInStringFormat.split("\\.")[0].split(":")){
                if(doubledigit.length()==1){
                    doubledigit = "0"+doubledigit;
                }
                date.append(doubledigit);
                date.append(":");
            }
            dateInStringFormat= date.substring(0, date.length()-1).toString()+"."+dateInStringFormat.split("\\.")[1];
            String time = dateInStringFormat.split("T")[1];
            String hour = time.split(":")[0];
            if(hour.length() == 1){
                dateInStringFormat = dateInStringFormat.replace("T"+hour,"T0"+hour);
            }
            return LocalDateTime.parse(dateInStringFormat);
        }catch (Exception e){
            log.error("Exception occurred while parsing : "+ dateInStringFormat +" : ", new PSDataServiceException(e.getMessage()));
        }
        return null;
    }
}

