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
package com.percussion.i18n.tmxdom;

import org.w3c.dom.Document;

/**
 * This interface defines a scheme and constants used in the scheme to merging
 * two TMX document nodes. The merge configuration document resembles the TMX
 * document in that there is only one element of each type in the TMX document.
 * The merge options to be used while merging this type of nodes are listed
 * under each element. Each merge option is a name-value pair under
 * configuration.
 */
public interface IPSTmxMergeConfig
{
   /**
    * Method that extracts the config params and returns as
    * {link PSTmxConfigParams} object.
    * @param    nodeid one of the merge nodeid's listed in this class. Never
    * <code>null</code>.
    * @return PSTmxConfigParams object for the given nodeid, may be <code>null</code>.
    */
   public PSTmxConfigParams getConfigParams(String nodeid);

   /**
    * Method to set new merge configuration document. The merge configuration
    * XML document must be based on the DTD defined for the default merge
    * configuartion XML file which is part of the archive. This document
    * provides the rules merging two TMX nodes.
    * @param doc XML document containg the merge configuration settings. Must not
    * be <code>null</code>
    * @throws IllegalArgumentException
    */
   void setConfigDoc(Document doc);

   /**
    * Uniquely identifiable nodeid to locate the merge configuration setting for
    * translation unit element in the merge configuration document.
    */
   static final String MERGE_NODEID_TU = IPSTmxDtdConstants.ELEM_TU;

   /**
    * Uniquely identifiable nodeid to locate the merge configuration setting for
    * note element(when it is a child of translation unit element) in the merge
    * configuration document.
    */
   static final String MERGE_NODEID_TU_NOTE =
      IPSTmxDtdConstants.ELEM_TU + "/" + IPSTmxDtdConstants.ELEM_NOTE;

   /**
    * Uniquely identifiable nodeid to locate the merge configuration setting for
    * prop element(when it is a child of translation unit element) in the merge
    * configuration document.
    */
   static final String MERGE_NODEID_TU_PROPERTY =
      IPSTmxDtdConstants.ELEM_TU + "/" + IPSTmxDtdConstants.ELEM_PROP;

   /**
    * Uniquely identifiable nodeid to locate the merge configuration setting for
    * translation unit variant element in the merge configuration document.
    */
   static final String MERGE_NODEID_TUV = IPSTmxDtdConstants.ELEM_TUV;

   /**
    * Uniquely identifiable nodeid to locate the merge configuration setting for
    * note element(when it is a child of translation unit variant element) in
    * the merge configuration document.
    */
   static final String MERGE_NODEID_TUV_NOTE =
      IPSTmxDtdConstants.ELEM_TUV + "/" + IPSTmxDtdConstants.ELEM_NOTE;

   /**
    * Uniquely identifiable nodeid to locate the merge configuration setting for
    * prop element(when it is a child of translation unit variant element) in
    * the merge configuration document.
    */
   static final String MERGE_NODEID_TUV_PROPERTY =
      IPSTmxDtdConstants.ELEM_TUV + "/" + IPSTmxDtdConstants.ELEM_PROP;

   /**
    * Uniquely identifiable nodeid to locate the merge configuration setting for
    * segment element in the merge configuration document.
    */
   static final String MERGE_NODEID_SEGMENT = IPSTmxDtdConstants.ELEM_SEG;

   /**
    * Merge optional name indicating whether to replace the TMX node with the
    * supplied one if already  exists in the current document.
    */
   static final String MERGE_OPTION_REPLACEIFEXISTS = "replaceifexists";

   /**
    * Merge optional name indicating whether to ignore the TMX node if the
    * supplied one already  exists in the current document.
    */
   static final String MERGE_OPTION_IGNOREIFEXISTS = "ignoreifexists";

   /**
    * Merge optional name indicating whether to delete the TMX node if the
    * supplied one already  exists in the current document.
    */
   static final String MERGE_OPTION_DELETEIFEXISTS = "deleteifexists";

   /**
    * Merge optional name indicating whether to modify the TMX node if the
    * supplied one already  exists in the current document. Modify means merge
    * all the child nodes applying their merge options.
    */
   static final String MERGE_OPTION_MODIFYIFEXISTS = "modifyifexists";

   /**
    * Merge optional name indicating whether to add the supplied TMX node if does
    * not exist in the current document.
    */
   static final String MERGE_OPTION_ADDIFNOTEXISTS = "addifnotexists";

   /**
    * Merge optional name indicating whether to ignore or do nothing if the
    * supplied TMX node does not exist in the current document.
    */
   static final String MERGE_OPTION_IGNOREIFNOTEXISTS = "ignoreifnotexists";

   /**
    * One of the two possible values for any merge option liste above.
    */
   static final String YES = "yes";
   /**
    * One of the two possible values for any merge option liste above.
    */
   static final String NO = "no";
}
