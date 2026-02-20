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

import com.percussion.cms.objectstore.PSItemDefinition;
import com.percussion.search.objectstore.PSWSSearchField;
import com.percussion.search.objectstore.PSWSSearchParams;
import com.percussion.webservices.common.ConnectorTypes;
import com.percussion.webservices.common.OperatorTypes;
import com.percussion.webservices.content.PSSearchField;
import com.percussion.webservices.content.PSSearchParamsFolderFilter;
import com.percussion.webservices.content.PSSearchParamsTitle;
import com.percussion.webservices.content.PSSearchProperty;
import com.percussion.webservices.content.PSSearchResultField;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.beanutils.BeanUtilsBean;
import org.apache.commons.beanutils.Converter;
import org.apache.commons.lang3.StringUtils;

/**
 * Converts objects between the classes
 * <code>com.percussion.search.objectstore.PSWSSearchParams</code> and
 * <code>com.percussion.webservices.content.PSSearchSearchParams</code>.
 */
public class PSSearchParamsConverter extends PSConverter
{
   /* (non-Javadoc)
    * @see PSConverter#PSConvert(BeanUtilsUtil)
    */
   public PSSearchParamsConverter(BeanUtilsBean beanUtils)
   {
      super(beanUtils);
   }

   /* (non-Javadoc)
    * @see PSConverter#convert(Class, Object)
    */
   @SuppressWarnings("unchecked")
   @Override
   public Object convert(@SuppressWarnings("unused") Class type, Object value)
   {
      if (value == null)
         return null;

      if (isClientToServer(value))
      {
         com.percussion.webservices.content.PSSearchParams source =
            (com.percussion.webservices.content.PSSearchParams) value;

         PSWSSearchParams target = new PSWSSearchParams();

         String contentTypeName = source.getContentType();
         if (StringUtils.isBlank(contentTypeName))
         {
            target.setContentTypeId(-1);
         }
         else
         {
            PSItemDefinition def = PSItemConverterUtils.getItemDefinition(
               contentTypeName);
            target.setContentTypeId(def.getContentEditor().getContentType());
         }

         com.percussion.webservices.content.PSSearchParams.Title sourceTitle = source.getTitle();
         if (sourceTitle != null)
         {
            OperatorTypes sourceOperator = sourceTitle.getOperator() == null ? OperatorTypes.EQUAL
                  : sourceTitle.getOperator();
            Converter converter = getConverter(OperatorTypes.class);
            PSWSSearchField.PSOperatorEnum operator =
               (PSWSSearchField.PSOperatorEnum) converter.convert(
                  PSWSSearchField.PSOperatorEnum.class, sourceOperator);
            ConnectorTypes sourceConnector = sourceTitle.getConnector() == null ? ConnectorTypes.AND
                  : sourceTitle.getConnector();
            converter = getConverter(ConnectorTypes.class);
            PSWSSearchField.PSConnectorEnum connector =
               (PSWSSearchField.PSConnectorEnum) converter.convert(
                  PSWSSearchField.PSConnectorEnum.class, sourceConnector);
            target.setTitle(sourceTitle.getValue(), operator.getOrdinal(),
               connector.getOrdinal());
         }

         target.setFTSQuery(source.getFullTextQuery());

         Map<String, String> properties = new HashMap<String, String>();
         if (source.getProperties() != null && source.getProperties().getPSSearchProperty() != null)
         {
            for (PSSearchProperty property : source.getProperties().getPSSearchProperty())
               properties.put(property.getName(), property.getValue());
         }
         target.setProperties(properties);

         Collection<String> searchResults = new ArrayList<String>();
         if (source.getSearchResults() != null && source.getSearchResults().getPSSearchResultField() != null)
         {
            for (PSSearchResultField searchResult : source.getSearchResults().getPSSearchResultField())
               searchResults.add(searchResult.getName());
         }
         target.setResultFields(searchResults);

         List<PSWSSearchField> searchFields = new ArrayList<PSWSSearchField>();
         if (source.getParameter() != null && source.getParameter().getPSSearchField() != null)
         {
            for (PSSearchField field : source.getParameter().getPSSearchField())
            {
               Converter converter = getConverter(field.getClass());
               searchFields.add((PSWSSearchField) converter.convert(
                  PSWSSearchField.class, field));
            }
         }
         target.setSearchFields(searchFields);

         target.setSearchForFolders(source.isSearchForFolders());
         com.percussion.webservices.content.PSSearchParams.FolderFilter folderFilter = source.getFolderFilter();
         if (folderFilter != null)
            target.setFolderPathFilter(folderFilter.getValue(),
               folderFilter.isIncludeSubFolders());

         return target;
      }
      else
      {
         PSWSSearchParams source = (PSWSSearchParams) value;

         com.percussion.webservices.content.PSSearchParams target =
            new com.percussion.webservices.content.PSSearchParams();

         long contentTypeId = source.getContentTypeId();
         if (contentTypeId != -1)
         {
            PSItemDefinition def = PSItemConverterUtils.getItemDefinition(
               contentTypeId);
            target.setContentType(def.getName());
         }

         PSWSSearchField sourceTitle = source.getTitle();
         if (sourceTitle != null)
         {
            com.percussion.webservices.content.PSSearchParams.Title title = new com.percussion.webservices.content.PSSearchParams.Title();
            title.setValue(sourceTitle.getValue());
            Converter converter = getConverter(
               PSWSSearchField.PSOperatorEnum.class);
            OperatorTypes operator = (OperatorTypes) converter.convert(
               OperatorTypes.class, sourceTitle.getOperatorEnum());
            title.setOperator(operator);
            converter = getConverter(PSWSSearchField.PSConnectorEnum.class);
            ConnectorTypes connector = (ConnectorTypes) converter.convert(
               ConnectorTypes.class, sourceTitle.getConnectorEnum());
            title.setConnector(connector);
            target.setTitle(title);
         }

         target.setFullTextQuery(source.getFTSQuery());

         Map<String, String> sourceProperties = source.getProperties();
         com.percussion.webservices.content.PSSearchParams.Properties propertiesWrapper = new com.percussion.webservices.content.PSSearchParams.Properties();
         if (sourceProperties != null)
         {
            for (Entry<String, String> sourceProperty : sourceProperties.entrySet())
            {
               PSSearchProperty property = new PSSearchProperty();
               property.setName(sourceProperty.getKey());
               property.setValue(sourceProperty.getValue());
               propertiesWrapper.getPSSearchProperty().add(property);
            }
         }
         target.setProperties(propertiesWrapper);

         Collection<String> sourceSearchResults = source.getResultFields();
         com.percussion.webservices.content.PSSearchParams.SearchResults searchResultsWrapper = new com.percussion.webservices.content.PSSearchParams.SearchResults();
         if (sourceSearchResults != null)
         {
            for (String sourceSearchResult : sourceSearchResults)
            {
               PSSearchResultField searchResult = new PSSearchResultField();
               searchResult.setName(sourceSearchResult);
               searchResultsWrapper.getPSSearchResultField().add(searchResult);
            }
         }
         target.setSearchResults(searchResultsWrapper);

         List<PSWSSearchField> sourceSearchFields = source.getSearchFields();
         com.percussion.webservices.content.PSSearchParams.Parameter paramsWrapper = new com.percussion.webservices.content.PSSearchParams.Parameter();
         if (sourceSearchFields != null)
         {
            for (PSWSSearchField sourceSearchField : sourceSearchFields)
            {
                Converter converter = getConverter(sourceSearchField.getClass());
                paramsWrapper.getPSSearchField().add(((PSSearchField) converter.convert(
                   PSSearchField.class, sourceSearchField)));
            }
         }
         target.setParameter(paramsWrapper);
         target.setSearchForFolders(source.isSearchForFolders());

com.percussion.webservices.content.PSSearchParams.FolderFilter folderWrapper =
            new com.percussion.webservices.content.PSSearchParams.FolderFilter();
         folderWrapper.setValue(source.getFolderPathFilter());
         folderWrapper.setIncludeSubFolders(source.isIncludeSubFolders());
         target.setFolderFilter(folderWrapper);

         return target;
      }
   }
}
