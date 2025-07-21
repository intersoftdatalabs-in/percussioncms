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
package com.percussion.services.filter;

/**
 * Provides error codes for messages located in
 * {@code PSFilterErrorStringBundle}
 */
public interface IPSFilterServiceErrors {

   /**
    * Missing filter
    * <p>
    * The arguments passed in for this message are: <TABLE BORDER="1">
    * <TR>
    * <TH>Arg</TH>
    * <TH>Description</TH>
    * </TR>
    * <TR>
    * <TD>0</TD>
    * <TD>The name of the filter</TD>
    * </TR>
    * </TABLE>
    */
   int FILTER_MISSING = 1;

   /**
    * Unknown authtype
    * <p>
    * The arguments passed in for this message are: <TABLE BORDER="1">
    * <TR>
    * <TH>Arg</TH>
    * <TH>Description</TH>
    * </TR>
    * <TR>
    * <TD>0</TD>
    * <TD>The value of the authtype</TD>
    * </TR>
    * </TABLE>
    */
   int AUTHTYPE_MISSING = 2;

   /**
    * Filter rule missing
    * <p>
    * The arguments passed in for this message are: <TABLE BORDER="1">
    * <TR>
    * <TH>Arg</TH>
    * <TH>Description</TH>
    * </TR>
    * <TR>
    * <TD>0</TD>
    * <TD>The name of the missing rule</TD>
    * </TR>
    * </TABLE>
    */
   int RULE_MISSING = 3;

   /**
    * Database problem while processing a filter - no arguments
    */
   int DATABASE = 4;

   /**
    * Filter rule argument missing
    * <p>
    * The arguments passed in for this message are: <TABLE BORDER="1">
    * <TR>
    * <TH>Arg</TH>
    * <TH>Description</TH>
    * </TR>
    * <TR>
    * <TD>0</TD>
    * <TD>The name of the missing argument</TD>
    * </TR>
    * <TR>
    * <TD>1</TD>
    * <TD>The name of the rule</TD>
    * </TR>
    * </TABLE>
    */
   int ARGUMENT_MISSING = 5;

   /**
    * Filters are forming a likely graph cycle
    * <p>
    * The arguments passed in for this message are: <TABLE BORDER="1">
    * <TR>
    * <TH>Arg</TH>
    * <TH>Description</TH>
    * </TR>
    * <TR>
    * <TD>0</TD>
    * <TD>The name of the broken filter</TD>
    * </TR>
    * </TABLE>
    */
   int PROBABLE_CYCLE = 6;

   /**
    * Missing site id that is required for the filter
    */
   int SITE_MISSING = 7;

   /**
    * Problems loading the site
    * <p>
    * The arguments passed in for this message are: <TABLE BORDER="1">
    * <TR>
    * <TH>Arg</TH>
    * <TH>Description</TH>
    * </TR>
    * <TR>
    * <TD>0</TD>
    * <TD>The site id that could not be loaded</TD>
    * </TR>
    * </TABLE>
    */
   int SITE_LOAD = 8;

   /**
    * Problems loading the finder for the slot
    * <p>
    * The arguments passed in for this message are: <TABLE BORDER="1">
    * <TR>
    * <TH>Arg</TH>
    * <TH>Description</TH>
    * </TR>
    * <TR>
    * <TD>0</TD>
    * <TD>The finder name that could not be loaded</TD>
    * </TR>
    * </TABLE>
    */
   int FINDER_MISSING = 9;

   /**
    * Unexpected problem, essentially a runtime exception
    */
   int UNEXPECTED = 10;

   /**
    * The sys_context parameter is missing
    */
   int CONTEXT_MISSING = 11;

   /**
    * The filter or authtype parameters must be specified - no params
    */
   int PARAMS_AUTHTYPE_OR_FILTER = 12;

   /**
    * Missing item filter.
    * <p>
    * The arguments passed in for this message are:
    * <TABLE BORDER="1">
    * <TR><TH>Arg</TH><TH>Description</TH></TR>
    * <TR><TD>0</TD><TD>The id of the missing item filter.</TD></TR>
    * </TABLE>
    */
   int MISSING_ITEM_FILTER = 13;
}
